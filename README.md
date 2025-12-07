# Karhebti Android - Vehicle Maintenance Management App

A modern Android application for managing vehicle maintenance, documents, and garage appointments with a beautiful Material Design 3 UI.

## 🎨 Design System

### Color Palette
- **Main Purple**: `#6658DD` - Primary brand color
- **Soft White**: `#FAFAFA` - Background color
- **Light Purple**: `#EBEAFE` - Secondary elements
- **Accent Green**: `#A4E8E0` - Success/Good status
- **Alert Red**: `#FE8596` - Urgent notifications
- **Accent Yellow**: `#FFD067` - Warning/Attention

### Typography
- **Headings**: Bold, 20-24sp (Roboto)
- **Subheadings**: Medium, 16-18sp
- **Body**: Regular, 14-16sp

### Design Principles
- All cards and buttons: 12-20dp rounded corners
- Subtle shadows for depth
- 16dp padding on all sides
- 12-20dp vertical spacing between elements

## 📱 Features

### Authentication
- ✅ Login Screen with email/password
- ✅ Sign Up Screen with validation
- ✅ Forgot Password functionality

### Dashboard
- ✅ Important Alerts section (maintenance reminders, fuel level)
- ✅ Quick Actions (Vehicles, Maintenance, Documents, Garages)
- ✅ Overview statistics

### Vehicle Management
- ✅ List all vehicles
- ✅ Vehicle status tracking (Good, Attention, Urgent)
- ✅ Mileage tracking
- ✅ Next maintenance reminders
- ✅ Add new vehicles

### Maintenance (Entretiens)
- ✅ Upcoming maintenance view
- ✅ Maintenance history
- ✅ Urgency levels (Normal, Attention, Urgent)
- ✅ Task lists for each maintenance
- ✅ Price tracking
- ✅ Garage location

### Documents
- ✅ Document upload and management
- ✅ Filter by type (Administrative, Maintenance)
- ✅ File size display
- ✅ Download functionality
- ✅ Vehicle association

### Garages
- ✅ Garage search
- ✅ Distance calculation
- ✅ Rating and reviews
- ✅ Opening hours
- ✅ Service filters
- ✅ Call and booking functionality

### Settings
- ✅ User profile management
- ✅ Notifications toggle
- ✅ Dark mode support
- ✅ Language selection
- ✅ Two-factor authentication
- ✅ Help center and support
- ✅ Logout functionality

## 🏗️ Architecture

### Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Navigation**: Jetpack Navigation Compose
- **Dependency Injection**: Manual (ready for Hilt/Dagger)
- **Backend**: MongoDB (localhost:27017/karhebti)
- **Networking**: Retrofit + OkHttp
- **Image Loading**: Coil

### Project Structure
```
app/
├── data/
│   ├── api/          # API configuration and services
│   └── model/        # Data models (Vehicle, Entretien, Document, Garage, User)
├── navigation/       # Navigation graph and routes
├── ui/
│   ├── screens/      # All screen composables
│   └── theme/        # Theme, colors, typography
└── MainActivity.kt   # App entry point
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- JDK 11 or later
- Android SDK 24+ (Nougat 7.0)
- MongoDB instance running on localhost:27017

### Installation

1. Clone the repository
```bash
git clone https://github.com/yourusername/karhebti-android.git
cd karhebti-android
```

2. Open the project in Android Studio

3. Sync Gradle dependencies
```bash
./gradlew build
```

4. Configure MongoDB backend
   - Ensure MongoDB is running on `localhost:27017`
   - Database name: `karhebti`
   - Update `ApiConfig.kt` if using a different host

5. Run the app
   - For emulator: The app uses `http://10.0.2.2:27017/`
   - For physical device: Update the BASE_URL in `ApiConfig.kt`

## 📦 Dependencies

```kotlin
// Core
androidx.core:core-ktx:1.17.0
androidx.lifecycle:lifecycle-runtime-ktx:2.9.4
androidx.activity:activity-compose:1.11.0

// Compose
androidx.compose:compose-bom:2024.09.00
androidx.compose.ui:ui
androidx.compose.material3:material3
androidx.compose.material:material-icons-extended

// Navigation
androidx.navigation:navigation-compose:2.8.0
androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4

// Networking
com.squareup.retrofit2:retrofit:2.9.0
com.squareup.retrofit2:converter-gson:2.9.0
com.squareup.okhttp3:okhttp:4.12.0
com.squareup.okhttp3:logging-interceptor:4.12.0

// Image Loading
io.coil-kt:coil-compose:2.5.0
```

## 🎯 Screens Overview

1. **LoginScreen** - User authentication with email/password
2. **SignUpScreen** - New user registration
3. **ForgotPasswordScreen** - Password reset functionality
4. **HomeScreen** - Dashboard with alerts and quick actions
5. **VehiclesScreen** - Vehicle management and listing
6. **EntretiensScreen** - Maintenance scheduler and history
7. **DocumentsScreen** - Document manager with filters
8. **GaragesScreen** - Garage finder with search and filters
9. **SettingsScreen** - User preferences and profile

## 🎨 Design Highlights

### Material Design 3
- Modern rounded corners on all UI elements
- Elevation and shadows for depth
- Consistent color scheme throughout
- Responsive touch targets (minimum 48dp)

### French Localization
- All UI text in French
- Date and time formatting for French locale
- Currency display in Euros (€)

### Status Indicators
- **Green** - Good/Success
- **Yellow** - Warning/Attention
- **Red** - Urgent/Error

## 🔧 Configuration

### API Configuration
Edit `app/src/main/java/com/example/karhebti_android/data/api/ApiConfig.kt`:

```kotlin
object ApiConfig {
    const val BASE_URL = "http://10.0.2.2:27017/" // For Emulator
    const val MONGODB_URL = "mongodb://localhost:27017/karhebti"
}
```

### Theme Customization
Edit `app/src/main/java/com/example/karhebti_android/ui/theme/Color.kt` to customize colors.

## 📝 Data Models

### Vehicle
- ID, name, brand, model, year
- Plate number, mileage
- Status (BON, ATTENTION, URGENT)
- Next maintenance info

### Entretien (Maintenance)
- ID, vehicle reference
- Type (VIDANGE, REVISION, PNEUS, etc.)
- Date, location, garage
- Price, urgency level
- Task list

### Document
- ID, vehicle reference
- Name, type, subtype
- Upload/expiry dates
- File URL and size

### Garage
- ID, name, address, coordinates
- Distance, rating, reviews
- Phone, opening hours
- Available services

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👨‍💻 Author

Karhebti Development Team

## 🙏 Acknowledgments

- Material Design 3 guidelines
- Jetpack Compose documentation
- Android development community

---

**Version**: 1.0.0  
**Last Updated**: November 2025

