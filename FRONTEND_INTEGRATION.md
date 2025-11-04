# 🚗 Karhebti Backend API - Documentation Complète pour Intégration Frontend

## 📋 Vue d'ensemble

Backend REST complet pour application de gestion automobile développé avec **NestJS**, **TypeScript**, **MongoDB** et **JWT**.

**Base URL:** `http://localhost:3000`  
**Documentation Swagger:** `http://localhost:3000/api`

---

## 🔐 Authentification

Toutes les routes (sauf `/auth/signup` et `/auth/login`) nécessitent un token JWT dans le header:
```
Authorization: Bearer <access_token>
```

### Endpoints d'Authentification

#### POST `/auth/signup` - Inscription
**Request:**
```json
{
  "nom": "Dupont",
  "prenom": "Jean",
  "email": "jean.dupont@example.com",
  "motDePasse": "Password123!",
  "telephone": "0612345678"
}
```
**Response 201:**
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

#### POST `/auth/login` - Connexion
**Request:**
```json
{
  "email": "jean.dupont@example.com",
  "motDePasse": "Password123!"
}
```
**Response 200:** Identique à signup

#### POST `/auth/forgot-password` - Demande de réinitialisation
**Request:**
```json
{
  "email": "jean.dupont@example.com"
}
```
**Response 200:**
```json
{
  "message": "Si cet email existe, un lien de réinitialisation a été envoyé"
}
```

#### POST `/auth/reset-password` - Réinitialiser le mot de passe
**Request:**
```json
{
  "token": "reset-token",
  "nouveauMotDePasse": "NewPassword123!"
}
```
**Response 200:**
```json
{
  "message": "Mot de passe réinitialisé avec succès"
}
```

---

## 👤 Users (Routes Protégées)

### GET `/users` - Liste tous les utilisateurs (Admin uniquement)
**Response 200:**
```json
[{
  "_id": "507f1f77bcf86cd799439011",
  "nom": "Dupont",
  "prenom": "Jean",
  "email": "jean.dupont@example.com",
  "telephone": "0612345678",
  "role": "utilisateur"
}]
```

### GET `/users/:id` - Récupérer un utilisateur
**Response 200:**
```json
{
  "_id": "507f1f77bcf86cd799439011",
  "nom": "Dupont",
  "prenom": "Jean",
  "email": "jean.dupont@example.com",
  "telephone": "0612345678",
  "role": "utilisateur"
}
```

### POST `/users` - Créer un utilisateur (Admin uniquement)
**Request:**
```json
{
  "nom": "Martin",
  "prenom": "Sophie",
  "email": "sophie.martin@example.com",
  "motDePasse": "Password123!",
  "telephone": "0623456789",
  "role": "utilisateur"
}
```

### PATCH `/users/:id` - Modifier un utilisateur (soi-même ou Admin)
**Request:**
```json
{
  "telephone": "0634567890",
  "prenom": "Jean-Pierre"
}
```

### DELETE `/users/:id` - Supprimer un utilisateur (Admin uniquement)

### PATCH `/users/:id/role` - Modifier le rôle (Admin uniquement)
**Request:**
```json
{
  "role": "admin"
}
```

---

## 🚗 Cars (Routes Protégées)

Les utilisateurs ne voient que leurs propres voitures. Les admins voient tout.

### POST `/cars` - Créer une voiture
**Request:**
```json
{
  "marque": "Peugeot",
  "modele": "208",
  "annee": 2020,
  "immatriculation": "AB-123-CD",
  "typeCarburant": "Essence"
}
```
**Response 201:**
```json
{
  "_id": "507f1f77bcf86cd799439012",
  "marque": "Peugeot",
  "modele": "208",
  "annee": 2020,
  "immatriculation": "AB-123-CD",
  "typeCarburant": "Essence",
  "user": "507f1f77bcf86cd799439011",
  "createdAt": "2024-01-15T10:00:00.000Z",
  "updatedAt": "2024-01-15T10:00:00.000Z"
}
```

