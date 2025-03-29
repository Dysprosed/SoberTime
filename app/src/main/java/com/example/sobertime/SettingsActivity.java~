package com.example.sobertime;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private Switch masterNotificationSwitch;
    private Switch morningNotificationSwitch;
    private Switch eveningNotificationSwitch;
    private Switch milestoneNotificationSwitch;
    private Button addCustomTimeButton;
    private LinearLayout customTimesContainer;

    // Drink settings
    private TextView drinkCostText;
    private TextView drinksPerWeekText;
    private TextView caloriesPerDrinkText;
    private CardView drinkCostCard;
    private CardView drinksPerWeekCard;
    private CardView caloriesPerDrinkCard;

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

    private SharedPreferences preferences;
    private List<String> customTimes;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Set up action bar with back button
        try {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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

            // Drink settings
            drinkCostText = findViewById(R.id.drinkCostText);
            drinksPerWeekText = findViewById(R.id.drinksPerWeekText);
            caloriesPerDrinkText = findViewById(R.id.caloriesPerDrinkText);
            drinkCostCard = findViewById(R.id.drinkCostCard);
            drinksPerWeekCard = findViewById(R.id.drinksPerWeekCard);
            caloriesPerDrinkCard = findViewById(R.id.caloriesPerDrinkCard);

            // Backup/Restore
            backupRestoreButton = findViewById(R.id.backupRestoreButton);

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

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupListeners() {
        try {
            // Master notification switch listener
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

            // Notification type switches
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

            // Theme toggle switch listener - only if it exists
            // In your setupListeners() method, add this for theme switch
            if (themeToggleSwitch != null) {
                themeToggleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        // Save preference in main preferences
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

                        // Recreate activity for theme to take effect
                        recreate();
                    }
                });
            }

            // Add custom time button
            addCustomTimeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showTimePickerDialog();
                }
            });

            // Drink setting cards
            drinkCostCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDrinkCostDialog();
                }
            });

            drinksPerWeekCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDrinksPerWeekDialog();
                }
            });

            caloriesPerDrinkCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCaloriesPerDrinkDialog();
                }
            });

            // Backup/Restore button
            backupRestoreButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(SettingsActivity.this, BackupRestoreActivity.class);
                    startActivity(intent);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error setting up listeners: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}