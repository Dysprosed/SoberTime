package com.example.sobertime;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of App Widget functionality.
 */
public class SobrietyWidgetProvider extends AppWidgetProvider {

    private static final String PREFS_NAME = "SobrietyTrackerPrefs";
    private static final String START_DATE_KEY = "sobriety_start_date";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // Get sobriety information
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long startDate = prefs.getLong(START_DATE_KEY, System.currentTimeMillis());
        int daysSober = getDaysSober(startDate);
        String formattedDate = formatDate(startDate);
        
        // Get next milestone
        int[] milestones = {1, 7, 14, 30, 60, 90, 180, 365, 730, 1095};
        int nextMilestone = getNextMilestone(daysSober, milestones);
        int daysUntilNextMilestone = nextMilestone - daysSober;
        
        // Get random motivational quote
        QuoteManager quoteManager = QuoteManager.getInstance(context);
        Quote randomQuote = quoteManager.getRandomQuote();
        String quoteText = randomQuote != null ? 
                "\"" + randomQuote.getText() + "\" — " + randomQuote.getAuthor() : 
                "Every day sober is a victory.";
        
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.sobriety_widget);
        views.setTextViewText(R.id.appwidget_days_count, String.valueOf(daysSober));
        views.setTextViewText(R.id.appwidget_since_date, "Sober since: " + formattedDate);
        
        if (daysUntilNextMilestone == 0) {
            views.setTextViewText(R.id.appwidget_next_milestone, "Today is a milestone day! 🎉");
        } else {
            views.setTextViewText(R.id.appwidget_next_milestone, 
                    nextMilestone + " days: " + daysUntilNextMilestone + " to go");
        }
        
        views.setTextViewText(R.id.appwidget_quote, quoteText);
        
        // Create an Intent to launch MainActivity when widget is clicked
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent);
        
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
    
    /**
     * Calculate days sober
     */
    private static int getDaysSober(long startDate) {
        long currentTime = System.currentTimeMillis();
        long diffInMillis = currentTime - startDate;
        return (int) TimeUnit.MILLISECONDS.toDays(diffInMillis);
    }
    
    /**
     * Format date for display
     */
    private static String formatDate(long timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        return dateFormat.format(new Date(timestamp));
    }
    
    /**
     * Get next milestone
     */
    private static int getNextMilestone(int daysSober, int[] milestones) {
        for (int milestone : milestones) {
            if (daysSober <= milestone) {
                return milestone;
            }
        }
        
        // If we've passed all predefined milestones, calculate next year milestone
        int yearsSober = daysSober / 365;
        return (yearsSober + 1) * 365;
    }
    
    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}
