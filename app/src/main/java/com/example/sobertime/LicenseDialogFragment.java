package com.example.sobertime;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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
        
        // Get references to the views
        TextView licenseTextView = view.findViewById(R.id.licenseTextView);
        Button closeButton = view.findViewById(R.id.closeButton);
        
        // Set license text
        licenseTextView.setText(getLicenseText());
        
        // Create the dialog
        AlertDialog dialog = builder.create();
        
        // Set up the close button
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        
        return dialog;
    }
    
    private String getLicenseText() {
        return "# New Dawn: Sobriety Tracking App\n\n" +
                "## GNU General Public License v3.0\n\n" +
                "Copyright (c) 2025\n\n" +
                "This program is free software: you can redistribute it and/or modify " +
                "it under the terms of the GNU General Public License as published by " +
                "the Free Software Foundation, either version 3 of the License, or " +
                "(at your option) any later version.\n\n" +
                "This program is distributed in the hope that it will be useful, " +
                "but WITHOUT ANY WARRANTY; without even the implied warranty of " +
                "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the " +
                "GNU General Public License for more details.\n\n" +
                "You should have received a copy of the GNU General Public License " +
                "along with this program. If not, see <https://www.gnu.org/licenses/>.\n\n" +
                "## Acknowledgments\n\n" +
                "- This application was developed with assistance from Claude AI by Anthropic.\n\n" +
                "- Special thanks to the open source community for their invaluable tools " +
                "and libraries that made this project possible.\n\n" +
                "- Icons and visual elements are created using Android's standard resources " +
                "or custom-designed with attribution where applicable.\n\n" +
                "## Privacy Policy\n\n" +
                "New Dawn is committed to user privacy:\n\n" +
                "- All data is stored locally on your device\n" +
                "- No personal information is collected or transmitted\n" +
                "- Backup files are saved to your device storage and are not shared\n" +
                "- No analytics or tracking is implemented\n\n" +
                "This app is provided for sobriety tracking assistance and " +
                "is not intended as a substitute for professional medical advice, diagnosis, or treatment.";
    }
}