package com.ansub.tvhome;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;

import java.util.Arrays;

// The wallpapers, saved by name: a gradient top to bottom, then a glow from the top left and one
// from the bottom right. Dark ones first, then bright ones that get darker towards the bottom, so
// the white labels stay easy to read. Only colours, no images, so every one costs the same as Dusk,
// the default.
final class Wallpapers {
    static final String[] NAMES = {"Dusk", "Midnight", "Plum", "Forest", "Graphite", "Ocean", "Ember",
            "Sunset", "Lagoon", "Aurora", "Coral", "Iris", "Meadow", "Gold"};
    // Per wallpaper: top, middle and bottom of the gradient, then the two glows (with their alpha).
    static final int[][] COLORS = {
        {0xFF15100F, 0xFF0A0809, 0xFF050405, 0x36FF8048, 0x2EAA3C8C},
        {0xFF0E1220, 0xFF080A13, 0xFF040509, 0x384A6CFF, 0x2E5A3CC8},
        {0xFF170E19, 0xFF0C070D, 0xFF050306, 0x38C050B4, 0x2E6440D2},
        {0xFF0C150F, 0xFF070C09, 0xFF030604, 0x3050B478, 0x28289682},
        {0xFF151617, 0xFF0B0C0D, 0xFF050506, 0x1CFFFFFF, 0x16A0B4C8},
        {0xFF0A1517, 0xFF060C0E, 0xFF030607, 0x3426B4C8, 0x2E2A64C0},
        {0xFF190D08, 0xFF0D0605, 0xFF060303, 0x40FF5A24, 0x32C8283C},
        {0xFF8C3A5E, 0xFF5E2348, 0xFF22101E, 0xA0FF8A4C, 0x80E0457A},
        {0xFF14588E, 0xFF15366E, 0xFF0A1632, 0x9030C8E0, 0x807A4CE0},
        {0xFF0F4048, 0xFF0F2E3A, 0xFF08141C, 0x9038E0A0, 0x90A050F0},
        {0xFFC0604E, 0xFF8A3456, 0xFF2E1230, 0x90FFB070, 0x70E04090},
        {0xFF5A3CAE, 0xFF3A2A84, 0xFF16123A, 0x90C080FF, 0x804A7CFF},
        {0xFF2E6E4C, 0xFF1C4A40, 0xFF0A201E, 0x90A8E070, 0x6030A8A8},
        {0xFFB07624, 0xFF86401E, 0xFF2A1410, 0x90FFD060, 0x70FF5040},
    };

    private Wallpapers() {}

    // The wallpaper saved under this name, as an index into COLORS (Dusk for an unknown or missing name).
    static int indexOf(String name) {
        return Math.max(0, Arrays.asList(NAMES).indexOf(name));
    }

    // Static gradient, drawn once by the window: no extra view, no animation. scale is the TV's
    // UI width over 1920, the size the glows were designed at.
    static Drawable background(int[] c, float scale) {
        GradientDrawable base = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {c[0], c[1], c[2]});
        GradientDrawable top = radial(c[3], 0.08f, -0.12f, Math.round(1250 * scale));
        GradientDrawable bottom = radial(c[4], 1.0f, 1.1f, Math.round(1000 * scale));
        for (GradientDrawable g : new GradientDrawable[] {base, top, bottom}) g.setDither(true);
        return new LayerDrawable(new Drawable[] {base, top, bottom});
    }

    // A small round swatch of a wallpaper's colours, for the Wallpaper menu.
    static GradientDrawable swatch(int[] c, int size, int strokeWidth) {
        GradientDrawable dot = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[] {glow(c[3], c[0]), c[1], glow(c[4], c[0])});
        dot.setShape(GradientDrawable.OVAL);
        dot.setStroke(strokeWidth, 0x26FFFFFF);
        dot.setSize(size, size);
        return dot;
    }

    // A glow colour made solid over the base, three times as strong, so a small dot shows it.
    // Plain arithmetic on the ARGB ints, so it runs anywhere (the tests run it on a laptop).
    static int glow(int glow, int base) {
        float a = Math.min(1f, 3 * (glow >>> 24) / 255f);
        int r = Math.round(((base >> 16) & 0xFF) + (((glow >> 16) & 0xFF) - ((base >> 16) & 0xFF)) * a);
        int g = Math.round(((base >> 8) & 0xFF) + (((glow >> 8) & 0xFF) - ((base >> 8) & 0xFF)) * a);
        int b = Math.round((base & 0xFF) + ((glow & 0xFF) - (base & 0xFF)) * a);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static GradientDrawable radial(int color, float cx, float cy, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        g.setColors(new int[] {color, color & 0x00FFFFFF});
        g.setGradientCenter(cx, cy);
        g.setGradientRadius(r);
        return g;
    }
}
