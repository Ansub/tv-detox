#!/bin/sh
# Lists every app on your TV with the facts you need to clean it up safely. Changes nothing.
#
#   sh candidates.sh <TV_IP>
#
# One line per app, grouped by verdict:
#
#   VERDICT  STATE  PACKAGE                            WHAT IT IS
#
#   never    the TV needs it, or Android keeps it running whatever you do. Leave it on.
#   keep     it carries a feature (casting, search, Live TV). Turn it off only if you don't use it.
#   safe     commonly preinstalled junk: ads, sponsored rows, unused vendor and Google apps.
#   review   nothing known about it. If you're not sure, leave it on.
#
# State is on or off, so what's already turned off never gets undone by accident.
#
# "never" also covers apps Android marks PERSISTENT: pm disable-user does not stick on those,
# so they come back on their own. Read-only, safe to run any time, same on any Android TV or
# Google TV. docs/debloat.md says what to do with the list.
set -e
cd "$(dirname "$0")" 2>/dev/null || true

TARGET="$1"
[ -n "$TARGET" ] || { echo "Usage: sh candidates.sh <TV_IP>" >&2; exit 1; }
command -v adb >/dev/null 2>&1 || {
  echo "adb is missing. Install platform-tools first:" >&2
  echo "  Mac:     brew install android-platform-tools" >&2
  echo "  Windows: https://developer.android.com/tools/releases/platform-tools" >&2
  exit 1; }

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

# The TV needs these to start, or to keep working once it has.
NEVER="com.google.android.gms com.google.android.gsf com.android.vending
com.android.tv.settings com.android.settings com.android.systemui
com.android.providers.settings com.android.settings.intelligence com.android.shell
com.android.packageinstaller com.google.android.packageinstaller
com.google.android.permissioncontroller com.android.bluetooth com.android.externalstorage
com.android.providers.media com.android.providers.media.module com.android.providers.downloads
com.android.keychain com.android.inputdevices com.android.location.fused com.android.se
com.android.providers.tv com.android.providers.contacts com.android.providers.calendar
com.android.networkstack.inprocess com.android.networkstack.permissionconfig
com.android.certinstaller com.android.companiondevicemanager com.android.dynsystem
com.android.statementservice com.android.vpndialogs com.android.wallpaperbackup
com.android.backupconfirm com.android.sharedstoragebackup com.android.localtransport
com.android.proxyhandler com.android.pacprocessor com.android.captiveportallogin
com.dolby.android.audio.service
com.google.android.inputmethod.latin com.android.inputmethod.latin com.google.android.leanback.ime
com.tcl.inputmethod.international com.android.providers.userdictionary
com.google.android.tungsten.setupwraith com.google.android.onetimeinitializer
com.google.android.partnersetup com.android.provision com.android.tv.provision com.android.managedprovisioning
com.tcl.suspension com.tcl.tvinput com.tcl.providers.config com.tcl.useragreement
com.tcl.settings com.tcl.tcl_bt_rcu_service com.google.android.apps.tv.launcherx"

# Turn off only if you don't use the feature they carry.
KEEP="com.google.android.apps.mediashell com.google.android.katniss
com.google.android.tv.assistant com.google.android.marvin.talkback
com.google.android.tv.remote.service com.google.android.tts
com.google.android.syncadapters.contacts com.google.android.syncadapters.calendar
com.ansub.tvhome com.google.android.webview com.tcl.android.webview
com.tcl.tv com.tcl.micmanager
com.android.dreams.basic com.google.android.apps.tv.dreamx"

# Commonly preinstalled junk: ads, sponsored rows, unused vendor and Google apps.
SAFE="com.google.android.tvrecommendations com.google.android.backdrop
com.google.android.videos com.google.android.youtube.tvmusic com.google.android.play.games
com.google.android.feedback
com.android.printspooler com.android.htmlviewer com.android.soundpicker
com.tcl.waterfall.overseas com.tcl.esticker com.tcl.partnercustomizer
com.tcl.messagebox com.tcl.smartalexa com.tcl.copydatatotv com.tcl.audioplayer
com.tcl.imageplayer com.tcl.keyhelp com.tcl.usercenter
com.tcl.dashboard com.tcl.ttvs com.tcl.micmanager com.tcl.factory.view"

dump=$(sh_tv dumpsys package) || lost
all=$(sh_tv pm list packages) || lost
off=$(sh_tv pm list packages -d) || lost
printf '%s\n' "$dump" | grep -q 'Package \[' || lost
printf '%s\n' "$all" | grep -q '^package:' || lost
all=$(printf '%s\n' "$all" | sed -n 's/^package://p')
off=$(printf '%s\n' "$off" | sed -n 's/^package://p')

tmp=$(mktemp -d 2>/dev/null || mktemp -d -t tvcand)
trap 'rm -rf "$tmp"' EXIT

