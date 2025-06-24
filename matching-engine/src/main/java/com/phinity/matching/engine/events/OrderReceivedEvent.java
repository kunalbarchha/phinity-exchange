package com.phinity.matching.engine.events;

import com.phinity.common.dto.enums.Side;
import java.math.BigDecimal;

public class OrderReceivedEvent extends OrderEvent {
    private final Side side;
    private final BigDecimal price;
    private final BigDecimal quantity;
    
    public OrderReceivedEvent(String orderId, String symbol, Side side, BigDecimal price, BigDecimal quantity) {
        super(orderId, symbol);
        this.side = side;
        this.price = price;
        this.quantity = quantity;
    }
    
    @Override
    public String getEventType() { return "ORDER_RECEIVED"; }
    public Side getSide() { return side; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getQuantity() { return quantity; }
}