# KiNDD Resources - App Store Submission Guide

Quick reference for submitting updates to the App Store.

---

## Quick Commands

### Build & Archive
```bash
cd /Users/alexbeattie/Developer/CHLA/chla-ios

# Build archive for App Store
xcodebuild -project CHLA-iOS.xcodeproj -scheme "CHLA-iOS" \
  -configuration Release \
  -destination "generic/platform=iOS" \
  -archivePath build/KiNDD-Resources.xcarchive \
  clean archive

# Open in Xcode Organizer
open build/KiNDD-Resources.xcarchive
```

### Capture Screenshots (iPhone 15 Pro Max - 6.9")
```bash
# Boot simulator
xcrun simctl boot "25A61AAD-C86D-4D85-ADD2-B0A8254D0D9C"

# Build and install
xcodebuild -project CHLA-iOS.xcodeproj -scheme "CHLA-iOS" \
  -destination "id=25A61AAD-C86D-4D85-ADD2-B0A8254D0D9C" build
xcrun simctl install "25A61AAD-C86D-4D85-ADD2-B0A8254D0D9C" \
  "build/Build/Products/Debug-iphonesimulator/NDD Resource Map.app"
xcrun simctl launch "25A61AAD-C86D-4D85-ADD2-B0A8254D0D9C" "com.nddresources.map"

# Capture screenshot
xcrun simctl io "25A61AAD-C86D-4D85-ADD2-B0A8254D0D9C" screenshot screenshot.png
```

### Remove Alpha Channel from Screenshots
```bash
# Screenshots with alpha get rejected!
for f in screenshots/*.png; do
  sips -s format jpeg "$f" --out "/tmp/temp.jpg"
  sips -s format png "/tmp/temp.jpg" --out "screenshots/appstore/$(basename $f)"
done
```

---

## App Store Connect Checklist

### App Information
| Field | Value |
|-------|-------|
| App Name | KiNDD Resources |
| Subtitle | Find Regional Center Help |
| Bundle ID | com.nddresources.map |
| SKU | ndd-resource-map-001 |
| Primary Category | Medical |
| Secondary Category | Navigation |
| Age Rating | 4+ |
| Copyright | © 2025 KiNDD Resources |

### URLs
| Field | URL |
|-------|-----|
| Privacy Policy | https://kinddhelp.com/privacy |
| Support URL | https://kinddhelp.com/about |
| Marketing URL | https://kinddhelp.com |

### Keywords (100 chars max)
```
regional center,disability,healthcare,resources,autism,developmental,services,LA county,NLACRC
```

### App Description
```
Find healthcare resources and service providers for individuals with neurodevelopmental disabilities across Los Angeles County Regional Centers.

FEATURES:
• Interactive map of 600+ verified resources
• Filter by Regional Center service area
• Turn-by-turn directions to providers
• Detailed provider information including services and contact details
• Search by ZIP code, city, or provider name
• Regional Center boundary visualization

REGIONAL CENTERS COVERED:
• North Los Angeles County (NLACRC)
• Eastern Los Angeles (ELARC)
• South Central Los Angeles (SCLARC)
• Westside (WRC)
• Frank D. Lanterman (FDLRC)
• Harbor (HRC)
• San Gabriel/Pomona (SGPRC)

Designed for families, caregivers, and professionals seeking developmental disability resources in Southern California.

Questions or feedback? Contact support@kinddhelp.com
```

### App Review Information
| Field | Value |
|-------|-------|
| First Name | Alex |
| Last Name | Beattie |
| Email | support@kinddhelp.com |
| Phone | (your phone) |
| Sign-in Required | No |
| Demo Username | notneeded |
| Demo Password | notneeded123 |
| Notes | This app does not require sign-in. All features are available without authentication. |

### App Privacy
- Collects: Location (not linked to user), Usage Data (not linked to user)
- Does not sell data
- Does not track users

### Pricing
- Free

### Export Compliance
- Uses encryption: No

---

## Screenshots

### Required Sizes
| Display | Size | Device |
|---------|------|--------|
| 6.9" | 1290 x 2796 | iPhone 15 Pro Max ✅ |
| 6.5" | Auto-scales from 6.9" | - |

### Screenshot Locations
- Raw: `/Users/alexbeattie/Developer/CHLA/chla-ios/screenshots/`
- App Store Ready (no alpha): `/Users/alexbeattie/Developer/CHLA/chla-ios/screenshots/appstore/`

### Recommended Screenshots
1. Map view with providers
2. Regional Centers map
3. Resource list
4. Provider detail sheet
5. Directions view

---

## Upload Process

### From Xcode Organizer
1. Open archive: `open build/KiNDD-Resources.xcarchive`
2. Click **"Distribute App"**
3. Select **"App Store Connect"** → Next
4. Select **"Upload"** → Next
5. Select **"Automatically manage signing"** → Next
6. Click **"Upload"**
7. Wait 15-30 min for processing

### In App Store Connect
1. Go to **App Store → iOS App → Version X.X**
2. Scroll to **"Build"** section
3. Click **"+"** to select uploaded build
4. Fill in "What's New" for updates
5. Click **"Add for Review"**
6. Click **"Submit to App Review"**

---

## TestFlight

### Internal Testing (instant, no review)
1. Go to **TestFlight** tab
2. Click **"+"** next to "Internal Testing"
3. Create group: "KiNDD testers"
4. Add testers by email
5. They get invite via TestFlight app

### External Testing (requires Beta App Review ~24hrs)
1. Go to **TestFlight** tab
2. Click **"+"** next to "External Testing"
3. Create group
4. Add test details
5. Submit for Beta App Review

---

## Version Updates

### Bump Version Number
Edit `Info.plist`:
```xml
<key>CFBundleShortVersionString</key>
<string>1.1.0</string>
<key>CFBundleVersion</key>
<string>2</string>
```

Or in Xcode: Target → General → Version & Build

### What's New Text (for updates)
```
• Bug fixes and performance improvements
• [Add specific features]
```

---

## Troubleshooting

### "No profiles for bundle ID"
- Use Xcode's automatic signing
- Or create profile at developer.apple.com

### Screenshots rejected
- Remove alpha channel (transparency)
- Use exact dimensions for device size
- PNG or JPEG format only

### Build processing stuck
- Wait up to 1 hour
- Check email for errors
- Try re-uploading

### App rejected
- Read rejection reason carefully
- Fix issues
- Resubmit (no need to re-upload build if code unchanged)

---

## File Locations

| File | Path |
|------|------|
| Xcode Project | `/Users/alexbeattie/Developer/CHLA/chla-ios/CHLA-iOS.xcodeproj` |
| Info.plist | `/Users/alexbeattie/Developer/CHLA/chla-ios/CHLA-iOS/Resources/Info.plist` |
| App Icon | `/Users/alexbeattie/Developer/CHLA/chla-ios/CHLA-iOS/Resources/Assets.xcassets/AppIcon.appiconset/` |
| Screenshots | `/Users/alexbeattie/Developer/CHLA/chla-ios/screenshots/` |
| Archive | `/Users/alexbeattie/Developer/CHLA/chla-ios/build/KiNDD-Resources.xcarchive` |
| This Guide | `/Users/alexbeattie/Developer/CHLA/chla-ios/APP_STORE_SUBMISSION_GUIDE.md` |

---

## Timeline

| Task | Duration |
|------|----------|
| Build archive | ~2 min |
| Upload to App Store Connect | ~5 min |
| Build processing | 15-30 min |
| App Review | 24-48 hours |
| TestFlight Beta Review | ~24 hours |

---

*Last updated: December 15, 2025*




