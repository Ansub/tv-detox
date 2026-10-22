#!/bin/sh
# Builds out/tvhome.apk. Needs Java 17 and the Android SDK (platform 34, build-tools 34).
#   macOS:  brew install openjdk@17 && brew install --cask android-commandlinetools
#           sdkmanager "platforms;android-34" "build-tools;34.0.0"
# Use a different app list with: APPS=my-apps.txt sh build.sh
set -e
cd "$(dirname "$0")"

[ -n "$JAVA_HOME" ] || for j in /opt/homebrew/opt/openjdk@17 /usr/local/opt/openjdk@17 /usr/lib/jvm/java-17-openjdk-amd64; do [ -d "$j" ] && JAVA_HOME=$j && break; done
[ -n "$JAVA_HOME" ] && export PATH="$JAVA_HOME/bin:$PATH"
SDK=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}
[ -n "$SDK" ] || for d in /opt/homebrew/share/android-commandlinetools "$HOME/Library/Android/sdk" "$HOME/Android/Sdk"; do [ -d "$d" ] && SDK=$d && break; done
# Releases are built with platform 34 and build-tools 34.0.0. For a local build a newer platform will
# do, and so will newer build-tools: BUILD_TOOLS=36.0.0 sh build.sh (build-tools 34's d8 can't read
# classes from a Java 21 javac).
BT=$SDK/build-tools/${BUILD_TOOLS:-34.0.0}; JAR=$SDK/platforms/android-34/android.jar
[ -f "$JAR" ] || JAR=$(ls "$SDK"/platforms/android-*/android.jar 2>/dev/null | sort -V | tail -1)
[ -d "$BT" ] || BT=$(ls -d "$SDK"/build-tools/*/ 2>/dev/null | sort -V | tail -1 | sed 's|/$||')
[ -f "$JAR" ] && [ -d "$BT" ] || { echo "Android SDK not found. Set ANDROID_HOME, and install platforms;android-34 and build-tools;34.0.0." >&2; exit 1; }

rm -rf out; mkdir -p out/classes out/dex out/gen/com/ansub/tvhome

# Turn the app list into Apps.java.
{
  echo "package com.ansub.tvhome;"
  echo "final class Apps { static final String[] LIST = {"
  grep -v '^[[:space:]]*#' "${APPS:-apps.txt}" | tr -d ' \t\r' | grep -v '^$' | sed 's/.*/    "&",/'
  echo "}; }"
} > out/gen/com/ansub/tvhome/Apps.java

"$BT/aapt2" link -o out/base.apk -I "$JAR" --manifest AndroidManifest.xml --min-sdk-version 21 --target-sdk-version 34
# shellcheck disable=SC2046 # javac and d8 want one argument per file, so the list is split on purpose
javac -nowarn -Xlint:-options -encoding UTF-8 -source 1.8 -target 1.8 -bootclasspath "$JAR:$BT/core-lambda-stubs.jar" \
  -d out/classes $(find src out/gen -name "*.java")
# shellcheck disable=SC2046 # same here
"$BT/d8" --min-api 21 --release --lib "$JAR" --output out/dex $(find out/classes -name '*.class')
cp out/base.apk out/unsigned.apk; (cd out/dex && zip -q ../unsigned.apk classes.dex)
"$BT/zipalign" -f 4 out/unsigned.apk out/aligned.apk

# A signing key is made on first build. Keep key.jks: updates must be signed with the same key.
# (Releases are signed with their own key: set KEYSTORE and KEYSTORE_PASS.)
KS=${KEYSTORE:-key.jks}; PASS=${KEYSTORE_PASS:-tvhome123}
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -storepass "$PASS" -keypass "$PASS" -alias tvhome \
  -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=tvhome" >/dev/null 2>&1
"$BT/apksigner" sign --ks "$KS" --ks-pass "pass:$PASS" --out out/tvhome.apk out/aligned.apk
echo "Built out/tvhome.apk"
