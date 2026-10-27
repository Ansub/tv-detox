package com.ansub.tvhome;

import static com.ansub.tvhome.T.check;
import static com.ansub.tvhome.T.eq;
import static com.ansub.tvhome.T.read;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

public final class ScoresTest {
    private static final TimeZone ET = TimeZone.getTimeZone("America/New_York");
    private static final long HOUR = 3_600_000L;

    private static String fixture(String name) { return read("test/fixtures/" + name); }

    // A moment in Miami's time zone.
    private static long et(int y, int month, int d, int h, int min) {
        Calendar c = Calendar.getInstance(ET);
        c.clear();
        c.set(y, month - 1, d, h, min);
        return c.getTimeInMillis();
    }

    private static Scores.Team team(String id, String abbr, int score) {
        return new Scores.Team(id, abbr, abbr, "#888888", "", score);
    }

    private static Scores.Game game(String league, String state, long start, Scores.Team away, Scores.Team home, int period, String status) {
        return new Scores.Game(league, away.abbr + "@" + home.abbr + start, state, status, away, home, start, period);
    }

    public static void testRealFeedsParse() {
        for (String[] f : new String[][] {{"nfl.json", "NFL"}, {"nba.json", "NBA"}, {"cfb.json", "College FB"}}) {
            List<Scores.Game> games = Scores.scoreboard(fixture(f[0]), f[1]);
            check(!games.isEmpty(), f[0] + " has games");
            for (Scores.Game g : games) {
                check(!g.away.abbr.isEmpty() && !g.home.abbr.isEmpty(), f[0] + " abbreviations");
                check(g.startMs > 0, f[0] + " start time");
                eq(f[1], g.league);
            }
        }
    }

    public static void testRealSchedulesParse() {
        check(!Scores.schedule(fixture("dolphins.json"), "NFL").isEmpty(), "Dolphins schedule");
        check(!Scores.schedule(fixture("hurricanes.json"), "College FB").isEmpty(), "Hurricanes schedule");
        // The Heat were playing when this was captured; schedule scores are objects, not strings.
        List<Scores.Game> heat = Scores.schedule(fixture("heat.json"), "NBA");
        String line = Scores.teamLine(heat, "NBA", "14", System.currentTimeMillis(), ET);
        check(line.startsWith("LIVE "), "Heat live line, got " + line);
    }

    public static void testLiveGameFields() {
        List<Scores.Game> games = Scores.scoreboard(fixture("live-game.json"), "NFL");
        eq(1, games.size());
        Scores.Game g = games.get(0);
        eq("NYG", g.away.abbr);
        eq(17, g.away.score);
        eq("#0b2265", g.away.color);
        eq("DAL", g.home.abbr);
        eq(21, g.home.score);
        eq("#041e42", g.home.color);
        eq("in", g.state);
        eq(3, g.period);
        eq("Q3 · 8:42", g.status);
    }

    public static void testUsesShortName() {
        eq("Giants", Scores.scoreboard(fixture("live-game.json"), "NFL").get(0).away.shortName);
    }

    public static void testRankFromCuratedRank() {
        String nd = null, navy = null;
        for (Scores.Game g : Scores.scoreboard(fixture("cfb.json"), "College FB"))
            for (Scores.Team t : new Scores.Team[] {g.away, g.home}) {
                if (t.abbr.equals("ND")) nd = t.rank;
                if (t.abbr.equals("NAVY")) navy = t.rank;
            }
        eq("5", nd);
        eq("", navy);
    }

    public static void testMissingFieldsDefaulted() {
        List<Scores.Game> games = Scores.scoreboard(fixture("missing-fields.json"), "NBA");
        eq(1, games.size());
        Scores.Game g = games.get(0);
        eq("#888888", g.home.color);
        eq(0, g.home.score);
        eq("Aces", g.home.shortName);
        eq("BBB", g.away.shortName); // no short name: falls back to the abbreviation
        eq("pre", g.state);
        check(g.startMs > 0, "start time parsed");
    }

    public static void testGarbageIsEmpty() {
        check(Scores.scoreboard("not json", "NFL").isEmpty(), "garbage");
        check(Scores.scoreboard("", "NBA").isEmpty(), "empty");
        check(Scores.scoreboard(null, "NFL").isEmpty(), "null");
        check(Scores.scoreboard("{\"events\":{}}", "NFL").isEmpty(), "wrong shape");
        check(Scores.schedule("[]", "NFL").isEmpty(), "schedule garbage");
    }

