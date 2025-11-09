# 📚 Documentation Complète des Endpoints - Karhebti API

## Table des matières
1. [Authentication](#authentication)
2. [Users](#users)
3. [Cars](#cars)
4. [Maintenances](#maintenances)
5. [Parts](#parts)
6. [Replacement History](#replacement-history)
7. [Documents](#documents)
8. [Deadlines](#deadlines)
9. [Garages](#garages)
10. [Services](#services)
11. [AI Features](#ai-features)

---

## Authentication

### POST /auth/signup
Inscription d'un nouvel utilisateur

**Body:**
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

### POST /auth/login
Connexion

**Body:**
```json
{
  "email": "jean.dupont@example.com",
  "motDePasse": "Password123!"
}
```

**Response 200:**
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

### POST /auth/forgot-password
Demande de réinitialisation du mot de passe

**Body:**
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

### POST /auth/reset-password
Réinitialisation du mot de passe

**Body:**
```json
{
  "token": "reset-token-received-by-email",
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

## Users

**Note:** Toutes les routes nécessitent l'authentification (Bearer Token)

### GET /users
Liste tous les utilisateurs (Admin uniquement)

**Headers:**
```
Authorization: Bearer <access_token>
```

**Response 200:**
```json
[
  {
    "_id": "507f1f77bcf86cd799439011",
    "nom": "Dupont",
    "prenom": "Jean",
    "email": "jean.dupont@example.com",
    "telephone": "0612345678",
    "role": "utilisateur"
  }
]
```

### GET /users/:id
Récupérer un utilisateur spécifique

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

### POST /users
Créer un utilisateur (Admin uniquement)

**Body:**
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

### PATCH /users/:id
Modifier un utilisateur (soi-même ou Admin)

**Body:**
```json
{
  "telephone": "0634567890",
  "prenom": "Jean-Pierre"
}
```

### DELETE /users/:id
Supprimer un utilisateur (Admin uniquement)

### PATCH /users/:id/role
Modifier le rôle d'un utilisateur (Admin uniquement)

**Body:**
```json
{
  "role": "admin"
}
```

---

## Cars

### POST /cars
Créer une voiture

**Body:**
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
  "user": "507f1f77bcf86cd799439011"
}
```

### GET /cars
Lister toutes mes voitures (ou toutes si Admin)

**Response 200:**
```json
[
  {
    "_id": "507f1f77bcf86cd799439012",
    "marque": "Peugeot",
    "modele": "208",
    "annee": 2020,
    "immatriculation": "AB-123-CD",
    "typeCarburant": "Essence",
    "user": {
      "_id": "507f1f77bcf86cd799439011",
      "nom": "Dupont",
      "prenom": "Jean"
    }
  }
]
```

### GET /cars/:id
Récupérer une voiture spécifique

### PATCH /cars/:id
Modifier une voiture

**Body:**
```json
{
  "typeCarburant": "Diesel"
}
```

### DELETE /cars/:id
Supprimer une voiture

---

## Maintenances

### POST /maintenances
Créer un entretien

**Body:**
```json
{
  "type": "vidange",
  "date": "2024-01-15T10:00:00.000Z",
  "cout": 150.50,
  "garage": "507f1f77bcf86cd799439013",
  "voiture": "507f1f77bcf86cd799439012"
}
```

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

### GET /maintenances
Lister tous les entretiens (filtrés par utilisateur)

### GET /maintenances/:id
Récupérer un entretien spécifique

### PATCH /maintenances/:id
Modifier un entretien

### DELETE /maintenances/:id
Supprimer un entretien

---

## Parts

### POST /parts
Créer une pièce

**Body:**
```json
{
  "nom": "Filtre à huile",
  "type": "Filtre",
  "dateInstallation": "2024-01-15T10:00:00.000Z",
  "kilometrageRecommande": 15000,
  "voiture": "507f1f77bcf86cd799439012"
}
```

### GET /parts
Lister toutes les pièces

### GET /parts/:id
Récupérer une pièce spécifique

### PATCH /parts/:id
Modifier une pièce

### DELETE /parts/:id
Supprimer une pièce

---

## Replacement History

### POST /replacement-history
Créer un historique de remplacement

**Body:**
```json
{
  "date": "2024-01-15T10:00:00.000Z",
  "cout": 45.99,
  "fournisseur": "AutoParts Inc.",
  "remarque": "Pièce de qualité supérieure",
  "piece": "507f1f77bcf86cd799439015"
}
```

### GET /replacement-history
Lister tous les historiques

### GET /replacement-history/:id
Récupérer un historique spécifique

### PATCH /replacement-history/:id
Modifier un historique

### DELETE /replacement-history/:id
Supprimer un historique

---

## Documents

### POST /documents
Créer un document

**Body:**
```json
{
  "type": "assurance",
  "dateEmission": "2024-01-01T00:00:00.000Z",
  "dateExpiration": "2025-01-01T00:00:00.000Z",
  "fichier": "https://storage.example.com/documents/assurance.pdf",
  "voiture": "507f1f77bcf86cd799439012"
}
```

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

### GET /documents
Lister tous les documents

### GET /documents/:id
Récupérer un document spécifique

### PATCH /documents/:id
Modifier un document

### DELETE /documents/:id
Supprimer un document

---

## Deadlines

### POST /deadlines
Créer une échéance

**Body:**
```json
{
  "dateRappel": "2024-12-15T09:00:00.000Z",
  "typeNotification": "email",
  "etat": "envoyé",
  "document": "507f1f77bcf86cd799439016"
}
```

### GET /deadlines
Lister toutes les échéances

### GET /deadlines/:id
Récupérer une échéance spécifique

### PATCH /deadlines/:id
Modifier une échéance

### DELETE /deadlines/:id
Supprimer une échéance

---

## Garages

### POST /garages
Créer un garage (Admin uniquement)

**Body:**
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

### GET /garages
Lister tous les garages

### GET /garages/:id
Récupérer un garage spécifique

### PATCH /garages/:id
Modifier un garage (Admin uniquement)

### DELETE /garages/:id
Supprimer un garage (Admin uniquement)

---

## Services

### POST /services
Créer un service (Admin uniquement)

**Body:**
```json
{
  "type": "vidange",
  "coutMoyen": 75.50,
  "dureeEstimee": 60,
  "garage": "507f1f77bcf86cd799439013"
}
```

### GET /services
Lister tous les services

### GET /services/:id
Récupérer un service spécifique

### GET /services/garage/:garageId
Récupérer tous les services d'un garage

### PATCH /services/:id
Modifier un service (Admin uniquement)

### DELETE /services/:id
Supprimer un service (Admin uniquement)

---

## AI Features

### POST /ai/report-road-issue
Signaler une anomalie routière détectée

**Body:**
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
    "signalements": 1
  }
}
```

### GET /ai/danger-zones
Récupérer les zones dangereuses

**Query Params:**
- `latitude` (optional): Latitude du centre de recherche
- `longitude` (optional): Longitude du centre de recherche
- `rayon` (optional): Rayon de recherche en km

**Response 200:**
```json
[
  {
    "id": "507f1f77bcf86cd799439017",
    "type": "nid de poule",
    "description": "Grande zone dangereuse",
    "latitude": 48.8566,
    "longitude": 2.3522,
    "signalements": 15,
    "niveauDanger": "très élevé"
  }
]
```

### POST /ai/maintenance-recommendations
Obtenir des recommandations d'entretien personnalisées

**Body:**
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
    }
  ],
  "scoreEntretien": 60
}
```

### GET /ai/garage-recommendation
Recommander des garages

**Query Params:**
- `typePanne` (optional): Type de service recherché
- `latitude` (optional): Latitude de votre position
- `longitude` (optional): Longitude de votre position
- `rayon` (optional): Rayon de recherche en km

**Response 200:**
```json
[
  {
    "id": "507f1f77bcf86cd799439013",
    "nom": "Garage Central",
    "adresse": "123 Rue de Paris, 75001 Paris",
    "telephone": "0145678901",
    "note": 4.5,
    "services": ["vidange", "réparation", "contrôle technique"],
    "distanceEstimee": "5 km",
    "recommande": true
  }
]
```

---

## Codes d'erreur courants

- **400 Bad Request** - Données invalides
- **401 Unauthorized** - Token manquant ou invalide
- **403 Forbidden** - Accès refusé (permissions insuffisantes)
- **404 Not Found** - Ressource non trouvée
- **409 Conflict** - Conflit (ex: email déjà utilisé)
- **500 Internal Server Error** - Erreur serveur

---

**Note:** Tous les endpoints (sauf `/auth/signup` et `/auth/login`) nécessitent un token JWT dans le header:
```
Authorization: Bearer <your_access_token>
```
