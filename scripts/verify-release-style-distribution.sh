#!/usr/bin/env bash
# Verify the desktop distribution flow using Maven-resolved artifacts.
#
# This script expects the native library for the current platform to have been
# built already, because godot-java-native packages native/build outputs.

set -euo pipefail

usage() {
    cat <<'USAGE'
Usage: verify-release-style-distribution.sh [options]

Options:
  --template <dir>       external godot-java-template project (default: ../godot-java-template)
  --version <version>    godot-java version to install and consume (default: root pom revision)
  --classifier <name>    native classifier passed to the template Maven build
  --help                 show this help
USAGE
}

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEMPLATE_DIR="$ROOT_DIR/../godot-java-template"
VERSION=""
CLASSIFIER=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --template)
            TEMPLATE_DIR="$2"
            shift 2
            ;;
        --version)
            VERSION="$2"
            shift 2
            ;;
        --classifier)
            CLASSIFIER="$2"
            shift 2
            ;;
        --help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            usage >&2
            exit 2
            ;;
    esac
done

if [[ -z "$VERSION" ]]; then
    VERSION="$(sed -n 's:.*<revision>\(.*\)</revision>.*:\1:p' "$ROOT_DIR/pom.xml" | head -1)"
fi

if [[ -z "$VERSION" ]]; then
    echo "ERROR: could not determine godot-java version. Pass --version." >&2
    exit 1
fi

native_name_for_classifier() {
    case "$1" in
        macos-*) echo "libgodot-java.dylib" ;;
        linux-*) echo "libgodot-java.so" ;;
        windows-*) echo "libgodot-java.dll" ;;
        *) echo "" ;;
    esac
}

TEMPLATE_DIR="$(cd "$TEMPLATE_DIR" && pwd)"
if [[ ! -f "$TEMPLATE_DIR/pom.xml" || ! -d "$TEMPLATE_DIR/godot" ]]; then
    echo "ERROR: template project not found: $TEMPLATE_DIR" >&2
    exit 1
fi

TMP_REPO="$(mktemp -d)"
cleanup() {
    rm -rf "$TMP_REPO"
}
trap cleanup EXIT

echo "Installing release-style artifacts into temporary Maven repo..."
"$ROOT_DIR/mvnw" -q -Dmaven.repo.local="$TMP_REPO" \
    -Drevision="$VERSION" \
    -DskipTests \
    -Dspotbugs.skip=true \
    -Dspotless.skip=true \
    -pl godot-java-core \
    -am \
    install

"$ROOT_DIR/mvnw" -q -Dmaven.repo.local="$TMP_REPO" \
    -Drevision="$VERSION" \
    -Dgodot.java.native.package=true \
    -DskipTests \
    -Dspotbugs.skip=true \
    -Dspotless.skip=true \
    -pl godot-java-native \
    install

echo "Building template against temporary Maven repo..."
MVN_TEMPLATE_ARGS=(
    -q
    "-Dmaven.repo.local=$TMP_REPO"
    "-Dgodot-java.version=$VERSION"
)

if [[ -n "$CLASSIFIER" ]]; then
    NATIVE_LIBRARY="$(native_name_for_classifier "$CLASSIFIER")"
    if [[ -z "$NATIVE_LIBRARY" ]]; then
        echo "ERROR: unsupported native classifier: $CLASSIFIER" >&2
        exit 1
    fi
    MVN_TEMPLATE_ARGS+=("-Dgodot.java.native.classifier=$CLASSIFIER")
    MVN_TEMPLATE_ARGS+=("-Dgodot.java.native.library=$NATIVE_LIBRARY")
fi

mvn "${MVN_TEMPLATE_ARGS[@]}" \
    -f "$TEMPLATE_DIR/pom.xml" \
    package

RUNTIME_DIR="$TEMPLATE_DIR/godot/godot-java"
if [[ ! -f "$RUNTIME_DIR/app.jar" || ! -f "$RUNTIME_DIR/VERSION" ]]; then
    echo "ERROR: template Maven build did not sync the Godot runtime directory." >&2
    exit 1
fi

echo "Release-style distribution verification passed."
