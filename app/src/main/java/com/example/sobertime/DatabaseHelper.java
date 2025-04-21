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
    private static final int DATABASE_VERSION = 3; // Increased from 2 to 3 for support_resources
    
    // Table Names
    private static final String TABLE_JOURNAL = "journal";
    private static final String TABLE_SETTINGS = "settings";
    private static final String TABLE_ACCOUNTABILITY_BUDDY = "accountability_buddy";
    private static final String TABLE_SUPPORT_RESOURCES = "support_resources";
    
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

    // Accountability Buddy Table Columns
    private static final String BUDDY_ID = "_id";  // Using Android standard naming convention
    private static final String BUDDY_NAME = "name";
    private static final String BUDDY_PHONE = "phone";
    private static final String BUDDY_USER_NAME = "user_name";
    private static final String BUDDY_USER_PHONE = "user_phone";
    private static final String BUDDY_ENABLED = "enabled";
    private static final String BUDDY_NOTIFY_ON_CHECKIN = "notify_on_checkin";
    private static final String BUDDY_NOTIFY_ON_RELAPSE = "notify_on_relapse";
    private static final String BUDDY_NOTIFY_ON_MILESTONE = "notify_on_milestone";
    
    // Support Resources Table Columns
    private static final String RESOURCE_ID = "id";
    private static final String RESOURCE_NAME = "name";
    private static final String RESOURCE_DESCRIPTION = "description";
    private static final String RESOURCE_PHONE = "phone";
    private static final String RESOURCE_WEBSITE = "website";
    private static final String RESOURCE_CATEGORY = "category";
    private static final String RESOURCE_IS_CUSTOM = "is_custom";
    
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

    private static final String CREATE_TABLE_ACCOUNTABILITY_BUDDY = 
            "CREATE TABLE " + TABLE_ACCOUNTABILITY_BUDDY + "("
                    + BUDDY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + BUDDY_NAME + " TEXT, "
                    + BUDDY_PHONE + " TEXT, "
                    + BUDDY_USER_NAME + " TEXT, "
                    + BUDDY_USER_PHONE + " TEXT, "
                    + BUDDY_ENABLED + " INTEGER DEFAULT 1, "
                    + BUDDY_NOTIFY_ON_CHECKIN + " INTEGER DEFAULT 0, "
                    + BUDDY_NOTIFY_ON_RELAPSE + " INTEGER DEFAULT 1, "
                    + BUDDY_NOTIFY_ON_MILESTONE + " INTEGER DEFAULT 1);";

    private static final String CREATE_TABLE_SUPPORT_RESOURCES = 
            "CREATE TABLE " + TABLE_SUPPORT_RESOURCES + "("
                    + RESOURCE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + RESOURCE_NAME + " TEXT NOT NULL, "
                    + RESOURCE_DESCRIPTION + " TEXT, "
                    + RESOURCE_PHONE + " TEXT, "
                    + RESOURCE_WEBSITE + " TEXT, "
                    + RESOURCE_CATEGORY + " TEXT, "
                    + RESOURCE_IS_CUSTOM + " INTEGER DEFAULT 0);";
    
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
        db.execSQL(CREATE_TABLE_ACCOUNTABILITY_BUDDY);
        db.execSQL(CREATE_TABLE_SUPPORT_RESOURCES);
        
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
        if (oldVersion < 2) {
            // For existing tables, migrate accountability_buddy table to use consistent column names
            Cursor cursor = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='" + TABLE_ACCOUNTABILITY_BUDDY + "'", 
                    null);
            
            boolean tableExists = cursor.getCount() > 0;
            cursor.close();
            
            if (tableExists) {
                // The table exists but needs migration to standardize column names
                migrateAccountabilityBuddyTable(db);
            } else {
                // Create the accountability_buddy table for existing users
                db.execSQL(CREATE_TABLE_ACCOUNTABILITY_BUDDY);
            }
        }
        if (oldVersion < 3) {
            // Create the support_resources table for existing users
            db.execSQL(CREATE_TABLE_SUPPORT_RESOURCES);
        }
        // Add further upgrade paths for future versions
        // if (oldVersion < 4) { ... }
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
    
    public void ensureAccountabilityBuddyTableExists() {
        SQLiteDatabase db = getWritableDatabase();
        
        // Check if the table exists first
        Cursor cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='" + TABLE_ACCOUNTABILITY_BUDDY + "'", 
                null);
        
        boolean tableExists = cursor.getCount() > 0;
        cursor.close();
        
        if (!tableExists) {
            // Create the table with consistent column names
            db.execSQL("CREATE TABLE " + TABLE_ACCOUNTABILITY_BUDDY + " (" +
                    BUDDY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    BUDDY_NAME + " TEXT," +
                    BUDDY_PHONE + " TEXT," +
                    BUDDY_USER_NAME + " TEXT," +
                    BUDDY_USER_PHONE + " TEXT," +
                    BUDDY_ENABLED + " INTEGER DEFAULT 0," +
                    BUDDY_NOTIFY_ON_CHECKIN + " INTEGER DEFAULT 0," +
                    BUDDY_NOTIFY_ON_RELAPSE + " INTEGER DEFAULT 1," +
                    BUDDY_NOTIFY_ON_MILESTONE + " INTEGER DEFAULT 1" +
                    ")");
        } else {
            // Check if ID column exists, if not, we need to recreate the table
            Cursor columnsQuery = db.rawQuery("PRAGMA table_info(" + TABLE_ACCOUNTABILITY_BUDDY + ")", null);
            boolean hasProperIdColumn = false;
            
            if (columnsQuery.moveToFirst()) {
                do {
                    String columnName = columnsQuery.getString(columnsQuery.getColumnIndex("name"));
                    if (BUDDY_ID.equals(columnName)) {
                        hasProperIdColumn = true;
                        break;
                    }
                } while (columnsQuery.moveToNext());
            }
            columnsQuery.close();
            
            if (!hasProperIdColumn) {
                // Handle case where table exists but doesn't have proper ID column
                migrateAccountabilityBuddyTable(db);
            } else {
                // Only add non-primary key columns
                addColumnIfNotExists(db, TABLE_ACCOUNTABILITY_BUDDY, BUDDY_NAME, "TEXT");
                addColumnIfNotExists(db, TABLE_ACCOUNTABILITY_BUDDY, BUDDY_PHONE, "TEXT");
                addColumnIfNotExists(db, TABLE_ACCOUNTABILITY_BUDDY, BUDDY_USER_NAME, "TEXT");
                addColumnIfNotExists(db, TABLE_ACCOUNTABILITY_BUDDY, BUDDY_USER_PHONE, "TEXT");
                addColumnIfNotExists(db, TABLE_ACCOUNTABILITY_BUDDY, BUDDY_ENABLED, "INTEGER DEFAULT 0");
                addColumnIfNotExists(db, TABLE_ACCOUNTABILITY_BUDDY, BUDDY_NOTIFY_ON_CHECKIN, "INTEGER DEFAULT 0");
                addColumnIfNotExists(db, TABLE_ACCOUNTABILITY_BUDDY, BUDDY_NOTIFY_ON_RELAPSE, "INTEGER DEFAULT 1");
                addColumnIfNotExists(db, TABLE_ACCOUNTABILITY_BUDDY, BUDDY_NOTIFY_ON_MILESTONE, "INTEGER DEFAULT 1");
            }
        }
    }

    private void migrateAccountabilityBuddyTable(SQLiteDatabase db) {
        // Create a temp table with the correct schema
        db.execSQL("CREATE TABLE temp_" + TABLE_ACCOUNTABILITY_BUDDY + " (" +
                BUDDY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                BUDDY_NAME + " TEXT," +
                BUDDY_PHONE + " TEXT," +
                BUDDY_USER_NAME + " TEXT," +
                BUDDY_USER_PHONE + " TEXT," +
                BUDDY_ENABLED + " INTEGER DEFAULT 0," +
                BUDDY_NOTIFY_ON_CHECKIN + " INTEGER DEFAULT 0," +
                BUDDY_NOTIFY_ON_RELAPSE + " INTEGER DEFAULT 1," +
                BUDDY_NOTIFY_ON_MILESTONE + " INTEGER DEFAULT 1" +
                ")");
        
        // Try to copy data from old table to temp table
        try {
            // Check if 'id' column exists in old table
            Cursor columnsQuery = db.rawQuery("PRAGMA table_info(" + TABLE_ACCOUNTABILITY_BUDDY + ")", null);
            boolean hasOldIdColumn = false;
            
            if (columnsQuery.moveToFirst()) {
                do {
                    String columnName = columnsQuery.getString(columnsQuery.getColumnIndex("name"));
                    if ("id".equals(columnName)) {
                        hasOldIdColumn = true;
                        break;
                    }
                } while (columnsQuery.moveToNext());
            }
            columnsQuery.close();
            
            // Copy data preserving ID if possible
            if (hasOldIdColumn) {
                db.execSQL("INSERT INTO temp_" + TABLE_ACCOUNTABILITY_BUDDY + " (" +
                        BUDDY_ID + ", " +
                        BUDDY_NAME + ", " + 
                        BUDDY_PHONE + ", " + 
                        BUDDY_USER_NAME + ", " + 
                        BUDDY_USER_PHONE + ", " + 
                        BUDDY_ENABLED + ", " + 
                        BUDDY_NOTIFY_ON_CHECKIN + ", " + 
                        BUDDY_NOTIFY_ON_RELAPSE + ", " + 
                        BUDDY_NOTIFY_ON_MILESTONE + ") " +
                        "SELECT " +
                        "id, name, phone, user_name, user_phone, enabled, notify_on_checkin, " +
                        "notify_on_relapse, notify_on_milestone " +
                        "FROM " + TABLE_ACCOUNTABILITY_BUDDY);
            } else {
                // Just copy data without ID
                db.execSQL("INSERT INTO temp_" + TABLE_ACCOUNTABILITY_BUDDY + " (" +
                        BUDDY_NAME + ", " + 
                        BUDDY_PHONE + ", " + 
                        BUDDY_USER_NAME + ", " + 
                        BUDDY_USER_PHONE + ", " + 
                        BUDDY_ENABLED + ", " + 
                        BUDDY_NOTIFY_ON_CHECKIN + ", " + 
                        BUDDY_NOTIFY_ON_RELAPSE + ", " + 
                        BUDDY_NOTIFY_ON_MILESTONE + ") " +
                        "SELECT " +
                        "name, phone, user_name, user_phone, enabled, notify_on_checkin, " +
                        "notify_on_relapse, notify_on_milestone " +
                        "FROM " + TABLE_ACCOUNTABILITY_BUDDY);
            }
        } catch (Exception e) {
            // If copy fails, it's OK - we'll have an empty but correctly structured table
        }
        
        // Drop the old table
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ACCOUNTABILITY_BUDDY);
        
        // Rename temp table to the proper name
        db.execSQL("ALTER TABLE temp_" + TABLE_ACCOUNTABILITY_BUDDY + " RENAME TO " + TABLE_ACCOUNTABILITY_BUDDY);
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
    
    // Helper method to add a column if it doesn't exist
    private void addColumnIfNotExists(SQLiteDatabase db, String table, String column, String type) {
        Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null);
        boolean columnExists = false;
        
        if (cursor.moveToFirst()) {
            do {
                String columnName = cursor.getString(cursor.getColumnIndex("name"));
                if (column.equalsIgnoreCase(columnName)) {
                    columnExists = true;
                    break;
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        
        if (!columnExists) {
            // Skip adding PRIMARY KEY columns to existing tables - not supported in SQLite
            if (type.contains("PRIMARY KEY")) {
                return; // Skip this operation - cannot add PRIMARY KEY with ALTER TABLE
            }
            
            try {
                db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
            } catch (Exception e) {
                // Log the error but continue
            }
        }
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
