#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

SRC_DIR="$SCRIPT_DIR/../src"
VERSIONS_DIR="$SCRIPT_DIR/../versions"

echo
echo "Linking $SRC_DIR into each version under $VERSIONS_DIR"
for version_path in "$VERSIONS_DIR"/*; do
    ver="$(basename "$version_path")"
    target="$version_path/src"
    if [[ -e "$target" ]]; then
      rm -rf "$target"
    fi
    echo "Linking src → versions/$ver/src"
    ln -s "$SRC_DIR" "$target"
done

echo
echo "All done. Each version now has a link to the src directory."