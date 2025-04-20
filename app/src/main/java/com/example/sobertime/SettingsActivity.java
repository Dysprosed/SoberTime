package com.example.sobertime;

import android.app.TimePickerDialog;
import android.content.DialogInterface; 
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import android.app.DatePickerDialog;
import android.util.Log;
import java.util.Date;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.appcompat.widget.SwitchCompat;
import com.example.sobertime.BaseActivity;
import com.example.sobertime.model.SobrietyTracker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends BaseActivity {

    private Switch masterNotificationSwitch;
    private Switch morningNotificationSwitch;
    private Switch eveningNotificationSwitch;
    private Switch milestoneNotificationSwitch;
    private Button addCustomTimeButton;
    private Button resetAppButton;
    private LinearLayout customTimesContainer;

    // Drink settings
    private TextView drinkCostText;
    private TextView drinksPerWeekText;
    private TextView caloriesPerDrinkText;
    private CardView drinkCostCard;
    private CardView drinksPerWeekCard;
    private CardView caloriesPerDrinkCard;
    private TextView sobrietyDateText;
    private CardView sobrietyDateCard;

    // Backup/Restore
    private Button backupRestoreButton;

    // Theme toggle - will be null if not in layout
    private Switch themeToggleSwitch;

    private static final String PREFS_NAME = "SobrietyNotificationPrefs";
    private static final String NOTIFICATIONS_ENABLED_KEY = "notifications_enabled";
    private static final String MORNING_ENABLED_KEY = "morning_notification_enabled";
    private static final String EVENING_ENABLED_KEY = "evening_notification_enabled";
    private static final String MILESTONE_ENABLED_KEY = "milestone_notification_enabled";
    private static final String CUSTOM_TIMES_KEY = "custom_notification_times";
    private static final String NIGHT_MODE_KEY = "night_mode_enabled";
    private static final String SOBRIETY_PREFS_NAME = "SobrietyTrackerPrefs";
    private static final String START_DATE_KEY = "sobriety_start_date";

    private SharedPreferences preferences;
    private List<String> customTimes;
    private DatabaseHelper databaseHelper;
    private SobrietyTracker sobrietyTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize SobrietyTracker
        sobrietyTracker = SobrietyTracker.getInstance(this);

        // Set up action bar with back button
        try {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Settings");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialize preferences and database
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        databaseHelper = DatabaseHelper.getInstance(this);

        // Initialize views
        initializeViews();

        // Load saved settings
        loadSettings();

        // Set up listeners
        setupListeners();
    }

    private void initializeViews() {
        try {
            // Notification settings
            masterNotificationSwitch = findViewById(R.id.masterNotificationSwitch);
            morningNotificationSwitch = findViewById(R.id.morningNotificationSwitch);
            eveningNotificationSwitch = findViewById(R.id.eveningNotificationSwitch);
            milestoneNotificationSwitch = findViewById(R.id.milestoneNotificationSwitch);
            addCustomTimeButton = findViewById(R.id.addCustomTimeButton);
            customTimesContainer = findViewById(R.id.customTimesContainer);

            addIntrusiveNotificationSettings();

            // Drink settings
            drinkCostText = findViewById(R.id.drinkCostText);
            drinksPerWeekText = findViewById(R.id.drinksPerWeekText);
            caloriesPerDrinkText = findViewById(R.id.caloriesPerDrinkText);
            drinkCostCard = findViewById(R.id.drinkCostCard);
            drinksPerWeekCard = findViewById(R.id.drinksPerWeekCard);
            caloriesPerDrinkCard = findViewById(R.id.caloriesPerDrinkCard);

            // Sobriety date
            sobrietyDateText = findViewById(R.id.sobrietyDateText);
            sobrietyDateCard = findViewById(R.id.sobrietyDateCard);
            
            // Backup/Restore
            backupRestoreButton = findViewById(R.id.backupRestoreButton);

            // Reset App Button
            resetAppButton = findViewById(R.id.resetAppButton);

            // Theme toggle switch - may not exist yet
            try {
                themeToggleSwitch = findViewById(R.id.themeToggleSwitch);

                // Only set up theme toggle if it exists
                if (themeToggleSwitch != null) {
                    // Check if night mode is currently enabled
                    int nightModeFlags = getResources().getConfiguration().uiMode &
                            Configuration.UI_MODE_NIGHT_MASK;
                    boolean isNightMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
                    themeToggleSwitch.setChecked(isNightMode);
                }
            } catch (Exception e) {
                // It's okay if the theme toggle switch doesn't exist yet
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error initializing views: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addIntrusiveNotificationSettings() {
        // Get the container from layout
        LinearLayout notificationSettingsContainer = findViewById(R.id.notificationSettingsContainer);
        
        // Create section header
        TextView intrusiveTitle = new TextView(this);
        intrusiveTitle.setText("Intrusive Check-in Notifications");
        intrusiveTitle.setTextSize(18);
        intrusiveTitle.setTypeface(null, Typeface.BOLD);
        intrusiveTitle.setPadding(0, 32, 0, 16);
        notificationSettingsContainer.addView(intrusiveTitle);
        
        // IMPORTANT: Create the container for intrusive settings BEFORE using it
        final LinearLayout intrusiveSettingsContainer = new LinearLayout(this);
        intrusiveSettingsContainer.setOrientation(LinearLayout.VERTICAL);
        intrusiveSettingsContainer.setPadding(32, 0, 0, 16);
        
        // Get saved preference or default to true
        SharedPreferences prefs = getSharedPreferences("notification_settings", MODE_PRIVATE);
        boolean intrusiveEnabled = prefs.getBoolean("intrusive_notifications_enabled", true);
        
        // Set initial visibility
        intrusiveSettingsContainer.setVisibility(intrusiveEnabled ? View.VISIBLE : View.GONE);
        
        // Create intrusive notification switch
        SwitchCompat intrusiveNotificationSwitch = new SwitchCompat(this);
        intrusiveNotificationSwitch.setText("Enable Intrusive Notifications");
        intrusiveNotificationSwitch.setPadding(0, 16, 0, 16);
        intrusiveNotificationSwitch.setChecked(intrusiveEnabled);
        
        // Add the container to parent BEFORE setting up the listener that references it
        notificationSettingsContainer.addView(intrusiveNotificationSwitch);
        notificationSettingsContainer.addView(intrusiveSettingsContainer);
        
        // Add listener after the container is added to the view hierarchy
        intrusiveNotificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("intrusive_notifications_enabled", isChecked);
                editor.apply();
                
                // Now it's safe to access the container since it's properly initialized
                intrusiveSettingsContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                
                // Reschedule notifications based on new setting
                NotificationHelper.scheduleNotifications(SettingsActivity.this);
            }
        });
        
        // Volume setting
        TextView volumeLabel = new TextView(this);
        volumeLabel.setText("Alarm Volume");
        volumeLabel.setPadding(0, 16, 0, 8);
        intrusiveSettingsContainer.addView(volumeLabel);
        
        SeekBar volumeSeekBar = new SeekBar(this);
        volumeSeekBar.setMax(100);
        int savedVolume = prefs.getInt("alarm_volume", 100);
        volumeSeekBar.setProgress(savedVolume);
        
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    prefs.edit().putInt("alarm_volume", progress).apply();
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        intrusiveSettingsContainer.addView(volumeSeekBar);
        
        // Vibration setting
        SwitchCompat vibrationSwitch = new SwitchCompat(this);
        vibrationSwitch.setText("Enable Vibration");
        vibrationSwitch.setPadding(0, 16, 0, 16);
        vibrationSwitch.setChecked(prefs.getBoolean("use_vibration", true));
        
        vibrationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean("use_vibration", isChecked).apply();
            }
        });
        
        intrusiveSettingsContainer.addView(vibrationSwitch);
        
        // Check-in time picker
        TextView timePickerLabel = new TextView(this);
        timePickerLabel.setText("Check-in Time");
        timePickerLabel.setPadding(0, 16, 0, 8);
        intrusiveSettingsContainer.addView(timePickerLabel);
        
        Button timePickerButton = new Button(this);
        
        // Get saved hour and minute or default to 9:00 PM
        int savedHour = prefs.getInt("check_in_hour", 21);
        int savedMinute = prefs.getInt("check_in_minute", 0);
        
        // Format time for button text
        String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", savedHour, savedMinute);
        timePickerButton.setText(formattedTime);
        
        timePickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog timePickerDialog = new TimePickerDialog(
                        SettingsActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                // Save selected time
                                prefs.edit()
                                    .putInt("check_in_hour", hourOfDay)
                                    .putInt("check_in_minute", minute)
                                    .apply();
                                
                                // Update button text
                                String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                                timePickerButton.setText(time);
                                
                                // Reschedule notifications
                                NotificationHelper.scheduleNotifications(SettingsActivity.this);
                            }
                        },
                        savedHour,
                        savedMinute,
                        false
                );
                timePickerDialog.show();
            }
        });
        
        intrusiveSettingsContainer.addView(timePickerButton);
        
        // Add setting to control daily check-in enforcement
        SwitchCompat enforceDailyCheckinSwitch = new SwitchCompat(this);
        enforceDailyCheckinSwitch.setText("Enforce Daily Check-ins");
        enforceDailyCheckinSwitch.setPadding(0, 24, 0, 8);
        enforceDailyCheckinSwitch.setChecked(prefs.getBoolean("enforce_daily_checkins", true));
        
        // Description text for the setting
        TextView enforceDailyCheckinDescription = new TextView(this);
        enforceDailyCheckinDescription.setText("When enabled, intrusive check-ins will only happen once per day. When disabled, they will happen each time at the set check-in time regardless of previous check-ins.");
        enforceDailyCheckinDescription.setTextSize(12);
        enforceDailyCheckinDescription.setPadding(16, 0, 16, 16);
        
        enforceDailyCheckinSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean("enforce_daily_checkins", isChecked).apply();
                
                // Reschedule notifications to reflect new setting
                NotificationHelper.scheduleIntrusiveCheckInNotification(SettingsActivity.this);
            }
        });
        
        intrusiveSettingsContainer.addView(enforceDailyCheckinSwitch);
        intrusiveSettingsContainer.addView(enforceDailyCheckinDescription);
    }

    private void loadSettings() {
        try {
            // Load notification settings
            boolean notificationsEnabled = preferences.getBoolean(NOTIFICATIONS_ENABLED_KEY, true);
            masterNotificationSwitch.setChecked(notificationsEnabled);
    
            boolean morningEnabled = preferences.getBoolean(MORNING_ENABLED_KEY, true);
            boolean eveningEnabled = preferences.getBoolean(EVENING_ENABLED_KEY, true);
            boolean milestoneEnabled = preferences.getBoolean(MILESTONE_ENABLED_KEY, true);
    
            morningNotificationSwitch.setChecked(morningEnabled);
            eveningNotificationSwitch.setChecked(eveningEnabled);
            milestoneNotificationSwitch.setChecked(milestoneEnabled);
    
            // Update enabled state based on master switch
            updateNotificationSwitchesState(notificationsEnabled);
    
            // Load custom notification times
            String customTimesString = preferences.getString(CUSTOM_TIMES_KEY, "");
            if (!customTimesString.isEmpty()) {
                customTimes = new ArrayList<>(Arrays.asList(customTimesString.split(",")));
            } else {
                customTimes = new ArrayList<>();
            }
    
            // Display custom times
            displayCustomTimes();
    
            // Load drink settings
            float drinkCost = databaseHelper.getFloatSetting("drink_cost", 8.50f);
            int drinksPerWeek = databaseHelper.getIntSetting("drinks_per_week", 15);
            int caloriesPerDrink = databaseHelper.getIntSetting("calories_per_drink", 150);
    
            drinkCostText.setText(String.format(Locale.getDefault(), "$%.2f", drinkCost));
            drinksPerWeekText.setText(String.valueOf(drinksPerWeek));
            caloriesPerDrinkText.setText(String.valueOf(caloriesPerDrink));
    
            // Add null checks for these views
            if (drinkCostText != null) {
                drinkCostText.setText(String.format(Locale.getDefault(), "$%.2f", drinkCost));
            }
            if (drinksPerWeekText != null) {
                drinksPerWeekText.setText(String.valueOf(drinksPerWeek));
            }
            if (caloriesPerDrinkText != null) {
                caloriesPerDrinkText.setText(String.valueOf(caloriesPerDrink));
            }

            // Load sobriety date from SobrietyTracker instead of directly from SharedPreferences
            long sobrietyStartDate = sobrietyTracker.getSobrietyStartDate();
            sobrietyDateText.setText(new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(new Date(sobrietyStartDate)));
    
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupListeners() {
        try {
            // Master notification switch listener
            if (masterNotificationSwitch != null) {
                masterNotificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean(NOTIFICATIONS_ENABLED_KEY, isChecked);
                        editor.apply();
    
                        // Update enabled state of other switches
                        updateNotificationSwitchesState(isChecked);
    
                        if (isChecked) {
                            // Re-schedule notifications
                            NotificationHelper.scheduleNotifications(SettingsActivity.this);
                            Toast.makeText(SettingsActivity.this, "Notifications enabled", Toast.LENGTH_SHORT).show();
                        } else {
                            // Cancel all notifications
                            NotificationHelper.cancelAllNotifications(SettingsActivity.this);
                            Toast.makeText(SettingsActivity.this, "All notifications disabled", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
    
            // Notification type switches
            if (morningNotificationSwitch != null) {
                morningNotificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!buttonView.isEnabled()) return;
    
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean(MORNING_ENABLED_KEY, isChecked);
                        editor.apply();
    
                        // Reschedule notifications
                        NotificationHelper.scheduleNotifications(SettingsActivity.this);
                    }
                });
            }
    
            if (eveningNotificationSwitch != null) {
                eveningNotificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!buttonView.isEnabled()) return;
    
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean(EVENING_ENABLED_KEY, isChecked);
                        editor.apply();
    
                        // Reschedule notifications
                        NotificationHelper.scheduleNotifications(SettingsActivity.this);
                    }
                });
            }
    
            if (milestoneNotificationSwitch != null) {
                milestoneNotificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!buttonView.isEnabled()) return;
    
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean(MILESTONE_ENABLED_KEY, isChecked);
                        editor.apply();
    
                        // Reschedule notifications
                        NotificationHelper.scheduleNotifications(SettingsActivity.this);
                    }
                });
            }
    
            // Theme toggle switch listener - only if it exists
            if (themeToggleSwitch != null) {
                themeToggleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        // Save preference
                        SharedPreferences prefs = getSharedPreferences("SobrietyTrackerPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("night_mode_enabled", isChecked);
                        editor.apply();
    
                        // Apply theme change
                        if (isChecked) {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        } else {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        }
    
                        // Recreate activity to apply theme
                        recreate();
                    }
                });
            }
    
            // Add custom time button
            if (addCustomTimeButton != null) {
                addCustomTimeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showTimePickerDialog();
                    }
                });
            }
    
            // Drink setting cards
            if (drinkCostCard != null) {
                drinkCostCard.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showDrinkCostDialog();
                    }
                });
            }
    
            if (drinksPerWeekCard != null) {
                drinksPerWeekCard.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showDrinksPerWeekDialog();
                    }
                });
            }
    
            if (caloriesPerDrinkCard != null) {
                caloriesPerDrinkCard.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showCaloriesPerDrinkDialog();
                    }
                });
            }
    
            // Sobriety date card
            if (sobrietyDateCard != null) {
                sobrietyDateCard.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showDatePickerDialog();
                    }
                });
            }
    
            // Backup/Restore button
            if (backupRestoreButton != null) {
                backupRestoreButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(SettingsActivity.this, BackupRestoreActivity.class);
                        startActivity(intent);
                    }
                });
            }
    
            // Reset App button
            if (resetAppButton != null) {
                resetAppButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showResetConfirmationDialog();
                    }
                });
            }
    
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error setting up listeners: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showResetConfirmationDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Reset App Data")
            .setMessage("This will reset ALL app data including your sobriety date, journal entries, and settings. This cannot be undone. Are you sure?")
            .setPositiveButton("Reset Everything", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    resetAppData();
                }
            })
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }

    private void resetAppData() {
        try {
            // 1. Clear all SharedPreferences
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().apply();
            getSharedPreferences(SOBRIETY_PREFS_NAME, MODE_PRIVATE).edit().clear().apply();
            getSharedPreferences("SobrietyTrackerPrefs", MODE_PRIVATE).edit().clear().apply();
            
            // 2. Reset database
            databaseHelper.resetAllData();
    
            // 3. Reset sobriety tracker date to today
            sobrietyTracker.setSobrietyStartDate(System.currentTimeMillis());
    
            // 4. Reset onboarding status to ensure welcome screen appears
            getSharedPreferences("SobrietyTrackerPrefs", MODE_PRIVATE)
                .edit()
                .putBoolean("onboarding_complete", false)
                .apply();
    
            // 5. Show success message
            Toast.makeText(this, "All data has been reset", Toast.LENGTH_SHORT).show();
            
            // 6. Restart app from Welcome Activity
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finishAffinity(); // Close all activities in this app
        } catch (Exception e) {
            Log.e("SettingsActivity", "Error resetting app data", e);
            Toast.makeText(this, "Error resetting app: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showDatePickerDialog() {
        try {
            // Get current sobriety date from SobrietyTracker
            long currentDate = sobrietyTracker.getSobrietyStartDate();
    
            // Set up calendar with current date
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(currentDate);
    
            // Create DatePickerDialog using lambda
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        Calendar newDate = Calendar.getInstance();
                        newDate.set(selectedYear, selectedMonth, selectedDay);
    
                        // Save the new date using SobrietyTracker
                        long newStartDate = newDate.getTimeInMillis();
                        sobrietyTracker.setSobrietyStartDate(newStartDate);
    
                        // Update UI
                        String formattedDate = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(new Date(newStartDate));
                        sobrietyDateText.setText(formattedDate);
    
                        // Reschedule notifications
                        NotificationHelper.rescheduleNotifications(SettingsActivity.this, newStartDate);
    
                        Toast.makeText(SettingsActivity.this, "Sobriety start date updated", Toast.LENGTH_SHORT).show();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
    
            // Set max date to today
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePickerDialog.show();
        } catch (Exception e) {
            Log.e("SettingsActivity", "Error showing date picker", e);
            Toast.makeText(this, "Error showing date picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateNotificationSwitchesState(boolean enabled) {
        try {
            morningNotificationSwitch.setEnabled(enabled);
            eveningNotificationSwitch.setEnabled(enabled);
            milestoneNotificationSwitch.setEnabled(enabled);
            addCustomTimeButton.setEnabled(enabled);

            // Update custom time views
            for (int i = 0; i < customTimesContainer.getChildCount(); i++) {
                View child = customTimesContainer.getChildAt(i);
                Button deleteButton = child.findViewById(R.id.deleteCustomTimeButton);
                if (deleteButton != null) {
                    deleteButton.setEnabled(enabled);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showDrinkCostDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Average Cost Per Drink");

        // Get current value
        float currentCost = databaseHelper.getFloatSetting("drink_cost", 8.50f);

        // Create input field
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.format(Locale.getDefault(), "%.2f", currentCost));
        builder.setView(input);

        // Set up buttons
        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                float newCost = Float.parseFloat(input.getText().toString());
                databaseHelper.setSetting("drink_cost", String.valueOf(newCost));
                drinkCostText.setText(String.format(Locale.getDefault(), "$%.2f", newCost));
                Toast.makeText(SettingsActivity.this, "Drink cost updated", Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(SettingsActivity.this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showDrinksPerWeekDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Average Drinks Per Week");

        // Get current value
        int currentCount = databaseHelper.getIntSetting("drinks_per_week", 15);

        // Create input field
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(currentCount));
        builder.setView(input);

        // Set up buttons
        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                int newCount = Integer.parseInt(input.getText().toString());
                databaseHelper.setSetting("drinks_per_week", String.valueOf(newCount));
                drinksPerWeekText.setText(String.valueOf(newCount));
                Toast.makeText(SettingsActivity.this, "Drinks per week updated", Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(SettingsActivity.this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showCaloriesPerDrinkDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Average Calories Per Drink");

        // Get current value
        int currentCalories = databaseHelper.getIntSetting("calories_per_drink", 150);

        // Create input field
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(currentCalories));
        builder.setView(input);

        // Set up buttons
        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                int newCalories = Integer.parseInt(input.getText().toString());
                databaseHelper.setSetting("calories_per_drink", String.valueOf(newCalories));
                caloriesPerDrinkText.setText(String.valueOf(newCalories));
                Toast.makeText(SettingsActivity.this, "Calories per drink updated", Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(SettingsActivity.this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showTimePickerDialog() {
        try {
            // Get current time
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            // Show time picker dialog
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    this,
                    new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            // Format time string (HH:MM)
                            String timeString = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);

                            // Check if time already exists
                            if (customTimes.contains(timeString)) {
                                Toast.makeText(SettingsActivity.this, "This time already exists", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // Add to list and save
                            customTimes.add(timeString);
                            saveCustomTimes();

                            // Display updated list
                            displayCustomTimes();

                            // Schedule the new notification
                            NotificationHelper.scheduleNotifications(SettingsActivity.this);

                            Toast.makeText(SettingsActivity.this, "Custom notification time added", Toast.LENGTH_SHORT).show();
                        }
                    },
                    hour,
                    minute,
                    false
            );

            timePickerDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error showing time picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void displayCustomTimes() {
        try {
            // Clear existing views
            customTimesContainer.removeAllViews();

            // Sort times for consistent display
            Collections.sort(customTimes);

            // Add a view for each custom time
            for (final String timeString : customTimes) {
                View customTimeView = getLayoutInflater().inflate(R.layout.item_custom_time, null);

                TextView timeTextView = customTimeView.findViewById(R.id.customTimeText);
                Button deleteButton = customTimeView.findViewById(R.id.deleteCustomTimeButton);

                // Format time for display
                String[] timeParts = timeString.split(":");
                int hour = Integer.parseInt(timeParts[0]);
                int minute = Integer.parseInt(timeParts[1]);

                String amPm = hour >= 12 ? "PM" : "AM";
                int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);

                String displayTime = String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, amPm);
                timeTextView.setText(displayTime);

                // Set delete button listener
                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        customTimes.remove(timeString);
                        saveCustomTimes();
                        displayCustomTimes();

                        // Reschedule notifications (this will remove the deleted one)
                        NotificationHelper.scheduleNotifications(SettingsActivity.this);

                        Toast.makeText(SettingsActivity.this, "Custom time removed", Toast.LENGTH_SHORT).show();
                    }
                });

                // Add to container
                customTimesContainer.addView(customTimeView);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error displaying custom times: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveCustomTimes() {
        try {
            // Convert list to comma-separated string
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < customTimes.size(); i++) {
                sb.append(customTimes.get(i));
                if (i < customTimes.size() - 1) {
                    sb.append(",");
                }
            }

            // Save to preferences
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(CUSTOM_TIMES_KEY, sb.toString());
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving custom times: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

}