package com.phinity.matching.engine.core;

import com.phinity.common.dto.models.PendingOrders;
import com.phinity.matching.engine.service.EventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MatchingEngine {
    private final String symbol;
    private final OrderBook orderBook;
    private final AtomicLong processedOrders = new AtomicLong(0);

    public MatchingEngine(String symbol) {
        this.symbol = symbol;
        this.orderBook = new OrderBook();
    }
    
    public void setEventPublisher(EventPublisher eventPublisher) {
        this.orderBook.setEventPublisher(eventPublisher);
    }

    public List<Trade> match(PendingOrders order) {
        processedOrders.incrementAndGet();
        return orderBook.matchOrder(order);
    }

    public String getSymbol() {
        return symbol;
    }

    public long getProcessedOrdersCount() {
        return processedOrders.get();
    }

    public OrderBook getOrderBook(){
        return orderBook;
    }
    
    /**
     * Modifies an existing order in the order book
     * @param orderId The ID of the order to modify
     * @param newPrice The new price (null to keep existing price)
     * @param newQuantity The new quantity (null to keep existing quantity)
     * @return The modified order if successful, null if order not found
     */
    public PendingOrders modifyOrder(String orderId, BigDecimal newPrice, BigDecimal newQuantity) {
        return orderBook.modifyOrder(orderId, newPrice, newQuantity);
    }
}