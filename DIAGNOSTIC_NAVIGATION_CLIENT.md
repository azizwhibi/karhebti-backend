# 🔍 DIAGNOSTIC - Navigation automatique ne fonctionne pas

## 📱 SYMPTÔME

L'utilisateur (client) reste bloqué sur l'écran "En attente de confirmation" avec le message "Connexion au garage..." même après que le garage ait accepté la demande.

**Écran actuel :**
- Title: "En attente de confirmation"
- Message: "Demande SOS envoyée !"
- Animation: Spinner rouge "Connexion au garage..."
- ID: 693431bc...
- Bouton: "✕ Annuler et retourner"

**Écran attendu après acceptation :**
- ClientTrackingScreen
- Banner: "✅ Demande acceptée!"
- Carte avec 2 positions
- Info garage

---

## 🔍 CAUSES POSSIBLES

### 1. Polling ne fonctionne pas ❌
**Test :**
```bash
adb logcat | grep "SOSWaiting\|BreakdownViewModel\|getBreakdownStatus"
```

**Logs attendus :**
```
D/BreakdownViewModel: getBreakdownStatus: 693431bc...
D/BreakdownsRepo: getBreakdownString: 693431bc...
D/SOSWaitingScreen: Status: PENDING
D/SOSWaitingScreen: Status: ACCEPTED
D/SOSWaitingScreen: Navigating to ClientTracking
```

**Si aucun log :** Le polling ne s'exécute pas

### 2. Backend ne retourne pas ACCEPTED ❌
**Test :**
```bash
# Dans le backend
# Vérifier les logs après que le garage accepte
# Devrait afficher :
✅ Status: PENDING → ACCEPTED
```

**Test API :**
```bash
curl -X GET "http://10.0.2.2:3000/api/breakdowns/693431bc..." \
  -H "Authorization: Bearer YOUR_TOKEN"

# Response devrait contenir :
# "status": "ACCEPTED"
```

### 3. Navigation ne se déclenche pas ❌
**Test :**
```bash
adb logcat | grep "NavController\|navigate"
```

**Si "ACCEPTED" détecté mais pas de navigation :** Problème dans le callback

---

## ✅ SOLUTION IMMÉDIATE

### Étape 1 : Vérifier les Logs

**Terminal 1 - Backend :**
```bash
# Logs backend
npm start

# Après que garage accepte, vérifier :
✅ [ACCEPT] Breakdown: 693431bc... by prop.garage@example.com
✅ Status: PENDING → ACCEPTED
```

**Terminal 2 - Android Client :**
```bash
adb logcat | grep "Breakdown\|SOSWaiting"

# Devrait afficher toutes les 5 secondes :
D/BreakdownViewModel: getBreakdownStatus: 693431bc...
D/BreakdownsRepo: getBreakdownString: 693431bc...
D/BreakdownsRepo: getBreakdownString success

# Après acceptation :
D/SOSWaitingScreen: Status changed to ACCEPTED
D/NavController: Navigating to ClientTracking
```

### Étape 2 : Vérifier la DB

```javascript
// MongoDB
db.breakdowns.findOne({ _id: ObjectId("693431bc...") })

// Vérifier :
{
  "status": "ACCEPTED",  // Doit être ACCEPTED
  "assignedTo": "6932f6f96551fb27afecc516",
  "acceptedAt": ISODate("2025-12-06T...")
}
```

### Étape 3 : Ajouter des Logs de Debug

Si les logs ne s'affichent pas, modifiez `SOSWaitingScreen.kt` temporairement :

```kotlin
// Dans SOSWaitingScreen.kt
LaunchedEffect(breakdownId) {
    while (true) {
        try {
            android.util.Log.d("SOSWaiting", "🔄 Polling status for: $breakdownId")
            val result = onGetBreakdownStatus(breakdownId)
            
            result.onSuccess { breakdown ->
                android.util.Log.d("SOSWaiting", "✅ Status: ${breakdown.status}")
                breakdownStatus = breakdown
                isLoading = false

                when (breakdown.status.uppercase()) {
                    "ACCEPTED" -> {
                        android.util.Log.d("SOSWaiting", "🎉 ACCEPTED! Navigating...")
                        delay(1000)
                        onGarageAccepted(breakdown)
                        return@LaunchedEffect
                    }
                    //...existing code...
                }
            }
            result.onFailure {
                android.util.Log.e("SOSWaiting", "❌ Error: ${it.message}")
                errorMessage = it.message
            }
        } catch (e: Exception) {
            android.util.Log.e("SOSWaiting", "❌ Exception: ${e.message}", e)
            errorMessage = e.message
        }

        delay(5000)
    }
}
```

---

## 🐛 PROBLÈMES CONNUS ET SOLUTIONS

### Problème 1 : Polling trop lent (5 secondes)
**Solution :** Réduire à 3 secondes

```kotlin
delay(3000) // Au lieu de 5000
```

### Problème 2 : Backend retourne status en minuscules
**Solution :** Utiliser `.uppercase()` (déjà fait)

```kotlin
when (breakdown.status.uppercase()) { ... }
```

### Problème 3 : Navigation bloquée par erreur
**Solution :** Vérifier que `Screen.ClientTracking` existe

```kotlin
// Dans NavGraph.kt
object ClientTracking : Screen("client_tracking/{breakdownId}") {
    fun createRoute(breakdownId: String) = "client_tracking/$breakdownId"
}
```

