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
                "## Open Source License\n\n" +
                "Copyright (c) 2025 [Your Name]\n\n" +
                "Permission is hereby granted, free of charge, to any person obtaining a copy " +
                "of this software and associated documentation files (the \"Software\"), to deal " +
                "in the Software without restriction, including without limitation the rights " +
                "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell " +
                "copies of the Software, and to permit persons to whom the Software is " +
                "furnished to do so, subject to the following conditions:\n\n" +
                "The above copyright notice and this permission notice shall be included in all " +
                "copies or substantial portions of the Software.\n\n" +
                "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR " +
                "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, " +
                "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE " +
                "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER " +
                "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, " +
                "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE " +
                "SOFTWARE.\n\n" +
                "## Acknowledgments\n\n" +
                "- This application was developed with assistance from AI technologies, " +
                "specifically Claude from Anthropic.\n\n" +
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
                "This app is provided for personal use to assist with sobriety tracking and " +
                "is not intended as a substitute for professional medical advice, diagnosis, or treatment.";
    }
}
