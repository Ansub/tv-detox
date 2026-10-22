package com.ansub.tvhome;

import java.util.Arrays;
import java.util.Locale;

// Checks on the parts of the home screen that don't need a TV: labels, translations, wallpaper
// colours. Run with: sh test.sh. Plain Java, no test framework, so it builds with the same tools
// as the app.
public final class Tests {
    private static int failed;

    private static void check(boolean ok, String what) {
        if (ok) return;
        failed++;
        System.err.println("FAIL: " + what);
    }

    private static void equal(Object got, Object want, String what) {
        check(want.equals(got), what + ": got \"" + got + "\", wanted \"" + want + "\"");
    }

    public static void main(String[] args) {
        labels();
        strings();
        wallpapers();
        if (failed > 0) {
            System.err.println(failed + " check(s) failed.");
            System.exit(1);
        }
        System.out.println("All checks passed.");
    }

    static void labels() {
        // Philips stores "name&DEVICE_TYPE"; everyone else's names are shown as they are.
        equal(Labels.withoutDeviceType("Philips", "Computer&COMPUTER"), "Computer", "Philips device type stripped");
        equal(Labels.withoutDeviceType("philips", "Living room &GAME_CONSOLE"), "Living room", "Philips, any case, trimmed");
        equal(Labels.withoutDeviceType("Philips", "Box&TV"), "Box&TV", "Philips: a two-letter suffix is not a type");
        equal(Labels.withoutDeviceType("Philips", "Hi-fi&audio"), "Hi-fi&audio", "Philips: a lower-case suffix is not a type");
        equal(Labels.withoutDeviceType("Philips", "&COMPUTER"), "&COMPUTER", "Philips: nothing before the & stays");
        equal(Labels.withoutDeviceType("TCL", "Computer&COMPUTER"), "Computer&COMPUTER", "other brands untouched");
        equal(Labels.withoutDeviceType("KONKA", "HDMI 1"), "HDMI 1", "plain name untouched");

        // The app list from install.sh: commas or spaces, no blanks, no repeats, order kept.
        equal(Labels.packageList("com.netflix.ninja,com.google.android.youtube.tv"),
                Arrays.asList("com.netflix.ninja", "com.google.android.youtube.tv"), "comma list");
        equal(Labels.packageList("  a.b , c.d  e.f,,a.b "), Arrays.asList("a.b", "c.d", "e.f"), "spaces, blanks and repeats");
        equal(Labels.packageList(""), Arrays.<String>asList(), "empty list");
        equal(Labels.packageList(null), Arrays.<String>asList(), "no list");
    }

    static void strings() {
        Locale before = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);
            equal(Strings.t("Settings"), "Settings", "English is the source");
            Locale.setDefault(Locale.FRENCH);
            equal(Strings.t("Settings"), "Paramètres", "French");
            equal(Strings.t("Replace %s"), "Remplacer %s", "French keeps the placeholder");
            equal(Strings.t("Not a label"), "Not a label", "unknown label falls back to English");
            Locale.setDefault(new Locale("pt", "BR"));
            equal(Strings.t("Settings"), "Configurações", "language without a country table uses the language");
            Locale.setDefault(Locale.TRADITIONAL_CHINESE);
            equal(Strings.t("Settings"), "設定", "zh-TW is Traditional Chinese");
            Locale.setDefault(new Locale.Builder().setLanguage("zh").setScript("Hant").setRegion("CN").build());
            equal(Strings.t("Settings"), "設定", "zh-Hant wins over the country");
            Locale.setDefault(Locale.SIMPLIFIED_CHINESE);
            equal(Strings.t("Settings"), "设置", "zh-CN is Simplified Chinese");
            Locale.setDefault(new Locale("in"));
            equal(Strings.t("Settings"), "Setelan", "Android's old code for Indonesian");
            Locale.setDefault(new Locale("sw"));
            equal(Strings.t("Settings"), "Settings", "language without a table stays English");
        } finally {
            Locale.setDefault(before);
        }
        // No table is longer than the English one (a shorter one falls back to English), and no label is blank.
        int n = Strings.english().length;
        for (java.util.Map.Entry<String, String[]> e : Strings.tables().entrySet()) {
            check(e.getValue().length <= n, e.getKey() + " has more labels than English");
            for (String label : e.getValue()) check(label != null && !label.trim().isEmpty(), e.getKey() + " has a blank label");
        }
        for (String label : Strings.english()) check(label != null && !label.trim().isEmpty(), "English has a blank label");
    }

    static void wallpapers() {
        equal(Wallpapers.NAMES.length, Wallpapers.COLORS.length, "one colour set per name");
        for (int i = 0; i < Wallpapers.COLORS.length; i++) {
            equal(Wallpapers.COLORS[i].length, 5, Wallpapers.NAMES[i] + " has three stops and two glows");
            for (int j = 0; j < 3; j++) check((Wallpapers.COLORS[i][j] >>> 24) == 0xFF, Wallpapers.NAMES[i] + " gradient stops are opaque");
        }
        equal(Wallpapers.indexOf("Dusk"), 0, "Dusk is first");
        equal(Wallpapers.indexOf("Gold"), Wallpapers.NAMES.length - 1, "Gold is last");
        equal(Wallpapers.indexOf("No such"), 0, "unknown name is Dusk");
        equal(Wallpapers.indexOf(null), 0, "no name is Dusk");
        // A glow at full strength is the glow's own colour; at zero it's the base.
        equal(Integer.toHexString(Wallpapers.glow(0xFF123456, 0xFF000000)), "ff123456", "full glow");
        equal(Integer.toHexString(Wallpapers.glow(0x00123456, 0xFF654321)), "ff654321", "no glow");
        // A third strength, tripled, is full.
        equal(Integer.toHexString(Wallpapers.glow(0x55FFFFFF, 0xFF000000)), "ffffffff", "a third tripled is full");
        // Half of a 0x10 glow over black: 3 * 0x10/255 is about 0.19, so 0.19 of 0xFF is 0x30.
        equal(Integer.toHexString(Wallpapers.glow(0x10FFFFFF, 0xFF000000)), "ff303030", "partial glow");
    }
}
