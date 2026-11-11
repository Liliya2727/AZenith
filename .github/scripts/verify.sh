#!/usr/bin/env bash

set -euo pipefail

if [ -z "${GITHUB_WORKSPACE:-}" ]; then
	echo "❌ Error: This script should only run on GitHub Actions!" >&2
	exit 1
fi

cd "$GITHUB_WORKSPACE" || {
	echo "❌ Error: Unable to cd to GITHUB_WORKSPACE" >&2
	exit 1
}

readonly GRADLE_FILE="manager/app/build.gradle.kts"
# Full-version define (à la C #define MODULE_VERSION), rewritten at build time.
readonly VERSION_RS_FILE="archdaemon/src/version.rs"
# All Rust binary projects except thermalcore (kept independent).
readonly CARGO_TOMLS=(
	"archdaemon/Cargo.toml"
	"sysdaemon/Cargo.toml"
	"binprofiles/Cargo.toml"
	"binutils/Cargo.toml"
	"binpreferenced/Cargo.toml"
)

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

# Gradle (manager): versionCode + full versionName.
sed -i "s/versionCode =.*/versionCode = $VERSION_CODE/" "$GRADLE_FILE"
sed -i "s/versionName =.*/versionName = \"$FULL_VERSION\"/" "$GRADLE_FILE"

# Every Rust binary crate (except thermalcore): semver from ./version.
# Checked-in default is 1.0.0; CI injects the real release version here.
for cargo_toml in "${CARGO_TOMLS[@]}"; do
	sed -i "s/^version = .*/version = \"$VERSION\"/" "$cargo_toml"
done

# archdaemon: full version embedded for the module.prop check.
sed -i "s|pub const FULL_VERSION: &str = \".*\";|pub const FULL_VERSION: \&str = \"$FULL_VERSION\";|" "$VERSION_RS_FILE"

echo "✅ Injection complete! Verifying changes:"
echo "---------------------------------------------------"

grep -H "versionCode" "$GRADLE_FILE"
grep -H "versionName" "$GRADLE_FILE"
for cargo_toml in "${CARGO_TOMLS[@]}"; do
	grep -H "^version" "$cargo_toml"
done
grep -H "FULL_VERSION" "$VERSION_RS_FILE"
echo "---------------------------------------------------"
