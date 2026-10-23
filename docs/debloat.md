# Cleaning your TV safely

TV Detox does two jobs. The home screen is automatic. Turning off the junk is a judgement
call for every app, so it is left to whoever is driving the setup. This page is that
judgement, plus the rules that keep it reversible.

Start with `sh candidates.sh <TV_IP>`. It first says how the TV is doing (memory, swap, how
busy it is, how many apps are off), then lists every app on the TV with a verdict, and changes
nothing:

| verdict | meaning |
| --- | --- |
| `never` | the TV needs it, or Android keeps it running whatever you do. Leave it on. |
| `keep` | it carries a feature you may use, casting or search or Live TV. Off only if unused. |
| `safe` | commonly preinstalled junk: ads, sponsored rows, unused vendor and Google apps. |
| `app` | an app on the TV's app list, or one you installed. Yours to keep, or to turn off if unused. |
| `review` | nothing known about it. Leave it on unless you know what it is. |

The verdict comes from [packages.txt](../packages.txt), the one list of what each app is, or from
what the TV itself says: apps Android keeps running, apps that provide an HDMI or tuner input, the
keyboard in use and the TV's home screen are `never`; an app that handles a button on the remote
says which; apps you installed and apps on the TV's app list are `app`.

Work down the list from the top. Put what you want off in a file, one line each:

```
off com.google.android.tvrecommendations
off com.google.android.backdrop
global window_animation_scale 0
```

and make the changes with `sh apply.sh <TV_IP> my-tv.txt`. It shows what it would do, asks once,
refuses what the TV needs, and writes every change to an undo list on the TV before making it.
`sh check.sh <TV_IP>` later checks it all stuck, and `sh undo.sh <TV_IP> --cleanup` puts it back.
Turn off, wait, check the TV still works.

## The four rules

### 1. Turn apps off, never uninstall them

`pm disable-user --user 0 <package>` hides an app and `pm enable <package>` brings it back.
`pm uninstall` on a system app cannot be undone from the TV. Everything on the TV should be
reversible from the TV, so disable, and keep every package you touch in the undo list.

### 2. Never turn off what starts the TV

The screen, the remote, the keyboard, the settings app and the package installer are load
bearing. If you turn one off you can end up with a TV that boots to a black screen, and ADB
is your only way back. `candidates.sh` marks these `never` for you.

### 3. Apps Android marks PERSISTENT cannot be turned off

Some apps restart themselves the moment the system comes up. `pm disable-user` will report
success and the app is back after a reboot:

```
adb shell dumpsys package com.tcl.guard | grep -m1 pkgFlags
# pkgFlags=[ SYSTEM HAS_CODE PERSISTENT ALLOW_CLEAR_USER_DATA ... ]
```

If you see `PERSISTENT`, leave it alone. Turning it off does not stick, and on a few TVs it
upsets the system instead. `candidates.sh` marks these `never` too.

### 4. Test casting if you cast

Casting is not one app you can spot by name. On Google TV the receiver is
`com.google.android.apps.mediashell`, and it is easy to disable while sweeping up
"media"-looking names. Keep it on, and after a batch that touched anything Google, cast a
video from your phone and confirm the TV still shows up. If it does not, `pm enable
com.google.android.apps.mediashell` and reboot. On a TV that never casts, it is safe off.

## A package name is not always the app

A package name doesn't always say what the app is. Prime Video, for example, installs as
`com.amazon.amazonvideo.livingroom`, and Netflix as `com.netflix.ninja`. Before turning
something off, look at what is actually inside:

```
adb shell cmd package resolve-activity --brief <package>
```

If it is an app you installed and use, it belongs with your apps whatever it is called.

## Do it in batches

Turn off five or ten apps, then check the TV: change the input, open the settings, play
something, cast something. When a batch is clean, do the next one. If the TV misbehaves you
then know which batch to undo, instead of guessing across forty changes.

A reboot after each batch is worth it. Disabling an app looks instant but its processes and
services can linger until the system restarts, so the TV is only really tested on a fresh
boot.

## If something goes wrong

- Bring one app back: `adb shell pm enable <package>`
- Bring back everything `apply.sh` changed, and keep the home screen: `sh undo.sh <TV_IP> --cleanup`.
- Bring back that and the launchers `install.sh` turned off, and remove the home screen:
  `sh undo.sh <TV_IP>`. Apps you turned off by hand with `pm disable-user` are on neither list,
  so write each one down as you go, and turn them back on with `pm enable`.
- Screen is black but the TV answers ADB: re-enable `com.google.android.tvlauncher`, boot to
  it, and run `undo.sh`.

## What is not a package

Some annoyances are the TV's own behaviour, not an app. A TV that opens Live TV or an input
at boot is set up that way in its own settings, usually under the system or power menu. No
package to disable. Set it on the remote instead.

## Reference

The lists live in one place, [packages.txt](../packages.txt): a verdict, a package, and what it
is, one app per line. `candidates.sh` reads it for the verdicts, `install.sh` for the home screens
it may turn off, and `apply.sh` for what it refuses. Android and Google come first, then one
block per TV maker (TCL, Realtek-based sets like Edenwood and Konka). To add your TV, run
`candidates.sh`, find out what each `review` app does, and add a line per app.

Vendors rename and add to these lists, so treat them as strong hints, not gospel. When in
doubt, leave it on: a slow TV is better than a TV you cannot use.
