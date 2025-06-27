package com.phinity.matching.engine.core;

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
    private final String makerOrderId;
    private final String takerOrderId;

    public Trade(String tradeId, String symbol, String buyOrderId, String sellOrderId, 
                 BigDecimal price, BigDecimal quantity, String makerOrderId, String takerOrderId) {
        this.tradeId = tradeId;
        this.symbol = symbol;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.price = price;
        this.quantity = quantity;
        this.makerOrderId = makerOrderId;
        this.takerOrderId = takerOrderId;
        this.timestamp = LocalDateTime.now();
    }

    public String getTradeId() { return tradeId; }
    public String getSymbol() { return symbol; }
    public String getBuyOrderId() { return buyOrderId; }
    public String getSellOrderId() { return sellOrderId; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getQuantity() { return quantity; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getMakerOrderId() { return makerOrderId; }
    public String getTakerOrderId() { return takerOrderId; }
}