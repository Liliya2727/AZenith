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

readonly ASSETSFOLDER="manager/app/src/main/assets/changelog.md"
readonly CHANGELOGFILE="changelog.md"

if [ -f "$CHANGELOGFILE" ]; then
    cp "$CHANGELOGFILE" "$ASSETSFOLDER"
    echo "✅ Success: $CHANGELOGFILE copied to $ASSETSFOLDER"
fi