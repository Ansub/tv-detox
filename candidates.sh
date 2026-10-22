#!/bin/sh
# Lists every app on your TV with the facts you need to clean it up safely. Changes nothing.
#
#   sh candidates.sh <TV_IP>            how the TV is doing, then every app with a verdict
#   sh candidates.sh <TV_IP> --health   just how the TV is doing (memory, swap, load, apps off)
#
# One line per app, grouped by verdict:
#
#   VERDICT  STATE  PACKAGE                            WHAT IT IS
#
#   never    the TV needs it, or Android keeps it running whatever you do. Leave it on.
#   keep     it carries a feature (casting, search, Live TV). Turn it off only if you don't use it.
#   safe     commonly preinstalled junk: ads, sponsored rows, unused vendor and Google apps.
#   app      an app on the TV's app list, or one you installed. Yours to keep, or to turn off if unused.
#   review   nothing known about it. If you're not sure, leave it on.
#
# State is on or off, so what's already turned off never gets undone by accident.
#
# The verdict comes from packages.txt, or from what the TV itself says about the app:
#   - PERSISTENT apps are "never": pm disable-user does not stick on those, they come back on their own.
#   - Apps that provide an HDMI, AV or tuner input are "never": without them the inputs are gone.
#   - The keyboard in use is "never"; other keyboards are "keep". The screensaver in use is "keep".
#   - An app that handles a button on the remote (TV, Input, Guide) is at least "keep", and says which.
#   - Apps you installed, and apps on the TV's app list, are "app".
# Read-only, safe to run any time, same on any Android TV or Google TV. docs/debloat.md says what to
# do with the list, and apply.sh makes the changes so undo.sh can put them back.
set -e
cd "$(dirname "$0")" 2>/dev/null || true

TARGET="$1"; HEALTH_ONLY=
[ "$2" = "--health" ] && HEALTH_ONLY=1
[ -n "$TARGET" ] || { echo "Usage: sh candidates.sh <TV_IP> [--health]" >&2; exit 1; }
command -v adb >/dev/null 2>&1 || {
  echo "adb is missing. Install platform-tools first:" >&2
  echo "  Mac:     brew install android-platform-tools" >&2
  echo "  Windows: https://developer.android.com/tools/releases/platform-tools" >&2
  exit 1; }
[ -n "$HEALTH_ONLY" ] || [ -r packages.txt ] || {
  echo "packages.txt is missing. Download the whole repository, not just this script." >&2; exit 1; }

# An IP gets :5555 added; an adb serial (like emulator-5554) is used as is.
case "$TARGET" in
  *.*.*.*:*) TV=$TARGET ;;
  *.*.*.*)   TV=$TARGET:5555 ;;
  *)         TV=$TARGET ;;
esac
case "$TV" in *:*) adb connect "$TV" >/dev/null 2>&1 || true ;; esac
[ "$(adb -s "$TV" get-state 2>/dev/null)" = device ] || {
  echo "Can't reach $TV. Is the TV on, on this network, with debugging allowed?" >&2; exit 1; }

sh_tv() { _rc=0; _out=$(adb -s "$TV" shell "$@" </dev/null 2>/dev/null) || _rc=$?; printf '%s\n' "$_out" | tr -d '\r'; return $_rc; }
# If the TV drops off halfway, stop: a half-read list would give wrong verdicts.
lost() { echo "Lost the connection to $TV while reading the app list. Nothing was changed. Run it again." >&2; exit 1; }

tmp=$(mktemp -d 2>/dev/null || mktemp -d -t tvcand)
trap 'rm -rf "$tmp"' EXIT

# ---- How the TV is doing: the "before" to compare with after the cleanup and a restart ----
all=$(sh_tv pm list packages) || lost
off=$(sh_tv pm list packages -d) || lost
case "$all" in *package:*) ;; *) lost ;; esac
all=$(printf '%s\n' "$all" | sed -n 's/^package://p')
off=$(printf '%s\n' "$off" | sed -n 's/^package://p')

