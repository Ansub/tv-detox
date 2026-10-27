package com.ansub.tvhome;

import static com.ansub.tvhome.T.check;

public final class BootRedirectTest {
    public static void testOpensRightAfterBoot() {
        check(BootRedirect.shouldOpen(60_000, true, false, false), "service connected a minute after boot");
    }

    public static void testWindowIsTwoMinutes() {
        check(BootRedirect.shouldOpen(119_999, true, false, false), "just inside");
        check(!BootRedirect.shouldOpen(120_000, true, false, false), "an app update later must not pop the home screen up");
    }

    public static void testNotDuringFirstSetup() {
        check(!BootRedirect.shouldOpen(60_000, false, false, false), "setup not finished");
    }

    public static void testNotWhenStayingOnGoogleTv() {
        check(!BootRedirect.shouldOpen(60_000, true, true, false), "Back was held to stay on Google TV");
    }

    // Once this boot's home screen has been up, a reconnect (crash, service toggled) must not pop it up
    // over whatever is in front, HDMI included.
    public static void testOnlyOncePerBoot() {
        check(!BootRedirect.shouldOpen(60_000, true, false, true), "home already shown this boot");
    }
}
