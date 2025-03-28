package com.example.sobertime;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SupportResourceAdapter extends RecyclerView.Adapter<SupportResourceAdapter.ViewHolder> {
    
    private Context context;
    private List<SupportResource> resources;
    
    public SupportResourceAdapter(Context context, List<SupportResource> resources) {
        this.context = context;
        this.resources = resources;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_support_resource, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SupportResource resource = resources.get(position);
        
        holder.nameTextView.setText(resource.getName());
        holder.descriptionTextView.setText(resource.getDescription());
        
        // Set visit button click listener
        holder.visitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl(resource.getUrl());
            }
        });
        
        // Show custom indicator if it's a custom resource
        if (resource.isCustom()) {
            holder.customIndicatorTextView.setVisibility(View.VISIBLE);
        } else {
            holder.customIndicatorTextView.setVisibility(View.GONE);
        }
    }
    
    @Override
    public int getItemCount() {
        return resources.size();
    }
    
    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        context.startActivity(intent);
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView descriptionTextView;
        TextView customIndicatorTextView;
        Button visitButton;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.resourceNameTextView);
            descriptionTextView = itemView.findViewById(R.id.resourceDescriptionTextView);
            customIndicatorTextView = itemView.findViewById(R.id.customIndicatorTextView);
            visitButton = itemView.findViewById(R.id.visitResourceButton);
        }
    }
}