mb() { awk -v kb="${1:-0}" 'BEGIN { printf "%d", kb / 1024 }'; }
mem=$(sh_tv cat /proc/meminfo) || mem=
total=$(printf '%s\n' "$mem" | awk '/^MemTotal:/ {print $2}')
avail=$(printf '%s\n' "$mem" | awk '/^MemAvailable:/ {print $2}')
swapt=$(printf '%s\n' "$mem" | awk '/^SwapTotal:/ {print $2}')
swapf=$(printf '%s\n' "$mem" | awk '/^SwapFree:/ {print $2}')
secs=$(sh_tv cat /proc/uptime | cut -d. -f1)
load=$(sh_tv cat /proc/loadavg | cut -d' ' -f1)
cores=$(sh_tv cat /sys/devices/system/cpu/present | awk -F- '{print ($2 == "" ? 1 : $2 + 1)}')
home=$(sh_tv cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1 | sed 's|/.*||')
echo "$(sh_tv getprop ro.product.model) ($(sh_tv getprop ro.product.manufacturer), $(sh_tv getprop ro.board.platform)), Android $(sh_tv getprop ro.build.version.release)"
if [ -n "$total" ]; then
  line="Memory: $(mb "$avail") MB free of $(mb "$total") MB"
  [ "${swapt:-0}" -gt 0 ] && line="$line, swap $(mb $((swapt - swapf))) MB of $(mb "$swapt") MB in use"
  echo "$line"
fi
if [ -n "$secs" ]; then
  busy=""
  if [ -n "$load" ] && [ -n "$cores" ] && [ "$(awk -v l="$load" -v c="$cores" 'BEGIN {print (l > c * 1.5)}')" = 1 ]; then
    # Some TVs (Realtek) keep a score of firmware threads stuck waiting on hardware. They count in
    # the load but use no CPU, so a high load there says nothing about the apps.
    stuck=$(sh_tv ps -A -T -o STAT | grep -c '^D' || true)
    if [ "${stuck:-0}" -gt 0 ]; then busy=" ($stuck of it is firmware threads waiting on hardware, not apps: go by memory and swap)"
    else busy=" (busy: above $cores means waiting)"; fi
  fi
  echo "Up $((secs / 86400)) days $((secs % 86400 / 3600)) h, load $load on $cores cores$busy"
fi
echo "Apps: $(printf '%s\n' "$all" | grep -c .) installed, $(printf '%s\n' "$off" | grep -c .) turned off. Home opens ${home:-?}."
[ -z "$HEALTH_ONLY" ] || exit 0
echo

# ---- What the TV says about each app ----
dump=$(sh_tv dumpsys package) || lost
case "$dump" in *"Package ["*) ;; *) lost ;; esac

