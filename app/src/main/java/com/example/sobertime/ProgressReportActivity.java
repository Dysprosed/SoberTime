package com.example.sobertime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ProgressReportActivity extends BaseActivity {

    private static final String PREFS_NAME = "SobrietyTrackerPrefs";
    private static final String START_DATE_KEY = "sobriety_start_date";
    
    // UI Elements
    private LinearLayout reportContainer;
    private TextView dateRangeTextView;
    private TextView totalDaysTextView;
    private TextView moneySavedTextView;
    private TextView caloriesSavedTextView;
    private TextView achievementsCountTextView;
    private TextView journalCountTextView;
    private Button emailReportButton;
    private Button shareReportButton;
    
    // Chart views - these will be initialized in the layout
    private TextView timeInvestmentChart;
    private TextView financialComparisonChart;
    private TextView healthProgressChart;
    private TextView achievementsChart;
    
    // Tab navigation
    private CardView financialTabCard;
    private CardView healthTabCard;
    private CardView timeTabCard;
    private CardView achievementsTabCard;
    
    // Content containers
    private LinearLayout financialContent;
    private LinearLayout healthContent;
    private LinearLayout timeContent;
    private LinearLayout achievementsContent;
    
    // Data
    private long sobrietyStartDate;
    private int totalDaysSober;
    private float moneySaved;
    private int caloriesSaved;
    private List<JournalEntry> journalEntries;
    private List<Achievement> achievements;
    private DatabaseHelper databaseHelper;
    private AchievementManager achievementManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress_report);
        
        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Progress Report");
        }
        
        // Initialize database helpers
        databaseHelper = DatabaseHelper.getInstance(this);
        achievementManager = AchievementManager.getInstance(this);
        
        // Get sobriety start date
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        sobrietyStartDate = prefs.getLong(START_DATE_KEY, System.currentTimeMillis());
        totalDaysSober = getDaysSober();
        
        // Initialize views
        initializeViews();
        
        // Load user data
        loadUserData();
        
        // Generate report data
        generateReportSummary();
        
        // Set up tab navigation
        setupTabNavigation();
        
        // Set up button listeners
        setupButtonListeners();
        
        // Show financial tab by default
        showFinancialTab();
    }
    
    private void initializeViews() {
        try {
            // Core summary views
            reportContainer = findViewById(R.id.reportContainer);
            dateRangeTextView = findViewById(R.id.dateRangeTextView);
            totalDaysTextView = findViewById(R.id.totalDaysTextView);
            moneySavedTextView = findViewById(R.id.moneySavedTextView);
            caloriesSavedTextView = findViewById(R.id.caloriesSavedTextView);
            achievementsCountTextView = findViewById(R.id.achievementsCountTextView);
            journalCountTextView = findViewById(R.id.journalCountTextView);
            
            // Charts (simplified to TextView placeholders)
            timeInvestmentChart = findViewById(R.id.timeInvestmentChart);
            financialComparisonChart = findViewById(R.id.financialComparisonChart);
            healthProgressChart = findViewById(R.id.healthProgressChart);
            achievementsChart = findViewById(R.id.achievementsChart);
            
            // Tab navigation
            financialTabCard = findViewById(R.id.financialTabCard);
            healthTabCard = findViewById(R.id.healthTabCard);
            timeTabCard = findViewById(R.id.timeTabCard);
            achievementsTabCard = findViewById(R.id.achievementsTabCard);
            
            // Content containers
            financialContent = findViewById(R.id.financialContent);
            healthContent = findViewById(R.id.healthContent);
            timeContent = findViewById(R.id.timeContent);
            achievementsContent = findViewById(R.id.achievementsContent);
            
            // Buttons
            emailReportButton = findViewById(R.id.emailReportButton);
            shareReportButton = findViewById(R.id.shareReportButton);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadUserData() {
        try {
            // Load data needed for the report
            
            // Get money saved calculation
            float drinkCost = databaseHelper.getFloatSetting("drink_cost", 8.50f);
            int drinksPerWeek = databaseHelper.getIntSetting("drinks_per_week", 15);
            float drinksAvoided = (totalDaysSober / 7.0f) * drinksPerWeek;
            moneySaved = drinksAvoided * drinkCost;
            
            // Calculate calories saved
            int caloriesPerDrink = databaseHelper.getIntSetting("calories_per_drink", 150);
            caloriesSaved = (int) (drinksAvoided * caloriesPerDrink);
            
            // Get achievements
            achievements = achievementManager.getUnlockedAchievements();
            
            // Get journal entries
            journalEntries = databaseHelper.getAllJournalEntries();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void generateReportSummary() {
        try {
            // Set date range text
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            String startDateStr = dateFormat.format(new Date(sobrietyStartDate));
            String currentDateStr = dateFormat.format(new Date());
            dateRangeTextView.setText(startDateStr + " - " + currentDateStr);
            
            // Set summary data
            totalDaysTextView.setText(String.valueOf(totalDaysSober));
            
            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance();
            moneySavedTextView.setText(currencyFormatter.format(moneySaved));
            
            caloriesSavedTextView.setText(NumberFormat.getNumberInstance(Locale.US).format(caloriesSaved));
            
            achievementsCountTextView.setText(String.valueOf(achievements.size()));
            
            journalCountTextView.setText(String.valueOf(journalEntries.size()));
            
            // Set detailed data
            setupDetailedContent();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void setupDetailedContent() {
        try {
            // Financial content
            TextView financialExamplesTextView = findViewById(R.id.financialExamplesTextView);
            if (financialExamplesTextView != null) {
                StringBuilder examples = new StringBuilder();
                examples.append("With $").append(Math.round(moneySaved)).append(" saved, you could buy:\n\n");
                examples.append("• ").append(Math.floor(moneySaved / 15)).append(" movie tickets\n");
                examples.append("• ").append(Math.floor(moneySaved / 25)).append(" meals at restaurants\n");
                examples.append("• ").append(Math.floor(moneySaved / 550)).append(" month(s) of groceries\n");
                examples.append("• ").append(Math.floor(moneySaved / 1000)).append(" new smartphone(s)");
                
                financialExamplesTextView.setText(examples.toString());
            }
            
            // Health content
            TextView healthDetailsTextView = findViewById(R.id.healthDetailsTextView);
            if (healthDetailsTextView != null) {
                float potentialWeightLoss = (caloriesSaved / 3500f); // 3500 calories = 1 pound
                
                StringBuilder healthDetails = new StringBuilder();
                healthDetails.append("Physical Impact:\n\n");
                healthDetails.append("• Potential weight control: ").append(String.format("%.1f", potentialWeightLoss)).append(" lbs\n");
                healthDetails.append("• Better sleep: ").append(Math.floor(totalDaysSober / 7)).append(" nights\n");
                healthDetails.append("• Healthier liver: ").append(totalDaysSober).append(" days\n");
                healthDetails.append("• Brain fog reduction: improved cognitive function\n\n");
                healthDetails.append("The calories you've avoided would take ").append(Math.floor(caloriesSaved / 600))
                        .append(" hours of running to burn!");
                
                healthDetailsTextView.setText(healthDetails.toString());
            }
            
            // Time content
            TextView timeDetailsTextView = findViewById(R.id.timeDetailsTextView);
            if (timeDetailsTextView != null) {
                // Calculate hours saved (assuming average of 1.5 hours per day typically spent drinking/recovering)
                float hoursSaved = totalDaysSober * 1.5f;
                
                StringBuilder timeDetails = new StringBuilder();
                timeDetails.append("Time Reclaimed: ").append(Math.round(hoursSaved)).append(" hours\n\n");
                timeDetails.append("This is equivalent to:\n");
                timeDetails.append("• ").append(Math.floor(hoursSaved / 24)).append(" full days\n");
                timeDetails.append("• ").append(Math.floor(hoursSaved / 40)).append(" work weeks\n");
                timeDetails.append("• ").append(Math.floor(hoursSaved / 2)).append(" movies\n\n");
                
                // Add journal consistency info
                timeDetails.append("Your journaling consistency:\n");
                Map<Integer, Integer> entriesByWeek = analyzeJournalEntriesByWeek();
                
                // Show the last 5 weeks or less
                int weeksToShow = Math.min(5, entriesByWeek.size());
                int startWeek = Math.max(1, entriesByWeek.size() - weeksToShow + 1);
                
                for (int i = 0; i < weeksToShow; i++) {
                    int weekNumber = startWeek + i;
                    int entries = entriesByWeek.getOrDefault(weekNumber, 0);
                    timeDetails.append("• Week ").append(weekNumber).append(": ")
                            .append(entries).append(" journal entries\n");
                }
                
                timeDetailsTextView.setText(timeDetails.toString());
            }
            
            // Achievements content
            TextView nextMilestoneTextView = findViewById(R.id.nextMilestoneTextView);
            if (nextMilestoneTextView != null) {
                // Find next milestone (will need to adjust for our app's milestone system)
                StringBuilder milestoneText = new StringBuilder();
                milestoneText.append("Achievements unlocked: ").append(achievements.size()).append("\n\n");
                
                // Next milestone calculation would go here - simplified for now
                milestoneText.append("Next milestone: Keep going! You're making great progress.");
                
                nextMilestoneTextView.setText(milestoneText.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void setupTabNavigation() {
        // Financial tab
        financialTabCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFinancialTab();
            }
        });
        
        // Health tab
        healthTabCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHealthTab();
            }
        });
        
        // Time tab
        timeTabCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimeTab();
            }
        });
        
        // Achievements tab
        achievementsTabCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAchievementsTab();
            }
        });
    }
    
    private void showFinancialTab() {
        // Update tab selection UI
        financialTabCard.setCardBackgroundColor(getResources().getColor(R.color.colorPrimary));
        healthTabCard.setCardBackgroundColor(getResources().getColor(R.color.colorCardBackground));
        timeTabCard.setCardBackgroundColor(getResources().getColor(R.color.colorCardBackground));
        achievementsTabCard.setCardBackgroundColor(getResources().getColor(R.color.colorCardBackground));
        
        // Show/hide content
        financialContent.setVisibility(View.VISIBLE);
        healthContent.setVisibility(View.GONE);
        timeContent.setVisibility(View.GONE);
        achievementsContent.setVisibility(View.GONE);
    }
    
    private void showHealthTab() {
        // Update tab selection UI
        financialTabCard.setCardBackgroundColor(getResources().getColor(R.color.colorCardBackground));
        healthTabCard.setCardBackgroundColor(getResources().getColor(R.color.colorPhysical));
        timeTabCard.setCardBackgroundColor(getResources().getColor(R.color.colorCardBackground));
        achievementsTabCard.setCardBackgroundColor(getResources().getColor(R.color.colorCardBackground));
        
        // Show/hide content
        financialContent.setVisibility(View.GONE);
        healthContent.setVisibility(View.VISIBLE);
        timeContent.setVisibility(View.GONE);
        achievementsContent.setVisibility(View.GONE);
    }
    
    private void showTimeTab() {
        // Update tab selection UI
        financialTabCard.setCardBackgroundColor(getResources().getColor(R.color.colorCardBackground));
        healthTabCard.setCardBackgroundColor(getResources().getColor(R.color.colorCardBackground));
        timeTabCard.setCardBackgroundColor(getResources().getColor(R.color.colorMental));
        achievementsTabCard.setCardBackgroundColor(getResources().getColor(R.color.colorCardBackground));
        
        // Show/hide content
        financialContent.setVisibility(View.GONE);
        healthContent.setVisibility(View.GONE);
        timeContent.setVisibility(View.VISIBLE);
        achievementsContent.setVisibility(View.GONE);
    }
    
    private void showAchievementsTab() {
        // Update tab selection UI
        financialTabCard.setCardBackgroundColor(getResources().getColor(R.color.colorCardBackground));
        healthTabCard.setCardBackgroundColor(getResources().getColor(R.color.colorCardBackground));
        timeTabCard.setCardBackgroundColor(getResources().getColor(R.color.colorCardBackground));
        achievementsTabCard.setCardBackgroundColor(getResources().getColor(R.color.colorAccent));
        
        // Show/hide content
        financialContent.setVisibility(View.GONE);
        healthContent.setVisibility(View.GONE);
        timeContent.setVisibility(View.GONE);
        achievementsContent.setVisibility(View.VISIBLE);
    }
    
    private Map<Integer, Integer> analyzeJournalEntriesByWeek() {
        Map<Integer, Integer> entriesByWeek = new HashMap<>();
        
        try {
            // Calculate weeks since sobriety start
            long sobrietyStartTimeMillis = sobrietyStartDate;
            
            for (JournalEntry entry : journalEntries) {
                long entryTimeMillis = entry.getTimestamp();
                
                // Skip entries before sobriety start
                if (entryTimeMillis < sobrietyStartTimeMillis) continue;
                
                // Calculate which week this entry belongs to
                long weekMillis = entryTimeMillis - sobrietyStartTimeMillis;
                int weekNumber = (int)(weekMillis / (7 * 24 * 60 * 60 * 1000)) + 1;
                
                // Increment count for this week
                entriesByWeek.put(weekNumber, entriesByWeek.getOrDefault(weekNumber, 0) + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return entriesByWeek;
    }
    
    private void setupButtonListeners() {
        // Email report button
        emailReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendReportEmail();
            }
        });
        
        // Share report button
        shareReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareReport();
            }
        });
    }
    
    private void sendReportEmail() {
        try {
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("message/rfc822");
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "My Sobriety Progress Report");
            emailIntent.putExtra(Intent.EXTRA_TEXT, generateReportText());
            
            startActivity(Intent.createChooser(emailIntent, "Send report via email"));
        } catch (Exception e) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void shareReport() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My Sobriety Progress Report");
            shareIntent.putExtra(Intent.EXTRA_TEXT, generateReportText());
            
            startActivity(Intent.createChooser(shareIntent, "Share report via"));
        } catch (Exception e) {
            Toast.makeText(this, "Error sharing report", Toast.LENGTH_SHORT).show();
        }
    }
    
    private String generateReportText() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        
        StringBuilder reportText = new StringBuilder();
        reportText.append("SOBRIETY TRACKER - PROGRESS REPORT\n\n");
        reportText.append("Date Range: ").append(dateRangeTextView.getText()).append("\n\n");
        reportText.append("Total Sober Days: ").append(totalDaysTextView.getText()).append("\n");
        reportText.append("Money Saved: ").append(moneySavedTextView.getText()).append("\n");
        reportText.append("Calories Saved: ").append(caloriesSavedTextView.getText()).append("\n");
        reportText.append("Achievements Unlocked: ").append(achievementsCountTextView.getText()).append("\n");
        reportText.append("Journal Entries: ").append(journalCountTextView.getText()).append("\n\n");
        
        // Add health benefits
        reportText.append("HEALTH BENEFITS\n");
        float potentialWeightLoss = (caloriesSaved / 3500f); // 3500 calories = 1 pound
        reportText.append("• Potential weight control: ").append(String.format("%.1f", potentialWeightLoss)).append(" lbs\n");
        reportText.append("• Better sleep: ").append(Math.floor(totalDaysSober / 7)).append(" nights\n");
        reportText.append("• Healthier liver: ").append(totalDaysSober).append(" days\n\n");
        
        // Add time reclaimed
        float hoursSaved = totalDaysSober * 1.5f;
        reportText.append("TIME RECLAIMED\n");
        reportText.append("• Total hours saved: ").append(Math.round(hoursSaved)).append(" hours\n");
        reportText.append("• Equivalent to: ").append(Math.floor(hoursSaved / 24)).append(" full days\n\n");
        
        // Add weekly journal data
        reportText.append("RECENT JOURNALING ACTIVITY\n");
        Map<Integer, Integer> entriesByWeek = analyzeJournalEntriesByWeek();
        int weeksToShow = Math.min(5, entriesByWeek.size());
        int startWeek = Math.max(1, entriesByWeek.size() - weeksToShow + 1);
        for (int i = 0; i < weeksToShow; i++) {
            int weekNumber = startWeek + i;
            int entries = entriesByWeek.getOrDefault(weekNumber, 0);
            reportText.append("• Week ").append(weekNumber).append(": ")
                    .append(entries).append(" journal entries\n");
        }
        reportText.append("\n");
        
        // Final encouragement
        reportText.append("Keep up the great work on your sobriety journey!\n\n");
        
        reportText.append("Generated by Sobriety Tracker on ");
        reportText.append(dateFormat.format(new Date()));
        
        return reportText.toString();
    }
    
    private int getDaysSober() {
        long currentTime = System.currentTimeMillis();
        long diffInMillis = currentTime - sobrietyStartDate;
        return (int) TimeUnit.MILLISECONDS.toDays(diffInMillis);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_progress_report, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
}