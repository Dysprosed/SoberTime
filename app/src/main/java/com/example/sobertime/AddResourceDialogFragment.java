package com.example.sobertime;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import android.util.Patterns;

public class AddResourceDialogFragment extends DialogFragment {
    
    private EditText nameEditText;
    private EditText descriptionEditText;
    private EditText urlEditText;
    private Button saveButton;
    private Button cancelButton;
    
    private AddResourceListener listener;
    
    public interface AddResourceListener {
        void onResourceAdded(SupportResource resource);
    }
    
    public void setListener(AddResourceListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        
        // Inflate the layout
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_resource, null);
        builder.setView(view);
        
        // Get references to views
        nameEditText = view.findViewById(R.id.resourceNameEditText);
        descriptionEditText = view.findViewById(R.id.resourceDescriptionEditText);
        urlEditText = view.findViewById(R.id.resourceUrlEditText);
        saveButton = view.findViewById(R.id.saveResourceButton);
        cancelButton = view.findViewById(R.id.cancelButton);
        
        // Create dialog
        AlertDialog dialog = builder.create();
        
        // Set button listeners
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInputs()) {
                    String name = nameEditText.getText().toString().trim();
                    String description = descriptionEditText.getText().toString().trim();
                    String url = urlEditText.getText().toString().trim();
                    
                    // Ensure URL has proper scheme
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://" + url;
                    }
                    
                    SupportResource resource = new SupportResource(name, description, url, true);
                    
                    if (listener != null) {
                        listener.onResourceAdded(resource);
                    }
                    
                    dialog.dismiss();
                }
            }
        });
        
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        
        return dialog;
    }
    
    private boolean validateInputs() {
        boolean isValid = true;
        
        // Validate name
        String name = nameEditText.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            nameEditText.setError("Name is required");
            isValid = false;
        }
        
        // Validate description
        String description = descriptionEditText.getText().toString().trim();
        if (TextUtils.isEmpty(description)) {
            descriptionEditText.setError("Description is required");
            isValid = false;
        }
        
        // Validate URL
        String url = urlEditText.getText().toString().trim();
        if (TextUtils.isEmpty(url)) {
            urlEditText.setError("URL is required");
            isValid = false;
        } else if (!isValidUrl(url)) {
            urlEditText.setError("Please enter a valid URL");
            isValid = false;
        }
        
        return isValid;
    }
    
    private boolean isValidUrl(String url) {
        // Add http:// if missing for validation purposes
        String urlForValidation = url;
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            urlForValidation = "https://" + url;
        }
        
        return Patterns.WEB_URL.matcher(urlForValidation).matches();
    }
}
