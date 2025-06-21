package com.phinity.matching.engine;

import com.phinity.common.dto.enums.Side;
import com.phinity.common.dto.models.PendingOrders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class HybridEngineManager {
    private final EngineManager standardManager;
    private final ConcurrentHashMap<String, OptimizedDisruptorEngine> disruptorEngines;
    private final PairConfigurationManager configManager;

    public HybridEngineManager() {
        this.standardManager = new EngineManager();
        this.disruptorEngines = new ConcurrentHashMap<>();
        this.configManager = new PairConfigurationManager();
    }

    public CompletableFuture<List<Trade>> processOrder(String orderId, String symbol,
                                                       Side side, BigDecimal price, BigDecimal quantity) {
        if (configManager.isHighVolumePair(symbol)) {
            // Use Disruptor for high-volume pairs
            OptimizedDisruptorEngine engine = disruptorEngines.computeIfAbsent(symbol, OptimizedDisruptorEngine::new);
            return CompletableFuture.completedFuture(engine.processOrderSync(orderId, symbol, side, price, quantity));
        } else {
            // Use standard engine for regular pairs
            PendingOrders order = new PendingOrders(orderId, symbol, side, price, quantity);
            return standardManager.processOrder(order);
        }
    }

    public void configureHighVolumePair(String symbol, boolean isHighVolume) {
        if (isHighVolume) {
            configManager.addHighVolumePair(symbol);
            // Pre-create Disruptor engine
            disruptorEngines.computeIfAbsent(symbol, OptimizedDisruptorEngine::new);
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

    public void shutdown() {
        standardManager.shutdown();
        disruptorEngines.values().forEach(OptimizedDisruptorEngine::shutdown);
        disruptorEngines.clear();
    }
}