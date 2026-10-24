package com.ansub.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.provider.Settings;
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
    // Google TV's home screen was asked for (Back held on this one): leave it there until this home
    // screen is opened again (its tile in Google TV's apps row) or the TV restarts.
    static volatile boolean googleTv;
    private long lastOpened;

    // Not more than once every 3 seconds: if this home screen ever crashes, the TV's own stays usable
    // instead of the two taking turns.
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || googleTv
                || !GOOGLE_TV_HOME.getClassName().equals(String.valueOf(event.getClassName())) || !setupDone()) return;
        if (SystemClock.elapsedRealtime() - lastOpened < 3000) return;
        lastOpened = SystemClock.elapsedRealtime();
        try {
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception ignored) {}
    }

    @Override
    public void onInterrupt() {}

    // During the TV's first setup, Home is left alone.
    private boolean setupDone() {
        return Settings.Secure.getInt(getContentResolver(), "user_setup_complete", 1) == 1;
    }

    // Opens Google TV's own home screen and keeps it there. False if this TV doesn't have it.
    static boolean openGoogleTv(Context c) {
        googleTv = true;
        try {
            c.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .setComponent(GOOGLE_TV_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (Exception e) {
            googleTv = false;
            return false;
        }
    }
}
