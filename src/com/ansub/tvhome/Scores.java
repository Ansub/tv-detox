package com.ansub.tvhome;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

// Reads ESPN's free public score feeds (scoreboards and team schedules) for the lock screen widgets.
// The feed is unofficial and can change, so every field is optional: a game missing what it needs is
// skipped, and bad input gives an empty list, never an exception. No Android here: tested on a computer.
final class Scores {
    private Scores() {}

    static final String BASE = "https://site.api.espn.com/apis/site/v2/sports/";
    // League label, feed path. Also the order live games are shown in.
    static final String[][] LEAGUES = {{"NFL", "football/nfl"}, {"NBA", "basketball/nba"}, {"College FB", "football/college-football"}};
    // League, ESPN team id, label: the "Your teams" widget (Miami's three; change to taste).
    static final String[][] TEAMS = {{"NFL", "15", "Dolphins"}, {"NBA", "14", "Heat"}, {"College FB", "2390", "Hurricanes"}};

    static final long LIVE_REFRESH_MS = 60_000, IDLE_REFRESH_MS = 900_000;
    private static final long RESULT_SHOWN_MS = 36 * 3_600_000L, WEEK_VIEW_MS = 6 * 86_400_000L;
    private static final String NO_COLOR = "#888888";

    static final class Team {
        final String id, abbr, shortName, color, rank;
        final int score;

        Team(String id, String abbr, String shortName, String color, String rank, int score) {
            this.id = id;
            this.abbr = abbr;
            this.shortName = shortName;
            this.color = color;
            this.rank = rank;
            this.score = score;
        }
    }

    static final class Game {
        final String league, id, state, status; // state: "pre", "in" or "post"
        final Team away, home;
        final long startMs;
        final int period;
        final boolean timeValid; // false: the date is set but the time is TBD (ESPN puts a placeholder)
        final boolean played; // false for a postponed or cancelled game, which ESPN marks "post" too

        Game(String league, String id, String state, String status, Team away, Team home, long startMs, int period) {
            this(league, id, state, status, away, home, startMs, period, true, true);
        }

        Game(String league, String id, String state, String status, Team away, Team home, long startMs, int period,
             boolean timeValid, boolean played) {
            this.timeValid = timeValid;
            this.played = played;
            this.league = league;
            this.id = id;
            this.state = state;
            this.status = status;
            this.away = away;
            this.home = home;
            this.startMs = startMs;
            this.period = period;
        }

        boolean has(String teamId) { return away.id.equals(teamId) || home.id.equals(teamId); }
    }

    static List<Game> scoreboard(String json, String league) { return events(json, league); }

    // Team schedules have the same events, with scores as objects instead of strings.
    static List<Game> schedule(String json, String league) { return events(json, league); }

    private static List<Game> events(String json, String league) {
        List<Game> out = new ArrayList<>();
        if (json == null) return out;
        try {
            JSONArray events = new JSONObject(json).optJSONArray("events");
            if (events == null) return out;
            for (int i = 0; i < events.length(); i++) {
                JSONObject e = events.optJSONObject(i);
                Game g = e == null ? null : game(e, league);
                if (g != null) out.add(g);
            }
        } catch (Exception ignored) {
            // Not JSON, or not this shape: no games.
        }
        return out;
    }

    private static Game game(JSONObject e, String league) {
        JSONArray comps = e.optJSONArray("competitions");
        JSONObject comp = comps == null ? null : comps.optJSONObject(0);
        JSONArray sides = comp == null ? null : comp.optJSONArray("competitors");
        if (sides == null) return null;
        Team home = null, away = null;
        for (int i = 0; i < sides.length(); i++) {
            JSONObject c = sides.optJSONObject(i);
            if (c == null) continue;
            if ("home".equals(c.optString("homeAway"))) home = team(c);
            else if ("away".equals(c.optString("homeAway"))) away = team(c);
        }
        if (home == null || away == null) return null;
        JSONObject status = e.optJSONObject("status");
        if (status == null) status = comp.optJSONObject("status");
        if (status == null) status = new JSONObject();
        JSONObject type = status.optJSONObject("type");
        String state = type == null ? "pre" : type.optString("state", "pre");
        int period = status.optInt("period", 0);
        String text;
        if ("in".equals(state)) {
            boolean half = type != null && ("STATUS_HALFTIME".equals(type.optString("name"))
                    || "Halftime".equalsIgnoreCase(type.optString("shortDetail")));
            text = half ? "Half" : periodLabel(period) + " · " + status.optString("displayClock", "");
        } else {
            text = "post".equals(state) ? "Final" : "";
        }
        String date = e.optString("date", comp.optString("date", ""));
        boolean timeValid = comp.optBoolean("timeValid", e.optBoolean("timeValid", true));
        String name = type == null ? "" : type.optString("name", "");
        boolean played = !"post".equals(state) || (type.optBoolean("completed", true)
                && !name.equals("STATUS_POSTPONED") && !name.equals("STATUS_CANCELED") && !name.equals("STATUS_SUSPENDED"));
        if (!played) text = "";
        return new Game(league, e.optString("id", ""), state, text, away, home, parseDate(date), period, timeValid, played);
    }

    private static Team team(JSONObject c) {
        JSONObject t = c.optJSONObject("team");
        if (t == null) t = new JSONObject();
        String abbr = text(t, "abbreviation");
        String shortName = text(t, "shortDisplayName");
        if (shortName.isEmpty()) shortName = abbr.isEmpty() ? text(t, "displayName") : abbr;
        String color = text(t, "color").toLowerCase(Locale.US);
        color = color.matches("[0-9a-f]{6}") ? "#" + color : NO_COLOR;
        JSONObject curated = c.optJSONObject("curatedRank");
        int r = curated == null ? 0 : curated.optInt("current", 0);
        return new Team(text(t, "id"), abbr, shortName, color, r >= 1 && r <= 25 ? String.valueOf(r) : "", score(c.opt("score")));
    }

