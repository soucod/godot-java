#!/usr/bin/env bash
# Sync a built Java application jar and godot-java native library into a Godot project.
#
# Usage:
#   scripts/sync-godot-java.sh --project godot-java-examples/examples/it-test \
#     --app-jar godot-java-examples/target/godot-java-examples.jar

set -euo pipefail

usage() {
    cat <<'USAGE'
Usage: sync-godot-java.sh --project <godot-project-dir> --app-jar <jar> [options]

Options:
  --version <version>            godot-java native artifact version (default: root pom revision)
  --classifier <classifier>      native artifact classifier (default: detected platform)
  --native-zip <zip>             use a local native zip instead of resolving with Maven
  --native-lib <path>            use a local native library instead of resolving with Maven
  --output-dir <name>            project-relative runtime dir (default: godot-java)
  --maven-repo <dir>             Maven local repository used when resolving native artifacts
  --help                         show this help
USAGE
}

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT_DIR=""
APP_JAR=""
VERSION=""
CLASSIFIER=""
NATIVE_ZIP=""
NATIVE_LIB=""
OUTPUT_DIR="godot-java"
MAVEN_REPO=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --project)
            PROJECT_DIR="$2"
            shift 2
            ;;
        --app-jar)
            APP_JAR="$2"
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
        --native-zip)
            NATIVE_ZIP="$2"
            shift 2
            ;;
        --native-lib)
            NATIVE_LIB="$2"
            shift 2
            ;;
        --output-dir)
            OUTPUT_DIR="$2"
            shift 2
            ;;
        --maven-repo)
            MAVEN_REPO="$2"
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

if [[ -z "$PROJECT_DIR" || -z "$APP_JAR" ]]; then
    usage >&2
    exit 2
fi

PROJECT_DIR="$(cd "$PROJECT_DIR" && pwd)"
APP_JAR="$(cd "$(dirname "$APP_JAR")" && pwd)/$(basename "$APP_JAR")"

if [[ ! -f "$APP_JAR" ]]; then
    echo "ERROR: app jar not found: $APP_JAR" >&2
    echo "Run the project package build first." >&2
    exit 1
fi

detect_classifier() {
    local os arch
    os="$(uname -s)"
    arch="$(uname -m)"
    case "$os:$arch" in
        Darwin:*) echo "macos-universal" ;;
        Linux:x86_64) echo "linux-x86_64" ;;
        Linux:aarch64|Linux:arm64) echo "linux-aarch64" ;;
        MINGW*:x86_64|MSYS*:x86_64|CYGWIN*:x86_64) echo "windows-x86_64" ;;
        *) echo "unsupported" ;;
    esac
}

native_name_for_classifier() {
    case "$1" in
        macos-*) echo "libgodot-java.dylib" ;;
        linux-*) echo "libgodot-java.so" ;;
        windows-*) echo "libgodot-java.dll" ;;
        *) echo "" ;;
    esac
}

if [[ -z "$CLASSIFIER" ]]; then
    CLASSIFIER="$(detect_classifier)"
fi

NATIVE_NAME="$(native_name_for_classifier "$CLASSIFIER")"
if [[ -z "$NATIVE_NAME" || "$CLASSIFIER" == "unsupported" ]]; then
    echo "ERROR: unsupported native classifier: $CLASSIFIER" >&2
    exit 1
fi

if [[ -z "$VERSION" ]]; then
    VERSION="$(sed -n 's:.*<revision>\(.*\)</revision>.*:\1:p' "$ROOT_DIR/pom.xml" | head -1)"
fi

if [[ -z "$VERSION" ]]; then
    echo "ERROR: could not determine godot-java version. Pass --version." >&2
    exit 1
fi

CORE_VERSION="$(unzip -p "$APP_JAR" "META-INF/maven/io.github.youngledo/godot-java-core/pom.properties" 2>/dev/null \
    | sed -n 's/^version=//p' \
    | head -1 || true)"
if [[ -n "$CORE_VERSION" && "$CORE_VERSION" != "$VERSION" ]]; then
    echo "ERROR: godot-java version mismatch." >&2
    echo "  app jar godot-java-core: $CORE_VERSION" >&2
    echo "  native artifact:         $VERSION" >&2
    echo "Use matching godot-java-core and godot-java-native versions." >&2
    exit 1
fi

if [[ -z "${JAVA_HOME:-}" ]]; then
    echo "WARNING: JAVA_HOME is not set. Godot may fail to locate the JVM at runtime." >&2
fi

RUNTIME_DIR="$PROJECT_DIR/$OUTPUT_DIR"
mkdir -p "$RUNTIME_DIR"
cp "$APP_JAR" "$RUNTIME_DIR/app.jar"

TMP_DIR=""
cleanup() {
    if [[ -n "$TMP_DIR" && -d "$TMP_DIR" ]]; then
        rm -rf "$TMP_DIR"
    fi
}
trap cleanup EXIT

if [[ -n "$NATIVE_LIB" ]]; then
    if [[ ! -f "$NATIVE_LIB" ]]; then
        echo "ERROR: native library not found: $NATIVE_LIB" >&2
        exit 1
    fi
    cp "$NATIVE_LIB" "$RUNTIME_DIR/$NATIVE_NAME"
elif [[ -n "$NATIVE_ZIP" ]]; then
    if [[ ! -f "$NATIVE_ZIP" ]]; then
        echo "ERROR: native zip not found: $NATIVE_ZIP" >&2
        exit 1
    fi
    unzip -q -o "$NATIVE_ZIP" "$NATIVE_NAME" -d "$RUNTIME_DIR"
else
    TMP_DIR="$(mktemp -d)"
    MVN_ARGS=(-q)
    if [[ -n "$MAVEN_REPO" ]]; then
        MVN_ARGS+=("-Dmaven.repo.local=$MAVEN_REPO")
    fi
    mvn "${MVN_ARGS[@]}" org.apache.maven.plugins:maven-dependency-plugin:3.7.0:copy \
        -Dartifact="io.github.youngledo:godot-java-native:${VERSION}:zip:${CLASSIFIER}" \
        -DoutputDirectory="$TMP_DIR"
    NATIVE_ZIP="$TMP_DIR/godot-java-native-${VERSION}-${CLASSIFIER}.zip"
    if [[ ! -f "$NATIVE_ZIP" ]]; then
        echo "ERROR: resolved native artifact not found: $NATIVE_ZIP" >&2
        exit 1
    fi
    unzip -q -o "$NATIVE_ZIP" "$NATIVE_NAME" -d "$RUNTIME_DIR"
fi

if [[ ! -f "$RUNTIME_DIR/$NATIVE_NAME" ]]; then
    echo "ERROR: native library was not synced: $RUNTIME_DIR/$NATIVE_NAME" >&2
    exit 1
fi

cat > "$RUNTIME_DIR/VERSION" <<EOF
godot-java.version=${VERSION}
godot-java.native.classifier=${CLASSIFIER}
godot-java.native.library=${NATIVE_NAME}
EOF

echo "Synced godot-java runtime:"
echo "  project:    $PROJECT_DIR"
echo "  app jar:    $RUNTIME_DIR/app.jar"
echo "  native lib: $RUNTIME_DIR/$NATIVE_NAME"
echo "  version:    $VERSION"
echo "  classifier: $CLASSIFIER"
