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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import android.util.Log;

import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

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

    private FloatingActionButton settingsButton;
    private Button resetDateButton;
    private ProgressBar milestoneProgressBar;

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

        // Change Button to FloatingActionButton to fix the class cast exception
        settingsButton = findViewById(R.id.settingsButton);
        resetDateButton = findViewById(R.id.resetDateButton);

        // Safely try to find the milestone progress bar
        milestoneProgressBar = findViewSafely(R.id.milestoneProgressBar);

        // Safely try to find all CardViews - no compile errors whether they exist or not
        journalCardView = findCardViewSafely(R.id.journalCardView);
        achievementsCardView = findCardViewSafely(R.id.achievementsCardView);
        emergencyHelpCardView = findCardViewSafely(R.id.emergencyHelpCardView);
        inspirationCardView = findCardViewSafely(R.id.inspirationCardView);
        communityCardView = findCardViewSafely(R.id.communityCardView);
        progressReportCardView = findCardViewSafely(R.id.progressReportCardView);
    }

    // Helper method to safely find CardViews
    private CardView findCardViewSafely(int resId) {
        try {
            return findViewById(resId);
        } catch (Exception e) {
            Log.d("MainActivity", "CardView with id " + resId + " not found");
            return null;
        }
    }

    // Generic helper method for any type of view
    private <T extends View> T findViewSafely(int resId) {
        try {
            return findViewById(resId);
        } catch (Exception e) {
            Log.d("MainActivity", "View with id " + resId + " not found");
            return null;
        }
    }

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
                    e.printStackTrace();
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
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this,
                            "Couldn't open Health Benefits: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Set up click listeners for the new CardViews only if they exist
        if (journalCardView != null) {
            journalCardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        // Open journal activity
                        Intent intent = new Intent(MainActivity.this, JournalActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        // Log error and show toast to user
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this,
                                "Couldn't open Journal: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

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
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this,
                                "Couldn't open Achievements: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (emergencyHelpCardView != null) {
            emergencyHelpCardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        // Open emergency help activity
                        Intent intent = new Intent(MainActivity.this, EmergencyHelpActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        // Log error and show toast to user
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this,
                                "Couldn't open Emergency Help: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (inspirationCardView != null) {
            inspirationCardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        // Open inspiration activity
                        Intent intent = new Intent(MainActivity.this, InspirationActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        // Log error and show toast to user
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this,
                                "Couldn't open Inspirational Quotes: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (communityCardView != null) {
            communityCardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        // Open community support activity
                        Intent intent = new Intent(MainActivity.this, CommunitySupportActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        // Log error and show toast to user
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this,
                                "Couldn't open Community Support: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (progressReportCardView != null) {
            progressReportCardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        // Open progress report activity
                        Intent intent = new Intent(MainActivity.this, ProgressReportActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        // Log error and show toast to user
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this,
                                "Couldn't open Progress Report: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // Open settings activity
                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    // Log error and show toast to user
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this,
                            "Couldn't open Settings: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        resetDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });
    }

    private void showDatePickerDialog() {
        // Create Calendar instance from sobriety start date
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(sobrietyStartDate);

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
        int daysSober = getDaysSober();

        // Update day count
        dayCountTextView.setText(String.valueOf(daysSober));

        // Update sober since text
        soberSinceTextView.setText("Sober since: " + dateFormat.format(new Date(sobrietyStartDate)));

        // Update next milestone info
        updateNextMilestone(daysSober);

        // Update motivation message
        updateMotivationMessage(daysSober);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_about) {
            // Open About activity
            Intent intent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}