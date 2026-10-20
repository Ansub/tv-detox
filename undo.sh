#!/bin/sh
# Removes the home screen and turns your TV's own home screen back on.
# Usage: sh undo.sh <TV_IP>
set -e
[ -n "$1" ] || { echo "Usage: sh undo.sh <TV_IP>" >&2; exit 1; }
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
<<<<<<< HEAD
adb -s "$TV" shell rm -f "$LIST"
# The throwaway input install.sh can add with FAKE_INPUT=1 (testing on an emulator).
adb -s "$TV" uninstall com.ansub.fakeinput >/dev/null 2>&1 || true
=======
# In the same argument as rm, like the read above: Git Bash on Windows turns an argument that starts with / into a Windows path.
adb -s "$TV" shell "rm -f $LIST"
>>>>>>> e3b490ff294616803d9c49f601b966ae89427409
adb -s "$TV" uninstall com.ansub.tvhome >/dev/null 2>&1 || true
adb -s "$TV" shell input keyevent KEYCODE_HOME
echo "Removed. Your TV is back to its own home screen."
