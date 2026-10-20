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
# Prints a result line. With --fix, runs the fix and checks again.
check() { # name, test command, fix command
  if eval "$2"; then echo "  ok    $1"; return; fi
  if [ -n "$FIX" ] && eval "$3" >/dev/null 2>&1 && eval "$2"; then echo "  fixed $1"; return; fi
  echo "  WRONG $1"; bad=$((bad + 1))
}
is_off() { sh_tv pm list packages -d "$1" | grep -qx "package:$1"; }
installed() { sh_tv pm list packages "$1" | grep -qx "package:$1"; }

# shellcheck disable=SC2034 # ME is read inside the eval'd check strings below
ME=com.ansub.tvhome/.MainActivity
home() { sh_tv cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1; }
echo "Home screen"
# The TV's own launchers first: while one is on, Home can't be fixed.
# In the same argument as cat: Git Bash on Windows turns an argument that starts with / into a Windows path.
turned_off=$(sh_tv "cat /data/local/tmp/tvhome-disabled.txt" 2>/dev/null | sort -u)
for p in $turned_off; do
  check "$p stays off" "is_off $p" "sh_tv pm disable-user --user 0 $p"
done
check "Home opens this home screen" '[ "$(home)" = "$ME" ]' \
  'sh_tv cmd role add-role-holder --user 0 android.app.role.HOME com.ansub.tvhome; sh_tv cmd package set-home-activity "$ME"'

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
echo "All good."
