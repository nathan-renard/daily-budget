package com.aimbire.dailybudget;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * Morning reminder: stores a copy of the budget data pushed from the web UI,
 * schedules a daily alarm, and builds the "today you can spend" notification.
 *
 * The allowance math mirrors assets/www/budget.js:
 *   (budget − spent before today) / days left in the month (incl. today)
 */
final class Reminder {

    private static final String PREFS = "reminder";
    private static final String CHANNEL_ID = "daily_allowance";
    private static final int NOTIFICATION_ID = 1;

    private Reminder() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---- Settings & data -----------------------------------------------------

    static void saveData(Context context, String json) {
        prefs(context).edit().putString("data", json).apply();
    }

    static void saveSettings(Context context, boolean enabled, int hour, int minute) {
        prefs(context).edit()
                .putBoolean("enabled", enabled)
                .putInt("hour", hour)
                .putInt("minute", minute)
                .apply();
    }

    static boolean isEnabled(Context context) {
        return prefs(context).getBoolean("enabled", false);
    }

    // ---- Scheduling ----------------------------------------------------------

    private static PendingIntent alarmIntent(Context context) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Arms the next alarm if the reminder is on, otherwise cancels it. */
    static void schedule(Context context) {
        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        PendingIntent pi = alarmIntent(context);
        if (!isEnabled(context)) {
            alarms.cancel(pi);
            return;
        }
        SharedPreferences p = prefs(context);
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime next = now.withHour(p.getInt("hour", 8)).withMinute(p.getInt("minute", 0))
                .withSecond(0).withNano(0);
        if (!next.isAfter(now)) next = next.plusDays(1);
        // Inexact but Doze-friendly; needs no exact-alarm permission.
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), pi);
    }

    // ---- Notification --------------------------------------------------------

    static void show(Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (!nm.areNotificationsEnabled()) return;
        String[] message = buildMessage(context);
        if (message == null) return; // no budget set yet

        nm.createNotificationChannel(new NotificationChannel(
                CHANNEL_ID, "Daily allowance", NotificationManager.IMPORTANCE_DEFAULT));

        Intent open = new Intent(context, MainActivity.class);
        PendingIntent tap = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(context.getColor(R.color.accent))
                .setContentTitle(message[0])
                .setContentText(message[1])
                .setContentIntent(tap)
                .setAutoCancel(true)
                .build();
        nm.notify(NOTIFICATION_ID, notification);
    }

    /** Returns {title, text}, or null if no budget has been set. */
    static String[] buildMessage(Context context) {
        try {
            JSONObject data = new JSONObject(prefs(context).getString("data", "{}"));
            double budget = data.optDouble("budget", 0);
            if (budget <= 0) return null;
            String currency = data.optString("currency", "$");

            LocalDate today = LocalDate.now();
            String todayKey = today.toString();             // yyyy-MM-dd, same as budget.js dayKey
            String monthKey = todayKey.substring(0, 7);     // yyyy-MM

            double spentBefore = 0;
            double spentToday = 0;
            JSONArray expenses = data.optJSONArray("expenses");
            for (int i = 0; expenses != null && i < expenses.length(); i++) {
                JSONObject e = expenses.getJSONObject(i);
                String day = e.getString("day");
                if (!day.startsWith(monthKey)) continue;
                double amount = e.getDouble("amount");
                if (day.equals(todayKey)) spentToday += amount;
                else if (day.compareTo(todayKey) < 0) spentBefore += amount;
            }

            int daysLeft = today.lengthOfMonth() - today.getDayOfMonth() + 1;
            double remaining = budget - spentBefore - spentToday;
            double allowance = Math.max(0, budget - spentBefore) / daysLeft;
            String days = daysLeft + (daysLeft == 1 ? " day left" : " days left");

            if (remaining < 0) {
                return new String[] {
                        "No allowance left this month",
                        "Over budget by " + money(currency, -remaining) + " · " + days,
                };
            }
            return new String[] {
                    "Today you can spend " + money(currency, Math.max(0, allowance - spentToday)),
                    money(currency, remaining) + " left this month · " + days,
            };
        } catch (JSONException e) {
            return null;
        }
    }

    /** Same style as the web UI: no decimals for whole amounts, otherwise two. */
    private static String money(String currency, double value) {
        double rounded = Math.round(value * 100) / 100.0;
        NumberFormat nf = NumberFormat.getNumberInstance();
        boolean whole = rounded == Math.floor(rounded);
        nf.setMinimumFractionDigits(whole ? 0 : 2);
        nf.setMaximumFractionDigits(2);
        return currency + nf.format(rounded);
    }
}
