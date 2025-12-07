# Guide de Dépannage - Détection de Position GPS

## Problème : La carte ne détecte pas la position actuelle

### Solution selon votre environnement

---

## 🖥️ **Sur Émulateur Android**

L'émulateur ne peut pas utiliser le GPS réel de votre ordinateur. Vous devez **définir manuellement** une position GPS.

### Méthode 1 : Via Extended Controls (Recommandée)

1. **Ouvrir Extended Controls**
   - Cliquez sur les **3 points** (⋮) en bas de l'émulateur
   - OU appuyez sur `Ctrl + Shift + P` (Windows/Linux) ou `Cmd + Shift + P` (Mac)

2. **Accéder à Location**
   - Dans le menu de gauche, sélectionnez **"Location"**
   - Vous verrez une carte Google Maps

3. **Définir une position**
   
   **Option A - Recherche par adresse :**
   - Tapez une adresse dans la barre de recherche en haut
   - Exemple : "Tunis, Tunisia" ou "Avenue Habib Bourguiba, Tunis"
   - Cliquez sur le résultat
   - Cliquez sur **"SET LOCATION"**

   **Option B - Cliquer sur la carte :**
   - Zoomez sur la zone désirée
   - Cliquez n'importe où sur la carte
   - Cliquez sur **"SET LOCATION"**

   **Option C - Coordonnées manuelles :**
   - Entrez directement les coordonnées GPS :
     - **Latitude** : `36.8065` (Tunis centre par exemple)
     - **Longitude** : `10.1815`
   - Cliquez sur **"SET LOCATION"**

4. **Vérifier**
   - Retournez à l'application Karhebti
   - La position devrait maintenant être détectée
   - Si ce n'est pas le cas, cliquez sur l'icône de rafraîchissement 🔄 sur la carte

### Méthode 2 : Via adb (Ligne de commande)

```bash
# Définir une position GPS (Tunis centre)
adb emu geo fix 10.1815 36.8065

# Syntaxe : adb emu geo fix [longitude] [latitude] [altitude]
# Note: longitude AVANT latitude !
```

### Positions GPS utiles en Tunisie

| Lieu | Latitude | Longitude | Commande adb |
|------|----------|-----------|--------------|
| **Tunis - Centre-ville** | 36.8065 | 10.1815 | `adb emu geo fix 10.1815 36.8065` |
| **La Marsa** | 36.8781 | 10.3247 | `adb emu geo fix 10.3247 36.8781` |
| **Carthage** | 36.8531 | 10.3233 | `adb emu geo fix 10.3233 36.8531` |
| **Sousse** | 35.8256 | 10.6369 | `adb emu geo fix 10.6369 35.8256` |
| **Sfax** | 34.7406 | 10.7603 | `adb emu geo fix 10.7603 34.7406` |
| **Monastir** | 35.7775 | 10.8264 | `adb emu geo fix 10.8264 35.7775` |

### Vérifier que le GPS fonctionne dans l'émulateur

1. Ouvrez l'application **Google Maps** dans l'émulateur
2. Cliquez sur l'icône de localisation (cible bleue)
3. Vous devriez voir un point bleu à l'emplacement défini
4. Si Google Maps fonctionne, Karhebti devrait aussi fonctionner

---

## 📱 **Sur Appareil Réel (Téléphone/Tablette)**

### Étape 1 : Activer le GPS

1. **Ouvrir les Paramètres**
   - Allez dans **Paramètres** > **Localisation**
   - OU tirez la barre de notifications et appuyez longuement sur l'icône GPS

2. **Activer la localisation**
   - Activez le bouton **"Utiliser la localisation"**
   - Sélectionnez le mode **"Haute précision"** ou **"Précision élevée"**

3. **Vérifier les permissions**
   - Dans Paramètres > Applications > Karhebti > Autorisations
   - Assurez-vous que **"Localisation"** est autorisée
   - Choisissez **"Autoriser tout le temps"** ou **"Uniquement pendant l'utilisation"**

### Étape 2 : Améliorer la précision GPS

1. **Sortez à l'extérieur**
   - Le GPS fonctionne mieux en extérieur
   - Évitez les bâtiments, tunnels, parkings couverts

2. **Activez le Wi-Fi**
   - Même sans connexion, le Wi-Fi aide à la géolocalisation
   - Activez **"Amélioration de la précision"** dans Paramètres > Localisation

3. **Attendez quelques secondes**
   - Le premier fix GPS peut prendre 10-30 secondes
   - Restez immobile pendant ce temps

4. **Calibrez la boussole**
   - Ouvrez Google Maps
   - Faites un mouvement en 8 avec votre téléphone
   - Cela calibre les capteurs

### Étape 3 : Résoudre les problèmes courants

**Problème : "Permission de localisation non accordée"**
- Solution : Allez dans Paramètres > Applications > Karhebti > Autorisations
- Activez la permission "Localisation"

**Problème : "GPS désactivé"**
- Solution : Activez le GPS dans Paramètres > Localisation

**Problème : Position imprécise ou ancienne**
- Solution 1 : Cliquez sur l'icône de rafraîchissement 🔄
- Solution 2 : Fermez et rouvrez l'application
- Solution 3 : Redémarrez le GPS (désactivez puis réactivez)

**Problème : Impossible d'obtenir la position**
- Solution 1 : Vérifiez que vous êtes à l'extérieur
- Solution 2 : Activez le Wi-Fi et les données mobiles
- Solution 3 : Désinstallez et réinstallez l'application
- Solution 4 : Effacez le cache GPS :
  - Téléchargez l'app "GPS Status & Toolbox"
  - Allez dans Outils > Manage A-GPS state > Reset

