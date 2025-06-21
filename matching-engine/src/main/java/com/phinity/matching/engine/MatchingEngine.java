package com.phinity.matching.engine;

import com.phinity.common.dto.models.PendingOrders;

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
}
