package com.ansub.tvhome;

import android.os.SystemClock;
import android.view.Choreographer;

// One animation, driven frame by frame. Android's own animators follow the TV's "animation scale",
// and a TV with animations turned off (to feel faster) would make them jump straight to the end.
// This keeps the home screen's slides and fades smooth whatever that setting is.
final class Motion implements Choreographer.FrameCallback {
    interface Frame { void at(float eased); }

    private final long durationMs;
    private final Frame frame;
    private final Runnable done;
    private long start = -1;
    private boolean cancelled;

    private Motion(long durationMs, Frame frame, Runnable done) {
        this.durationMs = durationMs;
        this.frame = frame;
        this.done = done;
    }

    // Starts on the next frame. Call from the main thread.
    static Motion run(long durationMs, Frame f, Runnable done) {
        Motion m = new Motion(durationMs, f, done);
        f.at(0f);
        Choreographer.getInstance().postFrameCallback(m);
        return m;
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (cancelled) return;
        long now = SystemClock.uptimeMillis();
        if (start < 0) start = now;
        float p = Ease.progress(start, now, durationMs);
        frame.at(Ease.outCubic(p));
        if (p < 1f) Choreographer.getInstance().postFrameCallback(this);
        else if (done != null) done.run();
    }

    // Stops where it is; done never runs.
    void cancel() {
        cancelled = true;
        Choreographer.getInstance().removeFrameCallback(this);
    }
}
