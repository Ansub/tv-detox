package com.ansub.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

import java.io.IOException;
import java.io.InputStream;

/** A static image with center cropping and a shade behind the lower tiles. */
final class WallpaperDrawable extends Drawable {
    // Additional allocation guard for large UI sizes; ordinary images follow the UI metrics.
    private static final long MAX_PIXELS = 3840L * 2160;
    private static final int MAX_EDGE = 4096;
    private final Bitmap bitmap;
    private final Paint image = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    private final Paint shade = new Paint();
    private final RectF destination = new RectF();
    private int alpha = 255;

    private WallpaperDrawable(Bitmap bitmap) { this.bitmap = bitmap; }

    /** Sample for the UI Android exposes, which can be smaller than the TV panel. */
    static WallpaperDrawable load(Context context, String asset) {
        DisplayMetrics size = context.getResources().getDisplayMetrics();
        return load(context, asset, size.widthPixels, size.heightPixels);
    }

    /** Missing, unreadable or unallocatable images leave the original gradient in place. */
    static WallpaperDrawable load(Context context, String asset, int targetWidth, int targetHeight) {
        if (targetWidth <= 0 || targetHeight <= 0) return null;
        int maxWidth = Math.min(MAX_EDGE, targetWidth);
        int maxHeight = Math.min(MAX_EDGE, targetHeight);
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try (InputStream stream = context.getAssets().open(asset)) {
                BitmapFactory.decodeStream(stream, null, options);
            }
            if (options.outWidth <= 0 || options.outHeight <= 0) return null;

            int sample = 1;
            while (true) {
                long width = ((long) options.outWidth + sample - 1) / sample;
                long height = ((long) options.outHeight + sample - 1) / sample;
                if (width <= maxWidth && height <= maxHeight && width * height <= MAX_PIXELS) break;
                if (sample >= (1 << 30)) return null;
                sample *= 2;
            }
            options.inSampleSize = sample;
            options.inJustDecodeBounds = false;
            options.inScaled = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap decoded;
            try (InputStream stream = context.getAssets().open(asset)) {
                decoded = BitmapFactory.decodeStream(stream, null, options);
            }
            return decoded == null ? null : new WallpaperDrawable(decoded);
        } catch (IOException | OutOfMemoryError failure) {
            return null;
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        if (bounds.isEmpty()) return;
        float scale = Math.max(bounds.width() / (float) bitmap.getWidth(),
                bounds.height() / (float) bitmap.getHeight());
        float width = bitmap.getWidth() * scale;
        float height = bitmap.getHeight() * scale;
        float left = bounds.left + (bounds.width() - width) / 2f;
        float top = bounds.top + (bounds.height() - height) / 2f;
        destination.set(left, top, left + width, top + height);
        shade.setShader(new LinearGradient(0, bounds.top + bounds.height() * 0.4f,
                0, bounds.bottom, 0x00000000, 0x99000000, Shader.TileMode.CLAMP));
    }

    @Override
    public void draw(Canvas canvas) {
        if (getBounds().isEmpty()) return;
        int saved = canvas.save();
        canvas.clipRect(getBounds());
        canvas.drawBitmap(bitmap, null, destination, image);
        canvas.drawRect(getBounds(), shade);
        canvas.restoreToCount(saved);
    }

    @Override public int getIntrinsicWidth() { return bitmap.getWidth(); }
    @Override public int getIntrinsicHeight() { return bitmap.getHeight(); }
    @Override public int getOpacity() { return alpha == 255 ? PixelFormat.OPAQUE : PixelFormat.TRANSLUCENT; }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha;
        image.setAlpha(alpha);
        shade.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
        image.setColorFilter(filter);
        invalidateSelf();
    }
}
