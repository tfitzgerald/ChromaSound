#!/bin/bash
# =============================================================================
# generate_keystore.sh
#
# Run this ONCE on your local machine to create a signing keystore and
# produce the base64 string you paste into GitHub Secrets.
#
# Usage:  bash generate_keystore.sh
# =============================================================================

set -e

echo ""
echo "========================================"
echo "  ChromaSound Keystore Generator"
echo "========================================"
echo ""

# ── Collect info ──────────────────────────────────────────────────────────────
read -p "Key alias (e.g. chromasound):        " KEY_ALIAS
read -s -p "Keystore password (min 6 chars):    " STORE_PASS; echo
read -s -p "Key password (can be same):         " KEY_PASS;   echo
read -p "Your name (for certificate):         " DNAME_CN
read -p "Organisation (or your name again):   " DNAME_O
read -p "Country code (e.g. CA):              " DNAME_C

KEYSTORE_FILE="chromasound.keystore"

echo ""
echo "Generating keystore: $KEYSTORE_FILE ..."

keytool -genkeypair \
  -v \
  -keystore "$KEYSTORE_FILE" \
  -alias "$KEY_ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass "$STORE_PASS" \
  -keypass  "$KEY_PASS" \
  -dname "CN=$DNAME_CN, O=$DNAME_O, C=$DNAME_C"

echo ""
echo "✅  Keystore created: $KEYSTORE_FILE"
echo ""

# ── Encode to base64 ──────────────────────────────────────────────────────────
BASE64_VALUE=$(base64 -w 0 "$KEYSTORE_FILE")   # Linux
# On macOS use:  base64 -i "$KEYSTORE_FILE" -o -

echo "========================================"
echo "  Copy these into GitHub Secrets"
echo "  (Settings → Secrets and variables → Actions → New repository secret)"
echo "========================================"
echo ""
echo "Secret name:   KEYSTORE_BASE64"
echo "Secret value:  $BASE64_VALUE"
echo ""
echo "Secret name:   KEY_STORE_PASSWORD"
echo "Secret value:  $STORE_PASS"
echo ""
echo "Secret name:   KEY_ALIAS"
echo "Secret value:  $KEY_ALIAS"
echo ""
echo "Secret name:   KEY_PASSWORD"
echo "Secret value:  $KEY_PASS"
echo ""
echo "========================================"
echo "⚠️  KEEP $KEYSTORE_FILE SAFE."
echo "    Back it up somewhere secure."
echo "    NEVER commit it to git."
echo "========================================"
