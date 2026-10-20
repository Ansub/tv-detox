package com.ansub.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.media.ExifInterface;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// The home screen's background: the warm gradient it ships with, or a picture picked in
// WallpaperActivity. Pictures are saved once, cropped to the screen's shape at 1920 wide, in the app's
// own storage: drawing one is a single decode, and a USB drive can come out afterwards.
final class Wallpaper {
    static final String ORIGINAL = "original"; // the gradient
    static final String PEXELS_KEY = "pexels";
    private static final String PREFS = "config", CURRENT = "wallpaper", RECENT = "wallpaper_recent";
    private static final int MAX_RECENT = 10, PER_PAGE = 30;

    interface Source { InputStream open() throws IOException; }

    // One online photo: a file name to save it as, a small 16:9 preview, the full picture, who took it.
    static final class Photo {
        final String name, thumb, full, credit;
        Photo(String name, String thumb, String full, String credit) {
            this.name = name;
            this.thumb = thumb;
            this.full = full;
            this.credit = credit;
        }
    }

    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, 0); }

    static File dir(Context c) {
        File d = new File(c.getFilesDir(), "wallpapers");
        d.mkdirs();
        return d;
    }

    // The saved picture in use, or ORIGINAL.
    static String current(Context c) {
        String name = prefs(c).getString(CURRENT, ORIGINAL);
        return ORIGINAL.equals(name) || new File(dir(c), name).isFile() ? name : ORIGINAL;
    }

    // Puts a saved picture (or ORIGINAL) in use and at the front of the recent list.
    static void use(Context c, String name) {
        List<String> recent = recent(c);
        recent.remove(name);
        if (!ORIGINAL.equals(name)) recent.add(0, name);
        while (recent.size() > MAX_RECENT) recent.remove(recent.size() - 1);
        prefs(c).edit().putString(CURRENT, name).putString(RECENT, TextUtils.join(",", recent)).apply();
    }

    // Pictures used before, latest first, skipping deleted ones.
    static List<String> recent(Context c) {
        List<String> out = new ArrayList<>();
        for (String n : prefs(c).getString(RECENT, "").split(","))
            if (!n.isEmpty() && new File(dir(c), n).isFile()) out.add(n);
        return out;
    }

    // Every saved picture, newest first.
    static List<File> saved(Context c) {
        File[] files = dir(c).listFiles((d, n) -> n.endsWith(".jpg"));
        List<File> out = files == null ? new ArrayList<File>() : new ArrayList<>(Arrays.asList(files));
        Collections.sort(out, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return out;
    }

    // Deleting the picture in use goes back to the gradient.
    static void delete(Context c, String name) {
        if (name.equals(current(c))) use(c, ORIGINAL);
        new File(dir(c), name).delete();
        prefs(c).edit().putString(RECENT, TextUtils.join(",", recent(c))).apply();
    }

    static String pexelsKey(Context c) { return prefs(c).getString(PEXELS_KEY, null); }

    static void setPexelsKey(Context c, String key) {
        if (key == null || key.trim().isEmpty()) prefs(c).edit().remove(PEXELS_KEY).apply();
        else prefs(c).edit().putString(PEXELS_KEY, key.trim()).apply();
    }

    // The size pictures are saved at: 1920 wide, as tall as the screen's shape.
    static int[] size(Context c) {
        android.util.DisplayMetrics dm = c.getResources().getDisplayMetrics();
        int w = Math.max(dm.widthPixels, dm.heightPixels), h = Math.min(dm.widthPixels, dm.heightPixels);
        return new int[] {1920, Math.round(1920f * h / w)};
    }

    // The window background: the picture in use under a veil that keeps the clock and the tiles
    // readable, else the gradient. Drawn once by the window: no extra view, no animation.
    static Drawable background(Context c, float scale) {
        String name = current(c);
        if (!ORIGINAL.equals(name)) try {
            Bitmap b = BitmapFactory.decodeFile(new File(dir(c), name).getPath());
            if (b != null) {
                GradientDrawable veil = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[] {0x8C000000, 0x33000000, 0x59000000, 0xD9000000});
                return new LayerDrawable(new Drawable[] {new BitmapDrawable(c.getResources(), b), veil});
            }
        } catch (Throwable ignored) {}
        return gradient(scale);
    }

    // The warm gradient the home screen ships with. scale is the target's width / 1920.
    static Drawable gradient(float scale) {
        GradientDrawable base = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0xFF15100F, 0xFF0A0809, 0xFF050405});
        GradientDrawable warm = radial(0x36FF8048, 0.08f, -0.12f, Math.round(1250 * scale));
        GradientDrawable plum = radial(0x2EAA3C8C, 1.0f, 1.1f, Math.round(1000 * scale));
        for (GradientDrawable g : new GradientDrawable[] {base, warm, plum}) g.setDither(true);
        return new LayerDrawable(new Drawable[] {base, warm, plum});
    }

    private static GradientDrawable radial(int color, float cx, float cy, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        g.setColors(new int[] {color, color & 0x00FFFFFF});
        g.setGradientCenter(cx, cy);
        g.setGradientRadius(r);
        return g;
    }

    // Decodes a picture no bigger than needed, turns it upright and center-crops it to w x h.
    static Bitmap crop(Source in, int rotate, int w, int h, Bitmap.Config config) throws IOException {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        try (InputStream s = in.open()) { BitmapFactory.decodeStream(s, null, o); }
        int sw = o.outWidth, sh = o.outHeight;
        if (sw <= 0 || sh <= 0) throw new IOException("not a picture");
        if (rotate % 180 != 0) { int t = sw; sw = sh; sh = t; }
        int sample = 1;
        while (sw / (sample * 2) >= w && sh / (sample * 2) >= h) sample *= 2;
        o = new BitmapFactory.Options();
        o.inSampleSize = sample;
        o.inPreferredConfig = config;
        Bitmap src;
        try (InputStream s = in.open()) { src = BitmapFactory.decodeStream(s, null, o); }
        if (src == null) throw new IOException("can't read this picture");
        Matrix m = new Matrix();
        m.postRotate(rotate);
        RectF r = new RectF(0, 0, src.getWidth(), src.getHeight());
        m.mapRect(r);
        float s = Math.max(w / r.width(), h / r.height());
        m.postTranslate(-r.left, -r.top);
        m.postScale(s, s);
        m.postTranslate((w - r.width() * s) / 2, (h - r.height() * s) / 2);
        Bitmap out = Bitmap.createBitmap(w, h, config);
        new Canvas(out).drawBitmap(src, m, new Paint(Paint.FILTER_BITMAP_FLAG));
        src.recycle();
        return out;
    }

    // How far a camera picture is turned, from its EXIF tag.
    static int rotation(String path) {
        try {
            switch (new ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)) {
                case ExifInterface.ORIENTATION_ROTATE_90: return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                default: return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    // Saves a picture as name, written in full before it replaces anything.
    static void save(Context c, Bitmap b, String name) throws IOException {
        File part = new File(dir(c), name + ".part"), f = new File(dir(c), name);
        try (OutputStream out = new FileOutputStream(part)) {
            if (!b.compress(Bitmap.CompressFormat.JPEG, 90, out)) throw new IOException("couldn't save");
        }
        if (!part.renameTo(f)) throw new IOException("couldn't save");
        f.setLastModified(System.currentTimeMillis());
    }

    // Downloads a picture and crops it to w x h. It goes to a file first, as crop reads it twice.
    static Bitmap fetch(Context c, String url, int w, int h, Bitmap.Config config) throws IOException {
        File f = File.createTempFile("download", ".img", c.getCacheDir());
        try {
            HttpURLConnection h2 = connect(url, null);
            try (InputStream in = h2.getInputStream(); OutputStream out = new FileOutputStream(f)) {
                copy(in, out);
            } finally {
                h2.disconnect();
            }
            return crop(() -> new FileInputStream(f), 0, w, h, config);
        } finally {
            f.delete();
        }
    }

    // Pexels: the curated feed (query null) or a search, landscape only. Needs a free key from
    // pexels.com/api. An IOException with the message "key" means Pexels turned the key down.
    static List<Photo> pexels(String key, String query, int page) throws IOException {
        String url = query == null ? "https://api.pexels.com/v1/curated?per_page=" + PER_PAGE + "&page=" + page
                : "https://api.pexels.com/v1/search?orientation=landscape&per_page=" + PER_PAGE + "&page=" + page
                + "&query=" + URLEncoder.encode(query, "UTF-8");
        List<Photo> out = new ArrayList<>();
        try {
            JSONArray a = new JSONObject(text(url, key)).getJSONArray("photos");
            for (int i = 0; i < a.length(); i++) {
                JSONObject p = a.getJSONObject(i);
                String original = p.getJSONObject("src").getString("original") + "?auto=compress&cs=tinysrgb&fit=crop";
                out.add(new Photo("pexels-" + p.getLong("id") + ".jpg", original + "&w=480&h=270",
                        original + "&w=1920&h=1080", p.optString("photographer")));
            }
        } catch (JSONException e) {
            throw new IOException("unexpected reply");
        }
        return out;
    }

    // Lorem Picsum: free Unsplash photos, no key, no search.
    static List<Photo> picsum(int page) throws IOException {
        List<Photo> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(text("https://picsum.photos/v2/list?limit=" + PER_PAGE + "&page=" + page, null));
            for (int i = 0; i < a.length(); i++) {
                JSONObject p = a.getJSONObject(i);
                String id = p.getString("id");
                out.add(new Photo("picsum-" + id + ".jpg", "https://picsum.photos/id/" + id + "/480/270",
                        "https://picsum.photos/id/" + id + "/1920/1080", p.optString("author")));
            }
        } catch (JSONException e) {
            throw new IOException("unexpected reply");
        }
        return out;
    }

    static int perPage() { return PER_PAGE; }

    private static HttpURLConnection connect(String url, String auth) throws IOException {
        HttpURLConnection h = (HttpURLConnection) new URL(url).openConnection();
        h.setConnectTimeout(15000);
        h.setReadTimeout(30000);
        h.setRequestProperty("User-Agent", "tvhome");
        if (auth != null) h.setRequestProperty("Authorization", auth);
        int code = h.getResponseCode();
        if (code / 100 != 2) {
            h.disconnect();
            throw new IOException(code == 401 || code == 403 ? "key" : "HTTP " + code);
        }
        return h;
    }

    private static String text(String url, String auth) throws IOException {
        HttpURLConnection h = connect(url, auth);
        try (InputStream in = h.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            copy(in, out);
            return out.toString("UTF-8");
        } finally {
            h.disconnect();
        }
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[16384];
        for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
    }
}
