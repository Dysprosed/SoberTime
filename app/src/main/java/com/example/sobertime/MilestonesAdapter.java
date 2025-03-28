package com.example.sobertime;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.List;

public class MilestonesAdapter extends ArrayAdapter<MilestonesActivity.Milestone> {

    private final Context context;
    private final List<MilestonesActivity.Milestone> milestones;

    public MilestonesAdapter(Context context, List<MilestonesActivity.Milestone> milestones) {
        super(context, R.layout.item_milestone, milestones);
        this.context = context;
        this.milestones = milestones;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_milestone, parent, false);

            holder = new ViewHolder();
            holder.titleTextView = convertView.findViewById(R.id.milestoneTitleText);
            holder.dateTextView = convertView.findViewById(R.id.milestoneDateText);
            holder.descriptionTextView = convertView.findViewById(R.id.milestoneDescriptionText);
            holder.statusImageView = convertView.findViewById(R.id.milestoneStatusIcon);
            holder.daysBadgeTextView = convertView.findViewById(R.id.daysBadgeText);
            holder.cardView = convertView.findViewById(R.id.milestoneCardView);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Get milestone item
        MilestonesActivity.Milestone milestone = milestones.get(position);

        // Set data to views
        holder.titleTextView.setText(milestone.getTitle());
        holder.dateTextView.setText(milestone.getDate());
        holder.descriptionTextView.setText(milestone.getDescription());
        holder.daysBadgeTextView.setText(String.valueOf(milestone.getDays()));

        // Set status icon and styling based on achievement
        if (milestone.isToday()) {
            // Today's milestone
            holder.statusImageView.setImageResource(R.drawable.ic_milestone_today);
            holder.statusImageView.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent));
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.celebrationBackground));
            holder.titleTextView.setTextColor(ContextCompat.getColor(context, R.color.celebrationText));
        } else if (milestone.isAchieved()) {
            // Already achieved milestone
            holder.statusImageView.setImageResource(R.drawable.ic_milestone_achieved);
            holder.statusImageView.setColorFilter(ContextCompat.getColor(context, R.color.colorPhysical));
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.white));
            holder.titleTextView.setTextColor(ContextCompat.getColor(context, R.color.colorText));
        } else {
            // Future milestone
            holder.statusImageView.setImageResource(R.drawable.ic_milestone_upcoming);
            holder.statusImageView.setColorFilter(ContextCompat.getColor(context, R.color.colorTextLight));
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.white));
            holder.titleTextView.setTextColor(ContextCompat.getColor(context, R.color.colorTextLight));
        }

        return convertView;
    }

    private static class ViewHolder {
        TextView titleTextView;
        TextView dateTextView;
        TextView descriptionTextView;
        ImageView statusImageView;
        TextView daysBadgeTextView;
        androidx.cardview.widget.CardView cardView;
    }
}
