package com.ansub.tvhome;

// Which screen is up, the lock screen or the apps, and when to go back to the lock screen.
// While a slide is running, more presses are ignored, so hammering OK or Back can't leave the page
// half-way between the two.
final class LockState {
    enum Screen { LOCK, HOME }

    static final long IDLE_MS = 120_000; // two minutes without a key on the apps screen locks it

    private final long idleMs;
    private Screen screen = Screen.LOCK;
    private boolean busy;
    private long lastTouch;
    private boolean woke;

    LockState(long idleMs) {
        this.idleMs = idleMs;
    }

    Screen screen() { return screen; }

    boolean busy() { return busy; }

    // OK on the lock screen: start sliding the apps up. False if that isn't possible right now.
    boolean open(long nowMs) {
        if (busy || screen != Screen.LOCK) return false;
        screen = Screen.HOME;
        busy = true;
        lastTouch = nowMs;
        return true;
    }

    // Back (or idle) on the apps: start sliding back to the lock screen.
    boolean lock() {
        if (busy || screen != Screen.HOME) return false;
        screen = Screen.LOCK;
        busy = true;
        return true;
    }

    void slideDone() { busy = false; }

    void touched(long nowMs) { lastTouch = nowMs; }

    boolean idleExpired(long nowMs) {
        return screen == Screen.HOME && !busy && nowMs - lastTouch >= idleMs;
    }

    // The screen went off: the next time the home screen comes back, it starts on the lock screen.
    void woke() { woke = true; }

    boolean consumeWake() {
        boolean w = woke;
        woke = false;
        return w;
    }

    // Shown straight away, without a slide (after waking, or Home pressed inside an app).
    void show(Screen s, long nowMs) {
        screen = s;
        busy = false;
        lastTouch = nowMs;
    }
}
