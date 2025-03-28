package com.example.sobertime;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class JournalEntry implements Serializable {
    
    private long id;
    private long timestamp;
    private String title;
    private String content;
    private String mood;
    private int cravingLevel; // 0-5 scale
    private String trigger;
    
    // Available mood options
    public static final String[] MOOD_OPTIONS = {
            "Calm", "Happy", "Excited", "Anxious", "Sad", "Frustrated", "Bored", "Proud"
    };
    
    // Available trigger categories
    public static final String[] TRIGGER_OPTIONS = {
            "Social Pressure", "Stress", "Celebration", "Boredom", "Loneliness", 
            "Habit/Routine", "Negative Feelings", "Other"
    };
    
    // Empty constructor
    public JournalEntry() {
        this.timestamp = System.currentTimeMillis();
        this.title = "";
        this.content = "";
        this.mood = "";
        this.cravingLevel = 0;
        this.trigger = "";
    }
    
    // Constructor with values
    public JournalEntry(String title, String content, String mood, int cravingLevel, String trigger) {
        this.timestamp = System.currentTimeMillis();
        this.title = title;
        this.content = content;
        this.mood = mood;
        this.cravingLevel = cravingLevel;
        this.trigger = trigger;
    }
    
    // Getters and Setters
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getMood() {
        return mood;
    }
    
    public void setMood(String mood) {
        this.mood = mood;
    }
    
    public int getCravingLevel() {
        return cravingLevel;
    }
    
    public void setCravingLevel(int cravingLevel) {
        this.cravingLevel = cravingLevel;
    }
    
    public String getTrigger() {
        return trigger;
    }
    
    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }
    
    // Helper methods
    
    public String getFormattedDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        return dateFormat.format(new Date(timestamp));
    }
    
    public String getFormattedTime() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return timeFormat.format(new Date(timestamp));
    }
    
    public String getFormattedDateTime() {
        return getFormattedDate() + " at " + getFormattedTime();
    }
    
    public String getSummary(int maxChars) {
        if (content.length() <= maxChars) {
            return content;
        }
        
        return content.substring(0, maxChars).trim() + "...";
    }
}
