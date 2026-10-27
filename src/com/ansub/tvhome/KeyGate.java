package com.ansub.tvhome;

// Which remote keys the lock screen keeps away from the apps underneath. OK or an arrow on the lock
// screen opens the apps, and every event of that same key (its repeats while held, and its key-up)
// is swallowed, so holding OK can't reach the app tiles and open an edit menu once the slide ends.
// While locked or sliding, the other arrows, OK and Menu are swallowed too. Back, volume and media keys
// always go through. Key codes are android.view.KeyEvent's, kept as numbers so this runs off-device.
final class KeyGate {
    enum Result { PASS, CONSUME, OPEN }

    private static final int DPAD_UP = 19, DPAD_DOWN = 20, DPAD_LEFT = 21, DPAD_RIGHT = 22, DPAD_CENTER = 23,
            ENTER = 66, NUMPAD_ENTER = 160, MENU = 82;

    private int held = -1; // the key being swallowed until its key-up

    Result on(int code, boolean down, boolean locked, boolean busy) {
        if (code == held) {
            if (!down) held = -1;
            return Result.CONSUME;
        }
        if (!locked && !busy) return Result.PASS;
        if (navigation(code)) {
            if (!down) return Result.CONSUME;
            held = code;
            return locked && !busy ? Result.OPEN : Result.CONSUME;
        }
        return code == MENU ? Result.CONSUME : Result.PASS;
    }

    private static boolean navigation(int code) {
        return code == DPAD_CENTER || code == ENTER || code == NUMPAD_ENTER
                || code == DPAD_UP || code == DPAD_DOWN || code == DPAD_LEFT || code == DPAD_RIGHT;
    }
}
