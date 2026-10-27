package com.ansub.tvhome;

import static com.ansub.tvhome.T.check;
import static com.ansub.tvhome.T.eq;

public final class LockStateTest {
    public static void testStartsLocked() {
        LockState s = new LockState(LockState.IDLE_MS);
        eq(LockState.Screen.LOCK, s.screen());
        check(!s.busy(), "not busy at start");
    }

    public static void testRapidOpenOnlyOnce() {
        LockState s = new LockState(LockState.IDLE_MS);
        check(s.open(0), "first OK opens");
        check(!s.open(10), "second OK while sliding is ignored");
        check(!s.open(20), "third OK while sliding is ignored");
        eq(LockState.Screen.HOME, s.screen());
    }

    public static void testLockWhileBusyIgnored() {
        LockState s = new LockState(LockState.IDLE_MS);
        s.open(0);
        check(!s.lock(), "Back during the slide is ignored");
        s.slideDone();
        check(s.lock(), "Back after the slide locks");
        eq(LockState.Screen.LOCK, s.screen());
    }

    public static void testOpenWhenHomeDoesNothing() {
        LockState s = new LockState(LockState.IDLE_MS);
        s.open(0);
        s.slideDone();
        check(!s.open(500), "already home");
    }

    public static void testIdle() {
        LockState s = new LockState(LockState.IDLE_MS);
        s.open(0);
        s.slideDone();
        s.touched(1000);
        check(!s.idleExpired(120_999), "not yet");
        check(s.idleExpired(121_000), "two minutes after the last key");
    }

    public static void testIdleOnlyOnHome() {
        LockState s = new LockState(LockState.IDLE_MS);
        check(!s.idleExpired(999_999), "already locked");
    }

    public static void testWakeOnce() {
        LockState s = new LockState(LockState.IDLE_MS);
        s.woke();
        check(s.consumeWake(), "first resume after waking");
        check(!s.consumeWake(), "only once");
    }

    public static void testIdleIsTwoMinutes() {
        eq(120_000L, LockState.IDLE_MS);
    }

    public static void testShowJumpsWithoutSlide() {
        LockState s = new LockState(LockState.IDLE_MS);
        s.open(0);
        s.show(LockState.Screen.LOCK, 5_000);
        eq(LockState.Screen.LOCK, s.screen());
        check(!s.busy(), "no slide running");
        s.show(LockState.Screen.HOME, 10_000);
        check(!s.idleExpired(129_999), "idle counts from when it was shown");
        check(s.idleExpired(130_000), "two minutes after it was shown");
    }
}
