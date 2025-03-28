package com.example.sobertime;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class EmergencyHelpActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "EmergencyContactPrefs";
    private static final String SPONSOR_NAME_KEY = "sponsor_name";
    private static final String SPONSOR_PHONE_KEY = "sponsor_phone";
    private static final String THERAPIST_NAME_KEY = "therapist_name";
    private static final String THERAPIST_PHONE_KEY = "therapist_phone";
    private static final String EMERGENCY_CONTACT_NAME_KEY = "emergency_contact_name";
    private static final String EMERGENCY_CONTACT_PHONE_KEY = "emergency_contact_phone";
    
    private CardView sponsorCard;
    private CardView therapistCard;
    private CardView emergencyContactCard;
    private CardView helplineCard;
    private CardView breathingExerciseCard;
    private CardView gratitudeExerciseCard;
    
    private TextView sponsorNameText;
    private TextView sponsorPhoneText;
    private TextView therapistNameText;
    private TextView therapistPhoneText;
    private TextView emergencyContactNameText;
    private TextView emergencyContactPhoneText;
    
    private Button editSponsorButton;
    private Button callSponsorButton;
    private Button editTherapistButton;
    private Button callTherapistButton;
    private Button editEmergencyContactButton;
    private Button callEmergencyContactButton;
    private Button callHelplineButton;
    
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_help);
        
        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Emergency Help");
        }
        
        // Initialize preferences
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Initialize views
        initializeViews();
        
        // Set up click listeners
        setupClickListeners();
        
        // Load saved contact information
        loadContactInfo();
    }
    
    private void initializeViews() {
        // Cards
        sponsorCard = findViewById(R.id.sponsorCard);
        therapistCard = findViewById(R.id.therapistCard);
        emergencyContactCard = findViewById(R.id.emergencyContactCard);
        helplineCard = findViewById(R.id.helplineCard);
        breathingExerciseCard = findViewById(R.id.breathingExerciseCard);
        gratitudeExerciseCard = findViewById(R.id.gratitudeExerciseCard);
        
        // TextViews
        sponsorNameText = findViewById(R.id.sponsorNameText);
        sponsorPhoneText = findViewById(R.id.sponsorPhoneText);
        therapistNameText = findViewById(R.id.therapistNameText);
        therapistPhoneText = findViewById(R.id.therapistPhoneText);
        emergencyContactNameText = findViewById(R.id.emergencyContactNameText);
        emergencyContactPhoneText = findViewById(R.id.emergencyContactPhoneText);
        
        // Buttons
        editSponsorButton = findViewById(R.id.editSponsorButton);
        callSponsorButton = findViewById(R.id.callSponsorButton);
        editTherapistButton = findViewById(R.id.editTherapistButton);
        callTherapistButton = findViewById(R.id.callTherapistButton);
        editEmergencyContactButton = findViewById(R.id.editEmergencyContactButton);
        callEmergencyContactButton = findViewById(R.id.callEmergencyContactButton);
        callHelplineButton = findViewById(R.id.callHelplineButton);
    }
    
    private void setupClickListeners() {
        // Sponsor card
        editSponsorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showContactEditDialog(
                        "Edit Sponsor Information",
                        preferences.getString(SPONSOR_NAME_KEY, ""),
                        preferences.getString(SPONSOR_PHONE_KEY, ""),
                        (name, phone) -> {
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString(SPONSOR_NAME_KEY, name);
                            editor.putString(SPONSOR_PHONE_KEY, phone);
                            editor.apply();
                            
                            sponsorNameText.setText(name.isEmpty() ? "Not set" : name);
                            sponsorPhoneText.setText(phone.isEmpty() ? "Not set" : phone);
                            callSponsorButton.setEnabled(!phone.isEmpty());
                        }
                );
            }
        });
        
        callSponsorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phone = preferences.getString(SPONSOR_PHONE_KEY, "");
                if (!TextUtils.isEmpty(phone)) {
                    dialPhoneNumber(phone);
                }
            }
        });
        
        // Therapist card
        editTherapistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showContactEditDialog(
                        "Edit Therapist Information",
                        preferences.getString(THERAPIST_NAME_KEY, ""),
                        preferences.getString(THERAPIST_PHONE_KEY, ""),
                        (name, phone) -> {
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString(THERAPIST_NAME_KEY, name);
                            editor.putString(THERAPIST_PHONE_KEY, phone);
                            editor.apply();
                            
                            therapistNameText.setText(name.isEmpty() ? "Not set" : name);
                            therapistPhoneText.setText(phone.isEmpty() ? "Not set" : phone);
                            callTherapistButton.setEnabled(!phone.isEmpty());
                        }
                );
            }
        });
        
        callTherapistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phone = preferences.getString(THERAPIST_PHONE_KEY, "");
                if (!TextUtils.isEmpty(phone)) {
                    dialPhoneNumber(phone);
                }
            }
        });
        
        // Emergency contact card
        editEmergencyContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showContactEditDialog(
                        "Edit Emergency Contact",
                        preferences.getString(EMERGENCY_CONTACT_NAME_KEY, ""),
                        preferences.getString(EMERGENCY_CONTACT_PHONE_KEY, ""),
                        (name, phone) -> {
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString(EMERGENCY_CONTACT_NAME_KEY, name);
                            editor.putString(EMERGENCY_CONTACT_PHONE_KEY, phone);
                            editor.apply();
                            
                            emergencyContactNameText.setText(name.isEmpty() ? "Not set" : name);
                            emergencyContactPhoneText.setText(phone.isEmpty() ? "Not set" : phone);
                            callEmergencyContactButton.setEnabled(!phone.isEmpty());
                        }
                );
            }
        });
        
        callEmergencyContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phone = preferences.getString(EMERGENCY_CONTACT_PHONE_KEY, "");
                if (!TextUtils.isEmpty(phone)) {
                    dialPhoneNumber(phone);
                }
            }
        });
        
        // Helpline card
        callHelplineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // SAMHSA National Helpline
                dialPhoneNumber("1-800-662-4357");
            }
        });
        
        // Breathing exercise card
        breathingExerciseCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show the breathing exercise dialog
                showBreathingExerciseDialog();
            }
        });
        
        // Gratitude exercise card
        gratitudeExerciseCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show the gratitude exercise dialog
                showGratitudeExerciseDialog();
            }
        });
    }
    
    private void loadContactInfo() {
        // Load sponsor info
        String sponsorName = preferences.getString(SPONSOR_NAME_KEY, "");
        String sponsorPhone = preferences.getString(SPONSOR_PHONE_KEY, "");
        sponsorNameText.setText(sponsorName.isEmpty() ? "Not set" : sponsorName);
        sponsorPhoneText.setText(sponsorPhone.isEmpty() ? "Not set" : sponsorPhone);
        callSponsorButton.setEnabled(!sponsorPhone.isEmpty());
        
        // Load therapist info
        String therapistName = preferences.getString(THERAPIST_NAME_KEY, "");
        String therapistPhone = preferences.getString(THERAPIST_PHONE_KEY, "");
        therapistNameText.setText(therapistName.isEmpty() ? "Not set" : therapistName);
        therapistPhoneText.setText(therapistPhone.isEmpty() ? "Not set" : therapistPhone);
        callTherapistButton.setEnabled(!therapistPhone.isEmpty());
        
        // Load emergency contact info
        String emergencyContactName = preferences.getString(EMERGENCY_CONTACT_NAME_KEY, "");
        String emergencyContactPhone = preferences.getString(EMERGENCY_CONTACT_PHONE_KEY, "");
        emergencyContactNameText.setText(emergencyContactName.isEmpty() ? "Not set" : emergencyContactName);
        emergencyContactPhoneText.setText(emergencyContactPhone.isEmpty() ? "Not set" : emergencyContactPhone);
        callEmergencyContactButton.setEnabled(!emergencyContactPhone.isEmpty());
    }
    
    /**
     * Shows a dialog to edit contact information
     */
    private void showContactEditDialog(String title, String currentName, String currentPhone, 
                                     ContactInfoListener listener) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_contact, null);
        EditText nameEditText = dialogView.findViewById(R.id.nameEditText);
        EditText phoneEditText = dialogView.findViewById(R.id.phoneEditText);
        
        nameEditText.setText(currentName);
        phoneEditText.setText(currentPhone);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setView(dialogView)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = nameEditText.getText().toString().trim();
                        String phone = phoneEditText.getText().toString().trim();
                        listener.onContactInfoSaved(name, phone);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    /**
     * Interface for contact info update callback
     */
    private interface ContactInfoListener {
        void onContactInfoSaved(String name, String phone);
    }
    
    /**
     * Initiates a phone call to the given number
     */
    private void dialPhoneNumber(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "No phone app available", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Shows the breathing exercise dialog
     */
    private void showBreathingExerciseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("4-7-8 Breathing Exercise")
                .setMessage("This exercise can help calm anxiety and reduce cravings:\n\n" +
                        "1. Exhale completely through your mouth\n" +
                        "2. Inhale through your nose for 4 seconds\n" +
                        "3. Hold your breath for 7 seconds\n" +
                        "4. Exhale completely through your mouth for 8 seconds\n" +
                        "5. Repeat at least 4 times\n\n" +
                        "Would you like to start a guided exercise?")
                .setPositiveButton("Start", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // In a full implementation, this would start a guided breathing activity
                        Toast.makeText(EmergencyHelpActivity.this, 
                                "Guided breathing coming soon!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    /**
     * Shows the gratitude exercise dialog
     */
    private void showGratitudeExerciseDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_gratitude, null);
        EditText gratitudeEditText = dialogView.findViewById(R.id.gratitudeEditText);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Gratitude Exercise")
                .setView(dialogView)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String gratitudeText = gratitudeEditText.getText().toString().trim();
                        if (!gratitudeText.isEmpty()) {
                            Toast.makeText(EmergencyHelpActivity.this, 
                                    "Gratitude noted!", Toast.LENGTH_SHORT).show();
                            
                            // In a full implementation, this would save to a gratitude journal
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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
