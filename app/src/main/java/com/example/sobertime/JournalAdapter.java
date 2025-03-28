package com.example.sobertime;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class JournalAdapter extends RecyclerView.Adapter<JournalAdapter.ViewHolder> {
    
    private Context context;
    private List<JournalEntry> entries;
    private OnEntryClickListener listener;
    
    public interface OnEntryClickListener {
        void onEntryClick(JournalEntry entry);
        void onEntryLongClick(JournalEntry entry);
    }
    
    public JournalAdapter(Context context, List<JournalEntry> entries, OnEntryClickListener listener) {
        this.context = context;
        this.entries = entries;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_journal_entry, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final JournalEntry entry = entries.get(position);
        
        holder.titleTextView.setText(entry.getTitle());
        holder.dateTextView.setText(entry.getFormattedDateTime());
        holder.summaryTextView.setText(entry.getSummary(100));
        
        // Show mood if available
        if (entry.getMood() != null && !entry.getMood().isEmpty()) {
            holder.moodTextView.setVisibility(View.VISIBLE);
            holder.moodTextView.setText("Mood: " + entry.getMood());
        } else {
            holder.moodTextView.setVisibility(View.GONE);
        }
        
        // Show craving level if available
        if (entry.getCravingLevel() > 0) {
            holder.cravingTextView.setVisibility(View.VISIBLE);
            holder.cravingTextView.setText("Craving: " + getCravingLevelString(entry.getCravingLevel()));
        } else {
            holder.cravingTextView.setVisibility(View.GONE);
        }
        
        // Click listeners
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onEntryClick(entry);
                }
            }
        });
        
        holder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (listener != null) {
                    listener.onEntryLongClick(entry);
                    return true;
                }
                return false;
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return entries.size();
    }
    
    private String getCravingLevelString(int level) {
        switch (level) {
            case 1: return "Very Low";
            case 2: return "Low";
            case 3: return "Moderate";
            case 4: return "High";
            case 5: return "Very High";
            default: return "None";
        }
    }
    
    public class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView titleTextView;
        TextView dateTextView;
        TextView summaryTextView;
        TextView moodTextView;
        TextView cravingTextView;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.journalEntryCardView);
            titleTextView = itemView.findViewById(R.id.journalTitleText);
            dateTextView = itemView.findViewById(R.id.journalDateText);
            summaryTextView = itemView.findViewById(R.id.journalSummaryText);
            moodTextView = itemView.findViewById(R.id.journalMoodText);
            cravingTextView = itemView.findViewById(R.id.journalCravingText);
        }
    }
}
