package com.phinity.matching.engine.manager;

import com.phinity.common.dto.enums.OrderType;
import com.phinity.common.dto.enums.Side;
import com.phinity.common.dto.models.PendingOrders;
import com.phinity.matching.engine.EngineManager;
import com.phinity.matching.engine.OptimizedDisruptorEngine;
import com.phinity.matching.engine.config.PairConfigurationManager;
import com.phinity.matching.engine.core.MatchingEngine;
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

    public CompletableFuture<List<Trade>> processOrder(String orderId, String symbol, Side side, BigDecimal price, BigDecimal quantity, OrderType orderType) {
        long startTime = System.nanoTime();

        if (configManager.isHighVolumePair(symbol)) {
            MetricsCollector.getInstance().recordEngineUsage("disruptor");
            OptimizedDisruptorEngine engine = disruptorEngines.computeIfAbsent(symbol, s -> {
                OptimizedDisruptorEngine newEngine = new OptimizedDisruptorEngine(s);
                if (eventPublisher != null) {
                    newEngine.setEventPublisher(eventPublisher);
                }
                return newEngine;
            });

            return engine.processOrder(orderId, symbol, side, price, quantity, orderType)
                    .whenComplete((trades, ex) -> {
                        long processingTime = System.nanoTime() - startTime;
                        MetricsCollector.getInstance().recordOrderProcessed(symbol, processingTime);
                        if (trades != null) {
                            trades.forEach(trade -> MetricsCollector.getInstance().recordTradeExecuted(symbol, trade.getQuantity()));
                        }
                    });
        } else {
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

    public CompletableFuture<List<Trade>> processOrder(String orderId, String symbol, Side side, BigDecimal price, BigDecimal quantity) {
        return processOrder(orderId, symbol, side, price, quantity, OrderType.LIMIT);
    }

    public void configureHighVolumePair(String symbol, boolean isHighVolume) {
        if (isHighVolume) {
            configManager.addHighVolumePair(symbol);
            disruptorEngines.computeIfAbsent(symbol, s -> {
                OptimizedDisruptorEngine newEngine = new OptimizedDisruptorEngine(s);
                if (eventPublisher != null) {
                    newEngine.setEventPublisher(eventPublisher);
                }
                return newEngine;
            });
        } else {
            configManager.removeHighVolumePair(symbol);
            OptimizedDisruptorEngine engine = disruptorEngines.remove(symbol);
            if (engine != null) {
                engine.shutdown();
            }
        }
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

    public PendingOrders modifyOrder(String orderId, String symbol, BigDecimal newPrice, BigDecimal newQuantity) {
        OrderBook orderBook = getOrderBook(symbol);
        if (orderBook != null) {
            return orderBook.modifyOrder(orderId, newPrice, newQuantity);
        }
        return null;
    }

    public void shutdown() {
        standardManager.shutdown();
        disruptorEngines.values().forEach(OptimizedDisruptorEngine::shutdown);
        disruptorEngines.clear();
    }
}
