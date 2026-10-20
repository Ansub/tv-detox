package com.ansub.tvhome;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

// Holds the night dim: a window above every app, darkening everything below it.
//
// Some TVs ignore Android's own brightness, so there is nothing to ask the system for:
// screen_brightness writes stick and the framework tracks them, but the panel never changes, and
// dumpsys color_display can report "Night display: Not available". A window above every app is the
// one thing the firmware cannot overrule, because it is plain compositing.
//
// The dim is FLAG_DIM_BEHIND with dimAmount, applied by the compositor to everything below the
// window, so it needs no pixels of its own: the window stays 1x1 and draws nothing. The view exists
// only because a window needs one.
//
// It takes no input either: NOT_FOCUSABLE and NOT_TOUCHABLE, so the remote still reaches whatever
// is underneath.
public class TintService extends Service {

    static final String EXTRA_DIM = "dim";
    private static final String CHANNEL = "tint";

    private WindowManager wm;
    private View view;

    // Shows the dim at this strength (0 to 100), or takes it away at 0.
    // Returns false when the overlay permission is missing, so the caller can send people to it.
    static boolean apply(Context c, int dim) {
        if (dim <= 0) {
            c.stopService(new Intent(c, TintService.class));
            return true;
        }
        if (!canDraw(c)) return false;
        Intent i = new Intent(c, TintService.class).putExtra(EXTRA_DIM, dim);
        // A foreground service survives the home screen going to the background; a plain one would be
        // stopped behind an app, which is exactly when the dim is wanted.
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
        else c.startService(i);
        return true;
    }

    static boolean canDraw(Context c) {
        // Before Android 6 the permission is granted at install time.
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(c);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        foreground();
        int dim;
        if (intent == null) {
            // START_STICKY restarted us after the system stopped the service, and a sticky restart
            // carries no intent. The saved level is the one to come back with: reading 0 from a
            // missing extra here would turn the dim off exactly when it is meant to return.
            SharedPreferences p = getSharedPreferences(MainActivity.PREFS, 0);
            if (!p.getBoolean(MainActivity.NIGHT, false)) {
                stopSelf();
                return START_NOT_STICKY;
            }
            dim = p.getInt(EXTRA_DIM, MainActivity.DIM_DEFAULT);
        } else {
            dim = intent.getIntExtra(EXTRA_DIM, 0);
        }
        if (dim <= 0 || !show(dim)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        // START_STICKY so the dim comes back if the system stops the service to free memory.
        return START_STICKY;
    }

    private boolean show(int dim) {
        if (wm == null) wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) return false;
        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(1, 1, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.dimAmount = (dim > 100 ? 100 : dim) / 100f;
        if (view == null) {
            view = new View(this);
            try {
                wm.addView(view, lp);
            } catch (Exception e) {
                view = null;
                return false;
            }
        } else {
            try { wm.updateViewLayout(view, lp); } catch (Exception ignored) {}
        }
        return true;
    }

    // Android 8+ needs a notification to keep a service in the foreground. A TV has no notification
    // shade, so this is never seen; it is kept at the lowest importance so it makes no sound.
    private void foreground() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (nm.getNotificationChannel(CHANNEL) == null)
            nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Night", NotificationManager.IMPORTANCE_MIN));
        Notification n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_day)
                .setContentTitle(Strings.t("Night"))
                .build();
        try { startForeground(1, n); } catch (Exception ignored) {}
    }

    @Override
    public void onDestroy() {
        if (view != null && wm != null) try { wm.removeView(view); } catch (Exception ignored) {}
        view = null;
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent i) { return null; }
}