---

## 🔧 **Fonctionnalités de l'application**

### Sélection Manuelle de Position

Si le GPS ne fonctionne pas ou que vous voulez choisir un autre emplacement :

1. **Une fois la carte visible** (même avec une position par défaut)
2. **Appuyez n'importe où sur la carte** pour choisir une nouvelle position
3. L'indicateur passe de **bleu (GPS)** à **violet (Manuel)**
4. Le marqueur se déplace à l'emplacement touché
5. Vous pouvez ajuster en touchant ailleurs

### Retour au Mode GPS

1. Cliquez sur l'icône **Rafraîchir** 🔄 en bas de la carte
2. L'application va récupérer votre position GPS actuelle
3. L'indicateur repasse en **bleu (GPS)**

---

## 🐛 **Logs de Débogage**

Si le problème persiste, consultez les logs Android pour plus d'informations :

### Via Android Studio

1. Ouvrez **Logcat** (en bas de l'IDE)
2. Filtrez par **"BreakdownSOS"**
3. Cherchez les messages d'erreur en rouge

### Messages importants

- ✅ **"Location received"** : Position obtenue avec succès
- ✅ **"Using last known location"** : Position précédente utilisée
- ⚠️ **"Location request timed out"** : Délai d'attente dépassé (15s)
- ❌ **"Location is not available"** : GPS non disponible
- ❌ **"Failed to get current location"** : Échec de récupération

### Exemple de log réussi

```
D/BreakdownSOS: Starting location request...
D/BreakdownSOS: Last known location: lat=36.8065, lon=10.1815, age=5000ms
D/BreakdownSOS: Using recent last known location
D/BreakdownSOS: Fresh location received: lat=36.8065, lon=10.1815, accuracy=20.0m
```

---

## 📋 **Checklist de Dépannage**

### Sur Émulateur
- [ ] Ouvrir Extended Controls (⋮) > Location
- [ ] Définir une position (recherche, clic, ou coordonnées)
- [ ] Cliquer sur "SET LOCATION"
- [ ] Rafraîchir l'application Karhebti (icône 🔄)
- [ ] Si échec : redémarrer l'émulateur

### Sur Appareil Réel
- [ ] GPS activé dans Paramètres > Localisation
- [ ] Mode "Haute précision" sélectionné
- [ ] Permission accordée à l'application
- [ ] À l'extérieur avec vue dégagée du ciel
- [ ] Wi-Fi activé (même sans connexion)
- [ ] Attendre 10-30 secondes pour le fix GPS
- [ ] Essayer le rafraîchissement (🔄)
- [ ] En dernier recours : redémarrer l'appareil

---

## 🆘 **Solutions de Secours**

### Si le GPS ne fonctionne absolument pas

1. **Utilisez la sélection manuelle**
   - Dès que la carte s'affiche (même avec une position par défaut)
   - Touchez la carte où vous vous trouvez réellement
   - L'emplacement sera utilisé pour votre demande SOS

2. **Utilisez Google Maps pour trouver vos coordonnées**
   - Ouvrez Google Maps
   - Appuyez longuement sur votre position
   - Copiez les coordonnées affichées
   - Utilisez la sélection manuelle sur la carte Karhebti

---

## 🔄 **Améliorations Apportées (Décembre 2025)**

### Stratégies multiples de localisation

L'application utilise maintenant **3 stratégies simultanées** :

1. **Last Known Location** : Position précédente (instantanée si récente)
2. **Location Updates** : Demandes de mises à jour (pour position fraîche)
3. **Current Location** : API moderne (pour appareils récents)

### Timeout intelligent (15 secondes)

- L'application attend jusqu'à 15 secondes pour obtenir une position
- Si aucune position fraîche n'est reçue, elle utilise la dernière position connue
- Évite les blocages infinis

### Priorité équilibrée

- Utilise `PRIORITY_BALANCED_POWER_ACCURACY` au lieu de `HIGH_ACCURACY`
- Fonctionne mieux sur émulateurs et appareils avec GPS faible
- Consomme moins de batterie

### Messages d'erreur améliorés

- Instructions spécifiques pour émulateurs (Extended Controls)
- Guidage pour résoudre les problèmes sur appareils réels
- Suggestions contextuelles

---

## 📞 **Besoin d'Aide Supplémentaire ?**

Si ce guide ne résout pas votre problème :

1. **Vérifiez les logs** (section ci-dessus)
2. **Prenez une capture d'écran** du message d'erreur
3. **Notez votre environnement** :
   - Émulateur ou appareil réel ?
   - Version Android ?
   - Modèle d'appareil ?
4. **Consultez les issues GitHub** du projet

---

## ✅ **Test Rapide**

### Tester la localisation en 30 secondes

**Sur Émulateur :**
```bash
# 1. Définir position Tunis
adb emu geo fix 10.1815 36.8065

# 2. Ouvrir Karhebti
# 3. Aller dans SOS
# 4. La carte devrait afficher Tunis centre
```

**Sur Appareil Réel :**
```
1. Activer GPS (Paramètres > Localisation)
2. Sortir à l'extérieur
3. Ouvrir Karhebti
4. Aller dans SOS
5. Attendre 10-20 secondes
6. Position détectée !
```

---

**Dernière mise à jour : 5 décembre 2025**  
**Version de l'application : 1.0 avec amélioration GPS**
