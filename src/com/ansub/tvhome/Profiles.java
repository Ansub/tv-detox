package com.ansub.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

// The owner and Guest. Guest only changes the look: the same photos and apps, no score widgets and no
// name in the greeting. Apps stay signed in (this Google TV blocks separate restricted profiles, and
// Google's own profiles need a Google account per person). The choice is kept until switched back.
final class Profiles {
    private Profiles() {}

    private static final String PREFS = "config", KEY = "profile", GUEST = "guest";
    static final String GUEST_NAME = "Guest";
    private static final int CIRCLE = 140;

    interface Pick { void picked(boolean guest); }

    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, 0); }

    static boolean isOwner(Context c) { return !GUEST.equals(prefs(c).getString(KEY, null)); }

    // The owner's name, set with --es name <Name>; "Me" until one is set.
    static String ownerName(Context c) { return prefs(c).getString("name", "Me"); }

    static boolean hasName(Context c) { return prefs(c).contains("name"); }

    static String name(Context c) { return isOwner(c) ? ownerName(c) : GUEST_NAME; }

    static String initial(String name) {
        return name == null || name.isEmpty() ? "" : name.substring(0, 1).toUpperCase(Locale.getDefault());
    }

    // True if this changed the profile.
    static boolean enterGuest(Context c) {
        if (!isOwner(c)) return false;
        prefs(c).edit().putString(KEY, GUEST).apply();
        return true;
    }

    static boolean backToOwner(Context c) {
        if (isOwner(c)) return false;
        prefs(c).edit().remove(KEY).apply();
        return true;
    }

    // "Who's watching?": a dimmed page with a glass card holding a circle for the owner and one for Guest,
    // the current one ringed. Left/Right to move, OK to pick. The caller adds it on top and removes
    // it on Back or after a pick. Focus starts on the profile not in use, the likely choice.
    static View picker(Context c, float scale, Typeface semibold, Typeface bold, Pick pick) {
        FrameLayout dim = new FrameLayout(c);
        dim.setBackgroundColor(0xB3000000);
        dim.setClickable(true);

        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE61C1C1E); // dark glass, so the apps behind don't show through
        bg.setCornerRadius(px(36, scale));
        card.setBackground(bg);
        card.setPadding(px(72, scale), px(48, scale), px(72, scale), px(56, scale));
        // The focused circle grows 15%: nothing around it may cut its edge off.
        card.setClipChildren(false);
        card.setClipToPadding(false);

        TextView title = new TextView(c);
        title.setText(Strings.t("Who's watching?"));
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(40, scale));
        if (bold != null) title.setTypeface(bold);
        card.addView(title, new LinearLayout.LayoutParams(-2, -2));

        LinearLayout people = new LinearLayout(c);
        people.setOrientation(LinearLayout.HORIZONTAL);
        people.setClipChildren(false);
        people.setClipToPadding(false);
        boolean owner = isOwner(c);
        View jakob = person(c, scale, semibold, bold, ownerName(c), owner, false, pick);
        View guest = person(c, scale, semibold, bold, GUEST_NAME, !owner, true, pick);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.topMargin = px(40, scale);
        people.addView(jakob, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-2, -2);
        gp.leftMargin = px(80, scale);
        people.addView(guest, gp);
        card.addView(people, lp);

        dim.addView(card, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        // Focus stays on the two circles: Up, Down and the outer sides lead nowhere, never to the apps
        // behind the dimmed page.
        View a = ((LinearLayout) jakob).getChildAt(0), b = ((LinearLayout) guest).getChildAt(0);
        for (View v : new View[] {a, b}) {
            v.setId(View.generateViewId());
            v.setNextFocusUpId(v.getId());
            v.setNextFocusDownId(v.getId());
        }
        a.setNextFocusLeftId(a.getId());
        a.setNextFocusRightId(b.getId());
        b.setNextFocusLeftId(a.getId());
        b.setNextFocusRightId(b.getId());
        View first = owner ? b : a;
        dim.post(first::requestFocus);
        return dim;
    }

    private static View person(Context c, float scale, Typeface semibold, Typeface bold, String name,
                               boolean current, final boolean isGuest, Pick pick) {
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setClipChildren(false);

        TextView circle = new TextView(c);
        circle.setText(initial(name));
        circle.setGravity(Gravity.CENTER);
        circle.setTextColor(0xFFFFFFFF);
        circle.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(58, scale));
        if (bold != null) circle.setTypeface(bold);
        circle.setIncludeFontPadding(false);
        final GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                isGuest ? new int[] {0xFF8E8E93, 0xFF48484A} : new int[] {0xFFFF9F0A, 0xFFFF375F});
        g.setShape(GradientDrawable.OVAL);
        g.setStroke(px(current ? 6 : 0, scale), 0xE6FFFFFF);
        circle.setBackground(g);
        circle.setFocusable(true);
        circle.setOnClickListener(v -> pick.picked(isGuest));
        circle.setOnFocusChangeListener((v, has) -> {
            float s = has ? 1.15f : 1f;
            Motion.run(150, t -> {
                float now = v.getScaleX() + (s - v.getScaleX()) * t;
                v.setScaleX(now);
                v.setScaleY(now);
            }, null);
        });
        col.addView(circle, new LinearLayout.LayoutParams(px(CIRCLE, scale), px(CIRCLE, scale)));

        TextView label = new TextView(c);
        label.setText(current ? name + " ✓" : name);
        label.setTextColor(0xE6FFFFFF);
        label.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(28, scale));
        if (semibold != null) label.setTypeface(semibold);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.topMargin = px(22, scale);
        col.addView(label, lp);
        return col;
    }

    private static int px(int v, float scale) { return Math.round(v * scale); }
}
