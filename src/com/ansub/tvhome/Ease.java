package com.ansub.tvhome;

// Timing for the home screen's own animations. Kept apart from Android so it can be tested on a computer.
final class Ease {
    private Ease() {}

    // Fast start, gentle stop: the curve Apple TV's panels slide on.
    static float outCubic(float t) {
        float u = 1f - t;
        return 1f - u * u * u;
    }

    // How far an animation that started at startMs is at nowMs, from 0 to 1.
    static float progress(long startMs, long nowMs, long durationMs) {
        if (durationMs <= 0) return 1f;
        float p = (nowMs - startMs) / (float) durationMs;
        return Math.max(0f, Math.min(1f, p));
    }
}
