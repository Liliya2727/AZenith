#!/bin/env bash
# shellcheck disable=SC2035

if [ -z "$GITHUB_WORKSPACE" ]; then
	echo "This script should only run on GitHub action!" >&2
	exit 1
fi

# Make sure we're on right directory
cd "$GITHUB_WORKSPACE" || {
	echo "Unable to cd to GITHUB_WORKSPACE" >&2
	exit 1
}


# Write module version to daemon and webui
version="$(cat version)"
version_type="$(cat version_type)"
version_code="$(git rev-list HEAD --count)"
release_code="$(git rev-list HEAD --count)-$(git rev-parse --short HEAD)-$version_type"
echo "start sending AZenith version: $version ($release_code) to WebUI, App and Daemon"
sed -i "s|#define MODULE_VERSION \".*\"|#define MODULE_VERSION \"$version ($release_code)\"|" jni/include/AZenith.h
sed -i "s/versionCode =.*/versionCode = $version_code/" manager/app/build.gradle.kts
sed -i "s/versionName =.*/versionName = \"$version ($release_code)\"/" manager/app/build.gradle.kts

echo "Successfully write Version code to gradle.build: $(cat manager/app/build.gradle.kts | grep versionCode)"
echo "Successfully write Version name to gradle.build: $(cat manager/app/build.gradle.kts | grep versionName)"
echo "Successfully write to AZenith.h: $(cat jni/include/AZenith.h | grep MODULE_VERSION)"