# One call reads every package's flags, so this stays quick on a slow TV.
printf '%s\n' "$dump" | awk '
  /^[[:space:]]*Package \[/ { n=$0; sub(/.*\[/,"",n); sub(/\].*/,"",n); cur=n; seen=0; next }
  cur!="" && !seen && /pkgFlags=\[/ { l=$0; sub(/.*pkgFlags=\[/,"",l); sub(/\].*/,"",l); print cur "\t" l; seen=1 }
' | sort -u > "$tmp/flags"

printf '%s\n' "$all" > "$tmp/all"
printf '%s\n' "$off" > "$tmp/off"

# Apps that provide a hardware input (HDMI, AV, tuner): their input ids end in /HW… or /HDMI….
sh_tv dumpsys tv_input | grep -o 'id=[^,} ]*' | sed 's/^id=//' | awk -F/ 'NF >= 3 { print $1 }' | sort -u > "$tmp/inputs"
# Apps that handle a button on the remote, with the button's name (TV, TV_INPUT, GUIDE…).
sh_tv dumpsys window policy | grep -o 'KEYCODE_[A-Z0-9_]*=[a-zA-Z0-9._]*' \
  | sed 's/^KEYCODE_//; s/=/\t/' | awk -F'\t' '{ k[$2] = (k[$2] == "" ? $1 : k[$2] ", " $1) } END { for (p in k) print p "\t" k[p] " button" (k[p] ~ /,/ ? "s" : "") }' > "$tmp/keys"
# The screensaver in use.
sh_tv settings get secure screensaver_components | tr ',' '\n' | sed 's|/.*||' | grep -v '^null$' > "$tmp/dreams"
# Keyboards, and the one in use.
sh_tv ime list -s | sed 's|/.*||' | sort -u > "$tmp/imes"
sh_tv settings get secure default_input_method | sed 's|/.*||' > "$tmp/ime"
# Apps you installed.
sh_tv pm list packages -3 | sed -n 's/^package://p' | sort -u > "$tmp/third"
# Apps on the TV's app list, and apps that answer Home.
sh_tv cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER \
  | grep / | sed 's|/.*||; s/^ *//' | sort -u > "$tmp/leanback"
sh_tv cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME \
  | grep / | sed 's|/.*||; s/^ *//' | sort -u > "$tmp/home"

# packages.txt: verdict, package, name. Comments and blank lines skipped.
grep -v '^[[:space:]]*#' packages.txt | grep -v '^[[:space:]]*$' | tr -d '\r' > "$tmp/list"

awk -F'\t' -v tmp="$tmp" '
  function load(file, arr,   l) { while ((getline l < (tmp "/" file)) > 0) if (l != "") arr[l] = 1; close(tmp "/" file) }
  BEGIN {
    while ((getline l < (tmp "/list")) > 0) { split(l, y, "\t"); LV[y[2]] = y[1]; NM[y[2]] = y[3] }
    while ((getline l < (tmp "/flags")) > 0) { split(l, y, "\t"); FL[y[1]] = y[2] }
    while ((getline l < (tmp "/keys")) > 0) { split(l, y, "\t"); KEYS[y[1]] = y[2] }
    load("off", OF); load("inputs", IN); load("imes", IME); load("third", THIRD); load("leanback", LEAN); load("home", HOME)
    load("dreams", DREAM)
    getline DEFIME < (tmp "/ime")
    ORD["never"] = 0; ORD["keep"] = 1; ORD["safe"] = 2; ORD["app"] = 3; ORD["review"] = 4
    while ((getline p < (tmp "/all")) > 0) {
      if (p == "") continue
      st = (p in OF) ? "off" : "on"
      v = (p in LV) ? LV[p] : ""; why = ""
      if (FL[p] ~ /PERSISTENT/)      { v = "never"; why = "Android keeps it running" }
      else if (v == "never")         { }
      else if (v == "launcher")      { v = "never"; why = "the TV\047s home screen: only install.sh turns it off, with undo" }
      else if (p in IN)              { v = "never"; why = "provides an HDMI, AV or tuner input" }
      else if (p == DEFIME)          { v = "never"; why = "the keyboard in use" }
      else if (p in DREAM)           { v = "keep"; why = "the screensaver in use" }
      else if (v == "keep" || v == "safe" || v == "app") {
        if (p in KEYS && v == "safe") { v = "keep"; why = "handles the " KEYS[p] " on the remote" }
      }
      else if (p in IME)             { v = "keep"; why = "a keyboard" }
      else if (p in THIRD)           { v = "app"; why = "you installed it" }
      else if (p in LEAN)            { v = "app"; why = "on the TV\047s app list" }
      else if (p in HOME)            { v = "keep"; why = "answers Home" }
      else if (p in KEYS)            { v = "keep"; why = "handles the " KEYS[p] " on the remote" }
      else                           { v = "review" }
      if (why == "" && p in KEYS) why = "handles the " KEYS[p] " on the remote"
      name = NM[p]
      if (why != "") name = (name == "" ? "" : name " ") "(" why ")"
      printf "%d\t%s\t%s\t%s\t%s\n", ORD[v], v, st, p, name
      C[v]++
    }
    printf "%d %d %d %d %d\n", C["never"] + 0, C["keep"] + 0, C["safe"] + 0, C["app"] + 0, C["review"] + 0 > (tmp "/summary")
    close(tmp "/summary")
    exit
  }
' | sort -k1,1n -k4,4 | cut -f2-

[ -s "$tmp/summary" ] || { echo "Couldn't sort the app list. Nothing was changed." >&2; exit 1; }
# shellcheck disable=SC2046 # the five counts are meant to split
set -- $(cat "$tmp/summary")
echo
echo "never $1 | keep $2 | safe $3 | app $4 | review $5"
echo
echo "safe = commonly preinstalled junk. app = yours to decide. review = unknown, so leave it on unless you know it."
echo "Before turning anything off, read docs/debloat.md. Make the changes with apply.sh, so undo.sh can put them back."
