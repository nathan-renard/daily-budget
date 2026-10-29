package com.aimbire.dailybudget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.SizeF;
import android.view.View;
import android.widget.RemoteViews;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Home-screen widget (Pro). One widget with four layouts that it switches between as it's
 * resized: just the number (2×1), a ring (2×2), number + quick add (4×1) and the week at a
 * glance (4×2). Without Pro it shows a locked placeholder that opens the Pro dialog.
 *
 * It reads the same synced data as the morning notification, and refreshes when the web UI
 * syncs, when Pro changes, and just after midnight.
 */
public class BudgetWidget extends AppWidgetProvider {

    private static final String ACTION_REFRESH = "com.aimbire.dailybudget.WIDGET_REFRESH";

    // Layouts, smallest first. The launcher picks the largest one that fits (Android 12+);
    // older versions pick by the size the launcher reports (see kindFor).
    static final int SMALL = 0, RING = 1, WIDE = 2, LARGE = 3;
    private static final SizeF[] SIZES = {
            new SizeF(40f, 40f), new SizeF(110f, 120f), new SizeF(240f, 56f), new SizeF(240f, 150f),
    };

    private static final int[] DAY_DOT = {R.id.day0_dot, R.id.day1_dot, R.id.day2_dot, R.id.day3_dot,
            R.id.day4_dot, R.id.day5_dot, R.id.day6_dot};
    private static final int[] DAY_NUM = {R.id.day0_num, R.id.day1_num, R.id.day2_num, R.id.day3_num,
            R.id.day4_num, R.id.day5_num, R.id.day6_num};
    private static final int[] DAY_NAME = {R.id.day0_name, R.id.day1_name, R.id.day2_name, R.id.day3_name,
            R.id.day4_name, R.id.day5_name, R.id.day6_name};

    // ---- Updates --------------------------------------------------------------

