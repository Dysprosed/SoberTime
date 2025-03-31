package com.example.sobertime;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
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

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private TextView dayCountTextView;
    private TextView soberSinceTextView;
    private TextView nextMilestoneTextView;
    private TextView motivationTextView;
    private CardView milestonesCardView;
    private CardView statsCardView;

    // These are the new CardViews which might not exist yet
    private CardView journalCardView;
    private CardView achievementsCardView;
    private CardView emergencyHelpCardView;
    private CardView inspirationCardView;
    private CardView communityCardView;
    private CardView progressReportCardView;

    private Button resetDateButton;
    private ProgressBar milestoneProgressBar;
    
    // Drawer elements
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navigationView;

    private SharedPreferences preferences;
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
        
        // Set up toolbar and drawer
        setupToolbarAndDrawer();

        // Initialize preferences
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Get sobriety start date from preferences, or use today's date as default
        sobrietyStartDate = preferences.getLong(START_DATE_KEY, Calendar.getInstance().getTimeInMillis());

        // Set up permission launcher
        setupPermissions();

        // Initialize UI elements
        initializeViews();
        setupClickListeners();

        // Initialize achievement manager
        achievementManager = AchievementManager.getInstance(this);

        // Update UI with current data
        updateSobrietyInfo();

        // Check achievements for current day count
        updateAchievements();
    }
    
    private void setupToolbarAndDrawer() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        
        navigationView.setNavigationItemSelectedListener(this);
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
        milestonesCardView = findViewById(R.id.milestonesCardView);
        statsCardView = findViewById(R.id.statsCardView);
        resetDateButton = findViewById(R.id.resetDateButton);

        // Safely try to find the milestone progress bar
        milestoneProgressBar = findProgressBarSafely(R.id.milestoneProgressBar);

        // Safely try to find all CardViews - no compile errors whether they exist or not
        journalCardView = findCardViewSafely(R.id.journalCardView);
        achievementsCardView = findCardViewSafely(R.id.achievementsCardView);
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
                Log.d("MainActivity", "View with id " + id + " (" + resourceName + ") is not a CardView");
                return null;
            }
        } catch (Exception e) {
            try {
                String resourceName = getResources().getResourceEntryName(id);
                Log.d("MainActivity", "View with id " + id + " (" + resourceName + ") not found");
            } catch (Exception e2) {
                Log.d("MainActivity", "View with id " + id + " not found. Resource name could not be retrieved.");
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
                Log.d("MainActivity", "View with id " + id + " (" + resourceName + ") not found");
            } catch (Exception e2) {
                Log.d("MainActivity", "View with id " + id + " not found, and resource name could not be retrieved");
            }
            return null;
        }
    }

    // Method to setup all card click listeners
    private void setupClickListeners() {
        milestonesCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // Open milestones activity
                    Intent intent = new Intent(MainActivity.this, MilestonesActivity.class);
                    intent.putExtra("start_date", sobrietyStartDate);
                    startActivity(intent);
                } catch (Exception e) {
                    // Log error and show toast to user
                    Log.e("MainActivity", "Couldn't open Milestones", e);
                    Toast.makeText(MainActivity.this,
                            "Couldn't open Milestones: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        statsCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // Open health benefits activity
                    Intent intent = new Intent(MainActivity.this, HealthBenefitsActivity.class);
                    intent.putExtra("days_sober", getDaysSober());
                    startActivity(intent);
                } catch (Exception e) {
                    // Log error and show toast to user
                    Log.e("MainActivity", "Couldn't open Health Benefits", e);
                    Toast.makeText(MainActivity.this,
                            "Couldn't open Health Benefits: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Initialize click listeners for optional CardViews
        setupCardViewClickListener(journalCardView, JournalActivity.class, "Journal");
        setupCardViewClickListener(achievementsCardView, AchievementsActivity.class, "Achievements");
        setupCardViewClickListener(emergencyHelpCardView, EmergencyHelpActivity.class, "Emergency Help");
        setupCardViewClickListener(inspirationCardView, InspirationActivity.class, "Inspirational Quotes");
        setupCardViewClickListener(communityCardView, CommunitySupportActivity.class, "Community Support");
        setupCardViewClickListener(progressReportCardView, ProgressReportActivity.class, "Progress Report");

        resetDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    showDatePickerDialog();
                } catch (Exception e) {
                    Log.e("MainActivity", "Failed to show DatePickerDialog", e);
                    Toast.makeText(MainActivity.this,
                            "An error occurred while opening the date picker. Please try again.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
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
        // Create Calendar instance from sobriety start date or today's date
        Calendar calendar = Calendar.getInstance();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int userStatus = prefs.getInt("current_status", 0);
        
        if (userStatus != 2) { // If not in "seeking" status
            calendar.setTimeInMillis(sobrietyStartDate);
        }
    
        // Create DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year, month, dayOfMonth);
    
                        // Save the new date
                        sobrietyStartDate = selectedDate.getTimeInMillis();
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putLong(START_DATE_KEY, sobrietyStartDate);
                        
                        // Update user status to "sober" (1) if they were previously "seeking" (2)
                        if (userStatus == 2) {
                            editor.putInt("current_status", 1); // Set to sober status
                            Toast.makeText(MainActivity.this, 
                                    "Congratulations on starting your sobriety journey!", 
                                    Toast.LENGTH_LONG).show();
                        }
                        
                        editor.apply();
    
                        // Update UI
                        updateSobrietyInfo();
    
                        // Check achievements for new day count
                        updateAchievements();
    
                        // Reschedule notifications based on new date
                        NotificationHelper.rescheduleNotifications(MainActivity.this, sobrietyStartDate);
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
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int userStatus = prefs.getInt("current_status", 0); // 0 = unknown, 1 = sober, 2 = seeking
        
        if (userStatus == 2) { // User is seeking support, not yet sober
            // Show special UI for users who haven't started sobriety yet
            dayCountTextView.setText("--");
            soberSinceTextView.setText("Ready to begin your journey?");
            nextMilestoneTextView.setText("Your first milestone awaits! Set your sobriety date to begin tracking.");
            
            // Set progress bar to 0 if it exists
            if (milestoneProgressBar != null) {
                milestoneProgressBar.setProgress(0);
            }
            
            // Change the button text to reflect starting vs. changing date
            resetDateButton.setText("SET SOBRIETY DATE");
            
            // Update motivation message for those still considering sobriety
            motivationTextView.setText("The journey of a thousand miles begins with a single step.");
            
        } else {
            // Regular sobriety tracking
            int daysSober = getDaysSober();
    
            // Update day count
            dayCountTextView.setText(String.valueOf(daysSober));
    
            // Update sober since text
            soberSinceTextView.setText("Sober since: " + dateFormat.format(new Date(sobrietyStartDate)));
    
            // Update next milestone info
            updateNextMilestone(daysSober);
    
            // Update motivation message
            updateMotivationMessage(daysSober);
            
            // Ensure button has the right text
            resetDateButton.setText("CHANGE START DATE");
        }
    }

    private int getDaysSober() {
        long currentTime = System.currentTimeMillis();
        long diffInMillis = currentTime - sobrietyStartDate;
        return (int) TimeUnit.MILLISECONDS.toDays(diffInMillis);
    }

    private void updateNextMilestone(int daysSober) {
        int[] milestones = {1, 7, 14, 30, 60, 90, 180, 365, 730, 1095};
        int nextMilestone = -1;
        int previousMilestone = 0;

        for (int milestone : milestones) {
            if (daysSober < milestone) {
                nextMilestone = milestone;
                break;
            }
            previousMilestone = milestone;
        }

        if (nextMilestone == -1) {
            // If we've passed all predefined milestones, calculate the next year milestone
            int yearsSober = daysSober / 365;
            nextMilestone = (yearsSober + 1) * 365;
            previousMilestone = yearsSober * 365;
        }

        int daysToNextMilestone = nextMilestone - daysSober;
        int totalDaysInMilestone = nextMilestone - previousMilestone;
        int progressDays = daysSober - previousMilestone;

        // Calculate progress percentage
        int progressPercentage = (int)(((float)progressDays / totalDaysInMilestone) * 100);

        // Update progress bar if it exists
        if (milestoneProgressBar != null) {
            milestoneProgressBar.setProgress(progressPercentage);
        }

        if (daysToNextMilestone == 0) {
            // Today is a milestone!
            nextMilestoneTextView.setText("Today is a milestone day! 🎉");

            // Show celebration dialog
            MilestoneCelebration.showCelebrationDialog(this, daysSober);
        } else {
            nextMilestoneTextView.setText("Next milestone: " + nextMilestone + " days\n" +
                    daysToNextMilestone + " days to go!");
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
        // Check for achievement unlocks based on current day count
        AchievementManager achievementManager = AchievementManager.getInstance(this);
        achievementManager.checkTimeAchievements(getDaysSober());

        // Also check financial achievements
        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(this);
        float drinkCost = databaseHelper.getFloatSetting("drink_cost", 8.50f);
        int drinksPerWeek = databaseHelper.getIntSetting("drinks_per_week", 15);

        float drinksAvoided = (getDaysSober() / 7.0f) * drinksPerWeek;
        float moneySaved = drinksAvoided * drinkCost;

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