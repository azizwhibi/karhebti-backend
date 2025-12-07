# 🧪 Test Rapide - Page Notifications

## ⚡ 3 Minutes pour Tester

### Étape 1: Démarrer le Backend (30 sec)
```bash
cd votre-backend-folder
npm start
```
**Attendu**: "Server listening on port 3000"

---

### Étape 2: Ouvrir l'App (10 sec)
1. Lancer l'app sur l'émulateur
2. Se connecter si nécessaire

---

### Étape 3: Aller aux Notifications (5 sec)
```
Menu → 🔔 Notifications
OU
HomeScreen → Icône 🔔
```

---

### Étape 4: Observer l'État (1 min)

#### ✅ CAS 1: Notifications Présentes
```
┌────────────────────────────┐
│ 🔔 Notifications      [3]  │  ← Badge avec nombre
│ [←] [🔄] [⋮]              │
├────────────────────────────┤
│ 📋 Document expirant       │
│ Votre assurance...         │
│ 🕐 Il y a 2 heures         │
├────────────────────────────┤
│ ✅ Réservation confirmée   │
│ ...                        │
└────────────────────────────┘
```
**✅ SUCCÈS!** Vous voyez vos notifications!

---

#### ✅ CAS 2: Aucune Notification
```
┌────────────────────────────┐
│ 🔔 Notifications           │
│ [←] [🔄] [⋮]              │
├────────────────────────────┤
│          🔔                │
│   Aucune notification      │
│                            │
│ 💡 Les notifications       │
│ apparaîtront ici:          │
│ • Documents expirants      │
│ • Réservations             │
│ • Messages                 │
│                            │
│ [🔄 Vérifier maintenant]   │
└────────────────────────────┘
```
**✅ NORMAL!** Pas de notifications dans la BDD.

**Action**: Créer une notification de test (voir ci-dessous)

---

#### ❌ CAS 3: Erreur de Connexion
```
┌────────────────────────────┐
│          ❌                 │
│   Erreur de connexion      │
│                            │
│ failed to connect to       │
│ /10.0.2.2 (port 3000)      │
│                            │
│ ℹ️ Endpoint: /notifications│
│ Backend: 10.0.2.2:3000     │
│                            │
│    [🔄 Réessayer]          │
└────────────────────────────┘
```
**❌ PROBLÈME!** Backend pas démarré.

**Action**: Retour à l'Étape 1!

---

### Étape 5: Tester les Actions (1 min)

#### Action 1: Rafraîchir
1. Cliquer sur l'icône **🔄** (barre du haut)
2. Observer le spinner de chargement
3. Vérifier les logs Logcat

#### Action 2: Marquer comme Lu
1. Swiper ou cliquer sur une notification
2. Cliquer "Marquer comme lu"
3. Vérifier que le badge diminue

#### Action 3: Tout Marquer comme Lu
1. Cliquer sur **⋮** (menu)
2. Sélectionner "Tout marquer comme lu"
3. Vérifier que le badge disparaît

---

## 📊 Logs Logcat à Vérifier

### Filtrer par:
```
NotificationsScreen
```

### Logs Attendus:
```
D/NotificationsScreen: 🔄 Chargement initial des notifications...
D/NotificationsScreen: 📊 État: isLoading=false, count=5, unread=3
```

### Si Erreur:
```
D/NotificationsScreen: 📊 État: isLoading=false, count=0, error=API Error: 401
```

---

## 🎯 Créer une Notification de Test

### Option A: Via Backend (Recommandé)

**Créer le endpoint de test** (si pas déjà fait):
```javascript
// backend/routes/notifications.js
router.post('/test', authMiddleware, async (req, res) => {
  const notification = new Notification({
    userId: req.user.id,
    title: 'Test Notification',
    body: 'Ceci est une notification de test',
    status: 'pending',
    data: { type: 'test' }
  });
  await notification.save();
  res.json({ success: true, notification });
});
```

**Appeler l'endpoint**:
```bash
# Depuis un terminal
curl -X POST http://localhost:3000/notifications/test \
  -H "Authorization: Bearer VOTRE_TOKEN" \
  -H "Content-Type: application/json"
```

### Option B: Via MongoDB Compass

1. Ouvrir MongoDB Compass
2. Se connecter à `mongodb://localhost:27017`
3. Base de données: `karhebti`
4. Collection: `notifications`
5. Cliquer **INSERT DOCUMENT**
6. Coller:
```json
{
  "userId": "VOTRE_USER_ID",
  "title": "Test Notification",
  "body": "Ceci est un test",
  "status": "pending",
  "createdAt": { "$date": "2025-12-05T10:00:00.000Z" },
  "data": { "type": "test" }
}
```

### Option C: Via l'App (Déclencher Automatiquement)

**Méthode 1: Document Expirant**
1. Documents → Ajouter Document
2. Date d'expiration: Aujourd'hui + 7 jours
3. Le système devrait créer une notification automatique

**Méthode 2: Réservation**
1. Garages → Réserver
2. Confirmer la réservation
3. Notification de confirmation créée

**Méthode 3: Chat**
1. Envoyer un message
2. Notification de nouveau message

---

## ✅ Checklist de Test

- [ ] Backend démarré
- [ ] App ouverte sur page Notifications
- [ ] État affiché correctement
- [ ] Bouton 🔄 fonctionne
- [ ] Logs Logcat visibles
- [ ] Notifications visibles (si existantes)
- [ ] Badge compteur correct
- [ ] Actions fonctionnent (marquer lu, supprimer)

---

## 🎉 Résultat Attendu

### Si Tout Fonctionne:
```
✅ Page s'ouvre
✅ Chargement rapide (< 2 secondes)
✅ Affichage correct (liste/vide/erreur)
✅ Bouton 🔄 rafraîchit
✅ Actions fonctionnent
✅ Badge mis à jour
```

### Si Problème:
1. **Vérifier les logs** Logcat
2. **Vérifier le backend** (curl test)
3. **Vérifier le token** (Settings → Se reconnecter)
4. **Créer une notification** de test

---

## 🆘 Aide Rapide

### Problème: Rien ne s'affiche
**Solution**: 
```bash
# 1. Backend démarré?
ps aux | grep node

# 2. Token valide?
# App → Settings → Se déconnecter → Se reconnecter

# 3. Notification existe?
# Créer une via MongoDB ou endpoint test
```

### Problème: Erreur 401
**Solution**: Token expiré → Se reconnecter

### Problème: Erreur connexion
**Solution**: Backend pas démarré → `npm start`

---

**Temps Total**: 3-5 minutes
**Difficulté**: ⭐ Facile
**Résultat**: Voir vos notifications! 🎉

