#!/bin/sh
# Installs the home screen on an Android TV (real or emulator) exactly like a user would,
# then checks it works. Usage: sh ci/smoke-test.sh <adb serial or TV_IP> [screenshot.png]
set -e
cd "$(dirname "$0")/.."
TV="$1"; SHOT="${2:-smoke.png}"
fail() {
  echo "FAIL: $*" >&2
  echo "-- on screen: $(focus)" >&2
  adb -s "$TV" logcat -d -b crash 2>/dev/null | grep -A12 "FATAL EXCEPTION" | head -40 >&2
  adb -s "$TV" exec-out screencap -p > "${SHOT%.png}-fail.png" 2>/dev/null || true
  exit 1
}
# Waits up to 15s for the home screen to be in front.
wait_home() { for _ in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15; do focus | grep -q com.ansub.tvhome && return 0; sleep 1; done; return 1; }
ui() { adb -s "$TV" shell "uiautomator dump /sdcard/ui.xml" >/dev/null 2>&1; adb -s "$TV" shell "cat /sdcard/ui.xml"; }
focus() { adb -s "$TV" shell dumpsys window | grep -m1 mCurrentFocus; }

echo "== Android $(adb -s "$TV" shell getprop ro.build.version.release | tr -d '\r'), $(adb -s "$TV" shell wm size | tail -1 | tr -d '\r')"

echo "== install"
YES=1 sh install.sh "$TV"
sleep 3
home=$(adb -s "$TV" shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1 | tr -d '\r')
[ "$home" = "com.ansub.tvhome/.MainActivity" ] || fail "default home is $home"
adb -s "$TV" shell input keyevent KEYCODE_HOME
wait_home || fail "home screen not in front"

echo "== home screen shows a clock and Settings"
x=$(ui)
echo "$x" | grep -q 'text="Settings"' || fail "no Settings tile"
echo "$x" | grep -qE 'text="[0-9]{1,2}:[0-9]{2}"' || fail "no clock"
tiles=$(echo "$x" | grep -o 'focusable="true"' | wc -l | tr -d ' ')
echo "   $tiles tiles"

echo "== remote: move right, open Settings, come back with Home"
adb -s "$TV" shell input keyevent KEYCODE_DPAD_RIGHT
adb -s "$TV" shell am start -a android.settings.SETTINGS >/dev/null; sleep 3
adb -s "$TV" shell input keyevent KEYCODE_HOME
wait_home || fail "Home didn't come back"

echo "== custom app list"
# Use whatever TV app this device has (test images may have none).
app=$(adb -s "$TV" shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER \
  | tr -d '\r' | grep / | sed 's|/.*||; s/^ *//' | grep -vx com.ansub.tvhome | head -1)
if [ -n "$app" ]; then
  YES=1 sh install.sh "$TV" "$app"
  adb -s "$TV" shell input keyevent KEYCODE_HOME
  wait_home || fail "home screen not in front after custom list"
  n=$(ui | grep -o 'focusable="true"' | wc -l | tr -d ' ')
  [ "$n" -ge 2 ] || fail "custom app $app didn't show up"
  echo "   $app shows up"
else
  echo "   skipped: this device has no TV apps"
fi

adb -s "$TV" exec-out screencap -p > "$SHOT"

echo "== check.sh refuses a list it can't read"
list=$(mktemp)
echo "# nothing to check" > "$list"
sh check.sh "$TV" "$list" 2>&1 | grep -q "Can't read" && fail "check.sh refused a list it can read"
rm -f "$list"
rc=0; sh check.sh "$TV" "$list" >/dev/null 2>&1 || rc=$?
[ "$rc" = 2 ] || fail "check.sh exit $rc for a list it can't read, expected 2"

echo "== candidates.sh reads the TV and changes nothing"
before=$(adb -s "$TV" shell pm list packages -d | tr -d '\r' | sort)
out=$(sh candidates.sh "$TV") || fail "candidates.sh failed"
echo "$out" | grep -q "^Memory: " || fail "candidates.sh printed no health line"
echo "$out" | grep -q "^never	on	com.android.systemui" || fail "candidates.sh didn't mark System UI never"
echo "$out" | grep -q "^never	on	com.google.android.tvlauncher\|^keep	on	com.ansub.tvhome" || fail "candidates.sh: no launcher or home screen line"
[ "$(adb -s "$TV" shell pm list packages -d | tr -d '\r' | sort)" = "$before" ] || fail "candidates.sh changed what's off"
sh candidates.sh "$TV" --health | grep -q "^Apps: " || fail "candidates.sh --health printed no Apps line"