    /** Redraws every placed widget. Safe to call from any thread. */
    static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, BudgetWidget.class));
        for (int id : ids) update(context, manager, id);
        scheduleRefresh(context, ids.length > 0);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) update(context, manager, id);
        scheduleRefresh(context, true);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int id, Bundle options) {
        update(context, manager, id); // resized: below Android 12 we pick the layout ourselves
    }

    @Override
    public void onDisabled(Context context) {
        scheduleRefresh(context, false);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_REFRESH.equals(intent.getAction())) updateAll(context);
        else super.onReceive(context, intent);
    }

    private static void update(Context context, AppWidgetManager manager, int id) {
        Content content = new Content(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Map<SizeF, RemoteViews> views = new HashMap<>();
            for (int kind = SMALL; kind <= LARGE; kind++) views.put(SIZES[kind], build(context, content, kind));
            manager.updateAppWidget(id, new RemoteViews(views));
        } else {
            manager.updateAppWidget(id, build(context, content, kindFor(manager.getAppWidgetOptions(id))));
        }
    }

    /** Below Android 12: the portrait size is min width × max height. */
    private static int kindFor(Bundle options) {
        float w = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        float h = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
        if (w == 0) return WIDE; // size not reported yet; WIDE is the default 4×1
        int best = SMALL;
        for (int kind = SMALL; kind <= LARGE; kind++) {
            SizeF s = SIZES[kind];
            if (s.getWidth() <= w && s.getHeight() <= h
                    && s.getWidth() * s.getHeight() > SIZES[best].getWidth() * SIZES[best].getHeight()) {
                best = kind;
            }
        }
        return best;
    }

    /** Midnight changes today's number, so redraw just after it. Not a wakeup alarm:
     *  if the screen is off it fires when the phone next wakes, before anyone looks. */
    private static void scheduleRefresh(Context context, boolean on) {
        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        Intent intent = new Intent(context, BudgetWidget.class).setAction(ACTION_REFRESH);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (!on) {
            alarms.cancel(pi);
            return;
        }
        long next = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault())
                .plusMinutes(1).toInstant().toEpochMilli();
        // set() lets Android deliver it up to an hour late. 10 minutes is the shortest window
        // Android 12+ allows without an exact alarm.
        alarms.setWindow(AlarmManager.RTC, next, 10 * 60 * 1000L, pi);
    }

    // ---- What to show -----------------------------------------------------------

    /** Everything the layouts show, worked out once per update. Mirrors renderBudget in app.js. */
    private static final class Content {
        final Budget.Data data;
        final Budget.Day day;
        final LocalDate today = LocalDate.now();
        final Look look;
        final boolean pro;

        String label;       // "You can spend today", "Over today's budget by", ...
        String shortLabel;  // the same for the small layouts: "left today", "over today", ...
        String amount;
        boolean over;
        String pill;        // tomorrow's allowance, or null to hide it
        int pillRole;
        int spentLevel;     // today's allowance used, 0–10000 (bar)
        int leftLevel;      // today's allowance left, 0–10000 (ring)

        Content(Context context) {
            data = Budget.load(context);
            day = Budget.compute(data, today);
            look = Look.of(context, data);
            pro = Pro.isPro(context);

            Budget.Day d = day;
            String cur = data.currency;
            if (data.budget <= 0) {
                label = context.getString(R.string.no_budget);
                shortLabel = context.getString(R.string.no_budget_short);
                amount = Budget.money(cur, 0);
            } else if (d.monthRemaining < 0) {
                label = context.getString(R.string.over_month);
                shortLabel = context.getString(R.string.over_month_short);
                amount = Budget.money(cur, -d.monthRemaining);
                over = true;
            } else if (d.todayLeft < -0.005) {
                label = context.getString(R.string.over_today);
                shortLabel = context.getString(R.string.over_today_short);
                amount = Budget.money(cur, -d.todayLeft);
                over = true;
            } else if (d.todayLeft < 0.005) {
                label = context.getString(R.string.used_up_today);
                shortLabel = context.getString(R.string.used_up_today_short);
                amount = Budget.money(cur, 0);
            } else {
                label = context.getString(R.string.can_spend_today);
                shortLabel = context.getString(R.string.can_spend_today_short);
                amount = Budget.money(cur, d.todayLeft);
            }

            if (data.budget > 0 && d.monthRemaining >= 0) {
                if (d.monthRemaining <= 0.005 && d.daysLeft > 1) {
                    pill = context.getString(R.string.allowance_ended);
                    pillRole = Look.OVER;
                } else if (d.daysLeft == 1) {
                    pill = context.getString(R.string.next_month_allowance, Budget.money(cur, d.tomorrowAllowance));
                    pillRole = Look.MUTED;
                } else {
                    double diff = d.tomorrowAllowance - d.dailyAllowance;
                    String arrow = diff > 0.005 ? "↑ " : diff < -0.005 ? "↓ " : "";
                    pill = context.getString(R.string.tomorrow_allowance, arrow + Budget.money(cur, d.tomorrowAllowance));
                    pillRole = diff > 0.005 ? Look.ACC : diff < -0.005 ? Look.OVER : Look.MUTED;
                }
            }

            if (over) {
                spentLevel = 10000;
                leftLevel = 10000;
            } else if (d.dailyAllowance > 0) {
                spentLevel = (int) Math.round(Math.min(1, d.spentToday / d.dailyAllowance) * 10000);
                leftLevel = (int) Math.round(Math.max(0, d.todayLeft / d.dailyAllowance) * 10000);
            }
        }

        /**
         * Within budget (0) / over (1) / nothing spent after the month's money ran out (2, not
         * scored) / not tracked (-1), as on the Calendar tab.
         */
        int status(LocalDate date) {
            String key = date.toString();
            if (data.budget <= 0 || data.since == null || key.compareTo(data.since) < 0) return -1;
            Budget.Day d = Budget.compute(data, date);
            if (d.spentToday > d.dailyAllowance + 0.005) return 1;
            return d.monthRemaining + d.spentToday < 0.005 ? 2 : 0;
        }
    }

    // ---- Layouts ----------------------------------------------------------------

    /** One layout with the current data, e.g. for a preview screen. */
    static RemoteViews views(Context context, int kind) {
        return build(context, new Content(context), kind);
    }

    private static RemoteViews build(Context context, Content c, int kind) {
        if (!c.pro) return locked(context, c, kind);
        int layout = kind == SMALL ? R.layout.widget_small
                : kind == RING ? R.layout.widget_ring
                : kind == WIDE ? R.layout.widget_wide
                : R.layout.widget_large;
        RemoteViews rv = new RemoteViews(context.getPackageName(), layout);
        background(rv, c);
        rv.setOnClickPendingIntent(android.R.id.background, open(context, null));

        rv.setTextViewText(R.id.amount, c.amount);
        c.look.text(rv, R.id.amount, c.over ? Look.OVER : Look.TEXT);
        rv.setTextViewText(R.id.label, kind == SMALL ? c.shortLabel : c.label);
        c.look.text(rv, R.id.label, Look.MUTED);

        if (kind == SMALL) return rv;

        rv.setViewVisibility(R.id.pill_box, c.pill == null ? View.GONE : View.VISIBLE);
        if (c.pill != null) {
            rv.setTextViewText(R.id.pill, c.pill);
            c.look.text(rv, R.id.pill, c.pillRole);
            c.look.tint(rv, R.id.pill_bg, Look.PILL);
        }

        if (kind == RING) {
            // Inside the ring there's only room for the short label.
            if (!c.over && c.data.budget > 0 && c.day.todayLeft >= 0.005) {
                rv.setTextViewText(R.id.label,
                        context.getString(R.string.ring_left, Budget.money(c.data.currency, c.day.dailyAllowance)));
            } else {
                rv.setTextViewText(R.id.label, c.shortLabel);
            }
            c.look.tint(rv, R.id.ring_track, Look.LINE);
            c.look.tint(rv, R.id.ring_arc, c.over ? Look.OVER : Look.ACC);
            rv.setInt(R.id.ring_arc, "setImageLevel", c.leftLevel);
            return rv;
        }

        addButton(context, rv, c);
        if (kind == WIDE) {
            c.look.tint(rv, R.id.bar_track, Look.LINE);
            c.look.tint(rv, R.id.bar_fill, c.over ? Look.OVER : Look.ACC);
            rv.setInt(R.id.bar_fill, "setImageLevel", c.spentLevel);
            return rv;
        }

        // LARGE: the last seven days in calendar colours, then the month.
        for (int i = 0; i < 7; i++) {
            LocalDate date = c.today.minusDays(6 - i);
            boolean isToday = i == 6;
            int status = c.status(date);
            rv.setTextViewText(DAY_NUM[i], String.valueOf(date.getDayOfMonth()));
            rv.setTextViewText(DAY_NAME[i], date.getDayOfWeek().getDisplayName(TextStyle.NARROW, Locale.getDefault()));
            c.look.text(rv, DAY_NAME[i], Look.MUTED);
            if (status == 1 || (status == 0 && !isToday)) {
                rv.setImageViewResource(DAY_DOT[i], R.drawable.widget_dot);
                rv.setViewVisibility(DAY_DOT[i], View.VISIBLE);
                c.look.tint(rv, DAY_DOT[i], status == 1 ? Look.OVER : Look.OK);
                rv.setTextColor(DAY_NUM[i], Color.WHITE);
            } else if (status == 2 && !isToday) {
                rv.setImageViewResource(DAY_DOT[i], R.drawable.widget_dot_dashed);
                rv.setViewVisibility(DAY_DOT[i], View.VISIBLE);
                c.look.tint(rv, DAY_DOT[i], Look.MUTED);
                c.look.text(rv, DAY_NUM[i], Look.MUTED);
            } else if (isToday) {
                rv.setImageViewResource(DAY_DOT[i], R.drawable.widget_dot_outline);
                rv.setViewVisibility(DAY_DOT[i], View.VISIBLE);
                c.look.tint(rv, DAY_DOT[i], Look.ACC);
                c.look.text(rv, DAY_NUM[i], Look.TEXT);
            } else {
                rv.setViewVisibility(DAY_DOT[i], View.INVISIBLE); // before tracking started
                c.look.text(rv, DAY_NUM[i], Look.MUTED);
            }
        }

        Budget.Day d = c.day;
        String cur = c.data.currency;
        rv.setTextViewText(R.id.foot_left, d.monthRemaining >= 0
                ? context.getString(R.string.month_left, Budget.money(cur, d.monthRemaining))
                : context.getString(R.string.month_over, Budget.money(cur, -d.monthRemaining)));
        int ok = 0;
        int judged = 0;
        for (int day = 1; day < c.today.getDayOfMonth(); day++) {
            int status = c.status(c.today.withDayOfMonth(day));
            if (status == 0 || status == 1) judged++;
            if (status == 0) ok++;
        }
        String days = context.getResources().getQuantityString(R.plurals.days_left, d.daysLeft, d.daysLeft);
        rv.setTextViewText(R.id.foot_right, judged > 0
                ? days + " · " + context.getString(R.string.days_on_budget, ok, judged)
                : days);
        c.look.text(rv, R.id.foot_left, Look.MUTED);
        c.look.text(rv, R.id.foot_right, Look.MUTED);
        return rv;
    }

    /** Free version: a placeholder whose tap opens the Pro dialog. */
    private static RemoteViews locked(Context context, Content c, int kind) {
        boolean small = kind == SMALL || kind == RING;
        RemoteViews rv = new RemoteViews(context.getPackageName(),
                small ? R.layout.widget_locked_small : R.layout.widget_locked);
        background(rv, c);
        rv.setOnClickPendingIntent(android.R.id.background, open(context, "pro"));
        rv.setTextViewText(R.id.amount, c.data.currency + "‒‒.‒‒");
        c.look.text(rv, R.id.amount, Look.MUTED);
        c.look.tint(rv, R.id.unlock_bg, Look.BTN);
        c.look.tint(rv, R.id.unlock_icon, Look.BTN_TEXT);
        rv.setTextViewText(R.id.unlock_text, context.getString(kind == SMALL ? R.string.unlock : R.string.unlock_widget));
        c.look.text(rv, R.id.unlock_text, Look.BTN_TEXT);
        if (!small) {
            // Set here, not only in the layout: the launcher inflates layouts in the phone's
            // language, which on Android 13+ can differ from the one chosen for the app.
            rv.setTextViewText(R.id.label, context.getString(R.string.can_spend_today));
            c.look.text(rv, R.id.label, Look.MUTED);
            c.look.tint(rv, R.id.bar_track, Look.LINE);
        }
        return rv;
    }

    private static void background(RemoteViews rv, Content c) {
        c.look.tint(rv, R.id.bg, Look.BG);
        rv.setViewVisibility(R.id.border, c.look.border ? View.VISIBLE : View.GONE);
    }

    private static void addButton(Context context, RemoteViews rv, Content c) {
        c.look.tint(rv, R.id.add_bg, Look.BTN);
        c.look.tint(rv, R.id.add_icon, Look.BTN_TEXT);
        rv.setContentDescription(R.id.add, context.getString(R.string.add_expense)); // see locked()
        rv.setOnClickPendingIntent(R.id.add, open(context, "add"));
    }

    /** Opens the app, optionally straight into a dialog ("add" or "pro"). */
    private static PendingIntent open(Context context, String action) {
        Intent intent = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(MainActivity.EXTRA_ACTION, action);
        // Distinct request codes, or the intents (which differ only in extras) would be merged.
        int code = action == null ? 0 : "add".equals(action) ? 1 : 2;
        return PendingIntent.getActivity(context, code, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    // ---- Colours ----------------------------------------------------------------

    /** The widget's colours by role, following the app's theme setting. */
    private static final class Look {
        static final int BG = 0, TEXT = 1, MUTED = 2, LINE = 3, ACC = 4, BTN = 5, BTN_TEXT = 6,
                OVER = 7, OK = 8, PILL = 9;

        // Same values as the themes in style.css.
        private static final int[] DARK = {0xff1b1b1b, 0xfff2f2f2, 0xff8a8a8a, 0xff2e2e2e, 0xff4caf7d,
                0xff004225, 0xffffffff, 0xffe5534b, 0xff0b6b3f, 0xff262626};
        private static final int[] AMOLED = {0xff000000, 0xfff2f2f2, 0xff7d7d7d, 0xff222222, 0xff4caf7d,
                0xff004225, 0xffffffff, 0xffe5534b, 0xff0b6b3f, 0xff141414};
        private static final int[] LIGHT = {0xffffffff, 0xff161616, 0xff6b6b68, 0xffe4e4df, 0xff0b6b3f,
                0xff004225, 0xffffffff, 0xffc9403a, 0xff0b6b3f, 0xfff1f1ee};
        // Material You: colour resources the launcher resolves, so they follow the wallpaper
        // and dark mode (values-v31 and values-night-v31).
        private static final int[] YOU = {R.color.you_bg, R.color.you_text, R.color.you_muted,
                R.color.you_line, R.color.you_acc, R.color.you_btn, R.color.you_btn_text,
                R.color.you_over, R.color.you_ok, R.color.you_pill};

        final int[] colors;     // the colours, or the light ones when following the phone
        final int[] night;      // dark colours when following the phone (Android 12+), else null
        final boolean wallpaper;
        final boolean border;   // AMOLED: a hairline so a black widget doesn't vanish on black

        private Look(int[] colors, int[] night, boolean wallpaper, boolean border) {
            this.colors = colors;
            this.night = night;
            this.wallpaper = wallpaper;
            this.border = border;
        }

        static Look of(Context context, Budget.Data data) {
            boolean v31 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
            if (v31 && "wallpaper".equals(data.widgetColors)) return new Look(null, null, true, false);
            switch (data.theme) {
                case "light": return new Look(LIGHT, null, false, false);
                case "amoled": return new Look(AMOLED, null, false, true);
                case "system":
                    // Android 12+ switches between the two itself when dark mode changes;
                    // older versions get whichever is current at the last update.
                    if (v31) return new Look(LIGHT, DARK, false, false);
                    int mode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                    return new Look(mode == Configuration.UI_MODE_NIGHT_YES ? DARK : LIGHT, null, false, false);
                default: return new Look(DARK, null, false, false);
            }
        }

        void text(RemoteViews rv, int id, int role) {
            set(rv, id, "setTextColor", role);
        }

        void tint(RemoteViews rv, int id, int role) {
            set(rv, id, "setColorFilter", role);
        }

        private void set(RemoteViews rv, int id, String method, int role) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && wallpaper) {
                rv.setColor(id, method, YOU[role]);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && night != null) {
                rv.setColorInt(id, method, colors[role], night[role]);
            } else {
                rv.setInt(id, method, colors[role]);
            }
        }
    }
}
