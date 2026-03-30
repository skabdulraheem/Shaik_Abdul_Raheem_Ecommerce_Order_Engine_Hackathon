package com.ecommerce.services;

import com.ecommerce.models.Event;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Task 14: Event-Driven System
 * Events execute in order; a failure stops subsequent events.
 */
public class EventService {
    private final Queue<Event> eventQueue = new LinkedList<>();
    private final AuditService auditService;

    public EventService(AuditService auditService) {
        this.auditService = auditService;
    }

    public void publish(Event.Type type, String payload) {
        Event event = new Event(type, payload);
        eventQueue.add(event);
        auditService.log("EVENT QUEUED: " + event);
    }

    /** Process all pending events in order; stops on first failure */
    public void processAll() {
        System.out.println("  ---- Processing Event Queue ----");
        if (eventQueue.isEmpty()) {
            System.out.println("  No events to process.");
            return;
        }
        while (!eventQueue.isEmpty()) {
            Event event = eventQueue.poll();
            boolean success = process(event);
            if (!success) {
                System.out.println("  ERROR: Event processing failed for " + event + ". Halting queue.");
                auditService.log("EVENT FAILED: " + event + " - queue halted");
                eventQueue.clear();
                return;
            }
        }
        System.out.println("  All events processed successfully.");
    }

    private boolean process(Event event) {
        System.out.println("  Processing: " + event);
        auditService.log("EVENT PROCESSED: " + event);
        // Simulate processing — each event type can hook into actual logic
        return true;
    }

    public int pendingCount() {
        return eventQueue.size();
    }
}