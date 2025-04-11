package com.example.sobertime;

import java.util.ArrayList;

import com.example.sobertime.DatabaseHelper;

import android.Manifest;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class AccountabilityBuddyActivity extends BaseActivity {

    private static final int SMS_PERMISSION_REQUEST_CODE = 101;

    private TextView buddyNameTextView;
    private TextView buddyPhoneTextView;
    private Button editBuddyButton;
    private Button testMessageButton;
    private Switch enableBuddySwitch;
    private Switch notifyOnCheckinSwitch;
    private Switch notifyOnRelapseSwitch;
    private Switch notifyOnMilestoneSwitch;
    private String userName;
    private String userPhone;

    private DatabaseHelper databaseHelper;
    private boolean hasBuddy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accountability_buddy);

        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Accountability Buddy");
        }

        // Initialize database helper
        databaseHelper = DatabaseHelper.getInstance(this);

        // Initialize views
        initializeViews();

        // Load buddy data
        loadBuddyData();

        // Set up listeners
        setupListeners();

        // Check SMS permission
        checkSmsPermission();
    }

    private void initializeViews() {
        buddyNameTextView = findViewById(R.id.buddyNameTextView);
        buddyPhoneTextView = findViewById(R.id.buddyPhoneTextView);
        editBuddyButton = findViewById(R.id.editBuddyButton);
        testMessageButton = findViewById(R.id.testMessageButton);
        enableBuddySwitch = findViewById(R.id.enableBuddySwitch);
        notifyOnCheckinSwitch = findViewById(R.id.notifyOnCheckinSwitch);
        notifyOnRelapseSwitch = findViewById(R.id.notifyOnRelapseSwitch);
        notifyOnMilestoneSwitch = findViewById(R.id.notifyOnMilestoneSwitch);
    }

    private void loadBuddyData() {
        // Make sure the table exists
        databaseHelper.ensureAccountabilityBuddyTableExists();
        
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.query(
                "accountability_buddy",
                null,
                null,
                null,
                null,
                null,
                null
        );
    
        if (cursor.moveToFirst()) {
            hasBuddy = true;
            String name = cursor.getString(cursor.getColumnIndex("name"));
            String phone = cursor.getString(cursor.getColumnIndex("phone"));
            
            // Load user information
            int userNameIndex = cursor.getColumnIndex("user_name");
            int userPhoneIndex = cursor.getColumnIndex("user_phone");
            
            if (userNameIndex != -1) {
                userName = cursor.getString(userNameIndex);
            }
            if (userPhoneIndex != -1) {
                userPhone = cursor.getString(userPhoneIndex);
            }
            
            boolean enabled = cursor.getInt(cursor.getColumnIndex("enabled")) == 1;
            boolean notifyCheckin = cursor.getInt(cursor.getColumnIndex("notify_on_checkin")) == 1;
            boolean notifyRelapse = cursor.getInt(cursor.getColumnIndex("notify_on_relapse")) == 1;
            boolean notifyMilestone = cursor.getInt(cursor.getColumnIndex("notify_on_milestone")) == 1;
    
            buddyNameTextView.setText(name);
            buddyPhoneTextView.setText(phone);
            enableBuddySwitch.setChecked(enabled);
            notifyOnCheckinSwitch.setChecked(notifyCheckin);
            notifyOnRelapseSwitch.setChecked(notifyRelapse);
            notifyOnMilestoneSwitch.setChecked(notifyMilestone);
    
            // Update UI based on whether buddy is enabled
            updateUIState(enabled);
        } else {
            // No buddy set up yet
            hasBuddy = false;
            userName = "";
            userPhone = "";
            buddyNameTextView.setText("Not set");
            buddyPhoneTextView.setText("Not set");
            enableBuddySwitch.setChecked(false);
            updateUIState(false);
        }
    
        cursor.close();
    }

    private void updateUIState(boolean enabled) {
        notifyOnCheckinSwitch.setEnabled(enabled);
        notifyOnRelapseSwitch.setEnabled(enabled);
        notifyOnMilestoneSwitch.setEnabled(enabled);
        testMessageButton.setEnabled(enabled && hasBuddy);
    }

    private void setupListeners() {
        // Edit buddy button
        editBuddyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddBuddyDialog();
            }
        });

        // Add Accountability Buddy button
        testMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddBuddyDialog();
            }
        });

        // Enable buddy switch
        enableBuddySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (hasBuddy) {
                    updateBuddyEnabled(isChecked);
                    updateUIState(isChecked);
                } else if (isChecked) {
                    // If no buddy is set yet but the switch is turned on, prompt to add a buddy
                    enableBuddySwitch.setChecked(false);
                    showAddBuddyDialog();
                }
            }
        });

        // Notification switches
        notifyOnCheckinSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateBuddyNotificationSetting("notify_on_checkin", isChecked);
            }
        });

        notifyOnRelapseSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateBuddyNotificationSetting("notify_on_relapse", isChecked);
            }
        });

        notifyOnMilestoneSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateBuddyNotificationSetting("notify_on_milestone", isChecked);
            }
        });
    }

    private void showAddBuddyDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_buddy, null);
        final EditText yourNameEditText = dialogView.findViewById(R.id.yourNameEditText);
        final EditText yourPhoneEditText = dialogView.findViewById(R.id.yourPhoneEditText);
        final EditText buddyNameEditText = dialogView.findViewById(R.id.buddyNameEditText);
        final EditText buddyPhoneEditText = dialogView.findViewById(R.id.buddyPhoneEditText);
    
        // Pre-fill fields if info exists
        if (!TextUtils.isEmpty(userName)) {
            yourNameEditText.setText(userName);
        }
        if (!TextUtils.isEmpty(userPhone)) {
            yourPhoneEditText.setText(userPhone);
        }
        
        if (hasBuddy) {
            buddyNameEditText.setText(buddyNameTextView.getText());
            buddyPhoneEditText.setText(buddyPhoneTextView.getText());
        }
    
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Accountability Buddy")
                .setView(dialogView)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String yourName = yourNameEditText.getText().toString().trim();
                        String yourPhone = yourPhoneEditText.getText().toString().trim();
                        String buddyName = buddyNameEditText.getText().toString().trim();
                        String buddyPhone = buddyPhoneEditText.getText().toString().trim();
    
                        if (TextUtils.isEmpty(yourName) || TextUtils.isEmpty(yourPhone) || 
                            TextUtils.isEmpty(buddyName) || TextUtils.isEmpty(buddyPhone)) {
                            Toast.makeText(AccountabilityBuddyActivity.this, 
                                    "All fields are required", Toast.LENGTH_SHORT).show();
                            return;
                        }
    
                        // Save all info
                        saveBuddyInfo(buddyName, buddyPhone, yourName, yourPhone);
                        
                        // Show confirmation dialog for sending message
                        showSendMessageConfirmationDialog(buddyName, buddyPhone, yourName, yourPhone);
                    }
                })
                .setNegativeButton("Cancel", null);
    
        if (hasBuddy) {
            builder.setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    deleteBuddy();
                }
            });
        }
    
        builder.show();
    }

    private void saveBuddyInfo(String buddyName, String buddyPhone, String yourName, String yourPhone) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", buddyName);
        values.put("phone", buddyPhone);
        values.put("user_name", yourName);
        values.put("user_phone", yourPhone);
    
        if (hasBuddy) {
            // Update existing buddy
            db.update("accountability_buddy", values, null, null);
        } else {
            // Insert new buddy
            values.put("enabled", 1);
            values.put("notify_on_checkin", 0);
            values.put("notify_on_relapse", 1);
            values.put("notify_on_milestone", 1);
            db.insert("accountability_buddy", null, values);
            hasBuddy = true;
        }
    
        // Save the user info
        userName = yourName;
        userPhone = yourPhone;
    
        // Refresh the UI
        loadBuddyData();
        Toast.makeText(this, "Buddy information saved", Toast.LENGTH_SHORT).show();
    }

    private void showSendMessageConfirmationDialog(final String buddyName, final String buddyPhone, 
                                                final String yourName, final String yourPhone) {
        new AlertDialog.Builder(this)
                .setTitle("Send Invitation Message")
                .setMessage("We'll send a text message to " + buddyName + " explaining that you've " +
                        "selected them as your sobriety accountability buddy. They'll be given " +
                        "information on how to support you and how to opt out if they prefer not to participate.")
                .setPositiveButton("Send Message", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendBuddyInvite(buddyName, buddyPhone, yourName, yourPhone);
                    }
                })
                .setNegativeButton("Not Now", null)
                .show();
    }

    private void sendBuddyInvite(String buddyName, String buddyPhone, String userName, String userPhone) {
        if (TextUtils.isEmpty(buddyPhone)) {
            Toast.makeText(this, "No valid buddy phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = "Hi " + buddyName + ", this is a message from New Dawn, a sobriety app. " + 
                userName + " has selected you as their sobriety accountability buddy. " +
                "As an accountability buddy, you may receive notifications when they check in, " +
                "reach milestones, or if they need extra support. " +
                "If you'd prefer not to participate, please contact " + userName + " directly at " + 
                userPhone + " to let them know. Thank you for your support!";
        
        try {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message);
            smsManager.sendMultipartTextMessage(buddyPhone, null, parts, null, null);
            Toast.makeText(this, "Invitation sent to " + buddyName, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send message: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteBuddy() {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.delete("accountability_buddy", null, null);
        hasBuddy = false;
        loadBuddyData();
        Toast.makeText(this, "Buddy removed", Toast.LENGTH_SHORT).show();
    }

    private void updateBuddyEnabled(boolean enabled) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("enabled", enabled ? 1 : 0);
        db.update("accountability_buddy", values, null, null);
    }

    private void updateBuddyNotificationSetting(String setting, boolean enabled) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(setting, enabled ? 1 : 0);
        db.update("accountability_buddy", values, null, null);
    }

    private void checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, 
                        "SMS permission denied. Buddy notifications will not work.", 
                        Toast.LENGTH_LONG).show();
                enableBuddySwitch.setChecked(false);
                updateBuddyEnabled(false);
                updateUIState(false);
            }
        }
    }

}