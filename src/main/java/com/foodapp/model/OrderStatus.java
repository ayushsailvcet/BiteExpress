package com.foodapp.model;

public enum OrderStatus {
    RECEIVED("Received", "Order received by restaurant"),
    PREPARING("Preparing", "Chef is preparing your meal"),
    OUT_FOR_DELIVERY("Out for Delivery", "Rider is on the way to deliver"),
    DELIVERED("Delivered", "Delivered successfully"),
    CANCELLED("Cancelled", "Order was cancelled");

    private final String displayName;
    private final String description;

    OrderStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
