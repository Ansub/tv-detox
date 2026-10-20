#!/bin/sh
# Installs the home screen on your TV and makes it the default. Only needs adb.
#
#   sh install.sh <TV_IP>                          your TV apps, picked for you
#   sh install.sh <TV_IP> "com.netflix.ninja,com.google.android.youtube.tv"
#   sh install.sh <TV_IP> "" "bluetooth,sound,picture"   adds settings shortcuts by the clock
#
# Uses out/tvhome.apk if you built it, otherwise downloads the latest release.
set -e
cd "$(dirname "$0")"

TARGET="$1"; APPS="$2"; QUICK="$3"
[ -n "$TARGET" ] || { echo "Usage: sh install.sh <TV_IP> [\"app1,app2,...\"] [\"bluetooth,sound,picture\"]" >&2; exit 1; }
if [ -z "$APPS" ] && [ -f "apps.txt" ]; then
  APPS=$(grep -v '^[[:space:]]*#' apps.txt | tr -d ' \t\r' | grep -v '^$' | paste -sd, -)
fi
command -v adb >/dev/null 2>&1 || {
  echo "adb is missing. Install it first:" >&2
  echo "  Mac:     brew install android-platform-tools" >&2
  echo "  Windows: https://developer.android.com/tools/releases/platform-tools" >&2
  exit 1; }

# An IP gets :5555 added; an adb serial (like emulator-5554) is used as is.
case "$TARGET" in
  *.*.*.*:*) TV=$TARGET ;;
  *.*.*.*)   TV=$TARGET:5555 ;;
  *)         TV=$TARGET ;;
esac
case "$TV" in *:*) adb connect "$TV" >/dev/null || true ;; esac

state=$(adb -s "$TV" get-state 2>&1 || true)
case "$state" in
  device) ;;
  *unauthorized*) echo "Your TV is asking \"Allow debugging?\". Press Allow (tick \"Always allow\"), then run this again." >&2; exit 1 ;;
  *) echo "Can't reach $TV. Check the TV is on, on the same Wi-Fi, and has USB/network debugging turned on." >&2; exit 1 ;;
esac

APK=out/tvhome.apk
if [ ! -f "$APK" ]; then
  APK=$(mktemp -d)/tvhome.apk
  echo "Downloading the home screen…"
  curl -fsSL -o "$APK" https://github.com/Ansub/tv-detox/releases/latest/download/tvhome.apk
fi

echo "Installing… If your TV shows a Google Play Protect message, follow it on the TV to allow the install."
if ! out=$(adb -s "$TV" install -r "$APK" 2>&1); then
  case "$out" in
    *UPDATE_INCOMPATIBLE*) echo "Replacing an older copy signed with a different key…"
                           adb -s "$TV" uninstall com.ansub.tvhome >/dev/null; adb -s "$TV" install "$APK" ;;
    *) echo "$out" >&2; exit 1 ;;
  esac
fi

if [ -n "$APPS" ] || [ -n "$QUICK" ]; then
  # The stamp makes the app apply this list once, even if Android later replays this start.
  set -- --es stamp "$(date +%s)"
  [ -n "$APPS" ] && set -- "$@" --es apps "\"$APPS\""
  [ -n "$QUICK" ] && set -- "$@" --es quick "\"$QUICK\""
  adb -s "$TV" shell am start -n com.ansub.tvhome/.MainActivity "$@" >/dev/null
fi

ME=com.ansub.tvhome/.MainActivity
# Keeps adb's exit status, so a failed command on the TV counts as failed here.
sh_tv() { _rc=0; _out=$(adb -s "$TV" shell "$@") || _rc=$?; [ -z "$_out" ] || printf '%s\n' "$_out" | tr -d '\r'; return $_rc; }
current_home() { sh_tv cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1; }

# Make it the default home: the role (Android 10+) and the classic way.
if [ "$(sh_tv getprop ro.build.version.sdk)" -ge 29 ]; then
  sh_tv cmd role add-role-holder --user 0 android.app.role.HOME com.ansub.tvhome >/dev/null 2>&1 || true
fi
sh_tv cmd package set-home-activity "$ME" >/dev/null 2>&1 || true

# Known TV home screens that are safe to turn off.
LAUNCHERS="com.google.android.tvlauncher com.google.android.apps.tv.launcherx com.google.android.leanbacklauncher
  com.mitv.tvhome com.amazon.tv.launcher"

# Only known launchers get turned off. Settings, setup and System UI also answer Home, and
# the TV needs them to start, so they, unknown apps and anything always running stay on.
is_launcher() {
  case "$1" in
    com.ansub.tvhome|com.android.settings|com.android.tv.settings|com.android.systemui|com.android.provision) return 1 ;;
    *setup*|*wraith*|*provision*|*systemui*|*tvcenter*|*.tv.framework*|*.tvinput*) return 1 ;;
  esac
  case " $(echo $LAUNCHERS) " in *" $1 "*) ;; *) return 1 ;; esac
  # If we can't read its flags, or it always runs, leave it on.
  flags=$(sh_tv dumpsys package "$1" 2>/dev/null | sed -n '/pkgFlags=\[/{p;q;}')
  [ -n "$flags" ] || return 1
  case "$flags" in *PERSISTENT*) return 1 ;; esac
  return 0
}

