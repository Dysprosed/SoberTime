package com.example.sobertime;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import android.graphics.Color;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class LicenseDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        
        // Inflate and set the layout for the dialog
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_license, null);
        builder.setView(view);
        
        // Get references to the views - replace TextView with WebView in your layout
        WebView licenseWebView = view.findViewById(R.id.licenseWebView);
        Button closeButton = view.findViewById(R.id.closeButton);
        
        // Configure WebView
        licenseWebView.setBackgroundColor(Color.TRANSPARENT);
        // This ensures text color adapts to theme
        String textColor = ThemeUtils.isDarkTheme(requireContext()) ? "#FFFFFF" : "#000000";
        
        // Load HTML content
        String htmlContent = "<html><body style='color:" + textColor + ";'>" + 
                            getHtmlLicenseText() + 
                            "</body></html>";
        licenseWebView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
        
        // Create the dialog
        AlertDialog dialog = builder.create();
        
        // Set up the close button
        closeButton.setOnClickListener(v -> dialog.dismiss());
        
        return dialog;
    }
    
    private String getHtmlLicenseText() {
        return "<h2>New Dawn: Sobriety Tracking App</h2>" +
               "<h3>GNU General Public License v3.0</h3>" +
               "<p>Copyright (c) 2025</p>" +
               "<p>This program is free software: you can redistribute it and/or modify " +
               "it under the terms of the GNU General Public License as published by " +
               "the Free Software Foundation, either version 3 of the License, or " +
               "(at your option) any later version.</p>" +
               "<p>This program is distributed in the hope that it will be useful, " +
               "but WITHOUT ANY WARRANTY; without even the implied warranty of " +
               "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the " +
               "GNU General Public License for more details.</p>" +
               "<p>You should have received a copy of the GNU General Public License " +
               "along with this program. If not, see <a href='https://www.gnu.org/licenses/'>https://www.gnu.org/licenses/</a>.</p>" +
               "<h3>Acknowledgments</h3>" +
               "<ul>" +
               "<li>This application was developed with assistance from Claude AI by Anthropic.</li>" +
               "<li>Special thanks to the open source community for their invaluable tools " +
               "and libraries that made this project possible.</li>" +
               "<li>Icons and visual elements are created using Android's standard resources " +
               "or custom-designed with attribution where applicable.</li>" +
               "</ul>" +
               "<h3>Privacy Policy</h3>" +
               "<p>New Dawn is committed to user privacy:</p>" +
               "<ul>" +
               "<li>All data is stored locally on your device</li>" +
               "<li>No personal information is collected or transmitted</li>" +
               "<li>Backup files are saved to your device storage and are not shared</li>" +
               "<li>No analytics or tracking is implemented</li>" +
               "</ul>" +
                   "<p>This app is provided for sobriety tracking assistance and " +
                   "is not intended as a substitute for professional medical advice, diagnosis, or treatment.</p>";
        }
    }