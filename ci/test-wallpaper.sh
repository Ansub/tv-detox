#!/bin/sh
# Build and run wallpaper instrumentation on an emulator, without the TV installer.
# Usage: sh ci/test-wallpaper.sh emulator-5554 (run sh build.sh first).
set -eu
cd "$(dirname "$0")/.."
TV=${1:?Usage: sh ci/test-wallpaper.sh emulator-5554}
SDK=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}
[ -n "$SDK" ] || for d in "$HOME/Library/Android/sdk" "$HOME/Android/Sdk"; do [ ! -d "$d" ] || { SDK=$d; break; }; done
BT=$SDK/build-tools/${ANDROID_BUILD_TOOLS:-34.0.0}
JAR=$SDK/platforms/${ANDROID_PLATFORM:-android-34}/android.jar
ADB=$SDK/platform-tools/adb
[ -f "$JAR" ] && [ -d out/classes ] || { echo "Build the app first and set ANDROID_HOME to its SDK." >&2; exit 1; }
[ "$("$ADB" -s "$TV" shell getprop ro.kernel.qemu | tr -d '\r')" = 1 ] || {
  echo "Refusing to install test APKs on a physical device. Use an Android emulator." >&2; exit 1;
}
TEST=out/wallpaper-tests
mkdir -p "$TEST/classes" "$TEST/dex" "$TEST/assets"
cp ci/fixtures/*.png "$TEST/assets/"
echo 'This is deliberately not an image.' > "$TEST/assets/invalid.jpg"
cat > "$TEST/AndroidManifest.xml" <<'MANIFEST'
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.ansub.tvhome.wallpapertest">
  <uses-sdk android:minSdkVersion="21" android:targetSdkVersion="34" />
  <application android:label="Wallpaper tests" />
  <instrumentation android:name="com.ansub.tvhome.WallpaperTest" android:targetPackage="com.ansub.tvhome" android:functionalTest="true" />
</manifest>
MANIFEST
javac -nowarn -encoding UTF-8 -source 1.8 -target 1.8 -bootclasspath "$JAR:$BT/core-lambda-stubs.jar" -classpath out/classes \
  -d "$TEST/classes" ci/WallpaperTest.java
"$BT/d8" --min-api 21 --lib "$JAR" --classpath out/classes --output "$TEST/dex" $(find "$TEST/classes" -name '*.class')
"$BT/aapt2" link -o "$TEST/base.apk" -I "$JAR" --manifest "$TEST/AndroidManifest.xml" -A "$TEST/assets"
cp "$TEST/base.apk" "$TEST/unsigned.apk"
(cd "$TEST/dex" && zip -q ../unsigned.apk classes.dex)
"$BT/zipalign" -f 4 "$TEST/unsigned.apk" "$TEST/aligned.apk"
"$BT/apksigner" sign --ks "${KEYSTORE:-key.jks}" --ks-pass "pass:${KEYSTORE_PASS:-tvhome123}" \
  --out "$TEST/tests.apk" "$TEST/aligned.apk"
"$ADB" -s "$TV" install -r out/tvhome.apk
"$ADB" -s "$TV" install -r "$TEST/tests.apk"
"$ADB" -s "$TV" shell am instrument -w -r com.ansub.tvhome.wallpapertest/com.ansub.tvhome.WallpaperTest > "$TEST/results.txt"
cat "$TEST/results.txt"
grep -q 'PASS:' "$TEST/results.txt" && grep -q 'INSTRUMENTATION_CODE: -1' "$TEST/results.txt"
