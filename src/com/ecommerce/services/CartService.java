package com.ecommerce.services;

import com.ecommerce.models.Cart;
import com.ecommerce.models.CartItem;
import com.ecommerce.models.Product;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class CartService {
    private final Map<String, Cart> carts = new ConcurrentHashMap<>();
    private final ProductService productService;
    private final AuditService auditService;

    public CartService(ProductService productService, AuditService auditService) {
        this.productService = productService;
        this.auditService = auditService;
    }

    private Cart getOrCreateCart(String userId) {
        return carts.computeIfAbsent(userId, Cart::new);
    }

   
    public boolean addToCart(String userId, String productId, int quantity) {
        Product p = productService.getProduct(productId);
        if (p == null) {
            System.out.println("  ERROR: Product not found.");
            return false;
        }
        if (quantity <= 0) {
            System.out.println("  ERROR: Quantity must be positive.");
            return false;
        }

        
        if (!p.reserveStock(quantity)) {
            System.out.printf("  ERROR: Insufficient stock. Available: %d%n", p.getAvailableStock());
            return false;
        }

        Cart cart = getOrCreateCart(userId);
       
        CartItem existing = cart.getItem(productId);
        if (existing != null) {
           
        }
        cart.addItem(new CartItem(productId, quantity, p.getPrice()));
        auditService.log(userId + " added " + productId + " qty=" + quantity + " to cart");

        
        productService.scheduleReservationExpiry(userId, productId, quantity, 30);

        System.out.printf("  Added %d x '%s' to cart.%n", quantity, p.getName());
        return true;
    }


    public boolean removeFromCart(String userId, String productId) {
        Cart cart = carts.get(userId);
        if (cart == null || cart.getItem(productId) == null) {
            System.out.println("  ERROR: Item not in cart.");
            return false;
        }
        CartItem item = cart.getItem(productId);
        Product p = productService.getProduct(productId);
        if (p != null) p.releaseReservation(item.getQuantity());
        cart.removeItem(productId);
        auditService.log(userId + " removed " + productId + " from cart");
        System.out.println("  Item removed and stock released.");
        return true;
    }

   
    public boolean updateQuantity(String userId, String productId, int newQty) {
        Cart cart = carts.get(userId);
        if (cart == null || cart.getItem(productId) == null) {
            System.out.println("  ERROR: Item not in cart.");
            return false;
        }
        CartItem item = cart.getItem(productId);
        int oldQty = item.getQuantity();
        Product p = productService.getProduct(productId);
        if (p == null) return false;

        if (newQty <= 0) {
            return removeFromCart(userId, productId);
        }

        int delta = newQty - oldQty;
        if (delta > 0) {
            if (!p.reserveStock(delta)) {
                System.out.printf("  ERROR: Only %d more units available.%n", p.getAvailableStock());
                return false;
            }
        } else {
            p.releaseReservation(-delta);
        }
        cart.updateQuantity(productId, newQty);
        auditService.log(userId + " updated " + productId + " qty: " + oldQty + " -> " + newQty);
        System.out.println("  Quantity updated.");
        return true;
    }

    public void viewCart(String userId) {
        Cart cart = carts.get(userId);
        if (cart == null || cart.isEmpty()) {
            System.out.println("  Cart is empty.");
            return;
        }
        System.out.println(cart);
    }

    public Cart getCart(String userId) {
        return carts.get(userId);
    }

    
    public void clearCart(String userId) {
        Cart cart = carts.get(userId);
        if (cart == null) return;
        for (CartItem item : cart.getItems()) {
            Product p = productService.getProduct(item.getProductId());
            if (p != null) p.releaseReservation(item.getQuantity());
        }
        cart.clear();
    }
}