### GET `/cars` - Mes voitures
**Response 200:**
```json
[{
  "_id": "507f1f77bcf86cd799439012",
  "marque": "Peugeot",
  "modele": "208",
  "annee": 2020,
  "immatriculation": "AB-123-CD",
  "typeCarburant": "Essence",
  "user": {
    "_id": "507f1f77bcf86cd799439011",
    "nom": "Dupont",
    "prenom": "Jean",
    "email": "jean.dupont@example.com"
  }
}]
```

### GET `/cars/:id` - Détails d'une voiture
**Response 200:** Objet voiture avec populate du user

### PATCH `/cars/:id` - Modifier une voiture
**Request:**
```json
{
  "typeCarburant": "Diesel",
  "annee": 2021
}
```

### DELETE `/cars/:id` - Supprimer une voiture
**Response 200:** Confirmation de suppression

---

## 🔧 Maintenances (Routes Protégées)

### POST `/maintenances` - Créer un entretien
**Request:**
```json
{
  "type": "vidange",
  "date": "2024-01-15T10:00:00.000Z",
  "cout": 150.50,
  "garage": "507f1f77bcf86cd799439013",
  "voiture": "507f1f77bcf86cd799439012"
}
```
**Types possibles:** `"vidange"`, `"révision"`, `"réparation"`

**Response 201:**
```json
{
  "_id": "507f1f77bcf86cd799439014",
  "type": "vidange",
  "date": "2024-01-15T10:00:00.000Z",
  "cout": 150.50,
  "garage": "507f1f77bcf86cd799439013",
  "voiture": "507f1f77bcf86cd799439012"
}
```

### GET `/maintenances` - Liste des entretiens (filtrés par utilisateur)
**Response 200:**
```json
[{
  "_id": "507f1f77bcf86cd799439014",
  "type": "vidange",
  "date": "2024-01-15T10:00:00.000Z",
  "cout": 150.50,
  "garage": {
    "_id": "507f1f77bcf86cd799439013",
    "nom": "Garage Central",
    "adresse": "123 Rue de Paris"
  },
  "voiture": {
    "_id": "507f1f77bcf86cd799439012",
    "marque": "Peugeot",
    "modele": "208"
  }
}]
```

### GET `/maintenances/:id` - Détails d'un entretien
### PATCH `/maintenances/:id` - Modifier un entretien
### DELETE `/maintenances/:id` - Supprimer un entretien

---

## ⚙️ Parts (Routes Protégées)

### POST `/parts` - Créer une pièce
**Request:**
```json
{
  "nom": "Filtre à huile",
  "type": "Filtre",
  "dateInstallation": "2024-01-15T10:00:00.000Z",
  "kilometrageRecommande": 15000,
  "voiture": "507f1f77bcf86cd799439012"
}
```

**Response 201:**
```json
{
  "_id": "507f1f77bcf86cd799439015",
  "nom": "Filtre à huile",
  "type": "Filtre",
  "dateInstallation": "2024-01-15T10:00:00.000Z",
  "kilometrageRecommande": 15000,
  "voiture": "507f1f77bcf86cd799439012"
}
```

### GET `/parts` - Liste des pièces (filtrées par voitures de l'utilisateur)
**Response 200:** Array de pièces avec populate de `voiture`

### GET `/parts/:id` - Détails d'une pièce
### PATCH `/parts/:id` - Modifier une pièce
### DELETE `/parts/:id` - Supprimer une pièce

---

## 🔄 Replacement History (Routes Protégées)

### POST `/replacement-history` - Créer un historique
**Request:**
```json
{
  "date": "2024-01-15T10:00:00.000Z",
  "cout": 45.99,
  "fournisseur": "AutoParts Inc.",
  "remarque": "Pièce de qualité supérieure",
  "piece": "507f1f77bcf86cd799439015"
}
```

**Response 201:**
```json
{
  "_id": "507f1f77bcf86cd799439020",
  "date": "2024-01-15T10:00:00.000Z",
  "cout": 45.99,
  "fournisseur": "AutoParts Inc.",
  "remarque": "Pièce de qualité supérieure",
  "piece": "507f1f77bcf86cd799439015"
}
```

### GET `/replacement-history` - Liste des historiques
**Response 200:** Array avec populate de `piece`

