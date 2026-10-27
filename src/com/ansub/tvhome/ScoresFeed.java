package com.ansub.tvhome;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Fetches the scores for the lock screen widgets in the background, only while the home screen is in
// front: the three league scoreboards every minute while anything is live (every 15 minutes otherwise),
// and the three "Your teams" schedules (much bigger) every 15 minutes. Results come back on the main thread.
final class ScoresFeed {
    interface Listener {
        void onScores(List<Scores.Game> live, List<Scores.Game> upcoming, List<String> teamLines);
        void onOffline();
    }

    static final String TAG = "TvHomeScores";
    private static final int TIMEOUT_MS = 8_000, MAX_BYTES = 4 << 20;
    private static final long SCHEDULES_MS = 900_000;

    private final Context context;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Runnable fetch = this::fetchNow;
    private volatile boolean running;
    // Kept between fetches (worker thread only): team schedules and when they were last read.
    private final List<List<Scores.Game>> schedules = new ArrayList<>();
    private long schedulesAt = -1;

    ScoresFeed(Context c, Listener l) {
        context = c.getApplicationContext();
        listener = l;
    }

    void start() {
        if (running) return;
        running = true;
        main.removeCallbacks(fetch);
        main.post(fetch);
    }

    void stop() {
        running = false;
        main.removeCallbacks(fetch);
    }

    // Changeable so an unreachable server can be tried out: --es scores_base https://invalid.invalid/
    private String base() {
        String b = context.getSharedPreferences("config", 0).getString("scores_base", null);
        return b == null ? Scores.BASE : b;
    }

    private void fetchNow() {
        if (!running) return;
        final String base = base();
        worker.execute(() -> {
            List<Scores.Game> boards = new ArrayList<>();
            int ok = 0;
            for (String[] league : Scores.LEAGUES) {
                String json = get(base + league[1] + "/scoreboard");
                if (json != null) ok++;
                boards.addAll(Scores.scoreboard(json, league[0]));
            }
            boolean failed = ok == 0;
            long now = SystemClock.elapsedRealtime();
            if (!failed && (schedulesAt < 0 || now - schedulesAt >= SCHEDULES_MS)) {
                // A failed download keeps the team's last schedule and is tried again on the next tick.
                boolean allOk = true;
                for (int i = 0; i < Scores.TEAMS.length && running; i++) {
                    String[] t = Scores.TEAMS[i];
                    String json = get(base + path(t[0]) + "/teams/" + t[1] + "/schedule");
                    allOk &= json != null;
                    List<Scores.Game> previous = i < schedules.size() ? schedules.get(i) : new ArrayList<Scores.Game>();
                    List<Scores.Game> next = Scores.keepOrReplace(previous, json, t[0]);
                    if (i < schedules.size()) schedules.set(i, next); else schedules.add(next);
                }
                if (allOk) schedulesAt = now;
            }
            // A team's game on today's scoreboard is fresher than the copy in its schedule. Ids repeat
            // across leagues, so games are keyed by league and id.
            Map<String, Scores.Game> byId = new LinkedHashMap<>();
            for (List<Scores.Game> s : schedules) for (Scores.Game g : s) byId.put(g.league + "/" + g.id, g);
            for (Scores.Game g : boards) if (byId.containsKey(g.league + "/" + g.id)) byId.put(g.league + "/" + g.id, g);
            List<Scores.Game> merged = new ArrayList<>(byId.values());
            List<String> lines = new ArrayList<>();
            long wall = System.currentTimeMillis();
            TimeZone tz = TimeZone.getDefault();
            for (String[] t : Scores.TEAMS) lines.add(merged.isEmpty() ? "" : Scores.teamLine(merged, t[0], t[1], wall, tz));
            // The Hurricanes can be live without being on the college Top 25 scoreboard.
            Map<String, Scores.Game> all = new LinkedHashMap<>();
            for (Scores.Game g : boards) all.put(g.league + "/" + g.id, g);
            for (Scores.Game g : merged) if (!all.containsKey(g.league + "/" + g.id)) all.put(g.league + "/" + g.id, g);
            List<Scores.Game> live = Scores.live(new ArrayList<>(all.values()));
            List<Scores.Game> upcoming = Scores.upcomingToday(boards, wall, tz);
            Log.i(TAG, "fetched: " + (failed ? "failed" : boards.size() + " games, " + live.size() + " live"));
            main.post(() -> {
                if (!running) return;
                if (failed) listener.onOffline();
                else listener.onScores(live, upcoming, lines);
                main.removeCallbacks(fetch);
                main.postDelayed(fetch, Scores.nextDelayMs(!live.isEmpty(), failed));
            });
        });
    }

    private static String path(String league) {
        for (String[] l : Scores.LEAGUES) if (l[0].equals(league)) return l[1];
        return "";
    }

    // The body of a GET, or null if anything went wrong.
    private static String get(String url) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(TIMEOUT_MS);
            c.setReadTimeout(TIMEOUT_MS);
            if (c.getResponseCode() != 200) return null;
            try (InputStream in = c.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[16 << 10];
                for (int n; (n = in.read(buf)) > 0; ) {
                    out.write(buf, 0, n);
                    if (out.size() > MAX_BYTES) return null;
                }
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }
}
