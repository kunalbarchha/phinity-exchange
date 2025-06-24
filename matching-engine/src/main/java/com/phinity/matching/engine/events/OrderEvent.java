package com.phinity.matching.engine.events;

import com.phinity.common.dto.enums.Side;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public abstract class OrderEvent {
    private final String eventId;
    private final String orderId;
    private final String symbol;
    private final LocalDateTime timestamp;
    
    protected OrderEvent(String orderId, String symbol) {
        this.eventId = UUID.randomUUID().toString();
        this.orderId = orderId;
        this.symbol = symbol;
        this.timestamp = LocalDateTime.now();
    }
    
    public String getEventId() { return eventId; }
    public String getOrderId() { return orderId; }
    public String getSymbol() { return symbol; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public abstract String getEventType();
}