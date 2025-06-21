package com.phinity.matching.engine;

import com.phinity.common.dto.models.PendingOrders;

import java.util.*;
import java.util.concurrent.*;

public class EnginePool {
    private final Map<String, MatchingEngine> engines = new ConcurrentHashMap<>();
    private final ExecutorService executorService;
    private final Set<String> highVolumePairs;
    
    public EnginePool(int poolSize, Set<String> highVolumePairs) {
        this.executorService = Executors.newFixedThreadPool(poolSize);
        this.highVolumePairs = new HashSet<>(highVolumePairs);
    }

    public CompletableFuture<List<Trade>> processOrder(PendingOrders order) {
        return CompletableFuture.supplyAsync(() -> {
            MatchingEngine engine = getOrCreateEngine(order.getSymbol());
            return engine.match(order);
        }, executorService);
    }

    private MatchingEngine getOrCreateEngine(String symbol) {
        return engines.computeIfAbsent(symbol, MatchingEngine::new);
    }

    public Set<String> getActivePairs() {
        return new HashSet<>(engines.keySet());
    }

    public MatchingEngine getEngine(String symbol) {
        return engines.get(symbol);
    }

    public boolean isHighVolumePair(String symbol) {
        return highVolumePairs.contains(symbol);
    }

    public void shutdown() {
        executorService.shutdown();
    }
}