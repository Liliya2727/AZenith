#!/usr/bin/env bash

set -euo pipefail
l
if [ -z "${GITHUB_WORKSPACE:-}" ]; then
	echo "❌ Error: This script should only run on GitHub Actions!" >&2
	exit 1
fi

cd "$GITHUB_WORKSPACE" || {
	echo "❌ Error: Unable to cd to GITHUB_WORKSPACE" >&2
	exit 1
}

readonly HEADER_FILE="archdaemon/jni/include/AZenith.h"
readonly GRADLE_FILE="manager/app/build.gradle.kts"

[ -f "version" ] || { echo "❌ Error: 'version' file not found!"; exit 1; }
[ -f "version_type" ] || { echo "❌ Error: 'version_type' file not found!"; exit 1; }

readonly VERSION=$(cat version)
readonly VERSION_TYPE=$(cat version_type)
readonly VERSION_CODE=$(git rev-list HEAD --count)
readonly SHORT_HASH=$(git rev-parse --short HEAD)
readonly RELEASE_CODE="${VERSION_CODE}-${SHORT_HASH}-${VERSION_TYPE}"
readonly FULL_VERSION="${VERSION} (${RELEASE_CODE})"

echo "Starting version injection..."
echo "Target Version: $FULL_VERSION"
echo "Version Code  : $VERSION_CODE"

sed -i "s|#define MODULE_VERSION \".*\"|#define MODULE_VERSION \"$FULL_VERSION\"|" "$HEADER_FILE"

sed -i "s/versionCode =.*/versionCode = $VERSION_CODE/" "$GRADLE_FILE"
sed -i "s/versionName =.*/versionName = \"$FULL_VERSION\"/" "$GRADLE_FILE"

echo "✅ Injection complete! Verifying changes:"
echo "---------------------------------------------------"

grep -H "versionCode" "$GRADLE_FILE"
grep -H "versionName" "$GRADLE_FILE"
grep -H "MODULE_VERSION" "$HEADER_FILE"
echo "---------------------------------------------------"
