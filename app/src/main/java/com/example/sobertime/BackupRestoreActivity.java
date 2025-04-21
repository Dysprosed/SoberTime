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
import com.example.sobertime.model.SobrietyTracker; 

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

// Add these imports at the top
import android.os.Build;
import android.content.ContentValues;
import android.content.ContentResolver;
import android.provider.MediaStore;
import android.net.Uri;

// Add this import
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class BackupRestoreActivity extends BaseActivity {

    private Button backupButton;
    private Button restoreButton;
    private TextView lastBackupText;
    private ProgressBar progressBar;
    
    private DatabaseHelper databaseHelper;
    private SharedPreferences preferences;
    private static final String PREFS_NAME = "SobrietyTrackerPrefs";
    private static final String LAST_BACKUP_KEY = "last_backup_time";
    private static final String BACKUP_DIRECTORY = "SoberTime_Backups";
    private static final String BACKUP_FILENAME = "sobriety_tracker_backup.json";
    private SobrietyTracker sobrietyTracker;
    
    // Add these constants
    private static final int PERMISSION_REQUEST_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_restore);
        
        // Initialize SobrietyTracker
        sobrietyTracker = SobrietyTracker.getInstance(this);
        
        // Set up action bar
        if (getSupportActionBar() != null) {
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
        // Check for permissions first if on older Android versions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE
                );
                return;
            }
        }
        
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
                    
                    // Add sobriety start date from SobrietyTracker instead of preferences
                    long startDate = sobrietyTracker.getSobrietyStartDate();
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
                            
                            String backupLocation;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                backupLocation = "Downloads/SoberTime_Backups folder";
                            } else {
                                backupLocation = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + 
                                                 "/SoberTime_Backups";
                            }
                            
                            Toast.makeText(BackupRestoreActivity.this, 
                                          "Backup created successfully in " + backupLocation, 
                                          Toast.LENGTH_LONG).show();
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+ use MediaStore
            ContentResolver resolver = getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Downloads.DISPLAY_NAME, BACKUP_FILENAME);
            contentValues.put(MediaStore.Downloads.MIME_TYPE, "application/json");
            contentValues.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + BACKUP_DIRECTORY);
            
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
            if (uri != null) {
                try (OutputStream outputStream = resolver.openOutputStream(uri)) {
                    if (outputStream != null) {
                        outputStream.write(data.getBytes());
                        
                        // Store the URI for future reference
                        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                        prefs.edit().putString("backup_uri", uri.toString()).apply();
                    }
                }
            }
        } else {
            // For older versions, write to Downloads folder
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File backupDir = new File(downloadsDir, BACKUP_DIRECTORY);
            
            if (!backupDir.exists()) {
                if (!backupDir.mkdirs()) {
                    throw new IOException("Failed to create backup directory");
                }
            }
            
            File backupFile = new File(backupDir, BACKUP_FILENAME);
            
            try (FileOutputStream fos = new FileOutputStream(backupFile);
                 OutputStreamWriter writer = new OutputStreamWriter(fos)) {
                writer.write(data);
                
                // Store the path for future reference
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                prefs.edit().putString("backup_path", backupFile.getAbsolutePath()).apply();
            }
        }
    }
    
    private void restoreBackup() {
        // Check for permissions first if on older Android versions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE + 1
                );
                return;
            }
        }
        
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
                    
                    // Restore sobriety start date using SobrietyTracker
                    if (backupJson.has("sobriety_start_date")) {
                        long startDate = backupJson.getLong("sobriety_start_date");
                        sobrietyTracker.setSobrietyStartDate(startDate);
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
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        StringBuilder stringBuilder = new StringBuilder();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+ try to use saved URI first
            String savedUri = prefs.getString("backup_uri", "");
            if (!savedUri.isEmpty()) {
                try {
                    Uri uri = Uri.parse(savedUri);
                    try (InputStream is = getContentResolver().openInputStream(uri);
                         BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            stringBuilder.append(line);
                        }
                        return stringBuilder.toString();
                    } catch (Exception e) {
                        // URI might be invalid, fall back to search
                    }
                } catch (Exception e) {
                    // Continue to fallback
                }
            }
            
            // If saved URI didn't work, search in Downloads
            try {
                Uri queryUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                String[] projection = {MediaStore.Downloads._ID};
                String selection = MediaStore.Downloads.DISPLAY_NAME + "=? AND " + 
                                  MediaStore.Downloads.RELATIVE_PATH + " LIKE ?";
                String[] selectionArgs = {
                    BACKUP_FILENAME,
                    "%" + BACKUP_DIRECTORY + "%"
                };
                
                try (Cursor cursor = getContentResolver().query(queryUri, projection, selection, selectionArgs, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID);
                        long id = cursor.getLong(idColumn);
                        Uri contentUri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id);
                        
                        // Save this URI for future use
                        prefs.edit().putString("backup_uri", contentUri.toString()).apply();
                        
                        try (InputStream is = getContentResolver().openInputStream(contentUri);
                             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                stringBuilder.append(line);
                            }
                            return stringBuilder.toString();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // For older versions, first try the saved path
            String savedPath = prefs.getString("backup_path", "");
            if (!savedPath.isEmpty()) {
                File backupFile = new File(savedPath);
                if (backupFile.exists()) {
                    try (FileInputStream fis = new FileInputStream(backupFile);
                         BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            stringBuilder.append(line);
                        }
                        return stringBuilder.toString();
                    }
                }
            }
            
            // If saved path didn't work, try the standard location
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File backupDir = new File(downloadsDir, BACKUP_DIRECTORY);
            File backupFile = new File(backupDir, BACKUP_FILENAME);
            
            if (!backupFile.exists()) {
                return null;
            }
            
            try (FileInputStream fis = new FileInputStream(backupFile);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                return stringBuilder.toString();
            }
        }
        
        // If we get here, we couldn't find a backup file
        return null;
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
    
    // Handle permission results
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with backup
                createBackup();
            } else {
                // Permission denied
                Toast.makeText(this, "Storage permission is required to create a backup", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == PERMISSION_REQUEST_CODE + 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with restore
                restoreBackup();
            } else {
                // Permission denied
                Toast.makeText(this, "Storage permission is required to restore a backup", Toast.LENGTH_LONG).show();
            }
        }
    }
}
