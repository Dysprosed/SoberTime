package com.example.sobertime;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Manages inspirational quotes and stories
 */
public class QuoteManager {
    
    private static final String PREFS_NAME = "QuotePrefs";
    private static final String FAVORITES_KEY = "favorite_quotes";
    
    private Context context;
    private List<Quote> quotes;
    private Map<Integer, Quote> quoteMap;
    private SharedPreferences preferences;
    private Random random;
    
    private static QuoteManager instance;
    
    // Create singleton instance
    public static synchronized QuoteManager getInstance(Context context) {
        if (instance == null) {
            instance = new QuoteManager(context.getApplicationContext());
        }
        return instance;
    }
    
    private QuoteManager(Context context) {
        this.context = context;
        this.quotes = new ArrayList<>();
        this.quoteMap = new HashMap<>();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.random = new Random();
        
        // Initialize quotes
        initializeQuotes();
        
        // Load saved favorites
        loadFavorites();
    }
    
    private void initializeQuotes() {
        // Motivation category
        addQuote(new Quote(
                1,
                "Recovery is hard. Regret is harder.",
                "Brené Brown",
                Quote.CATEGORY_MOTIVATION
        ));
        
        addQuote(new Quote(
                2,
                "You can't go back and change the beginning, but you can start where you are and change the ending.",
                "C.S. Lewis",
                Quote.CATEGORY_MOTIVATION
        ));
        
        addQuote(new Quote(
                3,
                "Success is the sum of small efforts, repeated day in and day out.",
                "Robert Collier",
                Quote.CATEGORY_MOTIVATION
        ));
        
        addQuote(new Quote(
                4,
                "Rock bottom became the solid foundation on which I rebuilt my life.",
                "J.K. Rowling",
                Quote.CATEGORY_MOTIVATION
        ));
        
        // Strength category
        addQuote(new Quote(
                101,
                "Strength does not come from physical capacity. It comes from an indomitable will.",
                "Mahatma Gandhi",
                Quote.CATEGORY_STRENGTH
        ));
        
        addQuote(new Quote(
                102,
                "You are stronger than you know. More capable than you ever dreamed. And you are loved more than you could possibly imagine.",
                "Unknown",
                Quote.CATEGORY_STRENGTH
        ));
        
        addQuote(new Quote(
                103,
                "What lies behind us and what lies before us are tiny matters compared to what lies within us.",
                "Ralph Waldo Emerson",
                Quote.CATEGORY_STRENGTH
        ));
        
        // Healing category
        addQuote(new Quote(
                201,
                "Healing is an art. It takes time, it takes practice. It takes love.",
                "Maza Dohta",
                Quote.CATEGORY_HEALING
        ));
        
        addQuote(new Quote(
                202,
                "The wound is the place where the light enters you.",
                "Rumi",
                Quote.CATEGORY_HEALING
        ));
        
        addQuote(new Quote(
                203,
                "Healing yourself is connected with healing others.",
                "Yoko Ono",
                Quote.CATEGORY_HEALING
        ));
        
        // Mindfulness category
        addQuote(new Quote(
                301,
                "The present moment is the only time over which we have dominion.",
                "Thich Nhat Hanh",
                Quote.CATEGORY_MINDFULNESS
        ));
        
        addQuote(new Quote(
                302,
                "Be where you are; otherwise you will miss your life.",
                "Buddha",
                Quote.CATEGORY_MINDFULNESS
        ));
        
        addQuote(new Quote(
                303,
                "The best way to capture moments is to pay attention. This is how we cultivate mindfulness.",
                "Jon Kabat-Zinn",
                Quote.CATEGORY_MINDFULNESS
        ));
        
        // Gratitude category
        addQuote(new Quote(
                401,
                "Gratitude turns what we have into enough.",
                "Anonymous",
                Quote.CATEGORY_GRATITUDE
        ));
        
        addQuote(new Quote(
                402,
                "When I started counting my blessings, my whole life turned around.",
                "Willie Nelson",
                Quote.CATEGORY_GRATITUDE
        ));
        
        addQuote(new Quote(
                403,
                "Gratitude makes sense of our past, brings peace for today, and creates a vision for tomorrow.",
                "Melody Beattie",
                Quote.CATEGORY_GRATITUDE
        ));
    }
    
    private void addQuote(Quote quote) {
        quotes.add(quote);
        quoteMap.put(quote.getId(), quote);
    }
    
    private void loadFavorites() {
        String favoritesJson = preferences.getString(FAVORITES_KEY, null);
        
        if (favoritesJson != null) {
            try {
                JSONArray jsonArray = new JSONArray(favoritesJson);
                
                for (int i = 0; i < jsonArray.length(); i++) {
                    int quoteId = jsonArray.getInt(i);
                    Quote quote = quoteMap.get(quoteId);
                    if (quote != null) {
                        quote.setFavorite(true);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void saveFavorites() {
        try {
            JSONArray jsonArray = new JSONArray();
            
            for (Quote quote : quotes) {
                if (quote.isFavorite()) {
                    jsonArray.put(quote.getId());
                }
            }
            
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(FAVORITES_KEY, jsonArray.toString());
            editor.apply();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public List<Quote> getAllQuotes() {
        return quotes;
    }
    
    public List<Quote> getQuotesByCategory(String category) {
        List<Quote> categoryQuotes = new ArrayList<>();
        for (Quote quote : quotes) {
            if (quote.getCategory().equals(category)) {
                categoryQuotes.add(quote);
            }
        }
        return categoryQuotes;
    }
    
    public List<Quote> getFavoriteQuotes() {
        List<Quote> favorites = new ArrayList<>();
        for (Quote quote : quotes) {
            if (quote.isFavorite()) {
                favorites.add(quote);
            }
        }
        return favorites;
    }
    
    public Quote getRandomQuote() {
        if (quotes.isEmpty()) {
            return null;
        }
        int randomIndex = random.nextInt(quotes.size());
        return quotes.get(randomIndex);
    }
    
    public Quote getRandomQuoteByCategory(String category) {
        List<Quote> categoryQuotes = getQuotesByCategory(category);
        if (categoryQuotes.isEmpty()) {
            return null;
        }
        int randomIndex = random.nextInt(categoryQuotes.size());
        return categoryQuotes.get(randomIndex);
    }
    
    public Quote getQuoteById(int id) {
        return quoteMap.get(id);
    }
    
    public void toggleFavorite(int quoteId) {
        Quote quote = quoteMap.get(quoteId);
        if (quote != null) {
            quote.setFavorite(!quote.isFavorite());
            saveFavorites();
        }
    }
}
