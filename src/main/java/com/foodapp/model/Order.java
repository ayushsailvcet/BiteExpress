package com.foodapp.model;

import com.foodapp.util.SimpleJson;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private String orderId;
    private String customerName;
    private String phone;
    private String deliveryAddress;
    private List<OrderItem> items = new ArrayList<>();
    private double subtotal;
    private double deliveryFee;
    private double tax;
    private double discount;
    private double total;
    private OrderStatus status;
    private String paymentMethod; // e.g. "CARD", "UPI", "COD"
    private String promoCode;
    private long timestamp;
    private int estimatedMinutes;

    public Order() {
        this.timestamp = System.currentTimeMillis();
        this.status = OrderStatus.RECEIVED;
        this.estimatedMinutes = 25;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(double deliveryFee) { this.deliveryFee = deliveryFee; }

    public double getTax() { return tax; }
    public void setTax(double tax) { this.tax = tax; }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(int estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }

    public String toJson() {
        StringBuilder itemsJson = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            itemsJson.append(items.get(i).toJson());
            if (i < items.size() - 1) itemsJson.append(",");
        }
        itemsJson.append("]");

        return String.format(
            "{\"orderId\":\"%s\",\"customerName\":\"%s\",\"phone\":\"%s\",\"deliveryAddress\":\"%s\",\"items\":%s,\"subtotal\":%.2f,\"deliveryFee\":%.2f,\"tax\":%.2f,\"discount\":%.2f,\"total\":%.2f,\"status\":\"%s\",\"statusDescription\":\"%s\",\"paymentMethod\":\"%s\",\"promoCode\":\"%s\",\"timestamp\":%d,\"estimatedMinutes\":%d}",
            SimpleJson.escape(orderId),
            SimpleJson.escape(customerName),
            SimpleJson.escape(phone),
            SimpleJson.escape(deliveryAddress),
            itemsJson.toString(),
            subtotal,
            deliveryFee,
            tax,
            discount,
            total,
            status != null ? status.name() : "RECEIVED",
            status != null ? SimpleJson.escape(status.getDescription()) : "",
            SimpleJson.escape(paymentMethod != null ? paymentMethod : "CARD"),
            SimpleJson.escape(promoCode != null ? promoCode : ""),
            timestamp,
            estimatedMinutes
        );
    }
}
