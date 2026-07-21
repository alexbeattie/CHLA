#!/usr/bin/env bash
set -euo pipefail

script_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
android_directory="$(cd "${script_directory}/.." && pwd)"
keystore_file="${KINDD_ANDROID_KEYSTORE_PATH:-${HOME}/.android/keys/kindd-upload.jks}"
key_alias="${KINDD_ANDROID_KEY_ALIAS:-kindd-upload}"
keychain_service="org.kindd.android.upload"
keychain_account="kindd-upload"
secrets_file="${android_directory}/secrets.properties"

if [[ ! -f "${keystore_file}" ]]; then
    echo "Missing KiNDD upload keystore at ${keystore_file}."
    echo "Run ./scripts/create-play-upload-key.sh once, then retry."
    exit 1
fi

if [[ ! -f "${secrets_file}" ]] || ! awk -F= '
    $1 == "MAPS_API_KEY" {
        value = substr($0, index($0, "=") + 1)
        valid = value ~ /^AIza[[:alnum:]_-]{35}$/
    }
    END { exit valid ? 0 : 1 }
' "${secrets_file}"; then
    echo "Missing or invalid Google Maps API key in ${secrets_file}."
    echo "Add a real Android-restricted MAPS_API_KEY before building a Play bundle."
    exit 1
fi

keystore_password="$(security find-generic-password \
    -a "${keychain_account}" \
    -s "${keychain_service}" \
    -w)"
decoded_manifest_file=""

unset_credentials() {
    unset keystore_password
    unset KINDD_ANDROID_KEYSTORE_PASSWORD KINDD_ANDROID_KEY_PASSWORD
    if [[ -n "${decoded_manifest_file}" ]]; then
        rm -f "${decoded_manifest_file}"
    fi
}
trap unset_credentials EXIT

export ANDROID_HOME="${ANDROID_HOME:-${HOME}/Library/Android/sdk}"
export KINDD_ANDROID_KEYSTORE_PATH="${keystore_file}"
export KINDD_ANDROID_KEYSTORE_PASSWORD="${keystore_password}"
export KINDD_ANDROID_KEY_ALIAS="${key_alias}"
export KINDD_ANDROID_KEY_PASSWORD="${keystore_password}"

if [[ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]]; then
    export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
fi

cd "${android_directory}"
./gradlew :app:bundleRelease --no-daemon

bundle_file="${android_directory}/app/build/outputs/bundle/release/app-release.aab"
signature_report="$(jarsigner -verify -verbose -certs "${bundle_file}" 2>&1)"
if ! grep -q '^jar verified\.$' <<<"${signature_report}"; then
    echo "The generated App Bundle is not signed correctly."
    exit 1
fi

decoded_manifest_file="$(mktemp /tmp/kindd-android-manifest.XXXXXX)"
unzip -p "${bundle_file}" base/manifest/AndroidManifest.xml >"${decoded_manifest_file}"
if LC_ALL=C grep -a -q 'YOUR_GOOGLE_MAPS_API_KEY_HERE' "${decoded_manifest_file}"; then
    echo "The generated App Bundle contains the placeholder Google Maps API key."
    exit 1
fi

echo "Signed Google Play bundle: ${bundle_file}"
shasum -a 256 "${bundle_file}"
