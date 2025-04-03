package com.example.sobertime;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.example.sobertime.model.SobrietyTracker;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    private TextView dayCountTextView;
    private TextView soberSinceTextView;
    private TextView nextMilestoneTextView;
    private TextView motivationTextView;
    private CardView achievementsCardView;
    private CardView statsCardView;

    // These are the new CardViews which might not exist yet
    private CardView journalCardView;
    private CardView emergencyHelpCardView;
    private CardView inspirationCardView;
    private CardView communityCardView;
    private CardView progressReportCardView;
    private ProgressBar achievementMilestoneProgressBar;
    
    // Drawer elements
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navigationView;

    private SobrietyTracker sobrietyTracker;
    private static final String PREFS_NAME = "SobrietyTrackerPrefs";
    private static final String START_DATE_KEY = "sobriety_start_date";

    private long sobrietyStartDate;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());

    private static final int PERMISSION_REQUEST_POST_NOTIFICATIONS = 1001;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private AchievementManager achievementManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme preference before setContentView
        applyThemePreference();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize SobrietyTracker instead of directly reading preferences
        sobrietyTracker = SobrietyTracker.getInstance(this);
        
        // Set up toolbar and drawer
        setupToolbarAndDrawer();

        if (getSupportActionBar() == null) {
            Log.e(TAG, "Support action bar is null after setup");
        }
        if (navigationView == null) {
            Log.e(TAG, "Navigation view is null after setup");
        }
        if (drawerLayout == null) {
            Log.e(TAG, "Drawer layout is null after setup");
        }

        // No need to initialize preferences anymore
        // Initialize preferences
        // preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Get sobriety start date from preferences, or use today's date as default
        // sobrietyStartDate = preferences.getLong(START_DATE_KEY, Calendar.getInstance().getTimeInMillis());

        // Set up permission launcher
        setupPermissions();

        // Initialize UI elements
        initializeViews();
        setupClickListeners();

        // Initialize achievement manager
        achievementManager = AchievementManager.getInstance(this);
        
        // Update milestone dates based on sobriety start date
        achievementManager.updateMilestoneDates(sobrietyStartDate);

        // Update UI with current data
        updateSobrietyInfo();

        // Check achievements for current day count
        updateAchievements();
    }
    
    private void setupToolbarAndDrawer() {
        try {
            toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                // Add this line to show the hamburger icon
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeButtonEnabled(true);
            } else {
                Log.e(TAG, "Toolbar not found in layout");
            }
            
            drawerLayout = findViewById(R.id.drawer_layout);
            navigationView = findViewById(R.id.nav_view);
            
            if (drawerLayout != null && toolbar != null) {
                ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                        this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
                drawerLayout.addDrawerListener(toggle);
                toggle.syncState();
            } else {
                Log.e(TAG, "DrawerLayout or Toolbar not found in layout");
            }
            
            if (navigationView != null) {
                navigationView.setNavigationItemSelectedListener(this);
            } else {
                Log.e(TAG, "NavigationView not found in layout");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up toolbar and drawer: " + e.getMessage());
        }
    }

    // Apply saved theme preference
    private void applyThemePreference() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isNightMode = prefs.getBoolean("night_mode_enabled", false);

        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        int daysSober = sobrietyTracker.getDaysSober();
        updateNextMilestone(daysSober);
        // Refresh data when returning to the app
        updateSobrietyInfo();

        // Check and request permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkNotificationPermission();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkExactAlarmPermission();
        }
    }

    private void setupPermissions() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Permission granted, schedule notifications
                        NotificationHelper.scheduleNotifications(this);
                    } else {
                        // Permission denied, show explanation
                        Toast.makeText(this,
                                "Notification permission is needed for reminders",
                                Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.Manifest.permission.POST_NOTIFICATIONS)) {

                    new AlertDialog.Builder(this)
                            .setTitle("Notification Permission")
                            .setMessage("This app needs notification permission to send you reminders and celebrate your milestones.")
                            .setPositiveButton("OK", (dialog, which) -> {
                                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                dialog.dismiss();
                            })
                            .create()
                            .show();
                } else {
                    // No explanation needed, request the permission
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
                }
            }
        }
    }

    private void checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!NotificationHelper.canScheduleExactAlarms(this)) {
                new AlertDialog.Builder(this)
                        .setTitle("Alarm Permission")
                        .setMessage("This app needs permission to schedule exact alarms for milestone reminders.")
                        .setPositiveButton("Settings", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            dialog.dismiss();
                        })
                        .create()
                        .show();
            }
        }
    }

    private void initializeViews() {
        // Initialize the views we're sure exist
        dayCountTextView = findViewById(R.id.dayCountTextView);
        soberSinceTextView = findViewById(R.id.soberSinceTextView);
        nextMilestoneTextView = findViewById(R.id.nextMilestoneTextView);
        motivationTextView = findViewById(R.id.motivationTextView);
        // Changed milestonesCardView to achievementsCardView
        achievementsCardView = findViewById(R.id.achievementsCardView);
        statsCardView = findViewById(R.id.statsCardView);
        
        // Safely try to find the milestone progress bar
        achievementMilestoneProgressBar = findViewById(R.id.achievementMilestoneProgressBar);

        // Safely try to find all CardViews - no compile errors whether they exist or not
        journalCardView = findCardViewSafely(R.id.journalCardView);
        emergencyHelpCardView = findCardViewSafely(R.id.emergencyHelpCardView);
        inspirationCardView = findCardViewSafely(R.id.inspirationCardView);
        communityCardView = findCardViewSafely(R.id.communityCardView);
        progressReportCardView = findCardViewSafely(R.id.progressReportCardView);
    }

    /**
     * Safely finds a CardView by ID, returning null if it doesn't exist
     * instead of crashing with a NullPointerException.
     * This method catches exceptions to handle cases where the view might not exist
     * or is not of the expected type, ensuring the app remains stable.
     */
    private androidx.cardview.widget.CardView findCardViewSafely(int id) {
        try {
            View view = findViewById(id);
            if (view instanceof androidx.cardview.widget.CardView) {
                return (androidx.cardview.widget.CardView) view;
            } else {
                String resourceName = getResources().getResourceEntryName(id);
                Log.d(TAG, "View with id " + id + " (" + resourceName + ") is not a CardView");
                return null;
            }
        } catch (Exception e) {
            try {
                String resourceName = getResources().getResourceEntryName(id);
                Log.d(TAG, "View with id " + id + " (" + resourceName + ") not found");
            } catch (Exception e2) {
                Log.d(TAG, "View with id " + id + " not found. Resource name could not be retrieved.");
            }
            return null;
        }
    }

    /**
      * Safely finds a ProgressBar by ID, returning null if it doesn't exist
      * instead of crashing with a NullPointerException
      */
    private ProgressBar findProgressBarSafely(int id) {
        try {
            return findViewById(id);
        } catch (Exception e) {
            try {
                String resourceName = getResources().getResourceEntryName(id);
                Log.d(TAG, "View with id " + id + " (" + resourceName + ") not found");
            } catch (Exception e2) {
                Log.d(TAG, "View with id " + id + " not found, and resource name could not be retrieved");
            }
            return null;
        }
    }

    // Method to setup all card click listeners
    private void setupClickListeners() {
        // Changed to achievements card
        if (achievementsCardView != null) {
            achievementsCardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        // Open achievements activity
                        Intent intent = new Intent(MainActivity.this, AchievementsActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        // Log error and show toast to user
                        Log.e(TAG, "Couldn't open Achievements", e);
                        Toast.makeText(MainActivity.this,
                                "Couldn't open Achievements: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (statsCardView != null) {
            statsCardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        // Open health benefits activity
                        Intent intent = new Intent(MainActivity.this, HealthBenefitsActivity.class);
                        intent.putExtra("days_sober", sobrietyTracker.getDaysSober());
                        startActivity(intent);
                    } catch (Exception e) {
                        // Log error and show toast to user
                        Log.e(TAG, "Couldn't open Health Benefits", e);
                        Toast.makeText(MainActivity.this,
                                "Couldn't open Health Benefits: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        // Initialize click listeners for optional CardViews
        setupCardViewClickListener(journalCardView, JournalActivity.class, "Journal");
        setupCardViewClickListener(emergencyHelpCardView, EmergencyHelpActivity.class, "Emergency Help");
        setupCardViewClickListener(inspirationCardView, InspirationActivity.class, "Inspirational Quotes");
        setupCardViewClickListener(communityCardView, CommunitySupportActivity.class, "Community Support");
        setupCardViewClickListener(progressReportCardView, ProgressReportActivity.class, "Progress Report");
    }

    /**
     * Helper method to set up click listeners for CardViews that might not exist yet
     */
    private <T> void setupCardViewClickListener(CardView cardView, Class<T> activityClass, String activityName) {
        if (cardView != null) {
            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent intent = new Intent(MainActivity.this, activityClass);
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e("MainActivity", "Couldn't open " + activityName + ": " + e.getMessage(), e);
                        Toast.makeText(MainActivity.this,
                                "Couldn't open " + activityName + ". Please try again later.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void showDatePickerDialog() {
        // Create Calendar instance from sobriety start date
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(sobrietyTracker.getSobrietyStartDate());
    
        // Create DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year, month, dayOfMonth);
    
                        // Save the new date using SobrietyTracker
                        sobrietyTracker.setSobrietyStartDate(selectedDate.getTimeInMillis());
    
                        // Update milestone dates
                        achievementManager.updateMilestoneDates(sobrietyTracker.getSobrietyStartDate());
                        
                        // Update UI
                        updateSobrietyInfo();
    
                        // Check achievements for new day count
                        updateAchievements();
    
                        // Reschedule notifications based on new date
                        NotificationHelper.rescheduleNotifications(MainActivity.this, sobrietyTracker.getSobrietyStartDate());
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
    
        // Set max date to today
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void updateSobrietyInfo() {
        // Get days sober from SobrietyTracker
        int daysSober = sobrietyTracker.getDaysSober();
    
        // Update day count
        dayCountTextView.setText(String.valueOf(daysSober));
    
        // Update sober since text
        soberSinceTextView.setText("Sober since: " + dateFormat.format(new Date(sobrietyTracker.getSobrietyStartDate())));
    
        // Update next milestone info
        updateNextMilestone(daysSober);
    
        // Update motivation message
        updateMotivationMessage(daysSober);
    }

    /*private int getDaysSober() {
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
    }*/

    private void updateNextMilestone(int daysSober) {
        // Get next milestone from achievement manager
        Achievement nextMilestone = achievementManager.getNextMilestone(daysSober);
        
        if (nextMilestone != null) {
            int nextMilestoneDays = nextMilestone.getDaysRequired();
            int daysToNextMilestone = nextMilestoneDays - daysSober;
            
            // Find previous milestone
            int previousMilestoneDays = 0;
            for (Achievement achievement : achievementManager.getTimeMilestones()) {
                int days = achievement.getDaysRequired();
                if (days < nextMilestoneDays && days > previousMilestoneDays && days <= daysSober) {
                    previousMilestoneDays = days;
                }
            }
            
            int totalDaysInMilestone = nextMilestoneDays - previousMilestoneDays;
            int progressDays = daysSober - previousMilestoneDays;
            
            // Calculate progress percentage
            int progressPercentage = (int)(((float)progressDays / totalDaysInMilestone) * 100);
            
            // Update progress bar with the new ID
            if (achievementMilestoneProgressBar != null) {
                achievementMilestoneProgressBar.setProgress(progressPercentage);
            }
            
            // Update next milestone text
            if (daysToNextMilestone == 0) {
                // Today is a milestone!
                nextMilestoneTextView.setText("Today is a milestone day! 🎉");
            } else {
                nextMilestoneTextView.setText("Next milestone: " + nextMilestoneDays + " days\n" +
                        daysToNextMilestone + " days to go!");
            }
        } else {
            // No predefined milestones left, calculate next year milestone
            int yearsSober = daysSober / 365;
            int nextYearMilestone = (yearsSober + 1) * 365;
            int daysToNextYearMilestone = nextYearMilestone - daysSober;
            
            // Update next milestone text
            nextMilestoneTextView.setText("Next milestone: " + nextYearMilestone + " days\n" +
                    daysToNextYearMilestone + " days to go!");
            
            // Update progress bar with the new ID
            if (achievementMilestoneProgressBar != null) {
                int progressPercentage = (int)(((float)(daysSober % 365) / 365) * 100);
                achievementMilestoneProgressBar.setProgress(progressPercentage);
            }
        }
    }

    private void updateMotivationMessage(int daysSober) {
        // Get a motivational quote from the QuoteManager
        QuoteManager quoteManager = QuoteManager.getInstance(this);
        Quote quote = quoteManager.getRandomQuoteByCategory(Quote.CATEGORY_MOTIVATION);

        if (quote != null) {
            motivationTextView.setText("\"" + quote.getText() + "\" — " + quote.getAuthor());
        } else {
            // Fallback to default messages if no quotes available
            String[] motivationalMessages = {
                    "Every day sober is a victory.",
                    "You are stronger than your addiction.",
                    "One day at a time, you're building a better life.",
                    "Be proud of your progress, no matter how small.",
                    "Your sobriety journey inspires others.",
                    "Each sober day makes you stronger.",
                    "Recovery isn't easy, but it's worth it.",
                    "Focus on today, not tomorrow's challenges.",
                    "Your future self thanks you for staying sober today.",
                    "Celebrate your strength and resilience."
            };

            // Select a message based on the day count (cycling through the array)
            int index = daysSober % motivationalMessages.length;
            motivationTextView.setText(motivationalMessages[index]);
        }
    }

    private void updateAchievements() {
        // Get days sober from SobrietyTracker
        int daysSober = sobrietyTracker.getDaysSober();
        
        // Check for achievement unlocks based on current day count
        Achievement todaysMilestone = achievementManager.checkTimeAchievements(daysSober);
        
        // Show celebration dialog if today is a milestone
        if (todaysMilestone != null) {
            achievementManager.showMilestoneCelebration(this, todaysMilestone);
        }
    
        // Also check financial achievements
        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(this);
        float drinkCost = databaseHelper.getFloatSetting("drink_cost", 8.50f);
        int drinksPerWeek = databaseHelper.getIntSetting("drinks_per_week", 15);
    
        // Use SobrietyTracker to calculate money saved instead of calculating it here
        float moneySaved = sobrietyTracker.calculateMoneySaved(drinkCost, drinksPerWeek);
    
        achievementManager.checkFinancialAchievements(moneySaved);
    }
    
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.nav_settings) {
            // Open Settings activity
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_about) {
            // Open About activity
            Intent intent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(intent);
        }
        
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
    
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}