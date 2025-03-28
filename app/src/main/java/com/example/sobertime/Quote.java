package com.example.sobertime;

import java.io.Serializable;

/**
 * Represents an inspirational quote or story for recovery
 */
public class Quote implements Serializable {
    private int id;
    private String text;
    private String author;
    private boolean isFavorite;
    private String category;
    
    // Quote categories
    public static final String CATEGORY_MOTIVATION = "Motivation";
    public static final String CATEGORY_STRENGTH = "Strength";
    public static final String CATEGORY_HEALING = "Healing";
    public static final String CATEGORY_MINDFULNESS = "Mindfulness";
    public static final String CATEGORY_GRATITUDE = "Gratitude";
    
    public Quote(int id, String text, String author, String category) {
        this.id = id;
        this.text = text;
        this.author = author;
        this.category = category;
        this.isFavorite = false;
    }
    
    // Getters and setters
    public int getId() {
        return id;
    }
    
    public String getText() {
        return text;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public boolean isFavorite() {
        return isFavorite;
    }
    
    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }
    
    public String getCategory() {
        return category;
    }
}