is_off() { sh_tv pm list packages -d "$1" | grep -Fqx "package:$1"; }

# Turns apps off (not uninstall). Each one is saved on the TV first, so undo.sh can always
# turn it back on. If it can't be saved, it's left on.
turn_off() {
  for p in "$@"; do
    if ! sh_tv "echo $p >> /data/local/tmp/tvhome-disabled.txt"; then
      echo "Leaving $p on: couldn't save it for undo.sh." >&2; continue
    fi
    sh_tv pm disable-user --user 0 "$p" >/dev/null 2>&1 || true
    is_off "$p" || echo "Couldn't turn $p off." >&2
  done
  # Android saves this a few seconds later. Restarting before then can undo it.
  echo "Saving the change on the TV, this takes 15 seconds…"
  sleep 15
  sh_tv cmd package set-home-activity "$ME" >/dev/null 2>&1 || true
}

# Google TV's setup app. It's handled on its own below.
WRAITH=com.google.android.tungsten.setupwraith
home_apps() { sh_tv cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2>/dev/null \
  | grep / | sed 's|/.*||; s/^ *//' | sort -u; }

# Most TVs' own launchers still win when you press Home. The fix is to turn them off.
if [ "$(current_home)" != "$ME" ]; then
  others=""
  for p in $(home_apps); do
    if is_launcher "$p"; then others="$others $p"; continue; fi
    case "$p" in com.ansub.tvhome|com.android.settings|com.android.tv.settings|$WRAITH) ;; *) echo "Leaving $p on: it may be needed for your TV to start." ;; esac
  done
  others=$(echo $others | tr ' ' '\n')
  if [ -n "$others" ]; then
    echo "Your TV's own home screen keeps taking over:"
    echo "$others" | sed 's/^/  /'
    answer=${YES:+y}
    if [ -z "$answer" ]; then
      printf "Turn it off so Home opens your new home screen? You can undo this with undo.sh. [y/N] "
      read answer </dev/tty || answer=n
    fi
    case "$answer" in
      y|Y|yes) turn_off $others ;;
      *) echo "Left it on. Your TV may keep opening its own home screen." ;;
    esac
  fi
fi

# On some Google TVs (like Sony BRAVIA) the setup app wins Home once Google TV's home screen
# is off. It's only offered if Home opens exactly that app, Google TV's home screen is already
# off, the TV has finished its first setup, and the system doesn't keep it always running.
# YES=1 doesn't cover it: it needs its own answer, or YES_SETUP=1.
LAUNCHERX=com.google.android.apps.tv.launcherx
off_list=$(sh_tv pm list packages -d "$LAUNCHERX" 2>/dev/null) || off_list=
if [ "$(current_home | sed 's|/.*||')" = "$WRAITH" ] \
    && printf '%s\n' "$off_list" | grep -Fqx "package:$LAUNCHERX"; then
  flags=$(sh_tv dumpsys package "$WRAITH" 2>/dev/null | sed -n '/pkgFlags=\[/{p;q;}')
  [ -n "$flags" ] || flags=PERSISTENT
  if [ "$(sh_tv settings get global device_provisioned)" != 1 ] || [ "$(sh_tv settings get secure user_setup_complete)" != 1 ]; then
    echo "Leaving $WRAITH on: your TV hasn't finished its first setup."
  else case "$flags" in
    *PERSISTENT*) echo "Leaving $WRAITH on: it may be needed for your TV to start." ;;
    *)
      echo "Google TV's setup app ($WRAITH) is taking over Home."
      echo "Turning it off fixed this on a Sony BRAVIA, but it hasn't been tested on every TV."
      echo "If anything goes wrong, undo.sh turns it back on."
      if [ "$YES_SETUP" = 1 ]; then answer=y
      elif [ -n "$YES" ]; then answer=n
      else
        printf "Turn off the setup app too? [y/N] "
        read answer </dev/tty || answer=n
      fi
      case "$answer" in
        y|Y|yes) turn_off "$WRAITH" ;;
        *) echo "Left it on. Your TV may keep opening its setup app when you press Home. To turn it off later, run this again with YES_SETUP=1." ;;
      esac ;;
  esac; fi
fi

adb -s "$TV" shell input keyevent KEYCODE_HOME
if [ "$(current_home)" = "$ME" ]; then
  echo "Done. Press Home on your remote to see it."
else
  echo "Installed, but your TV still opens its own home screen. If it asks which home screen to use, pick \"Home\" and choose Always."
fi
