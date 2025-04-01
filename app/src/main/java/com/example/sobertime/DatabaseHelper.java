package com.example.sobertime;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    
    // Database Information
    private static final String DATABASE_NAME = "sobriety_tracker.db";
    private static final int DATABASE_VERSION = 1;
    
    // Table Names
    private static final String TABLE_JOURNAL = "journal";
    private static final String TABLE_SETTINGS = "settings";
    
    // Journal Table Columns
    private static final String JOURNAL_ID = "id";
    private static final String JOURNAL_TIMESTAMP = "timestamp";
    private static final String JOURNAL_TITLE = "title";
    private static final String JOURNAL_CONTENT = "content";
    private static final String JOURNAL_MOOD = "mood";
    private static final String JOURNAL_CRAVING_LEVEL = "craving_level";
    private static final String JOURNAL_TRIGGER = "trigger";
    
    // Settings Table Columns
    private static final String SETTINGS_KEY = "key";
    private static final String SETTINGS_VALUE = "value";
    
    // Create Table Statements
    private static final String CREATE_TABLE_JOURNAL = 
            "CREATE TABLE " + TABLE_JOURNAL + "("
                    + JOURNAL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + JOURNAL_TIMESTAMP + " INTEGER NOT NULL, "
                    + JOURNAL_TITLE + " TEXT NOT NULL, "
                    + JOURNAL_CONTENT + " TEXT NOT NULL, "
                    + JOURNAL_MOOD + " TEXT, "
                    + JOURNAL_CRAVING_LEVEL + " INTEGER, "
                    + JOURNAL_TRIGGER + " TEXT);";
    
    private static final String CREATE_TABLE_SETTINGS = 
            "CREATE TABLE " + TABLE_SETTINGS + "("
                    + SETTINGS_KEY + " TEXT PRIMARY KEY, "
                    + SETTINGS_VALUE + " TEXT NOT NULL);";
    
    private static DatabaseHelper instance;
    
    // Get singleton instance
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }
    
    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_JOURNAL);
        db.execSQL(CREATE_TABLE_SETTINGS);
        
        // Initialize default settings
        ContentValues defaultSettings = new ContentValues();
        defaultSettings.put(SETTINGS_KEY, "drink_cost");
        defaultSettings.put(SETTINGS_VALUE, "8.50");
        db.insert(TABLE_SETTINGS, null, defaultSettings);
        
        defaultSettings.clear();
        defaultSettings.put(SETTINGS_KEY, "drinks_per_week");
        defaultSettings.put(SETTINGS_VALUE, "15");
        db.insert(TABLE_SETTINGS, null, defaultSettings);
        
        defaultSettings.clear();
        defaultSettings.put(SETTINGS_KEY, "calories_per_drink");
        defaultSettings.put(SETTINGS_VALUE, "150");
        db.insert(TABLE_SETTINGS, null, defaultSettings);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This will be implemented when we have database schema changes in future versions
        if (oldVersion < 2) {
            // Future upgrade logic will go here
        }
    }
    
    // Journal CRUD Operations
    
    public long addJournalEntry(JournalEntry entry) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(JOURNAL_TIMESTAMP, entry.getTimestamp());
        values.put(JOURNAL_TITLE, entry.getTitle());
        values.put(JOURNAL_CONTENT, entry.getContent());
        values.put(JOURNAL_MOOD, entry.getMood());
        values.put(JOURNAL_CRAVING_LEVEL, entry.getCravingLevel());
        values.put(JOURNAL_TRIGGER, entry.getTrigger());
        
        // Insert row
        long id = db.insert(TABLE_JOURNAL, null, values);
        
        return id;
    }
    
    public JournalEntry getJournalEntry(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        
        String selectQuery = "SELECT * FROM " + TABLE_JOURNAL + " WHERE " + JOURNAL_ID + " = " + id;
        Cursor cursor = db.rawQuery(selectQuery, null);
        
        JournalEntry entry = null;
        if (cursor.moveToFirst()) {
            entry = new JournalEntry();
            entry.setId(cursor.getLong(cursor.getColumnIndex(JOURNAL_ID)));
            entry.setTimestamp(cursor.getLong(cursor.getColumnIndex(JOURNAL_TIMESTAMP)));
            entry.setTitle(cursor.getString(cursor.getColumnIndex(JOURNAL_TITLE)));
            entry.setContent(cursor.getString(cursor.getColumnIndex(JOURNAL_CONTENT)));
            entry.setMood(cursor.getString(cursor.getColumnIndex(JOURNAL_MOOD)));
            entry.setCravingLevel(cursor.getInt(cursor.getColumnIndex(JOURNAL_CRAVING_LEVEL)));
            entry.setTrigger(cursor.getString(cursor.getColumnIndex(JOURNAL_TRIGGER)));
        }
        
        cursor.close();
        return entry;
    }
    
    public List<JournalEntry> getAllJournalEntries() {
        List<JournalEntry> entries = new ArrayList<>();
        
        String selectQuery = "SELECT * FROM " + TABLE_JOURNAL + " ORDER BY " + JOURNAL_TIMESTAMP + " DESC";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        
        if (cursor.moveToFirst()) {
            do {
                JournalEntry entry = new JournalEntry();
                entry.setId(cursor.getLong(cursor.getColumnIndex(JOURNAL_ID)));
                entry.setTimestamp(cursor.getLong(cursor.getColumnIndex(JOURNAL_TIMESTAMP)));
                entry.setTitle(cursor.getString(cursor.getColumnIndex(JOURNAL_TITLE)));
                entry.setContent(cursor.getString(cursor.getColumnIndex(JOURNAL_CONTENT)));
                entry.setMood(cursor.getString(cursor.getColumnIndex(JOURNAL_MOOD)));
                entry.setCravingLevel(cursor.getInt(cursor.getColumnIndex(JOURNAL_CRAVING_LEVEL)));
                entry.setTrigger(cursor.getString(cursor.getColumnIndex(JOURNAL_TRIGGER)));
                
                entries.add(entry);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return entries;
    }
    
    public int updateJournalEntry(JournalEntry entry) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(JOURNAL_TIMESTAMP, entry.getTimestamp());
        values.put(JOURNAL_TITLE, entry.getTitle());
        values.put(JOURNAL_CONTENT, entry.getContent());
        values.put(JOURNAL_MOOD, entry.getMood());
        values.put(JOURNAL_CRAVING_LEVEL, entry.getCravingLevel());
        values.put(JOURNAL_TRIGGER, entry.getTrigger());
        
        // Update row
        return db.update(TABLE_JOURNAL, values, JOURNAL_ID + " = ?",
                new String[]{String.valueOf(entry.getId())});
    }
    
    public void deleteJournalEntry(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_JOURNAL, JOURNAL_ID + " = ?", new String[]{String.valueOf(id)});
    }
    
    // Settings Operations
    
    public void setSetting(String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(SETTINGS_VALUE, value);
        
        // Check if setting exists
        Cursor cursor = db.query(TABLE_SETTINGS, new String[]{SETTINGS_KEY}, 
                SETTINGS_KEY + "=?", new String[]{key}, null, null, null);
        
        if (cursor.getCount() > 0) {
            // Update existing setting
            db.update(TABLE_SETTINGS, values, SETTINGS_KEY + " = ?", new String[]{key});
        } else {
            // Insert new setting
            values.put(SETTINGS_KEY, key);
            db.insert(TABLE_SETTINGS, null, values);
        }
        
        cursor.close();
    }
    
    public String getSetting(String key, String defaultValue) {
        SQLiteDatabase db = this.getReadableDatabase();
        
        String value = defaultValue;
        
        Cursor cursor = db.query(TABLE_SETTINGS, new String[]{SETTINGS_VALUE},
                SETTINGS_KEY + "=?", new String[]{key}, null, null, null);
        
        if (cursor.moveToFirst()) {
            value = cursor.getString(cursor.getColumnIndex(SETTINGS_VALUE));
        }
        
        cursor.close();
        return value;
    }
    
    public float getFloatSetting(String key, float defaultValue) {
        String value = getSetting(key, String.valueOf(defaultValue));
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public int getIntSetting(String key, int defaultValue) {
        String value = getSetting(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // Reset Database
    public void resetAllData() {
        SQLiteDatabase db = getWritableDatabase();
        // Clear all tables
        db.delete("journal", null, null);
        db.delete("settings", null, null);
        // Add other tables as needed
    }
}