### GET `/replacement-history/:id` - Détails d'un historique
### PATCH `/replacement-history/:id` - Modifier un historique
### DELETE `/replacement-history/:id` - Supprimer un historique

---

## 📄 Documents (Routes Protégées)

### POST `/documents` - Créer un document
**Request:**
```json
{
  "type": "assurance",
  "dateEmission": "2024-01-01T00:00:00.000Z",
  "dateExpiration": "2025-01-01T00:00:00.000Z",
  "fichier": "https://storage.example.com/documents/assurance.pdf",
  "voiture": "507f1f77bcf86cd799439012"
}
```
**Types possibles:** `"assurance"`, `"carte grise"`, `"contrôle technique"`

**Response 201:**
```json
{
  "_id": "507f1f77bcf86cd799439016",
  "type": "assurance",
  "dateEmission": "2024-01-01T00:00:00.000Z",
  "dateExpiration": "2025-01-01T00:00:00.000Z",
  "fichier": "https://storage.example.com/documents/assurance.pdf",
  "voiture": "507f1f77bcf86cd799439012"
}
```

### GET `/documents` - Liste des documents (filtrée par voitures)
**Response 200:** Array avec populate de `voiture`

### GET `/documents/:id` - Détails d'un document
### PATCH `/documents/:id` - Modifier un document
### DELETE `/documents/:id` - Supprimer un document

---

## ⏰ Deadlines (Routes Protégées)

### POST `/deadlines` - Créer une échéance
**Request:**
```json
{
  "dateRappel": "2024-12-15T09:00:00.000Z",
  "typeNotification": "email",
  "etat": "envoyé",
  "document": "507f1f77bcf86cd799439016"
}
```
**États possibles:** `"envoyé"`, `"reçu"`

**Response 201:**
```json
{
  "_id": "507f1f77bcf86cd799439018",
  "dateRappel": "2024-12-15T09:00:00.000Z",
  "typeNotification": "email",
  "etat": "envoyé",
  "document": "507f1f77bcf86cd799439016"
}
```

### GET `/deadlines` - Liste des échéances
**Response 200:** Array avec populate de `document`

### GET `/deadlines/:id` - Détails d'une échéance
### PATCH `/deadlines/:id` - Modifier une échéance
### DELETE `/deadlines/:id` - Supprimer une échéance

---

## 🏢 Garages (Routes Protégées)

### POST `/garages` - Créer un garage (Admin uniquement)
**Request:**
```json
{
  "nom": "Garage Central",
  "adresse": "123 Rue de Paris, 75001 Paris",
  "typeService": ["vidange", "réparation", "contrôle technique"],
  "telephone": "0145678901",
  "noteUtilisateur": 4.5
}
```

**Response 201:**
```json
{
  "_id": "507f1f77bcf86cd799439013",
  "nom": "Garage Central",
  "adresse": "123 Rue de Paris, 75001 Paris",
  "typeService": ["vidange", "réparation", "contrôle technique"],
  "telephone": "0145678901",
  "noteUtilisateur": 4.5
}
```

### GET `/garages` - Liste tous les garages (accessible à tous)
**Response 200:**
```json
[{
  "_id": "507f1f77bcf86cd799439013",
  "nom": "Garage Central",
  "adresse": "123 Rue de Paris, 75001 Paris",
  "typeService": ["vidange", "réparation", "contrôle technique"],
  "telephone": "0145678901",
  "noteUtilisateur": 4.5
}]
```

### GET `/garages/:id` - Détails d'un garage
### PATCH `/garages/:id` - Modifier un garage (Admin uniquement)
### DELETE `/garages/:id` - Supprimer un garage (Admin uniquement)

---

## 🛠️ Services (Routes Protégées)

### POST `/services` - Créer un service (Admin uniquement)
**Request:**
```json
{
  "type": "vidange",
  "coutMoyen": 75.50,
  "dureeEstimee": 60,
  "garage": "507f1f77bcf86cd799439013"
}
```
**Types possibles:** `"vidange"`, `"contrôle technique"`, `"réparation pneu"`

