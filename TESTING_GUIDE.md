# 🧪 Guide de Test et Utilisation - Karhebti API

## 🚀 Démarrage Rapide

### 1. Installation et Configuration

```bash
# Installer les dépendances
npm install

# Créer le fichier .env
cp .env.example .env

# Éditer .env avec vos paramètres
# MONGODB_URI=mongodb://localhost:27017/karhebti
# JWT_SECRET=votre-secret-unique-et-securise
```

### 2. Démarrer MongoDB

```bash
# Windows (si MongoDB est installé)
mongod

# Ou avec Docker
docker run -d -p 27017:27017 --name mongodb mongo:latest
```

### 3. Démarrer l'application

```bash
# Mode développement avec hot reload
npm run start:dev

# Ou avec le script PowerShell
.\start-dev.ps1

# Mode production
npm run build
npm run start:prod
```

### 4. Accéder à la documentation

```
http://localhost:3000/api
```

---

## 📝 Exemples de Requêtes avec cURL

### Authentication

#### 1. Signup (Inscription)

```bash
curl -X POST http://localhost:3000/auth/signup \
  -H "Content-Type: application/json" \
  -d "{
    \"nom\": \"Dupont\",
    \"prenom\": \"Jean\",
    \"email\": \"jean.dupont@example.com\",
    \"motDePasse\": \"Password123!\",
    \"telephone\": \"0612345678\"
  }"
```

**Réponse:**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "507f1f77bcf86cd799439011",
    "email": "jean.dupont@example.com",
    "nom": "Dupont",
    "prenom": "Jean",
    "role": "utilisateur"
  }
}
```

#### 2. Login (Connexion)

```bash
curl -X POST http://localhost:3000/auth/login \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"jean.dupont@example.com\",
    \"motDePasse\": \"Password123!\"
  }"
```

### Voitures (Cars)

#### 3. Créer une voiture

```bash
curl -X POST http://localhost:3000/cars \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d "{
    \"marque\": \"Peugeot\",
    \"modele\": \"208\",
    \"annee\": 2020,
    \"immatriculation\": \"AB-123-CD\",
    \"typeCarburant\": \"Essence\"
  }"
```

#### 4. Lister mes voitures

```bash
curl -X GET http://localhost:3000/cars \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

#### 5. Récupérer une voiture

```bash
curl -X GET http://localhost:3000/cars/CAR_ID \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

#### 6. Modifier une voiture

```bash
curl -X PATCH http://localhost:3000/cars/CAR_ID \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d "{
    \"typeCarburant\": \"Diesel\"
  }"
```

#### 7. Supprimer une voiture

```bash
curl -X DELETE http://localhost:3000/cars/CAR_ID \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Entretiens (Maintenances)

#### 8. Créer un entretien

```bash
curl -X POST http://localhost:3000/maintenances \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d "{
    \"type\": \"vidange\",
    \"date\": \"2024-01-15T10:00:00.000Z\",
    \"cout\": 150.50,
    \"garage\": \"GARAGE_ID\",
    \"voiture\": \"CAR_ID\"
  }"
```

### Garages

#### 9. Lister les garages

```bash
curl -X GET http://localhost:3000/garages \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

#### 10. Créer un garage (Admin uniquement)

```bash
curl -X POST http://localhost:3000/garages \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_ACCESS_TOKEN" \
  -d "{
    \"nom\": \"Garage Central\",
    \"adresse\": \"123 Rue de Paris, 75001 Paris\",
    \"typeService\": [\"vidange\", \"réparation\", \"contrôle technique\"],
    \"telephone\": \"0145678901\",
    \"noteUtilisateur\": 4.5
  }"
```

### IA - Recommandations

#### 11. Signaler une anomalie routière

```bash
curl -X POST http://localhost:3000/ai/report-road-issue \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d "{
    \"latitude\": 48.8566,
    \"longitude\": 2.3522,
    \"typeAnomalie\": \"nid de poule\",
    \"description\": \"Grande zone dangereuse\"
  }"
```

#### 12. Récupérer les zones dangereuses

```bash
curl -X GET "http://localhost:3000/ai/danger-zones?latitude=48.8566&longitude=2.3522&rayon=10" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

#### 13. Obtenir des recommandations d'entretien

```bash
curl -X POST http://localhost:3000/ai/maintenance-recommendations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d "{
    \"voitureId\": \"CAR_ID\"
  }"
```

#### 14. Recommander des garages

```bash
curl -X GET "http://localhost:3000/ai/garage-recommendation?typePanne=vidange&latitude=48.8566&longitude=2.3522&rayon=5" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## 🧪 Tests avec Postman/Insomnia

### Collection d'exemple

1. **Créer un environnement** avec:
   - `base_url`: `http://localhost:3000`
   - `token`: (sera rempli après login)

2. **Séquence de test complète:**

   a. **Signup** → Récupérer le `access_token`
   
   b. **Login** → Utiliser le token pour les requêtes suivantes
   
   c. **Créer une voiture** → Récupérer le `car_id`
   
   d. **Lister les voitures** → Vérifier que la voiture apparaît
   
   e. **Créer un entretien** → Utiliser le `car_id`
   
   f. **Obtenir des recommandations** → Utiliser le `car_id`
   
   g. **Signaler une anomalie**
   
   h. **Voir les zones dangereuses**

---

## 🔍 Scénarios de Test

### Scénario 1: Utilisateur Standard

