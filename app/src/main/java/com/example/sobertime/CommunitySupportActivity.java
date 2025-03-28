package com.example.sobertime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CommunitySupportActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "CommunitySupportPrefs";
    private static final String CUSTOM_RESOURCES_KEY = "custom_resources";
    
    private RecyclerView supportResourcesRecyclerView;
    private CardView aaCardView;
    private CardView naCardView;
    private CardView smartCardView;
    private CardView redditCardView;
    private CardView addResourceCardView;
    
    private SupportResourceAdapter adapter;
    private List<SupportResource> resources;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_support);
        
        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Community Support");
        }
        
        // Initialize views
        initializeViews();
        
        // Load resources
        loadResources();
        
        // Set up click listeners
        setupClickListeners();
    }
    
    private void initializeViews() {
        supportResourcesRecyclerView = findViewById(R.id.supportResourcesRecyclerView);
        aaCardView = findViewById(R.id.aaCardView);
        naCardView = findViewById(R.id.naCardView);
        smartCardView = findViewById(R.id.smartCardView);
        redditCardView = findViewById(R.id.redditCardView);
        addResourceCardView = findViewById(R.id.addResourceCardView);
        
        // Set up RecyclerView
        supportResourcesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
    
    private void loadResources() {
        resources = new ArrayList<>();
        
        // Add default resources
        resources.add(new SupportResource(
                "Alcoholics Anonymous",
                "Find local AA meetings and resources",
                "https://www.aa.org/find-aa",
                false
        ));
        
        resources.add(new SupportResource(
                "Narcotics Anonymous",
                "Find local NA meetings and resources",
                "https://www.na.org/meetingsearch/",
                false
        ));
        
        resources.add(new SupportResource(
                "SMART Recovery",
                "Self-Management and Recovery Training program",
                "https://www.smartrecovery.org/",
                false
        ));
        
        resources.add(new SupportResource(
                "r/stopdrinking",
                "Reddit community for those stopping drinking",
                "https://www.reddit.com/r/stopdrinking/",
                false
        ));
        
        // Load custom resources
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String customResourcesJson = prefs.getString(CUSTOM_RESOURCES_KEY, "");
        
        if (!customResourcesJson.isEmpty()) {
            try {
                // Parse JSON and add custom resources
                List<SupportResource> customResources = SupportResource.fromJson(customResourcesJson);
                resources.addAll(customResources);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Set up adapter
        adapter = new SupportResourceAdapter(this, resources);
        supportResourcesRecyclerView.setAdapter(adapter);
    }
    
    private void setupClickListeners() {
        // AA card
        aaCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl("https://www.aa.org/find-aa");
            }
        });
        
        // NA card
        naCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl("https://www.na.org/meetingsearch/");
            }
        });
        
        // SMART card
        smartCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl("https://www.smartrecovery.org/");
            }
        });
        
        // Reddit card
        redditCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl("https://www.reddit.com/r/stopdrinking/");
            }
        });
        
        // Add resource card
        addResourceCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddResourceDialog();
            }
        });
    }
    
    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }
    
    private void showAddResourceDialog() {
        AddResourceDialogFragment dialogFragment = new AddResourceDialogFragment();
        dialogFragment.setListener(new AddResourceDialogFragment.AddResourceListener() {
            @Override
            public void onResourceAdded(SupportResource resource) {
                // Add to list and save
                resources.add(resource);
                saveCustomResources();
                adapter.notifyDataSetChanged();
            }
        });
        dialogFragment.show(getSupportFragmentManager(), "add_resource");
    }
    
    private void saveCustomResources() {
        try {
            // Filter to get only custom resources
            List<SupportResource> customResources = new ArrayList<>();
            for (SupportResource resource : resources) {
                if (resource.isCustom()) {
                    customResources.add(resource);
                }
            }
            
            // Convert to JSON
            String json = SupportResource.toJson(customResources);
            
            // Save to preferences
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(CUSTOM_RESOURCES_KEY, json);
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
