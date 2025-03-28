package com.example.sobertime;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MilestonesActivity extends AppCompatActivity {

    private ListView milestonesListView;
    private long sobrietyStartDate;
    private List<Milestone> milestones;
    private MilestonesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_milestones);

        // Set up action bar with back button - FIX: Use built-in ActionBar, not Toolbar
        try {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Your Milestones");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Get sobriety start date from intent
        sobrietyStartDate = getIntent().getLongExtra("start_date", System.currentTimeMillis());

        // Initialize views
        milestonesListView = findViewById(R.id.milestonesListView);

        // Generate milestone data
        generateMilestones();

        // Set up adapter
        adapter = new MilestonesAdapter(this, milestones);
        milestonesListView.setAdapter(adapter);
    }

    /**
     * Generate milestone data based on sobriety start date
     */
    private void generateMilestones() {
        milestones = new ArrayList<>();

        // Define important milestones in days
        int[] milestoneDays = {1, 7, 14, 30, 60, 90, 180, 365, 730, 1095, 1460, 1825};

        // Calculate current days sober
        long currentTime = System.currentTimeMillis();
        long diffInMillis = currentTime - sobrietyStartDate;
        int daysSober = (int) TimeUnit.MILLISECONDS.toDays(diffInMillis);

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());

        // Create milestone objects
        for (int days : milestoneDays) {
            // Calculate milestone date
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(sobrietyStartDate);
            calendar.add(Calendar.DAY_OF_YEAR, days);

            Date milestoneDate = calendar.getTime();
            String formattedDate = dateFormat.format(milestoneDate);

            // Check if milestone is achieved or upcoming
            boolean isAchieved = days <= daysSober;
            boolean isToday = days == daysSober;

            // Create milestone object
            Milestone milestone = new Milestone();
            milestone.setDays(days);
            milestone.setDate(formattedDate);
            milestone.setAchieved(isAchieved);
            milestone.setToday(isToday);
            milestone.setTitle(getMilestoneTitle(days));
            milestone.setDescription(getMilestoneDescription(days));

            milestones.add(milestone);
        }
    }

    /**
     * Get title for milestone based on day count
     */
    private String getMilestoneTitle(int days) {
        if (days == 1) return "First Day Sober";
        if (days == 7) return "One Week Milestone";
        if (days == 14) return "Two Weeks Milestone";
        if (days == 30) return "One Month Milestone";
        if (days == 60) return "Two Months Milestone";
        if (days == 90) return "90 Day Milestone";
        if (days == 180) return "Six Months Milestone";
        if (days == 365) return "One Year Anniversary";
        if (days == 730) return "Two Year Anniversary";
        if (days == 1095) return "Three Year Anniversary";
        if (days == 1460) return "Four Year Anniversary";
        if (days == 1825) return "Five Year Anniversary";

        return days + " Day Milestone";
    }

    /**
     * Get description for milestone based on day count
     */
    private String getMilestoneDescription(int days) {
        if (days == 1) {
            return "The beginning of your journey. The first day is one of the most important milestones.";
        } else if (days == 7) {
            return "Completing a full week shows serious commitment to your sobriety journey.";
        } else if (days == 14) {
            return "Two weeks in, your body is starting to experience the benefits of sobriety.";
        } else if (days == 30) {
            return "One month sober! This is when many physical health improvements become noticeable.";
        } else if (days == 60) {
            return "Two months of sobriety shows your dedication to a healthier lifestyle.";
        } else if (days == 90) {
            return "90 days marks a significant point in recovery where many new habits become established.";
        } else if (days == 180) {
            return "Half a year of sobriety! Your brain chemistry has made significant positive changes.";
        } else if (days == 365) {
            return "A full year of sobriety is an incredible achievement worth celebrating!";
        } else if (days >= 730) {
            int years = days / 365;
            return years + " years of sobriety represents a profound life transformation.";
        }

        return "Every milestone in your sobriety journey deserves recognition and celebration.";
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Inner class to represent a milestone
     */
    public static class Milestone {
        private int days;
        private String date;
        private boolean achieved;
        private boolean today;
        private String title;
        private String description;

        public int getDays() { return days; }
        public void setDays(int days) { this.days = days; }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public boolean isAchieved() { return achieved; }
        public void setAchieved(boolean achieved) { this.achieved = achieved; }

        public boolean isToday() { return today; }
        public void setToday(boolean today) { this.today = today; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}