**Response 201:**
```json
{
  "_id": "507f1f77bcf86cd799439019",
  "type": "vidange",
  "coutMoyen": 75.50,
  "dureeEstimee": 60,
  "garage": "507f1f77bcf86cd799439013"
}
```

### GET `/services` - Liste tous les services
**Response 200:** Array avec populate de `garage`

### GET `/services/:id` - Détails d'un service

### GET `/services/garage/:garageId` - Services d'un garage spécifique
**Response 200:**
```json
[{
  "_id": "507f1f77bcf86cd799439019",
  "type": "vidange",
  "coutMoyen": 75.50,
  "dureeEstimee": 60,
  "garage": {
    "_id": "507f1f77bcf86cd799439013",
    "nom": "Garage Central"
  }
}]
```

### PATCH `/services/:id` - Modifier un service (Admin uniquement)
### DELETE `/services/:id` - Supprimer un service (Admin uniquement)

---

## 🤖 AI Features (Routes Protégées)

### POST `/ai/report-road-issue` - Signaler une anomalie routière
**Request:**
```json
{
  "latitude": 48.8566,
  "longitude": 2.3522,
  "typeAnomalie": "nid de poule",
  "description": "Grande zone dangereuse"
}
```
**Response 201:**
```json
{
  "message": "Anomalie signalée avec succès",
  "roadIssue": {
    "_id": "507f1f77bcf86cd799439017",
    "latitude": 48.8566,
    "longitude": 2.3522,
    "typeAnomalie": "nid de poule",
    "description": "Grande zone dangereuse",
    "signalements": 1,
    "createdAt": "2024-01-15T10:00:00.000Z"
  }
}
```

### GET `/ai/danger-zones` - Récupérer les zones dangereuses
**Query Params:**
- `latitude` (optional): number - Centre de recherche
- `longitude` (optional): number - Centre de recherche
- `rayon` (optional): number - Rayon de recherche en km

**Exemple:** `GET /ai/danger-zones?latitude=48.8566&longitude=2.3522&rayon=10`

**Response 200:**
```json
[{
  "id": "507f1f77bcf86cd799439017",
  "type": "nid de poule",
  "description": "Grande zone dangereuse",
  "latitude": 48.8566,
  "longitude": 2.3522,
  "signalements": 15,
  "niveauDanger": "très élevé"
}]
```
**Niveaux de danger:** `"faible"` (1 signalement), `"moyen"` (2-4), `"élevé"` (5-9), `"très élevé"` (10+)

### POST `/ai/maintenance-recommendations` - Recommandations d'entretien personnalisées
**Request:**
```json
{
  "voitureId": "507f1f77bcf86cd799439012"
}
```

**Response 200:**
```json
{
  "voiture": {
    "marque": "Peugeot",
    "modele": "208",
    "annee": 2020,
    "age": 4
  },
  "recommandations": [
    {
      "type": "vidange",
      "priorite": "moyenne",
      "raison": "Vidange recommandée tous les 15 000 km ou 1 an",
      "estimationCout": 80,
      "delaiRecommande": "2 mois"
    },
    {
      "type": "contrôle technique",
      "priorite": "haute",
      "raison": "Contrôle technique obligatoire pour les véhicules de plus de 4 ans",
      "estimationCout": 75,
      "delaiRecommande": "1 mois"
    },
    {
      "type": "révision",
      "priorite": "haute",
      "raison": "Votre véhicule a plus de 5 ans, une révision complète est recommandée",
      "estimationCout": 250,
      "delaiRecommande": "1 mois"
    }
  ],
  "scoreEntretien": 60
}
```
**Priorités:** `"faible"`, `"moyenne"`, `"haute"`  
**Score d'entretien:** 0-100 (diminue avec l'âge du véhicule)

### GET `/ai/garage-recommendation` - Recommander des garages
**Query Params:**
- `typePanne` (optional): string - Type de service recherché
- `latitude` (optional): number - Position actuelle
- `longitude` (optional): number - Position actuelle
- `rayon` (optional): number - Rayon de recherche en km

**Exemple:** `GET /ai/garage-recommendation?typePanne=vidange&latitude=48.8566&longitude=2.3522&rayon=5`

