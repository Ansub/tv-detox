package com.ansub.tvhome;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.os.StatFs;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.FocusFinder;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// The Settings tile's screen, in the home screen's own look: categories on the left, the chosen one
// on the right. The screensaver and sleep timers are changed here directly, once install.sh has
// granted the two permissions that takes; without them the page only shows the current values.
// Wi-Fi, pairing, resolution and audio can only be changed by the TV's own settings, so those pages
// show what's set now and open the TV's screen. Nothing runs in the background, and nothing is kept
// in memory once you leave.
public class SettingsActivity extends Activity {

    private static final String[] CATEGORIES = {"Screensaver & sleep", "Network & Bluetooth", "Display & sound",
            "Device info & storage", "Wallpaper", "All Android settings"};
    private static final int[][] CATEGORY_ICONS = {LineIcon.POWER, LineIcon.WIFI, LineIcon.PICTURE,
            LineIcon.INFO, LineIcon.FRAME, LineIcon.SETTINGS};
    private static final int SAVER = 0, NETWORK = 1, DISPLAY = 2, DEVICE = 3, WALLPAPER = 4;

    private static final int RADIUS = 26, FOCUS = 0x38FFFFFF, SELECTED = 0x1AFFFFFF;
    private static final int[] START_MINUTES = {1, 2, 5, 10, 15, 30};
    private static final int[] SLEEP_MINUTES = {5, 10, 15, 30, 60};
    private static final int NEVER = -1;

    // Android TV's own screens for what an app can't change.
    private static final String TV_SETTINGS = "com.android.tv.settings";
    private static final String NETWORK_PAGE = TV_SETTINGS + ".connectivity.NetworkActivity";
    private static final String DISPLAY_SOUND = TV_SETTINGS + ".device.displaysound.DisplaySoundActivity";

    private float scale = 1f;
    private LinearLayout menu, content;
    private ScrollView scroller;
    private final List<TextView> categoryRows = new ArrayList<>();
    private int current = -1;
    private boolean building;
    private boolean box; // NVIDIA Shield, Chromecast and the like: no screen or speakers of their own
    private String note; // shown once at the top of the screensaver page after a change, then cleared

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        MainActivity.loadFonts();
        scale = getResources().getDisplayMetrics().widthPixels / 1920f;
        box = MainActivity.isBox(this);
        getWindow().setBackgroundDrawable(MainActivity.background(
                MainActivity.WALLPAPERS[MainActivity.wallpaperIndex(this)], scale));

        FrameLayout root = new FrameLayout(this);

        TextView title = text(px(56), Color.WHITE, MainActivity.semibold);
        title.setText(Strings.t("Settings"));
        root.addView(title, at(px(96), px(70), -2, -2));

