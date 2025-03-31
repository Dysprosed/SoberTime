package com.example.sobertime;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import android.util.Log;

import java.text.NumberFormat;
import java.util.Locale;

public class HealthBenefitsActivity extends BaseActivity {

    private TextView physicalBenefitsTextView;
    private TextView mentalBenefitsTextView;
    private TextView financialBenefitsTextView;
    private CardView physicalCard;
    private CardView mentalCard;
    private CardView financialCard;

    private int daysSober;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_benefits);

        // Set the title for the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Your Health Benefits");
        }

        // Get days sober from intent
        daysSober = getIntent().getIntExtra("days_sober", 0);

        // Initialize database helper
        databaseHelper = DatabaseHelper.getInstance(this);
        if (databaseHelper == null) {
            showToast("Database error. Please restart the app.");
            Log.e("HealthBenefitsActivity", "DatabaseHelper instance is null.");
            return;
        }

        // Initialize views
        initializeViews();

        // Calculate and display benefits
        calculateBenefits();
    }

    private void initializeViews() {
        physicalBenefitsTextView = findViewById(R.id.physicalBenefitsText);
        mentalBenefitsTextView = findViewById(R.id.mentalBenefitsText);
        financialBenefitsTextView = findViewById(R.id.financialBenefitsText);
        physicalCard = findViewById(R.id.physicalCard);
        mentalCard = findViewById(R.id.mentalCard);
        financialCard = findViewById(R.id.financialCard);

        if (physicalBenefitsTextView == null || mentalBenefitsTextView == null || financialBenefitsTextView == null) {
            showToast("Error initializing views: Some views are missing.");
            Log.e("HealthBenefitsActivity", "View initialization failed.");
        }
    }

    private void calculateBenefits() {
        // Calculate physical benefits
        calculatePhysicalBenefits();

        // Calculate mental benefits
        calculateMentalBenefits();

        // Calculate financial benefits
        calculateFinancialBenefits();
    }

    private void calculatePhysicalBenefits() {
        StringBuilder physicalBenefits = new StringBuilder();

        // Get user's custom values from database
        int drinksPerWeek = databaseHelper.getIntSetting("drinks_per_week", 15);
        int caloriesPerDrink = databaseHelper.getIntSetting("calories_per_drink", 150);

        // Calories saved
        float drinksAvoided = (daysSober / 7.0f) * drinksPerWeek;
        int caloriesSaved = (int) (drinksAvoided * caloriesPerDrink);

        physicalBenefits.append("• ").append(NumberFormat.getNumberInstance(Locale.US).format(caloriesSaved))
                .append(" calories saved\n\n");

        // Weight potential (rough estimate, 3500 calories = 1 pound of fat)
        float potentialWeightLossPounds = caloriesSaved / 3500.0f;
        float potentialWeightLossKg = potentialWeightLossPounds * 0.453592f;

        physicalBenefits.append("• Potential weight loss: ").append(String.format("%.1f", potentialWeightLossPounds))
                .append(" lbs (").append(String.format("%.1f", potentialWeightLossKg)).append(" kg)\n\n");

        // Add various health benefits based on time sober
        if (daysSober >= 1) {
            physicalBenefits.append("• Blood sugar levels normalize\n\n");
        }

        if (daysSober >= 3) {
            physicalBenefits.append("• Improved hydration and skin appearance\n\n");
        }

        if (daysSober >= 7) {
            physicalBenefits.append("• Better sleep quality and patterns established\n\n");
            physicalBenefits.append("• Digestive system improvement begins\n\n");
        }

        if (daysSober >= 14) {
            physicalBenefits.append("• Reduced acid reflux and stomach irritation\n\n");
            physicalBenefits.append("• Liver begins to repair\n\n");
        }

        if (daysSober >= 30) {
            physicalBenefits.append("• Blood pressure may start to normalize\n\n");
            physicalBenefits.append("• Immune system function improves\n\n");
        }

        if (daysSober >= 90) {
            physicalBenefits.append("• Significant liver health improvement\n\n");
            physicalBenefits.append("• Reduced risk of alcohol-related cancer\n\n");
        }

        if (daysSober >= 365) {
            physicalBenefits.append("• Risk of stroke begins to decrease\n\n");
            physicalBenefits.append("• Reduced risk of cardiovascular disease\n\n");
            physicalBenefits.append("• Increased life expectancy\n\n");
        }

        physicalBenefitsTextView.setText(physicalBenefits.toString());
    }

    private void calculateMentalBenefits() {
        StringBuilder mentalBenefits = new StringBuilder();

        if (daysSober >= 1) {
            mentalBenefits.append("• Increased clarity and focus\n\n");
        }

        if (daysSober >= 3) {
            mentalBenefits.append("• Improved mood stability\n\n");
        }

        if (daysSober >= 7) {
            mentalBenefits.append("• Reduced anxiety levels\n\n");
            mentalBenefits.append("• Better memory function\n\n");
        }

        if (daysSober >= 14) {
            mentalBenefits.append("• Increased ability to manage stress\n\n");
            mentalBenefits.append("• Improved concentration\n\n");
        }

        if (daysSober >= 30) {
            mentalBenefits.append("• Depression symptoms may reduce\n\n");
            mentalBenefits.append("• Clearer thinking and problem-solving\n\n");
        }

        if (daysSober >= 90) {
            mentalBenefits.append("• Enhanced emotional processing\n\n");
            mentalBenefits.append("• Sharper mental acuity\n\n");
            mentalBenefits.append("• Improved self-esteem\n\n");
        }

        if (daysSober >= 180) {
            mentalBenefits.append("• New neural pathways formed\n\n");
            mentalBenefits.append("• Better decision making\n\n");
        }

        if (daysSober >= 365) {
            mentalBenefits.append("• Substantial cognitive improvement\n\n");
            mentalBenefits.append("• Positive outlook on life\n\n");
            mentalBenefits.append("• Healthier coping mechanisms established\n\n");
        }

        mentalBenefitsTextView.setText(mentalBenefits.toString());
    }

    private void calculateFinancialBenefits() {
        StringBuilder financialBenefits = new StringBuilder();

        // Get user's custom values from database
        float drinkCost = databaseHelper.getFloatSetting("drink_cost", 8.50f);
        int drinksPerWeek = databaseHelper.getIntSetting("drinks_per_week", 15);

        // Calculate money saved
        float drinksAvoided = (daysSober / 7.0f) * drinksPerWeek;
        float moneySaved = drinksAvoided * drinkCost;

        // Format as currency
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance();
        String formattedMoneySaved = currencyFormatter.format(moneySaved);

        financialBenefits.append("• Money saved: ").append(formattedMoneySaved).append("\n\n");

        // Monthly rate
        float monthlySavings = (drinksPerWeek * 4.3f) * drinkCost;
        String formattedMonthlySavings = currencyFormatter.format(monthlySavings);

        financialBenefits.append("• Current monthly savings rate: ").append(formattedMonthlySavings).append("\n\n");

        // Yearly projection
        float yearlySavings = (drinksPerWeek * 52) * drinkCost;
        String formattedYearlySavings = currencyFormatter.format(yearlySavings);

        financialBenefits.append("• Projected yearly savings: ").append(formattedYearlySavings).append("\n\n");

        // Additional financial benefits
        financialBenefits.append("• Reduced healthcare costs\n\n");
        financialBenefits.append("• Potential lower insurance premiums\n\n");

        if (daysSober >= 30) {
            financialBenefits.append("• Improved productivity at work\n\n");
        }

        if (daysSober >= 90) {
            financialBenefits.append("• Reduced spending on hangover remedies\n\n");
        }

        if (daysSober >= 180) {
            financialBenefits.append("• Better financial decision making\n\n");
        }

        if (daysSober >= 365) {
            financialBenefits.append("• With one year of savings (").append(formattedYearlySavings).append("), you could fund:\n");
            float vacation = yearlySavings * 0.5f;
            float investment = yearlySavings * 0.3f;
            float emergency = yearlySavings * 0.2f;

            financialBenefits.append("  - A vacation: ").append(currencyFormatter.format(vacation)).append("\n");
            financialBenefits.append("  - An investment: ").append(currencyFormatter.format(investment)).append("\n");
            financialBenefits.append("  - Emergency fund: ").append(currencyFormatter.format(emergency)).append("\n\n");
        }

        financialBenefitsTextView.setText(financialBenefits.toString());
    }

}