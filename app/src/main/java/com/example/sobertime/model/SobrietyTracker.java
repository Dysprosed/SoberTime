package com.example.sobertime.model;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Singleton class that handles all sobriety tracking calculations.
 * This class centralizes sobriety calculations to ensure consistency across the app.
 */
public class SobrietyTracker {
    
    private static final String PREFS_NAME = "SobrietyTrackerPrefs";
    private static final String START_DATE_KEY = "sobriety_start_date";
    
    private static SobrietyTracker instance;
    private Context context;
    private long sobrietyStartDate;
    
    /**
     * Private constructor to enforce singleton pattern
     */
    private SobrietyTracker(Context context) {
        this.context = context.getApplicationContext(); // Use application context to prevent leaks
        loadSobrietyStartDate();
    }
    
    /**
     * Get the singleton instance of SobrietyTracker
     * @param context Context used to access SharedPreferences
     * @return The singleton instance
     */
    public static synchronized SobrietyTracker getInstance(Context context) {
        if (instance == null) {
            instance = new SobrietyTracker(context);
        }
        return instance;
    }
    
    /**
     * Load the sobriety start date from shared preferences
     */
    private void loadSobrietyStartDate() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sobrietyStartDate = prefs.getLong(START_DATE_KEY, System.currentTimeMillis());
    }
    
    /**
     * Get the sobriety start date timestamp
     * @return Timestamp in milliseconds of the sobriety start date
     */
    public long getSobrietyStartDate() {
        return sobrietyStartDate;
    }
    
    /**
     * Set a new sobriety start date and save it to preferences
     * @param startDate New sobriety start date in milliseconds
     */
    public void setSobrietyStartDate(long startDate) {
        this.sobrietyStartDate = startDate;
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putLong(START_DATE_KEY, startDate);
        editor.apply();
    }
    
    /**
     * Calculate the number of days sober
     * @return Number of days sober, including today
     */
    public int getDaysSober() {
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTimeInMillis(sobrietyStartDate);
        // Clear time portion to start at beginning of day
        startCalendar.set(Calendar.HOUR_OF_DAY, 0);
        startCalendar.set(Calendar.MINUTE, 0);
        startCalendar.set(Calendar.SECOND, 0);
        startCalendar.set(Calendar.MILLISECOND, 0);
        
        Calendar currentCalendar = Calendar.getInstance();
        // Clear time portion to count full days
        currentCalendar.set(Calendar.HOUR_OF_DAY, 0);
        currentCalendar.set(Calendar.MINUTE, 0);
        currentCalendar.set(Calendar.SECOND, 0);
        currentCalendar.set(Calendar.MILLISECOND, 0);
        
        // Calculate days between (including today)
        long diffInMillis = currentCalendar.getTimeInMillis() - startCalendar.getTimeInMillis();
        return (int) TimeUnit.MILLISECONDS.toDays(diffInMillis) + 1; // Add 1 to count today
    }
    
    /**
     * Calculate days sober in a specific month
     * @param year The year to calculate for
     * @param month The month to calculate for (0-based, January is 0)
     * @return Number of days sober in specified month
     */
    public int getDaysSoberInMonth(int year, int month) {
        // Create calendar for first day of month
        Calendar firstDayOfMonth = Calendar.getInstance();
        firstDayOfMonth.set(year, month, 1, 0, 0, 0);
        firstDayOfMonth.set(Calendar.MILLISECOND, 0);
        
        // Create calendar for first day of next month
        Calendar firstDayOfNextMonth = (Calendar) firstDayOfMonth.clone();
        firstDayOfNextMonth.add(Calendar.MONTH, 1);
        
        // If sobriety started after this month, return 0
        if (sobrietyStartDate > firstDayOfNextMonth.getTimeInMillis()) {
            return 0;
        }
        
        // If sobriety started before this month
        if (sobrietyStartDate <= firstDayOfMonth.getTimeInMillis()) {
            // Calculate days in this month
            return (int) TimeUnit.MILLISECONDS.toDays(
                Math.min(System.currentTimeMillis(), firstDayOfNextMonth.getTimeInMillis()) - 
                firstDayOfMonth.getTimeInMillis());
        }
        
        // If sobriety started during this month
        return (int) TimeUnit.MILLISECONDS.toDays(
            Math.min(System.currentTimeMillis(), firstDayOfNextMonth.getTimeInMillis()) - 
            sobrietyStartDate);
    }
    
    /**
     * Calculate the money saved based on drink cost and frequency
     * @param drinkCost Cost per drink
     * @param drinksPerWeek Number of drinks typically consumed per week
     * @return Total money saved since sobriety start date
     */
    public float calculateMoneySaved(float drinkCost, int drinksPerWeek) {
        int daysSober = getDaysSober();
        float drinksAvoided = (daysSober / 7.0f) * drinksPerWeek;
        return drinksAvoided * drinkCost;
    }
    
    /**
     * Calculate calories saved based on calories per drink and frequency
     * @param caloriesPerDrink Calories in a typical drink
     * @param drinksPerWeek Number of drinks typically consumed per week
     * @return Total calories saved since sobriety start date
     */
    public int calculateCaloriesSaved(int caloriesPerDrink, int drinksPerWeek) {
        int daysSober = getDaysSober();
        float drinksAvoided = (daysSober / 7.0f) * drinksPerWeek;
        return (int)(drinksAvoided * caloriesPerDrink);
    }
}