        TextClock clock = new TextClock(this);
        clock.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(40));
        clock.setTextColor(0xA8FFFFFF);
        clock.setTypeface(MainActivity.regular);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        cp.topMargin = px(84);
        cp.rightMargin = px(96);
        root.addView(clock, cp);

        menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < CATEGORIES.length; i++) menu.addView(categoryRow(i), new LinearLayout.LayoutParams(-1, -2));
        root.addView(menu, at(px(72), px(196), px(520), -2));

        GradientDrawable card = new GradientDrawable();
        card.setCornerRadius(px(RADIUS));
        card.setColor(0x14FFFFFF);
        scroller = new ScrollView(this);
        scroller.setBackground(card);
        scroller.setVerticalScrollBarEnabled(false);
        scroller.setOverScrollMode(View.OVER_SCROLL_NEVER);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(px(24), px(24), px(24), px(24));
        scroller.addView(content, new FrameLayout.LayoutParams(-1, -2));
        root.addView(scroller, at(px(640), px(196), px(1184), px(824)));

        setContentView(root);
        show(SAVER);
        categoryRows.get(SAVER).requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Back from one of the TV's screens: show what it changed.
        if (current >= 0) {
            boolean inContent = inside(content, getCurrentFocus());
            show(current);
            if (inContent) focusFirst();
        }
    }

    // --- Moving around with the remote ----------------------------------------------------------
    // Focus stays in the column it's in: left and right jump between the two, and up or down never
    // wander across, so moving through a page never switches the page under you.
    @Override
    public boolean dispatchKeyEvent(KeyEvent ev) {
        if (ev.getAction() != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(ev);
        View f = getCurrentFocus();
        boolean inMenu = inside(menu, f), inContent = inside(content, f);
        switch (ev.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (inMenu) { focusFirst(); return true; }
                if (inContent) return true;
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (inContent) { categoryRows.get(current).requestFocus(); return true; }
                if (inMenu) return true;
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (inMenu || inContent) {
                    int dir = ev.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP ? View.FOCUS_UP : View.FOCUS_DOWN;
                    View next = FocusFinder.getInstance().findNextFocus(inMenu ? menu : content, f, dir);
                    if (next != null) next.requestFocus();
                    else if (inContent && dir == View.FOCUS_UP) scroller.smoothScrollTo(0, 0);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_BACK:
                if (inContent) { categoryRows.get(current).requestFocus(); return true; }
                break;
        }
        return super.dispatchKeyEvent(ev);
    }

    private static boolean inside(ViewGroup box, View v) {
        for (View p = v; p != null; p = p.getParent() instanceof View ? (View) p.getParent() : null)
            if (p == box) return true;
        return false;
    }

    private void focusFirst() {
        View first = FocusFinder.getInstance().findNextFocus(content, null, View.FOCUS_DOWN);
        if (first != null) first.requestFocus();
    }

    private TextView categoryRow(final int i) {
        final TextView t = text(px(30), 0xD9FFFFFF, MainActivity.regular);
        t.setText(Strings.t(CATEGORIES[i]));
        // Some languages need two lines here ("Todas as configurações do Android").
        t.setMaxLines(2);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setMinHeight(px(88));
        t.setPadding(px(24), px(14), px(24), px(14));
        LineIcon icon = new LineIcon(CATEGORY_ICONS[i]);
        icon.setBounds(0, 0, px(36), px(36));
        t.setCompoundDrawablesRelative(icon, null, null, null);
        t.setCompoundDrawablePadding(px(22));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(px(RADIUS));
        bg.setColor(Color.TRANSPARENT);
        t.setBackground(bg);
        t.setFocusable(true);
        t.setOnFocusChangeListener((v, has) -> {
            // Rebuilding a page drops the focused row and Android parks focus here for a moment:
            // only a different category means a different page.
            if (has && i != current) show(i);
            paintCategories();
        });
        t.setOnClickListener(v -> {
            if (i == WALLPAPER) openWallpaper();
            else if (i == CATEGORIES.length - 1) open(new Intent(Settings.ACTION_SETTINGS));
            else focusFirst();
        });
        categoryRows.add(t);
        return t;
    }

    // Focused: bright glass. The category whose page is showing while you're in it: a softer one.
    private void paintCategories() {
        for (int i = 0; i < categoryRows.size(); i++) {
            TextView t = categoryRows.get(i);
            boolean focused = t.hasFocus(), shown = i == current;
            ((GradientDrawable) t.getBackground()).setColor(focused ? FOCUS : shown ? SELECTED : Color.TRANSPARENT);
            t.setTextColor(focused || shown ? Color.WHITE : 0xB3FFFFFF);
        }
    }

    // --- The pages ------------------------------------------------------------------------------

    private void show(int which) {
        if (building) return;
        building = true;
        current = which;
        // Hand focus to the category first, so clearing the page doesn't send it somewhere else.
        if (inside(content, getCurrentFocus())) categoryRows.get(which).requestFocus();
        content.removeAllViews();
        scroller.scrollTo(0, 0);
        switch (which) {
            case SAVER: saverPage(); break;
            case NETWORK: networkPage(); break;
            case DISPLAY: displayPage(); break;
            case DEVICE: devicePage(); break;
            case WALLPAPER:
                text(Strings.t("The colour behind your home screen. Moving through the list shows each one; OK keeps it."));
                action(Strings.t("Choose wallpaper"), this::openWallpaper);
                break;
            default:
                text(Strings.t("Everything else: accounts, apps, accessibility, date and time, language, updates and more."));
                action(Strings.t("Open Android settings"), () -> open(new Intent(Settings.ACTION_SETTINGS)));
        }
        building = false;
        paintCategories();
    }

    private void saverPage() {
        final ContentResolver cr = getContentResolver();
        final boolean canWrite = canWriteSettings();
        if (note != null) { text(note); note = null; }
        if (!canWrite) text(Strings.t("To change these here, run install.sh again from your computer: it gives this "
                + "home screen the permission once. Until then this page only shows them."));

        // The screensavers on this TV, plus Off.
        final List<ComponentName> dreams = new ArrayList<>();
        final List<String> names = new ArrayList<>();
        PackageManager pm = getPackageManager();
        for (ResolveInfo ri : pm.queryIntentServices(new Intent("android.service.dreams.DreamService"), 0)) {
            dreams.add(new ComponentName(ri.serviceInfo.packageName, ri.serviceInfo.name));
            names.add(ri.loadLabel(pm).toString());
        }
        names.add(Strings.t("Off"));
        String saved = Settings.Secure.getString(cr, "screensaver_components");
        ComponentName now = saved == null ? null : ComponentName.unflattenFromString(saved.split(",")[0]);
        boolean on = Settings.Secure.getInt(cr, "screensaver_enabled", 1) == 1;
        int at = on ? dreams.indexOf(now) : -1;
        final int saverIndex = at < 0 ? names.size() - 1 : at;

        heading(Strings.t("Screensaver"));
        choice(Strings.t("Screensaver"), names.get(saverIndex), canWrite,
                () -> choose(Strings.t("Screensaver"), names, saverIndex, pick -> {
            boolean off = pick == names.size() - 1;
            Settings.Secure.putInt(cr, "screensaver_enabled", off ? 0 : 1);
            if (!off) Settings.Secure.putString(cr, "screensaver_components", dreams.get(pick).flattenToString());
            refresh();
        }));

        final int start = Settings.System.getInt(cr, Settings.System.SCREEN_OFF_TIMEOUT, 300000);
        final int sleep = Settings.Secure.getInt(cr, "sleep_timeout", NEVER);
        choice(Strings.t("Starts after"), minutes(start), canWrite, () -> {
            List<String> labels = new ArrayList<>();
            for (int m : START_MINUTES) labels.add(minutes(m * 60000));
            choose(Strings.t("Start the screensaver after"), labels, indexOf(START_MINUTES, start / 60000), pick -> {
                int ms = START_MINUTES[pick] * 60000;
                Settings.System.putInt(cr, Settings.System.SCREEN_OFF_TIMEOUT, ms);
                // Sleep runs on its own clock; sleeping first would skip the screensaver.
                if (sleep > 0 && sleep <= ms) {
                    int moved = NEVER;
                    for (int m : SLEEP_MINUTES) if (m * 60000 > ms) { moved = m * 60000; break; }
                    Settings.Secure.putInt(cr, "sleep_timeout", moved);
                    note = String.format(Strings.t("Sleep moved to %s, so the screensaver has time to show."),
                            moved == NEVER ? Strings.t("Never") : minutes(moved));
                }
                refresh();
            });
        });
        action(Strings.t("Preview screensaver"), this::previewScreensaver);

        heading(Strings.t("Sleep"));
        choice(Strings.t("Sleeps after"), sleep <= 0 ? Strings.t("Never") : minutes(sleep), canWrite, () -> {
            // Only times after the screensaver starts, so it always gets its turn.
            final List<Integer> values = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            for (int m : SLEEP_MINUTES) if (m * 60000 > start) { values.add(m * 60000); labels.add(minutes(m * 60000)); }
            values.add(NEVER);
            labels.add(Strings.t("Never"));
            choose(Strings.t("Put it to sleep after"), labels, Math.max(0, values.indexOf(sleep <= 0 ? NEVER : sleep)),
                    pick -> {
                Settings.Secure.putInt(cr, "sleep_timeout", values.get(pick));
                refresh();
            });
        });
        text(Strings.t("Times count from the last button you pressed. The screensaver shows first, then it sleeps."));
    }

    private void networkPage() {
        heading(Strings.t("Connection"));
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        String with = null;
        LinkProperties lp = null;
        if (cm != null && Build.VERSION.SDK_INT >= 23) {
            Network n = cm.getActiveNetwork();
            NetworkCapabilities caps = n == null ? null : cm.getNetworkCapabilities(n);
            lp = n == null ? null : cm.getLinkProperties(n);
            if (caps != null) with = caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ? "Ethernet"
                    : caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ? "Wi-Fi" : "Other";
        } else if (cm != null) {
            android.net.NetworkInfo ni = cm.getActiveNetworkInfo();
            if (ni != null && ni.isConnected()) with = ni.getType() == ConnectivityManager.TYPE_ETHERNET ? "Ethernet"
                    : ni.getType() == ConnectivityManager.TYPE_WIFI ? "Wi-Fi" : "Other";
        }
        info(Strings.t("Connected with"), with == null ? Strings.t("Offline") : Strings.t(with));
        if ("Wi-Fi".equals(with)) {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wi = wm == null ? null : wm.getConnectionInfo();
            if (wi != null) {
                String[] words = {"Very weak", "Weak", "Fair", "Good", "Excellent"};
                info(Strings.t("Signal"), Strings.t(words[Math.max(0, Math.min(4, WifiManager.calculateSignalLevel(wi.getRssi(), 5)))]));
                int mhz = wi.getFrequency();
                if (mhz > 0) info(Strings.t("Band"), mhz >= 5900 ? "6 GHz" : mhz >= 4900 ? "5 GHz" : "2.4 GHz");
                if (wi.getLinkSpeed() > 0) info(Strings.t("Link speed"), wi.getLinkSpeed() + " Mbps");
            }
        }
        if (lp != null) for (LinkAddress a : lp.getLinkAddresses())
            if (a.getAddress() instanceof Inet4Address) { info(Strings.t("IP address"), a.getAddress().getHostAddress()); break; }
        action(Strings.t("Wi-Fi & network settings"), () -> {
            if (!open(new Intent().setClassName(TV_SETTINGS, NETWORK_PAGE))) open(new Intent(Settings.ACTION_WIFI_SETTINGS));
        });

        heading(Strings.t("Bluetooth"));
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        // Android 12+ asks for a permission to read paired devices; install.sh grants it.
        boolean allowed = Build.VERSION.SDK_INT < 31 || granted("android.permission.BLUETOOTH_CONNECT");
        if (bt == null) {
            info(Strings.t("Bluetooth"), Strings.t("Not available"));
        } else if (!allowed) {
            info(Strings.t("Bluetooth"), bt.isEnabled() ? Strings.t("On") : Strings.t("Off"));
        } else try {
            info(Strings.t("Bluetooth"), bt.isEnabled() ? Strings.t("On") : Strings.t("Off"));
            List<BluetoothDevice> paired = new ArrayList<>(bt.getBondedDevices());
            if (paired.isEmpty()) info(Strings.t("Paired devices"), Strings.t("None"));
            for (BluetoothDevice d : paired) info(d.getName() == null ? d.getAddress() : d.getName(),
                    connected(d) ? Strings.t("Connected") : Strings.t("Not connected"));
        } catch (SecurityException e) {
            // Leave the list out rather than show something wrong.
        }
        action(Strings.t("Pair a remote, controller or headphones"), () -> {
            if (!open(new Intent("com.google.android.intent.action.CONNECT_INPUT")))
                open(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
        });
    }

    private void displayPage() {
        heading(Strings.t("Picture"));
        Display d = getWindowManager().getDefaultDisplay();
        int w, h;
        float hz;
        if (Build.VERSION.SDK_INT >= 23) {
            Display.Mode m = d.getMode();
            w = m.getPhysicalWidth();
            h = m.getPhysicalHeight();
            hz = m.getRefreshRate();
        } else {
            DisplayMetrics dm = new DisplayMetrics();
            d.getRealMetrics(dm);
            w = dm.widthPixels;
            h = dm.heightPixels;
            hz = d.getRefreshRate();
        }
        String name = h >= 2160 ? "4K" : h >= 1440 ? "1440p" : h >= 1080 ? "1080p" : h >= 720 ? "720p" : h + "p";
        info(Strings.t("Resolution"), name + " (" + w + " × " + h + ")");
        info(Strings.t("Refresh rate"), Math.round(hz * 100) / 100f + " Hz");
        if (Build.VERSION.SDK_INT >= 24) {
            List<String> hdr = new ArrayList<>();
            Display.HdrCapabilities caps = d.getHdrCapabilities();
            if (caps != null) for (int t : caps.getSupportedHdrTypes()) {
                switch (t) {
                    case 1: hdr.add("Dolby Vision"); break;
                    case 2: hdr.add("HDR10"); break;
                    case 3: hdr.add("HLG"); break;
                    case 4: hdr.add("HDR10+"); break;
                }
            }
            info(Strings.t("HDR"), hdr.isEmpty() ? Strings.t("None") : TextUtils.join(", ", hdr));
        }

        if (Build.VERSION.SDK_INT >= 23) {
            heading(Strings.t("Sound"));
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            // Media goes to Bluetooth headphones while they're connected, otherwise to HDMI or the speakers.
            String out = box ? "HDMI" : Strings.t("TV speakers"), bluetooth = null;
            List<String> formats = new ArrayList<>();
            if (am != null) for (AudioDeviceInfo dev : am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
                int type = dev.getType();
                if (type == AudioDeviceInfo.TYPE_HDMI || type == AudioDeviceInfo.TYPE_HDMI_ARC
                        || (Build.VERSION.SDK_INT >= 30 && type == AudioDeviceInfo.TYPE_HDMI_EARC)) {
                    out = type == AudioDeviceInfo.TYPE_HDMI ? "HDMI" : "HDMI ARC";
                    for (int e : dev.getEncodings()) {
                        String f = encoding(e);
                        if (f != null && !formats.contains(f)) formats.add(f);
                    }
                } else if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
                    bluetooth = "Bluetooth: " + dev.getProductName();
                }
            }
            info(Strings.t("Audio goes to"), bluetooth != null ? bluetooth : out);
            if (!formats.isEmpty()) info(Strings.t("Surround formats"), TextUtils.join(", ", formats));
        }

        // A box has no screen of its own to dim: its brightness is the TV's, on the TV's remote.
        if (!box) action(Strings.t("Picture & brightness"), () -> showOnHome("brightness"));
        action(Strings.t("Resolution, HDR & sound"), () -> {
            // The Shield lists Display & Sound but ships without it; its own is inside TV settings.
            if (MainActivity.SHIELD || !open(new Intent().setClassName(TV_SETTINGS, DISPLAY_SOUND)))
                open(new Intent(Settings.ACTION_SETTINGS));
        });
        if (MainActivity.SHIELD) text(Strings.t("On NVIDIA Shield these are under Device Preferences, then Display & Sound."));
    }

    private void devicePage() {
        heading(Strings.t("This device"));
        String maker = Build.MANUFACTURER;
        String model = Build.MODEL.toLowerCase(Locale.ROOT).startsWith(maker.toLowerCase(Locale.ROOT))
                ? Build.MODEL : maker + " " + Build.MODEL;
        info(Strings.t("Model"), model);
        info("Android", Build.VERSION.RELEASE);
        info(Strings.t("Software"), Build.DISPLAY);
        long up = SystemClock.elapsedRealtime() / 60000;
        info(Strings.t("On for"), up >= 1440 ? (up / 1440) + " d " + (up % 1440 / 60) + " h"
                : (up / 60) + " h " + (up % 60) + " min");

        heading(Strings.t("Memory & storage"));
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        info(Strings.t("Memory"), String.format(Strings.t("%s free of %s"), gb(mi.availMem), gb(mi.totalMem)));
        StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
        info(Strings.t("Storage"), String.format(Strings.t("%s free of %s"), gb(fs.getAvailableBytes()), gb(fs.getTotalBytes())));
        try {
            info(Strings.t("Home screen version"), getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (Exception ignored) {}
    }

    // --- Rows -----------------------------------------------------------------------------------

    private void heading(String s) {
        TextView t = text(px(24), 0x99FFFFFF, MainActivity.semibold);
        t.setText(s.toUpperCase(Locale.getDefault()));
        t.setLetterSpacing(0.06f);
        t.setPadding(px(24), content.getChildCount() == 0 ? px(8) : px(36), px(24), px(12));
        content.addView(t);
    }

    private void text(String s) {
        TextView t = text(px(24), 0x8CFFFFFF, MainActivity.regular);
        t.setText(s);
        t.setLineSpacing(0, 1.2f);
        t.setPadding(px(24), px(14), px(24), px(14));
        content.addView(t);
    }

    // A line of information. Focusable but quiet, so the remote can scroll through a long page.
    private void info(String label, String value) {
        row(label, value, false, null);
    }

    private void choice(String label, String value, boolean enabled, Runnable onClick) {
        row(label, value + "  ›", true, enabled ? onClick : () -> Toast.makeText(this,
                Strings.t("Run install.sh again from your computer to change this here"), Toast.LENGTH_LONG).show());
    }

    private void action(String label, Runnable onClick) {
        row(label, "›", true, onClick);
    }

    private void row(String label, String value, final boolean button, Runnable onClick) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(px(24), 0, px(24), 0);
        r.setMinimumHeight(px(button ? 84 : 68));
        final int labelColor = button ? 0xEBFFFFFF : 0xD9FFFFFF, valueColor = button ? 0xCCFFFFFF : 0x99FFFFFF;
        final TextView l = text(px(28), labelColor, MainActivity.regular);
        l.setText(label);
        l.setSingleLine(true);
        l.setEllipsize(TextUtils.TruncateAt.END);
        r.addView(l, new LinearLayout.LayoutParams(0, -2, 1f));
        final TextView v = text(px(28), valueColor, MainActivity.regular);
        v.setText(value);
        v.setSingleLine(true);
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(-2, -2);
        vp.leftMargin = px(24);
        r.addView(v, vp);
        final int rest = button ? 0x0DFFFFFF : Color.TRANSPARENT, focus = button ? FOCUS : 0x14FFFFFF;
        final GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(px(RADIUS));
        bg.setColor(rest);
        r.setBackground(bg);
        r.setFocusable(true);
        r.setTag(label); // so a rebuilt page can put focus back on the same row
        r.setOnFocusChangeListener((x, has) -> {
            bg.setColor(has ? focus : rest);
            l.setTextColor(has ? Color.WHITE : labelColor);
            v.setTextColor(has ? Color.WHITE : valueColor);
        });
        if (onClick != null) r.setOnClickListener(x -> onClick.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = button ? px(8) : 0;
        content.addView(r, lp);
    }

    // A side list like the home screen's own panels, with a tick by the current choice. Back closes it.
    private void choose(String title, final List<String> labels, final int selected, final MainActivity.Pick pick) {
        final Dialog d = new Dialog(this, android.R.style.Theme_Material_Dialog_NoActionBar);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(px(48), px(92), px(48), px(48));
        TextView head = text(px(40), Color.WHITE, MainActivity.semibold);
        head.setText(title);
        head.setPadding(px(24), 0, px(24), 0);
        box.addView(head);
        ListView list = new ListView(this);
        list.setDivider(null);
        list.setVerticalScrollBarEnabled(false);
        GradientDrawable focus = new GradientDrawable();
        focus.setCornerRadius(px(RADIUS));
        focus.setColor(FOCUS);
        list.setSelector(focus);
        list.setAdapter(new BaseAdapter() {
            @Override public int getCount() { return labels.size(); }
            @Override public Object getItem(int p) { return labels.get(p); }
            @Override public long getItemId(int p) { return p; }
            @Override public View getView(int p, View convert, ViewGroup parent) {
                TextView t = convert != null ? (TextView) convert : text(px(30), 0xE6FFFFFF, MainActivity.regular);
                t.setText((p == selected ? "✓  " : "    ") + labels.get(p));
                t.setSingleLine(true);
                t.setGravity(Gravity.CENTER_VERTICAL);
                t.setMinHeight(px(88));
                t.setPadding(px(24), 0, px(24), 0);
                return t;
            }
        });
        list.setSelection(Math.max(0, selected));
        list.setOnItemClickListener((parent, v, pos, id) -> {
            d.dismiss();
            try {
                pick.at(pos);
            } catch (SecurityException e) {
                Toast.makeText(this, Strings.t("Run install.sh again from your computer to change this here"),
                        Toast.LENGTH_LONG).show();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 0, 1f);
        lp.topMargin = px(36);
        box.addView(list, lp);
        d.setContentView(box);
        Window win = d.getWindow();
        win.setBackgroundDrawable(new ColorDrawable(0xF7161113));
        win.setLayout(px(640), -1);
        win.setGravity(Gravity.END);
        win.setDimAmount(0.5f);
        d.show();
    }

    // --- Helpers --------------------------------------------------------------------------------

    // Rebuilds the page after a change and puts focus back on the same row.
    private void refresh() {
        View f = getCurrentFocus();
        Object tag = f == null ? null : f.getTag();
        show(current);
        View again = tag == null ? null : content.findViewWithTag(tag);
        if (again != null) again.requestFocus();
        else focusFirst();
    }

    // WRITE_SETTINGS is the "modify system settings" switch (an app op since Android 6), and
    // WRITE_SECURE_SETTINGS can only be granted over adb. install.sh grants both.
    private boolean canWriteSettings() {
        return (Build.VERSION.SDK_INT < 23 || Settings.System.canWrite(this))
                && granted("android.permission.WRITE_SECURE_SETTINGS");
    }

    private boolean granted(String permission) {
        return checkPermission(permission, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED;
    }

    // System UI's dream starter, the same one Android's own "Start now" uses.
    private void previewScreensaver() {
        if (!open(new Intent(Intent.ACTION_MAIN).setClassName("com.android.systemui", "com.android.systemui.Somnambulator")))
            Toast.makeText(this, Strings.t("Can't show a preview here"), Toast.LENGTH_LONG).show();
    }

    private void openWallpaper() {
        showOnHome("wallpaper");
    }

    // The wallpaper picker and Brightness live on the home screen; it opens them when it comes back.
    private void showOnHome(String what) {
        startActivity(new Intent(this, MainActivity.class).putExtra("show", what)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    private boolean open(Intent i) {
        if (i.resolveActivity(getPackageManager()) == null) return false;
        try {
            startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Android hides isConnected(); a TV without it just reads "Not connected".
    private static boolean connected(BluetoothDevice d) {
        try { return (Boolean) d.getClass().getMethod("isConnected").invoke(d); }
        catch (Exception e) { return false; }
    }

    private static String encoding(int e) {
        switch (e) {
            case AudioFormat.ENCODING_AC3: return "Dolby Digital";
            case AudioFormat.ENCODING_E_AC3: return "Dolby Digital Plus";
            case 18: return "Dolby Atmos"; // AudioFormat.ENCODING_E_AC3_JOC, Android 9+
            case 14: return "Dolby TrueHD"; // AudioFormat.ENCODING_DOLBY_TRUEHD, Android 8+
            case AudioFormat.ENCODING_DTS: return "DTS";
            case AudioFormat.ENCODING_DTS_HD: return "DTS-HD";
            default: return null;
        }
    }

    // A day or more (some TVs store "never" as the largest number there is) reads Never, and under a
    // minute reads in seconds.
    private static String minutes(int ms) {
        if (ms <= 0 || ms >= 86400000) return Strings.t("Never");
        if (ms < 60000) return (ms / 1000) + " s";
        int m = ms / 60000;
        if (m >= 60 && m % 60 == 0) return m == 60 ? Strings.t("1 hour") : String.format(Strings.t("%d hours"), m / 60);
        return m == 1 ? Strings.t("1 minute") : String.format(Strings.t("%d minutes"), m);
    }

    private static int indexOf(int[] a, int v) {
        for (int i = 0; i < a.length; i++) if (a[i] == v) return i;
        return -1;
    }

    private static String gb(long bytes) {
        return String.format(Locale.getDefault(), "%.1f GB", bytes / 1073741824.0);
    }

    private int px(int v) {
        return Math.round(v * scale);
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
