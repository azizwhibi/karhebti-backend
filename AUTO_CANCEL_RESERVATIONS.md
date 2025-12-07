# Auto-Annulation des Réservations en Conflit

## 📋 Comportement Implémenté

Lorsqu'un **propriétaire de garage (propGarage)** confirme une réservation, **toutes les autres réservations en attente pour le même créneau, à la même date et avec des heures qui se chevauchent, sont automatiquement annulées**.

---

## 🔄 Flux Complet

### Scénario Exemple

**Configuration:**
- Garage: "Garage Central"
- Créneau: "Créneau 1"
- Date: 2025-12-15
- Horaire: 09:00 - 11:00

### Étape 1: Plusieurs utilisateurs créent des réservations

**Utilisateur A** crée une réservation:
```json
{
  "date": "2025-12-15",
  "heureDebut": "09:00",
  "heureFin": "11:00",
  "repairBayId": "créneau_1",
  "status": "en_attente"
}
```

**Utilisateur B** crée une réservation (même créneau, même horaire):
```json
{
  "date": "2025-12-15",
  "heureDebut": "09:00",
  "heureFin": "11:00",
  "repairBayId": "créneau_1",
  "status": "en_attente"
}
```

**Utilisateur C** crée une réservation (chevauchement partiel):
```json
{
  "date": "2025-12-15",
  "heureDebut": "10:00",
  "heureFin": "12:00",
  "repairBayId": "créneau_1",
  "status": "en_attente"
}
```

✅ **Résultat**: Les 3 réservations sont créées avec succès car elles sont toutes en attente.

---

### Étape 2: Le propGarage confirme une réservation

Le propriétaire du garage confirme la réservation de **l'Utilisateur A**:

**API Call:**
```http
PATCH /reservations/{id}/status
Authorization: Bearer {token_propGarage}

{
  "status": "confirmé"
}
```

---

### Étape 3: Annulation Automatique

**Ce qui se passe automatiquement:**

1. ✅ La réservation de **l'Utilisateur A** est confirmée
2. ❌ La réservation de **l'Utilisateur B** est annulée automatiquement (même horaire exact)
3. ❌ La réservation de **l'Utilisateur C** est annulée automatiquement (chevauchement 10:00-11:00)

**Réservations annulées reçoivent:**
```json
{
  "status": "annulé",
  "commentaires": "Annulée automatiquement - Créneau confirmé pour une autre réservation"
}
```

---

## 🔧 Implémentation Technique

### 1. Méthode `confirmReservation()` dans `RepairBaysService`

```typescript
// Trouve toutes les réservations en_attente qui :
// - Ont le même repairBayId
// - Ont la même date
// - Ont des heures qui se chevauchent

const conflictingPendingReservations = await this.reservationModel.find({
  _id: { $ne: reservationId },
  repairBayId: reservation.repairBayId,
  status: 'en_attente',
  date: { $gte: startOfDay, $lte: endOfDay },
  $or: [
    { 
      heureDebut: { $lt: reservation.heureFin }, 
      heureFin: { $gt: reservation.heureDebut } 
    }
  ]
});

// Annule toutes ces réservations
await this.reservationModel.updateMany(
  { _id: { $in: conflictingPendingReservations.map(r => r._id) } },
  { 
    $set: { 
      status: 'annulé',
      commentaires: 'Annulée automatiquement - Créneau confirmé pour une autre réservation'
    }
  }
);
```

### 2. Intégration dans `updateStatus()` de `ReservationsService`

Quand le statut passe à `"confirmé"`, le système appelle automatiquement `confirmReservation()` qui gère l'annulation des conflits.

---

## ✅ Avantages

1. **Pas de double-réservation**: Un créneau confirmé ne peut pas être réservé deux fois
2. **Gestion automatique**: Le propGarage n'a pas besoin d'annuler manuellement les autres réservations
3. **Transparence**: Les utilisateurs voient que leur réservation a été annulée avec une raison claire
4. **Historique**: Les réservations annulées restent en base pour audit

---

## 📱 Impact sur l'Application Android

### Notifications Recommandées

Il est recommandé d'envoyer des notifications aux utilisateurs dont les réservations ont été annulées:

```kotlin
// Pseudo-code Android
when (reservation.status) {
    "annulé" -> {
        if (reservation.commentaires?.contains("Annulée automatiquement") == true) {
            showNotification(
                title = "Réservation annulée",
                message = "Votre demande de réservation n'a pas pu être confirmée. Le créneau a été attribué à une autre réservation.",
                action = "Créer une nouvelle réservation"
            )
        }
    }
}
```

### Rafraîchissement de la Liste

Quand une réservation est annulée automatiquement, l'application devrait:
1. Rafraîchir la liste des réservations de l'utilisateur
2. Afficher un message expliquant l'annulation
3. Proposer de créer une nouvelle réservation avec d'autres créneaux disponibles

---

## 🧪 Tests à Effectuer

### Test 1: Annulation Automatique - Même Horaire Exact
1. Créer 3 réservations pour le même créneau, même date, 09:00-11:00
2. Confirmer la 1ère réservation
3. ✅ Vérifier que les 2 autres sont annulées

### Test 2: Annulation Automatique - Chevauchement Partiel
1. Créer réservation A: 09:00-11:00
2. Créer réservation B: 10:00-12:00 (chevauche 1h)
3. Confirmer réservation A
4. ✅ Vérifier que réservation B est annulée

### Test 3: Pas d'Annulation - Créneaux Différents
1. Créer réservation A: 09:00-11:00 sur Créneau 1
2. Créer réservation B: 09:00-11:00 sur Créneau 2
3. Confirmer réservation A
4. ✅ Vérifier que réservation B reste en_attente

### Test 4: Pas d'Annulation - Dates Différentes
1. Créer réservation A: 2025-12-15 09:00-11:00
2. Créer réservation B: 2025-12-16 09:00-11:00 (même créneau)
3. Confirmer réservation A
4. ✅ Vérifier que réservation B reste en_attente

---

## 📊 Logs Backend

Le système affiche des logs pour suivre les annulations:

```
✅ 2 réservation(s) en attente annulée(s) automatiquement pour le créneau Créneau 1
✅ Réservation 674c5e8a9f1234567890abcd confirmée pour le créneau Créneau 1
```

---

**Date de Mise en Place**: 2025-12-01  
**Version**: 1.0  
**Statut**: ✅ Implémenté et Testé

