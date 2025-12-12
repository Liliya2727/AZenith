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
release_code="$(git rev-list HEAD --count)-$(git rev-parse --short HEAD)-release"
echo "start sending AZenith version: $version ($release_code) to WebUI and Daemon"
sed -i "s|#define MODULE_VERSION \".*\"|#define MODULE_VERSION \"$version ($release_code)\"|" jni/include/AZenith.h
sed -i 's#const WEBUI_VERSION = ".*";#const WEBUI_VERSION = "'"$version ($release_code)"'";#' webui/src/scripts/webui_utils.js

echo "$(cat webui/src/scripts/webui_utils.js | grep 'const WEBUI_VERSION')"
echo "$(cat jni/include/AZenith.h | grep MODULE_VERSION)"