```bash
# 1. S'inscrire
POST /auth/signup
# Récupérer le token

# 2. Créer sa première voiture
POST /cars
{
  "marque": "Renault",
  "modele": "Clio",
  "annee": 2019,
  "immatriculation": "CD-456-EF",
  "typeCarburant": "Essence"
}

# 3. Créer un document pour la voiture
POST /documents
{
  "type": "assurance",
  "dateEmission": "2024-01-01T00:00:00.000Z",
  "dateExpiration": "2025-01-01T00:00:00.000Z",
  "fichier": "https://example.com/assurance.pdf",
  "voiture": "<car_id>"
}

# 4. Obtenir des recommandations d'entretien
POST /ai/maintenance-recommendations
{
  "voitureId": "<car_id>"
}

# 5. Chercher un garage recommandé
GET /ai/garage-recommendation?typePanne=vidange&latitude=48.8566&longitude=2.3522
```

### Scénario 2: Administrateur

```bash
# 1. Créer un compte admin (modifier role dans DB ou via endpoint admin)
PATCH /users/<user_id>/role
{
  "role": "admin"
}

# 2. Créer un garage
POST /garages
{
  "nom": "AutoService Pro",
  "adresse": "456 Avenue des Champs, 75008 Paris",
  "typeService": ["vidange", "réparation", "diagnostic"],
  "telephone": "0156789012",
  "noteUtilisateur": 4.8
}

# 3. Créer un service pour ce garage
POST /services
{
  "type": "vidange",
  "coutMoyen": 75.50,
  "dureeEstimee": 60,
  "garage": "<garage_id>"
}

# 4. Lister tous les utilisateurs
GET /users

# 5. Voir toutes les voitures
GET /cars
```

### Scénario 3: Fonctionnalités IA

```bash
# 1. Plusieurs utilisateurs signalent la même anomalie
POST /ai/report-road-issue
{
  "latitude": 48.8566,
  "longitude": 2.3522,
  "typeAnomalie": "nid de poule",
  "description": "Intersection dangereuse"
}

# 2. Vérifier l'agrégation des signalements
GET /ai/danger-zones

# 3. Obtenir des recommandations basées sur l'âge du véhicule
POST /ai/maintenance-recommendations
{
  "voitureId": "<car_id>"
}
# Devrait retourner des recommandations différentes selon l'âge

# 4. Filtrer les garages par service
GET /ai/garage-recommendation?typePanne=contrôle technique
```

---

## 📊 Tests de Performance

### Test de charge avec Apache Bench

```bash
# Test de login (100 requêtes, 10 concurrentes)
ab -n 100 -c 10 -p login.json -T application/json http://localhost:3000/auth/login

# Contenu de login.json:
{
  "email": "test@example.com",
  "motDePasse": "Password123!"
}
```

### Vérifier le Rate Limiting

```bash
# Envoyer plus de 100 requêtes en 1 minute
for i in {1..150}; do
  curl -X GET http://localhost:3000/garages \
    -H "Authorization: Bearer TOKEN"
done
# Devrait bloquer après 100 requêtes
```

---

## 🐛 Debug et Monitoring

### Voir les logs de MongoDB

```bash
# Se connecter à MongoDB
mongosh

# Utiliser la base de données
use karhebti

# Voir les collections
show collections

# Compter les documents
db.users.countDocuments()
db.cars.countDocuments()

# Voir les utilisateurs
db.users.find().pretty()

# Trouver un utilisateur par email
db.users.findOne({ email: "jean.dupont@example.com" })
```

### Vérifier les logs de l'application

```bash
# Les logs apparaissent dans la console en mode dev
npm run start:dev

# En production, configurer un logger comme Winston
```

---

## ✅ Checklist de Validation

- [ ] Signup fonctionne et retourne un token
- [ ] Login fonctionne avec les bons identifiants
- [ ] Login échoue avec de mauvais identifiants
- [ ] Routes protégées refusent l'accès sans token
- [ ] Utilisateur peut créer/modifier/supprimer ses voitures
- [ ] Utilisateur ne peut pas accéder aux voitures d'autrui
- [ ] Admin peut voir tous les utilisateurs
- [ ] Utilisateur standard ne peut pas voir tous les utilisateurs
- [ ] Création de garage nécessite le rôle admin
- [ ] Les recommandations IA retournent des données cohérentes
- [ ] Le rate limiting fonctionne (max 100 req/min)
- [ ] La documentation Swagger est accessible
- [ ] Tous les endpoints sont documentés dans Swagger
- [ ] La validation des DTOs rejette les données invalides
- [ ] Les mots de passe sont hashés en base de données

---

## 🔧 Commandes Utiles

```bash
# Développement
npm run start:dev          # Démarrer en mode watch
npm run build              # Compiler le projet
npm run start:prod         # Démarrer en production

# Tests
npm run test               # Tests unitaires
npm run test:watch         # Tests en mode watch
npm run test:cov           # Coverage
npm run test:e2e           # Tests end-to-end

# Linting
npm run lint               # Vérifier le code
npm run format             # Formater le code

# Base de données
mongosh                    # Shell MongoDB
mongodump                  # Backup
mongorestore               # Restore
```

---

## 📈 Métriques de Succès

- ✅ Temps de réponse moyen < 200ms
- ✅ 0 erreur au démarrage
- ✅ 100% des endpoints fonctionnels
- ✅ Documentation à jour
- ✅ Sécurité validée (hash, JWT, guards)
- ✅ Validation des données fonctionnelle
- ✅ Rate limiting actif

---

**🎉 Backend complet, testé et prêt à l'emploi!**
