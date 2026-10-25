#!/bin/sh
# Checks your TV is still cleaned up, and with --fix puts back whatever an update undid.
#
#   sh check.sh <TV_IP>                   check the home screen
#   sh check.sh <TV_IP> --fix             and fix it
#   sh check.sh <TV_IP> --fix my-tv.txt   also check your own list of changes
#
# The list has one change per line (# starts a comment):
#   off com.example.app                        the app stays turned off
#   global window_animation_scale 0            a setting (global, secure or system)
#   device_config activity_manager max_cached_processes 4
set -e

TARGET="$1"; FIX=; LIST=
shift 2>/dev/null || true
for a in "$@"; do case "$a" in --fix) FIX=1 ;; *) LIST=$a ;; esac; done
[ -n "$TARGET" ] || { echo "Usage: sh check.sh <TV_IP> [--fix] [list.txt]" >&2; exit 1; }
[ -z "$LIST" ] || { [ -f "$LIST" ] && [ -r "$LIST" ]; } || { echo "Can't read $LIST." >&2; exit 2; }
case "$TARGET" in
  *.*.*.*:*) TV=$TARGET ;;
  *.*.*.*)   TV=$TARGET:5555 ;;
  *)         TV=$TARGET ;;
esac
case "$TV" in *:*) adb connect "$TV" >/dev/null || true ;; esac
[ "$(adb -s "$TV" get-state 2>/dev/null)" = device ] || { echo "Can't reach $TV." >&2; exit 2; }

sh_tv() { adb -s "$TV" shell "$@" </dev/null | tr -d '\r'; } # adb would eat the list below
bad=0
home_unchecked=
# Prints a result line. With --fix, runs the fix and checks again.
check() { # name, test command, fix command
  if eval "$2"; then echo "  ok    $1"; return; fi
  if [ -n "$FIX" ] && eval "$3" >/dev/null 2>&1 && eval "$2"; then echo "  fixed $1"; return; fi
  echo "  WRONG $1"; bad=$((bad + 1))
}
is_off() { sh_tv pm list packages -d "$1" | grep -qx "package:$1"; }
installed() { sh_tv pm list packages "$1" | grep -qx "package:$1"; }

ME=com.ansub.tvhome/.MainActivity
home() { sh_tv cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1; }
# Read a foreground component; an empty result cannot verify Home routing.
foreground() {
  front=$(sh_tv dumpsys window 2>/dev/null | grep -m1 mCurrentFocus)
  case "$front" in
    */*) ;;
    *) front=$(sh_tv dumpsys activity activities 2>/dev/null | grep -m1 -E 'mResumedActivity|topResumedActivity') ;;
  esac
  printf '%s\n' "$front" | grep -oE '[[:alnum:]_.]+/[[:alnum:]_.$]+' | head -1
}
# Philips routes Home through this app. Verify a transition from another screen.
home_opens_me() {
  case "$1" in
    "$ME") return 0 ;;
    org.droidtv.homeintentresolver/*) ;;
    *) return 1 ;;
  esac
  echo "Testing Home: the Philips router probe opens Settings and presses Home."
  # Only the live probe needs adb's exit status; repairs use sh_tv and verify the result.
  adb -s "$TV" shell am start -W -a android.settings.SETTINGS </dev/null >/dev/null 2>&1 || return 1
  away=
  for _try in 1 2 3 4 5; do
    sleep 1
    front=$(foreground)
    case "$front" in
      com.ansub.tvhome/*|"") ;;
      */*) away=1; break ;;
    esac
  done
  [ -n "$away" ] || return 1
  adb -s "$TV" shell input keyevent KEYCODE_HOME </dev/null >/dev/null 2>&1 || return 1
  for _try in 1 2 3 4 5; do
    sleep 1
    front=$(foreground)
    case "$front" in
      "$ME"|com.ansub.tvhome/com.ansub.tvhome.MainActivity) return 0 ;;
    esac
  done
  return 1
}
echo "Home screen"
# The TV's own launchers first: while one is on, Home can't be fixed.
# In the same argument as cat: Git Bash on Windows turns an argument that starts with / into a Windows path.
turned_off=$(sh_tv "cat /data/local/tmp/tvhome-disabled.txt" 2>/dev/null | sort -u)
for p in $turned_off; do
  check "$p stays off" "is_off $p" "sh_tv pm disable-user --user 0 $p"
