#!/bin/sh
# Builds ci/fake-input/out/fakeinput.apk: a TV input service for testing on an emulator that has
# no HDMI inputs of its own. Needs the same Java 17 and Android SDK (platform 34) as the launcher.
#   sh ci/fake-input/build.sh
set -e
cd "$(dirname "$0")"

[ -n "$JAVA_HOME" ] || for j in /opt/homebrew/opt/openjdk@17 /usr/local/opt/openjdk@17 /usr/lib/jvm/java-17-openjdk-amd64; do [ -d "$j" ] && JAVA_HOME=$j && break; done
[ -n "$JAVA_HOME" ] && export PATH="$JAVA_HOME/bin:$PATH"
SDK=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}
[ -n "$SDK" ] || for d in /opt/homebrew/share/android-commandlinetools "$HOME/Library/Android/sdk" "$HOME/Android/Sdk"; do [ -d "$d" ] && SDK=$d && break; done
BT=$SDK/build-tools/34.0.0; JAR=$SDK/platforms/android-34/android.jar
[ -f "$JAR" ] && [ -d "$BT" ] || { echo "Android SDK not found. Set ANDROID_HOME, and install platforms;android-34 and build-tools;34.0.0." >&2; exit 1; }

rm -rf out; mkdir -p out/classes out/dex
"$BT/aapt2" compile --dir res -o out/res.zip
"$BT/aapt2" link -o out/base.apk -I "$JAR" --manifest AndroidManifest.xml --min-sdk-version 21 \
  --target-sdk-version 34 -R out/res.zip --auto-add-overlay
# shellcheck disable=SC2046 # javac and d8 want one argument per file, so the list is split on purpose
javac -nowarn -Xlint:-options -encoding UTF-8 -source 1.8 -target 1.8 -bootclasspath "$JAR:$BT/core-lambda-stubs.jar" \
  -d out/classes $(find src -name "*.java")
# shellcheck disable=SC2046 # same here
"$BT/d8" --min-api 21 --release --lib "$JAR" --output out/dex $(find out/classes -name '*.class')
cp out/base.apk out/unsigned.apk; (cd out/dex && zip -q ../unsigned.apk classes.dex)
"$BT/zipalign" -f 4 out/unsigned.apk out/aligned.apk

# Signed with the launcher's own key, so this never ends up installed by accident anywhere else.
KS=${KEYSTORE:-../../key.jks}; PASS=${KEYSTORE_PASS:-tvhome123}
"$BT/apksigner" sign --ks "$KS" --ks-pass "pass:$PASS" --out out/fakeinput.apk out/aligned.apk
echo "Built out/fakeinput.apk"
