# KINDD Android App

Native Android app for the KINDD (Kids in Need of Developmental Disabilities) Resource Navigator.

## Overview

KINDD is a free resource map developed by Children's Hospital Los Angeles to help families find ABA therapy and developmental disability services in Los Angeles County.

## Features

- **Interactive Map**: View provider locations on Google Maps
- **Resource Search**: Search and filter by therapy type, insurance, age groups
- **Regional Centers**: Find your Regional Center by ZIP code
- **AI Chat**: Get AI-powered assistance finding services
- **Bilingual**: English and Spanish support

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Networking**: Retrofit + OkHttp
- **Maps**: Google Maps SDK
- **Location**: FusedLocationProviderClient

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34 (Android 14)
- Google Maps API Key

## Setup

1. Clone the repository
2. Open in Android Studio
3. Create `secrets.properties` in the project root:
   ```
   MAPS_API_KEY=your_google_maps_api_key
   ```
4. Sync Gradle
5. Run on emulator or device

## Project Structure

```
app/src/main/java/com/chla/kindd/
├── ui/
│   ├── screens/          # Composable screens
│   ├── components/       # Reusable UI components
│   ├── navigation/       # Navigation setup
│   └── theme/            # Material 3 theming
├── data/
│   ├── api/              # Retrofit API interfaces
│   ├── models/           # Data classes
│   └── repository/       # Repository pattern
├── services/             # Location, LLM services
└── di/                   # Hilt modules
```

## API

The app connects to the Django backend at:
- **Development**: `http://10.0.2.2:8000/api` (Android emulator)
- **Production**: `https://api.kinddhelp.com/api`

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

## Testing

```bash
./gradlew test
./gradlew connectedAndroidTest
```

## Localization

Strings are defined in:
- `res/values/strings.xml` (English)
- `res/values-es/strings.xml` (Spanish)

## License

Copyright © Children's Hospital Los Angeles
