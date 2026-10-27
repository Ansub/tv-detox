package com.ansub.tvhome;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.Locale;

// What the projector shows when it turns on, like a locked iPhone: the date, a very big clock,
// room for small widgets under the time, the profile picture and "Press OK to open".
// Laid out at 1920x1080 and scaled like the rest of the home screen. Clear except for the text,
// so the photo behind it shows through.
final class LockScreen {
    private final float scale;
    private final FrameLayout view;
    private final FrameLayout widgetSlot;
    private final TextView avatar;
    private final TextClock time;

    LockScreen(Context c, float scale, Typeface semibold, Typeface bold) {
        this.scale = scale;
        view = new FrameLayout(c);
        view.setClipChildren(false);

        // Darker at the top and bottom so the white text reads on any photo.
        View shade = new View(c);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0x55000000, 0x00000000, 0x40000000}));
        view.addView(shade, new FrameLayout.LayoutParams(-1, -1));

        String day = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEMMMMd");
        TextClock date = new TextClock(c);
        date.setFormat12Hour(day);
        date.setFormat24Hour(day);
        style(date, 36, 0xEBFFFFFF, semibold);
        view.addView(date, centered(88));

        time = new TextClock(c);
        setClockFormat("h:mm", "H:mm");
        style(time, 269, 0xF0FFFFFF, bold);
        time.setLetterSpacing(-0.035f);
        time.setFontFeatureSettings("tnum");
        time.setIncludeFontPadding(false);
        time.setShadowLayer(px(30), 0, px(4), 0x40000000);
        view.addView(time, centered(119));

        widgetSlot = new FrameLayout(c);
        widgetSlot.setClipChildren(false);
        view.addView(widgetSlot, centered(415));

        avatar = avatar(c, scale, bold);
        view.addView(avatar, avatarPosition(scale));

        TextView hint = new TextView(c);
        style(hint, 23, 0xBFFFFFFF, semibold);
        hint.setText(Strings.t("Press OK to open"));
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        hp.bottomMargin = px(46);
        view.addView(hint, hp);
    }

    View view() { return view; }

    FrameLayout widgetSlot() { return widgetSlot; }

    void setInitial(String initial) { avatar.setText(initial); }

    void setGuest(boolean guest) { tint(avatar, guest); }

    // The owner's picture is orange-to-pink, Guest's grey, like the circles in "Who's watching?".
    static void tint(TextView avatar, boolean guest) {
        ((GradientDrawable) avatar.getBackground()).setColors(guest
                ? new int[] {0xFF8E8E93, 0xFF48484A} : new int[] {0xFFFF9F0A, 0xFFFF375F});
    }

    // The profile picture: the initial in an orange-to-pink circle with a light ring, like an Apple
    // account picture. The apps screen shows the same one in the same spot.
    static TextView avatar(Context c, float scale, Typeface bold) {
        TextView a = new TextView(c);
        a.setTextSize(TypedValue.COMPLEX_UNIT_PX, Math.round(32 * scale));
        a.setTextColor(0xFFFFFFFF);
        if (bold != null) a.setTypeface(bold);
        a.setGravity(Gravity.CENTER);
        a.setIncludeFontPadding(false);
        GradientDrawable circle = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] {0xFFFF9F0A, 0xFFFF375F});
        circle.setShape(GradientDrawable.OVAL);
        circle.setStroke(Math.max(1, Math.round(5 * scale)), 0x99FFFFFF);
        a.setBackground(circle);
        return a;
    }

    static FrameLayout.LayoutParams avatarPosition(float scale) {
        int size = Math.round(77 * scale);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size, Gravity.TOP | Gravity.END);
        lp.topMargin = Math.round(42 * scale);
        lp.rightMargin = Math.round(50 * scale);
        return lp;
    }

    // The home screen's clock override (--es clock 12|24) applies here too.
    void setClockFormat(String f12, String f24) {
        time.setFormat12Hour(f12);
        time.setFormat24Hour(f24);
    }

    private int px(int v) { return Math.round(v * scale); }

    private void style(TextView t, int sizePx, int color, Typeface tf) {
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(sizePx));
        t.setTextColor(color);
        if (tf != null) t.setTypeface(tf);
        t.setSingleLine(true);
    }

    private FrameLayout.LayoutParams centered(int top) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        lp.topMargin = px(top);
        return lp;
    }
}
