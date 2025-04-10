package com.example.sobertime;

import android.content.Context;
import android.content.SharedPreferences;

public class SobrietyTracker {
    private static SobrietyTracker instance;
    private static final String PREFS_NAME = "SobrietyTrackerPrefs";
    private static final String START_DATE_KEY = "sobriety_start_date";
    
    private Context context;
    
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
}