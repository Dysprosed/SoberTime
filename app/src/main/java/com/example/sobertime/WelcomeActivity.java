package com.example.sobertime;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.sobertime.model.SobrietyTracker; 

import java.util.Calendar;

public class WelcomeActivity extends AppCompatActivity {

    private CardView yesSoberCardView;
    private CardView notSoberCardView;
    private TextView skipTextView;

    private SobrietyTracker sobrietyTracker;
    private static final String PREFS_NAME = "SobrietyTrackerPrefs";
    private static final String ONBOARDING_COMPLETE_KEY = "onboarding_complete";
    private static final String START_DATE_KEY = "sobriety_start_date";
    private static final String CURRENT_STATUS_KEY = "current_status";
    
    // Status values
    private static final int STATUS_SOBER = 1;
    private static final int STATUS_SEEKING = 2;
    private static final int STATUS_UNKNOWN = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize SobrietyTracker
        sobrietyTracker = SobrietyTracker.getInstance(this);
        
        // Check if onboarding is complete
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean onboardingComplete = preferences.getBoolean(ONBOARDING_COMPLETE_KEY, false);
        
        if (onboardingComplete) {
            // Skip to appropriate screen based on saved status
            navigateBasedOnStatus();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_welcome);
        
        // Initialize views
        yesSoberCardView = findViewById(R.id.yesSoberCardView);
        notSoberCardView = findViewById(R.id.notSoberCardView);
        skipTextView = findViewById(R.id.skipTextView);
        
        // Set up click listeners
        setupClickListeners();
    }

    private void saveUserStatus(int status) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(CURRENT_STATUS_KEY, status);
        editor.putBoolean(ONBOARDING_COMPLETE_KEY, true);
        editor.apply();
    }
    
    private void setupClickListeners() {
        // Yes button - show sobriety date dialog then date picker
        yesSoberCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSobrietyDatePrompt();
            }
        });
        
        // No button - show support message and go to community support
        notSoberCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Important: Don't save status yet - we'll only save it if they decide to stay
                showSupportMessage();
            }
        });
        
        // Skip button - go to main activity without saving status
        skipTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserStatus(STATUS_UNKNOWN);
                goToMainActivity();
            }
        });
    }
    
    private void showSobrietyDatePrompt() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Your Sobriety Date")
            .setMessage("Please select the date when you began your sobriety journey. This will help us track your progress and milestones.")
            .setPositiveButton("Select Date", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    showDatePickerDialog();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void showDatePickerDialog() {
        // Get current date
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        
        // Create DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        // Create calendar with selected date
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year, month, dayOfMonth);
                        
                        // Save the date using SobrietyTracker and set status
                        sobrietyTracker.setSobrietyStartDate(selectedDate.getTimeInMillis());
                        saveUserStatus(STATUS_SOBER);
                        
                        // Show congratulation message
                        showCongratulationsMessage();
                    }
                },
                year, month, day
        );
        
        // Set max date to today
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }
    
    private void saveSobrietyStartDate(long startDate) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(START_DATE_KEY, startDate);
        editor.apply();
    }
    
    private void showCongratulationsMessage() {
        Toast.makeText(this, 
                "Congratulations on your sobriety journey! We're here to support you.", 
                Toast.LENGTH_LONG).show();
        goToMainActivity();
    }
    
    private void showSupportMessage() {
        Toast.makeText(this, 
                "Taking the first step is the hardest part. We're here to help you on your journey.", 
                Toast.LENGTH_LONG).show();
        goToCommunitySupport();
    }
    
    private void goToMainActivity() {
        Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    
    private void goToCommunitySupport() {
        Intent intent = new Intent(WelcomeActivity.this, CommunitySupportActivity.class);
        intent.putExtra("from_welcome_screen", true);
        intent.putExtra("temp_seeking_status", true); // Flag to indicate temporary status
        startActivity(intent);
        // Don't finish this activity yet - we want to come back here if user presses back
    }
    
    private void navigateBasedOnStatus() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int status = preferences.getInt(CURRENT_STATUS_KEY, STATUS_UNKNOWN);
        
        switch (status) {
            case STATUS_SEEKING:
                goToMainActivity(); // Changed from goToCommunitySupport() to avoid getting stuck
                break;
            case STATUS_SOBER:
            case STATUS_UNKNOWN:
            default:
                goToMainActivity();
                break;
        }
    }
}