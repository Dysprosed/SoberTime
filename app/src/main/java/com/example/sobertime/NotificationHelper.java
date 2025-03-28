package com.example.sobertime;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class NotificationHelper {

    private static final String CHANNEL_ID = "sobriety_tracker_channel";
    private static final String CHANNEL_NAME = "Sobriety Tracker";
    private static final String CHANNEL_DESCRIPTION = "Notifications for your sobriety journey";

    private static final String PREFS_NAME = "SobrietyNotificationPrefs";
    private static final String MORNING_ENABLED_KEY = "morning_notification_enabled";
    private static final String EVENING_ENABLED_KEY = "evening_notification_enabled";
    private static final String MILESTONE_ENABLED_KEY = "milestone_notification_enabled";
    private static final String CUSTOM_TIMES_KEY = "custom_notification_times";

    // Request codes for pending intents
    private static final int MORNING_NOTIFICATION_REQUEST_CODE = 1001;
    private static final int EVENING_NOTIFICATION_REQUEST_CODE = 1002;
    private static final int MILESTONE_NOTIFICATION_REQUEST_CODE = 1003;
    private static final int CUSTOM_NOTIFICATION_BASE_REQUEST_CODE = 2000;

    /**
     * Create the notification channel for Android 8.0 and higher
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESCRIPTION);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Check if the app has permission to schedule exact alarms
     * For Android 12 (API 31) and above
     */
    public static boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            return alarmManager.canScheduleExactAlarms();
        }
        return true; // Permission not required for older versions
    }

    /**
     * Schedule all notifications based on user preferences
     */
    public static void scheduleNotifications(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Schedule default notifications if enabled
        if (prefs.getBoolean(MORNING_ENABLED_KEY, true)) {
            scheduleMorningNotification(context);
        }

        if (prefs.getBoolean(EVENING_ENABLED_KEY, true)) {
            scheduleEveningNotification(context);
        }

        if (prefs.getBoolean(MILESTONE_ENABLED_KEY, true)) {
            scheduleMilestoneNotifications(context);
        }

        // Schedule custom time notifications
        String customTimesString = prefs.getString(CUSTOM_TIMES_KEY, "");
        if (!customTimesString.isEmpty()) {
            String[] customTimes = customTimesString.split(",");
            for (int i = 0; i < customTimes.length; i++) {
                String[] timeParts = customTimes[i].split(":");
                int hour = Integer.parseInt(timeParts[0]);
                int minute = Integer.parseInt(timeParts[1]);

                scheduleCustomTimeNotification(context, hour, minute, CUSTOM_NOTIFICATION_BASE_REQUEST_CODE + i);
            }
        }
    }

    /**
     * Reschedule all notifications when sobriety start date changes
     */
    public static void rescheduleNotifications(Context context, long newStartDate) {
        // Cancel all existing notifications
        cancelAllNotifications(context);

        // Schedule new notifications
        scheduleNotifications(context);

        // Update milestone notifications based on new start date
        scheduleMilestoneNotifications(context);
    }

    /**
     * Schedule morning motivation notification (9:00 AM)
     */
    private static void scheduleMorningNotification(Context context) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        // If time has passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("notification_type", "morning");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                MORNING_NOTIFICATION_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Set repeating alarm for every day
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
    }

    /**
     * Schedule evening check-in notification (7:00 PM)
     */
    private static void scheduleEveningNotification(Context context) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 19);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        // If time has passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("notification_type", "evening");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                EVENING_NOTIFICATION_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Set repeating alarm for every day
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
    }

    /**
     * Schedule custom time notification
     */
    private static void scheduleCustomTimeNotification(Context context, int hour, int minute, int requestCode) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // If time has passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("notification_type", "custom");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Set repeating alarm for every day
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
    }

    /**
     * Schedule notifications for upcoming milestones
     */
    private static void scheduleMilestoneNotifications(Context context) {
        // Get sobriety start date
        SharedPreferences prefs = context.getSharedPreferences("SobrietyTrackerPrefs", Context.MODE_PRIVATE);
        long startDate = prefs.getLong("sobriety_start_date", System.currentTimeMillis());

        // Calculate current days sober
        long currentTime = System.currentTimeMillis();
        long diffInMillis = currentTime - startDate;
        int daysSober = (int) TimeUnit.MILLISECONDS.toDays(diffInMillis);

        // Define milestones to notify about
        int[] milestones = {1, 7, 14, 30, 60, 90, 180, 365, 730, 1095, 1460, 1825};

        for (int milestone : milestones) {
            if (daysSober < milestone) {
                // Calculate when this milestone will be reached
                long daysToMilestone = milestone - daysSober;
                long milestoneTimeMillis = startDate + TimeUnit.DAYS.toMillis(milestone);

                // Only schedule if milestone is within the next 100 days
                if (daysToMilestone <= 100) {
                    scheduleMilestoneNotification(context, milestone, milestoneTimeMillis);
                }
            }
        }
    }

    /**
     * Schedule notification for a specific milestone
     */
    private static void scheduleMilestoneNotification(Context context, int milestone, long milestoneTimeMillis) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("notification_type", "milestone");
        intent.putExtra("milestone_days", milestone);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                MILESTONE_NOTIFICATION_REQUEST_CODE + milestone, // Unique request code for each milestone
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Set one-time alarm for milestone
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // For Android 12+ check if we can schedule exact alarms
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            milestoneTimeMillis,
                            pendingIntent
                    );
                } else {
                    // Fall back to inexact alarm if permission not granted
                    alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            milestoneTimeMillis,
                            pendingIntent
                    );
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        milestoneTimeMillis,
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        milestoneTimeMillis,
                        pendingIntent
                );
            }
        } catch (SecurityException e) {
            // Fall back to inexact alarm if permission denied
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    milestoneTimeMillis,
                    pendingIntent
            );
        }
    }

    /**
     * Cancel all scheduled notifications
     */
    public static void cancelAllNotifications(Context context) {
        cancelNotification(context, MORNING_NOTIFICATION_REQUEST_CODE);
        cancelNotification(context, EVENING_NOTIFICATION_REQUEST_CODE);

        // Cancel milestone notifications
        int[] milestones = {1, 7, 14, 30, 60, 90, 180, 365, 730, 1095, 1460, 1825};
        for (int milestone : milestones) {
            cancelNotification(context, MILESTONE_NOTIFICATION_REQUEST_CODE + milestone);
        }

        // Cancel custom notifications
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String customTimesString = prefs.getString(CUSTOM_TIMES_KEY, "");
        if (!customTimesString.isEmpty()) {
            String[] customTimes = customTimesString.split(",");
            for (int i = 0; i < customTimes.length; i++) {
                cancelNotification(context, CUSTOM_NOTIFICATION_BASE_REQUEST_CODE + i);
            }
        }
    }

    /**
     * Cancel a specific notification
     */
    private static void cancelNotification(Context context, int requestCode) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }
}