# 🎯 Guide Rapide - Configurer la Position GPS dans l'Émulateur

## Le Problème
L'émulateur Android **ne peut pas détecter automatiquement votre position**. Vous devez la définir manuellement.

---

## ⚡ Solution en 3 Étapes

### 1️⃣ Ouvrir Extended Controls

Dans l'émulateur Android, cliquez sur les **3 points (⋮)** en bas à droite :

```
┌────────────────────────┐
│                        │
│   Émulateur Android    │
│                        │
│                        │
│      [APP]             │
│                        │
└────────────────────────┘
          ⋮  ← Cliquez ici
```

### 2️⃣ Aller dans Location

Dans le menu qui s'ouvre, cliquez sur **"Location"** :

```
Extended Controls
├── Microphone
├── Battery
├── Phone
├── Directional pad
├── Fingerprint
├── Virtual sensors
├── Bug report
├── Settings
├── Help
└── ⏹ Close
    
    ►►► Location ◄◄◄  (Cliquez ici)
```

### 3️⃣ Définir une Position

Vous verrez une carte. Plusieurs options :

#### Option A - Recherche (Plus Simple)
1. Dans la barre de recherche en haut, tapez : **"Tunis, Tunisia"**
2. Cliquez sur le résultat
3. Cliquez sur le bouton **"SET LOCATION"** en bas

#### Option B - Coordonnées GPS
1. Dans les champs "Latitude" et "Longitude" :
   - **Latitude** : `36.8065`
   - **Longitude** : `10.1815`
2. Cliquez sur **"SET LOCATION"**

#### Option C - Cliquer sur la Carte
1. Naviguez sur la carte (zoom, déplacement)
2. Cliquez où vous voulez
3. Cliquez sur **"SET LOCATION"**

---

## 🌍 Positions GPS Prédéfinies (Tunisie)

Copiez-collez ces coordonnées dans Extended Controls > Location :

| Ville | Latitude | Longitude |
|-------|----------|-----------|
| **Tunis Centre** | 36.8065 | 10.1815 |
| **La Marsa** | 36.8781 | 10.3247 |
| **Carthage** | 36.8531 | 10.3233 |
| **Sousse** | 35.8256 | 10.6369 |
| **Sfax** | 34.7406 | 10.7603 |
| **Hammamet** | 36.4000 | 10.6167 |
| **Monastir** | 35.7775 | 10.8264 |
| **Nabeul** | 36.4564 | 10.7373 |

---

## ✅ Vérification

Après avoir défini la position :

1. **Retournez dans l'application Karhebti**
2. **Ouvrez l'écran SOS**
3. Vous devriez voir :
   - Un indicateur **bleu** avec "Position GPS actuelle"
   - La **carte** centrée sur la position définie
   - Les **coordonnées GPS** affichées en bas

4. **Si la position ne s'affiche pas** :
   - Cliquez sur l'icône **Rafraîchir** 🔄 en bas de la carte
   - OU fermez et rouvrez l'application

---

## 🚀 Méthode Alternative - Ligne de Commande

Si vous préférez la ligne de commande (plus rapide) :

```bash
# Ouvrir un terminal/cmd

# Définir position Tunis
adb emu geo fix 10.1815 36.8065

# ⚠️ IMPORTANT : longitude AVANT latitude !
# Format : adb emu geo fix [LONGITUDE] [LATITUDE]
```

**Autres villes :**

```bash
# La Marsa
adb emu geo fix 10.3247 36.8781

# Sousse
adb emu geo fix 10.6369 35.8256

# Sfax
adb emu geo fix 10.7603 34.7406

# Monastir
adb emu geo fix 10.8264 35.7775
```

---

## ❓ FAQ

### Q: Je ne vois pas l'icône ⋮ dans mon émulateur
**R:** Essayez de passer la souris sur le bord droit de l'émulateur. La barre d'outils devrait apparaître.

### Q: J'ai défini la position mais l'app ne la détecte toujours pas
**R:** Vérifiez que le **GPS est activé** dans l'émulateur :
1. Ouvrez les Paramètres de l'émulateur
2. Allez dans "Localisation"
3. Activez "Utiliser la localisation"

### Q: Puis-je simuler un déplacement ?
**R:** Oui ! Dans Extended Controls > Location :
1. Cliquez sur l'onglet **"Routes"**
2. Définissez un point de départ et un point d'arrivée
3. Cliquez sur **"PLAY ROUTE"**

### Q: L'application affiche "Récupération de votre position..."
**R:** C'est normal. L'application attend une mise à jour GPS. Options :
- Attendez 10-15 secondes (timeout automatique)
- Cliquez sur Rafraîchir 🔄
- Redéfinissez la position dans Extended Controls

---

## 🎓 En Résumé

```
1. Ouvrir émulateur
2. Cliquer sur ⋮ (Extended Controls)
3. Sélectionner "Location"
4. Chercher "Tunis" ou entrer coordonnées
5. Cliquer "SET LOCATION"
6. Retourner dans Karhebti
7. Ouvrir SOS
8. Position détectée ! ✅
```

---

## 🆘 Toujours un problème ?

Consultez le guide complet : **[LOCATION_TROUBLESHOOTING.md](./LOCATION_TROUBLESHOOTING.md)**

Ou utilisez la **sélection manuelle** :
- Touchez n'importe où sur la carte pour choisir votre position
- L'indicateur passera de bleu (GPS) à violet (Manuel)

---

**Astuce Pro** 💡 : Une fois la position définie dans l'émulateur, elle reste mémorisée jusqu'au redémarrage de l'émulateur.

**Dernière mise à jour : 5 décembre 2025**
