package com.example.sobertime;

import android.content.Intent;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import com.example.sobertime.IntrusiveCheckInActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.example.sobertime.DatabaseHelper;

public class CheckInActivity extends BaseActivity {

    private TextView dateTextView;
    private TextView timeTextView;
    private Button soberButton;
    private Button relapseButton;
    private Button skipButton;
    private Button debugIntrusiveButton;

    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in);

        // Add check for auto-prompt
        boolean isAutomaticPrompt = getIntent().getBooleanExtra("automatic_prompt", false);

        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Daily Check-in");
        }

        // Initialize database helper
        databaseHelper = DatabaseHelper.getInstance(this);

        // Initialize views
        initializeViews();

        // Set current date and time
        updateDateTime();

        // Set up button listeners
        setupListeners();
    }

    private void initializeViews() {
        dateTextView = findViewById(R.id.dateTextView);
        timeTextView = findViewById(R.id.timeTextView);
        soberButton = findViewById(R.id.soberButton);
        relapseButton = findViewById(R.id.relapseButton);
        skipButton = findViewById(R.id.skipButton);
        
        // Initialize debug button
        debugIntrusiveButton = findViewById(R.id.debugIntrusiveButton);
        
        // Always make debug button visible - remove conditional for testing purposes
        debugIntrusiveButton.setVisibility(View.VISIBLE);
    }

    private void updateDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        Date currentDate = new Date();

        dateTextView.setText(dateFormat.format(currentDate));
        timeTextView.setText(timeFormat.format(currentDate));
    }

    private void setupListeners() {
        // Sober button
        soberButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Record successful check-in
                recordCheckIn(true);
                finish();
            }
        });

        // Relapse button
        relapseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRelapseConfirmationDialog();
            }
        });

        // Skip button
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Add listener for debug button
        debugIntrusiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchIntrusiveCheckIn();
            }
        });
    }

    private void launchIntrusiveCheckIn() {
        Intent intent = new Intent(this, IntrusiveCheckInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("automatic_prompt", true);
        intent.putExtra("intrusive_prompt", true);
        intent.putExtra("debug_mode", true);
        startActivity(intent);
        
        Toast.makeText(this, "Launching intrusive check-in...", Toast.LENGTH_SHORT).show();
    }

    private void showRelapseConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Reset")
                .setMessage("Are you sure you want to reset your sobriety counter? This will record a relapse but remember: a lapse is not a collapse. Every new start is progress.")
                .setPositiveButton("Yes, Reset", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        resetSobrietyCounter();
                        recordCheckIn(false);
                        finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void recordCheckIn(boolean maintainedSobriety) {
        SobrietyTracker sobrietyTracker = SobrietyTracker.getInstance(this);
        
        if (maintainedSobriety) {
            // Confirm sobriety for today
            sobrietyTracker.confirmSobrietyForToday();
            
            // We could also record this confirmation in a new table for history
            // recordConfirmationInDatabase(true);
            
            Toast.makeText(this, "Sobriety confirmed for today!", Toast.LENGTH_SHORT).show();
        } else {
            // Reset the counter
            sobrietyTracker.resetSobrietyCounter();
            
            // We could also record this in a new table for history
            // recordConfirmationInDatabase(false);
            
            Toast.makeText(this, "Counter reset. Every new beginning is progress.", Toast.LENGTH_LONG).show();
        }
        
        // Notify accountability buddy if needed
        notifyBuddyIfNeeded(maintainedSobriety);
        
        // Schedule the next notification
        NotificationHelper.scheduleNotifications(this);
    }

    private void resetSobrietyCounter() {
        // Reset sobriety counter to today
        SobrietyTracker sobrietyTracker = SobrietyTracker.getInstance(this);
        sobrietyTracker.setSobrietyStartDate(System.currentTimeMillis());
    }

    private void notifyBuddyIfNeeded(boolean maintainedSobriety) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.query(
                "accountability_buddy",
                null,
                "enabled = 1",  // Only get enabled buddies
                null,
                null,
                null,
                null
        );
    
        while (cursor.moveToNext()) {
            String buddyName = cursor.getString(cursor.getColumnIndex("name"));
            String buddyPhone = cursor.getString(cursor.getColumnIndex("phone"));
            boolean notifyOnCheckin = cursor.getInt(cursor.getColumnIndex("notify_on_checkin")) == 1;
            boolean notifyOnRelapse = cursor.getInt(cursor.getColumnIndex("notify_on_relapse")) == 1;
    
            // Determine if we should send a message
            boolean shouldNotify = (maintainedSobriety && notifyOnCheckin) || 
                                (!maintainedSobriety && notifyOnRelapse);
    
            if (shouldNotify && buddyPhone != null && !buddyPhone.isEmpty()) {
                // Build message
                String message;
                if (maintainedSobriety) {
                    message = "Hi " + buddyName + ", this is a notification from New Dawn. " +
                            "Your buddy has completed their daily check-in and is still " +
                            "maintaining their sobriety. No action needed.";
                } else {
                    message = "Hi " + buddyName + ", this is a notification from New Dawn. " +
                            "Your buddy has had a lapse and could use your support right now. " +
                            "Please consider reaching out to them when you have a moment.";
                }
    
                // Send SMS
                try {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(buddyPhone, null, message, null, null);
                } catch (Exception e) {
                    Log.e("CheckInActivity", "Failed to send message to buddy: " + e.getMessage());
                }
            }
        }
        
        cursor.close();
    }
}