---

## 🧪 TEST MANUEL

### Test 1 : Forcer la Navigation

**Modifiez temporairement SOSWaitingScreen :**

```kotlin
// Ajoutez un bouton de test
Button(onClick = {
    breakdownStatus?.let { onGarageAccepted(it) }
}) {
    Text("🧪 TEST: Force Navigate")
}
```

**Si la navigation fonctionne :** Le problème est le polling ou le backend

**Si la navigation ne fonctionne pas :** Le problème est ClientTrackingScreen ou NavGraph

### Test 2 : Vérifier ClientTrackingScreen

**Naviguez manuellement :**

```kotlin
// Dans NavGraph, ajoutez temporairement un bouton dans Home
Button(onClick = {
    navController.navigate(Screen.ClientTracking.createRoute("693431bc..."))
}) {
    Text("🧪 TEST: Go to ClientTracking")
}
```

**Si l'écran s'affiche :** Le problème est le polling

**Si erreur :** ClientTrackingScreen a un problème

---

## ✅ SOLUTION DÉFINITIVE

### Si Polling ne fonctionne pas :

**Réduire l'intervalle et ajouter logs :**

```kotlin
// SOSWaitingScreen.kt
LaunchedEffect(breakdownId) {
    android.util.Log.d("SOSWaiting", "🚀 Starting polling for $breakdownId")
    
    while (true) {
        try {
            android.util.Log.d("SOSWaiting", "🔄 Polling... (${System.currentTimeMillis()})")
            val result = onGetBreakdownStatus(breakdownId)
            
            result.onSuccess { breakdown ->
                android.util.Log.d("SOSWaiting", "📊 Status: ${breakdown.status}")
                
                //...existing code...
            }
        } catch (e: Exception) {
            android.util.Log.e("SOSWaiting", "💥 Exception", e)
        }

        delay(3000) // 3 secondes au lieu de 5
    }
}
```

### Si Backend ne met pas à jour :

**Vérifiez l'endpoint accept :**

```javascript
// backend/routes/breakdowns.js
router.put('/:id/accept', authenticateToken, async (req, res) => {
    const breakdown = await Breakdown.findById(req.params.id);
    
    // IMPORTANT : Mettre à jour le statut
    breakdown.status = 'ACCEPTED'; // Pas 'accepted'
    breakdown.assignedTo = req.user.sub;
    breakdown.acceptedAt = new Date();
    
    await breakdown.save();
    
    console.log(`✅ Status updated: ${breakdown.status}`); // Vérifier le log
    
    res.json(breakdown);
});
```

---

## 📝 CHECKLIST DE VÉRIFICATION

### Backend
- [ ] Backend running sur port 3000
- [ ] Endpoint `/accept` fonctionne
- [ ] Status mis à jour en DB (vérifier avec MongoDB)
- [ ] Logs backend montrent "ACCEPTED"

### Android Client
- [ ] App recompilée après derniers changements
- [ ] Polling s'exécute (logs toutes les 3-5 secondes)
- [ ] Status "ACCEPTED" détecté dans les logs
- [ ] Navigation déclenchée
- [ ] ClientTrackingScreen s'affiche

### Navigation
- [ ] Route `Screen.ClientTracking` existe
- [ ] Composable ClientTrackingScreen enregistré dans NavHost
- [ ] Import ClientTrackingScreen dans NavGraph

---

## 🚀 COMMANDES RAPIDES

### Vérifier Logs Android
```bash
# Tous les logs breakdown
adb logcat | grep "Breakdown"

# Logs polling
adb logcat | grep "SOSWaiting"

# Logs navigation
adb logcat | grep "navigate\|NavController"
```

### Vérifier DB
```javascript
// MongoDB Shell
use karhebti
db.breakdowns.find({ status: "ACCEPTED" }).pretty()
```

### Tester API
```bash
# Get breakdown status
curl http://10.0.2.2:3000/api/breakdowns/693431bc... \
  -H "Authorization: Bearer TOKEN"
```

---

## 🎯 RÉSULTAT ATTENDU

**Après que le garage accepte :**

```
📱 CLIENT APP

1. Polling détecte status ACCEPTED
   D/SOSWaiting: Status: ACCEPTED

2. Délai 1 seconde

3. Navigation automatique
   D/NavController: navigate(client_tracking/693431bc...)

4. ClientTrackingScreen s'affiche
   ╔════════════════════════════════════╗
   ║  🎉 Garage trouvé!                ║
   ║  ✅ Demande acceptée!              ║
   ║  [Carte avec 2 positions]         ║
   ╚════════════════════════════════════╝

✅ SUCCÈS !
```

---

## 📞 SI PROBLÈME PERSISTE

**Envoyez-moi :**

1. **Logs Android complets :**
```bash
adb logcat > android_logs.txt
```

2. **Logs Backend :**
Terminal backend après acceptation

3. **DB Status :**
```javascript
db.breakdowns.findOne({ _id: ObjectId("693431bc...") })
```

4. **Screenshot :**
Écran "En attente de confirmation"

---

**Date:** 6 Décembre 2025  
**Symptôme:** Navigation automatique ne se déclenche pas  
**Solution:** Vérifier polling + backend + logs  
**Status:** 🔍 Diagnostic en cours

