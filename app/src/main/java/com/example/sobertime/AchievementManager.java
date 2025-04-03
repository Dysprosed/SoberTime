package com.example.sobertime;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.example.sobertime.model.SobrietyTracker;  

/**
 * Manages achievements, checks for unlocks, and handles notifications
 */
public class AchievementManager {
    
    private static final String PREFS_NAME = "AchievementsPrefs";
    private static final String ACHIEVEMENTS_KEY = "achievements_data";
    private static final String CHANNEL_ID = "achievement_channel";
    private static final String CHANNEL_NAME = "Achievements";
    private static final String CHANNEL_DESC = "Notifications for unlocked achievements";
    
    private Context context;
    private List<Achievement> achievements;
    private Map<Integer, Achievement> achievementMap;
    private SharedPreferences preferences;
    private SobrietyTracker sobrietyTracker;
    
    private static AchievementManager instance;
    
    // Create singleton instance
    public static synchronized AchievementManager getInstance(Context context) {
        if (instance == null) {
            instance = new AchievementManager(context.getApplicationContext());
        }
        return instance;
    }
    
    private AchievementManager(Context context) {
        this.context = context.getApplicationContext();
        this.databaseHelper = DatabaseHelper.getInstance(context);
        
        // Initialize SobrietyTracker
        this.sobrietyTracker = SobrietyTracker.getInstance(context);
        
        // Load achievements
        loadAchievements();
    }

    /**
     * Check for achievements based on current day count from SobrietyTracker
     * @return The achievement unlocked today or null if none
     */
    public Achievement checkTimeAchievements() {
        int daysSober = sobrietyTracker.getDaysSober();
        return checkTimeAchievements(daysSober);
    }

    /**
     * Get the next milestone based on current day count from SobrietyTracker
     * @return The next milestone achievement or null if none
     */
    public Achievement getNextMilestone() {
        int daysSober = sobrietyTracker.getDaysSober();
        return getNextMilestone(daysSober);
    }

    /**
     * Check for financial achievements using calculated money saved from SobrietyTracker
     */
    public void checkFinancialAchievements() {
        // Get settings from database
        float drinkCost = databaseHelper.getFloatSetting("drink_cost", 8.50f);
        int drinksPerWeek = databaseHelper.getIntSetting("drinks_per_week", 15);
        
        // Calculate money saved using SobrietyTracker
        float moneySaved = sobrietyTracker.calculateMoneySaved(drinkCost, drinksPerWeek);
        
        // Use the existing method
        checkFinancialAchievements(moneySaved);
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESC);
            
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    private void initializeAchievements() {
        // Time milestones (with day counts)
        addAchievement(new Achievement(
                1,
                "First Day",
                "Complete your first day of sobriety",
                "ic_achievement_day1",
                Achievement.AchievementCategory.TIME_MILESTONE,
                1
        ));
        
        addAchievement(new Achievement(
                2,
                "One Week Strong",
                "Complete one week of sobriety",
                "ic_achievement_week1",
                Achievement.AchievementCategory.TIME_MILESTONE,
                7
        ));
        
        addAchievement(new Achievement(
                3,
                "Two Weeks Milestone",
                "Complete two weeks of sobriety",
                "ic_achievement_week2",
                Achievement.AchievementCategory.TIME_MILESTONE,
                14
        ));
        
        addAchievement(new Achievement(
                4,
                "30 Day Milestone",
                "Complete one month of sobriety",
                "ic_achievement_month1",
                Achievement.AchievementCategory.TIME_MILESTONE,
                30
        ));
        
        addAchievement(new Achievement(
                5,
                "60 Day Milestone",
                "Complete two months of sobriety",
                "ic_achievement_60days",
                Achievement.AchievementCategory.TIME_MILESTONE,
                60
        ));
        
        addAchievement(new Achievement(
                6,
                "90 Day Warrior",
                "Complete three months of sobriety",
                "ic_achievement_90days",
                Achievement.AchievementCategory.TIME_MILESTONE,
                90
        ));
        
        addAchievement(new Achievement(
                7,
                "Half Year Hero",
                "Complete six months of sobriety",
                "ic_achievement_6months",
                Achievement.AchievementCategory.TIME_MILESTONE,
                180
        ));
        
        addAchievement(new Achievement(
                8,
                "One Year Champion",
                "Complete one year of sobriety",
                "ic_achievement_1year",
                Achievement.AchievementCategory.TIME_MILESTONE,
                365
        ));
        
        addAchievement(new Achievement(
                9,
                "Two Year Legend",
                "Complete two years of sobriety",
                "ic_achievement_2years",
                Achievement.AchievementCategory.TIME_MILESTONE,
                730
        ));
        
        addAchievement(new Achievement(
                10,
                "Three Year Inspiration",
                "Complete three years of sobriety",
                "ic_achievement_3years",
                Achievement.AchievementCategory.TIME_MILESTONE,
                1095
        ));
        
        // Journal achievements
        addAchievement(new Achievement(
                101,
                "Journal Beginner",
                "Create your first journal entry",
                "ic_achievement_journal1",
                Achievement.AchievementCategory.JOURNAL
        ));
        
        addAchievement(new Achievement(
                102,
                "Consistent Journaler",
                "Create journal entries for 5 consecutive days",
                "ic_achievement_journal_streak",
                Achievement.AchievementCategory.JOURNAL
        ));
        
        addAchievement(new Achievement(
                103,
                "Journal Master",
                "Create 30 journal entries",
                "ic_achievement_journal_master",
                Achievement.AchievementCategory.JOURNAL
        ));
        
        // Financial achievements
        addAchievement(new Achievement(
                201,
                "Money Saver",
                "Save $100 from not drinking",
                "ic_achievement_money100",
                Achievement.AchievementCategory.FINANCIAL
        ));
        
        addAchievement(new Achievement(
                202,
                "Big Spender No More",
                "Save $500 from not drinking",
                "ic_achievement_money500",
                Achievement.AchievementCategory.FINANCIAL
        ));
        
        addAchievement(new Achievement(
                203,
                "Financial Freedom",
                "Save $1000 from not drinking",
                "ic_achievement_money1000",
                Achievement.AchievementCategory.FINANCIAL
        ));
        
        // Health achievements
        addAchievement(new Achievement(
                301,
                "Health Conscious",
                "Save 2,000 calories from not drinking",
                "ic_achievement_calories",
                Achievement.AchievementCategory.HEALTH
        ));
        
        addAchievement(new Achievement(
                302,
                "Better Sleep",
                "Experience improved sleep after 1 week sober",
                "ic_achievement_sleep",
                Achievement.AchievementCategory.HEALTH
        ));
        
        // App usage achievements
        addAchievement(new Achievement(
                401,
                "App Explorer",
                "Visit all sections of the app",
                "ic_achievement_explorer",
                Achievement.AchievementCategory.APP_USAGE
        ));
    }
    
