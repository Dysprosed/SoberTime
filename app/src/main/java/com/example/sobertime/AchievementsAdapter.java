package com.example.sobertime;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AchievementsAdapter extends RecyclerView.Adapter<AchievementsAdapter.ViewHolder> {

    private Context context;
    private List<Achievement> achievements;

    public AchievementsAdapter(Context context, List<Achievement> achievements) {
        this.context = context;
        this.achievements = achievements;
    }

    public void updateAchievements(List<Achievement> achievements) {
        this.achievements = achievements;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_achievement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Achievement achievement = achievements.get(position);

        holder.titleTextView.setText(achievement.getTitle());
        holder.descriptionTextView.setText(achievement.getDescription());
        holder.categoryTextView.setText(achievement.getCategory().getDisplayName());

        // Set achievement icon
        int iconResourceId = getResourceId(achievement.getIconName(), "drawable", context.getPackageName());
        if (iconResourceId != 0) {
            holder.iconImageView.setImageResource(iconResourceId);
        } else {
            // Use default icon if resource not found
            holder.iconImageView.setImageResource(R.drawable.ic_notification);
        }

        // For time milestones, show the milestone date
        if (achievement.getCategory() == Achievement.AchievementCategory.TIME_MILESTONE && 
            !achievement.getMilestoneDate().isEmpty()) {
            holder.dateTextView.setText("Date: " + achievement.getMilestoneDate());
            holder.dateTextView.setVisibility(View.VISIBLE);
            
            // Also display days required
            holder.daysBadgeTextView.setText(String.valueOf(achievement.getDaysRequired()));
            holder.daysBadgeTextView.setVisibility(View.VISIBLE);
        } else {
            holder.dateTextView.setVisibility(View.GONE);
            holder.daysBadgeTextView.setVisibility(View.GONE);
        }

        // Style based on unlock status and special handling for today's milestone
        if (achievement.isToday()) {
            // Today's milestone - special highlighting
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.celebrationBackground));
            holder.iconImageView.setAlpha(1.0f);
            holder.titleTextView.setTextColor(ContextCompat.getColor(context, R.color.celebrationText));
            holder.statusTextView.setText("TODAY! 🎉");
            holder.statusTextView.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
            holder.statusTextView.setVisibility(View.VISIBLE);
        } else if (achievement.isUnlocked()) {
            // Unlocked achievement
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorSurface));
            holder.iconImageView.setAlpha(1.0f);
            holder.titleTextView.setTextColor(ContextCompat.getColor(context, R.color.colorOnSurface));
            holder.statusTextView.setText("Unlocked: " + formatDate(achievement.getUnlockTime()));
            holder.statusTextView.setTextColor(ContextCompat.getColor(context, R.color.colorPhysical));
            holder.statusTextView.setVisibility(View.VISIBLE);
        } else {
            // Locked achievement
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorBackground));
            holder.iconImageView.setAlpha(0.5f);
            holder.titleTextView.setTextColor(ContextCompat.getColor(context, R.color.colorTextLight));
            holder.statusTextView.setText("Locked");
            holder.statusTextView.setTextColor(ContextCompat.getColor(context, R.color.colorTextLight));
            holder.statusTextView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return achievements.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView iconImageView;
        TextView titleTextView;
        TextView descriptionTextView;
        TextView categoryTextView;
        TextView statusTextView;
        TextView dateTextView;
        TextView daysBadgeTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.achievementCardView);
            iconImageView = itemView.findViewById(R.id.achievementIconImageView);
            titleTextView = itemView.findViewById(R.id.achievementTitleTextView);
            descriptionTextView = itemView.findViewById(R.id.achievementDescriptionTextView);
            categoryTextView = itemView.findViewById(R.id.achievementCategoryTextView);
            statusTextView = itemView.findViewById(R.id.achievementStatusTextView);
            dateTextView = itemView.findViewById(R.id.achievementDateTextView);
            daysBadgeTextView = itemView.findViewById(R.id.daysBadgeTextView);
        }
    }

    /**
     * Helper method to get resource ID by name
     */
    private int getResourceId(String resourceName, String resourceType, String packageName) {
        try {
            return context.getResources().getIdentifier(resourceName, resourceType, packageName);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Format timestamp to readable date
     */
    private String formatDate(long timestamp) {
        if (timestamp == 0) {
            return "";
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        return dateFormat.format(new Date(timestamp));
    }
}