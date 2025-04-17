package com.example.sobertime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

public class IntrusiveNotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "IntrusiveNotifReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received intrusive notification alarm");
        
        // Check if the user has already checked in today
        SobrietyTracker tracker = SobrietyTracker.getInstance(context);
        
        if (!hasCheckedInToday(tracker, context)) {
            // Launch intrusive check-in activity
            Intent launchIntent = new Intent(context, IntrusiveCheckInActivity.class);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            // Add flag to indicate this is an automatic prompt
            launchIntent.putExtra("automatic_prompt", true);
            launchIntent.putExtra("intrusive_prompt", true);
            
            context.startActivity(launchIntent);
        }
        
        // Reschedule next alarm
        NotificationHelper.scheduleIntrusiveCheckInNotification(context);
    }
    
    private boolean hasCheckedInToday(SobrietyTracker tracker, Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SobrietyTracker.PREFS_NAME, Context.MODE_PRIVATE);
        long lastConfirmed = prefs.getLong(SobrietyTracker.LAST_CONFIRMED_DATE_KEY, 0);
        
        if (lastConfirmed == 0) {
            return false;
        }
        
        java.util.Calendar lastConfirmedCal = java.util.Calendar.getInstance();
        lastConfirmedCal.setTimeInMillis(lastConfirmed);
        
        java.util.Calendar todayCal = java.util.Calendar.getInstance();
        
        // Compare just the date portions
        return lastConfirmedCal.get(java.util.Calendar.YEAR) == todayCal.get(java.util.Calendar.YEAR) &&
               lastConfirmedCal.get(java.util.Calendar.DAY_OF_YEAR) == todayCal.get(java.util.Calendar.DAY_OF_YEAR);
    }
}