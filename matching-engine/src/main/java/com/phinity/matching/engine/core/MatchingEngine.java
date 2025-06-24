package com.phinity.matching.engine.core;

import com.phinity.common.dto.models.PendingOrders;
import com.phinity.matching.engine.service.EventPublisher;
import com.phinity.matching.engine.service.EventStore;

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
    
    public void setEventStore(EventStore eventStore) {
        this.orderBook.setEventStore(eventStore);
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
}