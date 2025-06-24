package com.phinity.matching.engine.events;

public class OrderRejectedEvent extends OrderEvent {
    private final String reason;
    
    public OrderRejectedEvent(String orderId, String symbol, String reason) {
        super(orderId, symbol);
        this.reason = reason;
    }
    
    @Override
    public String getEventType() { return "ORDER_REJECTED"; }
    public String getReason() { return reason; }
}