echo "== apply.sh makes the cleanup and undo.sh --cleanup puts it back"
# A safe app this image has, if any; the setting is one nothing reads, so it can't upset the emulator.
victim=$(adb -s "$TV" shell pm list packages | tr -d '\r' | sed -n 's/^package://p' \
  | grep -x 'com.android.htmlviewer\|com.android.printspooler\|com.google.android.feedback' | head -1)
plan=$(mktemp)
{
  echo "# smoke test"
  [ -z "$victim" ] || echo "off $victim"
  echo "off com.android.systemui"
  echo "global tvhome_smoke 1"
  echo "nonsense line"
} > "$plan"
out=$(sh apply.sh "$TV" "$plan" --dry-run) || fail "apply.sh --dry-run failed: $out"
echo "$out" | grep -q "refuse  off com.android.systemui" || fail "apply.sh didn't refuse System UI: $out"
echo "$out" | grep -q "refuse  nonsense" || fail "apply.sh didn't refuse a bad line: $out"
adb -s "$TV" shell "ls /data/local/tmp/tvhome-cleanup.txt" 2>/dev/null | grep -q cleanup && fail "a dry run wrote the undo list"
rc=0; out=$(YES=1 sh apply.sh "$TV" "$plan" 2>&1) || rc=$?
[ "$rc" = 1 ] || fail "apply.sh exit $rc with a refused line in the list, expected 1: $out"
echo "$out" | grep -q "set     global tvhome_smoke = 1 (was null)" || fail "apply.sh didn't set the setting: $out"
[ "$(adb -s "$TV" shell settings get global tvhome_smoke | tr -d '\r')" = 1 ] || fail "setting not on the TV"
if [ -n "$victim" ]; then
  adb -s "$TV" shell pm list packages -d | tr -d '\r' | grep -qx "package:$victim" || fail "$victim is not off"
  adb -s "$TV" shell "cat /data/local/tmp/tvhome-cleanup.txt" | tr -d '\r' | grep -qx "off $victim" || fail "$victim is not on the undo list"
fi
adb -s "$TV" shell "cat /data/local/tmp/tvhome-cleanup.txt" | tr -d '\r' | grep -qx "global tvhome_smoke 1 null" || fail "setting not on the undo list"
# Twice is the same as once: already-made changes are skipped, the undo list keeps the first old value.
YES=1 sh apply.sh "$TV" "$plan" >/dev/null 2>&1 || true
[ "$(adb -s "$TV" shell "cat /data/local/tmp/tvhome-cleanup.txt" | tr -d '\r' | grep -c 'tvhome_smoke')" = 1 ] || fail "undo list has the setting twice"
sh check.sh "$TV" | grep -q "ok    global tvhome_smoke = 1" || fail "check.sh doesn't see apply.sh's setting"
adb -s "$TV" shell settings put global tvhome_smoke 2 >/dev/null
sh check.sh "$TV" --fix | grep -q "fixed global tvhome_smoke = 1" || fail "check.sh --fix didn't put the setting back"
out=$(sh undo.sh "$TV" --cleanup) || fail "undo.sh --cleanup failed: $out"
[ "$(adb -s "$TV" shell settings get global tvhome_smoke | tr -d '\r')" = null ] || fail "undo didn't delete the setting"
if [ -n "$victim" ] && adb -s "$TV" shell pm list packages -d | tr -d '\r' | grep -qx "package:$victim"; then fail "undo didn't turn $victim back on"; fi
adb -s "$TV" shell "ls /data/local/tmp/tvhome-cleanup.txt" 2>/dev/null | grep -q cleanup && fail "undo left the undo list behind"
focus | grep -q com.ansub.tvhome || adb -s "$TV" shell input keyevent KEYCODE_HOME
wait_home || fail "home screen gone after undo --cleanup"
rm -f "$plan"
echo "   cleanup applied, checked, fixed and undone"

echo "== undo puts the TV back"
sh undo.sh "$TV" >/dev/null
sleep 3
home=$(adb -s "$TV" shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1 | tr -d '\r')
case "$home" in *com.ansub.tvhome*|"") fail "after undo, home is '$home'" ;; esac
echo "   home is back to $home"
echo "PASS ($SHOT)"
