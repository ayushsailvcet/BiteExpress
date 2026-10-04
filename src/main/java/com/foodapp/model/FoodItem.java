package com.foodapp.model;

import com.foodapp.util.SimpleJson;

public class FoodItem {
    private String id;
    private String name;
    private String category;
    private double price;
    private String description;
    private double rating;
    private String prepTime;
    private String tags; // e.g. "Veg", "Spicy", "Chef Special"
    private String imageUrl;
    private boolean available;

    public FoodItem() {}

    public FoodItem(String id, String name, String category, double price, String description,
                    double rating, String prepTime, String tags, String imageUrl, boolean available) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.description = description;
        this.rating = rating;
        this.prepTime = prepTime;
        this.tags = tags;
        this.imageUrl = imageUrl;
        this.available = available;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public String getPrepTime() { return prepTime; }
    public void setPrepTime(String prepTime) { this.prepTime = prepTime; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String toJson() {
        return String.format(
            "{\"id\":\"%s\",\"name\":\"%s\",\"category\":\"%s\",\"price\":%.2f,\"description\":\"%s\",\"rating\":%.1f,\"prepTime\":\"%s\",\"tags\":\"%s\",\"imageUrl\":\"%s\",\"available\":%b}",
            SimpleJson.escape(id),
            SimpleJson.escape(name),
            SimpleJson.escape(category),
            price,
            SimpleJson.escape(description),
            rating,
            SimpleJson.escape(prepTime),
            SimpleJson.escape(tags),
            SimpleJson.escape(imageUrl),
            available
        );
    }
}
