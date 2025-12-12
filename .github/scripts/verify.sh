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
sed -i "s|#define MODULE_VERSION \".placeholder\"|#define MODULE_VERSION \"$version ($release_code)\"|" jni/include/AZenith.h
sed -i "s|const WEBUI_VERSION = \".placeholder\";|const WEBUI_VERSION = \"$version ($release_code)\";|" webui/src/scripts/webui_utils.js
