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

import com.example.sobertime.Achievement.AchievementCategory;
import com.example.sobertime.model.SobrietyTracker; 
import com.example.sobertime.BaseActivity;

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

import android.graphics.Color;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.Legend;
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
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

public class ProgressReportActivity extends BaseActivity {

    private SobrietyTracker sobrietyTracker;
    
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

    private BarChart financialComparisonChart;
    private LineChart healthProgressChart;
    private PieChart timeInvestmentChart;
    private HorizontalBarChart achievementsChart;
    
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

        // Initialize SobrietyTracker
        sobrietyTracker = SobrietyTracker.getInstance(this);
        
        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Progress Report");
        }
        
        // Initialize database helpers
        databaseHelper = DatabaseHelper.getInstance(this);
        achievementManager = AchievementManager.getInstance(this);
        
        // Get sobriety count from the tracker instead of calculating it here
        // sobrietyStartDate = prefs.getLong(START_DATE_KEY, System.currentTimeMillis());
        totalDaysSober = sobrietyTracker.getDaysSober();
        
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
            // Core summary views (unchanged)
            reportContainer = findViewById(R.id.reportContainer);
            dateRangeTextView = findViewById(R.id.dateRangeTextView);
            totalDaysTextView = findViewById(R.id.totalDaysTextView);
            moneySavedTextView = findViewById(R.id.moneySavedTextView);
            caloriesSavedTextView = findViewById(R.id.caloriesSavedTextView);
            achievementsCountTextView = findViewById(R.id.achievementsCountTextView);
            journalCountTextView = findViewById(R.id.journalCountTextView);
            
            // Updated chart initializations
            financialComparisonChart = findViewById(R.id.financialComparisonChart);
            healthProgressChart = findViewById(R.id.healthProgressChart);
            timeInvestmentChart = findViewById(R.id.timeInvestmentChart);
            achievementsChart = findViewById(R.id.achievementsChart);
            
            // Tab navigation (unchanged)
            financialTabCard = findViewById(R.id.financialTabCard);
            healthTabCard = findViewById(R.id.healthTabCard);
            timeTabCard = findViewById(R.id.timeTabCard);
            achievementsTabCard = findViewById(R.id.achievementsTabCard);
            
            // Content containers (unchanged)
            financialContent = findViewById(R.id.financialContent);
            healthContent = findViewById(R.id.healthContent);
            timeContent = findViewById(R.id.timeContent);
            achievementsContent = findViewById(R.id.achievementsContent);
            
            // Buttons (unchanged)
            emailReportButton = findViewById(R.id.emailReportButton);
            shareReportButton = findViewById(R.id.shareReportButton);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadUserData() {
        try {
            // Load data needed for the report
            
            // Get money saved calculation using SobrietyTracker
            float drinkCost = databaseHelper.getFloatSetting("drink_cost", 8.50f);
            int drinksPerWeek = databaseHelper.getIntSetting("drinks_per_week", 15);
            moneySaved = sobrietyTracker.calculateMoneySaved(drinkCost, drinksPerWeek);
            
            // Calculate calories saved using SobrietyTracker
            int caloriesPerDrink = databaseHelper.getIntSetting("calories_per_drink", 150);
            caloriesSaved = sobrietyTracker.calculateCaloriesSaved(caloriesPerDrink, drinksPerWeek);
            
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
            String startDateStr = dateFormat.format(new Date(sobrietyTracker.getSobrietyStartDate()));
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
            // Financial content text (keeps existing text functionality)
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
            
            // Health content text (keeps existing text functionality)
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
            
            // Time content text (keeps existing text functionality)
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
            
            // Achievements content text (keeps existing text functionality)
            TextView nextMilestoneTextView = findViewById(R.id.nextMilestoneTextView);
            if (nextMilestoneTextView != null) {
                StringBuilder milestoneText = new StringBuilder();
                milestoneText.append("Achievements unlocked: ").append(achievements.size()).append("\n\n");
                
                // Find next milestone
                Achievement nextMilestone = achievementManager.getNextMilestone(totalDaysSober);
                if (nextMilestone != null) {
                    milestoneText.append("Next milestone: ").append(nextMilestone.getTitle())
                            .append(" (").append(nextMilestone.getDaysRequired() - totalDaysSober)
                            .append(" days to go)");
                } else {
                    milestoneText.append("Next milestone: Keep going! You're making great progress.");
                }
                
                nextMilestoneTextView.setText(milestoneText.toString());
            }
            
            // Now setup all charts
            setupFinancialChart();
            setupHealthChart();
            setupTimeChart();
            setupAchievementsChart();
            
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
    
    private void setupFinancialChart() {
        if (financialComparisonChart == null) return;
        
        try {
            // Create bar entries for financial comparison
            ArrayList<BarEntry> entries = new ArrayList<>();
        
            // Calculate savings every month for last 6 months
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            
            float drinkCost = databaseHelper.getFloatSetting("drink_cost", 8.50f);
            int drinksPerWeek = databaseHelper.getIntSetting("drinks_per_week", 15);
            
            // Month labels for X-axis
            final ArrayList<String> monthLabels = new ArrayList<>();
            
            // Get data for last 6 months
            for (int i = 5; i >= 0; i--) {
                Calendar monthCal = (Calendar) calendar.clone();
                monthCal.add(Calendar.MONTH, -i);
                
                // Add month label
                SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());
                monthLabels.add(monthFormat.format(monthCal.getTime()));
                
                // Calculate days sober in this month using SobrietyTracker
                int daysSoberInMonth = sobrietyTracker.getDaysSoberInMonth(
                    monthCal.get(Calendar.YEAR), 
                    monthCal.get(Calendar.MONTH)
                );
                
                // Calculate money saved in this month
                float drinksAvoidedInMonth = (daysSoberInMonth / 7.0f) * drinksPerWeek;
                float moneySavedInMonth = drinksAvoidedInMonth * drinkCost;
                
                // Add entry (position i, value)
                entries.add(new BarEntry(5-i, moneySavedInMonth));
            }
            
            // Create dataset
            BarDataSet dataSet = new BarDataSet(entries, "Monthly Savings ($)");
            dataSet.setColor(getResources().getColor(R.color.colorFinancial));
            dataSet.setValueTextColor(getResources().getColor(R.color.colorTextDark));
            dataSet.setValueTextSize(10f);
            
            // Format Y-axis values as currency
            dataSet.setValueFormatter(new ValueFormatter() {
                private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance();
                
                @Override
                public String getFormattedValue(float value) {
                    return currencyFormatter.format(value);
                }
            });
            
            // Create bar data with dataset
            BarData barData = new BarData(dataSet);
            
            // Setup chart appearance
            financialComparisonChart.setData(barData);
            financialComparisonChart.getDescription().setEnabled(false);
            financialComparisonChart.setDrawGridBackground(false);
            financialComparisonChart.setDrawBarShadow(false);
            financialComparisonChart.setDrawValueAboveBar(true);
            financialComparisonChart.setPinchZoom(false);
            financialComparisonChart.setDrawGridBackground(false);
            
            // Format X axis
            XAxis xAxis = financialComparisonChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setValueFormatter(new IndexAxisValueFormatter(monthLabels));
            xAxis.setDrawGridLines(false);
            
            // Format Y axis
            YAxis leftAxis = financialComparisonChart.getAxisLeft();
            leftAxis.setValueFormatter(new ValueFormatter() {
                private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance();
                
                @Override
                public String getFormattedValue(float value) {
                    return currencyFormatter.format(value);
                }
            });
            leftAxis.setDrawGridLines(true);
            
            // Right Y-axis not needed
            financialComparisonChart.getAxisRight().setEnabled(false);
            
            // Setup legend
            Legend legend = financialComparisonChart.getLegend();
            legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
            legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
            legend.setOrientation(Legend.LegendOrientation.VERTICAL);
            legend.setDrawInside(true);
            
            // Animate chart
            financialComparisonChart.animateY(1000);
            
            // Refresh chart
            financialComparisonChart.invalidate();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /*// Helper method to calculate days sober in a specific month
    private int getDaysSoberInMonth(int year, int month) {
        // Create calendar for first day of month
        Calendar firstDayOfMonth = Calendar.getInstance();
        firstDayOfMonth.set(year, month, 1, 0, 0, 0);
        firstDayOfMonth.set(Calendar.MILLISECOND, 0);
        
        // Create calendar for first day of next month
        Calendar firstDayOfNextMonth = (Calendar) firstDayOfMonth.clone();
        firstDayOfNextMonth.add(Calendar.MONTH, 1);
        
        // Get sobriety start millis
        long sobrietyStartMillis = sobrietyStartDate;
        
        // If sobriety started after this month, return 0
        if (sobrietyStartMillis > firstDayOfNextMonth.getTimeInMillis()) {
            return 0;
        }
        
        // If sobriety started before this month
        if (sobrietyStartMillis <= firstDayOfMonth.getTimeInMillis()) {
            // Calculate days in this month
            return (int) TimeUnit.MILLISECONDS.toDays(
                Math.min(System.currentTimeMillis(), firstDayOfNextMonth.getTimeInMillis()) - 
                firstDayOfMonth.getTimeInMillis());
        }
        
        // If sobriety started during this month
        return (int) TimeUnit.MILLISECONDS.toDays(
            Math.min(System.currentTimeMillis(), firstDayOfNextMonth.getTimeInMillis()) - 
            sobrietyStartMillis);
    }*/

    private void setupHealthChart() {
        if (healthProgressChart == null) return;
        
        try {
            // Create line entries for health benefits over time
            ArrayList<Entry> entries = new ArrayList<>();
            
            // Create fake data points based on health improvement timeline
            // X-axis: days, Y-axis: relative health improvement percentage
            
            // Improvements based on typical sobriety health benefits timeline
            entries.add(new Entry(0, 0)); // Day 0: Baseline
            entries.add(new Entry(1, 10)); // Day 1: Initial improvement
            entries.add(new Entry(3, 20)); // Day 3: Blood sugar normalizes
            entries.add(new Entry(7, 30)); // Day 7: Better sleep
            entries.add(new Entry(14, 40)); // Day 14: Reduced anxiety
            entries.add(new Entry(30, 50)); // Day 30: Blood pressure improvement
            entries.add(new Entry(90, 70)); // Day 90: Liver function improves
            entries.add(new Entry(180, 80)); // Day 180: Risk of stroke decreases
            entries.add(new Entry(365, 90)); // Day 365: Risk of heart disease decreases
            
            // Create dataset
            LineDataSet dataSet = new LineDataSet(entries, "Health Benefits Over Time");
            dataSet.setColor(getResources().getColor(R.color.colorPhysical));
            dataSet.setValueTextColor(getResources().getColor(R.color.colorTextDark));
            dataSet.setLineWidth(2f);
            dataSet.setCircleColor(getResources().getColor(R.color.colorPhysical));
            dataSet.setCircleRadius(4f);
            dataSet.setDrawCircleHole(true);
            dataSet.setCircleHoleRadius(2f);
            dataSet.setValueTextSize(10f);
            dataSet.setDrawValues(false); // Don't draw values on points for cleaner look
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Smooth curve
            dataSet.setDrawFilled(true); // Fill area under the line
            dataSet.setFillColor(getResources().getColor(R.color.colorPhysical));
            dataSet.setFillAlpha(50); // Semi-transparent fill
            
            // Create line data with dataset
            LineData lineData = new LineData(dataSet);
            
            // Setup chart appearance
            healthProgressChart.setData(lineData);
            healthProgressChart.getDescription().setEnabled(false);
            healthProgressChart.setDrawGridBackground(false);
            healthProgressChart.setTouchEnabled(true);
            healthProgressChart.setDragEnabled(true);
            healthProgressChart.setScaleEnabled(true);
            healthProgressChart.setPinchZoom(true);
            
            // Add a marker line for current day progress
            int currentDays = totalDaysSober;
            
            // LimitLine to show current day position
            LimitLine currentDayLine = new LimitLine(currentDays, "Today");
            currentDayLine.setLineColor(getResources().getColor(R.color.colorAccent));
            currentDayLine.setLineWidth(2f);
            currentDayLine.enableDashedLine(10f, 5f, 0f);
            currentDayLine.setTextSize(12f);
            currentDayLine.setTextColor(getResources().getColor(R.color.colorAccent));
            
            // Add limit line to X-axis
            XAxis xAxis = healthProgressChart.getXAxis();
            xAxis.addLimitLine(currentDayLine);
            
            // Format X axis
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(30f); // Show every 30 days
            xAxis.setDrawGridLines(false);
            xAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return (int)value + "d";
                }
            });
            
            // Format Y axis - percentage
            YAxis leftAxis = healthProgressChart.getAxisLeft();
            leftAxis.setDrawGridLines(true);
            leftAxis.setAxisMaximum(100);
            leftAxis.setAxisMinimum(0);
            leftAxis.setValueFormatter(new PercentFormatter());
            
            // Right Y-axis not needed
            healthProgressChart.getAxisRight().setEnabled(false);
            
            // Setup legend
            Legend legend = healthProgressChart.getLegend();
            legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
            legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
            legend.setOrientation(Legend.LegendOrientation.VERTICAL);
            legend.setDrawInside(true);
            
            // Animate chart
            healthProgressChart.animateX(1500);
            
            // Set visible range to make current day visible
            // Set minimum visible entry to show at least some history
            int visibleRange = Math.max(currentDays + 30, 90); // Show at least 90 days or current + 30
            healthProgressChart.setVisibleXRangeMaximum(visibleRange);
            
            // If current days > 90, move viewport to center on current days
            if (currentDays > 90) {
                healthProgressChart.moveViewToX(Math.max(0, currentDays - 45)); // Center view
            }
            
            // Refresh chart
            healthProgressChart.invalidate();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void setupTimeChart() {
        if (timeInvestmentChart == null) return;
        
        try {
            // Calculate the hours saved and create pie entries
            ArrayList<PieEntry> entries = new ArrayList<>();
            
            // Assuming 1.5 hours saved per day
            float hoursSaved = totalDaysSober * 1.5f;
            
            // Calculate different time benefits
            float productiveHours = hoursSaved * 0.4f; // 40% productive activities
            float sleepHours = hoursSaved * 0.3f;      // 30% better sleep
            float leisureHours = hoursSaved * 0.2f;    // 20% leisure time
            float socialHours = hoursSaved * 0.1f;     // 10% social time
            
            // Add entries
            entries.add(new PieEntry(productiveHours, "Productive"));
            entries.add(new PieEntry(sleepHours, "Better Sleep"));
            entries.add(new PieEntry(leisureHours, "Leisure"));
            entries.add(new PieEntry(socialHours, "Social"));
            
            // Create dataset
            PieDataSet dataSet = new PieDataSet(entries, "Time Reclaimed (Hours)");
            
            // Apply colors from a predefined ColorTemplate or custom colors
            ArrayList<Integer> colors = new ArrayList<>();
            colors.add(getResources().getColor(R.color.colorMental));
            colors.add(getResources().getColor(R.color.colorPrimary));
            colors.add(getResources().getColor(R.color.colorFinancial));
            colors.add(getResources().getColor(R.color.colorAccent));
            dataSet.setColors(colors);
            
            // Customize appearance
            dataSet.setValueTextColor(Color.WHITE);
            dataSet.setValueTextSize(12f);
            dataSet.setSliceSpace(3f);
            dataSet.setSelectionShift(5f);
            
            // Format values as hours
            dataSet.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return String.format("%.0f hrs", value);
                }
            });
            
            // Create pie data with dataset
            PieData pieData = new PieData(dataSet);
            
            // Setup chart appearance
            timeInvestmentChart.setData(pieData);
            timeInvestmentChart.getDescription().setEnabled(false);
            timeInvestmentChart.setRotationEnabled(true);
            timeInvestmentChart.setHighlightPerTapEnabled(true);
            timeInvestmentChart.setTransparentCircleRadius(30f);
            timeInvestmentChart.setHoleRadius(40f);
            timeInvestmentChart.setHoleColor(Color.WHITE);
            
            // Center text
            timeInvestmentChart.setCenterText(String.format("%.0f\nTotal Hours", hoursSaved));
            timeInvestmentChart.setCenterTextSize(16f);
            timeInvestmentChart.setCenterTextColor(getResources().getColor(R.color.colorTextDark));
            
            // Setup legend
            Legend legend = timeInvestmentChart.getLegend();
            legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
            legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
            legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
            legend.setDrawInside(false);
            
            // Add interaction
            timeInvestmentChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                @Override
                public void onValueSelected(Entry e, Highlight h) {
                    if (e instanceof PieEntry) {
                        PieEntry pe = (PieEntry) e;
                        Toast.makeText(ProgressReportActivity.this, 
                            String.format("%s: %.0f hours", pe.getLabel(), pe.getValue()),
                            Toast.LENGTH_SHORT).show();
                    }
                }
    
                @Override
                public void onNothingSelected() {
                    // Do nothing
                }
            });
            
            // Animate chart
            timeInvestmentChart.animateY(1400, Easing.EaseInOutQuad);
            
            // Refresh chart
            timeInvestmentChart.invalidate();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupAchievementsChart() {
        if (achievementsChart == null) return;
        
        try {
            // Create entries for achievement categories
            ArrayList<BarEntry> entries = new ArrayList<>();
            
            // Count achievements by category
            int timeAchievements = 0;
            int journalAchievements = 0;
            int savingsAchievements = 0;
            int challengeAchievements = 0;
            
            // Alternative approach using if statements instead of switch
            for (Achievement achievement : achievements) {
                AchievementCategory category = achievement.getCategory();
                
                if (category == AchievementCategory.TIME_MILESTONE) {
                    timeAchievements++;
                } else if (category == AchievementCategory.JOURNAL) {
                    journalAchievements++;
                } else if (category == AchievementCategory.FINANCIAL) {
                    savingsAchievements++;
                } else if (category == AchievementCategory.COMMUNITY) {
                    challengeAchievements++;
                }
            }
            
            // Add entries (position, value)
            entries.add(new BarEntry(0, timeAchievements));
            entries.add(new BarEntry(1, journalAchievements));
            entries.add(new BarEntry(2, savingsAchievements));
            entries.add(new BarEntry(3, challengeAchievements));
            
            // Category labels for Y-axis
            final String[] achievementLabels = new String[] {
                "Time Milestones", "Journal Entries", "Money Saved", "Challenges"
            };
            
            // Create dataset
            BarDataSet dataSet = new BarDataSet(entries, "Achievements By Category");
            
            // Apply colors
            ArrayList<Integer> colors = new ArrayList<>();
            colors.add(getResources().getColor(R.color.colorPrimary));
            colors.add(getResources().getColor(R.color.colorMental));
            colors.add(getResources().getColor(R.color.colorFinancial));
            colors.add(getResources().getColor(R.color.colorAccent));
            dataSet.setColors(colors);
            
            dataSet.setValueTextColor(getResources().getColor(R.color.colorTextDark));
            dataSet.setValueTextSize(12f);
            
            // Create bar data with dataset
            BarData barData = new BarData(dataSet);
            
            // Setup chart appearance
            achievementsChart.setData(barData);
            achievementsChart.getDescription().setEnabled(false);
            achievementsChart.setDrawGridBackground(false);
            achievementsChart.setDrawBarShadow(false);
            achievementsChart.setDrawValueAboveBar(true);
            
            // Format Y axis (categories)
            YAxis leftAxis = achievementsChart.getAxisLeft();
            leftAxis.setEnabled(true);
            leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
            leftAxis.setDrawGridLines(false);
            leftAxis.setGranularity(1f);
            leftAxis.setAxisMinimum(-0.5f);
            leftAxis.setAxisMaximum(entries.size() - 0.5f);
            leftAxis.setValueFormatter(new IndexAxisValueFormatter(achievementLabels));
            
            // Format X axis (values)
            XAxis xAxis = achievementsChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setDrawGridLines(false);
            xAxis.setGranularity(1f);
            xAxis.setAxisMinimum(0f);
            
            // Right Y-axis not needed
            achievementsChart.getAxisRight().setEnabled(false);
            
            // Setup legend
            Legend legend = achievementsChart.getLegend();
            legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
            legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
            legend.setOrientation(Legend.LegendOrientation.VERTICAL);
            legend.setDrawInside(true);
            
            // Add interaction
            achievementsChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                @Override
                public void onValueSelected(Entry e, Highlight h) {
                    int index = (int) h.getX();
                    if (index >= 0 && index < achievementLabels.length) {
                        String category = achievementLabels[index];
                        int count = (int) e.getY();
                        Toast.makeText(ProgressReportActivity.this, 
                            category + ": " + count + " achievements", 
                            Toast.LENGTH_SHORT).show();
                    }
                }
    
                @Override
                public void onNothingSelected() {
                    // Do nothing
                }
            });
            
            // Animate chart
            achievementsChart.animateY(1000);
            
            // Refresh chart
            achievementsChart.invalidate();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Add these methods to improve chart performance

    private void applyChartPerformanceOptimizations() {
        // Apply to all charts
        if (financialComparisonChart != null) {
            optimizeBarChart(financialComparisonChart);
        }
        
        if (healthProgressChart != null) {
            optimizeLineChart(healthProgressChart);
        }
        
        if (timeInvestmentChart != null) {
            optimizePieChart(timeInvestmentChart);
        }
        
        if (achievementsChart != null) {
            optimizeBarChart(achievementsChart);
        }
    }

    private void optimizeBarChart(BarChart chart) {
        // Reduce overdraw by setting maximum visible entries
        chart.setMaxVisibleValueCount(10);
        
        // Enable hardware acceleration
        chart.setHardwareAccelerationEnabled(true);
        
        // Disable unnecessary features
        chart.setKeepPositionOnRotation(true);
        chart.setExtraOffsets(10, 10, 10, 10);
        
        // Use software rendering for export only
        chart.setHardwareAccelerationEnabled(false);
        chart.setHardwareAccelerationEnabled(true);
    }

    private void optimizeLineChart(LineChart chart) {
        // Reduce overdraw
        chart.setMaxVisibleValueCount(60);
        
        // Enable hardware acceleration
        chart.setHardwareAccelerationEnabled(true);
        
        // More efficient drawing modes
        chart.setExtraOffsets(10, 10, 10, 10);
        
        // Set viewport limits
        chart.setVisibleXRangeMaximum(30);
        chart.setKeepPositionOnRotation(true);
        
        // For performance with many points, consider using:
        // LineDataSet.Mode.HORIZONTAL_BEZIER or LineDataSet.Mode.LINEAR
        // instead of CUBIC_BEZIER for datasets with many points
    }

    private void optimizePieChart(PieChart chart) {
        // Enable hardware acceleration
        chart.setHardwareAccelerationEnabled(true);
        
        // Reduce overdraw
        chart.setExtraOffsets(10, 10, 10, 10);
        
        // Disable features not needed
        chart.setRotationEnabled(true);
        chart.setHighlightPerTapEnabled(true);
        chart.setDrawEntryLabels(false); // Don't draw text on slices for better performance
    }

    // Call this method in onResume or when switching tabs
    private void onTabSelected(int tabPosition) {
        // Only load/refresh the chart that's being shown
        switch (tabPosition) {
            case 0: // Financial
                setupFinancialChart();
                break;
            case 1: // Health
                setupHealthChart();
                break;
            case 2: // Time
                setupTimeChart();
                break;
            case 3: // Achievements
                setupAchievementsChart();
                break;
        }
    }

    // Update your tab methods to call the optimized chart loading

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
        
        // Load only the visible chart for performance
        setupFinancialChart();
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

        // Load only the visible chart for performance
        setupHealthChart();
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

        // Load only the visible chart for performance
        setupTimeChart();
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

        // Load only the visible chart for performance
        setupAchievementsChart();
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