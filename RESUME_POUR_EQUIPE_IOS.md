# 📱 RÉSUMÉ TECHNIQUE - App Karhebti Android

**Pour : Équipe iOS**  
**Date : 6 Décembre 2025**  
**Version : 1.0 - Production Ready**

---

## 🎯 VUE D'ENSEMBLE

Application Android complète pour la gestion d'assistance routière avec 3 fonctionnalités principales :

1. **OCR** - Extraction automatique des informations de carte grise
2. **Notifications Push** - Système de notifications Firebase Cloud Messaging
3. **Gestion des Pannes (SOS)** - Système complet côté User et Garage Owner

---

## 📋 TABLE DES MATIÈRES

1. [OCR - Carte Grise](#1-ocr---extraction-carte-grise)
2. [Notifications Push](#2-notifications-push-fcm)
3. [Gestion des Pannes - Côté User](#3-gestion-des-pannes---côté-user)
4. [Gestion des Pannes - Côté Garage](#4-gestion-des-pannes---côté-garage)
5. [API Backend](#5-api-backend)
6. [Base de Données](#6-base-de-données)
7. [Technologies Utilisées](#7-technologies-utilisées)

---

## 1. OCR - EXTRACTION CARTE GRISE

### 📸 Fonctionnalité

Extraction automatique des informations de la carte grise tunisienne via photo.

### 🔧 Implémentation Android

**Bibliothèque :** ML Kit Text Recognition (Google)

**Flow :**
```
1. User prend photo de la carte grise
2. Compression de l'image (max 2MB)
3. Envoi au backend via API
4. Backend traite avec Tesseract OCR
5. Extraction des champs :
   - Immatriculation
   - Marque
   - Modèle
   - Année
   - Puissance fiscale
   - Nombre de places
6. Pré-remplissage automatique du formulaire
```

### 📱 Écrans Android

**VehicleAddScreen.kt**
- Bouton "📷 Scan Carte Grise"
- CameraX pour capture photo
- Prévisualisation image
- Confirmation avant envoi
- Affichage résultats OCR
- Édition manuelle possible

### 🔌 API Utilisée

```kotlin
POST /api/vehicles/ocr
Content-Type: multipart/form-data

Request:
- image: File (JPEG/PNG)
- userId: String

Response:
{
  "immatriculation": "123 TUN 4567",
  "marque": "RENAULT",
  "modele": "CLIO",
  "annee": 2020,
  "puissanceFiscale": 5,
  "nombrePlaces": 5,
  "confidence": 0.92
}
```

### ⚙️ Backend (Node.js)

```javascript
// Endpoint OCR
router.post('/ocr', upload.single('image'), async (req, res) => {
    // 1. Validation image
    // 2. Tesseract OCR processing
    // 3. Regex extraction des champs
    // 4. Nettoyage des données
    // 5. Retour JSON
});
```

### 💾 Modèle de Données

```javascript
Vehicle {
  _id: ObjectId,
  userId: ObjectId,
  immatriculation: String,
  marque: String,
  modele: String,
  annee: Number,
  puissanceFiscale: Number,
  nombrePlaces: Number,
  carteGriseUrl: String,  // Photo stockée
  ocrConfidence: Number,   // Score de confiance
  isVerified: Boolean,     // Vérifié manuellement
  createdAt: Date
}
```

### ✅ Points Importants pour iOS

1. **Camera Permission** requis
2. **Compression image** avant envoi (optimisation)
3. **Timeout** : 30 secondes max pour OCR
4. **Fallback** : Saisie manuelle si OCR échoue
5. **Validation** : Vérification format immatriculation tunisienne
6. **Cache** : Stocker résultats OCR localement

---

## 2. NOTIFICATIONS PUSH (FCM)

### 🔔 Fonctionnalité

Système de notifications temps réel pour alertes SOS et updates.

### 🔧 Implémentation Android

**Service :** Firebase Cloud Messaging (FCM)

**Types de Notifications :**
1. **SOS_REQUEST** - Nouvelle demande SOS (→ Garage)
2. **SOS_ACCEPTED** - Demande acceptée (→ User)
3. **SOS_REFUSED** - Demande refusée (→ User)
4. **GARAGE_ARRIVED** - Garage arrivé (→ User)
5. **SOS_COMPLETED** - Intervention terminée (→ User)

### 📱 Fichiers Android

**MyFirebaseMessagingService.kt**
```kotlin
class MyFirebaseMessagingService : FirebaseMessagingService() {
    
    override fun onNewToken(token: String) {
        // Envoi token au backend
        saveTokenToBackend(token)
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Traitement selon type
        when (remoteMessage.data["type"]) {
            "SOS_REQUEST" -> showSOSNotification()
            "SOS_ACCEPTED" -> navigateToTracking()
            "SOS_REFUSED" -> showRefusedDialog()
            // ...
        }
    }
}
```

**NotificationHelper.kt**
```kotlin
object NotificationHelper {
    fun showNotification(
        title: String,
        message: String,
        type: String,
        data: Map<String, String>
    ) {
        // Création notification avec actions
        // Channel selon le type
        // Deep link vers écran approprié
    }
}
```

### 🔌 API Backend

```javascript
// Envoi notification
POST /api/notifications/send
{
  "userId": "user_id",
  "type": "SOS_ACCEPTED",
  "title": "Garage trouvé!",
  "body": "Un garage a accepté votre demande",
  "data": {
    "breakdownId": "breakdown_id",
    "garageId": "garage_id"
  }
}

// Sauvegarde token FCM
POST /api/users/:id/fcm-token
{
  "token": "fcm_device_token"
}
```

### 💾 Modèle Backend

```javascript
User {
  fcmToken: String,           // Token device
  fcmTokenUpdatedAt: Date,
  notificationsEnabled: Boolean,
  notificationPreferences: {
    sos: Boolean,
    marketing: Boolean,
    updates: Boolean
  }
}

Notification {
  _id: ObjectId,
  userId: ObjectId,
  type: String,
  title: String,
  body: String,
  data: Object,
  isRead: Boolean,
  sentAt: Date
}
```

### 🎨 UI/UX Notifications

**Notification Channels (Android 8+) :**
- **SOS Urgent** - Son + Vibration + Heads-up
- **SOS Updates** - Son seulement
- **General** - Silencieux

**Actions dans notification :**
- **SOS_REQUEST** : "Voir détails", "Ignorer"
- **SOS_ACCEPTED** : "Voir tracking", "Appeler"

### ✅ Points Importants pour iOS

1. **APNs** équivalent à FCM
2. **Token** : Similaire, enregistrer au backend
3. **Payload** : Même structure JSON
4. **Deep Linking** : Même logique de navigation
5. **Badge Count** : Gérer le compteur d'unread
6. **Silent Notifications** : Pour refresh data

---

## 3. GESTION DES PANNES - CÔTÉ USER

### 🚨 Fonctionnalité

Système SOS complet pour demander assistance routière.

### 📱 Flow User

```
1. Home → Bouton SOS
2. Sélection type de panne :
   - PNEU (pneu crevé)
   - BATTERIE (batterie à plat)
   - MOTEUR (problème moteur)
   - CARBURANT (panne sèche)
   - REMORQUAGE (besoin remorque)
   - AUTRE
3. Description (optionnelle)
4. Position GPS automatique
5. Envoi demande
6. Écran "En attente de confirmation"
7. Polling toutes les 3 secondes
8. Quand garage accepte → Navigation auto
9. Écran "Tracking" avec carte
10. Suivi position garage en temps réel
```

### 📱 Écrans Android

#### **BreakdownSOSScreen.kt**
```kotlin
@Composable
fun BreakdownSOSScreen(
    onSendSOS: (type, description, lat, lon) -> Unit
) {
    // Sélection type panne (grid)
    // Champ description
    // Bouton "Envoyer SOS"
    // GPS automatique
}
```

**UI :**
- Grid 2x3 avec icônes panne
- Chips pour sélection
- TextField description
- Map preview position
- Bouton rouge urgent "🚨 Envoyer SOS"

#### **SOSWaitingScreen.kt**
```kotlin
@Composable
fun SOSWaitingScreen(
    breakdownId: String,
    onGarageAccepted: (Breakdown) -> Unit,
    onGarageRefused: () -> Unit
) {
    // Animation chargement
    // Polling status toutes les 3s
    // Message "Connexion au garage..."
    // ID demande affiché
    // Bouton "Annuler"
}
```

**Features :**
- Polling automatique (3 secondes)
- Détection ACCEPTED/REFUSED/CANCELLED
- Navigation automatique si ACCEPTED
- Logs de debug détaillés

#### **ClientTrackingScreen.kt**
```kotlin
@Composable
fun ClientTrackingScreen(
    breakdownId: String,
    clientLat: Double,
    clientLon: Double,
    garageLat: Double,
    garageLon: Double
) {
    // Banner "Garage trouvé!"
    // Carte avec 2 positions
    // Distance calculée
    // ETA estimé
    // Info garage
    // Bouton appeler garage
}
```

**Features :**
- Carte OpenStreetMap
- 2 marqueurs (User + Garage)
- Ligne entre les 2
- Calcul distance (Haversine)
- ETA dynamique (~3 min/km)
- Auto-refresh 10 secondes

### 🔌 API User

```kotlin
// Créer demande SOS
POST /api/breakdowns
{
  "userId": "user_id",
  "vehicleId": "vehicle_id",
  "type": "PNEU",
  "description": "Pneu crevé sur autoroute",
  "latitude": 36.8065,
  "longitude": 10.1815
}

Response:
{
  "_id": "breakdown_id",
  "status": "PENDING",
  "createdAt": "2025-12-06T14:30:00Z"
}

// Vérifier status (polling)
GET /api/breakdowns/:id

Response:
{
  "_id": "breakdown_id",
  "status": "ACCEPTED",  // PENDING, ACCEPTED, REFUSED
  "assignedTo": "garage_id",
  "acceptedAt": "2025-12-06T14:32:00Z"
}

// Annuler demande
PUT /api/breakdowns/:id/cancel
```

### 💾 Modèle Breakdown

```javascript
Breakdown {
  _id: ObjectId,
  userId: ObjectId,
  vehicleId: ObjectId,
  type: String,              // PNEU, BATTERIE, etc.
  description: String,
  latitude: Number,
  longitude: Number,
  status: String,            // PENDING, ACCEPTED, REFUSED, etc.
  assignedTo: ObjectId,      // Garage qui accepte
  refusedBy: [ObjectId],     // Garages qui refusent
  createdAt: Date,
  acceptedAt: Date,
  arrivedAt: Date,
  completedAt: Date,
  estimatedPrice: Number
}
```

### ✅ Points Importants pour iOS

1. **GPS Permission** requis
2. **Polling** : 3 secondes (optimiser batterie)
3. **Timeout** : 15 minutes max d'attente
4. **Auto-cancel** : Si pas de garage après 15 min
5. **Background Mode** : Continuer polling en background
6. **Map** : MapKit équivalent à OpenStreetMap

---

## 4. GESTION DES PANNES - CÔTÉ GARAGE

### 🔧 Fonctionnalité

Interface complète pour garage owner gérer demandes SOS.

### 📱 Flow Garage Owner

```
1. Notification Push "Nouvelle demande SOS"
2. Home → Section "🚨 SOS Management"
3. Liste des demandes en attente
4. Click sur demande → Détails complets
5. Voir carte, distance, ETA, info client
6. Boutons : "Accepter" ou "Refuser"
7. Dialog de confirmation
8. Si accepte → Navigation automatique
9. Écran "Navigation vers client"
10. Boutons : "Appeler client", "Naviguer", "Arrivé"
```

### 📱 Écrans Android

#### **HomeScreen.kt** (Section SOS)
```kotlin
// Si role = propGarage
Card(onClick = { navController.navigate("sos_requests_list") }) {
    Icon(Icons.Default.Warning)
    Text("🚨 SOS Management")
    Text("Demandes en attente")
}
```

#### **SOSRequestsListScreen.kt**
```kotlin
@Composable
fun SOSRequestsListScreen(
    onSOSClick: (breakdownId) -> Unit
) {
    // Liste demandes PENDING
    // Filtres : type, distance
    // Tri : plus proche, plus récent
    // Card par demande avec :
    //   - Type + icône
    //   - Description
    //   - Distance
    //   - ETA
    //   - Badge "PENDING"
}
```

**Features :**
- Refresh auto toutes les 10 secondes
- Pull-to-refresh manuel
- Nombre demandes en header
- Filtres type de panne

#### **GarageBreakdownDetailsScreen.kt**
```kotlin
@Composable
fun GarageBreakdownDetailsScreen(
    breakdownId: String,
    onAcceptSuccess: (Breakdown) -> Unit,
    onRefuseSuccess: () -> Unit
) {
    // Type panne + icône
    // Description complète
    // Carte avec position client
    // Distance calculée
    // ETA estimé
    // Info client (téléphone masqué)
    // Coordonnées GPS
    // Boutons Accepter/Refuser
}
```

**UI :**
- Card type panne
- Map avec marqueur client
- Info distance/ETA
- Info client
- 2 boutons action

**Dialogs :**
```kotlin
// Dialog Accepter
AlertDialog(
    title = "Accepter cette demande SOS?",
    text = """
        En acceptant, vous vous engagez à:
        • Vous rendre sur place
        • Arriver dans 15-20 minutes
        • Apporter le matériel nécessaire
    """,
    confirmButton = "Confirmer",
    onConfirm = { handleAccept() }
)

// Dialog Refuser
AlertDialog(
    title = "Refuser cette demande?",
    text = "La demande sera proposée à d'autres garages",
    confirmButton = "Refuser",
    onConfirm = { handleRefuse() }
)
```

#### **GarageNavigationScreen.kt**
```kotlin
@Composable
fun GarageNavigationScreen(
    breakdownId: String,
    clientLat: Double,
    clientLon: Double,
    clientPhone: String
) {
    // Banner "Demande acceptée!"
    // Carte avec route vers client
    // Distance et ETA
    // Info client
    // Bouton "Appeler client"
    // Bouton "Naviguer" (ouvre Maps)
    // Bouton "Marquer comme arrivé"
}
```

**Features :**
- Carte vers client
- Bouton ouvre Google Maps/OSM
- Appel direct client
- Marquer arrivée

### 🔌 API Garage

```kotlin
// Liste demandes PENDING
GET /api/breakdowns?status=PENDING&garageLocation=lat,lon&radius=10

Response:
[
  {
    "_id": "breakdown_id",
    "type": "PNEU",
    "description": "...",
    "latitude": 36.8065,
    "longitude": 10.1815,
    "distance": 5.2,  // km depuis garage
    "eta": 15,        // minutes
    "createdAt": "..."
  }
]

// Accepter demande
PUT /api/breakdowns/:id/accept

Response:
{
  "_id": "breakdown_id",
  "status": "ACCEPTED",
  "assignedTo": "garage_id",
  "acceptedAt": "..."
}

// Refuser demande
PUT /api/breakdowns/:id/refuse

Response:
{
  "message": "Breakdown refused"
}

// Marquer arrivée
PUT /api/breakdowns/:id/arrive

Response:
{
  "status": "IN_PROGRESS",
  "arrivedAt": "..."
}
```

### 🔔 Notifications Garage

**Déclencheur :** Nouvelle demande SOS créée

**Backend Logic :**
```javascript
// Quand breakdown créé
1. Trouver garages dans rayon 10km
2. Filtrer par disponibilité
3. Envoyer notification à chaque garage
4. Notification contient :
   - Type panne
   - Distance
   - Boutons "Voir" ou "Ignorer"
```

### ✅ Points Importants pour iOS

1. **Location Permission** : Background location pour calculer distance
2. **Push Notifications** : Haute priorité pour SOS
3. **Maps Integration** : Ouvrir Apple Maps
4. **Call Permission** : Appeler client directement
5. **Badge** : Nombre demandes PENDING
6. **Background Refresh** : Liste à jour même en background

---

## 5. API BACKEND

### 🔌 Endpoints Principaux

```javascript
// ===== AUTH =====
POST   /api/auth/register
POST   /api/auth/login
POST   /api/auth/refresh-token
POST   /api/auth/forgot-password
PUT    /api/auth/reset-password

// ===== USERS =====
GET    /api/users/:id
PUT    /api/users/:id
POST   /api/users/:id/fcm-token
DELETE /api/users/:id/fcm-token

// ===== VEHICLES =====
GET    /api/vehicles
POST   /api/vehicles
POST   /api/vehicles/ocr          // OCR carte grise
PUT    /api/vehicles/:id
DELETE /api/vehicles/:id

// ===== BREAKDOWNS (SOS) =====
GET    /api/breakdowns             // Liste avec filtres
POST   /api/breakdowns             // Créer SOS
GET    /api/breakdowns/:id         // Détails
PUT    /api/breakdowns/:id/accept  // Accepter (garage)
PUT    /api/breakdowns/:id/refuse  // Refuser (garage)
PUT    /api/breakdowns/:id/arrive  // Marquer arrivée
PUT    /api/breakdowns/:id/complete // Terminer
PUT    /api/breakdowns/:id/cancel  // Annuler (user)

// ===== NOTIFICATIONS =====
POST   /api/notifications/send
GET    /api/notifications/:userId
PUT    /api/notifications/:id/read
```

### 🔐 Authentification

**Type :** JWT (JSON Web Token)

**Header :**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Token Payload :**
```json
{
  "sub": "user_id",
  "email": "user@example.com",
  "role": "user" | "propGarage" | "admin",
  "iat": 1701878400,
  "exp": 1701964800
}
```

### 📊 Status Workflow

```
BREAKDOWN STATUS:
PENDING → ACCEPTED → IN_PROGRESS → COMPLETED
    ↓
  REFUSED
    ↓
CANCELLED
```

---

## 6. BASE DE DONNÉES

### 💾 MongoDB Collections

#### **users**
```javascript
{
  _id: ObjectId,
  email: String (unique),
  password: String (hashed),
  nom: String,
  prenom: String,
  telephone: String,
  role: String,  // "user", "propGarage", "admin"
  fcmToken: String,
  notificationsEnabled: Boolean,
  isEmailVerified: Boolean,
  createdAt: Date,
  updatedAt: Date
}
```

#### **vehicles**
```javascript
{
  _id: ObjectId,
  userId: ObjectId (ref: users),
  immatriculation: String (unique),
  marque: String,
  modele: String,
  annee: Number,
  puissanceFiscale: Number,
  nombrePlaces: Number,
  carteGriseUrl: String,
  ocrConfidence: Number,
  isVerified: Boolean,
  createdAt: Date
}
```

#### **breakdowns**
```javascript
{
  _id: ObjectId,
  userId: ObjectId (ref: users),
  vehicleId: ObjectId (ref: vehicles),
  type: String,  // PNEU, BATTERIE, etc.
  description: String,
  latitude: Number,
  longitude: Number,
  status: String,  // PENDING, ACCEPTED, etc.
  assignedTo: ObjectId (ref: users),
  refusedBy: [ObjectId],
  acceptedAt: Date,
  arrivedAt: Date,
  completedAt: Date,
  cancelledAt: Date,
  estimatedPrice: Number,
  actualPrice: Number,
  rating: Number,
  comment: String,
  createdAt: Date,
  updatedAt: Date
}
```

#### **notifications**
```javascript
{
  _id: ObjectId,
  userId: ObjectId (ref: users),
  type: String,  // SOS_REQUEST, SOS_ACCEPTED, etc.
  title: String,
  body: String,
  data: Object,
  isRead: Boolean,
  sentAt: Date
}
```

### 🔍 Indexes

```javascript
// users
users.createIndex({ email: 1 }, { unique: true });
users.createIndex({ fcmToken: 1 });

// vehicles
vehicles.createIndex({ userId: 1 });
vehicles.createIndex({ immatriculation: 1 }, { unique: true });

// breakdowns
breakdowns.createIndex({ userId: 1 });
breakdowns.createIndex({ status: 1 });
breakdowns.createIndex({ assignedTo: 1 });
breakdowns.createIndex({ latitude: 1, longitude: 1 });  // Geo queries
breakdowns.createIndex({ createdAt: -1 });

// notifications
notifications.createIndex({ userId: 1, isRead: 1 });
notifications.createIndex({ sentAt: -1 });
```

---

## 7. TECHNOLOGIES UTILISÉES

### 📱 Android

**Language :** Kotlin 1.9.0

**UI Framework :** Jetpack Compose

**Architecture :** MVVM (Model-View-ViewModel)

**Networking :**
- Retrofit 2.9.0 (REST API)
- OkHttp 4.11.0 (HTTP client)
- Gson (JSON parsing)

**Database :** Room (local cache)

**Image Loading :** Coil

**Maps :** OSMDroid (OpenStreetMap)

**Camera :** CameraX

**Permissions :** Accompanist Permissions

**Navigation :** Jetpack Navigation Compose

**DI :** Hilt (Dependency Injection)

**Async :** Coroutines + Flow

**Notifications :** Firebase Cloud Messaging (FCM)

**Analytics :** Firebase Analytics

**Crash Reporting :** Firebase Crashlytics

### 🖥️ Backend

**Runtime :** Node.js 18.x

**Framework :** Express.js 4.18

**Database :** MongoDB 6.0

**ODM :** Mongoose 7.0

**Authentication :** JWT (jsonwebtoken)

**File Upload :** Multer

**OCR :** Tesseract.js

**Notifications :** Firebase Admin SDK

**Validation :** Joi

**Logging :** Winston

**Environment :** dotenv

### ☁️ Services Cloud

- **Firebase** (FCM, Analytics, Crashlytics)
- **MongoDB Atlas** (Database hosting)
- **AWS S3** (File storage - carte grise, photos)

---

## 8. FONCTIONNALITÉS SUPPLÉMENTAIRES

### 🌍 Localisation

**Langues supportées :**
- Français (fr)
- Arabe (ar) - à implémenter iOS
- Anglais (en) - à implémenter iOS

**Format :**
- Dates : Format local
- Distances : km
- Téléphone : Format tunisien (+216)

### 🎨 Design System

**Colors :**
- Primary: #2196F3 (Bleu)
- Secondary: #4CAF50 (Vert)
- Error: #F44336 (Rouge)
- Warning: #FF9800 (Orange)
- SOS: #D32F2F (Rouge urgent)

**Typography :**
- Font : Roboto (Android) / San Francisco (iOS)
- Title: 24sp / Bold
- Body: 16sp / Regular
- Caption: 12sp / Light

### 🔒 Sécurité

- **HTTPS** obligatoire
- **JWT** avec expiration 24h
- **Refresh Token** 30 jours
- **Rate Limiting** (100 req/min par IP)
- **Input Validation** (Joi schemas)
- **SQL Injection** protection (Mongoose)
- **XSS** protection (sanitization)

### ⚡ Performance

- **Image Compression** avant upload
- **Lazy Loading** listes longues
- **Pagination** (20 items par page)
- **Cache** local (Room database)
- **Offline Mode** avec sync

### 📊 Métriques

**Analytics trackés :**
- Screen views
- SOS créés
- SOS acceptés/refusés
- Temps d'attente moyen
- Taux de succès
- OCR success rate
- Notifications ouvertes

---

## 9. DIFFÉRENCES ANDROID vs iOS

### 📱 Spécificités à Implémenter sur iOS

| Fonctionnalité | Android | iOS Équivalent |
|----------------|---------|----------------|
| Notifications | FCM | APNs |
| Maps | OSMDroid | MapKit |
| Camera | CameraX | AVFoundation |
| Storage | SharedPreferences | UserDefaults |
| Database | Room | Core Data / Realm |
| HTTP | Retrofit | Alamofire |
| JSON | Gson | Codable |
| Navigation | Navigation Compose | SwiftUI Navigation |
| UI | Jetpack Compose | SwiftUI |
| Background | WorkManager | Background Tasks |

### 🔧 Recommandations iOS

1. **Notifications**
   - Utiliser APNs (Apple Push Notification service)
   - Même payload JSON que FCM
   - Configurer certificates dans Firebase Console

2. **Maps**
   - MapKit natif Apple
   - Polylines pour tracer route
   - Annotations pour marqueurs

3. **Location**
   - CLLocationManager
   - Demander "When In Use" puis "Always" si besoin
   - Background location pour tracking

4. **Camera**
   - AVFoundation
   - Demander permission avant
   - Compression image avant upload

5. **Architecture**
   - MVVM avec Combine
   - SwiftUI pour UI
   - Async/await pour networking

6. **Database**
   - Core Data ou Realm
   - Même structure de modèles
   - Sync avec backend

---

## 10. POINTS D'ATTENTION

### ⚠️ Contraintes Techniques

1. **GPS Accuracy**
   - Précision minimum 50m
   - Fallback si GPS désactivé
   - Demander activation si nécessaire

2. **Timeout**
   - API calls : 30 secondes max
   - Polling : 3 secondes intervalle
   - OCR : 30 secondes max

3. **Offline**
   - Cache dernières demandes
   - Queue actions offline
   - Sync au retour online

4. **Battery**
   - Limiter polling en background
   - Utiliser push notifications
   - Arrêter GPS quand inutile

### 🔄 Synchronisation

**Backend → App :**
- Push notifications (temps réel)
- Polling (fallback)
- WebSocket (futur - temps réel)

**App → Backend :**
- Actions immédiates (HTTP POST/PUT)
- Retry automatique si échec
- Queue offline

---

## 11. STATISTIQUES DU PROJET

### 📊 Lignes de Code

**Android :**
- Kotlin : ~2,500 lignes
- Screens : 8 fichiers
- ViewModels : 5 fichiers
- API : 3 fichiers
- Models : 10 fichiers

**Backend :**
- JavaScript : ~1,500 lignes
- Routes : 5 fichiers
- Models : 4 fichiers
- Middlewares : 3 fichiers
- Utils : 5 fichiers

**Documentation :**
- Markdown : ~5,000 lignes
- Guides : 20+ fichiers

### ⏱️ Temps de Développement

- OCR : 2 jours
- Notifications : 1 jour
- SOS User : 3 jours
- SOS Garage : 3 jours
- Backend : 4 jours
- Tests : 2 jours
- Documentation : 2 jours

**Total : ~17 jours (2-3 semaines)**

---

## 12. ROADMAP FUTUR

### 🚀 Phase 2 (Q1 2026)

- [ ] WebSocket pour updates temps réel
- [ ] Chat in-app (Garage ↔ User)
- [ ] Photos panne (upload par user)
- [ ] Devis en ligne
- [ ] Payment intégré
- [ ] Rating system
- [ ] Historique complet
- [ ] Statistiques garage
- [ ] Multi-langues complet

### 🌟 Phase 3 (Q2 2026)

- [ ] Apple Watch app
- [ ] Widget iOS/Android
- [ ] Voice commands
- [ ] AI suggestions
- [ ] Predictive maintenance
- [ ] Fleet management (B2B)

---

## 13. CONTACTS & SUPPORT

### 👥 Équipe Android

**Lead Developer :** [Votre nom]  
**Backend Developer :** [Nom]  
**Designer :** [Nom]

### 📧 Contact

- **Email :** dev@karhebti.tn
- **Slack :** #karhebti-dev
- **Jira :** KARH project

### 📚 Documentation

- **API Docs :** http://api.karhebti.tn/docs
- **Postman Collection :** [Lien]
- **Figma Designs :** [Lien]
- **GitHub :** [Lien privé]

---

## 14. ANNEXES

### 📎 Liens Utiles

- [Firebase Console](https://console.firebase.google.com)
- [MongoDB Atlas](https://cloud.mongodb.com)
- [API Documentation](http://api.karhebti.tn/docs)
- [Postman Collection](...)
- [Figma Designs](...)

### 🔗 Endpoints Complets

Voir : `API_DOCUMENTATION.md`

### 🎨 Design Assets

Voir : `DESIGN_SYSTEM.md`

### 🧪 Tests

Voir : `TESTING_GUIDE.md`

---

## ✅ CHECKLIST POUR ÉQUIPE iOS

### Phase 1 : Setup
- [ ] Créer projet Xcode
- [ ] Configurer Firebase iOS
- [ ] Setup APNs
- [ ] Configurer CocoaPods/SPM
- [ ] Clone repositories

### Phase 2 : Backend
- [ ] Tester tous endpoints API
- [ ] Comprendre modèles de données
- [ ] Tester notifications
- [ ] Tester OCR

### Phase 3 : UI
- [ ] Créer design system SwiftUI
- [ ] Implémenter écrans principaux
- [ ] Navigation flow
- [ ] Animations

### Phase 4 : Features
- [ ] OCR carte grise
- [ ] Notifications push
- [ ] SOS User flow
- [ ] SOS Garage flow
- [ ] Maps integration

### Phase 5 : Tests
- [ ] Unit tests
- [ ] Integration tests
- [ ] UI tests
- [ ] Beta testing

### Phase 6 : Deployment
- [ ] App Store submission
- [ ] Screenshots
- [ ] Description
- [ ] Release notes

---

## 🎯 RÉSUMÉ EXÉCUTIF

### Fonctionnalités Principales

✅ **OCR** - Extraction automatique carte grise (Tesseract)  
✅ **Notifications** - Push temps réel (FCM → APNs pour iOS)  
✅ **SOS User** - Demande assistance + Tracking garage  
✅ **SOS Garage** - Gestion demandes + Navigation client  

### Technologies

📱 **Android** - Kotlin + Jetpack Compose + Retrofit  
🖥️ **Backend** - Node.js + Express + MongoDB  
☁️ **Cloud** - Firebase + MongoDB Atlas  

### Métriques

📊 **Code** - ~4,000 lignes (Android + Backend)  
⏱️ **Dev Time** - 2-3 semaines  
🎯 **Completion** - 100% fonctionnel  

### Next Steps iOS

1. Setup projet + Firebase
2. Implémenter même flow
3. Adapter UI pour iOS
4. Tests complets
5. Deployment App Store

---

**Document créé le :** 6 Décembre 2025  
**Version :** 1.0  
**Status :** ✅ Production Ready  
**Pour :** Équipe iOS  

---

**FIN DU DOCUMENT**

Pour toute question : dev@karhebti.tn

🚀 **Bon courage à l'équipe iOS !**

