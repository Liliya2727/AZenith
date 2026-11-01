#
# Copyright (C) 2024-2025 Rem01Gaming
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

TMPDIR_FOR_VERIFY="$TMPDIR/.vunzip"
mkdir "$TMPDIR_FOR_VERIFY"

if [ -f "$TMPDIR/installer-lang.sh" ]; then
	. "$TMPDIR/installer-lang.sh"
else
	AZENITH_LANG="en"
	azenith_text() {
		case "$1" in
			abort_corrupted) printf '%s' '! Installation aborted. The module may be corrupted.' ;;
			abort_redownload) printf '%s' '! Please re-download and try again.' ;;
			checksum_missing) printf '%s' 'Missing checksum for installation file.' ;;
			checksum_mismatch) printf '%s' 'Checksum mismatch for file.' ;;
			file_missing) printf 'File not found: %s' "$2" ;;
			update_binary_missing) printf '%s' 'Installer file does not exist.' ;;
			verified_file) printf '%s' '- Verified file' ;;
			download_from_magisk) printf '%s' '- Install from Magisk app' ;;
			*) printf '%s' "$1" ;;
		esac
	}
fi

abort_verify() {
    clear
	ui_print "! $1"
	ui_print "$(azenith_text abort_corrupted)"
	abort "$(azenith_text abort_redownload)"
}

# extract <zip> <file> <target dir>
extract() {
	zip=$1
	file=$2
	dir=$3

	file_path="$dir/$file"
	hash_path="$TMPDIR_FOR_VERIFY/$file.sha256"

	unzip -o "$zip" "$file" -d "$dir" >&2
	[ -f "$file_path" ] || abort_verify "$(azenith_text file_missing "$file")"

	unzip -o "$zip" "$file.sha256" -d "$TMPDIR_FOR_VERIFY" >&2
	[ -f "$hash_path" ] || abort_verify "$(azenith_text checksum_missing)"

	(echo "$(cat "$hash_path")  $file_path" | sha256sum -c -s -) || abort_verify "$(azenith_text checksum_mismatch)"
	ui_print "$(azenith_text verified_file): $file" >&1
}

file="META-INF/com/google/android/update-binary"
file_path="$TMPDIR_FOR_VERIFY/$file"
hash_path="$file_path.sha256"
unzip -o "$ZIPFILE" "META-INF/com/google/android/*" -d "$TMPDIR_FOR_VERIFY" >&2
[ -f "$file_path" ] || abort_verify "$(azenith_text update_binary_missing)"
if [ -f "$hash_path" ]; then
	(echo "$(cat "$hash_path")  $file_path" | sha256sum -c -s -) || abort_verify "$(azenith_text checksum_mismatch)"
	ui_print "$(azenith_text verified_file): $file" >&1
else
	ui_print "$(azenith_text download_from_magisk)"
fi
