package com.example.sobertime;

import java.io.Serializable;
import java.util.Date;

/**
 * Represents an achievement that can be earned in the app
 */
public class Achievement implements Serializable {
    
    private int id;
    private String title;
    private String description;
    private String iconName;
    private boolean unlocked;
    private long unlockTime;
    private AchievementCategory category;
    
    // Achievement categories
    public enum AchievementCategory {
        TIME_MILESTONE("Time Milestone"),
        JOURNAL("Journal Activity"),
        COMMUNITY("Community Engagement"),
        FINANCIAL("Financial Milestone"),
        HEALTH("Health Improvement"),
        APP_USAGE("App Usage");
        
        private final String displayName;
        
        AchievementCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public Achievement(int id, String title, String description, String iconName, AchievementCategory category) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.iconName = iconName;
        this.category = category;
        this.unlocked = false;
        this.unlockTime = 0;
    }
    
    // Getters and setters
    public int getId() {
        return id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getIconName() {
        return iconName;
    }
    
    public boolean isUnlocked() {
        return unlocked;
    }
    
    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
        if (unlocked && unlockTime == 0) {
            unlockTime = new Date().getTime();
        }
    }
    
    public long getUnlockTime() {
        return unlockTime;
    }
    
    public void setUnlockTime(long unlockTime) {
        this.unlockTime = unlockTime;
    }
    
    public AchievementCategory getCategory() {
        return category;
    }
}
