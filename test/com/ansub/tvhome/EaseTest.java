package com.ansub.tvhome;

import static com.ansub.tvhome.T.eq;

public final class EaseTest {
    public static void testOutCubicEnds() {
        eq(0f, Ease.outCubic(0f));
        eq(1f, Ease.outCubic(1f));
    }

    public static void testOutCubicMiddle() {
        eq(0.875f, Ease.outCubic(0.5f));
    }

    public static void testProgressAlongTheWay() {
        eq(0f, Ease.progress(1000, 1000, 500));
        eq(0.5f, Ease.progress(1000, 1250, 500));
    }

    public static void testProgressClamped() {
        eq(1f, Ease.progress(1000, 2000, 500));
        eq(0f, Ease.progress(1000, 900, 500));
    }

    public static void testZeroDurationIsDone() {
        eq(1f, Ease.progress(1000, 900, 0));
    }
}
