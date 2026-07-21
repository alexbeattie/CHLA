#!/usr/bin/env bash
set -euo pipefail

script_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
android_directory="$(cd "${script_directory}/.." && pwd)"
repository_directory="$(cd "${android_directory}/.." && pwd)"

ios_icon="${repository_directory}/chla-ios/CHLA-iOS/Resources/Assets.xcassets/AppIcon.appiconset/AppIcon.png"
android_icon="${android_directory}/app/src/main/res/drawable-nodpi/kindd_app_icon.png"

cmp "${ios_icon}" "${android_icon}"

grep -q '#FFFFFF' \
    "${android_directory}/app/src/main/res/drawable/ic_launcher_background.xml"
grep -q '@drawable/kindd_app_icon' \
    "${android_directory}/app/src/main/res/drawable/ic_launcher_foreground.xml"

for density in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
    for resource in ic_launcher ic_launcher_round; do
        launcher_file="${android_directory}/app/src/main/res/mipmap-${density}/${resource}.xml"
        grep -q '@drawable/kindd_app_icon' "${launcher_file}"
        if grep -q 'M16,10 L16,38' "${launcher_file}"; then
            echo "Legacy K artwork remains in ${launcher_file}." >&2
            exit 1
        fi
    done
done

echo "Android launcher resources use the exact iPhone AppIcon artwork."
