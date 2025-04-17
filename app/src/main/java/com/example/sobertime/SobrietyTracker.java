package com.example.sobertime;

import java.util.Calendar;

import android.content.Context;
import android.content.SharedPreferences;

public class SobrietyTracker {
    private static SobrietyTracker instance;
    public static final String PREFS_NAME = "SobrietyTrackerPrefs";
    public static final String START_DATE_KEY = "sobriety_start_date";
    public static final String LAST_CONFIRMED_DATE_KEY = "last_confirmed_date";
    public static final String CONFIRMED_DAYS_KEY = "confirmed_days_count";
    public static final String CURRENT_STREAK_KEY = "current_checkin_streak";
    public static final String BEST_STREAK_KEY = "best_checkin_streak";
    
    private Context context;
    
    public int getCurrentStreak() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(CURRENT_STREAK_KEY, 0);
    }
    
    public int getBestStreak() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(BEST_STREAK_KEY, 0);
    }
    
    // Update streak when confirming sobriety
    public void updateStreak() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Get current streak
        int currentStreak = prefs.getInt(CURRENT_STREAK_KEY, 0);
        int bestStreak = prefs.getInt(BEST_STREAK_KEY, 0);
        
        // Get last confirmed date
        long lastConfirmedMillis = prefs.getLong(LAST_CONFIRMED_DATE_KEY, 0);
        
        // If never confirmed before, start streak at 1
        if (lastConfirmedMillis == 0) {
            editor.putInt(CURRENT_STREAK_KEY, 1);
            editor.putInt(BEST_STREAK_KEY, 1);
            editor.apply();
            return;
        }
        
        Calendar lastConfirmed = Calendar.getInstance();
        lastConfirmed.setTimeInMillis(lastConfirmedMillis);
        
        Calendar today = Calendar.getInstance();
        
        // Clear time portion for accurate day comparison
        lastConfirmed.set(Calendar.HOUR_OF_DAY, 0);
        lastConfirmed.set(Calendar.MINUTE, 0);
        lastConfirmed.set(Calendar.SECOND, 0);
        lastConfirmed.set(Calendar.MILLISECOND, 0);
        
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        // Calculate days difference
        long diffInMillis = today.getTimeInMillis() - lastConfirmed.getTimeInMillis();
        int daysDifference = (int) (diffInMillis / (24 * 60 * 60 * 1000));
        
        if (daysDifference == 1) {
            // Consecutive day, increase streak
            currentStreak++;
        } else if (daysDifference > 1) {
            // Missed days, reset streak
            currentStreak = 1;
        }
        // If daysDifference == 0, it's the same day, don't change streak
        
        // Update best streak if needed
        if (currentStreak > bestStreak) {
            bestStreak = currentStreak;
        }
        
        // Save updated values
        editor.putInt(CURRENT_STREAK_KEY, currentStreak);
        editor.putInt(BEST_STREAK_KEY, bestStreak);
        editor.apply();
    }

    // Get confirmed days rather than calculated days
    public int getConfirmedDaysSober() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(CONFIRMED_DAYS_KEY, 0);
    }

    // Confirm today's sobriety
    public void confirmSobrietyForToday() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Get current date (normalized to start of day)
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        // Get last confirmed date
        long lastConfirmedMillis = prefs.getLong(LAST_CONFIRMED_DATE_KEY, 0);
        Calendar lastConfirmed = Calendar.getInstance();
        lastConfirmed.setTimeInMillis(lastConfirmedMillis);
        lastConfirmed.set(Calendar.HOUR_OF_DAY, 0);
        lastConfirmed.set(Calendar.MINUTE, 0);
        lastConfirmed.set(Calendar.SECOND, 0);
        lastConfirmed.set(Calendar.MILLISECOND, 0);
        
        // Check if we already confirmed today
        if (today.getTimeInMillis() == lastConfirmed.getTimeInMillis()) {
            // Already confirmed today, nothing to do
            return;
        }
        
        // Get current count and increment
        int currentCount = prefs.getInt(CONFIRMED_DAYS_KEY, 0);
        
        // Update values
        editor.putLong(LAST_CONFIRMED_DATE_KEY, today.getTimeInMillis());
        editor.putInt(CONFIRMED_DAYS_KEY, currentCount + 1);
        editor.apply();
    }
    
    // Reset sobriety counter but maintain the start date for reference
    public void resetSobrietyCounter() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Keep the original start date for historical reference
        long originalStartDate = prefs.getLong(START_DATE_KEY, System.currentTimeMillis());
        
        // Update start date to today
        editor.putLong(START_DATE_KEY, System.currentTimeMillis());
        
        // Reset confirmed count
        editor.putInt(CONFIRMED_DAYS_KEY, 0);
        
        // Set last confirmed to today
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        editor.putLong(LAST_CONFIRMED_DATE_KEY, today.getTimeInMillis());
        
        editor.apply();
    }

    /**
     * Calculate money saved by not drinking
     * @param drinkCost the average cost per drink
     * @param drinksPerWeek average number of drinks consumed per week before sobriety
     * @return total money saved during the sobriety period
     */
    public float calculateMoneySaved(float drinkCost, int drinksPerWeek) {
        int daysSober = getDaysSober();
        float daysInWeek = 7.0f;
        float drinksPerDay = drinksPerWeek / daysInWeek;
        return daysSober * drinksPerDay * drinkCost;
    }

    public static synchronized SobrietyTracker getInstance(Context context) {
        if (instance == null) {
            instance = new SobrietyTracker(context.getApplicationContext());
        }
        return instance;
    }
    
    private SobrietyTracker(Context context) {
        this.context = context;
    }
    
    public void setSobrietyStartDate(long startDate) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(START_DATE_KEY, startDate);
        editor.apply();
    }
    
    public long getSobrietyStartDate() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(START_DATE_KEY, System.currentTimeMillis());
    }
    
    public int getDaysSober() {
        long startDate = getSobrietyStartDate();
        long currentTime = System.currentTimeMillis();
        long diffInMillis = currentTime - startDate;
        
        return (int) java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffInMillis);
    }

    public boolean hasCheckedInRecently() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastConfirmed = prefs.getLong(LAST_CONFIRMED_DATE_KEY, 0);
        
        // If never confirmed, return false
        if (lastConfirmed == 0) return false;
        
        // Calculate days since last confirmation
        Calendar lastConfirmedCal = Calendar.getInstance();
        lastConfirmedCal.setTimeInMillis(lastConfirmed);
        
        Calendar todayCal = Calendar.getInstance();
        
        // Clear time portion for accurate day comparison
        lastConfirmedCal.set(Calendar.HOUR_OF_DAY, 0);
        lastConfirmedCal.set(Calendar.MINUTE, 0);
        lastConfirmedCal.set(Calendar.SECOND, 0);
        lastConfirmedCal.set(Calendar.MILLISECOND, 0);
        
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        todayCal.set(Calendar.MILLISECOND, 0);
        
        // Calculate days difference
        long diffInMillis = todayCal.getTimeInMillis() - lastConfirmedCal.getTimeInMillis();
        int daysDifference = (int) (diffInMillis / (24 * 60 * 60 * 1000));
        
        // Return true if checked in yesterday or today
        return daysDifference <= 1;
    }
}