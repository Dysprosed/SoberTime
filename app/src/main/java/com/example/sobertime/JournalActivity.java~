package com.example.sobertime;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import main.java.com.example.sobertime.BaseActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class JournalActivity extends BaseActivity implements JournalAdapter.OnEntryClickListener {

    private static final int REQUEST_ADD_ENTRY = 1001;
    private static final int REQUEST_EDIT_ENTRY = 1002;
    
    private RecyclerView recyclerView;
    private JournalAdapter adapter;
    private FloatingActionButton addEntryFab;
    private LinearLayout emptyStateContainer;
    private TextView emptyStateText;
    
    private DatabaseHelper databaseHelper;
    private List<JournalEntry> journalEntries;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal);
        
        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Journal");
        }
        
        // Initialize database helper
        databaseHelper = DatabaseHelper.getInstance(this);
        
        // Initialize views
        recyclerView = findViewById(R.id.journalRecyclerView);
        addEntryFab = findViewById(R.id.addEntryFab);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        emptyStateText = findViewById(R.id.emptyStateText);
        
        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Load journal entries
        loadJournalEntries();
        
        // Set up FAB click listener
        addEntryFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(JournalActivity.this, JournalEntryActivity.class);
                startActivityForResult(intent, REQUEST_ADD_ENTRY);
            }
        });
    }
    
    private void loadJournalEntries() {
        journalEntries = databaseHelper.getAllJournalEntries();
        
        // Show empty state if no entries
        if (journalEntries.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateContainer.setVisibility(View.VISIBLE);
            emptyStateText.setText("No journal entries yet.\nTap the + button to add your first entry.");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateContainer.setVisibility(View.GONE);
            
            // Set up adapter
            adapter = new JournalAdapter(this, journalEntries, this);
            recyclerView.setAdapter(adapter);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_ADD_ENTRY || requestCode == REQUEST_EDIT_ENTRY) {
                // Reload entries on successful add/edit
                loadJournalEntries();
                
                if (requestCode == REQUEST_ADD_ENTRY) {
                    Toast.makeText(this, "Journal entry added", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Journal entry updated", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    @Override
    public void onEntryClick(JournalEntry entry) {
        // Open entry for editing
        Intent intent = new Intent(this, JournalEntryActivity.class);
        intent.putExtra("entry_id", entry.getId());
        startActivityForResult(intent, REQUEST_EDIT_ENTRY);
    }
    
    @Override
    public void onEntryLongClick(final JournalEntry entry) {
        // Show delete confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Delete Entry")
                .setMessage("Are you sure you want to delete this journal entry?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        databaseHelper.deleteJournalEntry(entry.getId());
                        loadJournalEntries();
                        Toast.makeText(JournalActivity.this, "Entry deleted", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_journal, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_filter_journal) {
            // Show filter options dialog (to be implemented)
            Toast.makeText(this, "Filter feature coming soon", Toast.LENGTH_SHORT).show();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
}
