package com.example.sobertime;

import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.sobertime.SobrietyTracker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class IntrusiveCheckInActivity extends BaseActivity {
    
    private MediaPlayer mediaPlayer;
    private PowerManager.WakeLock wakeLock;
    private Vibrator vibrator;
    private boolean isPlaying = false;
    private Button soberButton;
    private Button relapseButton;
    private TextView dateTextView;
    private TextView timeTextView;
    private TextView messageTextView;
    private DatabaseHelper databaseHelper;
    private static final int NOTIFICATION_ID = 3001;
    private static final String TAG = "IntrusiveCheckIn";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Make sure this activity shows even on the lock screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                
        setContentView(R.layout.activity_intrusive_check_in);
        
        // Cancel the notification that launched this activity
        cancelNotification();
        
        // Try to wake up the device, but handle permission issues gracefully
        try {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK, 
                    "SoberTime:IntrusiveCheckInWakeLock");
            wakeLock.acquire(10*60*1000L); // 10 minutes max
            Log.d(TAG, "WakeLock acquired successfully");
        } catch (SecurityException e) {
            // Permission issue - continue without wakelock
            Log.e(TAG, "Failed to acquire wakelock: " + e.getMessage());
            wakeLock = null;
        } catch (Exception e) {
            // Other issue - continue without wakelock
            Log.e(TAG, "Error with wakelock: " + e.getMessage());
            wakeLock = null;
        }
        
        // Initialize views
        initializeViews();
        
        // Initialize database helper
        databaseHelper = DatabaseHelper.getInstance(this);
        
        // Set current date and time
        updateDateTime();
        
        // Start alarm and vibration
        startAlarmAndVibration();
        
        // Set up buttons
        setupListeners();
    }
    
    private void cancelNotification() {
        try {
            NotificationManager notificationManager = 
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(NOTIFICATION_ID);
            Log.d(TAG, "Cancelled notification");
        } catch (Exception e) {
            Log.e(TAG, "Error canceling notification: " + e.getMessage());
        }
    }

    private void initializeViews() {
        dateTextView = findViewById(R.id.intrusiveDateTextView);
        timeTextView = findViewById(R.id.intrusiveTimeTextView);
        messageTextView = findViewById(R.id.intrusiveMessageTextView);
        soberButton = findViewById(R.id.intrusiveSoberButton);
        relapseButton = findViewById(R.id.intrusiveRelapseButton);
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
                stopAlarmAndVibration();
                recordCheckIn(true);
                finish();
            }
        });
        
        // Relapse button
        relapseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopAlarmAndVibration();
                showRelapseConfirmationDialog();
            }
        });
    }
    
    private void startAlarmAndVibration() {
        SharedPreferences prefs = getSharedPreferences("notification_settings", MODE_PRIVATE);
        int alarmVolume = prefs.getInt("alarm_volume", 100); // Default to max
        boolean useVibration = prefs.getBoolean("use_vibration", true);
        
        try {
            // Get alarm sound
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmSound == null) {
                alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, alarmSound);
            
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            
            mediaPlayer.setAudioAttributes(attributes);
            mediaPlayer.setLooping(true);
            
            // Set volume based on settings
            float volume = alarmVolume / 100f;
            mediaPlayer.setVolume(volume, volume);
            
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPlaying = true;
            
            // Start vibration if enabled and permission is granted
            if (useVibration) {
                try {
                    vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrator != null && vibrator.hasVibrator()) {
                        // Pattern: wait 0ms, vibrate 500ms, wait 500ms, repeat
                        long[] pattern = {0, 500, 500};
                        vibrator.vibrate(pattern, 0); // 0 means repeat indefinitely
                    }
                } catch (SecurityException e) {
                    // No VIBRATE permission - log error and continue without vibration
                    Log.e("IntrusiveCheckIn", "No vibration permission: " + e.getMessage());
                    vibrator = null;
                } catch (Exception e) {
                    // Other error with vibrator
                    Log.e("IntrusiveCheckIn", "Vibration error: " + e.getMessage());
                    vibrator = null;
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void stopAlarmAndVibration() {
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            isPlaying = false;
        }
        
        if (vibrator != null) {
            try {
                vibrator.cancel();
            } catch (SecurityException e) {
                // Ignore security exceptions when trying to cancel vibration
                Log.e("IntrusiveCheckIn", "No permission to cancel vibration: " + e.getMessage());
            } catch (Exception e) {
                Log.e("IntrusiveCheckIn", "Error canceling vibration: " + e.getMessage());
            } finally {
                vibrator = null;
            }
        }
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
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // If they cancel, restart the alarm
                        if (!isPlaying) {
                            startAlarmAndVibration();
                        }
                    }
                })
                .show();
    }
    
    private void recordCheckIn(boolean maintainedSobriety) {
        SobrietyTracker sobrietyTracker = SobrietyTracker.getInstance(this);
        
        if (maintainedSobriety) {
            sobrietyTracker.confirmSobrietyForToday();
            Toast.makeText(this, "Sobriety confirmed for today!", Toast.LENGTH_SHORT).show();
        } else {
            sobrietyTracker.resetSobrietyCounter();
            Toast.makeText(this, "Counter reset. Every new beginning is progress.", Toast.LENGTH_LONG).show();
        }
        
        notifyBuddyIfNeeded(maintainedSobriety);
    }
    
    private void resetSobrietyCounter() {
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
                    Log.d("IntrusiveCheckInActivity", "Sent notification message to buddy: " + buddyName);
                } catch (Exception e) {
                    Log.e("IntrusiveCheckInActivity", "Failed to send message to buddy: " + e.getMessage());
                }
            }
        }
        
        cursor.close();
    }
    
    @Override
    protected void onDestroy() {
        stopAlarmAndVibration();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        super.onDestroy();
    }
    
    @Override
    public void onBackPressed() {
        // Prevent back button from dismissing without check-in
        // Do nothing
    }
}