#!/bin/sh
# Puts the TV back: undoes the cleanup apply.sh made, turns your TV's own home screen back on and
# removes this one.
#   sh undo.sh <TV_IP>             everything
#   sh undo.sh <TV_IP> --cleanup   only what apply.sh changed; the home screen stays
set -e
[ -n "$1" ] || { echo "Usage: sh undo.sh <TV_IP> [--cleanup]" >&2; exit 1; }
CLEANUP_ONLY=; [ "$2" = "--cleanup" ] && CLEANUP_ONLY=1
case "$1" in
  *.*.*.*:*) TV=$1 ;;
  *.*.*.*)   TV=$1:5555 ;;
  *)         TV=$1 ;;
esac
case "$TV" in *:*) adb connect "$TV" >/dev/null || true ;; esac
[ "$(adb -s "$TV" get-state 2>/dev/null)" = device ] || { echo "Can't reach $TV." >&2; exit 1; }

failed=
# 0: the app is off. 1: it's on or not installed. 2: the TV couldn't be asked.
is_off() {
  out=$(adb -s "$TV" shell pm list packages -d "$1") || return 2
  out=$(printf '%s\n' "$out" | tr -d '\r' | grep -v '^$') || return 1
  # Anything but package lines is an error message, even if pm said it worked.
  printf '%s\n' "$out" | grep -qv '^package:' && return 2
  printf '%s\n' "$out" | grep -Fqx "package:$1" && return 0
  return 1
}
# Turns an app back on and checks it worked. An app that's no longer installed counts as done.
turn_on() {
  adb -s "$TV" shell pm enable "$1" >/dev/null 2>&1 || true
  rc=0; is_off "$1" || rc=$?
  case $rc in
    1) echo "Turned $1 back on"
       # Google TV's home screen can come back without its notification access (#16).
       [ "$1" = com.google.android.apps.tv.launcherx ] && adb -s "$TV" shell cmd notification allow_listener \
         com.google.android.apps.tv.launcherx/.coreservices.notificationlistener.TvNotificationListenerService >/dev/null 2>&1 || true ;;
    0) echo "Couldn't turn $1 back on." >&2; failed=1 ;;
    *) echo "Couldn't check that $1 is back on." >&2; failed=1 ;;
  esac
}

sh_tv() { _rc=0; _out=$(adb -s "$TV" shell "$@" </dev/null 2>/dev/null) || _rc=$?; [ -z "$_out" ] || printf '%s\n' "$_out" | tr -d '\r'; return $_rc; }
# Reads a setting the way the TV stores it: "null" when it was never set.
get_setting() { case "$1" in device_config) sh_tv device_config get "$2" "$3" ;; *) sh_tv settings get "$1" "$2" ;; esac; }
# Puts a setting back to its old value; "null" means it wasn't set at all.
restore_setting() { # kind name [namespace-name] old
  if [ "$1" = device_config ]; then
    if [ "$4" = null ]; then sh_tv device_config delete "$2" "$3" >/dev/null 2>&1 || true
    else sh_tv device_config put "$2" "$3" "$4" >/dev/null 2>&1 || true; fi
    if [ "$(get_setting device_config "$2" "$3")" = "$4" ]; then echo "Put $2/$3 back to $4"
    else echo "Couldn't put $2/$3 back to $4." >&2; failed=1; fi
  else
    if [ "$3" = null ]; then sh_tv settings delete "$1" "$2" >/dev/null 2>&1 || true
    else sh_tv settings put "$1" "$2" "$3" >/dev/null 2>&1 || true; fi
    if [ "$(get_setting "$1" "$2")" = "$3" ]; then echo "Put $1 $2 back to $3"
    else echo "Couldn't put $1 $2 back to $3." >&2; failed=1; fi
  fi
}

# First, the cleanup: every line apply.sh wrote to its undo list on the TV, newest first.
# END marks a full read; no list at all is fine.
JOURNAL=/data/local/tmp/tvhome-cleanup.txt
journal=$(adb -s "$TV" shell "if [ -e $JOURNAL ]; then cat $JOURNAL && echo END; else echo END; fi" 2>/dev/null) || journal=
journal=$(printf '%s\n' "$journal" | tr -d '\r')
cleanup_failed=
case "$journal" in
  *END)
    printf '%s\n' "$journal" | grep -vx END | grep -v '^$' | sed -n '1!G;h;$p' | while read -r kind a b c d; do
      failed=
      case "$kind" in
        off) turn_on "$a" ;;
        global|secure|system) restore_setting "$kind" "$a" "$c" ;;
        device_config) restore_setting device_config "$a" "$b" "$d" ;;
        *) echo "Can't read this line of the undo list, left as it is: $kind $a $b $c $d" >&2; failed=1 ;;
      esac
      [ -z "$failed" ] || echo FAIL
    done > "${TMPDIR:-/tmp}/tvundo.$$" || true
    grep -vx FAIL "${TMPDIR:-/tmp}/tvundo.$$" || true
    grep -qx FAIL "${TMPDIR:-/tmp}/tvundo.$$" && cleanup_failed=1
    rm -f "${TMPDIR:-/tmp}/tvundo.$$"
    if [ -z "$cleanup_failed" ]; then
      adb -s "$TV" shell "rm -f $JOURNAL"
      [ "$journal" = END ] || echo "The cleanup is undone."
    else
      echo "Some of the cleanup couldn't be undone. The undo list is kept, so run undo.sh again." >&2; failed=1
    fi ;;
  *) echo "Couldn't read the list of changes apply.sh made." >&2; failed=1 ;;
esac
if [ -n "$CLEANUP_ONLY" ]; then
  [ -z "$failed" ] || exit 1
  echo "The home screen stays. sh undo.sh <TV_IP> without --cleanup removes it too."
  exit 0
fi

# Turn back on whatever install.sh turned off, before removing this home screen.
# END marks a full read; no list at all is fine.
LIST=/data/local/tmp/tvhome-disabled.txt
list=$(adb -s "$TV" shell "if [ -e $LIST ]; then cat $LIST && echo END; else echo END; fi" 2>/dev/null) || list=
list=$(printf '%s\n' "$list" | tr -d '\r')
case "$list" in
  *END) for p in $(printf '%s\n' "$list" | grep -vx END | sort -u); do turn_on "$p"; done ;;
  *) echo "Couldn't read the list of apps install.sh turned off." >&2; failed=1 ;;
esac
# Google TV's setup app may have been turned off by hand (see the README), so turn it back on too.
W=com.google.android.tungsten.setupwraith
rc=0; is_off "$W" || rc=$?
case $rc in
  0) turn_on "$W" ;;
  1) ;;
  *) echo "Couldn't check $W." >&2; failed=1 ;;
esac

# If something stayed off, keep this home screen and the list, so Home still works and
# undo.sh can try again.
if [ -n "$failed" ]; then
  echo "Kept this home screen so your TV still has one. Run undo.sh again, or turn the app on with: adb shell pm enable <package>" >&2
  exit 1
fi
# In the same argument as rm, like the read above: Git Bash on Windows turns an argument that starts with / into a Windows path.
adb -s "$TV" shell "rm -f $LIST"
adb -s "$TV" uninstall com.ansub.tvhome >/dev/null 2>&1 || true
adb -s "$TV" shell input keyevent KEYCODE_HOME
echo "Removed. Your TV is back to its own home screen."
