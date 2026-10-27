package com.ansub.tvhome;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.List;
import java.util.WeakHashMap;

public class MainActivity extends Activity {

    // Top-row apps, in this order of preference:
    //  1. a list saved at install time:  am start -n com.ansub.tvhome/.MainActivity --es apps "pkg1,pkg2"
    //     (--es apps auto  goes back to 2; add --es stamp <unique> so it's applied only once, see saveConfig)
    //  2. apps.txt (built in as Apps.LIST), topped up with other TV apps installed on this TV.
    // Missing apps are skipped. Topping up stops at six apps, but a chosen list can be any length:
    // apps wrap into rows of six, like Apple TV, and the page scrolls down once they don't all fit.
    // On the TV, hold OK on an app (or press Menu) to move or remove it, or add another; that saves list 1.
    // A network tile always sits at the top right, level with the clock. Settings shortcuts can join
    // it there, off until picked the same way:
    //   --es quick "bluetooth,sound,picture"
    // (--es quick none  takes them away again; "wifi" and "ethernet" are the network tile now).
    private static final String PREFS = "config";
    private static final List<String> NOT_APPS = Arrays.asList("com.android.vending", "com.android.tv.settings",
            "com.android.tv", "com.google.android.tvlauncher", "com.google.android.apps.tv.launcherx",
            "com.google.android.leanbacklauncher");
    private static final String PLAY_STORE = "com.android.vending";
    private static final String SETTINGS = "settings";
    private static final String ADD = "add";
    private static final String[] EDIT_ITEMS = {"Move", "Replace", "Remove from home", "Uninstall"};
    private static final String[] EDIT_ITEMS_SYSTEM = {"Move", "Replace", "Remove from home", "App info"};
    private static final int PER_ROW = 6, MAX_INPUTS = 4;

    // Settings shortcuts: name, label, icon. Tile keys are QUICK + name.
    private static final String QUICK = "quick:";
    private static final String[] QUICK_NAMES = {"bluetooth", "sound", "picture"};
    private static final String[] QUICK_LABELS = {"Bluetooth", "Sound", "Picture"};
    private static final int[][] QUICK_ICONS = {LineIcon.BLUETOOTH, LineIcon.SOUND, LineIcon.PICTURE};
    private static final String NETWORK = "network";
    private static final int OFFLINE = 0, ETHERNET = 4; // network states; 1 to 3 are Wi-Fi bars

    // Saved on the TV: custom input names ("id=name" per line), hidden inputs (one id per line),
    // and the clock override ("12" or "24"; absent follows the TV).
    private static final String PREF_INPUT_NAMES = "inputnames", PREF_HIDDEN_INPUTS = "hiddeninputs";
    private static final String PREF_CLOCK = "clock", PREF_FAKE_INPUTS = "fakeinputs";
    // The throwaway input install.sh adds with FAKE_INPUT=1. It has no HDMI port behind it; it is
    // there so an emulator, which has no inputs of its own, still has a tile to test with.
    private static final String FAKE_INPUT_PKG = "com.ansub.fakeinput";
    // Names an input can be given with the remote alone: no keyboard, no typing.
    private static final String[] INPUT_PRESETS = {"TV", "PlayStation", "Xbox", "Switch", "Laptop", "PC", "Blu-ray", "Soundbar"};
    // TCL keeps its own Picture and Sound panels in a service; Android's Sound page is missing there.
    private static final String TCL_SHOW_WINDOW = "com.tcl.settings.SHOW_WINDOW";

    // Layout is designed at 1920x1080 and scaled to the TV's real UI size by px().
    private static final int MARGIN = 96, GAP = 36, RADIUS = 26;
    // The focus lift: the tile grows, rises, brightens and rounds off a little, then settles.
    // Long enough to read as motion, short enough that a run of d-pad presses never feels behind.
    private static final int FOCUS_MS = 180, RADIUS_UP = 10;
    private static final Interpolator FOCUS_EASE = new DecelerateInterpolator(1.8f);
    private static final int ROW1_TOP = 604, ROW_STEP = 224;
    private static final int GRID_MIN_TOP = 400; // more rows push the grid up, but not into the clock
    private static final int ICON_TOP = 76, ICON_BUTTON = 72, ICON_SIZE = 40, ICON_GAP = 12; // level with the date

