package com.example.sobertime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import main.java.com.example.sobertime.BaseActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ProgressReportActivity extends BaseActivity {

    private static final String PREFS_NAME = "SobrietyTrackerPrefs";
    private static final String START_DATE_KEY = "sobriety_start_date";
    
    private LinearLayout reportContainer;
    private TextView dateRangeTextView;
    private TextView totalDaysTextView;
    private TextView moneySavedTextView;
    private TextView caloriesSavedTextView;
    private TextView achievementsCountTextView;
    private TextView journalCountTextView;
    private CardView weeklyTrendsCard;
    private TextView weeklyTrendsTextView;
    private Button emailReportButton;
    private Button sharingReportButton;
    
    private long sobrietyStartDate;
    private int totalDaysSober;
    private DatabaseHelper databaseHelper;
    private AchievementManager achievementManager;
    private Calendar startDate;
    private Calendar endDate;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress_report);
        
        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Progress Report");
        }
        
        // Initialize database helpers
        databaseHelper = DatabaseHelper.getInstance(this);
        achievementManager = AchievementManager.getInstance(this);
        
        // Get sobriety start date
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        sobrietyStartDate = prefs.getLong(START_DATE_KEY, System.currentTimeMillis());
        totalDaysSober = getDaysSober();
        
        // Set date range (default to last 30 days)
        startDate = Calendar.getInstance();
        startDate.add(Calendar.DAY_OF_YEAR, -30);
        endDate = Calendar.getInstance();
        
        // Initialize views
        initializeViews();
        
        // Generate report data
        generateReport();
        
        // Set up button listeners
        setupButtonListeners();
    }
    
    private void initializeViews() {
        reportContainer = findViewById(R.id.reportContainer);
        dateRangeTextView = findViewById(R.id.dateRangeTextView);
        totalDaysTextView = findViewById(R.id.totalDaysTextView);
        moneySavedTextView = findViewById(R.id.moneySavedTextView);
        caloriesSavedTextView = findViewById(R.id.caloriesSavedTextView);
        achievementsCountTextView = findViewById(R.id.achievementsCountTextView);
        journalCountTextView = findViewById(R.id.journalCountTextView);
        weeklyTrendsCard = findViewById(R.id.weeklyTrendsCard);
        weeklyTrendsTextView = findViewById(R.id.weeklyTrendsTextView);
        emailReportButton = findViewById(R.id.emailReportButton);
        sharingReportButton = findViewById(R.id.shareReportButton);
    }
    
    private void generateReport() {
        // Set date range text
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        String dateRangeStr = dateFormat.format(startDate.getTime()) + " - " + dateFormat.format(endDate.getTime());
        dateRangeTextView.setText(dateRangeStr);
        
        // Calculate total days in range
        int daysInRange = calculateDaysInRange();
        totalDaysTextView.setText(String.valueOf(daysInRange));
        
        // Calculate money saved
        float drinkCost = databaseHelper.getFloatSetting("drink_cost", 8.50f);
        int drinksPerWeek = databaseHelper.getIntSetting("drinks_per_week", 15);
        float drinksAvoided = (daysInRange / 7.0f) * drinksPerWeek;
        float moneySaved = drinksAvoided * drinkCost;
        
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance();
        moneySavedTextView.setText(currencyFormatter.format(moneySaved));
        
        // Calculate calories saved
        int caloriesPerDrink = databaseHelper.getIntSetting("calories_per_drink", 150);
        int caloriesSaved = (int) (drinksAvoided * caloriesPerDrink);
        caloriesSavedTextView.setText(NumberFormat.getNumberInstance(Locale.US).format(caloriesSaved));
        
        // Get achievements count
        int achievementsInRange = calculateAchievementsInRange();
        achievementsCountTextView.setText(String.valueOf(achievementsInRange));
        
        // Get journal entries count
        int journalEntriesInRange = calculateJournalEntriesInRange();
        journalCountTextView.setText(String.valueOf(journalEntriesInRange));
        
        // Generate weekly trends data
        generateWeeklyTrends();
    }
    
    private int calculateDaysInRange() {
        // If start date is before sobriety start date, adjust it
        Calendar adjustedStartDate = Calendar.getInstance();
        adjustedStartDate.setTimeInMillis(Math.max(startDate.getTimeInMillis(), sobrietyStartDate));
        
        // Calculate days between adjusted start date and end date
        long diffInMillis = endDate.getTimeInMillis() - adjustedStartDate.getTimeInMillis();
        return (int) TimeUnit.MILLISECONDS.toDays(diffInMillis) + 1; // Include end date
    }
    
    private int calculateAchievementsInRange() {
        // In a full implementation, this would look at achievement unlock dates
        // For simplicity, we'll just return the count of unlocked achievements
        return achievementManager.getUnlockedAchievements().size();
    }
    
    private int calculateJournalEntriesInRange() {
        // In a full implementation, this would query the database for entries in the date range
        // For simplicity, we'll generate a random number based on days in range
        int daysInRange = calculateDaysInRange();
        return Math.min(daysInRange, (int) (daysInRange * 0.6));
    }
    
    private void generateWeeklyTrends() {
        // In a full implementation, this would generate a weekly breakdown of activities
        // For simplicity, we'll just create a sample text description
        
        StringBuilder trendsText = new StringBuilder();
        trendsText.append("• Week 1: 7 sober days, 2 journal entries\n");
        trendsText.append("• Week 2: 7 sober days, 4 journal entries, 1 achievement\n");
        trendsText.append("• Week 3: 7 sober days, 3 journal entries\n");
        trendsText.append("• Week 4: 7 sober days, 5 journal entries, 1 achievement\n");
        
        weeklyTrendsTextView.setText(trendsText.toString());
    }
    
    private void setupButtonListeners() {
        emailReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendReportEmail();
            }
        });
        
        sharingReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareReport();
            }
        });
    }
    
    private void sendReportEmail() {
        try {
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("message/rfc822");
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "My Sobriety Progress Report");
            emailIntent.putExtra(Intent.EXTRA_TEXT, generateReportText());
            
            startActivity(Intent.createChooser(emailIntent, "Send report via email"));
        } catch (Exception e) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void shareReport() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My Sobriety Progress Report");
            shareIntent.putExtra(Intent.EXTRA_TEXT, generateReportText());
            
            startActivity(Intent.createChooser(shareIntent, "Share report via"));
        } catch (Exception e) {
            Toast.makeText(this, "Error sharing report", Toast.LENGTH_SHORT).show();
        }
    }
    
    private String generateReportText() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        
        StringBuilder reportText = new StringBuilder();
        reportText.append("NEW DAWN SOBRIETY TRACKER - PROGRESS REPORT\n\n");
        reportText.append("Date Range: ").append(dateRangeTextView.getText()).append("\n\n");
        reportText.append("Total Sober Days: ").append(totalDaysTextView.getText()).append("\n");
        reportText.append("Money Saved: ").append(moneySavedTextView.getText()).append("\n");
        reportText.append("Calories Saved: ").append(caloriesSavedTextView.getText()).append("\n");
        reportText.append("Achievements Unlocked: ").append(achievementsCountTextView.getText()).append("\n");
        reportText.append("Journal Entries: ").append(journalCountTextView.getText()).append("\n\n");
        
        reportText.append("Weekly Breakdown:\n");
        reportText.append(weeklyTrendsTextView.getText()).append("\n\n");
        
        reportText.append("Overall Progress: ").append(totalDaysSober).append(" days sober since ");
        reportText.append(dateFormat.format(new Date(sobrietyStartDate))).append("\n\n");
        
        reportText.append("Generated by New Dawn Sobriety Tracker on ");
        reportText.append(dateFormat.format(new Date()));
        
        return reportText.toString();
    }
    
    private int getDaysSober() {
        long currentTime = System.currentTimeMillis();
        long diffInMillis = currentTime - sobrietyStartDate;
        return (int) TimeUnit.MILLISECONDS.toDays(diffInMillis);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_progress_report, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_date_range) {
            // Open date range picker (would be implemented in full version)
            Toast.makeText(this, "Date range selection coming soon", Toast.LENGTH_SHORT).show();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
}
