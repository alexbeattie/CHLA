# CHLA-iOS

SwiftUI iOS app for the CHLA Provider Map - Healthcare provider directory for LA County Regional Centers.

## 🚀 Quick Start

### Prerequisites

- **Xcode 15.0+** (Swift 5.9+)
- **iOS 17.0+** deployment target
- **macOS Sonoma 14.0+** for development

### Opening the Project

1. **Create Xcode Project** (first time only):

   ```bash
   cd /Users/alexbeattie/Developer/CHLA/chla-ios
   ```

2. Open Xcode and create a new iOS App project:
   - File → New → Project → iOS → App
   - Product Name: `CHLA-iOS`
   - Team: Your development team
   - Organization Identifier: `com.chla`
   - Interface: SwiftUI
   - Language: Swift
   - Save to: `/Users/alexbeattie/Developer/CHLA/chla-ios`

3. **Replace Generated Files**:
   - Delete the auto-generated ContentView.swift and App file
   - Drag the `CHLA-iOS` folder contents into Xcode
   - Make sure "Copy items if needed" is **unchecked**
   - Make sure "Create groups" is selected

4. **Build & Run**:
   - Select an iOS Simulator (iPhone 15 Pro recommended)
   - Press `Cmd+R` to build and run

## 📁 Project Structure

```
chla-ios/
├── CHLA-iOS/
│   ├── App/
│   │   ├── CHLA_iOSApp.swift      # App entry point
│   │   └── ContentView.swift       # Main content view
│   ├── Models/
│   │   ├── Provider.swift          # Provider data model
│   │   ├── RegionalCenter.swift    # Regional center model
│   │   └── APIModels.swift         # API response models
│   ├── Services/
│   │   ├── APIService.swift        # Django API client
│   │   └── LocationService.swift   # GPS & geocoding
│   ├── Stores/
│   │   ├── ProviderStore.swift     # Provider state management
│   │   └── RegionalCenterStore.swift
│   ├── Views/
│   │   ├── OnboardingView.swift    # Welcome flow
│   │   ├── MapContainerView.swift  # Map with markers
│   │   ├── ProviderListView.swift  # Provider list
│   │   ├── ProviderDetailView.swift
│   │   ├── RegionalCentersView.swift
│   │   └── SettingsView.swift
│   ├── Extensions/
│   │   └── Color+Extensions.swift  # Custom colors
│   ├── Utils/
│   │   └── Formatters.swift        # Utility formatters
│   └── Resources/
│       └── Info.plist              # App configuration
└── README.md
```

## 🔌 API Integration

The app connects to your Django backend:

| Environment | URL                             |
| ----------- | ------------------------------- |
| Development | `http://localhost:8000/api`     |
| Production  | `https://api.kinddhelp.com/api` |

### API Endpoints Used

- `GET /api/providers-v2/` - List providers
- `GET /api/providers-v2/nearby/` - Find nearby providers
- `GET /api/providers-v2/comprehensive_search/` - Search with filters
- `GET /api/regional-centers/` - List regional centers
- `GET /api/regional-centers/by_zip_code/` - Find by ZIP
- `GET /api/regional-centers/service_area_boundaries/` - GeoJSON boundaries
- `GET /health/` - Health check

### Switching Environments

In `APIService.swift`, the environment is automatically selected based on build configuration:

```swift
#if DEBUG
private let environment: Environment = .development
#else
private let environment: Environment = .production
#endif
```

For local development, make sure your Django server is running:

```bash
cd /Users/alexbeattie/Developer/CHLA/maplocation
source ../venv/bin/activate
python3 manage.py runserver
```

## ✨ Features

### 🗺️ Map View

- Interactive map with provider markers
- User location tracking
- Tap markers to view provider details
- Search and filter functionality

### 📋 Provider List

- Sortable list of providers
- Distance-based sorting
- Quick search
- Pull to refresh

### 🏢 Regional Centers

- Browse all LA County regional centers
- View service areas and ZIP codes
- Find providers by regional center

### ⚙️ Settings

- Search radius preferences
- Filter preferences
- API health check
- Reset onboarding

### 🚀 Onboarding

- Welcome flow for new users
- Location permission request
- Age group selection
- Diagnosis and therapy preferences

## 🎨 Design System

### Colors (defined in `Color+Extensions.swift`)

| Color            | Usage                        |
| ---------------- | ---------------------------- |
| `.accentBlue`    | Primary interactive elements |
| `.accentPurple`  | Secondary accents            |
| `.successGreen`  | Success states               |
| `.warningOrange` | Warnings                     |
| `.errorRed`      | Errors                       |

### Components

- `ProviderMapMarker` - Custom map pins
- `ProviderRowView` - List item
- `ActionButton` - Quick action buttons
- `TagView` - Service/diagnosis tags
- `FlowLayout` - Flexible tag layout
- `SelectionButton` - Toggle selection

## 🔧 Configuration

### Location Permissions

The app requests location access for finding nearby providers. Permissions are configured in `Info.plist`:

- `NSLocationWhenInUseUsageDescription`
- `NSLocationAlwaysAndWhenInUseUsageDescription`

### App Transport Security

For local development with `localhost`, you may need to add:

```xml
<key>NSAppTransportSecurity</key>
<dict>
    <key>NSAllowsLocalNetworking</key>
    <true/>
</dict>
```

## 📱 Testing

### Running on Simulator

1. Select a simulator from Xcode's device dropdown
2. Press `Cmd+R` to build and run
3. Use the Location simulation features (Features → Location)

### Running on Device

1. Connect your iPhone
2. Select it from the device dropdown
3. You'll need a valid Apple Developer account
4. Press `Cmd+R` to build and deploy

## 🐛 Debugging

### API Issues

1. Check that Django backend is running
2. Verify the API URL in `APIService.swift`
3. Check the Xcode console for network errors
4. Use the Settings → Check Connection feature

### Location Issues

1. Verify location permissions in iOS Settings
2. In Simulator, use Features → Location to simulate
3. Check for `LocationError` in console

## 📝 TODO

- [ ] Add push notifications for provider updates
- [ ] Implement favorites/bookmarks
- [ ] Add offline caching
- [ ] Add provider reviews
- [ ] Implement appointment scheduling links
- [ ] Add accessibility improvements (VoiceOver)
- [ ] Add unit tests
- [ ] Add UI tests

## 🔗 Related

- [Django Backend](../maplocation/) - API server
- [Vue Frontend](../map-frontend/) - Web app
- [Main Docs](../docs/) - Project documentation

## 📄 License

Private - CHLA Project
