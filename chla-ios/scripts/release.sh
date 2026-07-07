#!/bin/bash
# Archive and upload KiNDD to App Store Connect in one command.
#
# One-time setup:
#   1. appstoreconnect.apple.com -> Users and Access -> Integrations
#      -> App Store Connect API -> Team Keys -> Generate API Key
#      (role: App Manager or Admin)
#   2. Download AuthKey_<KEYID>.p8 and place it at:
#      ~/.appstoreconnect/private_keys/AuthKey_<KEYID>.p8
#      (xcodebuild searches this location automatically)
#   3. export ASC_KEY_ID=<KEYID> ASC_ISSUER_ID=<issuer uuid from same page>
#      (or pass as arguments: ./release.sh <KEYID> <ISSUER_ID>)
#
# VERSIONING: the marketing version must EXCEED every version already
# uploaded to App Store Connect (history includes 1.3.0). Check the
# Organizer archive list or App Store Connect before bumping. Version
# lives in CHLA-iOS/Resources/Info.plist (CFBundleShortVersionString /
# CFBundleVersion) AND in project.pbxproj (MARKETING_VERSION /
# CURRENT_PROJECT_VERSION for the widget) - keep all targets aligned.

set -euo pipefail

KEY_ID="${1:-${ASC_KEY_ID:-}}"
ISSUER_ID="${2:-${ASC_ISSUER_ID:-}}"

if [[ -z "$KEY_ID" || -z "$ISSUER_ID" ]]; then
    echo "Usage: $0 <ASC_KEY_ID> <ASC_ISSUER_ID>  (or set env vars)" >&2
    exit 1
fi

cd "$(dirname "$0")/.."

VERSION=$(defaults read "$(pwd)/CHLA-iOS/Resources/Info.plist" CFBundleShortVersionString)
BUILD=$(defaults read "$(pwd)/CHLA-iOS/Resources/Info.plist" CFBundleVersion)
ARCHIVE="$HOME/Library/Developer/Xcode/Archives/$(date +%Y-%m-%d)/KiNDD $VERSION ($BUILD).xcarchive"
EXPORT_PLIST=$(mktemp -t ExportOptions).plist

cat > "$EXPORT_PLIST" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>method</key>
	<string>app-store-connect</string>
	<key>destination</key>
	<string>upload</string>
	<key>teamID</key>
	<string>LR4SP9C264</string>
	<key>uploadSymbols</key>
	<true/>
	<key>manageAppVersionAndBuildNumber</key>
	<true/>
</dict>
</plist>
PLIST

echo "== Archiving KiNDD $VERSION ($BUILD)"
xcodebuild -project CHLA-iOS.xcodeproj -scheme CHLA-iOS \
    -destination 'generic/platform=iOS' \
    -archivePath "$ARCHIVE" archive -allowProvisioningUpdates

echo "== Uploading to App Store Connect"
xcodebuild -exportArchive \
    -archivePath "$ARCHIVE" \
    -exportOptionsPlist "$EXPORT_PLIST" \
    -allowProvisioningUpdates \
    -authenticationKeyID "$KEY_ID" \
    -authenticationKeyIssuerID "$ISSUER_ID"

echo "== Uploaded KiNDD $VERSION ($BUILD). Processing takes 5-15 min in App Store Connect."
