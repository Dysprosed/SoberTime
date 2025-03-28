package com.example.sobertime;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class QuoteAdapter extends RecyclerView.Adapter<QuoteAdapter.ViewHolder> {
    
    private Context context;
    private List<Quote> quotes;
    private OnQuoteInteractionListener listener;
    
    public interface OnQuoteInteractionListener {
        void onFavoriteToggled(int quoteId);
        void onQuoteClicked(Quote quote);
    }
    
    public QuoteAdapter(Context context, List<Quote> quotes, OnQuoteInteractionListener listener) {
        this.context = context;
        this.quotes = quotes;
        this.listener = listener;
    }
    
    public void updateQuotes(List<Quote> quotes) {
        this.quotes = quotes;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_quote, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Quote quote = quotes.get(position);
        
        holder.quoteTextView.setText(quote.getText());
        holder.authorTextView.setText("— " + quote.getAuthor());
        holder.categoryTextView.setText(quote.getCategory());
        
        // Set favorite button state
        if (quote.isFavorite()) {
            holder.favoriteButton.setImageResource(android.R.drawable.btn_star_big_on);
            holder.favoriteButton.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent));
        } else {
            holder.favoriteButton.setImageResource(android.R.drawable.btn_star_big_off);
            holder.favoriteButton.setColorFilter(ContextCompat.getColor(context, R.color.colorTextLight));
        }
        
        // Set category color
        int categoryColor = getCategoryColor(quote.getCategory());
        holder.categoryTextView.setTextColor(categoryColor);
        
        // Set click listeners
        final int quoteId = quote.getId();
        holder.favoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onFavoriteToggled(quoteId);
                }
            }
        });
        
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onQuoteClicked(quote);
                }
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return quotes.size();
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView quoteTextView;
        TextView authorTextView;
        TextView categoryTextView;
        ImageButton favoriteButton;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.quoteCardView);
            quoteTextView = itemView.findViewById(R.id.quoteTextView);
            authorTextView = itemView.findViewById(R.id.authorTextView);
            categoryTextView = itemView.findViewById(R.id.categoryTextView);
            favoriteButton = itemView.findViewById(R.id.favoriteButton);
        }
    }
    
    /**
     * Get color for quote category
     */
    private int getCategoryColor(String category) {
        switch (category) {
            case Quote.CATEGORY_MOTIVATION:
                return ContextCompat.getColor(context, R.color.colorPrimary);
            case Quote.CATEGORY_STRENGTH:
                return ContextCompat.getColor(context, R.color.colorPhysical);
            case Quote.CATEGORY_HEALING:
                return ContextCompat.getColor(context, R.color.colorMental);
            case Quote.CATEGORY_MINDFULNESS:
                return ContextCompat.getColor(context, R.color.colorFinancial);
            case Quote.CATEGORY_GRATITUDE:
                return ContextCompat.getColor(context, R.color.colorAccent);
            default:
                return ContextCompat.getColor(context, R.color.colorText);
        }
    }
}
