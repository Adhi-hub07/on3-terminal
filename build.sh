#!/bin/bash
set -e

echo "=== On3 Terminal Build Script ==="
echo ""

# Check for Android SDK
if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
    echo "WARNING: ANDROID_HOME or ANDROID_SDK_ROOT not set."
    echo "Set the path to your Android SDK, e.g.:"
    echo "  export ANDROID_HOME=~/Android/Sdk"
    echo ""
fi

echo "To build the app:"
echo "  1. Set ANDROID_HOME to your Android SDK path"
echo "  2. Run: ./gradlew assembleDebug"
echo ""
echo "The APK will be at: app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "=== Project Structure ==="
echo "on3-terminal/"
echo "  app/                     - Main Android application"
echo "    src/main/java/         - App code (Activity, Service, View)"
echo "    src/main/res/          - Layouts, themes, strings"
echo "  terminal-core/           - Terminal emulator engine library"
echo "    src/main/java/         - Core: Emulator, Buffer, Session, JNI"
echo "    src/main/jni/          - C code for PTY creation"
echo "  build.gradle.kts         - Root build file"
echo "  settings.gradle.kts      - Project settings"