done
# On Google TV, install.sh keeps Google TV's home screen on unless GOOGLE_TV=1, so Home opening it is expected.
LAUNCHERX=com.google.android.apps.tv.launcherx
winner=$(home)
if [ -z "$FIX" ] && [ "${winner%%/*}" = org.droidtv.homeintentresolver ]; then
  if [ -n "$turned_off" ] && [ "$bad" -eq 0 ]; then
    echo "  ??    Home goes through Philips' Home router: not checked (run check.sh --fix to press Home and test it)"
    home_unchecked=1
  elif [ -z "$turned_off" ]; then
    echo "  WRONG Home goes through Philips' Home router and nothing install.sh turned off is off: Home likely opens the TV's own home screen (run install.sh again, or check.sh --fix to test Home)"
    bad=$((bad + 1))
  else
    echo "  WRONG Home goes through Philips' Home router, but disabled apps came back on"
    bad=$((bad + 1))
  fi
elif [ "${winner%%/*}" != org.droidtv.homeintentresolver ] && [ "$winner" != "$ME" ] && installed "$LAUNCHERX" && ! is_off "$LAUNCHERX" && ! echo "$turned_off" | grep -qx "$LAUNCHERX"; then
  echo "  ok    Home opens Google TV's home screen (kept on, as install.sh said; GOOGLE_TV=1 turns it off)"
else
  before_home=$bad
  check "Home opens this home screen" 'home_opens_me "$(home)"' \
    'sh_tv cmd role add-role-holder --user 0 android.app.role.HOME com.ansub.tvhome; sh_tv cmd package set-home-activity "$ME"'
  if [ -n "$FIX" ] && [ "${winner%%/*}" = org.droidtv.homeintentresolver ] && [ "$bad" -gt "$before_home" ]; then
    echo "If Home shows nothing, run undo.sh."
    sh_tv input keyevent KEYCODE_HOME >/dev/null 2>&1 || true
  fi
fi

if [ -n "$LIST" ]; then
  echo "Your changes ($LIST)"
  grep -v '^[[:space:]]*#' "$LIST" | grep -v '^[[:space:]]*$' | while read -r kind a b c; do
    case "$kind" in
      off)
        if installed "$a"; then check "$a stays off" "is_off $a" "sh_tv pm disable-user --user 0 $a"
        else echo "  gone  $a (not installed)"; fi ;;
      global|secure|system)
        check "$kind $a = $b" "[ \"\$(sh_tv settings get $kind $a)\" = \"$b\" ]" "sh_tv settings put $kind $a $b" ;;
      device_config)
        check "$a/$b = $c" "[ \"\$(sh_tv device_config get $a $b)\" = \"$c\" ]" "sh_tv device_config put $a $b $c" ;;
      *) echo "  ?     can't read: $kind $a $b $c" ;;
    esac
  done > "${TMPDIR:-/tmp}/tvcheck.$$" || true
  cat "${TMPDIR:-/tmp}/tvcheck.$$"
  n=$(grep -c "WRONG" "${TMPDIR:-/tmp}/tvcheck.$$" || true); bad=$((bad + n))
  rm -f "${TMPDIR:-/tmp}/tvcheck.$$"
fi

if [ "$bad" -gt 0 ]; then
  [ -n "$FIX" ] && echo "$bad thing(s) could not be fixed." || echo "$bad thing(s) changed back. Run again with --fix to put them right."
  exit 1
fi
if [ -n "$home_unchecked" ]; then
  echo "Other checks passed; Home was not checked. Run check.sh --fix to test it."
else
  echo "All good."
fi
