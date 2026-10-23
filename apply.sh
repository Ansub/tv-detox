#!/bin/sh
# Makes the cleanup changes from a list, and writes each one to an undo list on the TV first,
# so undo.sh can put every one of them back and check.sh can see they stuck.
#
#   sh apply.sh <TV_IP> my-tv.txt             shows what it would do, asks once, then does it
#   sh apply.sh <TV_IP> my-tv.txt --dry-run   only shows
#   YES=1 sh apply.sh <TV_IP> my-tv.txt       doesn't ask
#
# The list has one change per line (# starts a comment), the same lines check.sh reads:
#   off com.example.app                        turn the app off (not uninstall)
#   global window_animation_scale 0            a setting (global, secure or system)
#   device_config activity_manager max_cached_processes 4
#
# It refuses to turn off what the TV needs: apps Android keeps running (PERSISTENT), apps that
# provide an HDMI or tuner input, the keyboard in use, the TV's home screen, and everything marked
# "never" in packages.txt. An app that's already off is left alone and kept off the undo list, so
# undo.sh never turns on something you had turned off before.
set -e
cd "$(dirname "$0")"

TARGET="$1"; LIST="$2"; DRY=
[ "$3" = "--dry-run" ] && DRY=1
[ -n "$TARGET" ] && [ -n "$LIST" ] || { echo "Usage: sh apply.sh <TV_IP> <list.txt> [--dry-run]" >&2; exit 1; }
[ -f "$LIST" ] && [ -r "$LIST" ] || { echo "Can't read $LIST." >&2; exit 2; }
[ -r packages.txt ] || { echo "packages.txt is missing. Download the whole repository, not just this script." >&2; exit 1; }
command -v adb >/dev/null 2>&1 || { echo "adb is missing. Install platform-tools first." >&2; exit 1; }

case "$TARGET" in
  *.*.*.*:*) TV=$TARGET ;;
  *.*.*.*)   TV=$TARGET:5555 ;;
  *)         TV=$TARGET ;;
esac
case "$TV" in *:*) adb connect "$TV" >/dev/null 2>&1 || true ;; esac
[ "$(adb -s "$TV" get-state 2>/dev/null)" = device ] || { echo "Can't reach $TV." >&2; exit 2; }

# Keeps adb's exit status, so a failed command on the TV counts as failed here.
sh_tv() { _rc=0; _out=$(adb -s "$TV" shell "$@" </dev/null 2>/dev/null) || _rc=$?; [ -z "$_out" ] || printf '%s\n' "$_out" | tr -d '\r'; return $_rc; }
lost() { echo "Lost the connection to $TV. Check the TV with check.sh, then run this again." >&2; exit 2; }

# The undo list on the TV. Same lines as the list, plus the old value after the new one for settings:
#   off com.example.app
#   global window_animation_scale 0 1.0
#   device_config activity_manager max_cached_processes 4 null
JOURNAL=/data/local/tmp/tvhome-cleanup.txt

# ---- What the TV says, read once ----
dump=$(sh_tv dumpsys package) || lost
case "$dump" in *"Package ["*) ;; *) lost ;; esac
all=$(sh_tv pm list packages | sed -n 's/^package://p') || lost
off=$(sh_tv pm list packages -d | sed -n 's/^package://p') || lost
inputs=$(sh_tv dumpsys tv_input | grep -o 'id=[^,} ]*' | sed 's/^id=//' | awk -F/ 'NF >= 3 { print $1 }' | sort -u)
ime=$(sh_tv settings get secure default_input_method | sed 's|/.*||')
journal=$(sh_tv "if [ -e $JOURNAL ]; then cat $JOURNAL; fi") || lost
verdicts=$(tr -d '\r' < packages.txt | grep -v '^[[:space:]]*#' | awk -F'\t' 'NF >= 2 { print $1 "\t" $2 }')

