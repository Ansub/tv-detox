package com.ansub.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.service.dreams.DreamService;
import android.text.format.DateFormat;
import android.view.View;

import java.util.Date;
import java.util.Random;

// A screensaver that comes with the home screen: stars drifting slowly towards you, and a small clock
// that moves to a new spot every minute so nothing stays put on the screen. Pick it in Settings,
// Screensaver & sleep. It only runs while the screensaver shows: no service in the background, no
// pictures, everything drawn with a few hundred dots.
public class StarfieldDream extends DreamService {
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        MainActivity.loadFonts();
        setContentView(new Stars(this));
    }

    static final class Stars extends View {
        private static final int COUNT = 260;
        private static final float SPEED = 0.0016f;  // depth per frame: a slow drift, not warp speed
        private static final long FRAME_MS = 33;      // about 30 fps, light on a TV's CPU
        private static final long CLOCK_MOVE_MS = 60_000;

        private final Random rnd = new Random();
        private final float[] x = new float[COUNT], y = new float[COUNT], z = new float[COUNT], phase = new float[COUNT];
        private final Paint star = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint time = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint date = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float clockX = -1, clockY;
        private long clockMoved, shownMinute = -1;
        private String t, d; // the time and date, made again only when the minute changes

        private final Runnable tick = new Runnable() {
            @Override public void run() { invalidate(); postDelayed(this, FRAME_MS); }
        };

        Stars(Context c) {
            super(c);
            setBackgroundColor(Color.rgb(3, 4, 12));
            for (int i = 0; i < COUNT; i++) reset(i, rnd.nextFloat());
            time.setColor(0xC8E6EBFF);
            time.setTypeface(MainActivity.regular);
            date.setColor(0x8CC8D2F0);
            date.setTypeface(MainActivity.regular);
        }

        // A star somewhere ahead; depth 1 is as far as they go.
        private void reset(int i, float depth) {
            x[i] = rnd.nextFloat() * 2f - 1f;
            y[i] = rnd.nextFloat() * 2f - 1f;
            z[i] = 0.05f + depth * 0.95f;
            phase[i] = rnd.nextFloat() * 6.283f;
        }

        @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); post(tick); }
        @Override protected void onDetachedFromWindow() { removeCallbacks(tick); super.onDetachedFromWindow(); }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth(), h = getHeight();
            float cx = w / 2f, cy = h / 2f, scale = Math.max(w, h) * 0.5f;

            for (int i = 0; i < COUNT; i++) {
                z[i] -= SPEED;
                float sx = cx + x[i] / z[i] * scale, sy = cy + y[i] / z[i] * scale;
                if (z[i] <= 0.05f || sx < 0 || sx > w || sy < 0 || sy > h) { reset(i, 1f); continue; }
                float near = 1f - z[i]; // 0 far away, 1 close
                phase[i] += 0.04f;
                float twinkle = 0.75f + 0.25f * (float) Math.sin(phase[i]);
                int a = (int) (255 * Math.min(1f, near * 1.3f) * twinkle);
                star.setColor(Color.argb(a, 210 + (int) (45 * near), 220 + (int) (35 * near), 255));
                canvas.drawCircle(sx, sy, 0.6f + near * 2.6f, star);
            }

            // The clock follows the TV's 12 or 24 hour setting, and the date its language.
            long now = System.currentTimeMillis();
            time.setTextSize(h * 0.065f);
            date.setTextSize(h * 0.028f);
            if (now / 60000 != shownMinute) {
                shownMinute = now / 60000;
                t = DateFormat.getTimeFormat(getContext()).format(new Date(now));
                d = DateFormat.format(Strings.datePattern(), now).toString();
            }
            float width = Math.max(time.measureText(t), date.measureText(d));
            if (clockX < 0 || now - clockMoved > CLOCK_MOVE_MS) {
                float m = h * 0.06f;
                clockX = w - width - m - rnd.nextFloat() * w * 0.08f;
                clockY = h - m - rnd.nextFloat() * h * 0.08f;
                clockMoved = now;
            }
            canvas.drawText(t, clockX, clockY - date.getTextSize() * 1.4f, time);
            canvas.drawText(d, clockX, clockY, date);
        }
    }
}
