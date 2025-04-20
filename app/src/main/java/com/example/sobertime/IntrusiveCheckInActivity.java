package com.example.sobertime;

import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.VibrationEffect;
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
            // First ensure audio routing is properly set up
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            
            // Force the volume to maximum for alarm stream FIRST - this is critical
            try {
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0);
                Log.d(TAG, "Set alarm stream to maximum volume: " + maxVolume);
                
                // Save current ringer mode for restoring later if desired
                int originalRingerMode = audioManager.getRingerMode();
                
                // Force ringer mode to normal - this is key for sound playback
                if (originalRingerMode != AudioManager.RINGER_MODE_NORMAL) {
                    try {
                        Log.d(TAG, "Attempting to override ringer mode from " + originalRingerMode + " to NORMAL");
                        // This may fail on some devices due to DND permissions
                        audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                        Log.d(TAG, "Successfully switched ringer mode to: " + audioManager.getRingerMode());
                    } catch (SecurityException se) {
                        Log.e(TAG, "Security exception setting ringer mode: " + se.getMessage());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting volume: " + e.getMessage());
            }
            
            // Then request audio focus
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
                
                AudioFocusRequest focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setWillPauseWhenDucked(false)
                    .build();
                
                Log.d(TAG, "Requesting audio focus with exclusive gain");
                int res = audioManager.requestAudioFocus(focusRequest);
                Log.d(TAG, "Audio focus request result: " + res);
            } else {
                // For older Android versions
                int res = audioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);
                Log.d(TAG, "Legacy audio focus request result: " + res);
            }
            
            // Set up MediaPlayer with proper alarm audio path
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmSound == null) {
                alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                if (alarmSound == null) {
                    alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }
            }
            
            try {
                // Use a different approach - creating and starting the MediaPlayer immediately
                mediaPlayer = new MediaPlayer();
                
                // Configure flags and routing before setting data source
                if (Build.VERSION.SDK_INT >= 21) {
                    AudioAttributes attributes = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                            .build();
                    
                    mediaPlayer.setAudioAttributes(attributes);
                } else {
                    // For older devices
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                }
                
                // Set data source and prepare synchronously
                mediaPlayer.setDataSource(this, alarmSound);
                mediaPlayer.setLooping(true);
                
                // Set volume based on settings (use maximum volume)
                float volume = 1.0f; // Force maximum volume
                mediaPlayer.setVolume(volume, volume);
                
                // Prepare synchronously and start immediately
                mediaPlayer.prepare();
                mediaPlayer.start();
                isPlaying = true;
                
                Log.d(TAG, "Started MediaPlayer for alarm sound with maximum volume");
            } catch (Exception e) {
                Log.e(TAG, "Error with MediaPlayer: " + e.getMessage());
                try {
                    // Alternative method using Ringtone API
                    Ringtone ringtone = RingtoneManager.getRingtone(this, alarmSound);
                    if (Build.VERSION.SDK_INT >= 21) {
                        ringtone.setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build());
                    }
                    ringtone.play();
                    Log.d(TAG, "Started Ringtone as fallback");
                } catch (Exception e2) {
                    Log.e(TAG, "Failed to play sound with Ringtone API: " + e2.getMessage());
                }
            }
            
            // Start vibration if enabled and permission is granted - using a separate thread that continuously vibrates
            if (useVibration) {
                try {
                    vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrator != null && vibrator.hasVibrator()) {
                        // Start vibration in a more aggressive pattern
                        // Pattern: wait 0ms, vibrate 800ms, wait 400ms, vibrate 800ms, wait 400ms...
                        long[] pattern = {0, 800, 400, 800, 400};
                        
                        // Force stronger vibration on newer devices
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            // Use maximum amplitude (255) for stronger vibration
                            VibrationEffect vibrationEffect = VibrationEffect.createWaveform(pattern, new int[]{0, 255, 0, 255, 0}, 0);
                            vibrator.vibrate(vibrationEffect);
                            Log.d(TAG, "Started high-amplitude vibration on Android O+");
                        } else {
                            vibrator.vibrate(pattern, 0); // 0 means repeat indefinitely
                            Log.d(TAG, "Started standard vibration on pre-O device");
                        }
                    } else {
                        Log.d(TAG, "Device does not support vibration");
                    }
                } catch (SecurityException e) {
                    Log.e(TAG, "No vibration permission: " + e.getMessage());
                    vibrator = null;
                } catch (Exception e) {
                    Log.e(TAG, "Vibration error: " + e.getMessage());
                    vibrator = null;
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Critical error starting alarm sound: " + e.getMessage());
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