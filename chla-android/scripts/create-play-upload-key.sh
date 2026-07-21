#!/usr/bin/env bash
set -euo pipefail

key_directory="${KINDD_ANDROID_KEY_DIR:-${HOME}/.android/keys}"
keystore_file="${KINDD_ANDROID_KEYSTORE_PATH:-${key_directory}/kindd-upload.jks}"
certificate_file="${KINDD_ANDROID_UPLOAD_CERT_PATH:-${key_directory}/kindd-upload-certificate.pem}"
key_alias="${KINDD_ANDROID_KEY_ALIAS:-kindd-upload}"
keychain_service="org.kindd.android.upload"
keychain_account="kindd-upload"
credential_created=false

if [[ -e "${keystore_file}" ]]; then
    echo "Upload keystore already exists at ${keystore_file}. Refusing to replace it."
    exit 1
fi

if security find-generic-password \
    -a "${keychain_account}" \
    -s "${keychain_service}" >/dev/null 2>&1; then
    echo "The KiNDD upload credential already exists in Keychain. Refusing to replace it."
    exit 1
fi

mkdir -p "${key_directory}"
chmod 700 "${key_directory}"

generated_password="$(openssl rand -base64 48 | tr -d '\n')"
export KINDD_GENERATED_UPLOAD_PASSWORD="${generated_password}"

cleanup_on_exit() {
    exit_code=$?
    trap - EXIT
    unset generated_password KINDD_GENERATED_UPLOAD_PASSWORD
    if [[ ${exit_code} -ne 0 ]]; then
        rm -f "${keystore_file}" "${certificate_file}"
        if [[ "${credential_created}" == true ]]; then
            security delete-generic-password \
                -a "${keychain_account}" \
                -s "${keychain_service}" >/dev/null 2>&1 || true
        fi
    fi
    exit "${exit_code}"
}
trap cleanup_on_exit EXIT

security add-generic-password \
    -a "${keychain_account}" \
    -s "${keychain_service}" \
    -w "${KINDD_GENERATED_UPLOAD_PASSWORD}" >/dev/null
credential_created=true

keytool -genkeypair \
    -keystore "${keystore_file}" \
    -storetype JKS \
    -storepass:env KINDD_GENERATED_UPLOAD_PASSWORD \
    -keypass:env KINDD_GENERATED_UPLOAD_PASSWORD \
    -alias "${key_alias}" \
    -keyalg RSA \
    -keysize 4096 \
    -validity 10000 \
    -dname "CN=KiNDD Android Upload, OU=Mobile, O=KiNDD, L=Los Angeles, ST=California, C=US"

keytool -exportcert -rfc \
    -keystore "${keystore_file}" \
    -storepass:env KINDD_GENERATED_UPLOAD_PASSWORD \
    -alias "${key_alias}" \
    -file "${certificate_file}"

chmod 600 "${keystore_file}"
chmod 644 "${certificate_file}"

echo "Created KiNDD Google Play upload key: ${keystore_file}"
echo "Exported public upload certificate: ${certificate_file}"
keytool -list -v \
    -keystore "${keystore_file}" \
    -storepass:env KINDD_GENERATED_UPLOAD_PASSWORD \
    -alias "${key_alias}" | grep -E 'SHA1:|SHA256:'
echo "The private password is stored in macOS Keychain under ${keychain_service}."
