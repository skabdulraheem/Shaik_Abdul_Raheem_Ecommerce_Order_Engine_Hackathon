package com.ecommerce.models;

import java.util.concurrent.locks.ReentrantLock;

public class Product {
    private final String productId;
    private String name;
    private double price;
    private int stock;
    private int reservedStock;
    private final ReentrantLock lock = new ReentrantLock();

    public Product(String productId, String name, double price, int stock) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.reservedStock = 0;
    }

    // Thread-safe stock reservation
    public boolean reserveStock(int quantity) {
        lock.lock();
        try {
            int available = stock - reservedStock;
            if (available >= quantity) {
                reservedStock += quantity;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public void releaseReservation(int quantity) {
        lock.lock();
        try {
            reservedStock = Math.max(0, reservedStock - quantity);
        } finally {
            lock.unlock();
        }
    }

    public void confirmDeduction(int quantity) {
        lock.lock();
        try {
            stock -= quantity;
            reservedStock = Math.max(0, reservedStock - quantity);
        } finally {
            lock.unlock();
        }
    }

    public void restoreStock(int quantity) {
        lock.lock();
        try {
            stock += quantity;
        } finally {
            lock.unlock();
        }
    }

    public int getAvailableStock() {
        lock.lock();
        try {
            return stock - reservedStock;
        } finally {
            lock.unlock();
        }
    }

    public String getProductId() { return productId; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public int getReservedStock() { return reservedStock; }

    public void setName(String name) { this.name = name; }
    public void setPrice(double price) { this.price = price; }
    public void setStock(int stock) { this.stock = stock; }

    @Override
    public String toString() {
        return String.format("[%s] %s | Price: ₹%.2f | Stock: %d | Reserved: %d | Available: %d",
                productId, name, price, stock, reservedStock, getAvailableStock());
    }
}