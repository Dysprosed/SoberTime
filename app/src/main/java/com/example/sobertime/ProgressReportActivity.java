package com.example.sobertime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
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
import androidx.core.content.FileProvider;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

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
    private PieChart timeInvestmentChart;
    private BarChart financialComparisonChart;
    private LineChart healthProgressChart;
    private BarChart achievementsChart;
    
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
        
        // Create visualizations
        createVisualizations();
        
        // Set up button listeners
        setupButtonListeners();
        
        // Show financial tab by default
        showFinancialTab();
    }
    
    private void initializeViews() {
        // Core summary views
        reportContainer = findViewById(R.id.reportContainer);
        dateRangeTextView = findViewById(R.id.dateRangeTextView);
        totalDaysTextView = findViewById(R.id.totalDaysTextView);
        moneySavedTextView = findViewById(R.id.moneySavedTextView);
        caloriesSavedTextView = findViewById(R.id.caloriesSavedTextView);
        achievementsCountTextView = findViewById(R.id.achievementsCountTextView);
        journalCountTextView = findViewById(R.id.journalCountTextView);
        
        // Charts
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
    }
    
    private void loadUserData() {
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
    }
    
    private void generateReportSummary() {
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
    
    private void createVisualizations() {
        createFinancialVisualization();
        createHealthVisualization();
        createTimeVisualization();
        createAchievementsVisualization();
    }
    
    private void createFinancialVisualization() {
        // Create financial comparison chart
        if (financialComparisonChart != null) {
            ArrayList<BarEntry> entries = new ArrayList<>();
            final String[] labels = new String[]{"Monthly Rent", "Smartphone", "Vacation", "Your Savings"};
            
            // Sample values - these would be customized to user's region
            entries.add(new BarEntry(0, 1400f)); // Monthly Rent
            entries.add(new BarEntry(1, 1000f)); // Smartphone
            entries.add(new BarEntry(2, 2000f)); // Vacation
            entries.add(new BarEntry(3, moneySaved)); // User's savings
            
            BarDataSet dataSet = new BarDataSet(entries, "Cost Comparison");
            dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
            
            BarData data = new BarData(dataSet);
            data.setBarWidth(0.7f);
            
            financialComparisonChart.getDescription().setEnabled(false);
            financialComparisonChart.setData(data);
            
            // Format X-axis with custom labels
            XAxis xAxis = financialComparisonChart.getXAxis();
            xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            
            financialComparisonChart.invalidate();
            
            // Add "what you could buy" real-world examples
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
        }
    }
    
    private void createHealthVisualization() {
        // Create health progress line chart
        if (healthProgressChart != null) {
            ArrayList<Entry> bloodPressureEntries = new ArrayList<>();
            ArrayList<Entry> sleepQualityEntries = new ArrayList<>();
            ArrayList<Entry> energyLevelEntries = new ArrayList<>();
            
            // Determine how many months to show based on days sober
            int months = Math.min(6, Math.max(1, totalDaysSober / 30));
            
            // Sample data for improvement curves
            for (int i = 0; i < months; i++) {
                // Blood pressure improvement curve
                float bpImprovement = 85 + (15f * i / 5f);
                if (bpImprovement > 100) bpImprovement = 100;
                bloodPressureEntries.add(new Entry(i+1, bpImprovement));
                
                // Sleep quality improvement curve
                float sleepImprovement = 70 + (25f * i / 5f);
                if (sleepImprovement > 95) sleepImprovement = 95;
                sleepQualityEntries.add(new Entry(i+1, sleepImprovement));
                
                // Energy level improvement curve
                float energyImprovement = 60 + (35f * i / 5f);
                if (energyImprovement > 95) energyImprovement = 95;
                energyLevelEntries.add(new Entry(i+1, energyImprovement));
            }
            
            // Create datasets
            LineDataSet bloodPressureSet = new LineDataSet(bloodPressureEntries, "Heart Health");
            bloodPressureSet.setColor(Color.BLUE);
            bloodPressureSet.setCircleColor(Color.BLUE);
            bloodPressureSet.setLineWidth(2f);
            
            LineDataSet sleepQualitySet = new LineDataSet(sleepQualityEntries, "Sleep Quality");
            sleepQualitySet.setColor(Color.GREEN);
            sleepQualitySet.setCircleColor(Color.GREEN);
            sleepQualitySet.setLineWidth(2f);
            
            LineDataSet energyLevelSet = new LineDataSet(energyLevelEntries, "Energy Levels");
            energyLevelSet.setColor(Color.RED);
            energyLevelSet.setCircleColor(Color.RED);
            energyLevelSet.setLineWidth(2f);
            
            // Add datasets to chart
            LineData lineData = new LineData(bloodPressureSet, sleepQualitySet, energyLevelSet);
            healthProgressChart.setData(lineData);
            healthProgressChart.getDescription().setEnabled(false);
            
            // Format axes
            XAxis xAxis = healthProgressChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setAxisMinimum(0.5f);
            xAxis.setAxisMaximum(months + 0.5f);
            
            YAxis leftAxis = healthProgressChart.getAxisLeft();
            leftAxis.setAxisMinimum(50f);
            leftAxis.setAxisMaximum(105f);
            
            healthProgressChart.invalidate();
            
            // Add physical impact details
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
        }
    }
    
    private void createTimeVisualization() {
        // Create time investment pie chart
        if (timeInvestmentChart != null) {
            ArrayList<PieEntry> entries = new ArrayList<>();
            
            // Calculate hours saved (assuming average of 1.5 hours per day typically spent drinking/recovering)
            float hoursSaved = totalDaysSober * 1.5f;
            
            // How that time was reinvested (example allocation)
            entries.add(new PieEntry(hoursSaved * 0.4f, "Quality Sleep"));
            entries.add(new PieEntry(hoursSaved * 0.25f, "Hobbies"));
            entries.add(new PieEntry(hoursSaved * 0.15f, "Exercise"));
            entries.add(new PieEntry(hoursSaved * 0.2f, "Social Connection"));
            
            PieDataSet dataSet = new PieDataSet(entries, "Time Reinvested");
            dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
            
            PieData data = new PieData(dataSet);
            data.setValueFormatter(new PercentFormatter(timeInvestmentChart));
            data.setValueTextSize(12f);
            
            timeInvestmentChart.setData(data);
            timeInvestmentChart.getDescription().setEnabled(false);
            timeInvestmentChart.setUsePercentValues(true);
            timeInvestmentChart.setCenterText("Hours\nReclaimed");
            timeInvestmentChart.invalidate();
            
            // Add journal consistency details
            if (journalEntries != null && !journalEntries.isEmpty()) {
                // Analyze journal entries by week
                Map<Integer, Integer> entriesByWeek = analyzeJournalEntriesByWeek();
                
                TextView timeDetailsTextView = findViewById(R.id.timeDetailsTextView);
                if (timeDetailsTextView != null) {
                    StringBuilder timeDetails = new StringBuilder();
                    timeDetails.append("Time Reclaimed: ").append(Math.round(hoursSaved)).append(" hours\n\n");
                    timeDetails.append("This is equivalent to:\n");
                    timeDetails.append("• ").append(Math.floor(hoursSaved / 24)).append(" full days\n");
                    timeDetails.append("• ").append(Math.floor(hoursSaved / 40)).append(" work weeks\n");
                    timeDetails.append("• ").append(Math.floor(hoursSaved / 2)).append(" movies\n\n");
                    
                    // Add journal consistency info
                    timeDetails.append("Your journaling consistency:\n");
                    
                    // Show the last 5 weeks or less
                    int weeksToShow = Math.min(5, entriesByWeek.size());
                    int startWeek = entriesByWeek.size() - weeksToShow + 1;
                    
                    for (int i = 0; i < weeksToShow; i++) {
                        int weekNumber = startWeek + i;
                        int entries = entriesByWeek.getOrDefault(weekNumber, 0);
                        timeDetails.append("• Week ").append(weekNumber).append(": ")
                                .append(entries).append(" journal entries\n");
                    }
                    
                    timeDetailsTextView.setText(timeDetails.toString());
                }
            }
        }
    }
    
    private void createAchievementsVisualization() {
        // Create achievements chart
        if (achievementsChart != null) {
            ArrayList<BarEntry> entries = new ArrayList<>();
            final String[] labels = new String[5];
            
            // Count achievements by category
            Map<Achievement.AchievementCategory, Integer> unlockedByCategory = new HashMap<>();
            Map<Achievement.AchievementCategory, Integer> totalByCategory = new HashMap<>();
            
            // Initialize counts to zero
            for (Achievement.AchievementCategory category : Achievement.AchievementCategory.values()) {
                unlockedByCategory.put(category, 0);
                totalByCategory.put(category, 0);
            }
            
            // Count unlocked achievements
            for (Achievement achievement : achievements) {
                Achievement.AchievementCategory category = achievement.getCategory();
                unlockedByCategory.put(category, unlockedByCategory.get(category) + 1);
            }
            
            // Get totals for each category
            for (Achievement achievement : achievementManager.getAllAchievements()) {
                Achievement.AchievementCategory category = achievement.getCategory();
                totalByCategory.put(category, totalByCategory.get(category) + 1);
            }
            
            // Prepare chart data (only include categories with achievements)
            int index = 0;
            for (Achievement.AchievementCategory category : Achievement.AchievementCategory.values()) {
                if (totalByCategory.get(category) > 0) {
                    entries.add(new BarEntry(index, new float[]{
                            unlockedByCategory.get(category),
                            totalByCategory.get(category) - unlockedByCategory.get(category)
                    }));
                    labels[index] = category.getDisplayName();
                    index++;
                }
            }
            
            // Create stacked bar data set
            BarDataSet dataSet = new BarDataSet(entries, "Achievements");
            dataSet.setColors(new int[]{Color.rgb(104, 241, 175), Color.rgb(164, 228, 251)});
            dataSet.setStackLabels(new String[]{"Unlocked", "Locked"});
            
            BarData data = new BarData(dataSet);
            
            achievementsChart.setData(data);
            achievementsChart.getDescription().setEnabled(false);
            
            // Format X-axis with custom labels
            XAxis xAxis = achievementsChart.getXAxis();
            xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            
            achievementsChart.invalidate();
            
            // Add next milestone info
            TextView nextMilestoneTextView = findViewById(R.id.nextMilestoneTextView);
            if (nextMilestoneTextView != null) {
                // Find next milestone
                Achievement nextMilestone = achievementManager.getNextMilestone(totalDaysSober);
                
                if (nextMilestone != null) {
                    // Calculate progress to next milestone
                    int nextMilestoneDays = nextMilestone.getDaysRequired();
                    int daysToGo = nextMilestoneDays - totalDaysSober;
                    int progress = (int)(100 * (float)totalDaysSober / nextMilestoneDays);
                    
                    StringBuilder milestoneText = new StringBuilder();
                    milestoneText.append("Next milestone: ").append(nextMilestone.getTitle()).append("\n");
                    milestoneText.append("Days to go: ").append(daysToGo).append("\n");
                    milestoneText.append("Progress: ").append(progress).append("%");
                    
                    nextMilestoneTextView.setText(milestoneText.toString());
                } else {
                    nextMilestoneTextView.setText("Congratulations! You've unlocked all time milestones!");
                }
            }
        }
    }
    
    private Map<Integer, Integer> analyzeJournalEntriesByWeek() {
        Map<Integer, Integer> entriesByWeek = new HashMap<>();
        
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
        int startWeek = entriesByWeek.size() - weeksToShow + 1;
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