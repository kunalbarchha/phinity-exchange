package com.phinity.matching.engine.manager;

import com.phinity.common.dto.enums.Side;
import com.phinity.common.dto.models.PendingOrders;
import com.phinity.matching.engine.EngineManager;
import com.phinity.matching.engine.OptimizedDisruptorEngine;
import com.phinity.matching.engine.config.PairConfigurationManager;
import com.phinity.matching.engine.core.OrderBook;
import com.phinity.matching.engine.core.Trade;
import com.phinity.matching.engine.metrics.MetricsCollector;
import com.phinity.matching.engine.service.EventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class HybridEngineManager {
    private final EngineManager standardManager;
    private final ConcurrentHashMap<String, OptimizedDisruptorEngine> disruptorEngines;
    private final PairConfigurationManager configManager;
    private EventPublisher eventPublisher;

    public HybridEngineManager() {
        this.standardManager = new EngineManager();
        this.disruptorEngines = new ConcurrentHashMap<>();
        this.configManager = new PairConfigurationManager();
    }
    
    public void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.standardManager.setEventPublisher(eventPublisher);
        this.disruptorEngines.values().forEach(engine -> engine.setEventPublisher(eventPublisher));
    }

    public CompletableFuture<List<Trade>> processOrder(String orderId, String symbol,
                                                       Side side, BigDecimal price, BigDecimal quantity, 
                                                       com.phinity.common.dto.enums.OrderType orderType) {
        long startTime = System.nanoTime();
        
        if (configManager.isHighVolumePair(symbol)) {
            // Use Disruptor for high-volume pairs
            MetricsCollector.getInstance().recordEngineUsage("disruptor");
            OptimizedDisruptorEngine engine = disruptorEngines.computeIfAbsent(symbol, OptimizedDisruptorEngine::new);
            List<Trade> trades = engine.processOrderSync(orderId, symbol, side, price, quantity, orderType);
            
            long processingTime = System.nanoTime() - startTime;
            MetricsCollector.getInstance().recordOrderProcessed(symbol, processingTime);
            trades.forEach(trade -> MetricsCollector.getInstance().recordTradeExecuted(symbol, trade.getQuantity()));
            
            return CompletableFuture.completedFuture(trades);
        } else {
            // Use standard engine for regular pairs
            MetricsCollector.getInstance().recordEngineUsage("standard");
            PendingOrders order = new PendingOrders(orderId, symbol, side, price, quantity);
            order.setOrderType(orderType);
            
            return standardManager.processOrder(order).thenApply(trades -> {
                long processingTime = System.nanoTime() - startTime;
                MetricsCollector.getInstance().recordOrderProcessed(symbol, processingTime);
                trades.forEach(trade -> MetricsCollector.getInstance().recordTradeExecuted(symbol, trade.getQuantity()));
                return trades;
            });
        }
    }
    
    // Backward compatibility method
    public CompletableFuture<List<Trade>> processOrder(String orderId, String symbol,
                                                       Side side, BigDecimal price, BigDecimal quantity) {
        return processOrder(orderId, symbol, side, price, quantity, com.phinity.common.dto.enums.OrderType.LIMIT);
    }

    public void configureHighVolumePair(String symbol, boolean isHighVolume) {
        if (isHighVolume) {
            configManager.addHighVolumePair(symbol);
            // Pre-create Disruptor engine
            OptimizedDisruptorEngine engine = disruptorEngines.computeIfAbsent(symbol, OptimizedDisruptorEngine::new);
            if (eventPublisher != null) {
                engine.setEventPublisher(eventPublisher);
            }
        } else {
            configManager.removeHighVolumePair(symbol);
            // Shutdown and remove Disruptor engine
            OptimizedDisruptorEngine engine = disruptorEngines.remove(symbol);
            if (engine != null) {
                engine.shutdown();
            }
        }
        
        // Also configure standard manager
        standardManager.configureHighVolumePair(symbol, isHighVolume);
    }

    public Set<String> getAllActivePairs() {
        Set<String> allPairs = standardManager.getAllActivePairs();
        allPairs.addAll(disruptorEngines.keySet());
        return allPairs;
    }
    
    public OrderBook getOrderBook(String symbol) {
        if (configManager.isHighVolumePair(symbol)) {
            OptimizedDisruptorEngine engine = disruptorEngines.get(symbol);
            return engine != null ? engine.getOrderBook() : null;
        } else {
            return standardManager.getOrderBook(symbol);
        }
    }

    public void shutdown() {
        standardManager.shutdown();
        disruptorEngines.values().forEach(OptimizedDisruptorEngine::shutdown);
        disruptorEngines.clear();
    }
}