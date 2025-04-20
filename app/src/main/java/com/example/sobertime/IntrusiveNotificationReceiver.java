package com.example.sobertime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.app.AlarmManager;
import android.app.PendingIntent;

import com.example.sobertime.model.SobrietyTracker;

public class IntrusiveNotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "IntrusiveNotifReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received intrusive notification alarm");
        
        // Check if the user has already checked in today
        SobrietyTracker tracker = SobrietyTracker.getInstance(context);
        
        if (!hasCheckedInToday(tracker, context)) {
            Log.d(TAG, "User hasn't checked in today, launching intrusive check-in");
            
            // Launch intrusive check-in activity
            Intent launchIntent = new Intent(context, IntrusiveCheckInActivity.class);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            // Add flags to override DND mode and keyguard
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
            }
            
            // Add flag to indicate this is an automatic prompt
            launchIntent.putExtra("automatic_prompt", true);
            launchIntent.putExtra("intrusive_prompt", true);
            
            try {
                context.startActivity(launchIntent);
                Log.d(TAG, "Successfully launched intrusive check-in activity");
            } catch (Exception e) {
                Log.e(TAG, "Failed to launch intrusive check-in: " + e.getMessage(), e);
            }
        } else {
            Log.d(TAG, "User has already checked in today, no need for intrusive check-in");
        }
        
        // Always reschedule next alarm to ensure we don't miss future check-ins
        Log.d(TAG, "Rescheduling next intrusive check-in alarm");
        NotificationHelper.scheduleIntrusiveCheckInNotification(context);
    }
    
    private boolean hasCheckedInToday(SobrietyTracker tracker, Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SobrietyTracker.PREFS_NAME, Context.MODE_PRIVATE);
        long lastConfirmed = prefs.getLong("last_confirmed_date", 0);
        
        if (lastConfirmed == 0) {
            Log.d(TAG, "No previous check-in found");
            return false;
        }
        
        java.util.Calendar lastConfirmedCal = java.util.Calendar.getInstance();
        lastConfirmedCal.setTimeInMillis(lastConfirmed);
        
        java.util.Calendar todayCal = java.util.Calendar.getInstance();
        
        // Compare just the date portions
        boolean checkedInToday = lastConfirmedCal.get(java.util.Calendar.YEAR) == todayCal.get(java.util.Calendar.YEAR) &&
               lastConfirmedCal.get(java.util.Calendar.DAY_OF_YEAR) == todayCal.get(java.util.Calendar.DAY_OF_YEAR);
        
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