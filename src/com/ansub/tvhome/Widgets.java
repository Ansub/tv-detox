package com.ansub.tvhome;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// The two small glass widgets under the lock screen clock, like iPhone lock screen widgets:
// "On now" (every live NBA, NFL and college football game, one at a time) and "Your teams"
// (Dolphins, Heat, Hurricanes). Laid out at 1920x1080 and scaled.
final class Widgets {
    static final long CYCLE_MS = 8_000;
    // "Your teams" is wider so a line like "Sat 7:30 @ FSU" fits next to "Hurricanes".
    // Dark glass with a faint edge: readable on bright, busy photos too (a city skyline at dusk).
    private static final int CARD_W = 336, TEAMS_W = 400, GAP = 19, RADIUS = 27, FILL = 0x66101014, EDGE = 0x24FFFFFF;
    private static final int LIVE_RED = 0xFFFF453A;
    // The "Your teams" teams' own colours, in Scores.TEAMS order.
    private static final int[] TEAM_COLORS = {0xFF008E97, 0xFF98002E, 0xFFF47321};

    private final Context context;
    private final float scale;
    private final Typeface medium, semibold, bold;
    private final LinearLayout row;
    private final LinearLayout onNow, teams;
    private final TextView onNowHead, onNowTag, onNowFoot;
    private final TextView[] gameNames = new TextView[2], gameScores = new TextView[2];
    private final View[] gameDots = new View[2];
    private final TextView[] teamLines = new TextView[3];
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Scores.Game> cycle = new ArrayList<>();
    private boolean cycleLive;
    private int shown;
    private boolean paused = true;
    private final Runnable next = new Runnable() {
        @Override public void run() {
            if (cycle.isEmpty()) return;
            shown = (shown + 1) % cycle.size();
            showGame();
            if (cycle.size() > 1 && !paused) main.postDelayed(this, CYCLE_MS);
        }
    };

    Widgets(Context c, float scale, Typeface medium, Typeface semibold, Typeface bold) {
        context = c;
        this.scale = scale;
        this.medium = medium;
        this.semibold = semibold;
        this.bold = bold;
        row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);

