#!/bin/bash

# CHLA iOS App Setup Script
# This script sets up the Xcode project for the iOS app

set -e

echo "🚀 Setting up CHLA iOS App..."

# Check if we're in the right directory
if [[ ! -f "project.yml" ]]; then
    echo "❌ Error: Please run this script from the chla-ios directory"
    exit 1
fi

# Check if xcodegen is installed
if command -v xcodegen &> /dev/null; then
    echo "📦 Generating Xcode project with XcodeGen..."
    xcodegen generate
    echo "✅ Xcode project generated successfully!"
    echo ""
    echo "To open the project:"
    echo "  open CHLA-iOS.xcodeproj"
else
    echo "⚠️  XcodeGen not found. Installing via Homebrew..."
    
    # Check if Homebrew is installed
    if command -v brew &> /dev/null; then
        brew install xcodegen
        echo "📦 Generating Xcode project..."
        xcodegen generate
        echo "✅ Xcode project generated successfully!"
    else
        echo ""
        echo "📋 Manual Setup Required"
        echo "========================"
        echo ""
        echo "Option 1: Install XcodeGen"
        echo "  brew install xcodegen"
        echo "  ./setup.sh"
        echo ""
        echo "Option 2: Create project manually in Xcode"
        echo "  1. Open Xcode"
        echo "  2. File → New → Project → iOS → App"
        echo "  3. Product Name: CHLA-iOS"
        echo "  4. Interface: SwiftUI"
        echo "  5. Language: Swift"
        echo "  6. Save to this directory"
        echo "  7. Replace generated files with CHLA-iOS folder contents"
        echo ""
    fi
fi

# Create Assets catalog if it doesn't exist
if [[ ! -d "CHLA-iOS/Resources/Assets.xcassets" ]]; then
    echo "📁 Creating Assets catalog..."
    mkdir -p "CHLA-iOS/Resources/Assets.xcassets/AppIcon.appiconset"
    mkdir -p "CHLA-iOS/Resources/Assets.xcassets/AccentColor.colorset"
    
    # Create Contents.json for Assets.xcassets
    cat > "CHLA-iOS/Resources/Assets.xcassets/Contents.json" << 'EOF'
{
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
EOF

    # Create Contents.json for AppIcon
    cat > "CHLA-iOS/Resources/Assets.xcassets/AppIcon.appiconset/Contents.json" << 'EOF'
{
  "images" : [
    {
      "idiom" : "universal",
      "platform" : "ios",
      "size" : "1024x1024"
    }
  ],
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
EOF

    # Create Contents.json for AccentColor
    cat > "CHLA-iOS/Resources/Assets.xcassets/AccentColor.colorset/Contents.json" << 'EOF'
{
  "colors" : [
    {
      "color" : {
        "color-space" : "srgb",
        "components" : {
          "alpha" : "1.000",
          "blue" : "0.850",
          "green" : "0.470",
          "red" : "0.240"
        }
      },
      "idiom" : "universal"
    }
  ],
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
EOF
    
    echo "✅ Assets catalog created"
fi

echo ""
echo "🎉 Setup complete!"
echo ""
echo "Next steps:"
echo "  1. Open the project: open CHLA-iOS.xcodeproj"
echo "  2. Select your development team in Signing & Capabilities"
echo "  3. Build and run (Cmd+R)"
echo ""
echo "Make sure your Django backend is running for API access:"
echo "  cd ../maplocation && source ../venv/bin/activate && python3 manage.py runserver"

