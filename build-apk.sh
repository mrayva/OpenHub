#!/usr/bin/env bash
# Builds OpenHub into an APK and copies it to Google Drive.
#
# Usage:
#   ./build-apk.sh              # build normal-flavor debug APK (default)
#   ./build-apk.sh release      # build normal-flavor release APK (unsigned - no
#                                # signingConfig is defined for release in this project)
#   ./build-apk.sh fullname     # build fullName-flavor debug APK instead
#
# Unlike FastHub-RE, OpenHub needs no secrets.properties or custom keystore to
# build at all: debug uses Android's auto-generated ~/.android/debug.keystore,
# and public.properties' placeholder OAuth values only affect whether the (now
# secondary) OAuth login option works - login via Personal Access Token, the
# primary method in this fork, needs no credentials whatsoever.
#
# First run downloads the Android SDK command-line tools + platform 36 +
# build-tools 36.0.0 into $ANDROID_HOME (~/Android/Sdk by default) if not
# already present. Subsequent runs reuse them and are much faster.

set -euo pipefail

ARG="${1:-debug}"
FLAVOR="Normal"
BUILD_TYPE="Debug"
case "$ARG" in
    debug) ;;
    release) BUILD_TYPE="Release" ;;
    fullname) FLAVOR="FullName" ;;
    *) echo "Usage: $0 [debug|release|fullname]" >&2; exit 1 ;;
esac

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
DEST_DIR="${OPENHUB_APK_DEST:-/mnt/g/My Drive/apk}"

CMDLINE_TOOLS_VERSION="11076708"
PLATFORM="android-36"
BUILD_TOOLS="36.0.0"

echo "==> Project dir: $PROJECT_DIR"
echo "==> Android SDK: $ANDROID_HOME"

# --- 1. JDK 17+ (JDK 21 works fine too) ---
CURRENT_JAVA_MAJOR="$(java -version 2>&1 | head -1 | grep -oE '"[0-9]+' | tr -d '"' || true)"
if [ -n "$CURRENT_JAVA_MAJOR" ] && [ "$CURRENT_JAVA_MAJOR" -ge 17 ] 2>/dev/null; then
    JDK_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")"
else
    JDK_HOME="$(update-alternatives --list java 2>/dev/null | grep -E 'java-(1[7-9]|2[0-9])' | head -1 | sed 's#/bin/java##')"
    if [ -z "$JDK_HOME" ] && [ -d /usr/lib/jvm/java-17-openjdk-amd64 ]; then
        JDK_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
    fi
    if [ -z "$JDK_HOME" ]; then
        echo "==> Installing OpenJDK 17 (no JDK 17+ found; required to run this project's Gradle build)..."
        sudo apt-get update -qq
        sudo apt-get install -y -qq openjdk-17-jdk-headless
        JDK_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
    fi
fi
export JAVA_HOME="$JDK_HOME"
export PATH="$JAVA_HOME/bin:$PATH"
echo "==> Using JAVA_HOME=$JAVA_HOME"

# --- 2. Android SDK command-line tools ---
if [ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
    echo "==> Installing Android SDK command-line tools..."
    mkdir -p "$ANDROID_HOME/cmdline-tools"
    TMP_ZIP="$(mktemp -d)/cmdline-tools.zip"
    curl -sSL -o "$TMP_ZIP" \
        "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
    unzip -q "$TMP_ZIP" -d "$ANDROID_HOME/cmdline-tools"
    rm -rf "$ANDROID_HOME/cmdline-tools/latest"
    mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
fi
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"

# --- 3. SDK platform, build-tools, platform-tools ---
if [ ! -d "$ANDROID_HOME/platforms/$PLATFORM" ] || [ ! -d "$ANDROID_HOME/build-tools/$BUILD_TOOLS" ]; then
    echo "==> Installing SDK packages (platform $PLATFORM, build-tools $BUILD_TOOLS)..."
    yes | "$SDKMANAGER" --sdk_root="$ANDROID_HOME" --licenses > /dev/null 2>&1 || true
    "$SDKMANAGER" --sdk_root="$ANDROID_HOME" "platform-tools" "platforms;$PLATFORM" "build-tools;$BUILD_TOOLS"
fi

# --- 4. local.properties ---
echo "sdk.dir=$ANDROID_HOME" > "$PROJECT_DIR/local.properties"

# --- 5. Build ---
cd "$PROJECT_DIR"
GRADLE_TASK="assemble${FLAVOR}${BUILD_TYPE}"
echo "==> Running ./gradlew $GRADLE_TASK ..."
./gradlew "$GRADLE_TASK" --no-daemon

APK_PATH=$(find "app/build/outputs/apk/$(echo "$FLAVOR" | tr '[:upper:]' '[:lower:]')/$(echo "$BUILD_TYPE" | tr '[:upper:]' '[:lower:]')" -iname "*.apk" | head -1)

if [ -z "$APK_PATH" ] || [ ! -f "$APK_PATH" ]; then
    echo "==> Build finished but no APK was found." >&2
    exit 1
fi

echo "==> Built: $APK_PATH"

# --- 6. Copy to destination (Google Drive by default) ---
if [ -d "$DEST_DIR" ]; then
    DEST_NAME="OpenHub-${FLAVOR}-${BUILD_TYPE}.apk"
    cp "$APK_PATH" "$DEST_DIR/$DEST_NAME"
    echo "==> Copied to: $DEST_DIR/$DEST_NAME"
else
    echo "==> Destination '$DEST_DIR' not found, skipping copy. APK is at: $APK_PATH"
fi