        onNow = card();
        LinearLayout head = new LinearLayout(c);
        onNowHead = header("");
        onNowTag = header("● LIVE");
        onNowTag.setTextColor(LIVE_RED);
        onNowTag.setAlpha(1f);
        head.addView(onNowHead, new LinearLayout.LayoutParams(0, -2, 1f));
        head.addView(onNowTag, new LinearLayout.LayoutParams(-2, -2));
        onNow.addView(head, rowParams(0));
        for (int i = 0; i < 2; i++) {
            LinearLayout r = new LinearLayout(c);
            r.setGravity(Gravity.CENTER_VERTICAL);
            gameDots[i] = dot(Color.GRAY);
            r.addView(gameDots[i], new LinearLayout.LayoutParams(px(20), px(20)));
            gameNames[i] = text(22, semibold, Color.WHITE);
            LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, -2, 1f);
            np.leftMargin = px(11);
            r.addView(gameNames[i], np);
            gameScores[i] = text(22, bold, Color.WHITE);
            r.addView(gameScores[i], new LinearLayout.LayoutParams(-2, -2));
            onNow.addView(r, rowParams(i == 0 ? 9 : 6));
        }
        onNowFoot = text(16, medium, 0xBFFFFFFF);
        onNow.addView(onNowFoot, rowParams(6));
        row.addView(onNow, new LinearLayout.LayoutParams(px(CARD_W), -2));

        teams = card();
        teams.addView(header(Strings.t("Your teams")), rowParams(0));
        for (int i = 0; i < Scores.TEAMS.length; i++) {
            LinearLayout r = new LinearLayout(c);
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.addView(dot(TEAM_COLORS[i]), new LinearLayout.LayoutParams(px(20), px(20)));
            TextView name = text(20, semibold, Color.WHITE);
            name.setText(Scores.TEAMS[i][2]);
            LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-2, -2);
            np.leftMargin = px(11);
            r.addView(name, np);
            teamLines[i] = text(20, semibold, 0xD9FFFFFF);
            teamLines[i].setGravity(Gravity.END);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
            lp.leftMargin = px(12);
            r.addView(teamLines[i], lp);
            teams.addView(r, rowParams(i == 0 ? 9 : 7));
        }
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(px(TEAMS_W), -2);
        tp.leftMargin = px(GAP);
        row.addView(teams, tp);
        hide();
    }

    View view() { return row; }

    void update(List<Scores.Game> live, List<Scores.Game> upcoming, List<String> lines) {
        main.removeCallbacks(next);
        cycle.clear();
        cycleLive = !live.isEmpty();
        cycle.addAll(cycleLive ? live : upcoming);
        if (shown >= cycle.size()) shown = 0;
        onNow.setVisibility(cycle.isEmpty() ? View.GONE : View.VISIBLE);
        if (!cycle.isEmpty()) showGame();
        boolean anyTeam = false;
        for (int i = 0; i < teamLines.length; i++) {
            String line = i < lines.size() ? lines.get(i) : "";
            anyTeam |= !line.isEmpty();
            teamLines[i].setText(line);
            teamLines[i].setTextColor(line.startsWith("LIVE") ? LIVE_RED : 0xD9FFFFFF);
        }
        teams.setVisibility(anyTeam ? View.VISIBLE : View.GONE);
        row.setVisibility(cycle.isEmpty() && !anyTeam ? View.GONE : View.VISIBLE);
        if (cycle.size() > 1 && !paused) main.postDelayed(next, CYCLE_MS);
    }

    void hide() {
        main.removeCallbacks(next);
        cycle.clear();
        row.setVisibility(View.GONE);
    }

    void pause() {
        paused = true;
        main.removeCallbacks(next);
    }

    void resume() {
        paused = false;
        main.removeCallbacks(next);
        if (cycle.size() > 1) main.postDelayed(next, CYCLE_MS);
    }

    private void showGame() {
        Scores.Game g = cycle.get(shown);
        onNowHead.setText(((cycleLive ? Strings.t("On now") : Strings.t("Tonight")) + " · " + g.league).toUpperCase(Locale.getDefault()));
        onNowTag.setVisibility(cycleLive ? View.VISIBLE : View.GONE);
        Scores.Team[] sides = {g.away, g.home};
        for (int i = 0; i < 2; i++) {
            Scores.Team t = sides[i];
            gameNames[i].setText(t.rank.isEmpty() ? t.shortName : "#" + t.rank + " " + t.shortName);
            gameScores[i].setText(cycleLive ? String.valueOf(t.score) : "");
            ((GradientDrawable) gameDots[i].getBackground()).setColor(parse(t.color));
        }
        String when = cycleLive ? g.status : DateFormat.getTimeFormat(context).format(g.startMs);
        onNowFoot.setText(cycle.size() > 1 ? when + " · " + (shown + 1) + " of " + cycle.size() : when);
    }

    private static int parse(String color) {
        try { return Color.parseColor(color); } catch (Exception e) { return Color.GRAY; }
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(context);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(FILL);
        bg.setStroke(Math.max(1, px(2)), EDGE);
        bg.setCornerRadius(px(RADIUS));
        c.setBackground(bg);
        c.setPadding(px(23), px(17), px(23), px(17));
        return c;
    }

    private TextView header(String s) {
        TextView t = text(15, bold, Color.WHITE);
        t.setAlpha(0.7f);
        t.setLetterSpacing(0.08f);
        t.setText(s.toUpperCase(Locale.getDefault()));
        return t;
    }

    private View dot(int color) {
        View d = new View(context);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        d.setBackground(g);
        return d;
    }

    private TextView text(int sizePx, Typeface tf, int color) {
        TextView t = new TextView(context);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(sizePx));
        t.setTextColor(color);
        if (tf != null) t.setTypeface(tf);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setIncludeFontPadding(false);
        return t;
    }

    private LinearLayout.LayoutParams rowParams(int topMarginPx) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = px(topMarginPx);
        return lp;
    }

    private int px(int v) { return Math.round(v * scale); }
}
