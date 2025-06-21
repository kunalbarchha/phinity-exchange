package com.phinity.matching.engine;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Trade {
    private final String tradeId;
    private final String symbol;
    private final String buyOrderId;
    private final String sellOrderId;
    private final BigDecimal price;
    private final BigDecimal quantity;
    private final LocalDateTime timestamp;

    public Trade(String tradeId, String symbol, String buyOrderId, String sellOrderId, 
                 BigDecimal price, BigDecimal quantity) {
        this.tradeId = tradeId;
        this.symbol = symbol;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = LocalDateTime.now();
    }

    public String getTradeId() { return tradeId; }
    public String getSymbol() { return symbol; }
    public String getBuyOrderId() { return buyOrderId; }
    public String getSellOrderId() { return sellOrderId; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getQuantity() { return quantity; }
    public LocalDateTime getTimestamp() { return timestamp; }
}