package com.ecommerce.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Order {
    private final String orderId;
    private final String userId;
    private final List<OrderItem> items;
    private final double grossTotal;
    private final double discountAmount;
    private double finalTotal;
    private OrderStatus status;
    private final LocalDateTime createdAt;
    private String idempotencyKey;

    public Order(String orderId, String userId, List<OrderItem> items,
                 double grossTotal, double discountAmount) {
        this.orderId = orderId;
        this.userId = userId;
        this.items = new ArrayList<>(items);
        this.grossTotal = grossTotal;
        this.discountAmount = discountAmount;
        this.finalTotal = Math.max(0, grossTotal - discountAmount);
        this.status = OrderStatus.CREATED;
        this.createdAt = LocalDateTime.now();
    }

    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
    public double getGrossTotal() { return grossTotal; }
    public double getDiscountAmount() { return discountAmount; }
    public double getFinalTotal() { return finalTotal; }
    public OrderStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getIdempotencyKey() { return idempotencyKey; }

    public void setStatus(OrderStatus status) { this.status = status; }
    public void setIdempotencyKey(String key) { this.idempotencyKey = key; }
    public void setFinalTotal(double total) { this.finalTotal = total; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Order ID : %s%n", orderId));
        sb.append(String.format("User     : %s%n", userId));
        sb.append(String.format("Status   : %s%n", status));
        sb.append(String.format("Created  : %s%n", createdAt));
        sb.append("Items:\n");
        items.forEach(i -> sb.append(i).append("\n"));
        sb.append(String.format("Gross    : ₹%.2f%n", grossTotal));
        if (discountAmount > 0) sb.append(String.format("Discount : -₹%.2f%n", discountAmount));
        sb.append(String.format("Total    : ₹%.2f%n", finalTotal));
        return sb.toString();
    }
}