package com.phinity.matching.engine;

import com.phinity.common.dto.models.PendingOrders;
import java.time.LocalDateTime;
import java.util.List;

public class OrderBookSnapshot {
    private final String symbol;
    private final LocalDateTime timestamp;
    private final List<PendingOrders> bids;
    private final List<PendingOrders> asks;
    private final long lastTradeId;
    
    public OrderBookSnapshot(String symbol, List<PendingOrders> bids, List<PendingOrders> asks, long lastTradeId) {
        this.symbol = symbol;
        this.timestamp = LocalDateTime.now();
        this.bids = bids;
        this.asks = asks;
        this.lastTradeId = lastTradeId;
    }
    
    public String getSymbol() { return symbol; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public List<PendingOrders> getBids() { return bids; }
    public List<PendingOrders> getAsks() { return asks; }
    public long getLastTradeId() { return lastTradeId; }
}