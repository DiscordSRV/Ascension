#!/usr/bin/env bash

# Links every ../../modded/versions/VERSION/gradle.properties to ../versions/VERSION/gradle.properties
set -euo pipefail
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
VERSIONS_DIR="$SCRIPT_DIR/../versions"
MODDED_VERSIONS_DIR="$SCRIPT_DIR/../modded/versions"
echo
echo "Linking gradle.properties into each modded version under $MODDED_VERSIONS
_DIR"
for version_path in "$VERSIONS_DIR"/*; do
    ver="$(basename "$version_path")"
    target_dir="$MODDED_VERSIONS_DIR/$ver"
    target="$target_dir/gradle.properties"
    source="$version_path/gradle.properties"
    if [[ -e "$target" ]]; then
      rm -rf "$target"
    fi
    echo "Linking gradle.properties to modded/versions/$ver/gradle.properties
    ln -s "$source" "$target"
done
echo
echo "All done. Each NeoForge version now has a link to the corresponding gradle.properties in the modded module."
