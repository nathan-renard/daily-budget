package com.aimbire.dailybudget;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Java copy of assets/www/budget.js, used by the morning notification and the widget.
 * The web UI pushes a snapshot of its data through the bridge (syncData), because
 * neither can read the WebView's localStorage without opening the app.
 *
 * Keep {@link #compute} in step with Budget.compute in budget.js.
 */
final class Budget {

    // The snapshot predates the widget, so it lives in the reminder's prefs file.
    private static final String PREFS = "reminder";

    private Budget() {}

    /** The synced snapshot: settings plus this and last month's expenses. */
    static final class Data {
        double budget;
        String currency = "$";
        String since;                 // first tracked day (yyyy-MM-dd), or null
        String theme = "dark";        // dark | amoled | light | system
        String widgetColors = "app";  // app | wallpaper
        final List<String> days = new ArrayList<>();
        final List<Double> amounts = new ArrayList<>();
    }

    /** One day's numbers, as Budget.compute returns them in budget.js. */
    static final class Day {
        int daysLeft;
        double spentToday;
        double spentMonth;
        double dailyAllowance;
        double tomorrowAllowance;
        double todayLeft;
        double monthRemaining;
    }

    static void save(Context context, String json) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("data", json).apply();
    }

    static Data load(Context context) {
        Data data = new Data();
        try {
            JSONObject json = new JSONObject(
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("data", "{}"));
            data.budget = json.optDouble("budget", 0);
            data.currency = json.optString("currency", "$");
            data.since = json.isNull("since") ? null : json.optString("since", null);
            data.theme = json.optString("theme", "dark");
            data.widgetColors = json.optString("widgetColors", "app");
            JSONArray expenses = json.optJSONArray("expenses");
            for (int i = 0; expenses != null && i < expenses.length(); i++) {
                JSONObject e = expenses.getJSONObject(i);
                data.days.add(e.getString("day"));
                data.amounts.add(e.getDouble("amount"));
            }
        } catch (Exception ignored) {
            // A bad snapshot behaves like no budget; the next sync replaces it.
        }
        return data;
    }

    /**
     * The daily allowance is fixed at the start of each day:
     *   (budget − spent before today) / days left including today
     */
    static Day compute(Data data, LocalDate date) {
        String today = date.toString();             // yyyy-MM-dd, same as budget.js dayKey
        String month = today.substring(0, 7);       // yyyy-MM
        double spentBefore = 0;
        double spentToday = 0;
        for (int i = 0; i < data.days.size(); i++) {
            String day = data.days.get(i);
            if (!day.startsWith(month)) continue;
            if (day.equals(today)) spentToday += data.amounts.get(i);
            else if (day.compareTo(today) < 0) spentBefore += data.amounts.get(i);
        }

        Day d = new Day();
        d.daysLeft = date.lengthOfMonth() - date.getDayOfMonth() + 1;
        d.spentToday = spentToday;
        d.spentMonth = spentBefore + spentToday;
        d.dailyAllowance = Math.max(0, data.budget - spentBefore) / d.daysLeft;
        // On the last day of the month tomorrow starts a fresh month.
        d.tomorrowAllowance = d.daysLeft > 1
                ? Math.max(0, data.budget - d.spentMonth) / (d.daysLeft - 1)
                : data.budget / date.plusMonths(1).lengthOfMonth();
        d.todayLeft = d.dailyAllowance - spentToday;
        d.monthRemaining = data.budget - d.spentMonth;
        return d;
    }

    /** Same style as the web UI: no decimals for whole amounts, otherwise two. */
    static String money(String currency, double value) {
        double rounded = Math.round(Math.abs(value) * 100) / 100.0;
        NumberFormat nf = NumberFormat.getNumberInstance();
        boolean whole = rounded == Math.floor(rounded);
        nf.setMinimumFractionDigits(whole ? 0 : 2);
        nf.setMaximumFractionDigits(2);
        return (value < 0 && rounded > 0 ? "-" : "") + currency + nf.format(rounded);
    }
}
