package com.example.sobertime;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.sobertime.model.SobrietyTracker;

public class IntrusiveNotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "IntrusiveNotifReceiver";
    private static final String SOBRIETY_PREFS_NAME = "SobrietyTrackerPrefs";
    private static final String LAST_CONFIRMED_DATE_KEY = "last_confirmed_date";
    private static final String CHANNEL_ID = "intrusive_check_in_channel";
    private static final int NOTIFICATION_ID = 3001;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received intrusive notification alarm");
        
        // Check if the user has already checked in today
        SobrietyTracker tracker = SobrietyTracker.getInstance(context);
        
        if (!hasCheckedInToday(tracker, context)) {
            Log.d(TAG, "User hasn't checked in today, showing intrusive check-in notification");
            
            // Show a high-priority, full-screen notification instead of directly launching activity
            showIntrusiveNotification(context);
        } else {
            Log.d(TAG, "User has already checked in today, no need for intrusive check-in");
        }
        
        // Always reschedule next alarm to ensure we don't miss future check-ins
        Log.d(TAG, "Rescheduling next intrusive check-in alarm");
        NotificationHelper.scheduleIntrusiveCheckInNotification(context);
    }
    
    private void showIntrusiveNotification(Context context) {
        // First, create the notification channel (required for Android O and later)
        createIntrusiveNotificationChannel(context);
        
        // Prepare the intent for the notification
        Intent launchIntent = new Intent(context, IntrusiveCheckInActivity.class);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        launchIntent.putExtra("automatic_prompt", true);
        launchIntent.putExtra("intrusive_prompt", true);
        launchIntent.putExtra("from_notification", true);
        
        // Create pending intent for full-screen launch
        PendingIntent fullScreenIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Create main content intent (same as fullScreenIntent in this case)
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                1,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Get alarm sound
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        
        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Sobriety Check-In Required")
                .setContentText("It's time for your daily sobriety check-in")
                .setPriority(NotificationCompat.PRIORITY_MAX) // Highest priority
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSound(alarmSound)
                .setAutoCancel(false)
                .setOngoing(true) // Make notification persistent
                .setFullScreenIntent(fullScreenIntent, true) // Use full screen intent
                .setContentIntent(contentIntent);
        
        // Add vibration pattern if enabled
        SharedPreferences prefs = context.getSharedPreferences("notification_settings", Context.MODE_PRIVATE);
        boolean useVibration = prefs.getBoolean("use_vibration", true);
        
        if (useVibration) {
            // Vibration pattern: wait 0ms, vibrate 500ms, wait 500ms, repeat
            long[] vibrationPattern = {0, 500, 500};
            builder.setVibrate(vibrationPattern);
        }
        
        // Get notification manager and show notification
        NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                
        notificationManager.notify(NOTIFICATION_ID, builder.build());
        Log.d(TAG, "Intrusive check-in notification displayed");
    }
    
    private void createIntrusiveNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Intrusive Check-In Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
                    
            channel.setDescription("Notifications for critical sobriety check-ins");
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setBypassDnd(true); // Bypass Do Not Disturb mode
            channel.enableLights(true);
            channel.enableVibration(true);
            
            // Configure sound
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmSound == null) {
                alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
                    
            channel.setSound(alarmSound, audioAttributes);
            
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    private boolean hasCheckedInToday(SobrietyTracker tracker, Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SOBRIETY_PREFS_NAME, Context.MODE_PRIVATE);
        long lastConfirmed = prefs.getLong(LAST_CONFIRMED_DATE_KEY, 0);
        
        if (lastConfirmed == 0) {
            Log.d(TAG, "No previous check-in found");
            return false;
        }
        
        java.util.Calendar lastConfirmedCal = java.util.Calendar.getInstance();
        lastConfirmedCal.setTimeInMillis(lastConfirmed);
        
        java.util.Calendar todayCal = java.util.Calendar.getInstance();
        
        // Reset time portion to compare just the dates
        lastConfirmedCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        lastConfirmedCal.set(java.util.Calendar.MINUTE, 0);
        lastConfirmedCal.set(java.util.Calendar.SECOND, 0);
        lastConfirmedCal.set(java.util.Calendar.MILLISECOND, 0);
        
        todayCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        todayCal.set(java.util.Calendar.MINUTE, 0);
        todayCal.set(java.util.Calendar.SECOND, 0);
        todayCal.set(java.util.Calendar.MILLISECOND, 0);
        
        // Compare just the date portions
        boolean checkedInToday = lastConfirmedCal.get(java.util.Calendar.YEAR) == todayCal.get(java.util.Calendar.YEAR) &&
               lastConfirmedCal.get(java.util.Calendar.DAY_OF_YEAR) == todayCal.get(java.util.Calendar.DAY_OF_YEAR);
        
        // Get the settings to see if we should enforce daily check-ins
        SharedPreferences notifSettings = context.getSharedPreferences("notification_settings", Context.MODE_PRIVATE);
        boolean enforceDailyCheckins = notifSettings.getBoolean("enforce_daily_checkins", true);
        
        // If daily check-ins are not enforced, act as if the user hasn't checked in yet
        if (!enforceDailyCheckins) {
            Log.d(TAG, "Daily check-ins are not being enforced, allowing intrusive check-in anyway");
            return false;
        }
        
        Log.d(TAG, "Has checked in today: " + checkedInToday + 
              ", last check-in time: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
              .format(new java.util.Date(lastConfirmed)));
              
        return checkedInToday;
    }

    /**
     * Helper method to create a test alarm for debugging purposes
     */
    public static void scheduleTestAlarm(Context context) {
        Intent intent = new Intent(context, IntrusiveNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                9999, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        // Set alarm for 10 seconds from now (for testing)
        long triggerTime = System.currentTimeMillis() + 10000;
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
                Log.d(TAG, "Test alarm scheduled for 10 seconds from now");
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to schedule test alarm: " + e.getMessage());
            // Fall back to inexact alarm
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
            );
        }
    }
}