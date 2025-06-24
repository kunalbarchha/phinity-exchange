package com.phinity.matching.engine;

import com.phinity.common.dto.models.PendingOrders;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MatchingEngine {
    private final String symbol;
    private final Book book;
    private final AtomicLong processedOrders = new AtomicLong(0);

    public MatchingEngine(String symbol) {
        this.symbol = symbol;
        this.book = new Book();
    }
    
    public void setEventPublisher(EventPublisher eventPublisher) {
        this.book.setEventPublisher(eventPublisher);
    }
    
    public void setEventStore(EventStore eventStore) {
        this.book.setEventStore(eventStore);
    }

    public List<Trade> match(PendingOrders order) {
        processedOrders.incrementAndGet();
        return book.matchOrder(order);
    }

    public String getSymbol() {
        return symbol;
    }

    public long getProcessedOrdersCount() {
        return processedOrders.get();
    }

    public Book getOrderBook(){
        return book;
    }
}
