package com.phinity.matching.engine;

import com.phinity.common.dto.models.PendingOrders;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class EngineManager {
    private final EnginePool highVolumePool;
    private final EnginePool standardPool;
    private final PairConfigurationManager configManager;
    
    public EngineManager() {
        this.configManager = new PairConfigurationManager();
        this.highVolumePool = new EnginePool(50, Collections.emptySet());
        this.standardPool = new EnginePool(100, Collections.emptySet());
    }

    public CompletableFuture<List<Trade>> processOrder(PendingOrders order) {
        if (configManager.isHighVolumePair(order.getSymbol())) {
            return highVolumePool.processOrder(order);
        } else {
            return standardPool.processOrder(order);
        }
    }

    public void configureHighVolumePair(String symbol, boolean isHighVolume) {
        if (isHighVolume) {
            configManager.addHighVolumePair(symbol);
        } else {
            configManager.removeHighVolumePair(symbol);
        }
    }

    public Set<String> getAllActivePairs() {
        Set<String> allPairs = new HashSet<>();
        allPairs.addAll(highVolumePool.getActivePairs());
        allPairs.addAll(standardPool.getActivePairs());
        return allPairs;
    }

    public MatchingEngine getEngine(String symbol) {
        if (configManager.isHighVolumePair(symbol)) {
            return highVolumePool.getEngine(symbol);
        } else {
            return standardPool.getEngine(symbol);
        }
    }

    public void shutdown() {
        highVolumePool.shutdown();
        standardPool.shutdown();
    }
}