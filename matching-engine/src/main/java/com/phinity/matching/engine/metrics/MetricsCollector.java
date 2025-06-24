package com.phinity.matching.engine.metrics;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class MetricsCollector {
    private static final MetricsCollector INSTANCE = new MetricsCollector();
    
    // Technical Metrics
    private final LongAdder totalOrdersProcessed = new LongAdder();
    private final LongAdder totalTradesExecuted = new LongAdder();
    private final AtomicLong totalProcessingTimeNanos = new AtomicLong(0);
    private final ConcurrentHashMap<String, LongAdder> ordersBySymbol = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LongAdder> tradesBySymbol = new ConcurrentHashMap<>();
    
    // Business Metrics
    private final ConcurrentHashMap<String, AtomicLong> volumeBySymbol = new ConcurrentHashMap<>();
    private final LongAdder rejectedOrders = new LongAdder();
    private final ConcurrentHashMap<String, AtomicLong> engineUsage = new ConcurrentHashMap<>();
    
    public static MetricsCollector getInstance() { return INSTANCE; }
    
    // Technical Metrics
    public void recordOrderProcessed(String symbol, long processingTimeNanos) {
        totalOrdersProcessed.increment();
        totalProcessingTimeNanos.addAndGet(processingTimeNanos);
        ordersBySymbol.computeIfAbsent(symbol, k -> new LongAdder()).increment();
    }
    
    public void recordTradeExecuted(String symbol, BigDecimal volume) {
        totalTradesExecuted.increment();
        tradesBySymbol.computeIfAbsent(symbol, k -> new LongAdder()).increment();
        volumeBySymbol.computeIfAbsent(symbol, k -> new AtomicLong(0))
            .addAndGet(volume.multiply(BigDecimal.valueOf(1000000)).longValue()); // Store as micro-units
    }
    
    public void recordRejectedOrder() {
        rejectedOrders.increment();
    }
    
    public void recordEngineUsage(String engineType) {
        engineUsage.computeIfAbsent(engineType, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    // Getters
    public long getTotalOrdersProcessed() { return totalOrdersProcessed.sum(); }
    public long getTotalTradesExecuted() { return totalTradesExecuted.sum(); }
    public double getAverageProcessingTimeMs() {
        long total = totalOrdersProcessed.sum();
        return total > 0 ? (totalProcessingTimeNanos.get() / 1_000_000.0) / total : 0;
    }
    public long getOrdersPerSecond() { return totalOrdersProcessed.sum(); } // Will be calculated by monitoring system
    public ConcurrentHashMap<String, LongAdder> getOrdersBySymbol() { return ordersBySymbol; }
    public ConcurrentHashMap<String, LongAdder> getTradesBySymbol() { return tradesBySymbol; }
    public ConcurrentHashMap<String, AtomicLong> getVolumeBySymbol() { return volumeBySymbol; }
    public long getRejectedOrders() { return rejectedOrders.sum(); }
    public ConcurrentHashMap<String, AtomicLong> getEngineUsage() { return engineUsage; }
}