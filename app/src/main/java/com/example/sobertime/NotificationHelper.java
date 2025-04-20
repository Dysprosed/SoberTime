package com.example.sobertime;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import com.example.sobertime.IntrusiveNotificationReceiver;

import com.example.sobertime.model.SobrietyTracker;

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

    private static final int INTRUSIVE_NOTIFICATION_REQUEST_CODE = 1005;

    // Request codes for pending intents
    private static final int MORNING_NOTIFICATION_REQUEST_CODE = 1001;
    private static final int EVENING_NOTIFICATION_REQUEST_CODE = 1002;
    private static final int MILESTONE_NOTIFICATION_REQUEST_CODE = 1003;
    private static final int CUSTOM_NOTIFICATION_BASE_REQUEST_CODE = 2000;

    private static final String TAG = "NotificationHelper";
    private static final int MORNING_NOTIFICATION_ID = 101;
    private static final int EVENING_NOTIFICATION_ID = 102;
    private static final int MILESTONE_NOTIFICATION_ID = 103;
    private static final int CUSTOM_NOTIFICATION_BASE_ID = 200;

    private static final int CHECK_IN_NOTIFICATION_REQUEST_CODE = 1003;
    private static final int CHECK_IN_NOTIFICATION_ID = 3001;

    private static void scheduleFixedTimeNotification(Context context, int hour, int minute, int notificationId) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
    
        // If time has passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            Log.d(TAG, "Time " + hour + ":" + minute + " already passed today, scheduling for tomorrow");
        }

        String scheduledTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(calendar.getTime());
        Log.d(TAG, "Scheduling fixed time notification ID " + notificationId + 
                " at " + scheduledTime);
    
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("notification_type", "custom");
    
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
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
        Log.d(TAG, "Fixed time notification scheduled successfully for " + scheduledTime);
    }
    
    private static void scheduleNotification(Context context, String title, String message, 
                                            long triggerTimeMillis, int notificationId) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("notification_type", "milestone");
        intent.putExtra("notification_title", title);
        intent.putExtra("notification_message", message);
    
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTimeMillis,
                            pendingIntent
                    );
                } else {
                    alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            triggerTimeMillis,
                            pendingIntent
                    );
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                );
            }
        } catch (SecurityException e) {
            // Fall back to inexact alarm if permission denied
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
            );
        }
    }

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
     * Schedule an intrusive alarm-style check-in notification
     */
    public static void scheduleIntrusiveCheckInNotification(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("notification_settings", Context.MODE_PRIVATE);
        boolean intrusiveEnabled = prefs.getBoolean("intrusive_notifications_enabled", true); // Default to enabled
        
        if (!intrusiveEnabled) {
            Log.d(TAG, "Intrusive notifications disabled, falling back to regular check-in");
            // Fall back to regular check-in notification if intrusive is disabled
            scheduleCheckInNotification(context);
            return;
        }
        
        // Cancel any existing intrusive notification before scheduling a new one
        cancelIntrusiveCheckInNotification(context);
        
        Calendar calendar = Calendar.getInstance();
        
        // Get custom time from settings or use default (9:00 PM)
        int hour = prefs.getInt("check_in_hour", 21);
        int minute = prefs.getInt("check_in_minute", 0);
        
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        
        // If time has passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        String scheduledTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(calendar.getTime());
        Log.d(TAG, "Scheduling intrusive check-in notification for " + scheduledTime);
        
        Intent intent = new Intent(context, IntrusiveNotificationReceiver.class);
        // Add flags to ensure delivery regardless of device state
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                INTRUSIVE_NOTIFICATION_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        // Try to schedule exact alarm
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // For Android 13+ use setExactAndAllowWhileIdle with AlarmManager.WINDOW_EXACT
                        alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.getTimeInMillis(),
                                pendingIntent
                        );
                    } else {
                        // For Android 12 use setExactAndAllowWhileIdle
                        alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.getTimeInMillis(),
                                pendingIntent
                        );
                    }
                    Log.d(TAG, "Scheduled exact and allow-while-idle intrusive check-in alarm for Android 12+");
                } else {
                    // Fall back to inexact alarm
                    alarmManager.setRepeating(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            AlarmManager.INTERVAL_DAY,
                            pendingIntent
                    );
                    Log.d(TAG, "Scheduled repeating (inexact) intrusive check-in alarm for Android 12+ (no exact alarm permission)");
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
                Log.d(TAG, "Scheduled exact and allow-while-idle intrusive check-in alarm for Android 6-11");
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
                Log.d(TAG, "Scheduled exact intrusive check-in alarm for Android <6");
            }
        } catch (SecurityException e) {
            // Fall back to inexact alarm if permission is denied
            Log.e(TAG, "Security exception when scheduling exact alarm: " + e.getMessage());
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
            Log.d(TAG, "Falling back to repeating (inexact) intrusive check-in alarm");
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error scheduling intrusive check-in: " + e.getMessage());
            // Try one more approach with a basic alarm
            try {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
                Log.d(TAG, "Last resort: Scheduled basic alarm for intrusive check-in");
            } catch (Exception e2) {
                Log.e(TAG, "Failed to schedule any kind of alarm: " + e2.getMessage());
            }
        }
    }

    public static boolean areNotificationsEnabled(Context context) {
        // For Android 8.0+
        NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            return notificationManager.areNotificationsEnabled();
        }
        // For older versions
        return true;
    }

    public static void scheduleCheckInNotification(Context context) {
        Calendar calendar = Calendar.getInstance();
        
        // Set time to 9:00 PM for check-in notification
        calendar.set(Calendar.HOUR_OF_DAY, 21);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        
        // If time has passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("notification_type", "check_in");
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                CHECK_IN_NOTIFICATION_REQUEST_CODE,
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
     * Schedule all notifications based on user preferences using SobrietyTracker
     * @param context The application context
     */
    public static void scheduleNotifications(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("SobrietyNotificationPrefs", Context.MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
        
        Log.d(TAG, "Scheduling notifications. Master switch enabled: " + notificationsEnabled);
    
        // Check notification permissions
        boolean notificationsPermitted = areNotificationsEnabled(context);
        boolean exactAlarmsPermitted = canScheduleExactAlarms(context);
        
        if (!notificationsEnabled) {
            Log.d(TAG, "Notifications disabled by user preference. Cancelling all notifications.");
            cancelAllNotifications(context);
            return;
        }
    
        // Get sobriety start date from SobrietyTracker
        SobrietyTracker sobrietyTracker = SobrietyTracker.getInstance(context);
    
        // Check if user prefers intrusive notifications
        SharedPreferences notifSettings = context.getSharedPreferences("notification_settings", Context.MODE_PRIVATE);
        boolean intrusiveEnabled = notifSettings.getBoolean("intrusive_notifications_enabled", true);
        
        // Cancel existing regular notifications before creating new ones
        // But don't cancel the intrusive notification which we'll schedule separately
        cancelRegularNotifications(context);
        
        // Schedule appropriate check-in notification
        if (intrusiveEnabled) {
            Log.d(TAG, "Scheduling intrusive check-in notification");
            scheduleIntrusiveCheckInNotification(context);
        } else {
            Log.d(TAG, "Scheduling regular check-in notification");
            scheduleCheckInNotification(context);
        }
        
        // Schedule morning notifications if enabled
        boolean morningEnabled = prefs.getBoolean("morning_notification_enabled", true);
        if (morningEnabled) {
            scheduleFixedTimeNotification(context, 8, 0, MORNING_NOTIFICATION_ID);
        }
        
        // Schedule evening notifications if enabled
        boolean eveningEnabled = prefs.getBoolean("evening_notification_enabled", true);
        if (eveningEnabled) {
            scheduleFixedTimeNotification(context, 20, 0, EVENING_NOTIFICATION_ID);
        }
        
        // Schedule milestone notifications if enabled
        boolean milestoneEnabled = prefs.getBoolean("milestone_notification_enabled", true);
        if (milestoneEnabled) {
            scheduleMilestoneNotifications(context, sobrietyTracker.getSobrietyStartDate());
        }
        
        // Schedule custom time notifications
        String customTimesString = prefs.getString("custom_notification_times", "");
        if (!customTimesString.isEmpty()) {
            String[] customTimes = customTimesString.split(",");
            for (int i = 0; i < customTimes.length; i++) {
                try {
                    String[] timeParts = customTimes[i].split(":");
                    int hour = Integer.parseInt(timeParts[0]);
                    int minute = Integer.parseInt(timeParts[1]);
                    
                    // Use base ID + index for custom notifications
                    scheduleFixedTimeNotification(context, hour, minute, CUSTOM_NOTIFICATION_BASE_ID + i);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing custom time: " + customTimes[i], e);
                }
            }
        }
    }

    /**
     * Reschedule all notifications after sobriety start date has changed
     * @param context The application context
     * @param newStartDate The new sobriety start date (not used directly, using SobrietyTracker)
     */
    public static void rescheduleNotifications(Context context, long newStartDate) {
        // Simply call scheduleNotifications which will use the updated start date from SobrietyTracker
        scheduleNotifications(context);
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

    // Updated to use SobrietyTracker
    private static void scheduleMilestoneNotifications(Context context, long sobrietyStartDate) {
        SobrietyTracker sobrietyTracker = SobrietyTracker.getInstance(context);
        int currentDaysSober = sobrietyTracker.getDaysSober();
        
        // Get next milestones based on achievements
        AchievementManager achievementManager = AchievementManager.getInstance(context);
        Achievement nextMilestone = achievementManager.getNextMilestone(currentDaysSober);
        
        if (nextMilestone != null) {
            // Calculate when the next milestone will be reached
            int daysToMilestone = nextMilestone.getDaysRequired() - currentDaysSober;
            
            if (daysToMilestone > 0) {
                // Calculate the date for this milestone
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, daysToMilestone);
                
                // Set time to 9:00 AM for milestone notification
                calendar.set(Calendar.HOUR_OF_DAY, 9);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                
                // Schedule notification
                scheduleNotification(
                        context,
                        "Milestone Reached!",
                        "Congratulations on " + nextMilestone.getDaysRequired() + " days of sobriety!",
                        calendar.getTimeInMillis(),
                        MILESTONE_NOTIFICATION_ID
                );
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
     * Cancel all regular scheduled notifications, but preserve intrusive check-in
     */
    public static void cancelRegularNotifications(Context context) {
        cancelNotification(context, MORNING_NOTIFICATION_REQUEST_CODE);
        cancelNotification(context, EVENING_NOTIFICATION_REQUEST_CODE);
        cancelNotification(context, CHECK_IN_NOTIFICATION_REQUEST_CODE);

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

    /**
     * Cancel the intrusive check-in notification
     */
    private static void cancelIntrusiveCheckInNotification(Context context) {
        Log.d(TAG, "Cancelling existing intrusive check-in notification");
        Intent intent = new Intent(context, IntrusiveNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                INTRUSIVE_NOTIFICATION_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }
}