    public static void testLiveFilterAndOrder() {
        long t = et(2026, 10, 26, 19, 0);
        Scores.Game cfb = game("College FB", "in", t, team("1", "ALA", 0), team("2", "UGA", 0), 1, "Q1 · 9:00");
        Scores.Game nba = game("NBA", "in", t, team("3", "BOS", 0), team("4", "MIL", 0), 1, "Q1 · 9:00");
        Scores.Game nfl2 = game("NFL", "in", t + HOUR, team("5", "NYG", 0), team("6", "DAL", 0), 1, "Q1 · 9:00");
        Scores.Game nfl1 = game("NFL", "in", t, team("7", "KC", 0), team("8", "BUF", 0), 1, "Q1 · 9:00");
        Scores.Game done = game("NFL", "post", t, team("9", "SF", 0), team("10", "SEA", 0), 4, "Final");
        Scores.Game later = game("NBA", "pre", t + 2 * HOUR, team("11", "LAL", 0), team("12", "GSW", 0), 0, "");
        eq(Arrays.asList(nfl1, nfl2, nba, cfb), Scores.live(Arrays.asList(cfb, nba, nfl2, done, later, nfl1)));
    }

    public static void testUpcomingTodayOnly() {
        long now = et(2026, 10, 26, 18, 0);
        Scores.Game tonight = game("NFL", "pre", et(2026, 10, 26, 20, 15), team("1", "PHI", 0), team("2", "DAL", 0), 0, "");
        Scores.Game afterMidnight = game("NBA", "pre", et(2026, 10, 27, 0, 30), team("3", "LAL", 0), team("4", "GSW", 0), 0, "");
        Scores.Game started = game("NBA", "in", et(2026, 10, 26, 17, 0), team("5", "BOS", 0), team("6", "MIL", 0), 2, "Q2 · 1:00");
        eq(Collections.singletonList(tonight), Scores.upcomingToday(Arrays.asList(afterMidnight, started, tonight), now, ET));
    }

    public static void testTeamLineLive() {
        long now = et(2026, 10, 26, 20, 30);
        Scores.Game g = game("NBA", "in", now - HOUR, team("14", "MIA", 54), team("19", "ORL", 50), 2, "Q2 · 3:10");
        eq("LIVE 54–50 Q2", Scores.teamLine(Collections.singletonList(g), "NBA", "14", now, ET));
    }

    public static void testTeamLineHalfAndOvertime() {
        long now = et(2026, 10, 26, 20, 30);
        Scores.Game half = game("NFL", "in", now - HOUR, team("4", "CIN", 10), team("15", "MIA", 14), 2, "Half");
        eq("LIVE 14–10 Half", Scores.teamLine(Collections.singletonList(half), "NFL", "15", now, ET));
        Scores.Game ot = game("NBA", "in", now - HOUR, team("14", "MIA", 101), team("2", "BOS", 101), 5, "OT · 2:10");
        eq("LIVE 101–101 OT", Scores.teamLine(Collections.singletonList(ot), "NBA", "14", now, ET));
    }

    public static void testTeamLineResult() {
        long now = et(2026, 10, 26, 18, 0);
        Scores.Game win = game("NBA", "post", now - 20 * HOUR, team("16", "MIN", 14), team("14", "MIA", 31), 4, "Final");
        eq("W 31–14", Scores.teamLine(Collections.singletonList(win), "NBA", "14", now, ET));
        Scores.Game loss = game("NFL", "post", now - 20 * HOUR, team("15", "MIA", 20), team("2", "BUF", 24), 4, "Final");
        eq("L 20–24", Scores.teamLine(Collections.singletonList(loss), "NFL", "15", now, ET));
    }

    public static void testTeamLineOldResultShowsNext() {
        long now = et(2026, 10, 26, 12, 0);
        Scores.Game old = game("NFL", "post", now - 37 * HOUR, team("15", "MIA", 20), team("2", "BUF", 24), 4, "Final");
        Scores.Game next = game("NFL", "pre", et(2026, 10, 29, 20, 15), team("15", "MIA", 0), team("20", "NYJ", 0), 0, "");
        eq("Thu 8:15 @ NYJ", Scores.teamLine(Arrays.asList(old, next), "NFL", "15", now, ET));
    }

