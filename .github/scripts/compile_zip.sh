#!/bin/env bash

if [ -z "$GITHUB_WORKSPACE" ]; then
	echo "This script should only run on GitHub action!" >&2
	exit 1
fi

# Make sure we're on right directory
cd "$GITHUB_WORKSPACE" || {
	echo "Unable to cd to GITHUB_WORKSPACE" >&2
	exit 1
}

# Put critical files and folders here
need_integrity=(
	"mainfiles/system/bin"
	"mainfiles/libs"
	"mainfiles/META-INF"
	"mainfiles/service.sh"
	"mainfiles/uninstall.sh"
	"mainfiles/module.prop"
        "mainfiles/AZenith_icon.png"
	"mainfiles/gamelist.txt"
        "mainfiles/toast.apk"
)

# Version info
version="$(cat version)"
version_code="$(git rev-list HEAD --count)"
release_code="R-$(git rev-parse --short HEAD)"
sed -i "s/version=.*/version=$version ($release_code)/" mainfiles/module.prop
sed -i "s/versionCode=.*/versionCode=$version_code/" mainfiles/module.prop

# Compile Gamelist
paste -sd '|' - <"$GITHUB_WORKSPACE/gamelist.txt" >"$GITHUB_WORKSPACE/mainfiles/gamelist.txt"

# Copy module files
cp -r ./libs mainfiles
cp -r ./tweakfls/* mainfiles/system/bin
cp -r ./webui/* mainfiles/webroot
cp LICENSE ./mainfiles

# Remove .sh extension from scripts
find mainfiles/system/bin -maxdepth 1 -type f -name "*.sh" -exec sh -c 'mv -- "$0" "${0%.sh}"' {} \;

# Parse version info to module prop
zipName="AZenith火-$version-$release_code.zip"
echo "zipName=$zipName" >>"$GITHUB_OUTPUT"


# Zip the file
cd ./mainfiles || {
	echo "Unable to cd to ./mainfiles" >&2
	exit 1
}

zip -r9 ../"$zipName" * -x *placeholder* *.map .shellcheckrc
zip -z ../"$zipName" <<EOF
$version-$release_code
Build Date $(date +"%a %b %d %H:%M:%S %Z %Y")
EOF