has() { printf '%s\n' "$2" | grep -Fqx "$1"; }
verdict() { printf '%s\n' "$verdicts" | awk -F'\t' -v p="$1" '$2 == p { print $1; exit }'; }
flags() { printf '%s\n' "$dump" | awk -v p="$1" '
  /^[[:space:]]*Package \[/ { n=$0; sub(/.*\[/,"",n); sub(/\].*/,"",n); cur=n; next }
  cur == p && /pkgFlags=\[/ { print; exit }'; }
# Values go into shell commands on the TV, so only plain characters are allowed.
plain() { printf '%s' "$1" | grep -Eq '^[A-Za-z0-9._:/-]+$'; }

# Why an app can't be turned off, or nothing if it can.
refuse_off() {
  p=$1
  has "$p" "$all" || { echo "not installed"; return; }
  case "$(flags "$p")" in *PERSISTENT*) echo "Android keeps it running, so turning it off wouldn't stick"; return ;; esac
  has "$p" "$inputs" && { echo "it provides an HDMI, AV or tuner input"; return; }
  [ "$p" = "$ime" ] && { echo "it's the keyboard in use"; return; }
  [ "$p" = com.ansub.tvhome ] && { echo "it's this home screen; use undo.sh instead"; return; }
  case "$(verdict "$p")" in
    never) echo "the TV needs it (packages.txt says never)" ;;
    launcher) echo "it's the TV's home screen; install.sh turns it off, with undo" ;;
  esac
}

# Reads a setting the way the TV stores it: "null" when it was never set.
get_setting() { case "$1" in device_config) sh_tv device_config get "$2" "$3" ;; *) sh_tv settings get "$1" "$2" ;; esac; }

# ---- The plan ----
plan=$(grep -v '^[[:space:]]*#' "$LIST" | grep -v '^[[:space:]]*$' | tr -d '\r' || true)
[ -n "$plan" ] || { echo "Nothing to do: $LIST has no changes in it."; exit 0; }
todo=0; skipped=0
echo "What apply.sh would do on $TV:"
printf '%s\n' "$plan" | while read -r kind a b c d; do
  case "$kind" in
    off)
      why=$(refuse_off "$a")
      if [ -n "$why" ]; then echo "  refuse  off $a: $why"
      elif has "$a" "$off"; then echo "  skip    off $a: already off (left as it is, not on the undo list)"
      else echo "  do      off $a"; fi ;;
    global|secure|system)
      if [ -z "$b" ] || [ -n "$c" ] || ! plain "$a" || ! plain "$b"; then echo "  refuse  $kind $a $b $c: expected \"$kind <name> <value>\" with plain characters"
      elif [ "$(get_setting "$kind" "$a")" = "$b" ]; then echo "  skip    $kind $a = $b: already so"
      else echo "  do      $kind $a = $b (now $(get_setting "$kind" "$a"))"; fi ;;
    device_config)
      if [ -z "$c" ] || [ -n "$d" ] || ! plain "$a" || ! plain "$b" || ! plain "$c"; then echo "  refuse  device_config $a $b $c $d: expected \"device_config <namespace> <name> <value>\""
      elif [ "$(get_setting device_config "$a" "$b")" = "$c" ]; then echo "  skip    $a/$b = $c: already so"
      else echo "  do      $a/$b = $c (now $(get_setting device_config "$a" "$b"))"; fi ;;
    *) echo "  refuse  $(printf '%s' "$kind $a $b $c $d" | sed 's/ *$//'): can't read it" ;;
  esac
done > "${TMPDIR:-/tmp}/tvapply.$$"
cat "${TMPDIR:-/tmp}/tvapply.$$"
todo=$(grep -c '^  do ' "${TMPDIR:-/tmp}/tvapply.$$" || true)
skipped=$(grep -c '^  refuse ' "${TMPDIR:-/tmp}/tvapply.$$" || true)
rm -f "${TMPDIR:-/tmp}/tvapply.$$"
echo
[ "$skipped" -eq 0 ] || echo "$skipped line(s) refused. Fix or remove them; the rest can still go ahead."
if [ "$todo" -eq 0 ]; then echo "Nothing left to do."; exit 0; fi
[ -z "$DRY" ] || { echo "Dry run: $todo change(s) not made."; exit 0; }

answer=${YES:+y}
if [ -z "$answer" ]; then
  printf "Make these %s change(s)? undo.sh --cleanup puts them back. [y/N] " "$todo"
  read -r answer </dev/tty || answer=n
fi
case "$answer" in y|Y|yes) ;; *) echo "Nothing changed."; exit 0 ;; esac

