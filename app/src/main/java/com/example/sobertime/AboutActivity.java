package com.example.sobertime;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class AboutActivity extends BaseActivity {

    private TextView versionTextView;
    private Button githubButton;
    private Button donateButton;
    private Button licenseButton;
    
    private static final String APP_VERSION = "1.0.0";
    private static final String GITHUB_URL = "https://github.com/Dysprosed/SoberTime";
    private static final String DONATE_URL = "https://www.buymeacoffee.com/Dysprosed";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        
        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("About New Dawn");
        }
        
        // Initialize views
        initializeViews();
        
        // Set up app info
        setupAppInfo();
        
        // Set up button listeners
        setupButtonListeners();
    }
    
    private void initializeViews() {
        versionTextView = findViewById(R.id.versionTextView);
        githubButton = findViewById(R.id.githubButton);
        donateButton = findViewById(R.id.donateButton);
        licenseButton = findViewById(R.id.licenseButton);
    }
    
    private void setupAppInfo() {
        versionTextView.setText("Version: " + APP_VERSION);
    }
    
    private void setupButtonListeners() {
        githubButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl(GITHUB_URL);
            }
        });
        
        donateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl(DONATE_URL);
            }
        });
        
        licenseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLicenseDialog();
            }
        });
    }
    
    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }
    
    private void showLicenseDialog() {
        LicenseDialogFragment dialogFragment = new LicenseDialogFragment();
        dialogFragment.show(getSupportFragmentManager(), "license_dialog");
    }
    
}
