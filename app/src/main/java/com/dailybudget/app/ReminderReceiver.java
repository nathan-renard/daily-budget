package com.dailybudget.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Alarm fired: show today's allowance and arm tomorrow's alarm. */
public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Reminder.isEnabled(context)) Reminder.show(context);
        Reminder.schedule(context);
    }
}
