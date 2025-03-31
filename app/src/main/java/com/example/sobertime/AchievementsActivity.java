package com.example.sobertime;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.util.List;

public class AchievementsActivity extends BaseActivity {

    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private TextView emptyStateText;

    private AchievementManager achievementManager;
    private AchievementsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Achievements");
        }

        // Initialize achievement manager
        achievementManager = AchievementManager.getInstance(this);

        // Initialize views
        initializeViews();

        // Set up tabs
        setupTabs();

        // Initial display (default to all achievements)
        displayAchievements(achievementManager.getAllAchievements());
    }

    private void initializeViews() {
        tabLayout = findViewById(R.id.tabLayout);
        recyclerView = findViewById(R.id.achievementsRecyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupTabs() {
        // Add tabs
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("Unlocked"));
        tabLayout.addTab(tabLayout.newTab().setText("Locked"));

        // Tab selection listener
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: // All
                        displayAchievements(achievementManager.getAllAchievements());
                        break;
                    case 1: // Unlocked
                        displayAchievements(achievementManager.getUnlockedAchievements());
                        break;
                    case 2: // Locked
                        displayAchievements(achievementManager.getLockedAchievements());
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Not needed
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Not needed
            }
        });
    }

    private void displayAchievements(List<Achievement> achievements) {
        if (achievements.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);

            // Set appropriate empty state message based on selected tab
            int selectedTab = tabLayout.getSelectedTabPosition();
            switch (selectedTab) {
                case 0: // All
                    emptyStateText.setText("No achievements available yet.");
                    break;
                case 1: // Unlocked
                    emptyStateText.setText("You haven't unlocked any achievements yet.\nKeep going on your sobriety journey!");
                    break;
                case 2: // Locked
                    emptyStateText.setText("You've unlocked all achievements!\nCongratulations on your amazing progress!");
                    break;
            }
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);

            // Set up or update adapter
            if (adapter == null) {
                adapter = new AchievementsAdapter(this, achievements);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.updateAchievements(achievements);
                adapter.notifyDataSetChanged();
            }
        }
    }

}