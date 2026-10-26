package com.ansub.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

// Opt-in (Settings > Accessibility > Home button): makes Home open this home screen on TVs where the
// TV's own home screen has to stay on, like Google TV and Chromecast. Nothing gets turned off, so
// switching it off puts Home straight back to the TV's own.
//
// Google TV handles the Home key before accessibility services see it, so this doesn't watch keys.
// It watches for Google TV's main screen (not its other pages) to open, after Home or a restart,
// and opens this one over it.
public class HomeButton extends AccessibilityService {
    private static final ComponentName GOOGLE_TV_HOME = new ComponentName(
            "com.google.android.apps.tv.launcherx", "com.google.android.apps.tv.launcherx.home.HomeActivity");
    // Debounce duplicate events until the activity resumes.
    private static long lastOpened = -500;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || !GOOGLE_TV_HOME.getPackageName().equals(String.valueOf(event.getPackageName()))
                || !GOOGLE_TV_HOME.getClassName().equals(String.valueOf(event.getClassName())) || !setupDone()) return;
        SharedPreferences prefs = prefs(this);
        if (stayingOnGoogleTv(prefs)) return;
        long now = SystemClock.elapsedRealtime();
        if (now - lastOpened < 500) return;

        // Give an unfinished launch time to resume. Successful launches don't delay the next Home.
        long pending = prefs.getLong("pending", -1);
        if (pending >= 0 && now >= pending && now - pending < 3000) return;
        int failures = prefs.getInt("failures", 0);
        if (pending >= 0) failures++;
        if (failures >= 3) {
            prefs.edit().remove("pending").putInt("failures", failures).commit();
            return;
        }
        // Save before launching: a crash can take the service down with the activity.
        if (!prefs.edit().putLong("pending", now).putInt("failures", failures).commit()) return;
        lastOpened = now;
        try {
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception ignored) {}
    }

    // The settings (gear) button on Google TV remotes, TCL's among them, sends the Notification key, and
    // only Google TV's home screen answers it. Once that home screen is off (GOOGLE_TV=1), the button
    // opens this home screen's Settings screen instead, wherever you are; the app buttons open their
    // apps. Every other key, and these on TVs where Google TV's home screen is still on, go on as usual.
    // TCL remotes' app buttons send TCL's own key codes, which Google TV's home screen turns into the
    // app. With it off, they're mapped here: Netflix, Prime Video and YouTube, as on a TCL Smart TV Pro
    // (RC833 remote). A button for an app that isn't installed goes on as usual.
    private static final int[] APP_KEYS = {4062, 4099, 4065};
    private static final String[] APP_KEY_APPS = {"com.netflix.ninja", "com.amazon.amazonvideo.livingroom",
            "com.google.android.youtube.tv"};

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        Intent open = event.getKeyCode() == KeyEvent.KEYCODE_NOTIFICATION
                ? new Intent(this, SettingsActivity.class) : appKeyIntent(this, event.getKeyCode());
        if (open == null || !googleTvHomeOff(this) || !setupDone()) return false;
        if (event.getAction() == KeyEvent.ACTION_UP) {
            try {
                startActivity(open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception ignored) {}
        }
        return true;
    }

    // TCL lets its Netflix button past accessibility services, so this service never sees it. It still
    // reaches the app in front, so this home screen opens the app itself (MainActivity.onKeyUp).
    static boolean openAppKey(Context c, KeyEvent event) {
        Intent open = appKeyIntent(c, event.getKeyCode());
        if (open == null || !googleTvHomeOff(c)) return false;
        if (event.getAction() == KeyEvent.ACTION_UP) {
            try {
                c.startActivity(open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception ignored) {}
        }
        return true;
    }

    private static Intent appKeyIntent(Context c, int code) {
        for (int i = 0; i < APP_KEYS.length; i++) {
            if (code != APP_KEYS[i]) continue;
            Intent open = c.getPackageManager().getLeanbackLaunchIntentForPackage(APP_KEY_APPS[i]);
            return open != null ? open : c.getPackageManager().getLaunchIntentForPackage(APP_KEY_APPS[i]);
        }
        return null;
    }

    // Installed but turned off: on a TV that never had it, the key may mean something else.
    private static boolean googleTvHomeOff(Context c) {
        try {
            return !c.getPackageManager().getApplicationInfo(GOOGLE_TV_HOME.getPackageName(), 0).enabled;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onInterrupt() {}

    // During the TV's first setup, Home is left alone.
    private boolean setupDone() {
        return Settings.Secure.getInt(getContentResolver(), "user_setup_complete", 1) == 1;
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("home_button", 0);
    }

    // Back on this home screen: Home comes here again.
    static void onHomeResumed(Context c) {
        prefs(c).edit().remove("google_tv_boot").remove("google_tv_boot_count").commit();
    }

    // This home screen opened and didn't crash. Also resets a stopped redirect loop.
    static void onHomeOk(Context c) {
        prefs(c).edit().remove("pending").remove("failures").commit();
        lastOpened = -500;
    }

    private static int bootCount(Context c) {
        return Build.VERSION.SDK_INT >= 24
                ? Settings.Global.getInt(c.getContentResolver(), Settings.Global.BOOT_COUNT, -1) : -1;
    }

    private static long bootTime() {
        return System.currentTimeMillis() - SystemClock.elapsedRealtime();
    }

    private boolean stayingOnGoogleTv(SharedPreferences prefs) {
        if (!prefs.contains("google_tv_boot")) return false;
        int boot = bootCount(this);
        int saved = prefs.getInt("google_tv_boot_count", -1);
        if (boot >= 0 && saved >= 0) return boot == saved;
        return Math.abs(bootTime() - prefs.getLong("google_tv_boot", 0)) < 5000;
    }

    // Opens Google TV's own home screen and keeps it there. False if this TV doesn't have it.
    static boolean openGoogleTv(Context c) {
        SharedPreferences prefs = prefs(c);
        // Keep the choice through process death and updates, but not a TV restart.
        if (!prefs.edit().putLong("google_tv_boot", bootTime())
                .putInt("google_tv_boot_count", bootCount(c)).commit()) return false;
        try {
            c.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .setComponent(GOOGLE_TV_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (Exception e) {
            prefs.edit().remove("google_tv_boot").remove("google_tv_boot_count").commit();
            return false;
        }
    }
}
