package com.ansub.tvhome;

import static com.ansub.tvhome.T.eq;

// Key codes are android.view.KeyEvent's.
public final class KeyGateTest {
    private static final int OK = 23, UP = 19, DOWN = 20, LEFT = 21, MENU = 82, BACK = 4, VOLUME_UP = 24;

    public static void testHeldOkOpensOnceAndNeverReachesTheApps() {
        KeyGate g = new KeyGate();
        eq(KeyGate.Result.OPEN, g.on(OK, true, true, false));
        eq(KeyGate.Result.CONSUME, g.on(OK, true, false, true));   // repeat while sliding
        eq(KeyGate.Result.CONSUME, g.on(OK, true, false, false));  // still held after the slide: no edit menu
        eq(KeyGate.Result.CONSUME, g.on(OK, false, false, false)); // its key-up opens nothing
        eq(KeyGate.Result.PASS, g.on(OK, true, false, false));     // a fresh press on the apps works
    }

    public static void testMenuSwallowedWhileLockedOrSliding() {
        KeyGate g = new KeyGate();
        eq(KeyGate.Result.CONSUME, g.on(MENU, true, true, false));
        eq(KeyGate.Result.CONSUME, g.on(MENU, true, false, true));
        eq(KeyGate.Result.PASS, g.on(MENU, true, false, false));
    }

    public static void testArrowsSwallowedWhileSliding() {
        KeyGate g = new KeyGate();
        eq(KeyGate.Result.CONSUME, g.on(DOWN, true, false, true));
        eq(KeyGate.Result.CONSUME, g.on(DOWN, false, false, false));
    }

    public static void testAnyArrowOpensTheLockScreen() {
        eq(KeyGate.Result.OPEN, new KeyGate().on(LEFT, true, true, false));
        eq(KeyGate.Result.OPEN, new KeyGate().on(UP, true, true, false));
    }

    public static void testBackAndVolumeAlwaysPass() {
        KeyGate g = new KeyGate();
        eq(KeyGate.Result.PASS, g.on(VOLUME_UP, true, true, false));
        eq(KeyGate.Result.PASS, g.on(BACK, true, true, false));
        eq(KeyGate.Result.PASS, g.on(BACK, true, false, true));
    }
}
