#!/bin/bash
# App Icon Generator for NDD Resource Map
# This script creates a 1024x1024 app icon for the App Store

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "${SCRIPT_DIR}")"
ICON_DIR="${PROJECT_ROOT}/CHLA-iOS/Resources/Assets.xcassets/AppIcon.appiconset"

echo "=== NDD Resource Map App Icon Generator ==="
echo ""

# Check if an icon already exists
if [[ -f "${ICON_DIR}/AppIcon.png" ]]; then
    echo "✓ App icon already exists at: ${ICON_DIR}/AppIcon.png"
    sips -g pixelWidth -g pixelHeight "${ICON_DIR}/AppIcon.png"
    exit 0
fi

# Option 1: Scale up existing apple-touch-icon (not ideal quality)
EXISTING_ICON="/Users/alexbeattie/Developer/CHLA/map-frontend/public/apple-touch-icon.png"

if [[ -f "${EXISTING_ICON}" ]]; then
    echo "Found existing icon at: ${EXISTING_ICON}"
    echo ""
    echo "Options:"
    echo "  1. Scale up existing 180x180 icon (quick but lower quality)"
    echo "  2. Create a new icon manually (recommended)"
    echo ""
    read -p "Enter choice (1 or 2): " choice
    
    if [[ "${choice}" == "1" ]]; then
        echo "Scaling up existing icon..."
        sips -z 1024 1024 "${EXISTING_ICON}" --out "${ICON_DIR}/AppIcon.png"
        echo "✓ Created scaled icon at: ${ICON_DIR}/AppIcon.png"
        echo ""
        echo "⚠️  Note: Scaled icons may appear blurry. Consider creating a native 1024x1024 icon."
    else
        echo ""
        echo "To create a new app icon:"
        echo "  1. Use Figma, Sketch, or Adobe Illustrator"
        echo "  2. Create a 1024x1024 pixel design"
        echo "  3. Export as PNG without transparency"
        echo "  4. Save to: ${ICON_DIR}/AppIcon.png"
        echo ""
        echo "Design suggestions for NDD Resource Map:"
        echo "  - Map pin or location marker"
        echo "  - Blue/teal color scheme (matching Regional Centers)"
        echo "  - Clean, modern, accessible design"
        echo "  - Avoid text (too small to read)"
    fi
else
    echo "No existing icon found."
    echo ""
    echo "To create an app icon:"
    echo "  1. Use Figma, Sketch, or Adobe Illustrator"
    echo "  2. Create a 1024x1024 pixel design"
    echo "  3. Export as PNG without transparency"
    echo "  4. Save to: ${ICON_DIR}/AppIcon.png"
fi

echo ""
echo "After adding the icon, run this script again to verify."




