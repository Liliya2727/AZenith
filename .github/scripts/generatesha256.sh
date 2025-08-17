#!/bin/env bash

set -e

if [ -z "$1" ]; then
  echo "Usage: $0 <directory>"
  exit 1
fi

generate_checksum() {
  local file="$1"
  local hash
  hash=$(sha256sum "$file" | awk '{print $1}')
  printf "%s" "$hash" > "${file}.sha256"
  echo "Generated checksum for: $file"
}

find "$1" -type f ! -name "*.sha256" | while IFS= read -r file; do
  generate_checksum "$file"
done
