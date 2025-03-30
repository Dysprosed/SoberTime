package com.example.sobertime;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * BaseActivity provides common functionality for all activities in the app,
 * including consistent navigation, logging, and error handling.
 */
public abstract class BaseActivity extends AppCompatActivity {

    private static final String TAG = "BaseActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up the back button in the action bar for all activities except MainActivity
        if (!(this instanceof MainActivity) && getSupportActionBar() != null) {
            try {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            } catch (Exception e) {
                Log.e(TAG, "Error setting up back navigation", e);
            }
        } else if (getSupportActionBar() == null) {
            Log.w(TAG, "SupportActionBar is null");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Perform back navigation with custom logic if needed
            if (!handleSpecificBackNavigation()) {
                finish();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Provide consistent back navigation across activities
        if (!handleSpecificBackNavigation()) {
            super.onBackPressed();
        }
    }

    /**
     * Override this method in activities that need custom back navigation.
     * Return true if you handled the navigation, false to use default behavior.
     */
    protected boolean handleSpecificBackNavigation() {
        return false;
    }

    /**
     * Display a toast message safely with error handling.
     */
    protected void showToast(String message) {
        if (message == null || message.trim().isEmpty()) {
            Log.e(TAG, "Invalid toast message");
            return;
        }
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error displaying toast", e);
        }
    }
}
