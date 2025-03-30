package com.example.sobertime;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import main.java.com.example.sobertime.BaseActivity;

import com.google.android.material.tabs.TabLayout;

import java.util.List;

public class InspirationActivity extends BaseActivity implements QuoteAdapter.OnQuoteInteractionListener {
    
    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private TextView emptyStateText;
    
    private QuoteManager quoteManager;
    private QuoteAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inspiration);
        
        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Inspirational Quotes");
        }
        
        // Initialize quote manager
        quoteManager = QuoteManager.getInstance(this);
        
        // Initialize views
        initializeViews();
        
        // Set up tabs
        setupTabs();
        
        // Initial display (default to all quotes)
        displayQuotes(quoteManager.getAllQuotes());
    }
    
    private void initializeViews() {
        tabLayout = findViewById(R.id.tabLayout);
        recyclerView = findViewById(R.id.quotesRecyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);
        
        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
    
    private void setupTabs() {
        // Add tabs
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("Favorites"));
        tabLayout.addTab(tabLayout.newTab().setText("Motivation"));
        tabLayout.addTab(tabLayout.newTab().setText("Strength"));
        tabLayout.addTab(tabLayout.newTab().setText("Healing"));
        
        // Tab selection listener
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: // All
                        displayQuotes(quoteManager.getAllQuotes());
                        break;
                    case 1: // Favorites
                        displayQuotes(quoteManager.getFavoriteQuotes());
                        break;
                    case 2: // Motivation
                        displayQuotes(quoteManager.getQuotesByCategory(Quote.CATEGORY_MOTIVATION));
                        break;
                    case 3: // Strength
                        displayQuotes(quoteManager.getQuotesByCategory(Quote.CATEGORY_STRENGTH));
                        break;
                    case 4: // Healing
                        displayQuotes(quoteManager.getQuotesByCategory(Quote.CATEGORY_HEALING));
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
    
    private void displayQuotes(List<Quote> quotes) {
        if (quotes.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
            
            // Set appropriate empty state message based on selected tab
            int selectedTab = tabLayout.getSelectedTabPosition();
            switch (selectedTab) {
                case 0: // All
                    emptyStateText.setText("No quotes available.");
                    break;
                case 1: // Favorites
                    emptyStateText.setText("You haven't favorited any quotes yet.\nTap the heart icon on quotes you like to save them here!");
                    break;
                default:
                    emptyStateText.setText("No quotes available for this category.");
                    break;
            }
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
            
            // Set up or update adapter
            if (adapter == null) {
                adapter = new QuoteAdapter(this, quotes, this);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.updateQuotes(quotes);
                adapter.notifyDataSetChanged();
            }
        }
    }
    
    @Override
    public void onFavoriteToggled(int quoteId) {
        quoteManager.toggleFavorite(quoteId);
        
        // If we're in the favorites tab, update the display
        if (tabLayout.getSelectedTabPosition() == 1) {
            displayQuotes(quoteManager.getFavoriteQuotes());
        } else {
            // Just notify adapter of changes
            adapter.notifyDataSetChanged();
        }
    }
    
    @Override
    public void onQuoteClicked(Quote quote) {
        // In a future implementation, this could show a full-screen quote
        // or allow sharing the quote
    }
    
}
