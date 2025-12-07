# 🧪 Guide de Test - OCR Extraction Complète

## 🎯 Objectif
Tester que l'OCR extrait TOUTES les données du document et les affiche dans les champs modifiables.

## 📋 Checklist de Test

### ✅ Test 1: Scanner un Document

1. **Ouvrir l'application**
2. **Naviguer vers**: Documents → ➕ Ajouter → Scanner avec OCR
3. **Prendre une photo** d'un document (Assurance, Carte Grise, etc.)
4. **Attendre l'extraction** (~2-3 secondes)

### ✅ Test 2: Vérifier les Champs Extraits

Après le scan, vous devriez voir ces champs **automatiquement remplis**:

| Champ | Attendu | État |
|-------|---------|------|
| Type de document | Assurance/Carte Grise/etc. | ⬜ |
| Numéro de document | Ex: 123456789 | ⬜ |
| Nom du titulaire | Ex: MOSBEH Eya | ⬜ |
| Immatriculation | Ex: 123 TU 4567 | ⬜ |
| Date d'émission | Ex: 15/03/2024 | ⬜ |
| Date d'expiration | Ex: 15/03/2025 | ⬜ |

### ✅ Test 3: Modifier les Champs

1. **Cliquer sur chaque champ** et modifier la valeur
2. **Vérifier** que la modification est prise en compte

| Action | Résultat Attendu | État |
|--------|------------------|------|
| Changer le type | Dropdown s'ouvre | ⬜ |
| Modifier le numéro | Texte modifiable | ⬜ |
| Modifier le titulaire | Texte modifiable | ⬜ |
| Modifier l'immatriculation | Texte modifiable | ⬜ |
| Changer la date d'émission | DatePicker s'ouvre | ⬜ |
| Changer la date d'expiration | DatePicker s'ouvre | ⬜ |

### ✅ Test 4: Validation

1. **Sélectionner un véhicule** dans le dropdown
2. **Cliquer sur** "Confirmer et Enregistrer"
3. **Vérifier** que le document est créé avec succès

### ✅ Test 5: Cas Spécial - Carte Grise

1. **Scanner une Carte Grise**
2. **Vérifier** que:
   - Type = "Carte Grise"
   - Date d'expiration est **masquée** (ne s'affiche pas)
   - Les autres champs sont bien remplis

## 🔍 Logs à Vérifier

Dans Logcat, filtrer par `OCRExtraction`:

```
OCRExtraction: ✅ Données extraites: 
  Type=Assurance
  Numéro=123456789
  Titulaire=MOSBEH Eya
  Immat=123 TU 4567
```

## ⚠️ Problèmes Courants

### Problème 1: Champs vides
**Cause**: OCR n'a pas pu lire le texte
**Solution**: Reprendre une photo plus nette

### Problème 2: Données incorrectes
**Cause**: Erreur de reconnaissance OCR
**Solution**: Modifier manuellement les champs

### Problème 3: Date invalide
**Cause**: Format de date non reconnu
**Solution**: Sélectionner manuellement la date

## 📊 Résultats Attendus

| Métrique | Valeur Cible |
|----------|--------------|
| Champs extraits | 7/7 (100%) |
| Temps d'extraction | < 5 secondes |
| Précision OCR | > 80% |
| Champs modifiables | 7/7 (100%) |

## ✅ Critères de Réussite

- [ ] Tous les champs sont extraits (7/7)
- [ ] Tous les champs sont modifiables
- [ ] Les modifications sont sauvegardées
- [ ] Validation fonctionne correctement
- [ ] Carte Grise: date d'expiration masquée
- [ ] Document créé avec succès

## 🎉 Si Tout Fonctionne

**SUCCÈS!** 🎊 L'OCR extrait maintenant toutes les données et remplit automatiquement les champs!

**Gain de temps**: ~70% par rapport à la saisie manuelle complète.

---

**Date de Test**: _____________________
**Testeur**: _____________________
**Résultat**: ⬜ PASS  ⬜ FAIL
**Notes**: _____________________

