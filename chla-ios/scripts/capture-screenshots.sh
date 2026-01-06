#!/bin/bash
# App Store Screenshot Capture Script for NDD Resource Map
# Captures screenshots from iOS Simulator for App Store submission

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "${SCRIPT_DIR}")"
SCREENSHOT_DIR="${PROJECT_ROOT}/screenshots"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

echo "=== NDD Resource Map Screenshot Capture ==="
echo ""

# Create screenshot directory
mkdir -p "${SCREENSHOT_DIR}"

# Check for available simulators
echo "Available iPhone simulators:"
xcrun simctl list devices available | grep -E "iPhone (15|14|13|12) Pro" | head -10
echo ""

# Get the booted simulator or use iPhone 15 Pro Max
BOOTED_DEVICE=$(xcrun simctl list devices booted | grep -E "iPhone" | head -1 | sed -E 's/.*\(([A-Z0-9-]+)\).*/\1/')

if [[ -z "${BOOTED_DEVICE}" ]]; then
    echo "No simulator is currently booted."
    echo ""
    echo "To capture screenshots:"
    echo "  1. Open Xcode"
    echo "  2. Run the app on iPhone 15 Pro Max simulator"
    echo "  3. Navigate to each screen and press Cmd+S to capture"
    echo ""
    echo "Required screenshots (1290 x 2796 px for 6.7\" display):"
    echo "  1. Main map view with provider markers"
    echo "  2. Regional Centers list or map view"
    echo "  3. Provider detail sheet"
    echo "  4. Search results"
    echo "  5. Directions view"
    echo ""
    echo "Save screenshots to: ${SCREENSHOT_DIR}"
    exit 0
fi

echo "Using booted simulator: ${BOOTED_DEVICE}"
echo "Screenshots will be saved to: ${SCREENSHOT_DIR}"
echo ""

# Function to capture screenshot
capture() {
    local name=$1
    local filename="${SCREENSHOT_DIR}/${TIMESTAMP}_${name}.png"
    xcrun simctl io "${BOOTED_DEVICE}" screenshot "${filename}"
    echo "✓ Captured: ${filename}"
}

echo "Ready to capture screenshots."
echo "Navigate to each screen in the simulator, then press Enter to capture."
echo ""

# Capture sequence
screens=("01_map_view" "02_regional_centers" "03_provider_detail" "04_search" "05_directions")
descriptions=(
    "Main map view with provider markers"
    "Regional Centers list or map"
    "Provider detail sheet (tap a provider)"
    "Search results (search for something)"
    "Directions view (get directions to a provider)"
)

for i in "${!screens[@]}"; do
    echo "Screen $((i+1))/5: ${descriptions[${i}]}"
    read -p "Press Enter when ready to capture (or 's' to skip): " response
    if [[ "${response}" != "s" ]]; then
        capture "${screens[${i}]}"
    else
        echo "Skipped"
    fi
    echo ""
done

echo ""
echo "=== Screenshot capture complete ==="
echo "Screenshots saved to: ${SCREENSHOT_DIR}"
echo ""
echo "Next steps:"
echo "  1. Review screenshots in Finder"
echo "  2. Upload to App Store Connect under 'App Information' > 'Screenshots'"
echo "  3. Use 6.7\" display size category"

# List captured files
echo ""
echo "Captured files:"
ls -la "${SCREENSHOT_DIR}" 2>/dev/null | grep "${TIMESTAMP}"




