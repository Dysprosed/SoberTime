package com.example.sobertime;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Make sure this activity shows even on the lock screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                
        setContentView(R.layout.activity_intrusive_check_in);
        
        // Wake up the device
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, 
                "SoberTime:IntrusiveCheckInWakeLock");
        wakeLock.acquire(10*60*1000L); // 10 minutes max
        
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
            
            // Start vibration if enabled
            if (useVibration) {
                vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    // Pattern: wait 0ms, vibrate 500ms, wait 500ms, repeat
                    long[] pattern = {0, 500, 500};
                    vibrator.vibrate(pattern, 0); // 0 means repeat indefinitely
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
            vibrator.cancel();
            vibrator = null;
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
        // Reuse existing buddy notification code from CheckInActivity
        // This method would be identical to the one in CheckInActivity
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