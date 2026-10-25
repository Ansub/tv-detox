#!/bin/sh
# Checks a TV before anything is changed: what it is, which home screen it runs, and how safe it
# is to swap that home screen. Changes nothing on the TV.
#
#   sh supports.sh <TV_IP>
#
# Exit status: 0 for a report, 2 if the TV couldn't be reached.
set -e

TARGET="$1"
[ -n "$TARGET" ] || { echo "Usage: sh supports.sh <TV_IP>" >&2; exit 1; }
command -v adb >/dev/null 2>&1 || {
  echo "adb is missing. Install it first:" >&2
  echo "  Mac:     brew install android-platform-tools" >&2
  echo "  Windows: https://developer.android.com/tools/releases/platform-tools" >&2
  exit 2; }

# An IP gets :5555 added; an adb serial (like emulator-5554) is used as is.
case "$TARGET" in
  *.*.*.*:*) TV=$TARGET ;;
  *.*.*.*)   TV=$TARGET:5555 ;;
  *)         TV=$TARGET ;;
esac
case "$TV" in *:*) adb connect "$TV" >/dev/null 2>&1 || true ;; esac

state=$(adb -s "$TV" get-state 2>&1 || true)
case "$state" in
  device) ;;
  *unauthorized*) echo "Your TV is asking \"Allow debugging?\". Press Allow (tick \"Always allow\"), then run this again." >&2; exit 2 ;;
  *) echo "Can't reach $TV. Check the TV is on, on the same Wi-Fi, and has USB/network debugging turned on." >&2; exit 2 ;;
esac

sh_tv() { adb -s "$TV" shell "$@" </dev/null | tr -d '\r'; }
prop() { sh_tv getprop "$1"; }
has() { sh_tv pm list packages "$1" | grep -Fqx "package:$1"; }
homes() { sh_tv cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME | grep / | sed 's|/.*||; s/^ *//' | sort -u; }
count() { grep -c / || true; }

MAKER=$(prop ro.product.manufacturer)
MODEL=$(prop ro.product.model)
RELEASE=$(prop ro.build.version.release)
SDK=$(prop ro.build.version.sdk)
MEM=$(sh_tv cat /proc/meminfo | sed -n 's/^MemTotal:[[:space:]]*\([0-9]*\) kB/\1/p')
CURRENT=$(sh_tv cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1)
OFF=$(sh_tv pm list packages -d | grep -c '^package:' || true)
TVAPPS=$(sh_tv cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER | count)

echo "== TV"
echo "   $MAKER $MODEL, Android $RELEASE (SDK $SDK)"
if [ -n "$MEM" ]; then echo "   $((MEM / 1024)) MB of RAM"; fi
echo "   $TVAPPS TV apps installed, $OFF apps turned off"

echo "== Home screen"
echo "   now: ${CURRENT:-unknown}"
for p in $(homes); do echo "   also answers Home: $p"; done

WRAITH=com.google.android.tungsten.setupwraith
LAUNCHERX=com.google.android.apps.tv.launcherx
google_tv=
if has "$LAUNCHERX"; then google_tv=1; fi

echo "== Verdict"
if [ -n "$google_tv" ]; then
  echo "   Google TV. The cleanup works; replacing its home screen is the risky part."
  echo "   install.sh keeps Google TV's own home screen on unless you run it with GOOGLE_TV=1,"
  echo "   and that is only safe if you can reconnect over USB or port 5555."
  if has "$WRAITH"; then
    echo "   Its setup app ($WRAITH) can take over Home once that launcher is off."
  fi
elif [ "$TVAPPS" = 0 ]; then
  echo "   No TV apps found, so this may not be an Android TV. The cleanup half of this project"
  echo "   works on any Android device; the home screen is made for a remote, not a touchscreen."
else
  echo "   Android TV. Turning off its own home screen is what makes Home open this one."
  echo "   That is the step people undo most often, so keep the TV reachable while you run it."
fi

if [ -n "$CURRENT" ] && [ "$CURRENT" != "com.ansub.tvhome/.MainActivity" ]; then
  echo "   This home screen is not installed as Home yet."
fi

echo "== Next"
echo "   sh candidates.sh $TARGET        what is safe to turn off (changes nothing)"
echo "   sh install.sh $TARGET           install and make it Home"
echo "   sh undo.sh $TARGET              put the TV's own home screen back"
