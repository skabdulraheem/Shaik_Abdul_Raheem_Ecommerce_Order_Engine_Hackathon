package com.ecommerce.models;

public class CartItem {
    private final String productId;
    private int quantity;
    private final double priceAtAdd;

    public CartItem(String productId, int quantity, double priceAtAdd) {
        this.productId = productId;
        this.quantity = quantity;
        this.priceAtAdd = priceAtAdd;
    }

    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public double getPriceAtAdd() { return priceAtAdd; }
    public double getSubtotal() { return priceAtAdd * quantity; }

    public void setQuantity(int quantity) { this.quantity = quantity; }

    @Override
    public String toString() {
        return String.format("  ProductID: %s | Qty: %d | Unit Price: ₹%.2f | Subtotal: ₹%.2f",
                productId, quantity, priceAtAdd, getSubtotal());
    }
}