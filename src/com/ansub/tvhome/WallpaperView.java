package com.ansub.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/** Draws the wallpaper once on a surface that follows the UI layout. */
final class WallpaperView extends SurfaceView implements SurfaceHolder.Callback {
    private final WallpaperDrawable wallpaper;

    WallpaperView(Context context, WallpaperDrawable wallpaper) {
        super(context);
        this.wallpaper = wallpaper;
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        getHolder().setFormat(PixelFormat.RGBX_8888);
        getHolder().setSizeFromLayout();
        getHolder().addCallback(this);
    }

    @Override public void surfaceCreated(SurfaceHolder holder) {}
    @Override public void surfaceDestroyed(SurfaceHolder holder) {}

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Canvas canvas = null;
        try {
            canvas = holder.lockCanvas();
            if (canvas == null) return;
            canvas.drawColor(Color.BLACK);
            wallpaper.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            wallpaper.draw(canvas);
        } catch (RuntimeException | OutOfMemoryError failure) {
            fallback(failure);
        } finally {
            if (canvas != null) try {
                holder.unlockCanvasAndPost(canvas);
            } catch (RuntimeException failure) { fallback(failure); }
        }
    }

    private void fallback(Throwable failure) {
        Log.w("Wallpaper", "Unable to draw the wallpaper; keeping the gradient", failure);
        post(() -> setVisibility(GONE));
    }
}
