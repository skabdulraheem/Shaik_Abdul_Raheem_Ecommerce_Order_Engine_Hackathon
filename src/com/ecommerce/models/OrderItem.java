package com.ecommerce.models;

public class OrderItem {
    private final String productId;
    private final String productName;
    private int quantity;
    private int returnedQuantity;
    private final double unitPrice;

    public OrderItem(String productId, String productName, int quantity, double unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.returnedQuantity = 0;
    }

    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public int getReturnedQuantity() { return returnedQuantity; }
    public double getUnitPrice() { return unitPrice; }
    public double getSubtotal() { return unitPrice * (quantity - returnedQuantity); }

    public boolean returnQty(int qty) {
        if (qty <= 0 || qty > (quantity - returnedQuantity)) return false;
        returnedQuantity += qty;
        return true;
    }

    @Override
    public String toString() {
        return String.format("  [%s] %s | Qty: %d | Returned: %d | Unit: ₹%.2f | Subtotal: ₹%.2f",
                productId, productName, quantity, returnedQuantity, unitPrice, getSubtotal());
    }
}