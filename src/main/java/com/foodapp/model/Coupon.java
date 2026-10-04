package com.foodapp.model;

import com.foodapp.util.SimpleJson;

public class Coupon {
    private String code;
    private double discountPercentage;
    private double maxDiscount;
    private double minOrderValue;
    private String description;

    public Coupon(String code, double discountPercentage, double maxDiscount, double minOrderValue, String description) {
        this.code = code;
        this.discountPercentage = discountPercentage;
        this.maxDiscount = maxDiscount;
        this.minOrderValue = minOrderValue;
        this.description = description;
    }

    public String getCode() { return code; }
    public double getDiscountPercentage() { return discountPercentage; }
    public double getMaxDiscount() { return maxDiscount; }
    public double getMinOrderValue() { return minOrderValue; }
    public String getDescription() { return description; }

    public double calculateDiscount(double subtotal) {
        if (subtotal < minOrderValue) return 0.0;
        double discount = (subtotal * discountPercentage) / 100.0;
        if (maxDiscount > 0 && discount > maxDiscount) {
            discount = maxDiscount;
        }
        return discount;
    }

    public String toJson() {
        return String.format(
            "{\"code\":\"%s\",\"discountPercentage\":%.1f,\"maxDiscount\":%.2f,\"minOrderValue\":%.2f,\"description\":\"%s\"}",
            SimpleJson.escape(code),
            discountPercentage,
            maxDiscount,
            minOrderValue,
            SimpleJson.escape(description)
        );
    }
}
