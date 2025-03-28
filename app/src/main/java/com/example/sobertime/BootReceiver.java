package com.example.sobertime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Receiver that runs when the device boots to reschedule notifications
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String PREFS_NAME = "SobrietyNotificationPrefs";
    private static final String NOTIFICATIONS_ENABLED_KEY = "notifications_enabled";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            // Check if notifications are enabled
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            boolean notificationsEnabled = prefs.getBoolean(NOTIFICATIONS_ENABLED_KEY, true);

            if (notificationsEnabled) {
                // Reschedule all notifications
                NotificationHelper.scheduleNotifications(context);
            }
        }
    }
}
