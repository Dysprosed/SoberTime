package com.example.sobertime;

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
                showEditBuddyDialog();
            }
        });

        // Test message button
        testMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendTestMessage();
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
                    showEditBuddyDialog();
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

    private void showEditBuddyDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_buddy, null);
        final EditText nameEditText = dialogView.findViewById(R.id.nameEditText);
        final EditText phoneEditText = dialogView.findViewById(R.id.phoneEditText);

        // Pre-fill fields if buddy exists
        if (hasBuddy) {
            nameEditText.setText(buddyNameTextView.getText());
            phoneEditText.setText(buddyPhoneTextView.getText());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Accountability Buddy")
                .setView(dialogView)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = nameEditText.getText().toString().trim();
                        String phone = phoneEditText.getText().toString().trim();

                        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
                            Toast.makeText(AccountabilityBuddyActivity.this, 
                                    "Name and phone are required", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        saveBuddyInfo(name, phone);
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

    private void saveBuddyInfo(String name, String phone) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("phone", phone);

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

        // Refresh the UI
        loadBuddyData();
        Toast.makeText(this, "Buddy information saved", Toast.LENGTH_SHORT).show();
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

    private void sendTestMessage() {
        String phone = buddyPhoneTextView.getText().toString();
        if (TextUtils.isEmpty(phone) || phone.equals("Not set")) {
            Toast.makeText(this, "No valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = "This is a test message from SoberTime. " + 
                buddyNameTextView.getText() + " has added you as their accountability buddy.";
        
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phone, null, message, null, null);
            Toast.makeText(this, "Test message sent", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send message: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
        }
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