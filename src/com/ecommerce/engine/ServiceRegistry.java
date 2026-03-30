package com.ecommerce.engine;

import com.ecommerce.services.*;

/**
 * Task 20: Microservice Simulation
 * Loosely couples all services — each service is independent and
 * communicates only through well-defined interfaces, not direct field access.
 * This class acts as the service registry / dependency injector.
 */
public class ServiceRegistry {

    private final AuditService auditService;
    private final ProductService productService;
    private final CartService cartService;
    private final DiscountService discountService;
    private final PaymentService paymentService;
    private final EventService eventService;
    private final FraudDetectionService fraudDetectionService;
    private final OrderService orderService;
    private final ConcurrencyEngine concurrencyEngine;

    public ServiceRegistry() {
        // Build in dependency order
        this.auditService          = new AuditService();
        this.productService        = new ProductService(auditService);
        this.cartService           = new CartService(productService, auditService);
        this.discountService       = new DiscountService();
        this.paymentService        = new PaymentService();
        this.eventService          = new EventService(auditService);
        this.fraudDetectionService = new FraudDetectionService(auditService);
        this.orderService          = new OrderService(
                productService, cartService, paymentService,
                auditService, eventService, fraudDetectionService);
        this.concurrencyEngine     = new ConcurrencyEngine(productService, auditService);
    }

    public AuditService audit()         { return auditService; }
    public ProductService product()     { return productService; }
    public CartService cart()           { return cartService; }
    public DiscountService discount()   { return discountService; }
    public PaymentService payment()     { return paymentService; }
    public EventService event()         { return eventService; }
    public FraudDetectionService fraud(){ return fraudDetectionService; }
    public OrderService order()         { return orderService; }
    public ConcurrencyEngine concurrency() { return concurrencyEngine; }

    public void shutdown() {
        productService.shutdown();
        System.out.println("  Services shut down.");
    }
}