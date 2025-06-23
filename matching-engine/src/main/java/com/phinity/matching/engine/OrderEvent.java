package com.phinity.matching.engine;

import com.phinity.common.dto.enums.Side;
import com.phinity.common.dto.models.PendingOrders;

import java.math.BigDecimal;

public class OrderEvent {
    private String orderId;
    private String symbol;
    private Side side;
    private BigDecimal price;
    private BigDecimal quantity;
    private com.phinity.common.dto.enums.OrderType orderType;
    private volatile boolean processed = false;
    private volatile Trade[] trades;
    private volatile int tradeCount;

    public void set(String orderId, String symbol, Side side, BigDecimal price, BigDecimal quantity, 
                    com.phinity.common.dto.enums.OrderType orderType) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.side = side;
        this.price = price;
        this.quantity = quantity;
        this.orderType = orderType;
        this.processed = false;
        this.tradeCount = 0;
    }
    
    // Backward compatibility method
    public void set(String orderId, String symbol, Side side, BigDecimal price, BigDecimal quantity) {
        set(orderId, symbol, side, price, quantity, com.phinity.common.dto.enums.OrderType.LIMIT);
    }

    public PendingOrders toOrder() {
        PendingOrders order = new PendingOrders(orderId, symbol, side, price, quantity);
        order.setOrderType(orderType);
        return order;
    }

    public void setResult(Trade[] trades, int count) {
        this.trades = trades;
        this.tradeCount = count;
        this.processed = true;
    }

    public Trade[] getTrades() { return trades; }
    public int getTradeCount() { return tradeCount; }
    public boolean isProcessed() { return processed; }
    public String getSymbol() { return symbol; }
}