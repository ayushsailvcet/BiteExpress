package com.foodapp.service;

import com.foodapp.model.*;
import com.foodapp.util.SimpleJson;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class OrderService {
    private final Map<String, Order> orderMap = new ConcurrentHashMap<>();
    private final Map<String, Coupon> couponMap = new ConcurrentHashMap<>();
    private final AtomicLong orderSeq = new AtomicLong(1000);

    public OrderService() {
        initCoupons();
        initSampleOrders();
    }

    private void initCoupons() {
        couponMap.put("WELCOME50", new Coupon("WELCOME50", 50.0, 15.0, 20.0, "50% OFF up to $15 on orders above $20"));
        couponMap.put("FEAST10", new Coupon("FEAST10", 20.0, 10.0, 15.0, "20% OFF up to $10 on orders above $15"));
        couponMap.put("FREESHIP", new Coupon("FREESHIP", 100.0, 3.99, 10.0, "Free Delivery on orders above $10"));
    }

    private void initSampleOrders() {
        Order sample1 = new Order();
        sample1.setOrderId("ORD-9841");
        sample1.setCustomerName("Kartikeya");
        sample1.setPhone("+1 (555) 234-5678");
        sample1.setDeliveryAddress("742 Evergreen Terrace, Apt 4B");
        sample1.getItems().add(new OrderItem("F101", "Truffle Artisan Burger", 14.99, 2, "No onions, extra cheese"));
        sample1.getItems().add(new OrderItem("F104", "Chocolate Molten Lava Cake", 8.99, 1, "Cold spoon"));
        sample1.setSubtotal(38.97);
        sample1.setDeliveryFee(3.99);
        sample1.setTax(3.12);
        sample1.setDiscount(5.00);
        sample1.setTotal(41.08);
        sample1.setStatus(OrderStatus.PREPARING);
        sample1.setPaymentMethod("CARD");
        sample1.setPromoCode("WELCOME50");
        orderMap.put(sample1.getOrderId(), sample1);

        Order sample2 = new Order();
        sample2.setOrderId("ORD-9842");
        sample2.setCustomerName("Ayush Sail");
        sample2.setPhone("+1 (555) 987-6543");
        sample2.setDeliveryAddress("104 Ocean Drive, Suite 12");
        sample2.getItems().add(new OrderItem("F102", "Wood-Fired Pepperoni Pizza", 18.50, 1, "Well done crust"));
        sample2.getItems().add(new OrderItem("F106", "Sparkling Mango Passion Smoothie", 5.99, 2, "Less ice"));
        sample2.setSubtotal(30.48);
        sample2.setDeliveryFee(3.99);
        sample2.setTax(2.44);
        sample2.setDiscount(0.00);
        sample2.setTotal(36.91);
        sample2.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        sample2.setPaymentMethod("UPI");
        orderMap.put(sample2.getOrderId(), sample2);
    }

    public Coupon validateCoupon(String code) {
        if (code == null) return null;
        return couponMap.get(code.toUpperCase().trim());
    }

    public Order createOrder(Order order) {
        if (order.getOrderId() == null || order.getOrderId().isEmpty()) {
            order.setOrderId("ORD-" + (1000 + orderSeq.incrementAndGet()));
        }
        order.setTimestamp(System.currentTimeMillis());
        if (order.getStatus() == null) {
            order.setStatus(OrderStatus.RECEIVED);
        }

        // Calculate totals dynamically
        double subtotal = 0;
        for (OrderItem item : order.getItems()) {
            subtotal += item.getItemTotal();
        }
        order.setSubtotal(subtotal);

        double discount = 0.0;
        if (order.getPromoCode() != null && !order.getPromoCode().isEmpty()) {
            Coupon c = validateCoupon(order.getPromoCode());
            if (c != null) {
                discount = c.calculateDiscount(subtotal);
            }
        }
        order.setDiscount(discount);
        
        double delivery = subtotal > 50.0 ? 0.0 : 3.99;
        order.setDeliveryFee(delivery);
        
        double tax = Math.round((subtotal - discount) * 0.08 * 100.0) / 100.0;
        if (tax < 0) tax = 0;
        order.setTax(tax);

        double grandTotal = Math.round((subtotal - discount + delivery + tax) * 100.0) / 100.0;
        order.setTotal(grandTotal);

        orderMap.put(order.getOrderId(), order);
        return order;
    }

    public Order getOrder(String orderId) {
        return orderMap.get(orderId);
    }

    public List<Order> getAllOrders() {
        List<Order> list = new ArrayList<>(orderMap.values());
        list.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        return list;
    }

    public boolean updateStatus(String orderId, OrderStatus newStatus) {
        Order order = orderMap.get(orderId);
        if (order != null) {
            order.setStatus(newStatus);
            return true;
        }
        return false;
    }

    public String getAllOrdersAsJson() {
        StringBuilder sb = new StringBuilder("[");
        List<Order> list = getAllOrders();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i).toJson());
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public String getAnalyticsAsJson() {
        double totalRevenue = 0.0;
        int activeOrders = 0;
        int completedOrders = 0;

        for (Order o : orderMap.values()) {
            if (o.getStatus() != OrderStatus.CANCELLED) {
                totalRevenue += o.getTotal();
            }
            if (o.getStatus() == OrderStatus.RECEIVED || o.getStatus() == OrderStatus.PREPARING || o.getStatus() == OrderStatus.OUT_FOR_DELIVERY) {
                activeOrders++;
            } else if (o.getStatus() == OrderStatus.DELIVERED) {
                completedOrders++;
            }
        }

        return String.format(
            "{\"totalRevenue\":%.2f,\"totalOrders\":%d,\"activeOrders\":%d,\"completedOrders\":%d,\"avgOrderValue\":%.2f}",
            totalRevenue,
            orderMap.size(),
            activeOrders,
            completedOrders,
            orderMap.isEmpty() ? 0.0 : totalRevenue / Math.max(1, orderMap.size())
        );
    }
}
