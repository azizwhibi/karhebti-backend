# Fix : Disponibilité des créneaux de réparation

## Problème
Les créneaux de réparation sont marqués comme occupés même pour les réservations **en attente** (non confirmées par le propriétaire du garage).

## Solution requise (Backend)

L'endpoint `/repair-bays/garage/{garageId}/available` doit **filtrer uniquement les réservations confirmées** lors de la vérification de disponibilité.

### Code à modifier

**Fichier**: `backend/routes/repairBays.js` ou similaire

**Avant** :
```javascript
const occupiedBays = await Reservation.find({
  garageId: garageId,
  date: new Date(date),
  // Manque le filtre sur le statut !
  $or: [
    { heureDebut: { $lt: heureFin }, heureFin: { $gt: heureDebut } }
  ]
}).distinct('repairBayId');
```

**Après** :
```javascript
const occupiedBays = await Reservation.find({
  garageId: garageId,
  date: new Date(date),
  status: 'confirmée', // ✅ AJOUTER CETTE LIGNE
  $or: [
    { heureDebut: { $lt: heureFin }, heureFin: { $gt: heureDebut } }
  ]
}).distinct('repairBayId');
```

## Statuts de réservation

Les réservations ont les statuts suivants :
- `"en attente"` - Nouvelle réservation créée par l'utilisateur
- `"confirmée"` - Confirmée par le propriétaire du garage ✅ **SEULES CELLES-CI OCCUPENT UN CRÉNEAU**
- `"annulée"` - Annulée par l'utilisateur ou le garage
- `"terminée"` - Service complété

## Flux de réservation

1. **Utilisateur** crée une réservation → Statut: `"en attente"`
2. **PropGarage** consulte les réservations en attente
3. **PropGarage** confirme la réservation → Statut: `"confirmée"` → **Le créneau devient occupé**
4. Si le garage refuse → Statut: `"annulée"` → Le créneau reste disponible

## Impact sur l'application Android

✅ L'application Android est déjà correctement configurée :
- Le `RepairBayViewModel` appelle l'API `/repair-bays/garage/{garageId}/available`
- Il affiche correctement les créneaux retournés par le backend
- Aucune modification n'est requise côté Android

## Test de vérification

Après la correction backend, testez :

1. Créez une réservation (statut: "en attente")
2. Essayez de créer une autre réservation au même horaire
3. ✅ Le créneau doit être **DISPONIBLE** (car pas encore confirmé)
4. Confirmez la première réservation (statut: "confirmée")
5. Essayez de créer une autre réservation au même horaire
6. ❌ Le créneau doit être **OCCUPÉ** maintenant

---

**Date**: 2025-12-01
**Priorité**: HAUTE
**Assigné à**: Équipe Backend

