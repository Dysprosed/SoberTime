package com.example.sobertime;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BackupRestoreActivity extends AppCompatActivity {

    private Button backupButton;
    private Button restoreButton;
    private TextView lastBackupText;
    private ProgressBar progressBar;
    
    private DatabaseHelper databaseHelper;
    private SharedPreferences preferences;
    private static final String PREFS_NAME = "SobrietyTrackerPrefs";
    private static final String LAST_BACKUP_KEY = "last_backup_time";
    private static final String BACKUP_FILENAME = "sobriety_tracker_backup.json";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_restore);
        
        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Backup & Restore");
        }
        
        // Initialize database and preferences
        databaseHelper = DatabaseHelper.getInstance(this);
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Initialize views
        backupButton = findViewById(R.id.backupButton);
        restoreButton = findViewById(R.id.restoreButton);
        lastBackupText = findViewById(R.id.lastBackupText);
        progressBar = findViewById(R.id.progressBar);
        
        // Display last backup time
        updateLastBackupText();
        
        // Set up click listeners
        backupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBackupConfirmationDialog();
            }
        });
        
        restoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRestoreConfirmationDialog();
            }
        });
    }
    
    private void updateLastBackupText() {
        long lastBackupTime = preferences.getLong(LAST_BACKUP_KEY, 0);
        
        if (lastBackupTime > 0) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault());
            String formattedDate = dateFormat.format(new Date(lastBackupTime));
            lastBackupText.setText("Last backup: " + formattedDate);
        } else {
            lastBackupText.setText("No backup has been created yet");
        }
    }
    
    private void showBackupConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Backup");
        builder.setMessage("This will create a backup of all your data. Are you sure you want to continue?");
        
        builder.setPositiveButton("Backup", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                createBackup();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void showRestoreConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Restore Data");
        builder.setMessage("This will replace all your current data with the data from your backup. Are you sure you want to continue?");
        
        builder.setPositiveButton("Restore", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                restoreBackup();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void createBackup() {
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        backupButton.setEnabled(false);
        restoreButton.setEnabled(false);
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Create JSON object for all app data
                    final JSONObject backupData = new JSONObject();
                    
                    // Add journal entries
                    backupData.put("journal_entries", getJournalEntriesJson());
                    
                    // Add settings
                    backupData.put("settings", getSettingsJson());
                    
                    // Add sobriety start date
                    long startDate = preferences.getLong("sobriety_start_date", System.currentTimeMillis());
                    backupData.put("sobriety_start_date", startDate);
                    
                    // Add notification settings
                    JSONObject notificationSettings = new JSONObject();
                    SharedPreferences notifPrefs = getSharedPreferences("SobrietyNotificationPrefs", MODE_PRIVATE);
                    notificationSettings.put("notifications_enabled", notifPrefs.getBoolean("notifications_enabled", true));
                    notificationSettings.put("morning_notification_enabled", notifPrefs.getBoolean("morning_notification_enabled", true));
                    notificationSettings.put("evening_notification_enabled", notifPrefs.getBoolean("evening_notification_enabled", true));
                    notificationSettings.put("milestone_notification_enabled", notifPrefs.getBoolean("milestone_notification_enabled", true));
                    notificationSettings.put("custom_notification_times", notifPrefs.getString("custom_notification_times", ""));
                    
                    backupData.put("notification_settings", notificationSettings);
                    
                    // Write to file
                    writeBackupToFile(backupData.toString());
                    
                    // Update last backup time
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putLong(LAST_BACKUP_KEY, System.currentTimeMillis());
                    editor.apply();
                    
                    // Update UI on main thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            backupButton.setEnabled(true);
                            restoreButton.setEnabled(true);
                            updateLastBackupText();
                            Toast.makeText(BackupRestoreActivity.this, "Backup created successfully", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (final Exception e) {
                    e.printStackTrace();
                    
                    // Show error on main thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            backupButton.setEnabled(true);
                            restoreButton.setEnabled(true);
                            Toast.makeText(BackupRestoreActivity.this, "Backup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }
    
    private JSONArray getJournalEntriesJson() throws JSONException {
        JSONArray entriesArray = new JSONArray();
        
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.query("journal", null, null, null, null, null, null);
        
        if (cursor.moveToFirst()) {
            do {
                JSONObject entry = new JSONObject();
                entry.put("id", cursor.getLong(cursor.getColumnIndex("id")));
                entry.put("timestamp", cursor.getLong(cursor.getColumnIndex("timestamp")));
                entry.put("title", cursor.getString(cursor.getColumnIndex("title")));
                entry.put("content", cursor.getString(cursor.getColumnIndex("content")));
                entry.put("mood", cursor.getString(cursor.getColumnIndex("mood")));
                entry.put("craving_level", cursor.getInt(cursor.getColumnIndex("craving_level")));
                entry.put("trigger", cursor.getString(cursor.getColumnIndex("trigger")));
                
                entriesArray.put(entry);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return entriesArray;
    }
    
    private JSONObject getSettingsJson() throws JSONException {
        JSONObject settingsObject = new JSONObject();
        
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.query("settings", null, null, null, null, null, null);
        
        if (cursor.moveToFirst()) {
            do {
                String key = cursor.getString(cursor.getColumnIndex("key"));
                String value = cursor.getString(cursor.getColumnIndex("value"));
                settingsObject.put(key, value);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return settingsObject;
    }
    
    private void writeBackupToFile(String data) throws IOException {
        File backupFile = new File(getExternalFilesDir(null), BACKUP_FILENAME);
        
        FileOutputStream fos = new FileOutputStream(backupFile);
        OutputStreamWriter writer = new OutputStreamWriter(fos);
        writer.write(data);
        writer.close();
        fos.close();
    }
    
    private void restoreBackup() {
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        backupButton.setEnabled(false);
        restoreButton.setEnabled(false);
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Read backup file
                    String backupData = readBackupFromFile();
                    
                    if (backupData == null || backupData.isEmpty()) {
                        throw new IOException("Backup file is empty or not found");
                    }
                    
                    JSONObject backupJson = new JSONObject(backupData);
                    
                    // Restore all data
                    restoreJournalEntries(backupJson.getJSONArray("journal_entries"));
                    restoreSettings(backupJson.getJSONObject("settings"));
                    
                    // Restore sobriety start date
                    if (backupJson.has("sobriety_start_date")) {
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putLong("sobriety_start_date", backupJson.getLong("sobriety_start_date"));
                        editor.apply();
                    }
                    
                    // Restore notification settings
                    if (backupJson.has("notification_settings")) {
                        JSONObject notificationSettings = backupJson.getJSONObject("notification_settings");
                        SharedPreferences notifPrefs = getSharedPreferences("SobrietyNotificationPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = notifPrefs.edit();
                        
                        editor.putBoolean("notifications_enabled", notificationSettings.optBoolean("notifications_enabled", true));
                        editor.putBoolean("morning_notification_enabled", notificationSettings.optBoolean("morning_notification_enabled", true));
                        editor.putBoolean("evening_notification_enabled", notificationSettings.optBoolean("evening_notification_enabled", true));
                        editor.putBoolean("milestone_notification_enabled", notificationSettings.optBoolean("milestone_notification_enabled", true));
                        editor.putString("custom_notification_times", notificationSettings.optString("custom_notification_times", ""));
                        
                        editor.apply();
                    }
                    
                    // Reschedule notifications based on restored settings
                    NotificationHelper.scheduleNotifications(BackupRestoreActivity.this);
                    
                    // Update UI on main thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            backupButton.setEnabled(true);
                            restoreButton.setEnabled(true);
                            Toast.makeText(BackupRestoreActivity.this, "Data restored successfully", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (final Exception e) {
                    e.printStackTrace();
                    
                    // Show error on main thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            backupButton.setEnabled(true);
                            restoreButton.setEnabled(true);
                            Toast.makeText(BackupRestoreActivity.this, "Restore failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }
    
    private String readBackupFromFile() throws IOException {
        File backupFile = new File(getExternalFilesDir(null), BACKUP_FILENAME);
        
        if (!backupFile.exists()) {
            return null;
        }
        
        StringBuilder stringBuilder = new StringBuilder();
        FileInputStream fis = new FileInputStream(backupFile);
        InputStreamReader inputStreamReader = new InputStreamReader(fis);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
        }
        
        bufferedReader.close();
        return stringBuilder.toString();
    }
    
    private void restoreJournalEntries(JSONArray entriesArray) throws JSONException {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        
        // Clear existing entries
        db.delete("journal", null, null);
        
        // Insert backup entries
        for (int i = 0; i < entriesArray.length(); i++) {
            JSONObject entryJson = entriesArray.getJSONObject(i);
            
            ContentValues values = new ContentValues();
            values.put("id", entryJson.getLong("id"));
            values.put("timestamp", entryJson.getLong("timestamp"));
            values.put("title", entryJson.getString("title"));
            values.put("content", entryJson.getString("content"));
            
            // Handle nullable fields
            if (!entryJson.isNull("mood")) {
                values.put("mood", entryJson.getString("mood"));
            }
            
            values.put("craving_level", entryJson.getInt("craving_level"));
            
            if (!entryJson.isNull("trigger")) {
                values.put("trigger", entryJson.getString("trigger"));
            }
            
            db.insert("journal", null, values);
        }
    }
    
    private void restoreSettings(JSONObject settingsObject) throws JSONException {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        
        // Clear existing settings
        db.delete("settings", null, null);
        
        // Insert backup settings
        JSONArray keys = settingsObject.names();
        if (keys != null) {
            for (int i = 0; i < keys.length(); i++) {
                String key = keys.getString(i);
                String value = settingsObject.getString(key);
                
                ContentValues values = new ContentValues();
                values.put("key", key);
                values.put("value", value);
                
                db.insert("settings", null, values);
            }
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