    private void addAchievement(Achievement achievement) {
        achievements.add(achievement);
        achievementMap.put(achievement.getId(), achievement);
    }
    
    private void loadAchievements() {
        String jsonData = preferences.getString(ACHIEVEMENTS_KEY, null);
        
        if (jsonData != null) {
            try {
                JSONArray jsonArray = new JSONArray(jsonData);
                
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    int id = jsonObject.getInt("id");
                    boolean unlocked = jsonObject.getBoolean("unlocked");
                    long unlockTime = jsonObject.getLong("unlockTime");
                    
                    Achievement achievement = achievementMap.get(id);
                    if (achievement != null) {
                        achievement.setUnlocked(unlocked);
                        achievement.setUnlockTime(unlockTime);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void saveAchievements() {
        try {
            JSONArray jsonArray = new JSONArray();
            
            for (Achievement achievement : achievements) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", achievement.getId());
                jsonObject.put("unlocked", achievement.isUnlocked());
                jsonObject.put("unlockTime", achievement.getUnlockTime());
                jsonArray.put(jsonObject);
            }
            
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(ACHIEVEMENTS_KEY, jsonArray.toString());
            editor.apply();
            
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    public List<Achievement> getAllAchievements() {
        return achievements;
    }
    
    public List<Achievement> getUnlockedAchievements() {
        List<Achievement> unlocked = new ArrayList<>();
        for (Achievement achievement : achievements) {
            if (achievement.isUnlocked()) {
                unlocked.add(achievement);
            }
        }
        return unlocked;
    }
    
    public List<Achievement> getLockedAchievements() {
        List<Achievement> locked = new ArrayList<>();
        for (Achievement achievement : achievements) {
            if (!achievement.isUnlocked()) {
                locked.add(achievement);
            }
        }
        return locked;
    }
    
    /**
     * Get all time-based milestone achievements
     */
    public List<Achievement> getTimeMilestones() {
        List<Achievement> milestones = new ArrayList<>();
        for (Achievement achievement : achievements) {
            if (achievement.getCategory() == Achievement.AchievementCategory.TIME_MILESTONE) {
                milestones.add(achievement);
            }
        }
        return milestones;
    }
    
    /**
     * Update milestone dates based on sobriety start date
     */
    public void updateMilestoneDates(long sobrietyStartDate) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        
        for (Achievement achievement : achievements) {
            if (achievement.getCategory() == Achievement.AchievementCategory.TIME_MILESTONE) {
                // Calculate milestone date
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(sobrietyStartDate);
                calendar.add(Calendar.DAY_OF_YEAR, achievement.getDaysRequired());
                
                Date milestoneDate = calendar.getTime();
                String formattedDate = dateFormat.format(milestoneDate);
                achievement.setMilestoneDate(formattedDate);
            }
        }
    }
    
    /**
     * Check for achievement unlocks based on days sober
     * Returns the achievement if today is a milestone, null otherwise
     */
    public Achievement checkTimeAchievements(int daysSober) {
        boolean newAchievement = false;
        Achievement todaysMilestone = null;
        
        // Reset "today" flag for all achievements
        for (Achievement achievement : achievements) {
            if (achievement.getCategory() == Achievement.AchievementCategory.TIME_MILESTONE) {
                achievement.setToday(false);
            }
        }
        
        // Check each time milestone
        for (Achievement achievement : achievements) {
            if (achievement.getCategory() == Achievement.AchievementCategory.TIME_MILESTONE) {
                int daysRequired = achievement.getDaysRequired();
                
                if (daysSober >= daysRequired) {
                    // Achievement should be unlocked
                    if (!achievement.isUnlocked()) {
                        achievement.setUnlocked(true);
                        newAchievement = true;
                    }
                    
                    // Check if this is today's milestone
                    if (daysSober == daysRequired) {
                        achievement.setToday(true);
                        todaysMilestone = achievement;
                    }
                }
            }
        }
        
        if (newAchievement) {
            saveAchievements();
        }
        
        return todaysMilestone;
    }
    
    /**
     * Get the next milestone based on current day count
     */
    public Achievement getNextMilestone(int daysSober) {
        Achievement nextMilestone = null;
        int daysToNext = Integer.MAX_VALUE;
        
        for (Achievement achievement : getTimeMilestones()) {
            int days = achievement.getDaysRequired();
            if (days > daysSober && (days - daysSober) < daysToNext) {
                nextMilestone = achievement;
                daysToNext = days - daysSober;
            }
        }
        
        return nextMilestone;
    }
    
    /**
     * Check for financial achievements based on money saved
     */
    public void checkFinancialAchievements(float moneySaved) {
        boolean newAchievement = false;
        
        if (moneySaved >= 100) {
            newAchievement |= unlockAchievement(201);
        }
        
        if (moneySaved >= 500) {
            newAchievement |= unlockAchievement(202);
        }
        
        if (moneySaved >= 1000) {
            newAchievement |= unlockAchievement(203);
        }
        
        if (newAchievement) {
            saveAchievements();
        }
    }
    
    /**
     * Check for journal achievements
     */
    public void checkJournalAchievements(List<JournalEntry> entries) {
        boolean newAchievement = false;
        
        // First journal entry
        if (entries.size() >= 1) {
            newAchievement |= unlockAchievement(101);
        }
        
        // 30 journal entries
        if (entries.size() >= 30) {
            newAchievement |= unlockAchievement(103);
        }
        
        // 5 consecutive days
        if (checkConsecutiveDaysJournaling(entries, 5)) {
            newAchievement |= unlockAchievement(102);
        }
        
        if (newAchievement) {
            saveAchievements();
        }
    }
    
    private boolean checkConsecutiveDaysJournaling(List<JournalEntry> entries, int days) {
        if (entries.size() < days) {
            return false;
        }
        
        // Sort entries by timestamp in descending order (most recent first)
        entries.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        
        long oneDayMillis = TimeUnit.DAYS.toMillis(1);
        long previousDay = -1;
        int consecutiveDays = 0;
        
        for (JournalEntry entry : entries) {
            long entryDay = entry.getTimestamp() / oneDayMillis;
            
            if (previousDay == -1) {
                // First entry
                previousDay = entryDay;
                consecutiveDays = 1;
            } else if (entryDay == previousDay - 1) {
                // Consecutive day
                previousDay = entryDay;
                consecutiveDays++;
                
                if (consecutiveDays >= days) {
                    return true;
                }
            } else if (entryDay == previousDay) {
                // Same day, continue
                continue;
            } else {
                // Streak broken
                return false;
            }
        }
        
        return consecutiveDays >= days;
    }
    
    /**
     * Unlock an achievement and show notification if newly unlocked
     */
    public boolean unlockAchievement(int id) {
        Achievement achievement = achievementMap.get(id);
        
        if (achievement != null && !achievement.isUnlocked()) {
            achievement.setUnlocked(true);
            showAchievementNotification(achievement);
            return true;
        }
        
        return false;
    }
    
    /**
     * Show notification for newly unlocked achievement
     */
    private void showAchievementNotification(Achievement achievement) {
        Intent intent = new Intent(context, AchievementsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Achievement Unlocked!")
                .setContentText(achievement.getTitle() + ": " + achievement.getDescription())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        
        NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        notificationManager.notify(achievement.getId(), builder.build());
    }
    
    /**
     * Show celebration dialog for time milestone achievement
     */
    public void showMilestoneCelebration(Context context, Achievement milestone) {
        // Only show celebration for time milestones
        if (milestone != null && 
            milestone.getCategory() == Achievement.AchievementCategory.TIME_MILESTONE) {
            
            // Use the MilestoneCelebration helper class
            MilestoneCelebration.showCelebrationDialog(context, milestone.getDaysRequired());
        }
    }
}