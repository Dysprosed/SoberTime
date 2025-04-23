package com.example.sobertime;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.UriPermission;
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
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.sobertime.model.SobrietyTracker; 

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// Add these imports for Storage Access Framework
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.documentfile.provider.DocumentFile;

// Keep existing Android version imports
import android.os.Build;
import android.content.ContentValues;
import android.content.ContentResolver;
import android.provider.MediaStore;
import android.content.ContentUris;

// Keep existing permission imports
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
    
    // Storage Access Framework constants
    private static final String SAF_TREE_URI_KEY = "saf_tree_uri";
    private ActivityResultLauncher<Intent> folderPickerLauncher;
    private boolean isBackupOperation; // Flag to track if we're doing backup or restore

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
        
        // Set up folder picker launcher
        folderPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), 
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    if (result.getData() != null) {
                        Uri treeUri = result.getData().getData();
                        if (treeUri != null) {
                            // Take a persistent permission to keep access across app restarts
                            getContentResolver().takePersistableUriPermission(
                                treeUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            );
                            
                            // Save the URI
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString(SAF_TREE_URI_KEY, treeUri.toString());
                            editor.apply();
                            
                            Log.i("BackupRestore", "Folder permission granted and saved: " + treeUri);
                            
                            // Continue with operation
                            if (isBackupOperation) {
                                performBackup();
                            } else {
                                performRestore();
                            }
                        }
                    }
                } else {
                    Toast.makeText(BackupRestoreActivity.this, "Folder access required for this operation", Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.GONE);
                    backupButton.setEnabled(true);
                    restoreButton.setEnabled(true);
                }
            });
        
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
        
        String message;
        if (hasExistingSafPermission()) {
            String savedUriString = preferences.getString(SAF_TREE_URI_KEY, "");
            Uri savedUri = Uri.parse(savedUriString);
            DocumentFile folder = DocumentFile.fromTreeUri(this, savedUri);
            String folderName = folder != null ? folder.getName() : "selected folder";
            
            message = "This will create a backup in the folder \"" + folderName + "\". " +
                      "If a backup already exists, it will be overwritten.";
        } else {
            message = "This will create a backup of all your data. " +
                      "You'll need to select a folder where the backup will be stored. " +
                      "This folder will be remembered for future backups.";
        }
        
        builder.setMessage(message);
        
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
        
        String message;
        if (hasExistingSafPermission()) {
            String savedUriString = preferences.getString(SAF_TREE_URI_KEY, "");
            Uri savedUri = Uri.parse(savedUriString);
            DocumentFile folder = DocumentFile.fromTreeUri(this, savedUri);
            DocumentFile backupFile = folder != null ? folder.findFile(BACKUP_FILENAME) : null;
            
            if (backupFile != null && backupFile.exists()) {
                String folderName = folder.getName();
                message = "This will restore your data from the backup file in \"" + folderName + "\". " +
                          "All your current data will be replaced. Are you sure you want to continue?";
            } else {
                message = "No backup file was found in the previously selected folder. " +
                          "You'll need to select a folder containing a valid backup file.";
            }
        } else {
            message = "This will restore your data from a backup file. " +
                      "You'll need to select a folder containing your backup file. " +
                      "All your current data will be replaced. Are you sure you want to continue?";
        }
        
        builder.setMessage(message);
        
        builder.setPositiveButton("Restore", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                restoreBackup();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    /**
     * Check if we already have a backup folder permission saved
     */
    private boolean hasExistingSafPermission() {
        String savedUriString = preferences.getString(SAF_TREE_URI_KEY, "");
        if (savedUriString.isEmpty()) {
            return false;
        }
        
        Uri treeUri = Uri.parse(savedUriString);
        
        // Verify we still have the permission
        int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        List<UriPermission> permissions = getContentResolver().getPersistedUriPermissions();
        for (UriPermission permission : permissions) {
            if (permission.getUri().equals(treeUri) && 
                (permission.isReadPermission() && permission.isWritePermission())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Launch document tree picker to get a persistable URI permission
     */
    private void requestSafFolderPermission() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION 
                      | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                      | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                      
        folderPickerLauncher.launch(intent);
    }
    
    /**
     * Starts the backup process - either requests SAF permission or performs backup
     */
    private void createBackup() {
        isBackupOperation = true;
        
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        backupButton.setEnabled(false);
        restoreButton.setEnabled(false);
        
        if (!hasExistingSafPermission()) {
            Log.d("BackupRestore", "No saved folder permission, requesting new one");
            requestSafFolderPermission();
        } else {
            Log.d("BackupRestore", "Using existing folder permission");
            performBackup();
        }
    }
    
    /**
     * Starts the restore process - either requests SAF permission or performs restore
     */
    private void restoreBackup() {
        isBackupOperation = false;
        
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        backupButton.setEnabled(false);
        restoreButton.setEnabled(false);
        
        if (!hasExistingSafPermission()) {
            Log.d("BackupRestore", "No saved folder permission, requesting new one");
            requestSafFolderPermission();
        } else {
            Log.d("BackupRestore", "Using existing folder permission");
            performRestore();
        }
    }
    
    /**
     * Creates and writes the backup file using the SAF DocumentFile API
     */
    private void performBackup() {
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
                    
                    // Add sobriety start date from SobrietyTracker
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
                    
                    // Add dark theme settings from both possible locations
                    SharedPreferences themePrefs = getSharedPreferences("ThemePreferences", MODE_PRIVATE);
                    JSONObject themeSettings = new JSONObject();
                    themeSettings.put("dark_mode_enabled", themePrefs.getBoolean("dark_mode_enabled", false));
                    themeSettings.put("follow_system_theme", themePrefs.getBoolean("follow_system_theme", true));
                    
                    // Check alternate theme location
                    SharedPreferences mainPrefs = getSharedPreferences("SobrietyTrackerPrefs", MODE_PRIVATE);
                    boolean nightModeEnabled = mainPrefs.getBoolean("night_mode_enabled", false);
                    themeSettings.put("night_mode_enabled", nightModeEnabled);
                    
                    backupData.put("theme_settings", themeSettings);
                    
                    // Add intrusive notification settings
                    SharedPreferences intrusivePrefs = getSharedPreferences("IntrusiveNotificationPrefs", MODE_PRIVATE);
                    JSONObject intrusiveSettings = new JSONObject();
                    intrusiveSettings.put("intrusive_enabled", intrusivePrefs.getBoolean("intrusive_enabled", false));
                    intrusiveSettings.put("intrusive_frequency", intrusivePrefs.getString("intrusive_frequency", "medium"));
                    intrusiveSettings.put("intrusive_start_hour", intrusivePrefs.getInt("intrusive_start_hour", 9));
                    intrusiveSettings.put("intrusive_end_hour", intrusivePrefs.getInt("intrusive_end_hour", 21));
                    backupData.put("intrusive_notification_settings", intrusiveSettings);
                    
                    // Add accountability buddies
                    JSONArray buddiesArray = getAccountabilityBuddiesJson();
                    backupData.put("accountability_buddies", buddiesArray);
                    
                    // Add community support resources
                    JSONArray resourcesArray = getSupportResourcesJson();
                    backupData.put("support_resources", resourcesArray);
                    
                    // Add emergency contacts
                    JSONObject emergencyContacts = getEmergencyContactsJson();
                    backupData.put("emergency_contacts", emergencyContacts);
                    
                    // Add achievements
                    JSONArray achievementsArray = getAchievementsJson();
                    backupData.put("achievements", achievementsArray);
                    
                    // Write backup data to SAF document
                    writeSafBackupFile(backupData.toString());
                    
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
                            Toast.makeText(BackupRestoreActivity.this, 
                                          "Backup created successfully", 
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
    
    /**
     * Performs a restore operation using the SAF DocumentFile API
     */
    private void performRestore() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Read backup data from SAF document
                    String backupData = readSafBackupFile();
                    
                    if (backupData == null || backupData.isEmpty()) {
                        throw new IOException("Backup file is empty or not found");
                    }
                    
                    if (!isValidBackupContent(backupData)) {
                        throw new IOException("Invalid backup file format");
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
                    
                    // Restore dark theme settings from both possible locations
                    final boolean followSystemTheme;
                    final boolean darkModeEnabled;
                    boolean nightModeEnabled;
                    
                    if (backupJson.has("theme_settings")) {
                        JSONObject themeSettings = backupJson.getJSONObject("theme_settings");
                        
                        // Restore to ThemePreferences
                        SharedPreferences themePrefs = getSharedPreferences("ThemePreferences", MODE_PRIVATE);
                        SharedPreferences.Editor themeEditor = themePrefs.edit();
                        
                        darkModeEnabled = themeSettings.optBoolean("dark_mode_enabled", false);
                        followSystemTheme = themeSettings.optBoolean("follow_system_theme", true);
                        nightModeEnabled = themeSettings.optBoolean("night_mode_enabled", false);
                        
                        themeEditor.putBoolean("dark_mode_enabled", darkModeEnabled);
                        themeEditor.putBoolean("follow_system_theme", followSystemTheme);
                        themeEditor.apply();
                        
                        // Restore to main preferences which may be used by some activities
                        SharedPreferences mainPrefs = getSharedPreferences("SobrietyTrackerPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor mainEditor = mainPrefs.edit();
                        mainEditor.putBoolean("night_mode_enabled", nightModeEnabled);
                        mainEditor.apply();
                        
                        // Apply theme changes on the UI thread
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Apply theme changes
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && followSystemTheme) {
                                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                                } else if (darkModeEnabled) {
                                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                                } else {
                                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                                }
                            }
                        });
                    }
                    
                    // Restore intrusive notification settings
                    if (backupJson.has("intrusive_notification_settings")) {
                        JSONObject intrusiveSettings = backupJson.getJSONObject("intrusive_notification_settings");
                        SharedPreferences intrusivePrefs = getSharedPreferences("IntrusiveNotificationPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = intrusivePrefs.edit();
                        
                        editor.putBoolean("intrusive_enabled", intrusiveSettings.optBoolean("intrusive_enabled", false));
                        editor.putString("intrusive_frequency", intrusiveSettings.optString("intrusive_frequency", "medium"));
                        editor.putInt("intrusive_start_hour", intrusiveSettings.optInt("intrusive_start_hour", 9));
                        editor.putInt("intrusive_end_hour", intrusiveSettings.optInt("intrusive_end_hour", 21));
                        
                        editor.apply();
                    }
                    
                    // Restore emergency contacts
                    if (backupJson.has("emergency_contacts")) {
                        JSONObject emergencyContacts = backupJson.getJSONObject("emergency_contacts");
                        SharedPreferences emergencyPrefs = getSharedPreferences("EmergencyContactPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = emergencyPrefs.edit();
                        
                        editor.putString("sponsor_name", emergencyContacts.optString("sponsor_name", ""));
                        editor.putString("sponsor_phone", emergencyContacts.optString("sponsor_phone", ""));
                        editor.putString("therapist_name", emergencyContacts.optString("therapist_name", ""));
                        editor.putString("therapist_phone", emergencyContacts.optString("therapist_phone", ""));
                        editor.putString("emergency_contact_name", emergencyContacts.optString("emergency_contact_name", ""));
                        editor.putString("emergency_contact_phone", emergencyContacts.optString("emergency_contact_phone", ""));
                        
                        editor.apply();
                    }
                    
                    // Restore accountability buddies
                    if (backupJson.has("accountability_buddies")) {
                        JSONArray buddiesArray = backupJson.getJSONArray("accountability_buddies");
                        restoreAccountabilityBuddies(buddiesArray);
                    }
                    
                    // Restore community support resources
                    if (backupJson.has("support_resources")) {
                        JSONArray resourcesArray = backupJson.getJSONArray("support_resources");
                        restoreSupportResources(resourcesArray);
                        
                        // Also restore any custom resources to the SharedPreferences
                        restoreCustomSupportResources(resourcesArray);
                    }
                    
                    // Restore achievements
                    if (backupJson.has("achievements")) {
                        JSONArray achievementsArray = backupJson.getJSONArray("achievements");
                        restoreAchievements(achievementsArray);
                    }
                    
                    // Reschedule notifications based on restored settings - do this on UI thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            NotificationHelper.scheduleNotifications(BackupRestoreActivity.this);
                        }
                    });
                    
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
    
    /**
     * Write backup data to a document file using SAF
     */
    private void writeSafBackupFile(String data) throws IOException {
        String savedUriString = preferences.getString(SAF_TREE_URI_KEY, "");
        if (savedUriString.isEmpty()) {
            throw new IOException("No saved document tree URI");
        }
        
        Uri treeUri = Uri.parse(savedUriString);
        DocumentFile tree = DocumentFile.fromTreeUri(this, treeUri);
        
        if (tree == null || !tree.exists() || !tree.canWrite()) {
            throw new IOException("Cannot access or write to the selected folder");
        }
        
        // Delete existing backup file if it exists
        DocumentFile existing = tree.findFile(BACKUP_FILENAME);
        if (existing != null) {
            if (!existing.delete()) {
                Log.w("BackupRestore", "Could not delete existing backup file, but continuing anyway");
            }
        }
        
        // Create new backup file
        DocumentFile newFile = tree.createFile("application/json", BACKUP_FILENAME);
        if (newFile == null) {
            throw new IOException("Failed to create backup file");
        }
        
        // Write data to the file
        OutputStream out = null;
        try {
            out = getContentResolver().openOutputStream(newFile.getUri());
            if (out == null) {
                throw new IOException("Failed to open output stream");
            }
            out.write(data.getBytes());
            out.flush();
            
            Log.i("BackupRestore", "Successfully wrote backup to SAF file: " + newFile.getUri());
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    Log.e("BackupRestore", "Error closing output stream", e);
                }
            }
        }
    }
    
    /**
     * Read backup data from a document file using SAF
     */
    private String readSafBackupFile() throws IOException {
        String savedUriString = preferences.getString(SAF_TREE_URI_KEY, "");
        if (savedUriString.isEmpty()) {
            throw new IOException("No saved document tree URI");
        }
        
        Uri treeUri = Uri.parse(savedUriString);
        DocumentFile tree = DocumentFile.fromTreeUri(this, treeUri);
        
        if (tree == null || !tree.exists() || !tree.canRead()) {
            throw new IOException("Cannot access or read from the selected folder");
        }
        
        // Find backup file
        DocumentFile backupFile = tree.findFile(BACKUP_FILENAME);
        if (backupFile == null || !backupFile.exists()) {
            throw new IOException("Backup file not found");
        }
        
        // Read data from the file
        InputStream in = null;
        try {
            in = getContentResolver().openInputStream(backupFile.getUri());
            if (in == null) {
                throw new IOException("Failed to open input stream");
            }
            
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            
            String content = result.toString("UTF-8");
            Log.i("BackupRestore", "Successfully read backup from SAF file: " + backupFile.getUri());
            
            return content;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e("BackupRestore", "Error closing input stream", e);
                }
            }
        }
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
    
    /**
     * Helper method to read content from a URI
     */
    private String readUriContent(Uri uri) throws IOException {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is != null) {
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                return result.toString("UTF-8");
            }
        }
        return "";
    }
    
    /**
     * Check if content is a valid backup by verifying it contains expected JSON fields
     */
    private boolean isValidBackupContent(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        
        try {
            JSONObject json = new JSONObject(content);
            // Check for at least one of these key fields
            return json.has("journal_entries") || json.has("settings") || json.has("sobriety_start_date");
        } catch (JSONException e) {
            return false;
        }
    }
    
    private String readBackupFromFile() throws IOException {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        StringBuilder stringBuilder = new StringBuilder();
        boolean fileFound = false;
        String errorMessage = "";
        
        // Log start of backup read attempt
        Log.d("BackupRestore", "Starting to read backup file");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+ try to use saved URI first
            String savedUri = prefs.getString("backup_uri", "");
            if (!savedUri.isEmpty()) {
                Log.d("BackupRestore", "Found saved URI: " + savedUri);
                try {
                    Uri uri = Uri.parse(savedUri);
                    try {
                        InputStream is = getContentResolver().openInputStream(uri);
                        if (is != null) {
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    stringBuilder.append(line);
                                }
                                
                                if (stringBuilder.length() > 0) {
                                    Log.d("BackupRestore", "Successfully read backup from saved URI");
                                    return stringBuilder.toString();
                                } else {
                                    Log.w("BackupRestore", "Backup file from saved URI is empty");
                                }
                            } finally {
                                try {
                                    is.close();
                                } catch (IOException e) {
                                    Log.e("BackupRestore", "Error closing input stream", e);
                                }
                            }
                        } else {
                            Log.e("BackupRestore", "Could not open input stream for URI: " + uri);
                        }
                    } catch (Exception e) {
                        Log.e("BackupRestore", "Error reading from saved URI: " + savedUri, e);
                        errorMessage = "Error reading from saved URI: " + e.getMessage();
                    }
                } catch (Exception e) {
                    Log.e("BackupRestore", "Invalid saved URI: " + savedUri, e);
                    errorMessage = "Invalid saved URI: " + e.getMessage();
                    // Clear invalid URI from preferences
                    prefs.edit().remove("backup_uri").apply();
                }
            }
            
            // If saved URI didn't work, try exact filename search in Downloads
            Log.d("BackupRestore", "Searching for backup in MediaStore");
            try {
                Uri queryUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                String[] projection = {MediaStore.Downloads._ID};
                
                // First try exact match
                String selection = MediaStore.Downloads.DISPLAY_NAME + "=? AND " + 
                                MediaStore.Downloads.RELATIVE_PATH + " LIKE ?";
                String[] selectionArgs = {
                    BACKUP_FILENAME,
                    "%" + BACKUP_DIRECTORY + "%"
                };
                
                Cursor cursor = null;
                try {
                    cursor = getContentResolver().query(queryUri, projection, selection, selectionArgs, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID);
                        long id = cursor.getLong(idColumn);
                        Uri contentUri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id);
                        
                        Log.d("BackupRestore", "Found backup file in MediaStore: " + contentUri);
                        
                        // Save this URI for future use
                        prefs.edit().putString("backup_uri", contentUri.toString()).apply();
                        
                        stringBuilder.setLength(0); // Clear any previous content
                        InputStream is = null;
                        try {
                            is = getContentResolver().openInputStream(contentUri);
                            if (is != null) {
                                byte[] buffer = new byte[1024];
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                int bytesRead;
                                while ((bytesRead = is.read(buffer)) != -1) {
                                    baos.write(buffer, 0, bytesRead);
                                }
                                
                                String content = baos.toString("UTF-8");
                                if (!content.isEmpty()) {
                                    Log.d("BackupRestore", "Successfully read backup from MediaStore");
                                    fileFound = true;
                                    return content;
                                } else {
                                    Log.w("BackupRestore", "Backup file from MediaStore is empty");
                                }
                            }
                        } catch (Exception e) {
                            Log.e("BackupRestore", "Error reading from MediaStore URI: " + contentUri, e);
                            errorMessage = "Error reading from MediaStore: " + e.getMessage();
                        } finally {
                            if (is != null) {
                                try {
                                    is.close();
                                } catch (IOException e) {
                                    Log.e("BackupRestore", "Error closing input stream", e);
                                }
                            }
                        }
                    } else {
                        Log.d("BackupRestore", "No exact match found, trying broader search");
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                
                // If exact match didn't work, try broader search for any similar filenames
                if (!fileFound) {
                    String broadSelection = MediaStore.Downloads.DISPLAY_NAME + " LIKE ? AND " + 
                                        MediaStore.Downloads.RELATIVE_PATH + " LIKE ?";
                    String[] broadSelectionArgs = {
                        "%" + BACKUP_FILENAME.replace(".json", "") + "%",
                        "%" + BACKUP_DIRECTORY + "%"
                    };
                    
                    try {
                        cursor = getContentResolver().query(queryUri, projection, broadSelection, broadSelectionArgs, null);
                        if (cursor != null && cursor.getCount() > 0) {
                            Log.d("BackupRestore", "Found " + cursor.getCount() + " similar backup files");
                            
                            // Try each file until we find one that works
                            while (cursor.moveToNext()) {
                                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID);
                                long id = cursor.getLong(idColumn);
                                Uri contentUri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id);
                                
                                InputStream is = null;
                                try {
                                    is = getContentResolver().openInputStream(contentUri);
                                    if (is != null) {
                                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                        byte[] buffer = new byte[1024];
                                        int bytesRead;
                                        while ((bytesRead = is.read(buffer)) != -1) {
                                            baos.write(buffer, 0, bytesRead);
                                        }
                                        
                                        String content = baos.toString("UTF-8");
                                        if (!content.isEmpty()) {
                                            // Try to validate it's a proper backup by checking for key fields
                                            try {
                                                JSONObject testJson = new JSONObject(content);
                                                if (testJson.has("journal_entries") || testJson.has("settings")) {
                                                    Log.d("BackupRestore", "Found valid backup in similar file: " + contentUri);
                                                    // Save this URI for future use
                                                    prefs.edit().putString("backup_uri", contentUri.toString()).apply();
                                                    fileFound = true;
                                                    return content;
                                                }
                                            } catch (JSONException je) {
                                                Log.w("BackupRestore", "Found file is not valid JSON: " + contentUri);
                                                // Continue to next file
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e("BackupRestore", "Error reading similar backup file: " + contentUri, e);
                                    // Continue to next file
                                } finally {
                                    if (is != null) {
                                        try {
                                            is.close();
                                        } catch (IOException e) {
                                            Log.e("BackupRestore", "Error closing input stream", e);
                                        }
                                    }
                                }
                            }
                        } else {
                            Log.d("BackupRestore", "No similar backup files found");
                        }
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("BackupRestore", "Error searching MediaStore", e);
                errorMessage = "Error searching MediaStore: " + e.getMessage();
            }
        } else {
            // For older versions, first try the saved path
            String savedPath = prefs.getString("backup_path", "");
            if (!savedPath.isEmpty()) {
                Log.d("BackupRestore", "Trying to read from saved path: " + savedPath);
                File backupFile = new File(savedPath);
                if (backupFile.exists()) {
                    try {
                        String content = readFileAsString(backupFile);
                        if (!content.isEmpty()) {
                            Log.d("BackupRestore", "Successfully read backup from saved path");
                            return content;
                        } else {
                            Log.w("BackupRestore", "Backup file from saved path is empty");
                        }
                    } catch (Exception e) {
                        Log.e("BackupRestore", "Error reading from saved path: " + savedPath, e);
                        errorMessage = "Error reading from saved path: " + e.getMessage();
                    }
                } else {
                    Log.d("BackupRestore", "Backup file at saved path doesn't exist");
                }
            }
            
            // If saved path didn't work, try the standard location
            Log.d("BackupRestore", "Trying standard backup location");
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File backupDir = new File(downloadsDir, BACKUP_DIRECTORY);
            File backupFile = new File(backupDir, BACKUP_FILENAME);
            
            if (backupFile.exists()) {
                try {
                    String content = readFileAsString(backupFile);
                    if (!content.isEmpty()) {
                        Log.d("BackupRestore", "Successfully read backup from standard location");
                        
                        // Save this path for future use
                        prefs.edit().putString("backup_path", backupFile.getAbsolutePath()).apply();
                        
                        return content;
                    } else {
                        Log.w("BackupRestore", "Backup file from standard location is empty");
                    }
                } catch (Exception e) {
                    Log.e("BackupRestore", "Error reading from standard location", e);
                    errorMessage = "Error reading from standard location: " + e.getMessage();
                }
            } else {
                Log.d("BackupRestore", "Backup file doesn't exist at standard location");
                
                // Try to find any backup file in the directory
                if (backupDir.exists() && backupDir.isDirectory()) {
                    File[] possibleBackups = backupDir.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            return name.contains(BACKUP_FILENAME.replace(".json", "")) && name.endsWith(".json");
                        }
                    });
                    
                    if (possibleBackups != null && possibleBackups.length > 0) {
                        Log.d("BackupRestore", "Found " + possibleBackups.length + " possible backup files");
                        
                        // Try each file until we find one that works
                        for (File file : possibleBackups) {
                            try {
                                String content = readFileAsString(file);
                                if (!content.isEmpty()) {
                                    // Try to validate it's a proper backup by checking for key fields
                                    try {
                                        JSONObject testJson = new JSONObject(content);
                                        if (testJson.has("journal_entries") || testJson.has("settings")) {
                                            Log.d("BackupRestore", "Found valid backup in file: " + file.getPath());
                                            
                                            // Save this path for future use
                                            prefs.edit().putString("backup_path", file.getAbsolutePath()).apply();
                                            
                                            return content;
                                        }
                                    } catch (JSONException je) {
                                        Log.w("BackupRestore", "Found file is not valid JSON: " + file.getPath());
                                        // Continue to next file
                                    }
                                }
                            } catch (Exception e) {
                                Log.e("BackupRestore", "Error reading possible backup file: " + file.getPath(), e);
                                // Continue to next file
                            }
                        }
                    }
                }
            }
        }
        
        // If we get here, we couldn't find a backup file
        Log.w("BackupRestore", "No backup file found. Last error: " + errorMessage);
        return null;
    }
    
    // Helper method to read file contents as string
    private String readFileAsString(File file) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
            
            // Try reading line by line first
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            
            if (stringBuilder.length() == 0) {
                // If that didn't work, try reading as binary
                fis.getChannel().position(0); // Reset position to beginning
                byte[] buffer = new byte[1024];
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                return baos.toString("UTF-8");
            }
        }
        
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
    
    private JSONArray getAccountabilityBuddiesJson() throws JSONException {
        JSONArray buddiesArray = new JSONArray();
        
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.query("accountability_buddy", null, null, null, null, null, null);
        
        if (cursor.moveToFirst()) {
            do {
                JSONObject buddy = new JSONObject();
                buddy.put("id", cursor.getLong(cursor.getColumnIndex("_id")));
                buddy.put("name", cursor.getString(cursor.getColumnIndex("name")));
                buddy.put("phone", cursor.getString(cursor.getColumnIndex("phone")));
                buddy.put("notify_on_checkin", cursor.getInt(cursor.getColumnIndex("notify_on_checkin")));
                buddy.put("notify_on_milestone", cursor.getInt(cursor.getColumnIndex("notify_on_milestone")));
                buddy.put("notify_on_relapse", cursor.getInt(cursor.getColumnIndex("notify_on_relapse")));
                
                buddiesArray.put(buddy);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return buddiesArray;
    }
    
    private void restoreAccountabilityBuddies(JSONArray buddiesArray) throws JSONException {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        
        // Clear existing buddies
        db.delete("accountability_buddy", null, null);
        
        // Insert backup buddies
        for (int i = 0; i < buddiesArray.length(); i++) {
            JSONObject buddyJson = buddiesArray.getJSONObject(i);
            
            ContentValues values = new ContentValues();
            values.put("_id", buddyJson.optLong("id"));
            values.put("name", buddyJson.optString("name"));
            values.put("phone", buddyJson.optString("phone"));
            values.put("notify_on_checkin", buddyJson.optInt("notify_on_checkin"));
            values.put("notify_on_milestone", buddyJson.optInt("notify_on_milestone"));
            values.put("notify_on_relapse", buddyJson.optInt("notify_on_relapse"));
            
            db.insert("accountability_buddy", null, values);
        }
    }
    
    private JSONArray getSupportResourcesJson() throws JSONException {
        JSONArray resourcesArray = new JSONArray();
        
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        
        // Check if the support_resources table exists
        Cursor tableCheck = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='support_resources'", 
                null);
        boolean tableExists = tableCheck.getCount() > 0;
        tableCheck.close();
        
        if (!tableExists) {
            // If table doesn't exist yet, return empty array
            return resourcesArray;
        }
        
        // If table exists, proceed with query
        Cursor cursor = db.query("support_resources", null, null, null, null, null, null);
        
        if (cursor.moveToFirst()) {
            do {
                JSONObject resource = new JSONObject();
                resource.put("id", cursor.getLong(cursor.getColumnIndex("id")));
                resource.put("name", cursor.getString(cursor.getColumnIndex("name")));
                resource.put("description", cursor.getString(cursor.getColumnIndex("description")));
                resource.put("phone", cursor.getString(cursor.getColumnIndex("phone")));
                resource.put("website", cursor.getString(cursor.getColumnIndex("website")));
                resource.put("category", cursor.getString(cursor.getColumnIndex("category")));
                resource.put("is_custom", cursor.getInt(cursor.getColumnIndex("is_custom")));
                
                resourcesArray.put(resource);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return resourcesArray;
    }
    
    private void restoreSupportResources(JSONArray resourcesArray) throws JSONException {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        
        // Only delete custom resources, keep default ones
        db.delete("support_resources", "is_custom = 1", null);
        
        // Insert backup custom resources
        for (int i = 0; i < resourcesArray.length(); i++) {
            JSONObject resourceJson = resourcesArray.getJSONObject(i);
            
            // Only restore custom resources
            if (resourceJson.optInt("is_custom") == 1) {
                ContentValues values = new ContentValues();
                values.put("name", resourceJson.optString("name"));
                values.put("description", resourceJson.optString("description"));
                values.put("phone", resourceJson.optString("phone"));
                values.put("website", resourceJson.optString("website"));
                values.put("category", resourceJson.optString("category"));
                values.put("is_custom", 1);
                
                db.insert("support_resources", null, values);
            }
        }
    }
    
    private JSONArray getAchievementsJson() throws JSONException {
        JSONArray achievementsArray = new JSONArray();
        
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        
        // Check if the achievements table exists
        Cursor tableCheck = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='achievements'", 
                null);
        boolean tableExists = tableCheck.getCount() > 0;
        tableCheck.close();
        
        if (!tableExists) {
            // Return empty array if table doesn't exist
            return achievementsArray;
        }
        
        // If table exists, proceed with query
        Cursor cursor = db.query("achievements", null, null, null, null, null, null);
        
        if (cursor.moveToFirst()) {
            do {
                JSONObject achievement = new JSONObject();
                achievement.put("id", cursor.getLong(cursor.getColumnIndex("id")));
                achievement.put("name", cursor.getString(cursor.getColumnIndex("name")));
                achievement.put("description", cursor.getString(cursor.getColumnIndex("description")));
                achievement.put("unlock_date", cursor.getLong(cursor.getColumnIndex("unlock_date")));
                achievement.put("achievement_type", cursor.getString(cursor.getColumnIndex("achievement_type")));
                achievement.put("unlocked", cursor.getInt(cursor.getColumnIndex("unlocked")));
                
                achievementsArray.put(achievement);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return achievementsArray;
    }
    
    private void restoreAchievements(JSONArray achievementsArray) throws JSONException {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        
        // We'll update existing achievements rather than deleting them all
        // This preserves any new achievements added in app updates
        
        for (int i = 0; i < achievementsArray.length(); i++) {
            JSONObject achievementJson = achievementsArray.getJSONObject(i);
            String name = achievementJson.optString("name");
            
            // Check if achievement exists
            Cursor cursor = db.query("achievements", 
                                   new String[]{"id"}, 
                                   "name = ?", 
                                   new String[]{name}, 
                                   null, null, null);
                                   
            boolean exists = cursor.moveToFirst();
            cursor.close();
            
            ContentValues values = new ContentValues();
            values.put("name", name);
            values.put("description", achievementJson.optString("description"));
            values.put("unlock_date", achievementJson.optLong("unlock_date"));
            values.put("achievement_type", achievementJson.optString("achievement_type"));
            values.put("unlocked", achievementJson.optInt("unlocked"));
            
            if (exists) {
                // Update existing achievement
                db.update("achievements", values, "name = ?", new String[]{name});
            } else {
                // Insert new achievement
                values.put("id", achievementJson.optLong("id"));
                db.insert("achievements", null, values);
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

    private JSONObject getEmergencyContactsJson() throws JSONException {
        JSONObject emergencyContacts = new JSONObject();
        
        // Get emergency contacts from their SharedPreferences file
        SharedPreferences emergencyPrefs = getSharedPreferences("EmergencyContactPrefs", MODE_PRIVATE);
        
        emergencyContacts.put("sponsor_name", emergencyPrefs.getString("sponsor_name", ""));
        emergencyContacts.put("sponsor_phone", emergencyPrefs.getString("sponsor_phone", ""));
        emergencyContacts.put("therapist_name", emergencyPrefs.getString("therapist_name", ""));
        emergencyContacts.put("therapist_phone", emergencyPrefs.getString("therapist_phone", ""));
        emergencyContacts.put("emergency_contact_name", emergencyPrefs.getString("emergency_contact_name", ""));
        emergencyContacts.put("emergency_contact_phone", emergencyPrefs.getString("emergency_contact_phone", ""));
        
        return emergencyContacts;
    }

    /**
     * Restores the custom support resources to the SharedPreferences file
     * used by the CommunitySupportActivity
     */
    private void restoreCustomSupportResources(JSONArray resourcesArray) throws JSONException {
        // Filter resources to just get the custom ones
        List<SupportResource> customResources = new ArrayList<>();
        
        for (int i = 0; i < resourcesArray.length(); i++) {
            JSONObject resourceJson = resourcesArray.getJSONObject(i);
            if (resourceJson.optInt("is_custom") == 1) {
                String name = resourceJson.optString("name", "");
                String description = resourceJson.optString("description", "");
                String website = resourceJson.optString("website", "");
                
                // Create SupportResource object and add to list
                SupportResource resource = new SupportResource(name, description, website, true);
                customResources.add(resource);
            }
        }
        
        // If we have custom resources, save them to SharedPreferences
        if (!customResources.isEmpty()) {
            try {
                // Convert to JSON string using the SupportResource.toJson method
                String json = SupportResource.toJson(customResources);
                
                // Save to the CommunitySupportActivity preferences file
                SharedPreferences prefs = getSharedPreferences("CommunitySupportPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("custom_resources", json);
                editor.apply();
                
                Log.d("BackupRestore", "Restored " + customResources.size() + " custom support resources");
            } catch (Exception e) {
                Log.e("BackupRestore", "Error saving custom resources to SharedPreferences", e);
            }
        }
    }
}
