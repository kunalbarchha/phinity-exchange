package com.phinity.matching.engine;

import com.phinity.common.dto.models.PendingOrders;
import com.phinity.matching.engine.config.PairConfigurationManager;
import com.phinity.matching.engine.core.MatchingEngine;
import com.phinity.matching.engine.core.OrderBook;
import com.phinity.matching.engine.core.Trade;
import com.phinity.matching.engine.service.EventPublisher;
import com.phinity.matching.engine.service.EventStore;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class EngineManager {
    private final EnginePool highVolumePool;
    private final EnginePool standardPool;
    private final PairConfigurationManager configManager;
    private EventPublisher eventPublisher;
    
    public EngineManager() {
        this.configManager = new PairConfigurationManager();
        this.highVolumePool = new EnginePool(50, Collections.emptySet());
        this.standardPool = new EnginePool(100, Collections.emptySet());
    }
    
    public void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.highVolumePool.setEventPublisher(eventPublisher);
        this.standardPool.setEventPublisher(eventPublisher);
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
    
    public OrderBook getOrderBook(String symbol) {
        MatchingEngine engine = getEngine(symbol);
        return engine != null ? engine.getOrderBook() : null;
    }
    
    public void setEventStore(EventStore eventStore) {
        this.highVolumePool.setEventStore(eventStore);
        this.standardPool.setEventStore(eventStore);
    }

    public void shutdown() {
        highVolumePool.shutdown();
        standardPool.shutdown();
    }
}