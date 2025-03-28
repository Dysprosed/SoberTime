package com.example.sobertime;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import androidx.core.app.NotificationCompat;

import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String PREFS_NAME = "SobrietyTrackerPrefs";
    private static final String START_DATE_KEY = "sobriety_start_date";
    private static final int NOTIFICATION_ID = 1001;
    private static final int MILESTONE_NOTIFICATION_ID = 2001;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Create the notification channel
        NotificationHelper.createNotificationChannel(context);

        String notificationType = intent.getStringExtra("notification_type");

        if (notificationType != null) {
            switch (notificationType) {
                case "morning":
                    sendMorningNotification(context);
                    break;
                case "evening":
                    sendEveningNotification(context);
                    break;
                case "milestone":
                    int milestoneDays = intent.getIntExtra("milestone_days", 0);
                    sendMilestoneNotification(context, milestoneDays);
                    break;
                case "custom":
                    sendCustomNotification(context);
                    break;
            }
        }
    }

    /**
     * Send morning motivation notification
     */
    private void sendMorningNotification(Context context) {
        int daysSober = getDaysSober(context);

        String[] morningMessages = {
                "Good morning! You've been sober for " + daysSober + " days. Today is a new opportunity to stay strong.",
                "Rise and shine! Another day of sobriety awaits. You've got this!",
                "Morning reminder: " + daysSober + " days sober and counting. Be proud of your journey.",
                "Start your day with confidence. " + daysSober + " days of choosing health and clarity.",
                "Good morning! Remember why you started this journey " + daysSober + " days ago.",
                "A new day, a renewed commitment. You're doing amazing!",
                "Morning motivation: Every day sober is a victory. Today makes " + daysSober + "!",
                "Today is day " + daysSober + " of your incredible journey. Make it count!"
        };

        // Pick a random message
        String message = morningMessages[new Random().nextInt(morningMessages.length)];

        // Build and send notification
        sendNotification(context, "Morning Motivation", message, NOTIFICATION_ID);
    }

    /**
     * Send evening check-in notification
     */
    private void sendEveningNotification(Context context) {
        int daysSober = getDaysSober(context);

        String[] eveningMessages = {
                "Evening check-in: You've made it through another day! That's " + daysSober + " days of strength.",
                "Reflect on your " + daysSober + " day journey. Each day is an achievement.",
                "Evening reminder: You've stayed sober for " + daysSober + " days. Keep going!",
                "Another sober day complete. You're building a healthier future with each passing day.",
                "You've made it through today. " + daysSober + " days of choosing yourself.",
                "Tonight, celebrate " + daysSober + " days of sobriety. You deserve it!",
                "As day " + daysSober + " comes to a close, remember how far you've come.",
                "Evening reflection: " + daysSober + " days of growth and healing. Be proud."
        };

        // Pick a random message
        String message = eveningMessages[new Random().nextInt(eveningMessages.length)];

        // Build and send notification
        sendNotification(context, "Evening Check-in", message, NOTIFICATION_ID);
    }

    /**
     * Send milestone celebration notification
     */
    private void sendMilestoneNotification(Context context, int milestoneDays) {
        String title;
        String message;

        if (milestoneDays == 1) {
            title = "First Day Milestone!";
            message = "Congratulations on your first day of sobriety! This is where it all begins.";
        } else if (milestoneDays == 7) {
            title = "One Week Milestone!";
            message = "A full week of sobriety! Your body is already thanking you.";
        } else if (milestoneDays == 30) {
            title = "One Month Milestone!";
            message = "A whole month of sobriety! This is a major achievement.";
        } else if (milestoneDays == 90) {
            title = "90 Day Milestone!";
            message = "90 days sober! This is a huge turning point in recovery.";
        } else if (milestoneDays == 180) {
            title = "Six Months Milestone!";
            message = "Half a year of sobriety! Your dedication is paying off in countless ways.";
        } else if (milestoneDays == 365) {
            title = "ONE YEAR MILESTONE!";
            message = "A FULL YEAR OF SOBRIETY! This incredible achievement deserves massive celebration!";
        } else {
            title = milestoneDays + " Day Milestone!";
            message = "Congratulations on " + milestoneDays + " days of sobriety! Every day is a victory worth celebrating.";
        }

        // Create intent that opens the main activity
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Get notification sound
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Build the notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, "sobriety_tracker_channel")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        // Get notification manager
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Show notification
        notificationManager.notify(MILESTONE_NOTIFICATION_ID, notificationBuilder.build());
    }

    /**
     * Send custom time notification
     */
    private void sendCustomNotification(Context context) {
        int daysSober = getDaysSober(context);

        String[] customMessages = {
                "You've been sober for " + daysSober + " days. Each day is a victory.",
                "Reminder: " + daysSober + " days into your sobriety journey. Stay strong.",
                "Just checking in: " + daysSober + " days sober and counting!",
                "Remember your commitment: " + daysSober + " days of choosing health.",
                "You're doing great! " + daysSober + " days of sobriety is impressive.",
                "Stay focused on your goal. " + daysSober + " days and counting!",
                "Take a moment to appreciate your " + daysSober + " day accomplishment.",
                "Every hour sober is a victory. You've had " + (daysSober * 24) + " hours of victories!"
        };

        // Pick a random message
        String message = customMessages[new Random().nextInt(customMessages.length)];

        // Get current hour to customize title
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String title;

        if (currentHour >= 5 && currentHour < 12) {
            title = "Morning Reminder";
        } else if (currentHour >= 12 && currentHour < 17) {
            title = "Afternoon Reminder";
        } else if (currentHour >= 17 && currentHour < 21) {
            title = "Evening Reminder";
        } else {
            title = "Night Reminder";
        }

        // Build and send notification
        sendNotification(context, title, message, NOTIFICATION_ID);
    }

    /**
     * Helper method to build and send a standard notification
     */
    private void sendNotification(Context context, String title, String message, int notificationId) {
        // Create intent that opens the main activity
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Get notification sound
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Build the notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, "sobriety_tracker_channel")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent);

        // Get notification manager
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Show notification
        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    /**
     * Get days sober based on saved start date
     */
    private int getDaysSober(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long startDate = prefs.getLong(START_DATE_KEY, System.currentTimeMillis());

        long currentTime = System.currentTimeMillis();
        long diffInMillis = currentTime - startDate;

        return (int) TimeUnit.MILLISECONDS.toDays(diffInMillis);
    }
}