**Response 200:**
```json
[{
  "id": "507f1f77bcf86cd799439013",
  "nom": "Garage Central",
  "adresse": "123 Rue de Paris, 75001 Paris",
  "telephone": "0145678901",
  "note": 4.5,
  "services": ["vidange", "réparation", "contrôle technique"],
  "distanceEstimee": "5 km",
  "recommande": true
}]
```
**Note:** Les garages sont triés par note décroissante. `recommande: true` si note >= 4.

---

## 🔒 Système de Permissions

### Utilisateur Standard (`role: "utilisateur"`)
**Peut:**
- ✅ Créer/modifier/supprimer ses propres voitures
- ✅ Créer/modifier/supprimer ses entretiens (pour ses voitures uniquement)
- ✅ Créer/modifier/supprimer ses pièces (pour ses voitures uniquement)
- ✅ Créer/modifier/supprimer ses documents (pour ses voitures uniquement)
- ✅ Créer/modifier/supprimer ses échéances
- ✅ Voir la liste complète des garages
- ✅ Voir la liste complète des services
- ✅ Utiliser tous les endpoints IA
- ✅ Voir et modifier son propre profil

**Ne peut pas:**
- ❌ Voir/modifier les données d'autres utilisateurs
- ❌ Accéder aux voitures d'autres utilisateurs
- ❌ Créer/modifier/supprimer des garages
- ❌ Créer/modifier/supprimer des services
- ❌ Gérer les rôles utilisateurs
- ❌ Voir la liste complète des utilisateurs

### Administrateur (`role: "admin"`)
**Peut tout faire** +
- ✅ Voir/modifier tous les utilisateurs (GET /users)
- ✅ Changer les rôles utilisateurs (PATCH /users/:id/role)
- ✅ Créer des utilisateurs (POST /users)
- ✅ Supprimer des utilisateurs (DELETE /users/:id)
- ✅ Créer/modifier/supprimer des garages (POST/PATCH/DELETE /garages)
- ✅ Créer/modifier/supprimer des services (POST/PATCH/DELETE /services)
- ✅ Voir toutes les voitures de tous les utilisateurs
- ✅ Voir tous les entretiens, pièces, documents

---

## ⚠️ Codes d'Erreur HTTP

### 400 Bad Request
Données de requête invalides (validation DTO échouée)
```json
{
  "statusCode": 400,
  "message": [
    "email must be an email",
    "motDePasse must be longer than or equal to 6 characters"
  ],
  "error": "Bad Request"
}
```

### 401 Unauthorized
Token JWT manquant, invalide ou expiré
```json
{
  "statusCode": 401,
  "message": "Unauthorized"
}
```

### 403 Forbidden
Accès refusé (permissions insuffisantes)
```json
{
  "statusCode": 403,
  "message": "Vous ne pouvez modifier que vos propres voitures",
  "error": "Forbidden"
}
```

### 404 Not Found
Ressource non trouvée
```json
{
  "statusCode": 404,
  "message": "Voiture non trouvée",
  "error": "Not Found"
}
```

### 409 Conflict
Conflit (ex: email déjà utilisé)
```json
{
  "statusCode": 409,
  "message": "Cet email est déjà utilisé",
  "error": "Conflict"
}
```

### 429 Too Many Requests
Rate limit dépassé (100 req/min)
```json
{
  "statusCode": 429,
  "message": "ThrottlerException: Too Many Requests"
}
```

### 500 Internal Server Error
Erreur serveur
```json
{
  "statusCode": 500,
  "message": "Internal server error"
}
```

---

## 🔑 Gestion du Token JWT

### Stockage du Token (Frontend)
**Recommandations:**
1. Stocker le `access_token` après login/signup
2. Stocker aussi les infos `user` (id, role, email, nom, prenom)
3. L'inclure dans toutes les requêtes sauf `/auth/signup` et `/auth/login`
4. Gérer l'expiration (token expire après **24 heures**)
5. Rediriger vers login si erreur 401
6. Supprimer le token au logout

