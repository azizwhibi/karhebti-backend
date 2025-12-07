# 🧪 TEST RAPIDE - Navigation Automatique Client

## 📱 OBJECTIF

Vérifier que l'utilisateur (client) navigue automatiquement de "En attente de confirmation" vers "ClientTrackingScreen" quand le garage accepte.

---

## ⚡ TEST EN 5 MINUTES

### Étape 1 : Préparer 2 Terminaux

**Terminal 1 - Logs Android :**
```bash
adb logcat | grep "SOSWaiting\|ClientTracking\|Breakdown"
```

**Terminal 2 - Backend (déjà running)**

### Étape 2 : Lancer le Test

**1. CLIENT (Device 1) :**
```
Login user normal
Home → SOS → PNEU
Envoyer SOS
```

**Écran affiché :**
```
╔════════════════════════════════════╗
║  En attente de confirmation       ║
║  Demande SOS envoyée !            ║
║  ⏳ Connexion au garage...        ║
║  ID: 693431bc...                  ║
╚════════════════════════════════════╝
```

**Logs attendus (Terminal 1) :**
```
D/SOSWaiting: 🚀 Starting polling for breakdown: 693431bc...
D/SOSWaiting: 🔄 Polling status... (interval: 3s)
D/BreakdownViewModel: getBreakdownStatus: 693431bc...
D/BreakdownsRepo: getBreakdownString: 693431bc...
D/SOSWaiting: ✅ Got status: PENDING
D/SOSWaiting: 🔄 Polling status... (interval: 3s)
D/SOSWaiting: ✅ Got status: PENDING
... (répété toutes les 3 secondes)
```

**2. GARAGE (Device 2) :**
```
Login prop.garage@example.com
Home → "🚨 Demandes SOS"
Click sur la demande
Click "✅ Accepter" → "Confirmer"
```

**Logs Backend (Terminal 2) :**
```
🟢 [ACCEPT] Breakdown: 693431bc... by prop.garage@example.com
✅ Status: PENDING → ACCEPTED
👤 assignedTo = 6932f6f96551fb27afecc516
```

**3. CLIENT (retour Device 1) - AUTOMATIQUE :**

**Logs attendus (Terminal 1) :**
```
D/SOSWaiting: 🔄 Polling status... (interval: 3s)
D/BreakdownsRepo: getBreakdownString success: 693431bc...
D/SOSWaiting: ✅ Got status: ACCEPTED
D/SOSWaiting: 🎉 Status ACCEPTED detected! Navigating to ClientTracking...
D/NavController: navigate(client_tracking/693431bc...)
D/ClientTrackingScreen: Displayed with distance: 7.1 km, ETA: 21 min
```

**Écran affiché (AUTOMATIQUEMENT) :**
```
╔════════════════════════════════════╗
║  🎉 Garage trouvé!         [←]    ║
╠════════════════════════════════════╣
║  ✅ Demande acceptée!              ║
║  🗺️ Navigation démarrée            ║
║  🚗 Auto Service Pro               ║
║  ⏱️ ETA: 15 minutes                ║
║                                    ║
║  [Carte avec 2 positions]         ║
║  🏢 Garage ─────→ 📍 Vous         ║
║                                    ║
║  📏 Distance: 7.1 km               ║
║  [📞 Appeler le garage]            ║
╚════════════════════════════════════╝
```

---

## ✅ RÉSULTAT ATTENDU

### Timing
```
T+0s   : Client envoie SOS
T+3s   : Premier polling (PENDING)
T+6s   : Deuxième polling (PENDING)
T+9s   : Troisième polling (PENDING)
T+15s  : Garage accepte
T+18s  : Polling détecte ACCEPTED
T+19s  : Navigation automatique
T+20s  : ClientTrackingScreen affiché

Total : ~20 secondes max
```

### Logs Complets
```
CLIENT LOGS:
D/SOSWaiting: 🚀 Starting polling
D/SOSWaiting: 🔄 Polling... PENDING
D/SOSWaiting: 🔄 Polling... PENDING
D/SOSWaiting: 🔄 Polling... PENDING
D/SOSWaiting: ✅ Got status: ACCEPTED
D/SOSWaiting: 🎉 Navigating to ClientTracking
✅ ClientTrackingScreen displayed

BACKEND LOGS:
🟢 [ACCEPT] Breakdown: 693431bc...
✅ Status: PENDING → ACCEPTED
```

---

## ❌ SI ÇA NE MARCHE PAS

### Problème 1 : Pas de logs "🔄 Polling"

**Cause :** Polling ne démarre pas

**Solution :**
1. Vérifiez que l'app est recompilée
2. Vérifiez que SOSWaitingScreen.kt a les nouveaux logs
3. Recompilez :
```bash
gradlew.bat clean assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### Problème 2 : Logs "PENDING" à l'infini

**Cause :** Backend ne met pas à jour le status

**Solution :**
1. Vérifiez les logs backend
2. Vérifiez la DB :
```javascript
db.breakdowns.findOne({ _id: ObjectId("693431bc...") })
// status doit être "ACCEPTED"
```
3. Si status toujours "PENDING", le problème est dans le backend

### Problème 3 : Log "ACCEPTED" détecté mais pas de navigation

**Cause :** Callback onGarageAccepted ne fonctionne pas

**Solution :**
1. Vérifiez les logs NavController
2. Vérifiez que Screen.ClientTracking existe
3. Vérifiez que le composable est enregistré dans NavHost

### Problème 4 : Navigation fonctionne mais écran blanc

**Cause :** ClientTrackingScreen a une erreur

**Solution :**
```bash
adb logcat | grep "Error\|Exception\|ClientTracking"
```

---

## 🎯 CHECKLIST RAPIDE

### Avant de tester
- [ ] Backend running
- [ ] App recompilée avec nouveaux logs
- [ ] 2 terminaux ouverts (logs + backend)
- [ ] 2 devices/emulators prêts

### Pendant le test
- [ ] Client envoie SOS
- [ ] Logs polling s'affichent (toutes les 3s)
- [ ] Status = PENDING dans les logs
- [ ] Garage accepte la demande
- [ ] Backend log: "ACCEPTED"
- [ ] Client log: "ACCEPTED detected"
- [ ] Client log: "Navigating"
- [ ] ClientTrackingScreen s'affiche

### Après le test
- [ ] Screenshot ClientTrackingScreen
- [ ] Logs sauvegardés
- [ ] Test répété avec succès

---

## 🚀 COMMANDES RAPIDES

### Recompiler
```bash
gradlew.bat clean assembleDebug && adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### Logs en temps réel
```bash
# Terminal 1
adb logcat | grep "SOSWaiting"

# Terminal 2  
adb logcat | grep "ClientTracking"

# Terminal 3
adb logcat | grep "Error"
```

### Vérifier DB
```javascript
db.breakdowns.findOne({ _id: ObjectId("693431bc...") })
```

---

## 📊 RÉSULTAT FINAL

**✅ Test réussi si :**
1. Polling s'exécute toutes les 3 secondes
2. Status ACCEPTED détecté dans les logs
3. Navigation automatique déclenchée
4. ClientTrackingScreen s'affiche
5. Carte avec 2 positions visible
6. Distance et ETA affichés

**❌ Test échoué si :**
1. Pas de logs polling
2. Status toujours PENDING
3. Pas de navigation automatique
4. Écran blanc ou erreur

---

**Date:** 6 Décembre 2025  
**Status:** 🧪 Prêt pour test  
**Durée:** 5 minutes  
**Action:** Lancez le test maintenant !

