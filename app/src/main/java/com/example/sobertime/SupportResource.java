package com.example.sobertime;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a community support resource
 */
public class SupportResource {
    private String name;
    private String description;
    private String url;
    private boolean isCustom;
    
    public SupportResource(String name, String description, String url, boolean isCustom) {
        this.name = name;
        this.description = description;
        this.url = url;
        this.isCustom = isCustom;
    }
    
    // Getters
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getUrl() {
        return url;
    }
    
    public boolean isCustom() {
        return isCustom;
    }
    
    /**
     * Convert a list of resources to JSON string
     */
    public static String toJson(List<SupportResource> resources) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        
        for (SupportResource resource : resources) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", resource.getName());
            jsonObject.put("description", resource.getDescription());
            jsonObject.put("url", resource.getUrl());
            jsonObject.put("isCustom", resource.isCustom());
            
            jsonArray.put(jsonObject);
        }
        
        return jsonArray.toString();
    }
    
    /**
     * Create a list of resources from JSON string
     */
    public static List<SupportResource> fromJson(String json) throws JSONException {
        List<SupportResource> resources = new ArrayList<>();
        JSONArray jsonArray = new JSONArray(json);
        
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            
            String name = jsonObject.getString("name");
            String description = jsonObject.getString("description");
            String url = jsonObject.getString("url");
            boolean isCustom = jsonObject.getBoolean("isCustom");
            
            resources.add(new SupportResource(name, description, url, isCustom));
        }
        
        return resources;
    }
}
