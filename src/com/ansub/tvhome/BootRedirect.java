package com.ansub.tvhome;

// At boot, Google TV's home screen can open a moment before HomeButton starts (it waits for the TV to
// unlock its storage), so HomeButton never sees it open. On a projector seen to start on Google TV's
// home, it then opens this one as soon as it connects, but only in the first two minutes after boot
// and only if this home screen hasn't been up yet this boot: later connects (a crash, an update, the
// service switched back on) must not pop the home screen up over an app or HDMI.
final class BootRedirect {
    private BootRedirect() {}

    static final long WINDOW_MS = 120_000;

    static boolean shouldOpen(long sinceBootMs, boolean setupDone, boolean stayingOnGoogleTv, boolean shownThisBoot) {
        return sinceBootMs < WINDOW_MS && setupDone && !stayingOnGoogleTv && !shownThisBoot;
    }
}