**Exemple avec Axios (React/Vue):**
```javascript
// Après login/signup
localStorage.setItem('token', response.data.access_token);
localStorage.setItem('user', JSON.stringify(response.data.user));

// Configurer Axios pour toutes les requêtes
axios.defaults.headers.common['Authorization'] = `Bearer ${localStorage.getItem('token')}`;

// Ou intercepteur
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Gérer les erreurs 401
axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

**Exemple avec Fetch:**
```javascript
fetch('http://localhost:3000/cars', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('token')}`,
    'Content-Type': 'application/json'
  }
})
```

---

## 📦 Structure des Données MongoDB

Tous les documents MongoDB incluent automatiquement:
- `_id`: ObjectId MongoDB (string de 24 caractères)
- `createdAt`: Date de création (timestamp ISO)
- `updatedAt`: Date de dernière modification (timestamp ISO)

**Exemple de timestamps:**
```json
{
  "_id": "507f1f77bcf86cd799439011",
  "nom": "Dupont",
  "createdAt": "2024-01-15T10:00:00.000Z",
  "updatedAt": "2024-01-20T14:30:00.000Z"
}
```

---

## 🚀 Rate Limiting

**Limite:** 100 requêtes par minute par IP  
**Comportement:** Retourne HTTP 429 si dépassé  
**Reset:** Automatique après 1 minute

---

## 🎨 Suggestions d'UI Frontend

### Pages Recommandées

1. **🔐 Login/Signup** - Authentification
   - Formulaire login
   - Formulaire signup
   - Lien "Mot de passe oublié"
   - Validation en temps réel

2. **📊 Dashboard** - Vue d'ensemble
   - Résumé: nombre de voitures, prochains entretiens
   - Échéances proches (alertes)
   - Graphiques des dépenses
   - Dernières activités
   - Recommandations IA en un coup d'œil

3. **🚗 Mes Voitures** - Liste + CRUD
   - Liste en cartes ou tableau
   - Bouton "Ajouter une voiture"
   - Actions: voir détails, modifier, supprimer
   - Filtres et recherche

4. **🔍 Détails Voiture** - Vue complète
   - Informations principales
   - Liste des entretiens
   - Liste des pièces
   - Documents associés
   - Onglets pour navigation

5. **🔧 Entretiens** - Calendrier/Liste
   - Vue calendrier
   - Vue liste/tableau
   - Filtres par type, date, voiture
   - Statistiques de coûts
   - Bouton "Ajouter un entretien"

6. **📄 Documents** - Gestion des documents
   - Liste avec dates d'expiration
   - Alertes pour documents expirant bientôt
   - Upload de fichiers (à implémenter)
   - Filtres par type et voiture

7. **🏢 Garages** - Annuaire
   - Liste/cartes des garages
   - Recherche et filtres (par service, note)
   - Voir les services proposés
   - Bouton "Contacter" (tel)
   - Carte interactive (Google Maps)

8. **🤖 Recommandations IA** - Analyse intelligente
   - Sélectionner une voiture
   - Afficher recommandations personnalisées
   - Priorités visuelles (couleurs)
   - Estimer les coûts
   - Bouton "Planifier un entretien"

9. **🗺️ Carte des Dangers** - Visualisation
   - Carte interactive (Google Maps/Leaflet)
   - Marqueurs pour anomalies
   - Filtres par type d'anomalie
   - Niveau de danger (couleurs)
   - Formulaire de signalement

10. **👤 Profil** - Gestion utilisateur
    - Voir/modifier informations personnelles
    - Changer mot de passe
    - Préférences
    - Statistiques personnelles

11. **👨‍💼 Admin Panel** - Pour administrateurs
    - Gestion utilisateurs (liste, créer, modifier rôle)
    - Gestion garages (CRUD)
    - Gestion services (CRUD)
    - Statistiques globales
    - Logs d'activité

### Fonctionnalités UI Recommandées

**🔔 Notifications & Alertes:**
- Badge pour échéances proches (< 30 jours)
- Toast notifications pour actions réussies
- Alertes pour documents expirés
- Rappels d'entretien

