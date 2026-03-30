package com.ecommerce.models;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class Cart {
    private final String userId;
    private final Map<String, CartItem> items = new LinkedHashMap<>();
    private String appliedCoupon;
    private double discountAmount;

    public Cart(String userId) {
        this.userId = userId;
    }

    public String getUserId() { return userId; }
    public String getAppliedCoupon() { return appliedCoupon; }
    public double getDiscountAmount() { return discountAmount; }

    public void setAppliedCoupon(String coupon) { this.appliedCoupon = coupon; }
    public void setDiscountAmount(double amount) { this.discountAmount = amount; }

    public void addItem(CartItem item) {
        String pid = item.getProductId();
        if (items.containsKey(pid)) {
            items.get(pid).setQuantity(items.get(pid).getQuantity() + item.getQuantity());
        } else {
            items.put(pid, item);
        }
    }

    public boolean removeItem(String productId) {
        return items.remove(productId) != null;
    }

    public boolean updateQuantity(String productId, int newQty) {
        if (!items.containsKey(productId)) return false;
        if (newQty <= 0) {
            items.remove(productId);
        } else {
            items.get(productId).setQuantity(newQty);
        }
        return true;
    }

    public Collection<CartItem> getItems() { return items.values(); }

    public CartItem getItem(String productId) { return items.get(productId); }

    public boolean isEmpty() { return items.isEmpty(); }

    public void clear() {
        items.clear();
        appliedCoupon = null;
        discountAmount = 0;
    }

    public double getGrossTotal() {
        return items.values().stream().mapToDouble(CartItem::getSubtotal).sum();
    }

    public double getFinalTotal() {
        return Math.max(0, getGrossTotal() - discountAmount);
    }

    @Override
    public String toString() {
        if (items.isEmpty()) return "  Cart is empty.";
        StringBuilder sb = new StringBuilder();
        sb.append("  Cart for User: ").append(userId).append("\n");
        items.values().forEach(i -> sb.append(i).append("\n"));
        sb.append(String.format("  Gross Total : ₹%.2f%n", getGrossTotal()));
        if (discountAmount > 0) {
            sb.append(String.format("  Coupon (%s): -₹%.2f%n", appliedCoupon, discountAmount));
            sb.append(String.format("  Final Total : ₹%.2f%n", getFinalTotal()));
        }
        return sb.toString();
    }
}