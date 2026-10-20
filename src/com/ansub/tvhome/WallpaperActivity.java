package com.ansub.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Picks the home screen's background, in the home screen's own look. The first page has the wallpapers
// used recently (and the original gradient), every one saved on the TV, and two ways to get a new one:
// pictures on the TV or a USB drive, or free photos online (Pexels with a key, else Lorem Picsum, which
// needs none). Picking one saves it at screen size, puts it in use and goes home. Back goes up a page.
public class WallpaperActivity extends Activity {
    private static final String TAG = "tvhome";
    // Layout is designed at 1920x1080 and scaled to the TV's real UI size by px(), like the home screen.
    private static final int MARGIN = 96, GAP = 36, RADIUS = 22, PER_ROW = 5, MAX_PICTURES = 100;
    private static final int MAIN = 0, LOCAL = 1, ONLINE = 2;
    private static final int ASK_PICTURES = 1;
    private static final String[] TOPICS = {"Nature", "Mountains", "Ocean", "Forest", "Space", "City at night",
            "Abstract", "Minimal", "Dark"};

    interface Pick { void at(int which); }
    interface Done { void with(String text); }
    interface Make { Bitmap make() throws IOException; }

    // A picture found on the TV or a USB drive: a name to save it as, how to read it, how it's turned,
    // and its photo-index entry (null when it was found by looking through a drive's folders).
    private static final class Pic {
        final String name;
        final Wallpaper.Source src;
        final int rotate;
        final Uri uri;
        Pic(String name, Wallpaper.Source src, int rotate, Uri uri) {
            this.name = name;
            this.src = src;
            this.rotate = rotate;
            this.uri = uri;
        }
    }

    private float scale = 1f;
    private int cardW, cardH, wallW, wallH;
    private ScrollView scroller;
    private LinearLayout page; // the page's sections
    private TextView title, status;
    private int shown = MAIN;
    // Changes with the page and with each online search: late results for an old one are dropped.
    private volatile int generation;
    private boolean busy;   // a new wallpaper is being saved: other picks wait
    private boolean asking; // Android's permission prompt or app info is in front: stay open
    private boolean askedPictures;
    private final ExecutorService work = Executors.newFixedThreadPool(3);
    // Online: the search shown (null for the feed), its last page, and where results go.
    private String query;
    private int onlinePage;
    private TextView resultsHead;
    private LinearLayout results;
    private View more;

    // A USB drive went in or came out while its page is open.
    private final BroadcastReceiver drivesChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { if (shown == LOCAL) show(LOCAL); }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        MainActivity.loadFonts();
        int screenW = getResources().getDisplayMetrics().widthPixels;
        scale = screenW / 1920f;
        cardW = (screenW - 2 * px(MARGIN) - (PER_ROW - 1) * px(GAP)) / PER_ROW;
        cardH = cardW * 9 / 16;
        int[] size = Wallpaper.size(this);
        wallW = size[0];
        wallH = size[1];
        paint();

        scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        scroller.setVerticalScrollBarEnabled(false);
        scroller.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, px(76), 0, px(MARGIN));
        title = text(px(56), Color.WHITE, MainActivity.semibold);
        status = text(px(26), 0xA8FFFFFF, MainActivity.regular);
        box.addView(title, inset(0));
        box.addView(status, inset(px(8)));
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        box.addView(page, new LinearLayout.LayoutParams(-1, -2));
        scroller.addView(box, new FrameLayout.LayoutParams(-1, -2));
        setContentView(scroller);
        show(MAIN);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_MEDIA_MOUNTED);
        f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        f.addAction(Intent.ACTION_MEDIA_REMOVED);
        f.addDataScheme("file");
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(drivesChanged, f, Context.RECEIVER_EXPORTED);
        else registerReceiver(drivesChanged, f);
        // Back from app info: access may have been allowed there.
        if (asking) {
            asking = false;
            if (shown == LOCAL) show(LOCAL);
        }
    }

    @Override
    protected void onPause() {
        unregisterReceiver(drivesChanged);
        super.onPause();
    }

    // Home was pressed, or something else came to the front: close, so nothing stays behind in RAM.
    @Override
    protected void onStop() {
        super.onStop();
        if (!asking && !isChangingConfigurations()) finish();
    }

    @Override
    protected void onDestroy() {
        generation++;
        work.shutdownNow();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (shown != MAIN) show(MAIN);
        else finish();
    }

    // The wallpaper in use, darker, so the page reads well over any picture.
    private void paint() {
        getWindow().setBackgroundDrawable(new LayerDrawable(new Drawable[] {
                Wallpaper.background(this, scale), new ColorDrawable(0x99000000)}));
    }

    private void show(int which) {
        shown = which;
        generation++;
        busy = false;
        page.removeAllViews();
        results = null;
        more = null;
        status.setText("");
        scroller.scrollTo(0, 0);
        if (which == MAIN) showMain();
        else if (which == LOCAL) showLocal();
        else showOnline();
    }

    private void showMain() {
        title.setText(Strings.t("Wallpaper"));
        status.setText(Strings.t("Pick a picture for your home screen. Hold OK on a downloaded one to delete it."));
        String current = Wallpaper.current(this);

        LinearLayout recent = section(Strings.t("Recently used"));
        ImageView original = card(recent, Strings.t("Original"), Wallpaper.ORIGINAL.equals(current),
                () -> use(Wallpaper.ORIGINAL), null);
        original.setImageDrawable(Wallpaper.gradient(cardW / 1920f));
        View focus = (View) original.getParent();
        for (String name : Wallpaper.recent(this)) {
            ImageView iv = savedCard(recent, name, current, false);
            if (name.equals(current)) focus = (View) iv.getParent();
        }

        List<File> saved = Wallpaper.saved(this);
        LinearLayout all = section(String.format(Locale.getDefault(), Strings.t("Downloaded · %d"), saved.size()));
        if (saved.isEmpty()) note(all, Strings.t("Nothing yet. Every picture you pick or download is kept here."));
        for (File f : saved) savedCard(all, f.getName(), current, true);

        LinearLayout add = section(Strings.t("Get a new one"));
        tile(add, LineIcon.FOLDER, Strings.t("TV storage & USB"), () -> show(LOCAL));
        tile(add, LineIcon.GLOBE, Strings.t("Online photos"), () -> show(ONLINE));
        focusLater(focus);
    }

    private ImageView savedCard(LinearLayout row, final String name, String current, boolean deletable) {
        boolean inUse = name.equals(current);
        ImageView iv = card(row, inUse ? Strings.t("In use") : null, inUse, () -> use(name),
                deletable ? () -> askDelete(name) : null);
        final File f = new File(Wallpaper.dir(this), name);
        load(iv, () -> Wallpaper.crop(() -> new FileInputStream(f), 0, cardW, cardH, Bitmap.Config.RGB_565));
        return iv;
    }

    private void askDelete(final String name) {
        choose(Strings.t("Delete this wallpaper?"), new String[] {Strings.t("Delete"), Strings.t("Cancel")}, which -> {
            if (which != 0) return;
            Wallpaper.delete(this, name);
            paint();
            show(MAIN);
        });
    }

    // Puts a saved picture (or the gradient) in use and goes home, which draws it.
    private void use(String name) {
        Wallpaper.use(this, name);
        finish();
    }

    // Saves a new picture at screen size, then uses it.
    private void keep(final String name, final Make make) {
        if (busy) return;
        busy = true;
        status.setText(Strings.t("Saving…"));
        final int gen = generation;
        work.execute(() -> {
            boolean ok = false;
            try {
                Bitmap b = make.make();
                Wallpaper.save(this, b, name);
                b.recycle();
                ok = true;
            } catch (Throwable t) {
                Log.w(TAG, "save " + name, t);
            }
            final boolean saved = ok;
            runOnUiThread(() -> {
                if (saved) {
                    use(name);
                } else if (gen == generation) {
                    busy = false;
                    status.setText(Strings.t("Couldn't use that picture. Try another one."));
                }
            });
        });
    }

    // ---- Pictures on the TV and on USB drives

    private void showLocal() {
        title.setText(Strings.t("TV storage & USB"));
        if (!canReadPictures()) {
            status.setText(Strings.t("Allow Home to see the pictures on the TV and on USB drives."));
            LinearLayout row = section(null);
            focusLater(tile(row, LineIcon.FOLDER, Strings.t("Allow access"), this::askPictures));
            // Android stops asking after a refusal or two; app info is the way from there.
            if (askedPictures) tile(row, LineIcon.SETTINGS, Strings.t("App info"), this::openAppInfo);
            return;
        }
        status.setText(Strings.t("Looking for pictures…"));
        final int gen = generation;
        work.execute(() -> {
            final Map<String, List<Pic>> found = findPictures();
            runOnUiThread(() -> {
                if (gen != generation) return;
                if (found.isEmpty()) {
                    status.setText(Strings.t("No pictures found. Plug in a USB drive with JPG or PNG pictures and they show up here."));
                    return;
                }
                status.setText(Strings.t("Pick one. It's copied to the TV, so a USB drive can come out afterwards."));
                View first = null;
                for (Map.Entry<String, List<Pic>> e : found.entrySet()) {
                    LinearLayout row = section(e.getKey() + " · " + e.getValue().size());
                    for (final Pic p : e.getValue()) {
                        ImageView iv = card(row, null, false, () -> keep(p.name,
                                () -> Wallpaper.crop(p.src, p.rotate, wallW, wallH, Bitmap.Config.ARGB_8888)), null);
                        if (first == null) first = (View) iv.getParent();
                        load(iv, () -> preview(p));
                    }
                }
                focusLater(first);
            });
        });
    }

    // Pictures on each drive, newest first, MAX_PICTURES at most per drive. They come from Android's
    // photo index; a USB drive it hasn't indexed (yet) is looked through folder by folder.
    private Map<String, List<Pic>> findPictures() {
        Map<String, List<Pic>> out = new LinkedHashMap<>();
        StorageManager sm = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
        List<String> volumes = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 29) volumes.addAll(MediaStore.getExternalVolumeNames(this));
        else volumes.add("external");
        for (String vol : volumes) {
            Uri base = Build.VERSION.SDK_INT >= 29 ? MediaStore.Images.Media.getContentUri(vol)
                    : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            List<Pic> pics = new ArrayList<>();
            try (Cursor c = getContentResolver().query(base, new String[] {MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.ORIENTATION}, null, null, MediaStore.Images.Media.DATE_MODIFIED + " DESC")) {
                while (c != null && c.moveToNext() && pics.size() < MAX_PICTURES) {
                    final Uri u = ContentUris.withAppendedId(base, c.getLong(0));
                    pics.add(new Pic(localName(u.toString()), () -> {
                        InputStream s = getContentResolver().openInputStream(u);
                        if (s == null) throw new IOException("gone");
                        return s;
                    }, c.getInt(1), u));
                }
            } catch (Exception e) {
                Log.w(TAG, "pictures on " + vol, e);
            }
            if (!pics.isEmpty()) out.put(driveName(sm, vol), pics);
        }
        if (Build.VERSION.SDK_INT >= 30 && sm != null) for (StorageVolume v : sm.getStorageVolumes()) {
            File root = v.getDirectory();
            if (!v.isRemovable() || root == null || !Environment.MEDIA_MOUNTED.equals(v.getState())) continue;
            String name = v.getDescription(this);
            if (out.containsKey(name)) continue;
            List<File> files = new ArrayList<>();
            walk(root, 0, files);
            Collections.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            List<Pic> pics = new ArrayList<>();
            for (final File f : files.subList(0, Math.min(files.size(), MAX_PICTURES)))
                pics.add(new Pic(localName(f.getPath()), () -> new FileInputStream(f), Wallpaper.rotation(f.getPath()), null));
            if (!pics.isEmpty()) out.put(name, pics);
        }
        return out;
    }

    private static String localName(String where) {
        return "local-" + Integer.toHexString(where.hashCode()) + ".jpg";
    }

    private String driveName(StorageManager sm, String volume) {
        if (Build.VERSION.SDK_INT < 30 || sm == null || MediaStore.VOLUME_EXTERNAL_PRIMARY.equals(volume))
            return Strings.t("This TV");
        for (StorageVolume v : sm.getStorageVolumes())
            if (volume.equals(v.getMediaStoreVolumeName())) return v.getDescription(this);
        return Strings.t("USB drive");
    }

    // Collects pictures under dir, a few folders deep, skipping hidden folders and apps' own data.
    private static void walk(File dir, int depth, List<File> out) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (out.size() >= 500) return;
            String n = f.getName().toLowerCase(Locale.ROOT);
            if (f.isDirectory()) {
                if (depth < 4 && !n.startsWith(".") && !n.equals("android")) walk(f, depth + 1, out);
            } else if (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp")) {
                out.add(f);
            }
        }
    }

    // A picture's preview: the photo index's own thumbnail when there is one (quick), else a small decode.
    private Bitmap preview(Pic p) throws IOException {
        if (p.uri != null && Build.VERSION.SDK_INT >= 29) try {
            return getContentResolver().loadThumbnail(p.uri, new android.util.Size(cardW, cardH), null);
        } catch (Exception ignored) {}
        return Wallpaper.crop(p.src, p.rotate, cardW, cardH, Bitmap.Config.RGB_565);
    }

    private static String picturesPermission() {
        return Build.VERSION.SDK_INT >= 33 ? "android.permission.READ_MEDIA_IMAGES"
                : "android.permission.READ_EXTERNAL_STORAGE";
    }

    private boolean canReadPictures() {
        return Build.VERSION.SDK_INT < 23 || checkSelfPermission(picturesPermission()) == PackageManager.PERMISSION_GRANTED;
    }

    private void askPictures() {
        if (Build.VERSION.SDK_INT < 23) return;
        askedPictures = true;
        asking = true;
        requestPermissions(new String[] {picturesPermission()}, ASK_PICTURES);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] granted) {
        if (code != ASK_PICTURES) return;
        asking = false;
        if (shown == LOCAL) show(LOCAL);
    }

    private void openAppInfo() {
        asking = true;
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
        } catch (Exception e) {
            asking = false;
        }
    }

    // ---- Free photos online

    private void showOnline() {
        title.setText(Strings.t("Online photos"));
        String key = Wallpaper.pexelsKey(this);
        LinearLayout browse = section(key != null ? Strings.t("Browse Pexels") : null);
        View first;
        if (key != null) {
            first = chip(browse, null, Strings.t("Popular"), () -> search(null));
            for (final String topic : TOPICS) chip(browse, null, Strings.t(topic), () -> search(Strings.t(topic)));
            chip(browse, LineIcon.SEARCH, Strings.t("Search"), () -> ask(Strings.t("Search Pexels"),
                    Strings.t("Like: mountains at sunset"), query, text -> { if (!text.isEmpty()) search(text); }));
            chip(browse, LineIcon.KEY, Strings.t("Pexels key"), this::askKey);
        } else {
            first = chip(browse, LineIcon.KEY, Strings.t("Add a free Pexels key for topics and search"), this::askKey);
        }
        resultsHead = head("");
        results = row();
        search(null);
        focusLater(first);
    }

    private void askKey() {
        ask(Strings.t("Pexels key"), Strings.t("Paste your free key from pexels.com/api"), Wallpaper.pexelsKey(this), text -> {
            Wallpaper.setPexelsKey(this, text);
            show(ONLINE);
        });
    }

    // Shows the first page of a search (q null: the Pexels feed, or Lorem Picsum without a key).
    private void search(String q) {
        query = q;
        onlinePage = 0;
        generation++;
        busy = false;
        results.removeAllViews();
        more = null;
        resultsHead.setText(Wallpaper.pexelsKey(this) == null ? Strings.t("Unsplash photos, via Lorem Picsum")
                : q == null ? Strings.t("Popular on Pexels") : q);
        loadMore();
    }

    private void loadMore() {
        final int gen = generation, pageNo = ++onlinePage;
        final String key = Wallpaper.pexelsKey(this), q = query;
        status.setText(Strings.t("Loading…"));
        work.execute(() -> {
            List<Wallpaper.Photo> photos = null;
            String error = null;
            try {
                photos = key != null ? Wallpaper.pexels(key, q, pageNo) : Wallpaper.picsum(pageNo);
            } catch (IOException e) {
                error = "key".equals(e.getMessage()) ? Strings.t("Pexels didn't accept the key. Check it under Pexels key.")
                        : Strings.t("Couldn't load photos. Check the TV's internet connection.");
            }
            final List<Wallpaper.Photo> got = photos;
            final String err = error;
            runOnUiThread(() -> {
                if (gen != generation) return;
                if (err != null) {
                    status.setText(err);
                    return;
                }
                status.setText(key != null ? Strings.t("Photos provided by Pexels. Pick one to download it and use it.")
                        : Strings.t("Free photos from Unsplash. Pick one to download it and use it."));
                if (more != null) results.removeView((View) more.getParent());
                more = null;
                View firstNew = null;
                for (final Wallpaper.Photo p : got) {
                    String credit = p.credit.isEmpty() ? null : String.format(Strings.t("Photo by %s"), p.credit);
                    ImageView iv = card(results, credit, false, () -> keep(p.name,
                            () -> Wallpaper.fetch(this, p.full, wallW, wallH, Bitmap.Config.ARGB_8888)), null);
                    if (firstNew == null) firstNew = (View) iv.getParent();
                    load(iv, () -> Wallpaper.fetch(this, p.thumb, cardW, cardH, Bitmap.Config.RGB_565));
                }
                if (got.isEmpty() && pageNo == 1) note(results, Strings.t("Nothing found. Try another word."));
                if (got.size() >= Wallpaper.perPage()) more = tile(results, LineIcon.PLUS, Strings.t("More"), this::loadMore);
                if (pageNo > 1 && firstNew != null) firstNew.requestFocus();
            });
        });
    }

    // ---- Building blocks, in the home screen's look

    // Fills a card's picture in the background. Previews are small and in 16-bit colour to save RAM.
    private void load(final ImageView iv, final Make make) {
        final int gen = generation;
        work.execute(() -> {
            if (gen != generation) return;
            final Bitmap b;
            try {
                b = make.make();
            } catch (Throwable t) {
                Log.w(TAG, "preview", t);
                return;
            }
            runOnUiThread(() -> { if (gen == generation) iv.setImageBitmap(b); });
        });
    }

    private LinearLayout section(String name) {
        head(name);
        return row();
    }

    // A section's name; null for none.
    private TextView head(String name) {
        TextView t = text(px(30), 0xD9FFFFFF, MainActivity.semibold);
        if (name != null) t.setText(name);
        else t.setVisibility(View.GONE);
        LinearLayout.LayoutParams lp = inset(px(44));
        page.addView(t, lp);
        return t;
    }

    // One sideways row that scrolls with the focus. Cards start at the page margin and fade out at the
    // screen's edges, so a focused card that grows never touches them. The row itself never takes
    // focus, so an empty one is skipped.
    private LinearLayout row() {
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setFocusable(false);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setOverScrollMode(View.OVER_SCROLL_NEVER);
        hs.setHorizontalFadingEdgeEnabled(true);
        hs.setFadingEdgeLength(px(MARGIN));
        hs.setClipChildren(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setClipChildren(false);
        row.setClipToPadding(false); // a focused card grows into the padding
        row.setPadding(px(MARGIN), px(24), px(MARGIN) - px(GAP), 0);
        hs.addView(row);
        page.addView(hs, new LinearLayout.LayoutParams(-1, -2));
        return row;
    }

    private void note(LinearLayout row, String text) {
        TextView t = text(px(26), 0x80FFFFFF, MainActivity.regular);
        t.setText(text);
        t.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(t, new LinearLayout.LayoutParams(-2, cardH / 2));
    }

    // A 16:9 picture card. Returns its picture, to fill in later.
    private ImageView card(LinearLayout row, String caption, boolean inUse, Runnable onPick, Runnable onHold) {
        FrameLayout card = new FrameLayout(this);
        card.setBackground(rounded(0x13FFFFFF, RADIUS));
        card.setClipToOutline(true);
        ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        card.addView(iv, new FrameLayout.LayoutParams(-1, -1));
        if (inUse) {
            GradientDrawable ring = rounded(Color.TRANSPARENT, RADIUS);
            ring.setStroke(px(4), Color.WHITE);
            card.setForeground(ring);
        }
        add(row, card, null, caption, onPick, onHold);
        return iv;
    }

    // A quiet glass card with a line icon above its name, like the home screen's Settings tile.
    private View tile(LinearLayout row, int[] icon, String label, Runnable onPick) {
        FrameLayout card = new FrameLayout(this);
        GradientDrawable bg = rounded(0x13FFFFFF, RADIUS);
        bg.setStroke(Math.max(1, px(2)), 0x17FFFFFF);
        card.setBackground(bg);
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER);
        ImageView iv = new ImageView(this);
        iv.setImageDrawable(new LineIcon(icon));
        inner.addView(iv, new LinearLayout.LayoutParams(px(44), px(44)));
        TextView t = text(px(24), 0xE0FFFFFF, MainActivity.regular);
        t.setText(label);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setGravity(Gravity.CENTER);
        t.setPadding(px(16), 0, px(16), 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.topMargin = px(12);
        inner.addView(t, lp);
        card.addView(inner, new FrameLayout.LayoutParams(-1, -1));
        add(row, card, bg, null, onPick, null);
        return card;
    }

    // Focus makes a card grow (and a glass one brighter), and shows its caption underneath. OK runs
    // onPick; holding OK, or Menu, runs onHold.
    private void add(LinearLayout row, FrameLayout card, final GradientDrawable glass, String caption,
                     final Runnable onPick, final Runnable onHold) {
        card.setFocusable(true);
        final TextView cap = caption == null ? null : text(px(24), Color.WHITE, MainActivity.regular);
        if (cap != null) {
            cap.setText(caption);
            cap.setSingleLine(true);
            cap.setEllipsize(TextUtils.TruncateAt.END);
            cap.setGravity(Gravity.CENTER_HORIZONTAL);
            cap.setVisibility(View.INVISIBLE);
        }
        card.setOnFocusChangeListener((v, has) -> {
            if (glass != null) glass.setColor(has ? 0x38FFFFFF : 0x13FFFFFF);
            v.animate().scaleX(has ? 1.1f : 1f).scaleY(has ? 1.1f : 1f).translationZ(has ? px(26) : 0)
                    .setDuration(140).withLayer().start();
            if (cap != null) cap.setVisibility(has ? View.VISIBLE : View.INVISIBLE);
        });
        card.setOnClickListener(v -> onPick.run());
        if (onHold != null) {
            card.setOnLongClickListener(v -> {
                onHold.run();
                return true;
            });
            card.setOnKeyListener((v, code, ev) -> {
                if (code != KeyEvent.KEYCODE_MENU) return false;
                if (ev.getAction() == KeyEvent.ACTION_UP) onHold.run();
                return true;
            });
        }
        FrameLayout wrap = new FrameLayout(this);
        wrap.setClipChildren(false);
        wrap.addView(card, new FrameLayout.LayoutParams(cardW, cardH));
        if (cap != null) {
            FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(cardW, -2);
            cp.topMargin = cardH + px(22);
            wrap.addView(cap, cp);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(cardW, cardH + px(70));
        lp.rightMargin = px(GAP);
        row.addView(wrap, lp);
    }

    // A small glass pill, for topics and actions.
    private View chip(LinearLayout row, int[] icon, String label, final Runnable onPick) {
        TextView t = text(px(26), 0xE6FFFFFF, MainActivity.regular);
        t.setText(label);
        t.setSingleLine(true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(px(30), 0, px(30), 0);
        if (icon != null) {
            LineIcon li = new LineIcon(icon);
            li.setBounds(0, 0, px(32), px(32));
            t.setCompoundDrawablesRelative(li, null, null, null);
            t.setCompoundDrawablePadding(px(14));
        }
        final GradientDrawable bg = rounded(0x13FFFFFF, 40);
        bg.setStroke(Math.max(1, px(2)), 0x17FFFFFF);
        t.setBackground(bg);
        t.setFocusable(true);
        t.setOnFocusChangeListener((v, has) -> {
            bg.setColor(has ? 0x38FFFFFF : 0x13FFFFFF);
            v.animate().scaleX(has ? 1.08f : 1f).scaleY(has ? 1.08f : 1f).setDuration(140).withLayer().start();
        });
        t.setOnClickListener(v -> onPick.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, px(80));
        lp.rightMargin = px(20);
        row.addView(t, lp);
        return t;
    }

    // A small menu in the middle of the screen, in the same glass as the home screen's menus.
    private void choose(String heading, String[] items, final Pick pick) {
        LinearLayout box = box(heading);
        final Dialog d = dialog(box);
        for (int i = 0; i < items.length; i++) {
            final int which = i;
            menuRow(box, items[i], () -> {
                d.dismiss();
                pick.at(which);
            });
        }
        d.show();
    }

    // A one-line text box; the TV's keyboard opens for it. done gets the text, trimmed.
    private void ask(String heading, String hint, String text, final Done done) {
        LinearLayout box = box(heading);
        final Dialog d = dialog(box);
        final EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT);
        e.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        e.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(28));
        e.setTypeface(MainActivity.regular);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(0x80FFFFFF);
        e.setHint(hint);
        if (text != null) {
            e.setText(text);
            e.setSelection(text.length());
        }
        GradientDrawable field = rounded(0x13FFFFFF, RADIUS - 10);
        field.setStroke(Math.max(1, px(2)), 0x38FFFFFF);
        e.setBackground(field);
        e.setPadding(px(22), px(16), px(22), px(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = px(20);
        lp.bottomMargin = px(12);
        box.addView(e, lp);
        final Runnable finish = () -> {
            d.dismiss();
            done.with(e.getText().toString().trim());
        };
        e.setOnEditorActionListener((v, action, ev) -> {
            finish.run();
            return true;
        });
        menuRow(box, Strings.t("OK"), finish);
        menuRow(box, Strings.t("Cancel"), d::dismiss);
        d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        d.show();
        e.requestFocus();
    }

    private LinearLayout box(String heading) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(px(28), px(28), px(28), px(28));
        GradientDrawable bg = rounded(0xFF231D1F, RADIUS);
        bg.setStroke(Math.max(1, px(2)), 0x17FFFFFF);
        box.setBackground(bg);
        TextView head = text(px(30), Color.WHITE, MainActivity.semibold);
        head.setText(heading);
        head.setPadding(px(22), 0, px(22), px(16));
        box.addView(head);
        return box;
    }

    private Dialog dialog(LinearLayout box) {
        Dialog d = new Dialog(this, android.R.style.Theme_Material_Dialog_NoActionBar);
        d.setContentView(box);
        Window w = d.getWindow();
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        w.setLayout(px(760), -2);
        w.setDimAmount(0.5f);
        return d;
    }

    private void menuRow(LinearLayout box, String label, final Runnable run) {
        final TextView t = text(px(26), 0xD9FFFFFF, MainActivity.regular);
        t.setText(label);
        t.setSingleLine(true);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(px(22), 0, px(22), 0);
        final GradientDrawable focus = rounded(Color.TRANSPARENT, RADIUS - 10);
        t.setBackground(focus);
        t.setFocusable(true);
        t.setOnFocusChangeListener((v, has) -> {
            focus.setColor(has ? 0x38FFFFFF : Color.TRANSPARENT);
            t.setTextColor(has ? Color.WHITE : 0xD9FFFFFF);
        });
        t.setOnClickListener(v -> run.run());
        box.addView(t, new LinearLayout.LayoutParams(-1, px(64)));
    }

    private static void focusLater(final View v) {
        if (v != null) v.post(v::requestFocus);
    }

    private LinearLayout.LayoutParams inset(int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.leftMargin = px(MARGIN);
        lp.rightMargin = px(MARGIN);
        lp.topMargin = top;
        return lp;
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(px(radius));
        g.setColor(color);
        return g;
    }

    private int px(int v) {
        return Math.round(v * scale);
    }

    private TextView text(int px, int color, android.graphics.Typeface tf) {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
        t.setTextColor(color);
        if (tf != null) t.setTypeface(tf);
        return t;
    }
}
