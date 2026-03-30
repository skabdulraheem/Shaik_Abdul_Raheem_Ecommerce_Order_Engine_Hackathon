package com.ecommerce.services;

import com.ecommerce.models.Cart;
import com.ecommerce.models.CartItem;

import java.util.HashMap;
import java.util.Map;

/**
 * Task 9: Discount & Coupon Engine
 */
public class DiscountService {

    private static final Map<String, Double> COUPON_FLAT = new HashMap<>();
    private static final Map<String, Double> COUPON_PCT = new HashMap<>();

    static {
        COUPON_FLAT.put("FLAT200", 200.0);
        COUPON_PCT.put("SAVE10", 0.10);
    }

    /**
     * Calculate total discount for cart.
     * Rules:
     *  - Total > 1000 => 10% auto discount
     *  - Qty > 3 same product => extra 5%
     *  - Coupon codes applied on top
     */
    public double calculateDiscount(Cart cart, String couponCode) {
        double gross = cart.getGrossTotal();
        double discount = 0;

        // Rule 1: Total > 1000 → 10%
        if (gross > 1000) {
            double d = gross * 0.10;
            discount += d;
            System.out.printf("  Auto discount (total > ₹1000): -₹%.2f%n", d);
        }

        // Rule 2: Qty > 3 for same product → extra 5% per such product
        for (CartItem item : cart.getItems()) {
            if (item.getQuantity() > 3) {
                double d = item.getSubtotal() * 0.05;
                discount += d;
                System.out.printf("  Bulk discount (qty>3 for %s): -₹%.2f%n", item.getProductId(), d);
            }
        }

        // Rule 3: Coupon
        if (couponCode != null && !couponCode.isBlank()) {
            String code = couponCode.trim().toUpperCase();
            if (COUPON_PCT.containsKey(code)) {
                double d = gross * COUPON_PCT.get(code);
                discount += d;
                System.out.printf("  Coupon %s (%.0f%%): -₹%.2f%n", code,
                        COUPON_PCT.get(code) * 100, d);
            } else if (COUPON_FLAT.containsKey(code)) {
                double d = COUPON_FLAT.get(code);
                discount += d;
                System.out.printf("  Coupon %s (flat): -₹%.2f%n", code, d);
            } else {
                System.out.println("  WARNING: Invalid coupon code '" + couponCode + "'.");
            }
        }

        // Avoid discount > gross
        discount = Math.min(discount, gross);
        return discount;
    }

    public boolean applyCoupon(Cart cart, String couponCode) {
        String code = couponCode.trim().toUpperCase();
        if (!COUPON_PCT.containsKey(code) && !COUPON_FLAT.containsKey(code)) {
            System.out.println("  ERROR: Invalid coupon code.");
            return false;
        }
        double discount = calculateDiscount(cart, code);
        cart.setAppliedCoupon(code);
        cart.setDiscountAmount(discount);
        System.out.printf("  Coupon applied! Discount: ₹%.2f%n", discount);
        return true;
    }
}