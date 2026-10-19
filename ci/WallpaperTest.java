package com.ansub.tvhome;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.PixelCopy;
import android.view.View;
import android.widget.FrameLayout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Runs only through ci/test-wallpaper.sh on an emulator. */
public final class WallpaperTest extends Instrumentation {
    private Activity activity;
    private WallpaperView view;

    private void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }

    @Override
    public void onStart() {
        Bundle result = new Bundle();
        try {
            WallpaperDrawable image = WallpaperDrawable.load(getContext(), "uhd.png", 1920, 1080);
            check(image != null && image.getIntrinsicWidth() == 1920 && image.getIntrinsicHeight() == 1080,
                    "A UHD image must decode at 1080p for a 1080p UI");
            WallpaperDrawable nativeSize = WallpaperDrawable.load(getContext(), "uhd.png", 3840, 2160);
            check(nativeSize != null && nativeSize.getIntrinsicWidth() == 3840 && nativeSize.getIntrinsicHeight() == 2160,
                    "A UHD UI must retain UHD source detail");
            nativeSize = null;
            WallpaperDrawable larger = WallpaperDrawable.load(getContext(), "oversized.png", 1920, 1080);
            check(larger != null && larger.getIntrinsicWidth() == 1920 && larger.getIntrinsicHeight() == 1080,
                    "Larger images must be sampled to the UI size before allocation");
            larger = null;
            WallpaperDrawable automatic = WallpaperDrawable.load(getContext(), "uhd.png");
            android.util.DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
            check(automatic != null && automatic.getIntrinsicWidth() <= metrics.widthPixels
                    && automatic.getIntrinsicHeight() <= metrics.heightPixels,
                    "The default loader must follow Android UI metrics");
            automatic = null;
            WallpaperDrawable smaller = WallpaperDrawable.load(getContext(), "uhd.png", 7680, 4320);
            check(smaller != null && smaller.getIntrinsicWidth() == 3840 && smaller.getIntrinsicHeight() == 2160,
                    "Decoding must not enlarge the source image");
            smaller = null;
            check(WallpaperDrawable.load(getContext(), "uhd.png", 0, 1080) == null,
                    "Invalid UI dimensions must leave the gradient available");

            Bitmap cropped = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
            image.setBounds(20, 30, 180, 170);
            image.draw(new Canvas(cropped));
            check(Color.alpha(cropped.getPixel(0, 0)) == 0, "Drawing must stay inside its bounds");
            check(cropped.getPixel(100, 50) == Color.GREEN,
                    "Center cropping must retain the center of the source without stretching");
            check(Color.green(cropped.getPixel(100, 160)) < 255,
                    "The lower image must be shaded for readable captions");
            cropped.recycle();
            check(WallpaperDrawable.load(getContext(), "missing.jpg") == null,
                    "A missing asset must leave the original gradient available");
            check(WallpaperDrawable.load(getContext(), "invalid.jpg") == null,
                    "An invalid asset must leave the original gradient available");

            activity = startActivitySync(new Intent(Intent.ACTION_MAIN)
                    .setClassName("com.ansub.tvhome", "com.ansub.tvhome.MainActivity")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            runOnMainSync(() -> {
                view = new WallpaperView(activity, image);
                // Use a known viewport, independent of an emulator's navigation bar.
                activity.addContentView(view, new FrameLayout.LayoutParams(1920, 1080));
            });
            awaitSurface(1920, 1080);
            checkPixels();

            runOnMainSync(() -> {
                view.getLayoutParams().width = 480;
                view.getLayoutParams().height = 320;
                view.requestLayout();
            });
            awaitSurface(480, 320);
            checkPixels();

            runOnMainSync(() -> view.setVisibility(View.GONE));
            waitForIdleSync();
            long until = SystemClock.uptimeMillis() + 5000;
            while (view.getHolder().getSurface().isValid() && SystemClock.uptimeMillis() < until)
                SystemClock.sleep(50);
            check(!view.getHolder().getSurface().isValid(), "Hiding the view must release its surface");
            runOnMainSync(() -> {
                view.getLayoutParams().width = 1920;
                view.getLayoutParams().height = 1080;
                view.setVisibility(View.VISIBLE);
                view.requestLayout();
            });
            awaitSurface(1920, 1080);
            checkPixels();
            result.putString("stream", "PASS: automatic UI-sized decode and surface, native UHD decode, oversized sampling, crop, shade, "
                    + "missing/invalid fallback, aspect ratio and surface recreation\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable failure) {
            result.putString("stream", "FAIL: " + failure + "\n");
            finish(Activity.RESULT_CANCELED, result);
        } finally {
            if (activity != null) runOnMainSync(() -> activity.finish());
        }
    }

    private void awaitSurface(int width, int height) {
        long until = SystemClock.uptimeMillis() + 5000;
        AtomicReference<Rect> frame = new AtomicReference<>();
        do {
            runOnMainSync(() -> frame.set(new Rect(view.getHolder().getSurfaceFrame())));
            if (view.getHolder().getSurface().isValid()
                    && frame.get().width() == width && frame.get().height() == height) return;
            SystemClock.sleep(50);
        } while (SystemClock.uptimeMillis() < until);
        throw new AssertionError("Expected surface " + width + "x" + height + ", got " + frame.get());
    }

    private void checkPixels() throws InterruptedException {
        if (Build.VERSION.SDK_INT < 24) return;
        Bitmap copy = Bitmap.createBitmap(160, 90, Bitmap.Config.ARGB_8888);
        CountDownLatch done = new CountDownLatch(1);
        int[] status = {-1};
        runOnMainSync(() -> PixelCopy.request(view, copy, value -> {
            status[0] = value;
            done.countDown();
        }, new Handler(Looper.getMainLooper())));
        check(done.await(5, TimeUnit.SECONDS) && status[0] == PixelCopy.SUCCESS,
                "The surface must contain a posted frame");
        check(copy.getPixel(80, 20) == Color.GREEN, "The UI-sized surface must contain the source image");
        copy.recycle();
    }
}
