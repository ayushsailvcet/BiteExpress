package com.foodapp.service;

import com.foodapp.model.FoodItem;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class FoodService {
    private final Map<String, FoodItem> foodMap = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(100);

    public FoodService() {
        initDefaultMenu();
    }

    private void initDefaultMenu() {
        addFood(new FoodItem(
            "F101",
            "Truffle Artisan Burger",
            "Burgers",
            14.99,
            "Prime aged beef patty, white truffle aioli, melted sharp cheddar, crispy applewood bacon & brioche bun.",
            4.9,
            "15-20 min",
            "Bestseller,Chef Special",
            "/images/burger.jpg",
            true
        ));

        addFood(new FoodItem(
            "F102",
            "Wood-Fired Pepperoni Pizza",
            "Pizza",
            18.50,
            "Authentic Neapolitan sourdough, San Marzano tomato sauce, fresh buffalo mozzarella & spicy artisan pepperoni.",
            4.8,
            "20-25 min",
            "Bestseller,Spicy",
            "/images/pizza.jpg",
            true
        ));

        addFood(new FoodItem(
            "F103",
            "Rich Tonkotsu Pork Ramen",
            "Asian",
            16.00,
            "18-hour slow simmered pork bone broth, hand-crafted ramen noodles, tender chashu pork belly & ajitsuke tamago egg.",
            4.9,
            "15-18 min",
            "Popular",
            "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=600&auto=format&fit=crop",
            true
        ));

        addFood(new FoodItem(
            "F104",
            "Chocolate Molten Lava Cake",
            "Desserts",
            8.99,
            "Warm decadent chocolate cake with flowing dark ganache center, served with Madagascan vanilla bean ice cream.",
            4.9,
            "10-12 min",
            "Chef Special,Sweet",
            "https://images.unsplash.com/photo-1606313564200-e75d5e30476c?w=600&auto=format&fit=crop",
            true
        ));

        addFood(new FoodItem(
            "F105",
            "Smoked Salmon Avocado Poke",
            "Healthy",
            15.50,
            "Fresh Atlantic salmon, hass avocado, edamame, cucumber ribbon, sushi rice & tamari sesame drizzle.",
            4.7,
            "10-15 min",
            "Healthy,Fresh",
            "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600&auto=format&fit=crop",
            true
        ));

        addFood(new FoodItem(
            "F106",
            "Sparkling Mango Passion Smoothie",
            "Drinks",
            5.99,
            "Fresh tropical Alphonso mango blended with passionfruit, sparkling coconut water & fresh mint.",
            4.8,
            "5 min",
            "Vegan,Refreshing",
            "https://images.unsplash.com/photo-1553530666-ba11a7da3888?w=600&auto=format&fit=crop",
            true
        ));
    }

    public void addFood(FoodItem item) {
        if (item.getId() == null || item.getId().isEmpty()) {
            item.setId("F" + idSequence.incrementAndGet());
        }
        foodMap.put(item.getId(), item);
    }

    public List<FoodItem> getAllFood() {
        return new ArrayList<>(foodMap.values());
    }

    public FoodItem getFoodById(String id) {
        return foodMap.get(id);
    }

    public boolean updateFood(FoodItem updatedItem) {
        if (foodMap.containsKey(updatedItem.getId())) {
            foodMap.put(updatedItem.getId(), updatedItem);
            return true;
        }
        return false;
    }

    public boolean deleteFood(String id) {
        return foodMap.remove(id) != null;
    }

    public String getAllFoodAsJson() {
        StringBuilder sb = new StringBuilder("[");
        List<FoodItem> list = getAllFood();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i).toJson());
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