    // org.json turns a JSON null into the string "null"; treat it as missing.
    private static String text(JSONObject o, String key) {
        return o.isNull(key) ? "" : o.optString(key, "");
    }

    private static int score(Object s) {
        try {
            if (s instanceof JSONObject) return (int) Math.round(((JSONObject) s).optDouble("value", 0));
            if (s instanceof Number) return ((Number) s).intValue();
            if (s instanceof String) return (int) Math.round(Double.parseDouble((String) s));
        } catch (Exception ignored) {}
        return 0;
    }

    private static String periodLabel(int period) {
        if (period <= 4) return "Q" + Math.max(1, period);
        return period == 5 ? "OT" : (period - 4) + "OT";
    }

    private static long parseDate(String iso) {
        for (String pattern : new String[] {"yyyy-MM-dd'T'HH:mmX", "yyyy-MM-dd'T'HH:mm:ssX"}) {
            SimpleDateFormat f = new SimpleDateFormat(pattern, Locale.US);
            f.setLenient(false);
            try { return f.parse(iso).getTime(); } catch (ParseException ignored) {}
        }
        return 0;
    }

    // Games being played now: NFL first, then NBA, then college football, each by start time.
    static List<Game> live(List<Game> all) {
        List<Game> out = new ArrayList<>();
        for (Game g : all) if ("in".equals(g.state)) out.add(g);
        Collections.sort(out, (a, b) -> a.league.equals(b.league)
                ? Long.compare(a.startMs, b.startMs) : Integer.compare(leagueOrder(a.league), leagueOrder(b.league)));
        return out;
    }

    private static int leagueOrder(String league) {
        for (int i = 0; i < LEAGUES.length; i++) if (LEAGUES[i][0].equals(league)) return i;
        return LEAGUES.length;
    }

    // Games still to start today (before local midnight), soonest first.
    static List<Game> upcomingToday(List<Game> all, long nowMs, TimeZone tz) {
        Calendar midnight = Calendar.getInstance(tz);
        midnight.setTimeInMillis(nowMs);
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);
        midnight.add(Calendar.DAY_OF_MONTH, 1);
        List<Game> out = new ArrayList<>();
        for (Game g : all)
            if ("pre".equals(g.state) && g.timeValid && g.startMs > nowMs && g.startMs < midnight.getTimeInMillis()) out.add(g);
        Collections.sort(out, (a, b) -> Long.compare(a.startMs, b.startMs));
        return out;
    }

    // One line for "Your teams": live score, a recent result, the next game, or "Offseason".
    // Team ids are only unique within a league (NFL 15 is the Dolphins, NBA 15 the Bucks).
    static String teamLine(List<Game> schedule, String league, String teamId, long nowMs, TimeZone tz) {
        Game last = null, next = null;
        for (Game g : schedule) {
            if (!g.league.equals(league) || !g.has(teamId)) continue;
            if ("in".equals(g.state)) {
                Team us = side(g, teamId, true), them = side(g, teamId, false);
                String when = "Half".equals(g.status) ? "Half" : periodLabel(g.period);
                return "LIVE " + us.score + "–" + them.score + " " + when;
            }
            if ("post".equals(g.state) && g.played && g.startMs <= nowMs && (last == null || g.startMs > last.startMs)) last = g;
            if ("pre".equals(g.state) && g.startMs > nowMs && (next == null || g.startMs < next.startMs)) next = g;
        }
        if (last != null && nowMs - last.startMs <= RESULT_SHOWN_MS) {
            Team us = side(last, teamId, true), them = side(last, teamId, false);
            String wlt = us.score > them.score ? "W" : us.score < them.score ? "L" : "T";
            return wlt + " " + us.score + "–" + them.score;
        }
        if (next != null) {
            boolean soon = next.startMs - nowMs < WEEK_VIEW_MS;
            // A TBD kick-off has a placeholder time: show the day only.
            SimpleDateFormat f = new SimpleDateFormat(!soon ? "MMM d" : next.timeValid ? "EEE h:mm" : "EEE", Locale.US);
            f.setTimeZone(tz);
            String when = f.format(next.startMs) + (soon && !next.timeValid ? " TBD" : "");
            boolean away = next.away.id.equals(teamId);
            return when + (away ? " @ " : " vs ") + side(next, teamId, false).abbr;
        }
        return "Offseason";
    }

    private static Team side(Game g, String teamId, boolean ours) {
        boolean homeIsUs = g.home.id.equals(teamId);
        return ours == homeIsUs ? g.home : g.away;
    }

    // A team's schedule after a fetch: a failed download (null) or a body with no games keeps what was
    // there, so one hiccup doesn't turn a team into "Offseason" for 15 minutes.
    static List<Game> keepOrReplace(List<Game> previous, String json, String league) {
        if (json == null) return previous;
        List<Game> fresh = schedule(json, league);
        return fresh.isEmpty() && !previous.isEmpty() ? previous : fresh;
    }

    // How long to wait before fetching again: every minute while anything is live, or after a failed
    // try (the projector may have woken before its Wi-Fi), otherwise every 15 minutes.
    static long nextDelayMs(boolean anyLive, boolean lastFailed) {
        return anyLive || lastFailed ? LIVE_REFRESH_MS : IDLE_REFRESH_MS;
    }
}