**📊 Graphiques & Statistiques:**
- Graphique des coûts d'entretien par mois
- Répartition des dépenses par type
- Évolution du nombre d'entretiens
- Comparaison entre voitures

**🗺️ Cartes Interactives:**
- Carte des garages autour de moi
- Carte des zones dangereuses
- Distance calculée depuis position actuelle
- Directions vers le garage

**📱 Responsive Design:**
- Mobile-first approach
- Menu hamburger sur mobile
- Cartes empilables
- Tables scrollables

**🔍 Recherche & Filtres:**
- Barre de recherche globale
- Filtres par date, type, voiture
- Tri par colonnes (tableaux)
- Pagination

**📅 Calendrier:**
- Vue mensuelle des entretiens
- Codes couleur par type
- Click pour détails
- Ajouter entretien depuis calendrier

**💰 Suivi des Dépenses:**
- Coût total par voiture
- Coût moyen par entretien
- Prévisions basées sur IA
- Export en PDF/Excel

**🎨 Design System:**
- Couleurs cohérentes:
  - Primaire: Bleu (#2563eb)
  - Succès: Vert (#22c55e)
  - Alerte: Orange (#f59e0b)
  - Danger: Rouge (#ef4444)
- Icônes: Font Awesome, Heroicons, Material Icons
- Composants: Cards, Badges, Buttons, Modals

---

## 🔧 Exemples de Code Frontend

### Service API (TypeScript)

```typescript
// services/api.ts
import axios from 'axios';

const API_BASE_URL = 'http://localhost:3000';

// Intercepteur pour ajouter le token
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Intercepteur pour gérer les erreurs
axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth
export const authAPI = {
  signup: (data: any) => axios.post(`${API_BASE_URL}/auth/signup`, data),
  login: (data: any) => axios.post(`${API_BASE_URL}/auth/login`, data),
  forgotPassword: (email: string) => axios.post(`${API_BASE_URL}/auth/forgot-password`, { email }),
  resetPassword: (data: any) => axios.post(`${API_BASE_URL}/auth/reset-password`, data),
};

// Cars
export const carsAPI = {
  getAll: () => axios.get(`${API_BASE_URL}/cars`),
  getOne: (id: string) => axios.get(`${API_BASE_URL}/cars/${id}`),
  create: (data: any) => axios.post(`${API_BASE_URL}/cars`, data),
  update: (id: string, data: any) => axios.patch(`${API_BASE_URL}/cars/${id}`, data),
  delete: (id: string) => axios.delete(`${API_BASE_URL}/cars/${id}`),
};

// Maintenances
export const maintenancesAPI = {
  getAll: () => axios.get(`${API_BASE_URL}/maintenances`),
  getOne: (id: string) => axios.get(`${API_BASE_URL}/maintenances/${id}`),
  create: (data: any) => axios.post(`${API_BASE_URL}/maintenances`, data),
  update: (id: string, data: any) => axios.patch(`${API_BASE_URL}/maintenances/${id}`, data),
  delete: (id: string) => axios.delete(`${API_BASE_URL}/maintenances/${id}`),
};

// Garages
export const garagesAPI = {
  getAll: () => axios.get(`${API_BASE_URL}/garages`),
  getOne: (id: string) => axios.get(`${API_BASE_URL}/garages/${id}`),
  create: (data: any) => axios.post(`${API_BASE_URL}/garages`, data),
  update: (id: string, data: any) => axios.patch(`${API_BASE_URL}/garages/${id}`, data),
  delete: (id: string) => axios.delete(`${API_BASE_URL}/garages/${id}`),
};

// AI
export const aiAPI = {
  reportRoadIssue: (data: any) => axios.post(`${API_BASE_URL}/ai/report-road-issue`, data),
  getDangerZones: (params?: any) => axios.get(`${API_BASE_URL}/ai/danger-zones`, { params }),
  getMaintenanceRecommendations: (voitureId: string) => 
    axios.post(`${API_BASE_URL}/ai/maintenance-recommendations`, { voitureId }),
  getGarageRecommendations: (params?: any) => 
    axios.get(`${API_BASE_URL}/ai/garage-recommendation`, { params }),
};
```

### Context d'Authentification (React)

```typescript
// contexts/AuthContext.tsx
import { createContext, useState, useEffect } from 'react';
import { authAPI } from '../services/api';

interface User {
  id: string;
  email: string;
  nom: string;
  prenom: string;
  role: string;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (email: string, password: string) => Promise<void>;
  signup: (data: any) => Promise<void>;
  logout: () => void;
  isAdmin: boolean;
}

export const AuthContext = createContext<AuthContextType>(null!);

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);

  useEffect(() => {
    const storedToken = localStorage.getItem('token');
    const storedUser = localStorage.getItem('user');
    if (storedToken && storedUser) {
      setToken(storedToken);
      setUser(JSON.parse(storedUser));
    }
  }, []);

  const login = async (email: string, motDePasse: string) => {
    const response = await authAPI.login({ email, motDePasse });
    const { access_token, user } = response.data;
    
    localStorage.setItem('token', access_token);
    localStorage.setItem('user', JSON.stringify(user));
    
    setToken(access_token);
    setUser(user);
  };

  const signup = async (data: any) => {
    const response = await authAPI.signup(data);
    const { access_token, user } = response.data;
    
    localStorage.setItem('token', access_token);
    localStorage.setItem('user', JSON.stringify(user));
    
    setToken(access_token);
    setUser(user);
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
  };

  const isAdmin = user?.role === 'admin';

  return (
    <AuthContext.Provider value={{ user, token, login, signup, logout, isAdmin }}>
      {children}
    </AuthContext.Provider>
  );
};
```

---

## 📋 Checklist d'Implémentation Frontend

### Phase 1: Setup & Authentification
- [ ] Installer dépendances (axios, react-router-dom, etc.)
- [ ] Configurer routing
- [ ] Créer service API avec intercepteurs
- [ ] Implémenter Context d'authentification
- [ ] Page Login
- [ ] Page Signup
- [ ] Protected routes
- [ ] Forgot/Reset password

### Phase 2: CRUD Principal
- [ ] Dashboard avec statistiques
- [ ] Liste des voitures
- [ ] Formulaire créer/modifier voiture
- [ ] Détails voiture
- [ ] Liste des entretiens
- [ ] Formulaire créer/modifier entretien
- [ ] Liste des documents
- [ ] Gestion des échéances

### Phase 3: Fonctionnalités Avancées
- [ ] Annuaire des garages
- [ ] Carte interactive des garages
- [ ] Recommandations IA d'entretien
- [ ] Signalement anomalies routières
- [ ] Carte des zones dangereuses
- [ ] Graphiques et statistiques
- [ ] Notifications et alertes

### Phase 4: Admin & Finitions
- [ ] Panel admin (gestion users)
- [ ] Gestion garages (admin)
- [ ] Gestion services (admin)
- [ ] Profil utilisateur
- [ ] Responsive design
- [ ] Loading states
- [ ] Error handling
- [ ] Toast notifications

---

## 🎯 Points Clés pour l'IA Frontend

1. **Authentication First:** Toujours implémenter l'auth en premier, car tout le reste en dépend

2. **State Management:** Utiliser Context API ou Redux pour gérer user/token globalement

3. **Error Handling:** Gérer tous les codes d'erreur (400, 401, 403, 404, 409, 429, 500)

4. **Loading States:** Afficher des loaders pendant les requêtes API

5. **Validation:** Valider les formulaires avant envoi (même validation que backend)

6. **Permissions:** Afficher/cacher les fonctionnalités selon le rôle (admin vs user)

7. **UX:** 
   - Confirmations avant suppression
   - Messages de succès après actions
   - Feedback visuel immédiat

8. **Performance:**
   - Pagination pour grandes listes
   - Lazy loading des images
   - Cache les données quand possible

9. **Responsive:** Design mobile-first

10. **Accessibilité:** Labels, aria-labels, navigation au clavier

---

**Documentation Swagger interactive:** `http://localhost:3000/api`

**Base URL API:** `http://localhost:3000`

**Token JWT:** Expire après 24 heures

**Rate Limit:** 100 requêtes/minute