    // The Settings tile opens a small menu: the home screen's own settings, then the TV's.
    private static final String[] SETTINGS_ITEMS = {"Wallpaper", "Brightness", "TV settings"};
    // Android TV 11 and later (Google TV too) has no Display page, but its "Display & Sound" page holds Picture.
    private static final String[] TV_DISPLAY_SOUND = {"com.android.tv.settings",
            "com.android.tv.settings.device.displaysound.DisplaySoundActivity"};
    // Wallpapers, saved by name: a gradient top to bottom, then a glow from the top left and one from
    // the bottom right. Dark ones first, then bright ones that get darker towards the bottom, so the
    // white labels stay easy to read. Only colours, no images, so every one costs the same as Dusk,
    // the default.
    private static final String[] WALLPAPER_NAMES = {"Dusk", "Midnight", "Plum", "Forest", "Graphite", "Ocean", "Ember",
            "Sunset", "Lagoon", "Aurora", "Coral", "Iris", "Meadow", "Gold"};
    private static final int[][] WALLPAPERS = {
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
    private float scale = 1f;

    private static Typeface regular, semibold;

    private FrameLayout root; // the page, inside a vertical scroller
    private int tileW, tileH, gridTop; // app grid geometry, set by buildTiles
    private final List<View> tiles = new ArrayList<>();
    private final Map<View, ValueAnimator> lifts = new WeakHashMap<>(); // a running focus fade per tile
    private boolean dirty = true;
    private java.util.Locale shownLocale; // the TV's language (and region) when the page was built
    private String focusedKey;
    private final List<String> rowApps = new ArrayList<>(); // the top row as shown, in order
    private String movingKey;   // the app being moved with left/right, if any
    private TextView moveHint;  // shown above the apps while moving
    private boolean longPressed; // OK was held: swallow the key-up so the app doesn't open
    private boolean backHeld; // Back was held and opened Google TV's home screen: swallow the key-up
    // The tile OK went down on. An app that closes on OK's key-down (Netflix's "Exit") hands the matching
    // key-up to the tile still focused here, so a key-up only opens the app it went down on (#30).
    private String okDownKey;

    // Apps installed, removed or updated. This often lands once Home is already back in front (an app
    // uninstalled from the menu is gone only after Android's prompt has closed), so catch up then.
    private final BroadcastReceiver packagesChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            dirty = true;
            refreshApps();
        }
    };

    // The network tile follows the connection live: Wi-Fi bars, Ethernet or Offline. Only its icon and
    // label change, so nothing is rebuilt.
    private ImageView netIcon;
    private int netState = -1;
    private final ConnectivityManager.NetworkCallback networkChanged = new ConnectivityManager.NetworkCallback() {
        @Override public void onAvailable(Network n) { runOnUiThread(MainActivity.this::refreshNetwork); }
        @Override public void onLost(Network n) { runOnUiThread(MainActivity.this::refreshNetwork); }
        // Also fires when the Wi-Fi signal gets stronger or weaker.
        @Override public void onCapabilitiesChanged(Network n, NetworkCapabilities c) {
            runOnUiThread(MainActivity.this::refreshNetwork);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        saveConfig(getIntent());
        loadFonts();
        scale = getResources().getDisplayMetrics().widthPixels / 1920f;
        showWallpaper(wallpaper());

        ScrollView scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        scroller.setVerticalScrollBarEnabled(false);
        scroller.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroller.setClipChildren(false);
        root = new FrameLayout(this);
        root.setClipChildren(false);
        scroller.addView(root, new FrameLayout.LayoutParams(-1, -2));

        LinearLayout clock = new LinearLayout(this);
        clock.setOrientation(LinearLayout.VERTICAL);
        shownLocale = java.util.Locale.getDefault();
        String day = Strings.datePattern();
        TextClock date = clock(day, day, px(30), 0xA8FFFFFF, regular);
        // The clock follows the TV unless --es clock 12 or 24 says otherwise.
        String clockPref = prefs().getString(PREF_CLOCK, null);
        TextClock time = clock("24".equals(clockPref) ? "H:mm" : "h:mm", "12".equals(clockPref) ? "h:mm" : "H:mm",
                px(176), Color.WHITE, semibold);
        time.setLetterSpacing(-0.028f);
        time.setFontFeatureSettings("tnum");
        time.setIncludeFontPadding(false);
        // Wrap, not the default match_parent: the column must be as wide as the date, not just the time,
        // or "0:00" to "9:59" (one digit less) cuts a long date off.
        clock.addView(date, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, -2);
        tp.topMargin = px(-20); // pull the big digits up under the date
        clock.addView(time, tp);
        root.addView(clock, at(px(MARGIN), px(92), -2, -2));

        moveHint = text(px(24), 0xA8FFFFFF, regular);
        moveHint.setText(Strings.t("Use the arrows to move it, then press OK"));
        moveHint.setVisibility(View.GONE);
        root.addView(moveHint, at(px(MARGIN), px(ROW1_TOP - 64), -2, -2));

        setContentView(scroller);

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addDataScheme("package");
        if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(packagesChanged, f, Context.RECEIVER_EXPORTED);
        else registerReceiver(packagesChanged, f);

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) try {
            if (android.os.Build.VERSION.SDK_INT >= 24) cm.registerDefaultNetworkCallback(networkChanged);
            else cm.registerNetworkCallback(new NetworkRequest.Builder().build(), networkChanged);
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(packagesChanged);
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) try { cm.unregisterNetworkCallback(networkChanged); } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // A clock change needs the whole page rebuilt; a new app list only needs the tiles.
        boolean clock = intent != null && intent.hasExtra("clock");
        if (saveConfig(intent)) {
            if (clock) recreate();
            else buildTiles();
        }
    }

    // Saves an app list passed with --es apps "...", shortcuts passed with --es quick "...", and a
    // clock override passed with --es clock 12|24|auto. Returns true if it changed.
    // When Android kills the home screen in the background, it can start it again with the same old
    // intent; the stamp install.sh sends stops that from undoing changes made on the TV since.
    private boolean saveConfig(Intent intent) {
        String apps = intent == null ? null : intent.getStringExtra("apps");
        String quick = intent == null ? null : intent.getStringExtra("quick");
        String clock = intent == null ? null : intent.getStringExtra("clock");
        String fake = intent == null ? null : intent.getStringExtra("fakeinputs");
        if (apps == null && quick == null && clock == null && fake == null) return false;
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        String stamp = intent.getStringExtra("stamp");
        if (stamp != null && stamp.equals(prefs.getString("stamp", null))) return false;
        SharedPreferences.Editor e = prefs.edit();
        if (stamp != null) e.putString("stamp", stamp);
        if (apps != null) {
            apps = apps.trim();
            if (apps.isEmpty() || apps.equalsIgnoreCase("auto")) e.remove("apps");
            else e.putString("apps", apps);
        }
        if (quick != null) {
            quick = quick.trim();
            if (quick.isEmpty() || quick.equalsIgnoreCase("none")) e.remove("quick");
            else e.putString("quick", quick);
        }
        if (clock != null) {
            clock = clock.trim();
            if (clock.isEmpty() || clock.equalsIgnoreCase("auto")) e.remove(PREF_CLOCK);
            else e.putString(PREF_CLOCK, clock);
        }
        // Set only by install.sh's FAKE_INPUT=1, which also installs the throwaway input itself.
        // It is for an emulator, which has no HDMI ports, and never for a real TV.
        if (fake != null) {
            if (fake.trim().equals("1")) {
                e.putString(PREF_FAKE_INPUTS, "1");
                // An input taken off the home screen stays off, even when FAKE_INPUT=1 adds it back
                // (undo.sh uninstalls it, but running install.sh again does not). Left alone, the
                // emulator would come back with no input tile at all, so put the test input back.
                Set<String> hidden = hiddenInputs();
                for (java.util.Iterator<String> it = hidden.iterator(); it.hasNext(); )
                    if (it.next().startsWith(FAKE_INPUT_PKG + "/")) it.remove();
                e.putString(PREF_HIDDEN_INPUTS, android.text.TextUtils.join("\n", new TreeSet<>(hidden)));
            } else e.remove(PREF_FAKE_INPUTS);
        }
        e.apply();
        dirty = true;
        return true;
    }

    private List<String> appList(PackageManager pm) {
        List<String> out = new ArrayList<>();
        String saved = getSharedPreferences(PREFS, 0).getString("apps", null);
        String[] wanted = saved != null ? saved.split("[,\\s]+") : Apps.LIST;
        for (String p : wanted) if (!p.isEmpty() && !out.contains(p) && launchIntent(pm, p) != null) out.add(p);
        if (saved == null) {
            // Top up with other TV apps people installed (or updated), skipping launchers and system tools.
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                if (out.size() >= PER_ROW) break;
                String p = ri.activityInfo.packageName;
                int flags = ri.activityInfo.applicationInfo.flags;
                boolean stockSystem = (flags & ApplicationInfo.FLAG_SYSTEM) != 0
                        && (flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0;
                if (stockSystem || out.contains(p) || NOT_APPS.contains(p) || p.equals(getPackageName())) continue;
                out.add(p);
            }
        }
        return out;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Rebuild only when apps were installed, removed or updated.
        if (dirty) {
            dirty = false;
            buildTiles();
        }
        refreshNetwork();
        View f = getCurrentFocus();
        if (f == null && !tiles.isEmpty()) tiles.get(0).requestFocus();
        HomeButton.onHomeResumed(this); // back on this home screen: Home comes here again
        // Only counted as opened once it has stayed up a moment, so a crash right after opening still
        // counts as a failed launch.
        getWindow().getDecorView().postDelayed(homeOk, 1500);
    }

    private final Runnable homeOk = () -> HomeButton.onHomeOk(this);

    @Override
    protected void onPause() {
        super.onPause();
        // Left normally (an app or Settings opened), so it didn't crash.
        getWindow().getDecorView().removeCallbacks(homeOk);
        HomeButton.onHomeOk(this);
    }

    // Back from a menu or a window on top (like the uninstall prompt): catch up on apps that changed.
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) refreshApps();
        else okDownKey = null; // an OK still held finishes in the window that took over
    }

    // Rebuilds the tiles if apps changed, but only while Home is in front with no menu open and no
    // app being moved; otherwise the next onResume, focus change or end of the move does it.
    private void refreshApps() {
        if (!dirty || !hasWindowFocus() || movingKey != null) return;
        dirty = false;
        buildTiles();
    }

    // The TV's language changed: start over so the labels and the date follow it.
    @Override
    public void onConfigurationChanged(android.content.res.Configuration c) {
        super.onConfigurationChanged(c);
        if (!java.util.Locale.getDefault().equals(shownLocale)) recreate();
    }

    @Override
    public void onBackPressed() { /* home screen: nothing to go back to */ }

    // Holding Back opens Google TV's own home screen, where it stays on (see HomeButton). Spotted on a
    // key repeat, or when Back is let go if the remote sends none.
    @Override
    public boolean onKeyLongPress(int code, KeyEvent ev) {
        if (code == KeyEvent.KEYCODE_BACK && HomeButton.openGoogleTv(this)) {
            backHeld = true;
            return true;
        }
        return super.onKeyLongPress(code, ev);
    }

    @Override
    public boolean onKeyUp(int code, KeyEvent ev) {
        if (code == KeyEvent.KEYCODE_BACK) {
            boolean held = backHeld;
            backHeld = false;
            if (held) return true;
            if (ev.isTracking() && !ev.isCanceled()
                    && ev.getEventTime() - ev.getDownTime() >= ViewConfiguration.getLongPressTimeout()
                    && HomeButton.openGoogleTv(this)) return true;
        }
        return super.onKeyUp(code, ev);
    }

    // Static gradient, drawn once by the window: no extra view, no animation.
    private Drawable background(int[] c) {
        GradientDrawable base = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {c[0], c[1], c[2]});
        GradientDrawable top = radial(c[3], 0.08f, -0.12f, px(1250));
        GradientDrawable bottom = radial(c[4], 1.0f, 1.1f, px(1000));
        for (GradientDrawable g : new GradientDrawable[] {base, top, bottom}) g.setDither(true);
        return new LayerDrawable(new Drawable[] {base, top, bottom});
    }

    // The saved wallpaper, as an index into WALLPAPERS (Dusk if none is saved).
    private int wallpaper() {
        String saved = getSharedPreferences(PREFS, 0).getString("wallpaper", null);
        return Math.max(0, Arrays.asList(WALLPAPER_NAMES).indexOf(saved));
    }

    private void showWallpaper(int i) {
        getWindow().setBackgroundDrawable(background(WALLPAPERS[i]));
    }

    // The wallpapers, each with a small dot of its colours. Moving through the list shows each one
    // behind the panel; OK keeps it, Back goes back to the one before.
    private void showWallpapers() {
        final int size = px(40);
        panel(Strings.t("Wallpaper"), Arrays.asList(WALLPAPER_NAMES), pos -> {
            int[] c = WALLPAPERS[pos];
            GradientDrawable dot = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[] {glow(c[3], c[0]), c[1], glow(c[4], c[0])});
            dot.setShape(GradientDrawable.OVAL);
            dot.setStroke(Math.max(1, px(2)), 0x26FFFFFF);
            dot.setSize(size, size);
            return dot;
        }, size, wallpaper(), this::showWallpaper, which -> {
            getSharedPreferences(PREFS, 0).edit().putString("wallpaper", WALLPAPER_NAMES[which]).apply();
            showWallpaper(which);
        });
    }

    // A glow colour made solid over the base, three times as strong, so a small dot shows it.
    private static int glow(int glow, int base) {
        float a = Math.min(1f, 3 * (glow >>> 24) / 255f);
        int r = Math.round(Color.red(base) + (Color.red(glow) - Color.red(base)) * a);
        int g = Math.round(Color.green(base) + (Color.green(glow) - Color.green(base)) * a);
        int b = Math.round(Color.blue(base) + (Color.blue(glow) - Color.blue(base)) * a);
        return Color.rgb(r, g, b);
    }

    private static GradientDrawable radial(int color, float cx, float cy, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        g.setColors(new int[] {color, color & 0x00FFFFFF});
        g.setGradientCenter(cx, cy);
        g.setGradientRadius(r);
        return g;
    }

    private void buildTiles() {
        // While tiles are swapped out, removing the focused one would hand focus down the row (and
        // overwrite focusedKey), so hold focus off until the new tiles are in.
        String want = focusedKey;
        int wantAt = rowApps.indexOf(want); // where it was, in case the app is gone now
        root.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        for (View t : tiles) {
            View wrap = (View) t.getParent();
            ((ViewGroup) wrap.getParent()).removeView(wrap);
        }
        tiles.clear();
        rowApps.clear();
        movingKey = null;
        moveHint.setVisibility(View.GONE);

        PackageManager pm = getPackageManager();
        int m = px(MARGIN), g = px(GAP);
        int w = (getResources().getDisplayMetrics().widthPixels - 2 * m - g * (PER_ROW - 1)) / PER_ROW;
        int h = w * 9 / 16;
        tileW = w;
        tileH = h;

        List<String> apps = appList(pm);
        // Apps fill rows of six. With more rows the grid starts higher, so up to two rows of apps and
        // the bottom row still fit on screen; past that the page scrolls.
        int launchable = 0;
        for (String pkg : apps) if (launchIntent(pm, pkg) != null) launchable++;
        int rows = Math.max(1, (launchable + PER_ROW - 1) / PER_ROW);
        gridTop = Math.max(px(GRID_MIN_TOP), px(ROW1_TOP) - (rows - 1) * px(ROW_STEP));
        int bottomTop = gridTop + rows * px(ROW_STEP);
        ((FrameLayout.LayoutParams) moveHint.getLayoutParams()).topMargin = gridTop - px(64);
        root.setMinimumHeight(bottomTop + h + px(MARGIN));

        for (String pkg : apps) {
            Intent li = launchIntent(pm, pkg);
            if (li == null) continue;
            Drawable banner = null;
            try {
                banner = pm.getActivityBanner(li);
                if (banner == null) banner = pm.getApplicationBanner(pkg);
            } catch (Exception ignored) {}
            String label = pkg;
            try { label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString(); } catch (Exception ignored) {}
            // Apps without a TV banner get a plain tile with their normal icon.
            Drawable icon = null;
            if (banner == null) try { icon = pm.getApplicationIcon(pkg); } catch (Exception ignored) {}
            int i = rowApps.size();
            rowApps.add(pkg);
            addTile(root, pkg, label, banner, null, icon, gridX(i), gridY(i), w, h);
        }

        // Bottom row: Play Store, HDMI inputs, Settings and Add app, six at most.
        int col = 0;
        boolean store = !apps.contains(PLAY_STORE) && launchIntent(pm, PLAY_STORE) != null;
        List<TvInputInfo> inputs = hdmiInputs();
        int room = PER_ROW - 2 - (store ? 1 : 0);
        if (inputs.size() > room) inputs = inputs.subList(0, room);
        if (store)
            addTile(root, PLAY_STORE, "Play Store", null, LineIcon.PLAY, null, m + col++ * (w + g), bottomTop, w, h);
        for (TvInputInfo in : inputs)
            addTile(root, in.getId(), inputLabel(in), null, LineIcon.HDMI, null, m + col++ * (w + g), bottomTop, w, h);
        addTile(root, SETTINGS, Strings.t("Settings"), null, LineIcon.SETTINGS, null, m + col++ * (w + g), bottomTop, w, h);
        addTile(root, ADD, Strings.t("Add app"), null, LineIcon.PLUS, null, m + col * (w + g), bottomTop, w, h);

        // Plain icons at the top right, level with the date: any settings shortcuts picked, then the
        // network icon. Added last, so the first app keeps first focus; Up from the apps reaches them.
        List<Integer> quick = quickList();
        int ib = px(ICON_BUTTON), ig = px(ICON_GAP);
        // The last icon's right edge lines up with the apps' (its focus circle sticks out past them).
        int qx = getResources().getDisplayMetrics().widthPixels - m + (ib - px(ICON_SIZE)) / 2
                - (quick.size() + 1) * ib - quick.size() * ig;
        for (int q : quick) {
            addIcon(QUICK + QUICK_NAMES[q], Strings.t(QUICK_LABELS[q]), new LineIcon(QUICK_ICONS[q]), qx);
            qx += ib + ig;
        }
        netIcon = addIcon(NETWORK, Strings.t("Network"), null, qx);
        netState = -1;
        refreshNetwork();

        root.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        // An app that was uninstalled hands focus to the one now in its place, as Remove does.
        if (wantAt >= 0 && !rowApps.contains(want))
            want = rowApps.isEmpty() ? ADD : rowApps.get(Math.min(wantAt, rowApps.size() - 1));
        View restore = null;
        for (View t : tiles) if (t.getTag().equals(want)) restore = t;
        if (restore != null) restore.requestFocus();
    }

    private void addTile(ViewGroup parent, final String key, String label, Drawable banner, int[] lineIcon, Drawable appIcon,
                         int x, int y, int w, int h) {
        FrameLayout card = new FrameLayout(this);
        final GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(px(RADIUS));
        if (banner != null) {
            bg.setColor(0xFF1E1A1C);
            ImageView iv = new ImageView(this);
            iv.setImageDrawable(fit(banner, w, h));
            card.addView(iv, new FrameLayout.LayoutParams(-1, -1));
        } else {
            // Quiet "glass" tile: icon above label.
            bg.setColor(0x13FFFFFF);
            bg.setStroke(Math.max(1, px(2)), 0x17FFFFFF);
            LinearLayout inner = new LinearLayout(this);
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setGravity(Gravity.CENTER);
            ImageView iv = new ImageView(this);
            int size = lineIcon != null ? px(44) : px(64);
            if (lineIcon != null) iv.setImageDrawable(new LineIcon(lineIcon));
            else if (appIcon != null) iv.setImageDrawable(fit(appIcon, size, size));
            inner.addView(iv, new LinearLayout.LayoutParams(size, size));
            TextView t = text(px(24), 0xE0FFFFFF, regular);
            t.setText(label);
            t.setSingleLine(true);
            t.setEllipsize(android.text.TextUtils.TruncateAt.END);
            t.setGravity(Gravity.CENTER);
            t.setPadding(px(16), 0, px(16), 0);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.topMargin = px(12);
            inner.addView(t, lp);
            card.addView(inner, new FrameLayout.LayoutParams(-1, -1));
        }
        card.setBackground(bg);
        card.setClipToOutline(true);
        card.setFocusable(true);
        card.setTag(key);

        // Banner tiles show their name underneath when focused.
        final TextView caption = banner == null ? null : text(px(24), Color.WHITE, regular);
        if (caption != null) {
            caption.setText(label);
            caption.setGravity(Gravity.CENTER_HORIZONTAL);
            caption.setVisibility(View.INVISIBLE);
        }

        final boolean glass = banner == null;
        card.setOnFocusChangeListener((v, has) -> {
            if (has) focusedKey = key;
            lift(v, bg, has, glass ? 0x13FFFFFF : 0xFF1E1A1C, glass ? 0x38FFFFFF : 0xFF1E1A1C,
                    px(RADIUS), px(RADIUS + RADIUS_UP));
            if (caption != null) {
                caption.animate().cancel();
                if (has) {
                    caption.setVisibility(View.VISIBLE);
                    caption.setAlpha(0f);
                    caption.animate().alpha(1f).setDuration(FOCUS_MS).setInterpolator(FOCUS_EASE).start();
                } else {
                    caption.animate().alpha(0f).setDuration(FOCUS_MS / 2).withEndAction(
                            () -> caption.setVisibility(View.INVISIBLE)).start();
                }
            }
        });
        final boolean editable = rowApps.contains(key);
        card.setOnClickListener(v -> open(key));
        if (editable) card.setOnLongClickListener(v -> { showEditMenu(key); return true; });
        // What a long OK (or Menu) on this tile opens: the app row's menu, an input's menu, or, on
        // the + tile, the inputs taken off the home screen.
        final Runnable menu = editable ? () -> showEditMenu(key)
                : isInput(key) ? () -> showInputMenu(key)
                : ADD.equals(key) && !hiddenInputs().isEmpty() ? this::showHiddenInputs : null;
        card.setOnKeyListener((v, code, ev) -> {
            boolean ok = code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER
                    || code == KeyEvent.KEYCODE_NUMPAD_ENTER;
            boolean down = ev.getAction() == KeyEvent.ACTION_DOWN;
            if (key.equals(movingKey)) return moveKey(key, code, ok, down);
            if (menu != null && code == KeyEvent.KEYCODE_MENU) {
                if (!down) menu.run();
                return true;
            }
            if (!ok) return false;
            if (down) {
                if (ev.getRepeatCount() == 0) {
                    longPressed = false;
                    okDownKey = key;
                } else if (menu != null && !longPressed && (ev.isLongPress()
                        || ev.getEventTime() - ev.getDownTime() >= ViewConfiguration.getLongPressTimeout())) {
                    longPressed = true;
                    menu.run();
                }
            } else {
                // Only the key-up of an OK that went down on this tile opens the app, and not a cancelled
                // one (focus left while OK was held). View does the same for a click with isPressed().
                boolean pressed = key.equals(okDownKey) && !ev.isCanceled();
                okDownKey = null;
                if (pressed && !longPressed) open(key);
            }
            return true;
        });

        FrameLayout wrap = new FrameLayout(this);
        wrap.setClipChildren(false);
        wrap.addView(card, new FrameLayout.LayoutParams(w, h));
        if (caption != null) {
            FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(w, -2);
            cp.topMargin = h + px(22);
            wrap.addView(caption, cp);
        }
        parent.addView(wrap, at(x, y, w, -2));
        tiles.add(card);
    }

    // The focus lift: the view grows and rises while its background fades and rounds off. A new
    // change cancels the fade the view was running, so a fast run of d-pad presses only ever shows
    // the tile in front and the one it just left mid-move.
    private void lift(View v, GradientDrawable bg, boolean has, int restColor, int focusColor,
                      int restRadius, int focusRadius) {
        v.animate().cancel();
        v.animate().scaleX(has ? 1.1f : 1f).scaleY(has ? 1.1f : 1f).translationZ(has ? px(26) : 0)
                .setDuration(FOCUS_MS).setInterpolator(FOCUS_EASE).withLayer().start();
        tween(v, bg, has, restColor, focusColor, restRadius, focusRadius);
    }

    // Just the background part: colour and corner radius, with no view animation. Used for the
    // dropdown rows, whose text already changes colour on focus.
    private void tween(View v, GradientDrawable bg, boolean has, int restColor, int focusColor,
                       int restRadius, int focusRadius) {
        if (bg == null) return;
        ValueAnimator old = lifts.remove(v);
        if (old != null) old.cancel();
        final int from = has ? restColor : focusColor, to = has ? focusColor : restColor;
        final int r0 = has ? restRadius : focusRadius, r1 = has ? focusRadius : restRadius;
        if (from == to && r0 == r1) return;
        // Tweened by hand so no objects are made per frame: this runs on TVs with 2 GB of RAM.
        final ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(FOCUS_MS);
        a.setInterpolator(FOCUS_EASE);
        a.addUpdateListener(an -> {
            float t = (float) an.getAnimatedValue();
            if (from != to) bg.setColor(blend(from, to, t));
            if (r0 != r1) bg.setCornerRadius(r0 + (r1 - r0) * t);
        });
        a.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator an) { if (lifts.get(v) == a) lifts.remove(v); }
        });
        lifts.put(v, a);
        a.start();
    }

    private static int blend(int from, int to, float t) {
        return (channel(from, to, 24, t) << 24) | (channel(from, to, 16, t) << 16)
                | (channel(from, to, 8, t) << 8) | channel(from, to, 0, t);
    }

    private static int channel(int from, int to, int shift, float t) {
        int a = (from >>> shift) & 0xFF, b = (to >>> shift) & 0xFF;
        return Math.round(a + (b - a) * t);
    }

    // An icon on its own, no tile or label: a glass circle shows behind it only while it's focused.
    private ImageView addIcon(final String key, String label, Drawable icon, int x) {
        int b = px(ICON_BUTTON), size = px(ICON_SIZE);
        FrameLayout card = new FrameLayout(this);
        final GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.TRANSPARENT);
        card.setBackground(bg);
        ImageView iv = new ImageView(this);
        iv.setImageDrawable(icon);
        card.addView(iv, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
        card.setFocusable(true);
        card.setTag(key);
        card.setContentDescription(label);
        card.setOnFocusChangeListener((v, has) -> {
            if (has) focusedKey = key;
            lift(v, bg, has, Color.TRANSPARENT, 0x38FFFFFF, 0, 0);
        });
        card.setOnClickListener(v -> open(key));
        FrameLayout wrap = new FrameLayout(this);
        wrap.setClipChildren(false);
        wrap.addView(card, new FrameLayout.LayoutParams(b, b));
        root.addView(wrap, at(x, px(ICON_TOP), b, b));
        tiles.add(card);
        return iv;
    }

    private void open(String key) {
        Intent i;
        if (ADD.equals(key)) {
            showAddMenu(-1);
            return;
        } else if (SETTINGS.equals(key)) {
            // A box has no screen of its own to dim: its picture is the TV's, on the TV's remote. On NVIDIA
            // Shield, Brightness also fell back to a Display & Sound page that's listed but missing there.
            List<String> list = new ArrayList<>(Arrays.asList(SETTINGS_ITEMS));
            if (isBox(this)) list.remove("Brightness");
            final String[] items = list.toArray(new String[0]);
            dropdown(tile(SETTINGS), items, which -> {
                if ("Wallpaper".equals(items[which])) showWallpapers();
                else if ("Brightness".equals(items[which])) openBrightness();
                else openTvSettings();
            });
            return;
        } else if (isInput(key)) {
            openInput(key);
            return;
        } else if (NETWORK.equals(key)) {
            openNetworkSettings();
            return;
        } else if (key.startsWith(QUICK)) {
            openQuick(key.substring(QUICK.length()));
            return;
        } else {
            i = launchIntent(getPackageManager(), key);
            if (i == null) return;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(i); } catch (Exception ignored) {}
    }

    // Live TV apps an HDMI tile can open, in order. Droidlogic (Amlogic) TVs ship Live TV under their
    // own package, with TvActivity as its entry point.
    private static final String[][] LIVE_TV = {{"com.android.tv", "com.android.tv.MainActivity"},
            {"com.droidlogic.android.tv", "com.android.tv.TvActivity"}};
    private static final String INPUT_PLAYER = "com.google.android.tv.inputplayer";

    // Opens an HDMI input: the stock Live TV app first, then Droidlogic's, then whatever the TV picks.
    // Each one is tried only when the one before it can't start, so a TV where the stock app already
    // worked takes exactly the same path as before. The one exception: when the TV itself sends HDMI to
    // Google TV's input player, that goes first, because some Google TVs ship com.android.tv as a stub
    // that starts fine but only says "Hello World!" (issue #5).
    private void openInput(String id) {
        Intent view = new Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(id));
        List<Intent> tries = new ArrayList<>();
        ResolveInfo def = getPackageManager().resolveActivity(view, PackageManager.MATCH_DEFAULT_ONLY);
        if (def != null && def.activityInfo != null && INPUT_PLAYER.equals(def.activityInfo.packageName))
            tries.add(new Intent(view).setClassName(INPUT_PLAYER, def.activityInfo.name));
        for (String[] live : LIVE_TV) {
            Intent t = new Intent(view).setClassName(live[0], live[1]);
            if (t.resolveActivity(getPackageManager()) != null) tries.add(t);
        }
        tries.add(view);
        for (Intent t : tries) {
            t.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(t);
                return;
            } catch (Exception ignored) {}
        }
    }

    // The shortcuts picked with --es quick, in that order, as indexes into QUICK_NAMES. A shortcut
    // this TV has no screen for is left out, so it never shows a tile that does nothing.
    private List<Integer> quickList() {
        List<Integer> out = new ArrayList<>();
        String saved = getSharedPreferences(PREFS, 0).getString("quick", null);
        if (saved == null) return out;
        for (String name : saved.split("[,\\s]+")) {
            int q = Arrays.asList(QUICK_NAMES).indexOf(name.toLowerCase(java.util.Locale.ROOT));
            if (q < 0 || out.contains(q) || quickIntents(QUICK_NAMES[q]).isEmpty()) continue;
            out.add(q);
        }
        return out;
    }

    // OFFLINE, 1 to 3 for Wi-Fi with that many bars, or ETHERNET (any other link, which on a TV is a cable).
    private int networkState() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return OFFLINE;
        try {
            int rssi = Integer.MIN_VALUE;
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                NetworkCapabilities c = cm.getNetworkCapabilities(cm.getActiveNetwork());
                if (c == null) return OFFLINE;
                if (!c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return ETHERNET;
                if (android.os.Build.VERSION.SDK_INT >= 29) rssi = c.getSignalStrength();
            } else {
                android.net.NetworkInfo ni = cm.getActiveNetworkInfo();
                if (ni == null || !ni.isConnected()) return OFFLINE;
                if (ni.getType() != ConnectivityManager.TYPE_WIFI) return ETHERNET;
            }
            if (rssi == Integer.MIN_VALUE) { // Android 9 and older: ask the Wi-Fi service
                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wm != null && wm.getConnectionInfo() != null) rssi = wm.getConnectionInfo().getRssi();
            }
            return 1 + WifiManager.calculateSignalLevel(rssi, 3);
        } catch (Exception e) {
            return OFFLINE;
        }
    }

    private void refreshNetwork() {
        if (netIcon == null) return;
        int state = networkState();
        if (state == netState) return;
        netState = state;
        if (state == ETHERNET) {
            netIcon.setImageDrawable(new LineIcon(LineIcon.ETHERNET));
        } else {
            // The whole Wi-Fi fan dim, with the bars it has bright on top. Offline is a slash instead.
            LineIcon fan = new LineIcon(LineIcon.WIFI);
            fan.setAlpha(0x47);
            int[] top = state == OFFLINE ? LineIcon.SLASH : state == 1 ? LineIcon.WIFI_1
                    : state == 2 ? LineIcon.WIFI_2 : LineIcon.WIFI;
            netIcon.setImageDrawable(new LayerDrawable(new Drawable[] {fan, new LineIcon(top)}));
        }
        ((View) netIcon.getParent()).setContentDescription(
                state == OFFLINE ? Strings.t("Offline") : state == ETHERNET ? "Ethernet" : "Wi-Fi");
    }

    // Wi-Fi settings (on Android TV that page lists Ethernet too), else the TV's network page, else Settings.
    private void openNetworkSettings() {
        for (String a : new String[] {Settings.ACTION_WIFI_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS,
                Settings.ACTION_SETTINGS}) {
            try {
                startActivity(new Intent(a).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return;
            } catch (Exception ignored) {}
        }
    }

    // Screens a shortcut can open, best first, keeping only those this TV has.
    private List<Intent> quickIntents(String name) {
        List<Intent> all = new ArrayList<>();
        String tclType = name.equals("sound") ? "SoundPreset" : name.equals("picture") ? "picture" : null;
        // Some TCL Google TVs (Q651G, Android 12) ship the same service inside the TV's own settings app.
        if (tclType != null)
            for (String pkg : new String[] {"com.tcl.settings", "com.android.tv.settings"})
                all.add(new Intent(TCL_SHOW_WINDOW).setClassName(pkg, "com.tcl.settings.ShowWindowService")
                        .putExtra("Type", tclType));
        switch (name) {
            case "bluetooth":
                all.add(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                all.add(new Intent("com.google.android.intent.action.CONNECT_INPUT")); // Android TV's pairing screen
                break;
            case "sound": all.add(new Intent(Settings.ACTION_SOUND_SETTINGS)); break;
            case "picture": all.add(new Intent(Settings.ACTION_DISPLAY_SETTINGS)); break;
        }
        PackageManager pm = getPackageManager();
        List<Intent> out = new ArrayList<>();
        for (Intent i : all) {
            boolean service = TCL_SHOW_WINDOW.equals(i.getAction());
            if (service ? pm.resolveService(i, 0) != null : i.resolveActivity(pm) != null) out.add(i);
        }
        return out;
    }

    // Opens a shortcut's screen: the first one that starts wins. False if none did.
    private boolean openQuick(String name) {
        for (Intent i : quickIntents(name)) {
            try {
                if (TCL_SHOW_WINDOW.equals(i.getAction())) startService(i);
                else startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    // The TV's own picture screen, where brightness lives: the Picture shortcut's screens first (TCL's
    // panel, then Android's Display page), then Android TV's Display & Sound page, else TV settings.
    // Only system screens are started, so it needs no permission and changes nothing by itself.
    private void openBrightness() {
        if (openQuick("picture")) return;
        Intent i = new Intent().setClassName(TV_DISPLAY_SOUND[0], TV_DISPLAY_SOUND[1]);
        if (i.resolveActivity(getPackageManager()) != null) {
            try {
                startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return;
            } catch (Exception ignored) {}
        }
        openTvSettings();
    }

    private void openTvSettings() {
        try { startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch (Exception ignored) {}
    }

    interface Pick { void at(int which); }
    interface Icon { Drawable at(int which) throws Exception; }

    private void panel(String title, List<String> labels, Pick pick) {
        panel(title, labels, null, 0, -1, null, pick);
    }

    // A side panel in the home screen's own look: a title, then one row per choice, with the same
    // glass focus as the tiles. Back closes it. icon (or null) puts an icon of that size by each name.
    // With preview, the list starts on selected and moving through it calls preview; Back calls it
    // with selected again, to put things back.
    private void panel(String title, final List<String> labels, final Icon icon, final int size,
                       final int selected, final Pick preview, final Pick pick) {
        final Dialog d = new Dialog(this, android.R.style.Theme_Material_Dialog_NoActionBar);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(px(48), px(92), px(48), px(48));

        TextView head = text(px(40), Color.WHITE, semibold);
        head.setText(title);
        head.setSingleLine(true);
        head.setEllipsize(android.text.TextUtils.TruncateAt.END);
        head.setPadding(px(24), 0, px(24), 0);
        box.addView(head);

        ListView list = new ListView(this);
        list.setDivider(null);
        list.setVerticalScrollBarEnabled(false);
        list.setOverScrollMode(View.OVER_SCROLL_NEVER);
        GradientDrawable focus = new GradientDrawable();
        focus.setCornerRadius(px(RADIUS));
        focus.setColor(0x38FFFFFF);
        list.setSelector(focus);
        // Icons load as rows appear, drawn small to save RAM.
        final Drawable[] icons = new Drawable[labels.size()];
        list.setAdapter(new BaseAdapter() {
            @Override public int getCount() { return labels.size(); }
            @Override public Object getItem(int pos) { return labels.get(pos); }
            @Override public long getItemId(int pos) { return pos; }
            @Override public View getView(int pos, View convert, ViewGroup parent) {
                TextView t = convert != null ? (TextView) convert : text(px(30), 0xE6FFFFFF, regular);
                t.setText(labels.get(pos));
                t.setSingleLine(true);
                t.setEllipsize(android.text.TextUtils.TruncateAt.END);
                t.setGravity(Gravity.CENTER_VERTICAL);
                t.setMinHeight(px(88));
                t.setPadding(px(24), 0, px(24), 0);
                if (icon != null && icons[pos] == null) {
                    try { icons[pos] = icon.at(pos); }
                    catch (Exception e) { icons[pos] = new ColorDrawable(Color.TRANSPARENT); }
                    icons[pos].setBounds(0, 0, size, size);
                }
                t.setCompoundDrawablesRelative(icons[pos], null, null, null);
                t.setCompoundDrawablePadding(px(24));
                return t;
            }
        });
        list.setOnItemClickListener((parent, v, pos, id) -> {
            d.dismiss();
            pick.at(pos);
        });
        if (preview != null) {
            list.setSelection(selected);
            list.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                    preview.at(pos);
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
            });
            d.setOnCancelListener(x -> preview.at(selected));
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 0, 1f);
        lp.topMargin = px(36);
        box.addView(list, lp);

        d.setContentView(box);
        Window w = d.getWindow();
        w.setBackgroundDrawable(new ColorDrawable(preview != null ? 0xFF161113 : 0xF7161113));
        w.setLayout(px(640), -1);
        w.setGravity(Gravity.END);
        w.setDimAmount(preview != null ? 0f : 0.5f); // a preview needs the home screen undimmed
        d.show();
    }

    // A small menu that drops down under a tile, in the same glass as the tiles. Opens above the
    // tile when there's no room below. Back closes it.
    private void dropdown(View anchor, String[] items, final Pick pick) {
        final Dialog d = new Dialog(this, android.R.style.Theme_Material_Dialog_NoActionBar);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = px(10), rowH = px(64);
        box.setPadding(pad, pad, pad, pad);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(px(RADIUS));
        bg.setColor(0xFF231D1F);
        bg.setStroke(Math.max(1, px(2)), 0x17FFFFFF);
        box.setBackground(bg);
        float widest = 0; // some languages need more room than the tile is wide
        for (int i = 0; i < items.length; i++) {
            final int which = i;
            final TextView t = text(px(26), 0xD9FFFFFF, regular);
            t.setText(Strings.t(items[i]));
            t.setSingleLine(true);
            t.setGravity(Gravity.CENTER_VERTICAL);
            t.setPadding(px(22), 0, px(22), 0);
            widest = Math.max(widest, t.getPaint().measureText(t.getText().toString()) + 2 * px(22) + 2 * pad);
            final GradientDrawable focus = new GradientDrawable();
            focus.setCornerRadius(px(RADIUS - pad));
            focus.setColor(Color.TRANSPARENT);
            t.setBackground(focus);
            t.setFocusable(true);
            t.setOnFocusChangeListener((v, has) -> {
                tween(t, focus, has, Color.TRANSPARENT, 0x38FFFFFF, px(RADIUS - pad), px(RADIUS - pad));
                t.setTextColor(has ? Color.WHITE : 0xD9FFFFFF);
            });
            t.setOnClickListener(v -> {
                d.dismiss();
                pick.at(which);
            });
            box.addView(t, new LinearLayout.LayoutParams(-1, rowH));
        }
        d.setContentView(box);

        // Line it up with the tile as drawn (focused tiles are scaled up), below its name.
        int[] at = new int[2];
        anchor.getLocationOnScreen(at);
        int tileBottom = at[1] + Math.round(anchor.getHeight() * anchor.getScaleY());
        int w = Math.max(Math.max(Math.round(anchor.getWidth() * anchor.getScaleX()), px(320)), (int) Math.ceil(widest));
        int h = items.length * rowH + 2 * pad;
        int y = tileBottom + px(64);
        if (y + h > getResources().getDisplayMetrics().heightPixels - px(24)) y = at[1] - h - px(16);
        Window win = d.getWindow();
        win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        win.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        win.setGravity(Gravity.TOP | Gravity.START);
        android.view.WindowManager.LayoutParams lp = win.getAttributes();
        lp.x = at[0];
        lp.y = y;
        lp.width = w;
        lp.height = h;
        win.setAttributes(lp);
        d.show();
    }

    // Hold OK (or Menu) on a top-row app: move it, swap it for another, or take it off the home
    // screen. Removing only takes the tile away: the app stays installed and can be added back.
    // Apps the user installed can also be uninstalled; Android asks to confirm, and the tile goes
    // once the app is gone. Apps that came with the TV can't be, so they get App info instead,
    // where Android offers to turn them off.
    private void showEditMenu(final String key) {
        final int index = rowApps.indexOf(key);
        if (index < 0) return;
        final boolean user = userInstalled(key);
        dropdown(tile(key), user ? EDIT_ITEMS : EDIT_ITEMS_SYSTEM, which -> {
            if (which == 0) startMove(key);
            else if (which == 1) showAddMenu(index);
            else if (which == 3) {
                Uri pkg = Uri.parse("package:" + key);
                try { startActivity(user ? new Intent(Intent.ACTION_DELETE, pkg)
                        : new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)); }
                catch (Exception ignored) {}
            } else {
                rowApps.remove(index);
                saveRow();
                focusedKey = rowApps.isEmpty() ? ADD : rowApps.get(Math.min(index, rowApps.size() - 1));
                buildTiles();
            }
        });
    }

    // Hold OK on an HDMI tile: name it after the box plugged into it, or take it off the home
    // screen. Hiding keeps the input in the TV's list, so nothing about the TV changes.
    private void showInputMenu(final String id) {
        View anchor = tile(id);
        if (anchor == null) return;
        dropdown(anchor, new String[] {"Rename", "Remove from home"}, which -> {
            if (which == 0) showInputNames(id);
            else hideInput(id);
        });
    }

    // The TV's own name comes first, and picking it resets the custom one; the rest are the usual
    // things people plug in, followed by an option to type a name with the TV's keyboard.
    private void showInputNames(final String id) {
        TvInputInfo in = inputById(id);
        if (in == null) return;
        String own = tvInputLabel(in);
        List<String> names = new ArrayList<>();
        names.add(own);
        for (String p : INPUT_PRESETS) if (!p.equals(own)) names.add(p);
        // Keep canonical preset names in storage so a language change also updates the tile.
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) labels.add(i == 0 ? own : Strings.t(names.get(i)));
        labels.add(Strings.t("Type a name…"));
        panel(Strings.t("Rename"), labels, which -> {
            if (which == names.size()) {
                showInputNameEditor(id, own);
                return;
            }
            setInputName(id, which == 0 ? null : names.get(which));
            focusedKey = id;
            buildTiles();
        });
    }

    private void showInputNameEditor(final String id, String own) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        EditText name = new EditText(builder.getContext());
        name.setSingleLine(true);
        name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        name.setImeOptions(EditorInfo.IME_ACTION_DONE);
        name.setFilters(new InputFilter[] {new InputFilter.LengthFilter(24)});
        String custom = inputNames().get(id);
        name.setText(custom != null ? custom : own);
        final String initialName = name.getText().toString();
        name.setSelectAllOnFocus(true);
        name.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(32));
        name.setTextColor(Color.WHITE);
        FrameLayout box = new FrameLayout(builder.getContext());
        box.setPadding(px(40), px(16), px(40), 0);
        box.addView(name, new FrameLayout.LayoutParams(-1, -2));
        final AlertDialog dialog = builder.setTitle(Strings.t("Rename"))
                .setView(box)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        Runnable save = () -> {
            setInputName(id, name.getText().toString());
            dialog.dismiss();
            focusedKey = id;
            buildTiles();
        };
        name.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_DONE) {
                save.run();
                return true;
            }
            return false;
        });
        Window window = dialog.getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(px(RADIUS));
        bg.setColor(0xFF231D1F);
        bg.setStroke(Math.max(1, px(2)), 0x17FFFFFF);
        window.setBackgroundDrawable(bg);
        dialog.show();
        window.setLayout(px(800), ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> save.run());
        name.requestFocus();
        name.selectAll();
        name.post(() -> {
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .showSoftInput(name, InputMethodManager.SHOW_IMPLICIT);
            // Some TV keyboards move the cursor on startup; reselect after their opening animation.
            name.postDelayed(() -> {
                if (dialog.isShowing() && name.hasFocus()
                        && initialName.equals(name.getText().toString())) name.selectAll();
            }, 500);
        });
    }

    private void hideInput(String id) {
        Set<String> hidden = hiddenInputs();
        hidden.add(id);
        saveHiddenInputs(hidden);
        focusedKey = ADD;
        buildTiles();
    }

    // Long-press on the + tile: the inputs that were taken off the home screen, to put back.
    private void showHiddenInputs() {
        final List<String> ids = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        Set<String> hidden = hiddenInputs();
        for (TvInputInfo in : allInputs()) {
            if (!hidden.contains(in.getId())) continue;
            ids.add(in.getId());
            labels.add(inputLabel(in));
        }
        if (ids.isEmpty()) return;
        panel(Strings.t("Show hidden inputs"), labels, which -> {
            Set<String> now = hiddenInputs();
            now.remove(ids.get(which));
            saveHiddenInputs(now);
            focusedKey = ids.get(which);
            buildTiles();
        });
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, 0);
    }

    // Custom input names, keyed by the input's id. Split only at the first '=', so names can contain '='.
    private Map<String, String> inputNames() {
        Map<String, String> out = new HashMap<>();
        for (String line : prefs().getString(PREF_INPUT_NAMES, "").split("\n")) {
            int eq = line.indexOf('=');
            if (eq > 0) out.put(line.substring(0, eq), line.substring(eq + 1));
        }
        return out;
    }

    // A null or blank name goes back to the TV's own label. Keep names on one storage line.
    private void setInputName(String id, String name) {
        if (name != null) {
            name = name.replace("\r", "").replace("\n", "").trim();
            if (name.isEmpty()) name = null;
        }
        Map<String, String> names = inputNames();
        if (name == null) names.remove(id);
        else names.put(id, name);
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> e : names.entrySet()) {
            if (out.length() > 0) out.append('\n');
            out.append(e.getKey()).append('=').append(e.getValue());
        }
        prefs().edit().putString(PREF_INPUT_NAMES, out.toString()).apply();
    }

    private Set<String> hiddenInputs() {
        Set<String> out = new HashSet<>();
        for (String id : prefs().getString(PREF_HIDDEN_INPUTS, "").split("\n")) if (!id.isEmpty()) out.add(id);
        return out;
    }

    private void saveHiddenInputs(Set<String> ids) {
        prefs().edit().putString(PREF_HIDDEN_INPUTS, android.text.TextUtils.join("\n", new TreeSet<>(ids))).apply();
    }

    // Every launchable app that isn't on the top row yet, by name. The pick is added at the end,
    // or takes the place of the app at replaceIndex (-1 to add).
    private void showAddMenu(final int replaceIndex) {
        final PackageManager pm = getPackageManager();
        TreeMap<String, String> byLabel = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String category : new String[] {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                String p = ri.activityInfo.packageName;
                if (rowApps.contains(p) || byLabel.containsValue(p) || p.equals(getPackageName())
                        || (NOT_APPS.contains(p) && !p.equals(PLAY_STORE))) continue;
                String label = labelOf(p);
                if (!byLabel.containsKey(label)) byLabel.put(label, p);
            }
        }
        if (byLabel.isEmpty()) {
            panel(Strings.t("Every app is already here"), Collections.singletonList("OK"), which -> {});
            return;
        }
        final List<String> pkgs = new ArrayList<>(byLabel.values());
        String title = replaceIndex < 0 ? Strings.t("Add an app")
                : String.format(Strings.t("Replace %s"), labelOf(rowApps.get(replaceIndex)));
        panel(title, new ArrayList<>(byLabel.keySet()), pos -> fit(pm.getApplicationIcon(pkgs.get(pos)), px(48), px(48)),
                px(48), -1, null, which -> {
            if (replaceIndex < 0) rowApps.add(pkgs.get(which));
            else rowApps.set(replaceIndex, pkgs.get(which));
            saveRow();
            focusedKey = pkgs.get(which);
            buildTiles();
        });
    }

    private String labelOf(String pkg) {
        PackageManager pm = getPackageManager();
        try { return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString(); }
        catch (Exception e) { return pkg; }
    }

    // Saves the top row as the app list, so it no longer follows apps.txt.
    private void saveRow() {
        getSharedPreferences(PREFS, 0).edit().putString("apps", android.text.TextUtils.join(",", rowApps)).apply();
    }

    // Where app number i sits in the grid.
    private int gridX(int i) {
        return px(MARGIN) + (i % PER_ROW) * (tileW + px(GAP));
    }

    private int gridY(int i) {
        return gridTop + (i / PER_ROW) * px(ROW_STEP);
    }

    private View tile(String key) {
        for (View t : tiles) if (t.getTag().equals(key)) return t;
        return null;
    }

    // Move mode: the tile gets an outline, the arrows move it through the grid, OK or Back finishes.
    private void startMove(String key) {
        View card = tile(key);
        if (card == null) return;
        movingKey = key;
        GradientDrawable outline = new GradientDrawable();
        outline.setCornerRadius(px(RADIUS));
        outline.setStroke(px(4), Color.WHITE);
        ((FrameLayout) card).setForeground(outline);
        moveHint.setVisibility(View.VISIBLE);
        card.requestFocus();
    }

    private boolean moveKey(String key, int code, boolean ok, boolean down) {
        int step = code == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : code == KeyEvent.KEYCODE_DPAD_RIGHT ? 1
                : code == KeyEvent.KEYCODE_DPAD_UP ? -PER_ROW : code == KeyEvent.KEYCODE_DPAD_DOWN ? PER_ROW : 0;
        if (step != 0) {
            int from = rowApps.indexOf(key), to = from + step;
            if (down && to >= 0 && to < rowApps.size()) {
                // Slide the tiles into their new places: no rebuild, so it stays quick on slow TVs.
                rowApps.add(to, rowApps.remove(from));
                for (int i = Math.min(from, to); i <= Math.max(from, to); i++) {
                    View wrap = (View) tile(rowApps.get(i)).getParent();
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) wrap.getLayoutParams();
                    lp.leftMargin = gridX(i);
                    lp.topMargin = gridY(i);
                    wrap.setLayoutParams(lp);
                }
                // Scroll along with it once it has moved.
                final View card = tile(key);
                card.post(() -> card.requestRectangleOnScreen(
                        new android.graphics.Rect(0, 0, card.getWidth(), card.getHeight())));
            }
            return true;
        }
        if (ok || code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_MENU) {
            if (!down) {
                movingKey = null;
                longPressed = false;
                ((FrameLayout) tile(key)).setForeground(null);
                moveHint.setVisibility(View.GONE);
                saveRow();
                refreshApps();
            }
            return true;
        }
        return true; // up/down and everything else stay put while moving
    }

    private static boolean isInput(String key) {
        return key.indexOf('/') > 0;
    }

    // The TV's own HDMI inputs (not devices behind them), in order, skipping the ones taken off
    // the home screen from the tile menu.
    private List<TvInputInfo> hdmiInputs() {
        List<TvInputInfo> out = new ArrayList<>();
        Set<String> hidden = hiddenInputs();
        for (TvInputInfo in : allInputs()) if (!hidden.contains(in.getId())) out.add(in);
        return out.size() > MAX_INPUTS ? out.subList(0, MAX_INPUTS) : out;
    }

    // HDMI inputs are what a home screen shows. install.sh's FAKE_INPUT=1 takes every TV input
    // instead, so the tiles and their menus can be tested on an emulator that has no HDMI of its own.
    private boolean countsAsInput(TvInputInfo in) {
        boolean fake = "1".equals(prefs().getString(PREF_FAKE_INPUTS, null));
        return (fake || in.getType() == TvInputInfo.TYPE_HDMI) && in.getParentId() == null && !in.isHidden(this);
    }

    // Every HDMI input the TV reports that isn't switched off in its own settings, hidden or not.
    private List<TvInputInfo> allInputs() {
        List<TvInputInfo> out = new ArrayList<>();
        TvInputManager tim = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        if (tim == null) return out;
        for (TvInputInfo in : tim.getTvInputList()) if (countsAsInput(in)) out.add(in);
        Collections.sort(out, (a, b) -> a.getId().compareTo(b.getId()));
        return out;
    }

    // A TV has inputs of its own (HDMI, AV); a box that plugs into one, like NVIDIA Shield, Chromecast
    // or a Mi Box, has none. Some TVs register their HDMI inputs a while after starting (#52), so with
    // none yet it's a box only if HDMI-CEC says it's a playback device and not a TV. When unsure, it's
    // a TV, so TVs keep their menu.
    static boolean isBox(Context c) {
        TvInputManager tim = (TvInputManager) c.getSystemService(Context.TV_INPUT_SERVICE);
        if (tim != null) for (TvInputInfo in : tim.getTvInputList()) if (in.isPassthroughInput()) return false;
        List<String> types = cecTypes("ro.hdmi.device_type"), named = cecTypes("ro.hdmi.cec_device_types");
        if (types.contains("0") || named.contains("tv")) return false;
        return types.contains("4") || named.contains("playback_device");
    }

    private static List<String> cecTypes(String prop) {
        try {
            String v = String.valueOf(Class.forName("android.os.SystemProperties")
                    .getMethod("get", String.class).invoke(null, prop)).trim();
            if (!v.isEmpty()) return Arrays.asList(v.split("\\s*,\\s*"));
        } catch (Throwable ignored) {}
        return Collections.emptyList();
    }

    private TvInputInfo inputById(String id) {
        for (TvInputInfo in : allInputs()) if (in.getId().equals(id)) return in;
        return null;
    }

    // The name picked on the TV, else the one set in Settings > Inputs (like "Bedroom"), else the
    // TV's own name (like "HDMI 1").
    private String inputLabel(TvInputInfo in) {
        String custom = inputNames().get(in.getId());
        return custom != null ? Strings.t(custom) : tvInputLabel(in);
    }

    private String tvInputLabel(TvInputInfo in) {
        CharSequence custom = in.loadCustomLabel(this);
        if (custom != null && custom.length() > 0) return withoutDeviceType(custom.toString());
        CharSequence label = in.loadLabel(this);
        return label != null && label.length() > 0 ? withoutDeviceType(label.toString()) : "HDMI";
    }

    // Philips stores a name as "name&DEVICE_TYPE" (like "Computer&COMPUTER"), so show the name only.
    // Only on Philips TVs: elsewhere a name like "AUDIO&VIDEO" is the name itself.
    private static String withoutDeviceType(String label) {
        if (!"Philips".equalsIgnoreCase(android.os.Build.MANUFACTURER)) return label;
        int amp = label.lastIndexOf('&');
        return amp > 0 && label.substring(amp + 1).matches("[A-Z][A-Z0-9_]{2,}") ? label.substring(0, amp).trim() : label;
    }

    private boolean userInstalled(String pkg) {
        try { return (getPackageManager().getApplicationInfo(pkg, 0).flags & ApplicationInfo.FLAG_SYSTEM) == 0; }
        catch (PackageManager.NameNotFoundException e) { return false; }
    }

    private static Intent launchIntent(PackageManager pm, String pkg) {
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        return i != null ? i : pm.getLaunchIntentForPackage(pkg);
    }

    // Xiaomi TVs ship MiLan Pro but don't register it by name; every other TV gets Roboto.
    private static void loadFonts() {
        if (regular != null) return;
        regular = fromFile("/system/fonts/MILanPro_Normal.ttf", Typeface.create("sans-serif", Typeface.NORMAL));
        semibold = fromFile("/system/fonts/MILanPro_SemiBold.ttf", Typeface.create("sans-serif-medium", Typeface.NORMAL));
    }

    private static Typeface fromFile(String path, Typeface fallback) {
        try { return new File(path).exists() ? Typeface.createFromFile(path) : fallback; }
        catch (Exception e) { return fallback; }
    }

    // Draws a banner once into a tile-sized bitmap (center-cropped). Some apps ship huge banners
    // that Android 9 refuses to draw ("Canvas: trying to draw too large bitmap"); this also saves RAM.
    private Drawable fit(Drawable d, int w, int h) {
        try {
            Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            int iw = d.getIntrinsicWidth(), ih = d.getIntrinsicHeight();
            if (iw <= 0 || ih <= 0) { iw = w; ih = h; }
            float s = Math.max((float) w / iw, (float) h / ih);
            int dw = Math.round(iw * s), dh = Math.round(ih * s);
            d.setBounds((w - dw) / 2, (h - dh) / 2, (w - dw) / 2 + dw, (h - dh) / 2 + dh);
            d.draw(new Canvas(b));
            return new BitmapDrawable(getResources(), b);
        } catch (Throwable t) {
            return d;
        }
    }

    private int px(int v) {
        return Math.round(v * scale);
    }

    private TextClock clock(String f12, String f24, int px, int color, Typeface tf) {
        TextClock c = new TextClock(this);
        c.setFormat12Hour(f12);
        c.setFormat24Hour(f24);
        c.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
        c.setTextColor(color);
        c.setTypeface(tf);
        return c;
    }

    private TextView text(int px, int color, Typeface tf) {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
        t.setTextColor(color);
        if (tf != null) t.setTypeface(tf);
        return t;
    }

    private static FrameLayout.LayoutParams at(int x, int y, int w, int h) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h);
        lp.leftMargin = x;
        lp.topMargin = y;
        return lp;
    }
}