# One call reads every package's flags, so this stays quick on a slow TV.
printf '%s\n' "$dump" | awk '
  /^[[:space:]]*Package \[/ { n=$0; sub(/.*\[/,"",n); sub(/\].*/,"",n); cur=n; seen=0; next }
  cur!="" && !seen && /pkgFlags=\[/ { l=$0; sub(/.*pkgFlags=\[/,"",l); sub(/\].*/,"",l); print cur "\t" l; seen=1 }
' | sort -u > "$tmp/flags"

printf '%s\n' "$all" > "$tmp/all"
printf '%s\n' "$off" > "$tmp/off"

# Plain-English names, so the list reads like a list of apps and not package names.
cat > "$tmp/names" <<'EOF'
com.google.android.gms	Google Play services
com.google.android.gsf	Google services framework
com.android.vending	Google Play Store
com.android.tv.settings	TV Settings
com.android.settings	Settings
com.android.systemui	System UI
com.android.bluetooth	Bluetooth
com.google.android.apps.mediashell	Google Cast, casting from your phone
com.google.android.katniss	Google TV search and voice
com.google.android.tv.assistant	Google Assistant
com.google.android.marvin.talkback	TalkBack screen reader
com.google.android.tv.remote.service	Phone remote control
com.google.android.inputmethod.latin	Gboard keyboard
com.android.inputmethod.latin	Android keyboard
com.google.android.tts	Text to speech
com.google.android.tvrecommendations	Google TV sponsored row
com.google.android.backdrop	Google TV backdrop ads
com.google.android.videos	Google TV, Play Movies
com.google.android.youtube.tvmusic	YouTube Music
com.google.android.play.games	Play Games
com.google.android.youtube.tv	YouTube
com.google.android.feedback	Google feedback
com.google.android.onetimeinitializer	One-time setup helper
com.google.android.partnersetup	Partner app setup
com.google.android.syncadapters.contacts	Contacts sync
com.google.android.syncadapters.calendar	Calendar sync
com.android.printspooler	Printing
com.android.dreams.basic	Basic screensaver
com.google.android.apps.tv.dreamx	Google TV screensaver
com.android.htmlviewer	HTML file viewer
com.netflix.ninja	Netflix
com.amazon.amazonvideo.livingroom	Prime Video
com.tcl.waterfall.overseas	TCL content and ads feed
com.tcl.esticker	TCL e-sticker
com.tcl.partnercustomizer	TCL launcher customizer
com.tcl.suspension	TCL input picker
com.tcl.messagebox	TCL message box
com.tcl.smartalexa	TCL Alexa
com.tcl.copydatatotv	TCL phone migration
com.tcl.audioplayer	TCL audio player
com.tcl.imageplayer	TCL picture player
com.tcl.videoplayer	TCL video player
com.tcl.keyhelp	TCL key help
com.tcl.useragreement	TCL user agreement
com.tcl.usercenter	TCL user centre
com.tcl.dashboard	TCL dashboard
com.tcl.ttvs	TCL analytics and Alexa hooks
com.tcl.providers.config	TCL settings store
com.tcl.settings	TCL settings
com.tcl.tvinput	TV input framework
com.tcl.tv	Live TV
com.tcl.tcl_bt_rcu_service	Bluetooth remote
com.google.android.tvlauncher	Google TV home screen
com.google.android.apps.tv.launcherx	Google TV home screen
com.google.android.leanbacklauncher	Android TV home screen
com.google.android.tungsten.setupwraith	Google TV setup
com.dolby.android.audio.service	Dolby audio
com.ansub.tvhome	TV Detox home screen
com.tcl.inputmethod.international	TV keyboard
com.google.android.webview	Web pages inside apps
com.tcl.android.webview	Web pages inside apps
com.google.android.apps.nbu.smartconnect.tv	Google TV phone pairing
com.android.providers.userdictionary	Keyboard dictionary
EOF

# One line each: the awk that ships with macOS won't take a newline inside -v.
NEVER=$(printf '%s' "$NEVER" | tr '\n' ' '); KEEP=$(printf '%s' "$KEEP" | tr '\n' ' '); SAFE=$(printf '%s' "$SAFE" | tr '\n' ' ')
awk -F'\t' -v never="$NEVER" -v keep="$KEEP" -v safe="$SAFE" -v tmp="$tmp" '
  BEGIN {
    n=split(never,x," "); for (i=1;i<=n;i++) if (x[i]!="") NV[x[i]]=1
    n=split(keep,x," ");  for (i=1;i<=n;i++) if (x[i]!="") KP[x[i]]=1
    n=split(safe,x," ");  for (i=1;i<=n;i++) if (x[i]!="") SF[x[i]]=1
    while ((getline l < (tmp "/flags")) > 0) { split(l,y,"\t"); FL[y[1]]=y[2] }
    while ((getline l < (tmp "/off")) > 0) OF[l]=1
    while ((getline l < (tmp "/names")) > 0) { split(l,y,"\t"); NM[y[1]]=y[2] }
    ORD["never"]=0; ORD["keep"]=1; ORD["safe"]=2; ORD["review"]=3
    while ((getline l < (tmp "/all")) > 0) {
      p=l; st=(p in OF) ? "off" : "on"
      if (FL[p] ~ /PERSISTENT/) v="never"
      else if (p in NV) v="never"
      else if (p in KP) v="keep"
      else if (p in SF) v="safe"
      else v="review"
      name=(p in NM) ? NM[p] : ""
      if (FL[p] ~ /PERSISTENT/) name=(name=="" ? "" : name " ") "(Android keeps it running)"
      printf "%d\t%s\t%s\t%s\t%s\n", ORD[v], v, st, p, name
      C[v]++
    }
    printf "SUMMARY\t%s\t%s\t%s\n", C["never"]+0, C["keep"]+0, C["safe"]+0 > (tmp "/summary")
    printf "%d\n", C["review"]+0 >> (tmp "/summary")
    close(tmp "/summary")
    exit
  }
' | sort -k1,1n -k4,4 | cut -f2-

[ -s "$tmp/summary" ] || { echo "Couldn't sort the app list. Nothing was changed." >&2; exit 1; }
n() { awk -v k="$1" 'NR==1{print $k}' "$tmp/summary" 2>/dev/null; }
echo
echo "never $(n 2) | keep $(n 3) | safe $(n 4) | review $(awk 'NR==2{print $1}' "$tmp/summary" 2>/dev/null)"
echo
echo "safe = commonly preinstalled junk. review = unknown, so leave it on unless you know it."
echo "Before turning anything off, read docs/debloat.md and ask someone before every change."
