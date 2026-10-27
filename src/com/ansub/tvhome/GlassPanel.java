package com.ansub.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

// The frosted glass behind the apps, like Apple TV: from panelTop down, the photo's blurred copy
// (drawn exactly where the sharp photo is, so it lines up), a dark tint, and a thin bright top edge.
final class GlassPanel extends View {
    private static final int TINT = 0x4D141419, EDGE = 0x1FFFFFFF;

    private final Paint photoPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint edgePaint = new Paint();
    private final Rect dst = new Rect();
    private Bitmap blurred;
    private int panelTop;

    GlassPanel(Context c) {
        super(c);
        edgePaint.setColor(EDGE);
        setWillNotDraw(false);
    }

    void setPhoto(Bitmap b) {
        blurred = b;
        invalidate();
    }

    void setPanelTop(int y) {
        if (y == panelTop) return;
        panelTop = y;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        canvas.save();
        canvas.clipRect(0, panelTop, w, h);
        if (blurred != null) {
            // Centre-crop like the photo's ImageView, so the blur sits right behind what it blurs.
            float s = Math.max(w / (float) blurred.getWidth(), h / (float) blurred.getHeight());
            int bw = Math.round(blurred.getWidth() * s), bh = Math.round(blurred.getHeight() * s);
            dst.set((w - bw) / 2, (h - bh) / 2, (w - bw) / 2 + bw, (h - bh) / 2 + bh);
            canvas.drawBitmap(blurred, null, dst, photoPaint);
        }
        canvas.drawColor(TINT);
        canvas.restore();
        canvas.drawRect(0, panelTop, w, panelTop + Math.max(1, h / 1080f * 2), edgePaint);
    }
}