# ---- Doing it: every change is written to the undo list on the TV before it's made ----
failed=0; apps_off=0
# Appends a line to the undo list, once. Fails if the TV can't save it.
journal_add() {
  has "$1" "$journal" && return 0
  sh_tv "echo '$1' >> $JOURNAL" || return 1
  journal="$journal
$1"
}
printf '%s\n' "$plan" | while read -r kind a b c d; do
  case "$kind" in
    off)
      [ -z "$(refuse_off "$a")" ] || continue
      has "$a" "$off" && continue
      journal_add "off $a" || { echo "  FAILED  off $a: couldn't save it for undo.sh, so it stays on" >&2; echo FAIL; continue; }
      sh_tv pm disable-user --user 0 "$a" >/dev/null || true
      if sh_tv pm list packages -d "$a" | grep -Fqx "package:$a"; then echo "  off     $a"; echo APP
      else echo "  FAILED  off $a: the TV wouldn't turn it off" >&2; echo FAIL; fi ;;
    global|secure|system)
      [ -n "$b" ] && [ -z "$c" ] && plain "$a" && plain "$b" || continue
      old=$(get_setting "$kind" "$a"); [ "$old" = "$b" ] && continue
      # Only the first change of a setting records the old value, so undo gets back to the start.
      prev=$(printf '%s\n' "$journal" | awk -v k="$kind" -v n="$a" '$1 == k && $2 == n { print; exit }')
      if [ -n "$prev" ]; then
        sh_tv "sed -i 's/^$kind $a .*/$kind $a $b ${prev##* }/' $JOURNAL" || { echo "  FAILED  $kind $a: couldn't update the undo list" >&2; echo FAIL; continue; }
      else
        journal_add "$kind $a $b $old" || { echo "  FAILED  $kind $a: couldn't save it for undo.sh" >&2; echo FAIL; continue; }
      fi
      sh_tv settings put "$kind" "$a" "$b" >/dev/null || true
      if [ "$(get_setting "$kind" "$a")" = "$b" ]; then echo "  set     $kind $a = $b (was $old)"
      else echo "  FAILED  $kind $a: the TV didn't take the value" >&2; echo FAIL; fi ;;
    device_config)
      [ -n "$c" ] && [ -z "$d" ] && plain "$a" && plain "$b" && plain "$c" || continue
      old=$(get_setting device_config "$a" "$b"); [ "$old" = "$c" ] && continue
      prev=$(printf '%s\n' "$journal" | awk -v n="$a" -v k="$b" '$1 == "device_config" && $2 == n && $3 == k { print; exit }')
      if [ -n "$prev" ]; then
        sh_tv "sed -i 's/^device_config $a $b .*/device_config $a $b $c ${prev##* }/' $JOURNAL" || { echo "  FAILED  $a/$b: couldn't update the undo list" >&2; echo FAIL; continue; }
      else
        journal_add "device_config $a $b $c $old" || { echo "  FAILED  $a/$b: couldn't save it for undo.sh" >&2; echo FAIL; continue; }
      fi
      sh_tv device_config put "$a" "$b" "$c" >/dev/null || true
      if [ "$(get_setting device_config "$a" "$b")" = "$c" ]; then echo "  set     $a/$b = $c (was $old)"
      else echo "  FAILED  $a/$b: the TV didn't take the value" >&2; echo FAIL; fi ;;
  esac
done > "${TMPDIR:-/tmp}/tvapply.$$" || true
grep -Ev '^(FAIL|APP)$' "${TMPDIR:-/tmp}/tvapply.$$" || true
failed=$(grep -c '^FAIL$' "${TMPDIR:-/tmp}/tvapply.$$" || true)
apps_off=$(grep -c '^APP$' "${TMPDIR:-/tmp}/tvapply.$$" || true)
rm -f "${TMPDIR:-/tmp}/tvapply.$$"

if [ "$apps_off" -gt 0 ]; then
  # Android saves this a few seconds later. Restarting before then can undo it.
  echo "Saving the changes on the TV, this takes 15 seconds…"
  sleep 15
fi
echo
if [ "$failed" -gt 0 ]; then
  echo "$failed change(s) failed. The rest are on the TV's undo list: sh undo.sh $TARGET --cleanup puts them back." >&2
  exit 1
fi
echo "Done. Restart the TV once, then: sh check.sh $TARGET checks it all stuck, sh undo.sh $TARGET --cleanup puts it back."
# Refused lines are not a failure on the TV, but they're not done either: say so with the exit status.
[ "$skipped" -eq 0 ] || exit 1
