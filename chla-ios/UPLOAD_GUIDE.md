# App Store Upload Guide

## Archive Ready!

The app has been archived and is ready for upload. The Xcode Organizer should have opened with your archive.

## Steps to Upload to App Store Connect

### Step 1: In Xcode Organizer

1. The archive window should be open (if not, go to **Window > Organizer** in Xcode)
2. Select the "NDD Resource Map" archive (dated today)
3. Click **"Distribute App"** button on the right

### Step 2: Select Distribution Method

1. Choose **"App Store Connect"**
2. Click **Next**

### Step 3: Select Destination

1. Choose **"Upload"** (to upload directly to App Store Connect)
2. Click **Next**

### Step 4: App Store Connect Distribution Options

1. Leave all options checked:
   - ☑️ Strip Swift symbols
   - ☑️ Upload your app's symbols
2. Click **Next**

### Step 5: Re-sign "NDD Resource Map"

1. Select **"Automatically manage signing"**
2. Your team (LR4SP9C264) should be selected
3. Xcode will create any necessary provisioning profiles automatically
4. Click **Next**

### Step 6: Review and Upload

1. Review the summary showing:
   - App: NDD Resource Map
   - Version: 1.0.0
   - Bundle ID: com.nddresources.map
2. Click **"Upload"**

### Step 7: Wait for Processing

- Upload typically takes 5-10 minutes
- You'll see a progress indicator
- When complete, you'll see "Upload Succeeded"

## After Upload

### In App Store Connect (https://appstoreconnect.apple.com)

1. **Create New App** (if not already created):
   - Click "+" → "New App"
   - Platform: iOS
   - Name: NDD Resource Map
   - Bundle ID: com.nddresources.map
   - SKU: ndd-resource-map-001

2. **Wait for Build Processing**:
   - Apple processes the build (15-30 minutes)
   - You'll receive an email when ready

3. **Add App Information**:
   - Use the metadata from `AppStoreMetadata.md`
   - Upload screenshots from `screenshots/` folder

4. **Submit for Review**:
   - Answer compliance questions (encryption: No)
   - Click "Submit for Review"

## Files Reference

| File | Purpose |
|------|---------|
| `build/CHLA-iOS.xcarchive` | The archived app |
| `screenshots/` | App Store screenshots |
| `AppStoreMetadata.md` | All metadata for App Store |

## Troubleshooting

### "No profiles for bundle ID"
- Xcode's automatic signing should create one
- If not, go to developer.apple.com and create an App Store distribution profile

### "Upload failed: App Store Connect operation error"
- Ensure you're logged into Xcode with your Apple ID
- Go to Xcode > Settings > Accounts

### "Build processing failed"
- Check email from Apple for specific errors
- Common issues: missing icons, invalid Info.plist entries

## Need Help?

- Apple Developer Documentation: https://developer.apple.com/documentation/xcode/distributing-your-app-for-beta-testing-and-releases
- App Store Review Guidelines: https://developer.apple.com/app-store/review/guidelines/




