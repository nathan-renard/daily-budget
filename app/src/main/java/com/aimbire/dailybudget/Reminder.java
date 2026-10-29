package com.aimbire.dailybudget;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * Morning reminder: schedules a daily alarm and builds the "today you can spend"
 * notification from the data the web UI syncs (see {@link Budget}).
 */
final class Reminder {

    private static final String PREFS = "reminder";
    private static final String CHANNEL_ID = "daily_allowance";
    private static final int NOTIFICATION_ID = 1;

    private Reminder() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---- Settings ------------------------------------------------------------

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
                CHANNEL_ID, context.getString(R.string.channel_daily_allowance), NotificationManager.IMPORTANCE_DEFAULT));

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
        Budget.Data data = Budget.load(context);
        if (data.budget <= 0) return null;
        Budget.Day d = Budget.compute(data, LocalDate.now());
        String days = context.getResources().getQuantityString(R.plurals.days_left, d.daysLeft, d.daysLeft);

        if (d.monthRemaining < 0) {
            return new String[] {
                    context.getString(R.string.notify_over_title),
                    context.getString(R.string.over_budget_by, Budget.money(data.currency, -d.monthRemaining)) + " · " + days,
            };
        }
        return new String[] {
                context.getString(R.string.notify_title, Budget.money(data.currency, Math.max(0, d.todayLeft))),
                context.getString(R.string.month_left, Budget.money(data.currency, d.monthRemaining)) + " · " + days,
        };
    }
}
