package com.foodapp.model;

import com.foodapp.util.SimpleJson;

public class OrderItem {
    private String foodId;
    private String name;
    private double price;
    private int quantity;
    private String notes;

    public OrderItem() {}

    public OrderItem(String foodId, String name, double price, int quantity, String notes) {
        this.foodId = foodId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.notes = notes != null ? notes : "";
    }

    public String getFoodId() { return foodId; }
    public void setFoodId(String foodId) { this.foodId = foodId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public double getItemTotal() {
        return price * quantity;
    }

    public String toJson() {
        return String.format(
            "{\"foodId\":\"%s\",\"name\":\"%s\",\"price\":%.2f,\"quantity\":%d,\"notes\":\"%s\",\"itemTotal\":%.2f}",
            SimpleJson.escape(foodId),
            SimpleJson.escape(name),
            price,
            quantity,
            SimpleJson.escape(notes),
            getItemTotal()
        );
    }
}
