package com.phinity.matching.engine.events;

import java.math.BigDecimal;

public class OrderMatchedEvent extends OrderEvent {
    private final String tradeId;
    private final BigDecimal matchedQuantity;
    private final BigDecimal matchedPrice;
    
    public OrderMatchedEvent(String orderId, String symbol, String tradeId, BigDecimal matchedQuantity, BigDecimal matchedPrice) {
        super(orderId, symbol);
        this.tradeId = tradeId;
        this.matchedQuantity = matchedQuantity;
        this.matchedPrice = matchedPrice;
    }
    
    @Override
    public String getEventType() { return "ORDER_MATCHED"; }
    public String getTradeId() { return tradeId; }
    public BigDecimal getMatchedQuantity() { return matchedQuantity; }
    public BigDecimal getMatchedPrice() { return matchedPrice; }
}