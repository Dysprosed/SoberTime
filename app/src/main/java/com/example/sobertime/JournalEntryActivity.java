package com.example.sobertime;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;

public class JournalEntryActivity extends AppCompatActivity {

    private EditText titleEditText;
    private EditText contentEditText;
    private TextView moodTextView;
    private TextView triggerTextView;
    private SeekBar cravingSeekBar;
    private TextView cravingLevelText;
    private LinearLayout moodSelectorLayout;
    private LinearLayout triggerSelectorLayout;
    private Button saveButton;
    
    private DatabaseHelper databaseHelper;
    private JournalEntry currentEntry;
    private boolean isEditMode = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal_entry);
        
        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Initialize database helper
        databaseHelper = DatabaseHelper.getInstance(this);
        
        // Initialize views
        titleEditText = findViewById(R.id.journalTitleEditText);
        contentEditText = findViewById(R.id.journalContentEditText);
        moodTextView = findViewById(R.id.moodSelectText);
        triggerTextView = findViewById(R.id.triggerSelectText);
        cravingSeekBar = findViewById(R.id.cravingSeekBar);
        cravingLevelText = findViewById(R.id.cravingLevelText);
        moodSelectorLayout = findViewById(R.id.moodSelectorLayout);
        triggerSelectorLayout = findViewById(R.id.triggerSelectorLayout);
        saveButton = findViewById(R.id.saveJournalButton);
        
        // Check if we're editing an existing entry
        long entryId = getIntent().getLongExtra("entry_id", -1);
        if (entryId != -1) {
            isEditMode = true;
            currentEntry = databaseHelper.getJournalEntry(entryId);
            
            if (currentEntry != null) {
                // Set the title based on edit mode
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("Edit Journal Entry");
                }
                
                // Populate fields with existing data
                populateFields();
            } else {
                // Handle error case
                Toast.makeText(this, "Error loading journal entry", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            // We're adding a new entry
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("New Journal Entry");
            }
            
            currentEntry = new JournalEntry();
        }
        
        // Set up mood selector
        moodSelectorLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMoodSelector();
            }
        });
        
        // Set up trigger selector
        triggerSelectorLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTriggerSelector();
            }
        });
        
        // Set up craving level seekbar
        cravingSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateCravingLevelText(progress);
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not needed
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Not needed
            }
        });
        
        // Set up save button
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveJournalEntry();
            }
        });
    }
    
    private void populateFields() {
        titleEditText.setText(currentEntry.getTitle());
        contentEditText.setText(currentEntry.getContent());
        
        if (!TextUtils.isEmpty(currentEntry.getMood())) {
            moodTextView.setText(currentEntry.getMood());
        }
        
        if (!TextUtils.isEmpty(currentEntry.getTrigger())) {
            triggerTextView.setText(currentEntry.getTrigger());
        }
        
        cravingSeekBar.setProgress(currentEntry.getCravingLevel());
        updateCravingLevelText(currentEntry.getCravingLevel());
    }
    
    private void showMoodSelector() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("How are you feeling?");
        
        final String[] moods = JournalEntry.MOOD_OPTIONS;
        
        // Find the current selection index
        int selectedIndex = -1;
        String currentMood = moodTextView.getText().toString();
        for (int i = 0; i < moods.length; i++) {
            if (moods[i].equals(currentMood)) {
                selectedIndex = i;
                break;
            }
        }
        
        builder.setSingleChoiceItems(moods, selectedIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                moodTextView.setText(moods[which]);
                dialog.dismiss();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void showTriggerSelector() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("What triggered your craving?");
        
        final String[] triggers = JournalEntry.TRIGGER_OPTIONS;
        
        // Find the current selection index
        int selectedIndex = -1;
        String currentTrigger = triggerTextView.getText().toString();
        for (int i = 0; i < triggers.length; i++) {
            if (triggers[i].equals(currentTrigger)) {
                selectedIndex = i;
                break;
            }
        }
        
        builder.setSingleChoiceItems(triggers, selectedIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                triggerTextView.setText(triggers[which]);
                dialog.dismiss();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void updateCravingLevelText(int level) {
        String levelText;
        
        switch (level) {
            case 0:
                levelText = "None";
                break;
            case 1:
                levelText = "Very Low";
                break;
            case 2:
                levelText = "Low";
                break;
            case 3:
                levelText = "Moderate";
                break;
            case 4:
                levelText = "High";
                break;
            case 5:
                levelText = "Very High";
                break;
            default:
                levelText = "None";
                break;
        }
        
        cravingLevelText.setText(levelText);
    }
    
    private void saveJournalEntry() {
        String title = titleEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();
        String mood = moodTextView.getText().toString();
        String trigger = triggerTextView.getText().toString();
        int cravingLevel = cravingSeekBar.getProgress();
        
        // Validate required fields
        if (TextUtils.isEmpty(title)) {
            titleEditText.setError("Title is required");
            titleEditText.requestFocus();
            return;
        }
        
        if (TextUtils.isEmpty(content)) {
            contentEditText.setError("Content is required");
            contentEditText.requestFocus();
            return;
        }
        
        // Set values to the current entry
        currentEntry.setTitle(title);
        currentEntry.setContent(content);
        currentEntry.setMood(mood);
        currentEntry.setTrigger(trigger);
        currentEntry.setCravingLevel(cravingLevel);
        
        // Save to database
        if (isEditMode) {
            databaseHelper.updateJournalEntry(currentEntry);
        } else {
            // For new entries, set the current timestamp
            currentEntry.setTimestamp(System.currentTimeMillis());
            databaseHelper.addJournalEntry(currentEntry);
        }
        
        // Return success
        setResult(RESULT_OK);
        finish();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Only show the delete option for existing entries
        if (isEditMode) {
            getMenuInflater().inflate(R.menu.menu_journal_entry, menu);
        }
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            // Prompt to save if there are changes
            if (hasUnsavedChanges()) {
                showUnsavedChangesDialog();
            } else {
                finish();
            }
            return true;
        } else if (id == R.id.action_delete_entry) {
            showDeleteConfirmationDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onBackPressed() {
        if (hasUnsavedChanges()) {
            showUnsavedChangesDialog();
        } else {
            super.onBackPressed();
        }
    }
    
    private boolean hasUnsavedChanges() {
        if (isEditMode) {
            // Check if any field is different from the original
            return !titleEditText.getText().toString().equals(currentEntry.getTitle()) ||
                    !contentEditText.getText().toString().equals(currentEntry.getContent()) ||
                    !moodTextView.getText().toString().equals(currentEntry.getMood()) ||
                    !triggerTextView.getText().toString().equals(currentEntry.getTrigger()) ||
                    cravingSeekBar.getProgress() != currentEntry.getCravingLevel();
        } else {
            // For new entries, check if any required field has content
            return !TextUtils.isEmpty(titleEditText.getText()) || 
                    !TextUtils.isEmpty(contentEditText.getText());
        }
    }
    
    private void showUnsavedChangesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Unsaved Changes");
        builder.setMessage("You have unsaved changes. Would you like to save them?");
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveJournalEntry();
            }
        });
        builder.setNegativeButton("Discard", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.setNeutralButton("Cancel", null);
        builder.show();
    }
    
    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Entry");
        builder.setMessage("Are you sure you want to delete this journal entry?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                databaseHelper.deleteJournalEntry(currentEntry.getId());
                setResult(RESULT_OK);
                finish();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
