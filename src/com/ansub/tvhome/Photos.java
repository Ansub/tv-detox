package com.ansub.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// The lock screen's photos (assets/photos), one at a time, changing every 15 minutes. Each comes with
// a small blurred copy for the frosted-glass panel. Photos are decoded off the main thread, and only
// while the home screen is in front.
final class Photos {
    interface Listener { void onPhoto(Bitmap full, Bitmap blurred); }

    static final long INTERVAL_MS = 900_000;
    private static final String DIR = "photos";
    private static final int BLUR_DOWNSCALE = 8, BLUR_RADIUS = 4, BLUR_PASSES = 3;

    private final Context context;
    private final int width, height;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private PhotoOrder order;
    private Listener listener;
    private long shownAt = -1; // uptime of the last photo shown, -1 before the first
    private final Runnable change = this::loadNext;

    Photos(Context c, int widthPx, int heightPx) {
        context = c.getApplicationContext();
        width = widthPx;
        height = heightPx;
    }

    // Shows a photo now if none is up or one is due, then keeps changing them until stop().
    void start(Listener l) {
        listener = l;
        main.removeCallbacks(change);
        long wait = shownAt < 0 ? 0 : Math.max(0, shownAt + interval() - SystemClock.uptimeMillis());
        main.postDelayed(change, wait);
    }

    void stop() {
        main.removeCallbacks(change);
        listener = null;
    }

    // From prefs, so a short interval can be tried out: --el photo_ms 20000 (0 goes back to 15 minutes).
    private long interval() {
        long ms = context.getSharedPreferences("config", 0).getLong("photo_ms", 0);
        return ms > 0 ? ms : INTERVAL_MS;
    }

    private void loadNext() {
        if (order == null) {
            List<String> names = new ArrayList<>();
            try {
                for (String n : context.getAssets().list(DIR)) if (n.endsWith(".jpg")) names.add(n);
            } catch (Exception ignored) {}
            if (names.isEmpty()) return;
            order = new PhotoOrder(names, System.currentTimeMillis());
        }
        final String name = order.next();
        worker.execute(() -> {
            Bitmap full = decode(DIR + "/" + name);
            Bitmap blurred = full == null ? null : blur(full);
            main.post(() -> {
                if (listener == null) return;
                if (full != null) {
                    shownAt = SystemClock.uptimeMillis();
                    listener.onPhoto(full, blurred);
                }
                main.removeCallbacks(change);
                main.postDelayed(change, interval());
            });
        });
    }

    // Decoded no bigger than needed for the screen.
    private Bitmap decode(String path) {
        try (InputStream in = context.getAssets().open(path)) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, o);
            int sample = 1;
            while (o.outWidth / (sample * 2) >= width && o.outHeight / (sample * 2) >= height) sample *= 2;
            o = new BitmapFactory.Options();
            o.inSampleSize = sample;
            try (InputStream again = context.getAssets().open(path)) {
                return BitmapFactory.decodeStream(again, null, o);
            }
        } catch (Throwable t) {
            return null;
        }
    }

    // A tiny copy, box-blurred a few times: drawn stretched behind the glass panel it looks like
    // frosted glass, without a live blur this TV's chip couldn't keep up with.
    private static Bitmap blur(Bitmap src) {
        int w = Math.max(1, src.getWidth() / BLUR_DOWNSCALE), h = Math.max(1, src.getHeight() / BLUR_DOWNSCALE);
        Bitmap small = Bitmap.createScaledBitmap(src, w, h, true);
        int[] px = new int[w * h], tmp = new int[w * h];
        small.getPixels(px, 0, w, 0, 0, w, h);
        for (int i = 0; i < BLUR_PASSES; i++) {
            boxBlur(px, tmp, w, h, BLUR_RADIUS, true);
            boxBlur(tmp, px, w, h, BLUR_RADIUS, false);
        }
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        out.setPixels(px, 0, w, 0, 0, w, h);
        return out;
    }

    // One pass of a running-sum box blur, along rows or along columns.
    private static void boxBlur(int[] in, int[] out, int w, int h, int r, boolean rows) {
        int lines = rows ? h : w, len = rows ? w : h;
        for (int line = 0; line < lines; line++) {
            int a = 0, red = 0, g = 0, b = 0;
            for (int i = -r; i <= r; i++) {
                int c = in[index(line, Math.max(0, Math.min(len - 1, i)), w, rows)];
                a += c >>> 24; red += (c >> 16) & 0xFF; g += (c >> 8) & 0xFF; b += c & 0xFF;
            }
            int n = 2 * r + 1;
            for (int i = 0; i < len; i++) {
                out[index(line, i, w, rows)] = ((a / n) << 24) | ((red / n) << 16) | ((g / n) << 8) | (b / n);
                int add = in[index(line, Math.min(len - 1, i + r + 1), w, rows)];
                int drop = in[index(line, Math.max(0, i - r), w, rows)];
                a += (add >>> 24) - (drop >>> 24);
                red += ((add >> 16) & 0xFF) - ((drop >> 16) & 0xFF);
                g += ((add >> 8) & 0xFF) - ((drop >> 8) & 0xFF);
                b += (add & 0xFF) - (drop & 0xFF);
            }
        }
    }

    private static int index(int line, int i, int w, boolean rows) {
        return rows ? line * w + i : i * w + line;
    }
}
