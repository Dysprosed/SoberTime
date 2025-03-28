package com.example.sobertime;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Helper class to show celebration dialogs for sobriety milestones
 */
public class MilestoneCelebration {
    
    /**
     * Show a celebration dialog for a milestone
     */
    public static void showCelebrationDialog(Context context, int daysSober) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_milestone_celebration);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);
        
        // Get dialog views
        TextView titleTextView = dialog.findViewById(R.id.celebrationTitleText);
        TextView messageTextView = dialog.findViewById(R.id.celebrationMessageText);
        ImageView celebrationImageView = dialog.findViewById(R.id.celebrationImageView);
        Button shareButton = dialog.findViewById(R.id.shareButton);
        Button closeButton = dialog.findViewById(R.id.closeButton);
        
        // Set celebration content based on milestone
        setCelebrationContent(context, daysSober, titleTextView, messageTextView, celebrationImageView);
        
        // Add animations
        Animation pulseAnimation = AnimationUtils.loadAnimation(context, R.anim.pulse_animation);
        celebrationImageView.startAnimation(pulseAnimation);
        
        // Setup share button
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareMilestone(context, daysSober);
            }
        });
        
        // Setup close button
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        
        // Auto dismiss after 10 seconds
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            }
        }, 10000);
        
        dialog.show();
    }
    
    /**
     * Set the content of the celebration dialog based on the milestone
     */
    private static void setCelebrationContent(Context context, int daysSober, 
                                              TextView titleTextView, 
                                              TextView messageTextView, 
                                              ImageView celebrationImageView) {
        String title;
        String message;
        int imageResId = R.drawable.ic_notification; // Default image, update with actual image resources
        
        if (daysSober == 1) {
            title = "First Day Milestone!";
            message = "Congratulations on your first day of sobriety! This is where it all begins. Every journey starts with a single step, and you've taken that brave first step.";
        } else if (daysSober == 7) {
            title = "One Week Milestone!";
            message = "A full week of sobriety! Your body is already thanking you. The first week can be one of the hardest, and you conquered it!";
        } else if (daysSober == 14) {
            title = "Two Weeks Milestone!";
            message = "Two weeks strong! Your determination is inspiring. You're establishing healthy patterns that will serve you well going forward.";
        } else if (daysSober == 30) {
            title = "One Month Milestone!";
            message = "A whole month of sobriety! This is a major achievement. Your mind and body are experiencing significant healing already.";
        } else if (daysSober == 60) {
            title = "Two Months Milestone!";
            message = "60 days of choosing yourself and your health. You're building incredible strength and resilience every day.";
        } else if (daysSober == 90) {
            title = "Three Months Milestone!";
            message = "90 days sober! This is a huge milestone in recovery. Many experts consider this a critical turning point. Your new habits are taking root.";
        } else if (daysSober == 180) {
            title = "Six Months Milestone!";
            message = "Half a year of sobriety! You've faced challenges, weathered storms, and emerged stronger. Your dedication is paying off in countless ways.";
        } else if (daysSober == 365) {
            title = "One Year Milestone!";
            message = "ONE YEAR SOBER! This incredible achievement deserves massive celebration. You've experienced all seasons, holidays, and challenges while maintaining your sobriety. You are amazing!";
        } else if (daysSober % 365 == 0) {
            int years = daysSober / 365;
            title = years + " Year" + (years > 1 ? "s" : "") + " Milestone!";
            message = "Incredible! " + years + " year" + (years > 1 ? "s" : "") + " of sobriety is a monumental achievement. Your journey inspires others and shows what determination and commitment can accomplish.";
        } else {
            title = daysSober + " Days Milestone!";
            message = "Congratulations on " + daysSober + " days of sobriety! Every day is a victory worth celebrating. Keep going, you're doing amazing!";
        }
        
        titleTextView.setText(title);
        messageTextView.setText(message);
        celebrationImageView.setImageResource(imageResId);
    }
    
    /**
     * Share milestone achievement with others
     */
    private static void shareMilestone(Context context, int daysSober) {
        String milestoneText;
        
        if (daysSober == 1) {
            milestoneText = "I'm celebrating my first day of sobriety! Every journey begins with a single step. #SobrietyJourney";
        } else if (daysSober == 7) {
            milestoneText = "7 days sober! One week down, a lifetime of health and clarity ahead. #SobrietyJourney";
        } else if (daysSober == 30) {
            milestoneText = "I'm celebrating 1 month of sobriety today! Every day is a victory. #SobrietyJourney";
        } else if (daysSober == 90) {
            milestoneText = "90 days sober! A major milestone in my recovery journey. Grateful for each day. #SobrietyJourney";
        } else if (daysSober == 365) {
            milestoneText = "ONE YEAR SOBER TODAY! 365 days of choosing health and clarity. This journey has transformed my life. #SobrietyJourney";
        } else {
            milestoneText = "Today I'm celebrating " + daysSober + " days of sobriety! Every day is a gift. #SobrietyJourney";
        }
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, milestoneText);
        context.startActivity(Intent.createChooser(shareIntent, "Share Your Milestone"));
    }
}
