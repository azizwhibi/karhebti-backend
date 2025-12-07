# 🎯 Solution Rapide - Position GPS sur Émulateur

## Le Problème
```
❌ "La maps ne détecte pas la position actuelle"
```

## La Solution (3 Étapes)

### 📍 Étape 1 : Ouvrir Extended Controls

Dans l'émulateur, cherchez cette icône en bas à droite :

```
┌─────────────────────────┐
│                         │
│   ÉMULATEUR ANDROID     │
│                         │
│      [Écran App]        │
│                         │
│                         │
└─────────────────────────┘
         ⋮  ← CLIQUEZ ICI
    (3 points)
```

**Alternative Clavier :**
- Windows/Linux : `Ctrl + Shift + P`
- Mac : `Cmd + Shift + P`

---

### 🗺️ Étape 2 : Sélectionner Location

Un menu s'ouvre. Cliquez sur **"Location"** :

```
┌──────────────────────────────┐
│  Extended Controls           │
├──────────────────────────────┤
│  📱 Microphone               │
│  🔋 Battery                  │
│  📞 Phone                    │
│  🎮 Directional pad          │
│  👆 Fingerprint              │
│  📊 Virtual sensors          │
│  🐞 Bug report               │
│  ⚙️  Settings                │
│  ❓ Help                     │
│                              │
│  ►►► 📍 Location  ◄◄◄       │  ← CLIQUEZ ICI
│                              │
│  ⏹️  Close                   │
└──────────────────────────────┘
```

---

### ✅ Étape 3 : Définir la Position

Vous verrez une carte avec des champs. **3 Options** :

#### Option A - Recherche (Plus Facile) ⭐

```
┌────────────────────────────────────────┐
│  🔍 [Tunis, Tunisia]  🔎              │  ← Tapez ici
├────────────────────────────────────────┤
│                                        │
│  📍 Résultats :                        │
│  → Tunis, Tunisia                      │  ← Cliquez
│                                        │
│          [CARTE GOOGLE MAPS]           │
│                                        │
│  Latitude  : [36.8065]                │
│  Longitude : [10.1815]                │
│                                        │
│         [SET LOCATION]  ← CLIQUEZ     │
└────────────────────────────────────────┘
```

#### Option B - Coordonnées GPS

```
Latitude  : 36.8065   ← Tapez
Longitude : 10.1815   ← Tapez
             ↓
    [SET LOCATION]    ← Cliquez
```

#### Option C - Commande Terminal (Plus Rapide) ⚡

Ouvrez un terminal (CMD) et tapez :

```bash
adb emu geo fix 10.1815 36.8065
```

✅ **C'est tout !** Position définie instantanément.

---

## 🔄 Retour dans Karhebti

Après avoir défini la position :

```
1. Retournez dans l'application Karhebti
2. Ouvrez l'écran SOS
3. ✅ Vous devriez voir :

┌────────────────────────────────┐
│  🔵 Position GPS actuelle      │  ← Indicateur bleu
├────────────────────────────────┤
│                                │
│      [CARTE OPENSTREETMAP]     │
│           📍 Marqueur          │
│        (Tunis centre)          │
│                                │
├────────────────────────────────┤
│ 📍 Lat: 36.8065, Lon: 10.1815 │  ← Coordonnées
│                         🔄     │  ← Rafraîchir
└────────────────────────────────┘
```

---

## 🏙️ Positions des Villes Tunisiennes

Copiez-collez ces commandes selon votre besoin :

### Tunis (Centre-ville)
```bash
adb emu geo fix 10.1815 36.8065
```

### La Marsa
```bash
adb emu geo fix 10.3247 36.8781
```

### Sousse
```bash
adb emu geo fix 10.6369 35.8256
```

### Sfax
```bash
adb emu geo fix 10.7603 34.7406
```

### Carthage
```bash
adb emu geo fix 10.3233 36.8531
```

### Monastir
```bash
adb emu geo fix 10.8264 35.7775
```

### Hammamet
```bash
adb emu geo fix 10.6167 36.4000
```

---

## ❓ FAQ Rapide

### Q : Je ne trouve pas l'icône ⋮
**R :** Passez la souris sur le bord droit de l'émulateur. La barre d'outils apparaîtra.

### Q : La position ne change pas dans l'app
**R :** Cliquez sur l'icône **Rafraîchir** 🔄 en bas de la carte.

### Q : J'ai une erreur "Location is not available"
**R :** Vérifiez que le GPS est activé dans l'émulateur :
```
Paramètres émulateur > Localisation > Activer
```

### Q : Puis-je définir n'importe quelle position ?
**R :** Oui ! Vous pouvez :
- Chercher n'importe quelle adresse
- Entrer n'importe quelles coordonnées GPS
- Cliquer n'importe où sur la carte

### Q : La position reste après redémarrage émulateur ?
**R :** Non, vous devrez la redéfinir. Mais c'est très rapide avec la commande adb !

---

## 🎨 Alternative : Sélection Manuelle

Si vous ne voulez pas utiliser Extended Controls :

```
1. Ouvrez l'écran SOS (même avec position par défaut)
2. Touchez n'importe où sur la carte
3. L'indicateur passe en VIOLET
4. Le marqueur se déplace
5. ✅ Position choisie !

┌────────────────────────────────┐
│  🟣 Appuyez sur la carte pour  │  ← Indicateur violet
│     choisir votre position     │
├────────────────────────────────┤
│      [TOUCHEZ LA CARTE]        │
│           📍                   │
└────────────────────────────────┘
```

---

## 🚀 Méthode Ultra-Rapide

```bash
# Une seule commande !
adb emu geo fix 10.1815 36.8065 && echo Position definie: Tunis

# Ensuite :
# - Ouvrir Karhebti
# - Ouvrir SOS
# ✅ Terminé !
```

---

## ✅ Checklist

- [ ] Émulateur lancé
- [ ] Extended Controls ouvert (⋮)
- [ ] Location sélectionné
- [ ] Position définie (recherche/coordonnées/commande)
- [ ] "SET LOCATION" cliqué OU commande adb exécutée
- [ ] Retour dans Karhebti
- [ ] Écran SOS ouvert
- [ ] ✅ Position détectée !

---

## 🎉 Résultat Final

Après ces étapes, vous devriez voir :

```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃   SOS - Assistance routière    ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛

        🔴
       ⚠️ SOS

┌──────────────────────────────┐
│ 🔵 Position GPS actuelle      │
├──────────────────────────────┤
│                              │
│     [CARTE INTERACTIVE]      │
│           📍                 │
│                              │
│  Tunis, Tunisia              │
│                              │
├──────────────────────────────┤
│ 📍 Lat: 36.8065              │
│    Lon: 10.1815       🔄     │
└──────────────────────────────┘

Type de problème : [CHOISIR]  ▼

Description : _________________

[📷 Ajouter une photo]

[🚨 Envoyer la demande SOS]
```

---

**Problème résolu ! 🎊**

Si vous avez encore des questions, consultez :
- 📖 [LOCATION_TROUBLESHOOTING.md](./LOCATION_TROUBLESHOOTING.md) - Guide complet
- 📋 [GPS_FIX_SUMMARY.md](./GPS_FIX_SUMMARY.md) - Détails techniques

**Date :** 5 décembre 2025
