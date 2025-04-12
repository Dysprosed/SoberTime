package com.example.sobertime.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sobertime.R;
import com.example.sobertime.model.AccountabilityBuddy;

import java.util.List;

public class BuddyAdapter extends RecyclerView.Adapter<BuddyAdapter.BuddyViewHolder> {

    private List<AccountabilityBuddy> buddyList;
    private Context context;
    private OnBuddyActionListener listener;

    public interface OnBuddyActionListener {
        void onBuddyEditClicked(AccountabilityBuddy buddy, int position);
        void onBuddyDeleteClicked(AccountabilityBuddy buddy, int position);
        void onBuddyEnabledChanged(AccountabilityBuddy buddy, boolean enabled);
        void onNotificationSettingChanged(AccountabilityBuddy buddy, String setting, boolean enabled);
    }

    public BuddyAdapter(Context context, List<AccountabilityBuddy> buddyList, OnBuddyActionListener listener) {
        this.context = context;
        this.buddyList = buddyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BuddyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_accountability_buddy, parent, false);
        return new BuddyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BuddyViewHolder holder, int position) {
        AccountabilityBuddy buddy = buddyList.get(position);
        
        // Set buddy information
        holder.buddyNameText.setText(buddy.getName());
        holder.buddyPhoneText.setText(buddy.getPhone());
        
        // Set switch states without triggering listener
        holder.buddyEnabledSwitch.setOnCheckedChangeListener(null);
        holder.notifyCheckinCheckbox.setOnCheckedChangeListener(null);
        holder.notifyRelapseCheckbox.setOnCheckedChangeListener(null);
        holder.notifyMilestoneCheckbox.setOnCheckedChangeListener(null);
        
        holder.buddyEnabledSwitch.setChecked(buddy.isEnabled());
        holder.notifyCheckinCheckbox.setChecked(buddy.isNotifyOnCheckin());
        holder.notifyRelapseCheckbox.setChecked(buddy.isNotifyOnRelapse());
        holder.notifyMilestoneCheckbox.setChecked(buddy.isNotifyOnMilestone());
        
        // Setup listeners
        holder.buddyEnabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    AccountabilityBuddy currentBuddy = buddyList.get(adapterPosition);
                    currentBuddy.setEnabled(isChecked);
                    listener.onBuddyEnabledChanged(currentBuddy, isChecked);
                }
            }
        });
        
        holder.notifyCheckinCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                AccountabilityBuddy currentBuddy = buddyList.get(adapterPosition);
                currentBuddy.setNotifyOnCheckin(isChecked);
                listener.onNotificationSettingChanged(currentBuddy, "notify_on_checkin", isChecked);
            }
        });
        
        holder.notifyRelapseCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                AccountabilityBuddy currentBuddy = buddyList.get(adapterPosition);
                currentBuddy.setNotifyOnRelapse(isChecked);
                listener.onNotificationSettingChanged(currentBuddy, "notify_on_relapse", isChecked);
            }
        });
        
        holder.notifyMilestoneCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                AccountabilityBuddy currentBuddy = buddyList.get(adapterPosition);
                currentBuddy.setNotifyOnMilestone(isChecked);
                listener.onNotificationSettingChanged(currentBuddy, "notify_on_milestone", isChecked);
            }
        });
        
        // Setup button click listeners
        holder.editBuddyButton.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                listener.onBuddyEditClicked(buddyList.get(adapterPosition), adapterPosition);
            }
        });
        
        holder.deleteBuddyButton.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                listener.onBuddyDeleteClicked(buddyList.get(adapterPosition), adapterPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return buddyList.size();
    }

    public void updateList(List<AccountabilityBuddy> newList) {
        this.buddyList = newList;
        notifyDataSetChanged();
    }

    static class BuddyViewHolder extends RecyclerView.ViewHolder {
        TextView buddyNameText;
        TextView buddyPhoneText;
        Switch buddyEnabledSwitch;
        CheckBox notifyCheckinCheckbox;
        CheckBox notifyRelapseCheckbox;
        CheckBox notifyMilestoneCheckbox;
        ImageButton editBuddyButton;
        ImageButton deleteBuddyButton;

        public BuddyViewHolder(@NonNull View itemView) {
            super(itemView);
            buddyNameText = itemView.findViewById(R.id.buddyNameText);
            buddyPhoneText = itemView.findViewById(R.id.buddyPhoneText);
            buddyEnabledSwitch = itemView.findViewById(R.id.buddyEnabledSwitch);
            notifyCheckinCheckbox = itemView.findViewById(R.id.notifyCheckinCheckbox);
            notifyRelapseCheckbox = itemView.findViewById(R.id.notifyRelapseCheckbox);
            notifyMilestoneCheckbox = itemView.findViewById(R.id.notifyMilestoneCheckbox);
            editBuddyButton = itemView.findViewById(R.id.editBuddyButton);
            deleteBuddyButton = itemView.findViewById(R.id.deleteBuddyButton);
        }
    }
}