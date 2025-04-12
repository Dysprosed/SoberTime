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

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.sobertime.adapter.BuddyAdapter;
import com.example.sobertime.model.AccountabilityBuddy;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class AccountabilityBuddyActivity extends BaseActivity {

    private static final int SMS_PERMISSION_REQUEST_CODE = 101;

    private Button editBuddyButton;
    private Button testMessageButton;
    private Switch enableBuddySwitch;
    private Switch notifyOnCheckinSwitch;
    private Switch notifyOnRelapseSwitch;
    private Switch notifyOnMilestoneSwitch;
    private String userName;
    private String userPhone;

    private RecyclerView buddiesRecyclerView;
    private TextView emptyBuddiesText;
    private Button addBuddyButton;
    private List<AccountabilityBuddy> buddyList;
    private BuddyAdapter buddyAdapter;

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

    protected void initializeViews() {
        // Find the RecyclerView and Button
        buddiesRecyclerView = findViewById(R.id.buddiesRecyclerView);
        emptyBuddiesText = findViewById(R.id.emptyBuddiesText);
        addBuddyButton = findViewById(R.id.addBuddyButton);
        
        // Initialize RecyclerView
        buddiesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        buddyList = new ArrayList<>();
        buddyAdapter = new BuddyAdapter(this, buddyList, new BuddyAdapter.OnBuddyActionListener() {
            @Override
            public void onBuddyEditClicked(AccountabilityBuddy buddy, int position) {
                showEditBuddyDialog(buddy, position);
            }
            
            @Override
            public void onBuddyDeleteClicked(AccountabilityBuddy buddy, int position) {
                confirmDeleteBuddy(buddy, position);
            }
            
            @Override
            public void onBuddyEnabledChanged(AccountabilityBuddy buddy, boolean enabled) {
                updateBuddyEnabled(buddy.getId(), enabled);
            }
            
            @Override
            public void onNotificationSettingChanged(AccountabilityBuddy buddy, String setting, boolean enabled) {
                updateBuddyNotificationSetting(buddy.getId(), setting, enabled);
            }
        });
        buddiesRecyclerView.setAdapter(buddyAdapter);
    }

    private void confirmDeleteBuddy(AccountabilityBuddy buddy, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Buddy")
                .setMessage("Are you sure you want to remove " + buddy.getName() + " as an accountability buddy?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    deleteBuddy(buddy.getId());
                    buddyList.remove(position);
                    buddyAdapter.notifyItemRemoved(position);
                    if (buddyList.isEmpty()) {
                        buddiesRecyclerView.setVisibility(View.GONE);
                        emptyBuddiesText.setVisibility(View.VISIBLE);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadBuddyData() {
        // Make sure the table exists
        databaseHelper.ensureAccountabilityBuddyTableExists();
        
        // Clear the existing list
        buddyList.clear();
        
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
            do {
                AccountabilityBuddy buddy = new AccountabilityBuddy();
                buddy.setId(cursor.getLong(cursor.getColumnIndex("_id")));
                buddy.setName(cursor.getString(cursor.getColumnIndex("name")));
                buddy.setPhone(cursor.getString(cursor.getColumnIndex("phone")));
                buddy.setEnabled(cursor.getInt(cursor.getColumnIndex("enabled")) == 1);
                buddy.setNotifyOnCheckin(cursor.getInt(cursor.getColumnIndex("notify_on_checkin")) == 1);
                buddy.setNotifyOnRelapse(cursor.getInt(cursor.getColumnIndex("notify_on_relapse")) == 1);
                buddy.setNotifyOnMilestone(cursor.getInt(cursor.getColumnIndex("notify_on_milestone")) == 1);
                
                buddyList.add(buddy);
            } while (cursor.moveToNext());
            
            // Show RecyclerView, hide empty text
            buddiesRecyclerView.setVisibility(View.VISIBLE);
            emptyBuddiesText.setVisibility(View.GONE);
        } else {
            // No buddies - show empty text
            buddiesRecyclerView.setVisibility(View.GONE);
            emptyBuddiesText.setVisibility(View.VISIBLE);
        }
        
        cursor.close();
        
        // Update adapter with new data
        buddyAdapter.notifyDataSetChanged();
    }

    private void updateUIState(boolean enabled) {
        notifyOnCheckinSwitch.setEnabled(enabled);
        notifyOnRelapseSwitch.setEnabled(enabled);
        notifyOnMilestoneSwitch.setEnabled(enabled);
        testMessageButton.setEnabled(enabled && hasBuddy);
    }

    private void setupListeners() {
        // Set up "Add Buddy" button
        addBuddyButton.setOnClickListener(new View.OnClickListener() {
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
                    updateBuddyEnabled(/* buddyId */ 1, isChecked);  // Use real buddyId when available
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
                updateBuddyNotificationSetting(/* buddyId */ 1, "notify_on_checkin", isChecked);  // Use real buddyId
            }
        });

        notifyOnRelapseSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateBuddyNotificationSetting(/* buddyId */ 1, "notify_on_checkin", isChecked);  // Use real buddyId
            }
        });

        notifyOnMilestoneSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateBuddyNotificationSetting(/* buddyId */ 1, "notify_on_checkin", isChecked);  // Use real buddyId
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
        
        if (hasBuddy && !buddyList.isEmpty()) {
            AccountabilityBuddy firstBuddy = buddyList.get(0);
            buddyNameEditText.setText(firstBuddy.getName());
            buddyPhoneEditText.setText(firstBuddy.getPhone());
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
                    deleteBuddy(/* buddyId */ 1);  // Use real buddyId
                }
            });
        }
    
        builder.show();
    }

    private void showEditBuddyDialog(AccountabilityBuddy buddy, int position) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_buddy, null);
        final EditText yourNameEditText = dialogView.findViewById(R.id.yourNameEditText);
        final EditText yourPhoneEditText = dialogView.findViewById(R.id.yourPhoneEditText);
        final EditText buddyNameEditText = dialogView.findViewById(R.id.buddyNameEditText);
        final EditText buddyPhoneEditText = dialogView.findViewById(R.id.buddyPhoneEditText);
    
        // Pre-fill fields with user info
        if (!TextUtils.isEmpty(userName)) {
            yourNameEditText.setText(userName);
        }
        if (!TextUtils.isEmpty(userPhone)) {
            yourPhoneEditText.setText(userPhone);
        }
    
        // Pre-fill with buddy's existing info
        buddyNameEditText.setText(buddy.getName());
        buddyPhoneEditText.setText(buddy.getPhone());
    
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Accountability Buddy")
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
    
                        // Update buddy with new info (preserve the ID)
                        buddy.setName(buddyName);
                        buddy.setPhone(buddyPhone);
                        
                        // Update in database
                        updateBuddyInfo(buddy, yourName, yourPhone);
                        
                        // Refresh UI
                        buddyAdapter.notifyItemChanged(position);
                        
                        // If phone number changed, ask about sending invitation
                        if (!buddyPhone.equals(buddy.getPhone())) {
                            showSendMessageConfirmationDialog(buddyName, buddyPhone, yourName, yourPhone);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        confirmDeleteBuddy(buddy, position);
                    }
                });
    
        builder.show();
    }

    // Helper method to update buddy info in database
    private void updateBuddyInfo(AccountabilityBuddy buddy, String yourName, String yourPhone) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", buddy.getName());
        values.put("phone", buddy.getPhone());
        values.put("user_name", yourName);
        values.put("user_phone", yourPhone);
        
        // Update existing buddy
        db.update("accountability_buddy", values, "_id = ?", new String[]{String.valueOf(buddy.getId())});
        
        // Save the user info
        userName = yourName;
        userPhone = yourPhone;
        
        Toast.makeText(this, "Buddy information updated", Toast.LENGTH_SHORT).show();
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

    private void deleteBuddy(long buddyId) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.delete("accountability_buddy", "_id = ?", new String[]{String.valueOf(buddyId)});
        Toast.makeText(this, "Buddy removed", Toast.LENGTH_SHORT).show();
    }

    private void updateBuddyEnabled(long buddyId, boolean enabled) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("enabled", enabled ? 1 : 0);
        db.update("accountability_buddy", values, "_id = ?", new String[]{String.valueOf(buddyId)});
    }

    private void updateBuddyNotificationSetting(long buddyId, String setting, boolean enabled) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(setting, enabled ? 1 : 0);
        db.update("accountability_buddy", values, "_id = ?", new String[]{String.valueOf(buddyId)});
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
                updateBuddyEnabled(1, false);
                updateUIState(false);
            }
        }
    }

}