    public static void testTeamLineNext() {
        long now = et(2026, 10, 26, 12, 0);
        Scores.Game away = game("NFL", "pre", et(2026, 10, 29, 20, 15), team("15", "MIA", 0), team("20", "NYJ", 0), 0, "");
        eq("Thu 8:15 @ NYJ", Scores.teamLine(Collections.singletonList(away), "NFL", "15", now, ET));
        Scores.Game home = game("NFL", "pre", et(2026, 11, 5, 13, 0), team("2", "BUF", 0), team("15", "MIA", 0), 0, "");
        eq("Nov 5 vs BUF", Scores.teamLine(Collections.singletonList(home), "NFL", "15", now, ET));
    }

    public static void testTeamLineOffseason() {
        eq("Offseason", Scores.teamLine(new ArrayList<Scores.Game>(), "NFL", "15", et(2026, 10, 26, 12, 0), ET));
    }

    public static void testNextDelay() {
        eq(60_000L, Scores.nextDelayMs(true, false));
        eq(900_000L, Scores.nextDelayMs(false, false));
    }

    public static void testNextDelayAfterFailure() {
        eq(60_000L, Scores.nextDelayMs(false, true));
    }

    public static void testTeams() {
        eq("15", Scores.TEAMS[0][1]);
        eq("14", Scores.TEAMS[1][1]);
        eq("2390", Scores.TEAMS[2][1]);
    }

    // ESPN team ids repeat across leagues: NFL 15 is the Dolphins, NBA 15 the Bucks.
    public static void testTeamLineIgnoresOtherLeagues() {
        List<Scores.Game> heat = Scores.schedule(fixture("heat.json"), "NBA");
        eq("Offseason", Scores.teamLine(heat, "NFL", "15", et(2026, 11, 18, 19, 30), ET));
        long now = et(2026, 10, 26, 12, 0);
        Scores.Game bucks = game("NBA", "in", now - HOUR, team("15", "MIL", 50), team("14", "MIA", 48), 2, "Q2 · 1:00");
        Scores.Game dolphins = game("NFL", "pre", et(2026, 10, 29, 20, 15), team("15", "MIA", 0), team("20", "NYJ", 0), 0, "");
        eq("Thu 8:15 @ NYJ", Scores.teamLine(Arrays.asList(bucks, dolphins), "NFL", "15", now, ET));
    }

    public static void testTbdKickoff() {
        List<Scores.Game> canes = Scores.schedule(fixture("hurricanes.json"), "College FB");
        eq("Sat TBD vs DUKE", Scores.teamLine(canes, "College FB", "2390", et(2026, 11, 9, 12, 0), ET));
        // More than 6 days out (Duke on the 14th has passed by noon): the date only.
        eq("Nov 21 vs VT", Scores.teamLine(canes, "College FB", "2390", et(2026, 11, 14, 12, 0), ET));
    }

    public static void testTbdNotInTonight() {
        long now = et(2026, 10, 26, 12, 0);
        Scores.Game tbd = new Scores.Game("NFL", "1", "pre", "", team("1", "A", 0), team("2", "B", 0), et(2026, 10, 26, 20, 0), 0, false, true);
        check(Scores.upcomingToday(Collections.singletonList(tbd), now, ET).isEmpty(), "a TBD time isn't a real time");
    }

    public static void testPostponedIsNotAResult() {
        List<Scores.Game> games = Scores.schedule(fixture("postponed.json"), "NFL");
        eq(2, games.size());
        check(!games.get(0).played, "postponed game not played");
        eq("Thu 8:15 @ NYJ", Scores.teamLine(games, "NFL", "15", et(2026, 10, 26, 12, 0), ET));
    }

    public static void testScheduleKeptWhenFetchFails() {
        List<Scores.Game> previous = Scores.schedule(fixture("dolphins.json"), "NFL");
        check(Scores.keepOrReplace(previous, null, "NFL") == previous, "download failed: keep what we had");
        check(Scores.keepOrReplace(previous, "<html>busy</html>", "NFL") == previous, "garbage: keep what we had");
        List<Scores.Game> fresh = Scores.keepOrReplace(new ArrayList<Scores.Game>(), fixture("dolphins.json"), "NFL");
        eq(previous.size(), fresh.size());
    }
}
