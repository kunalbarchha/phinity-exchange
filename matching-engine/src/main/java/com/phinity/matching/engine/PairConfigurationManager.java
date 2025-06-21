package com.phinity.matching.engine;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PairConfigurationManager {
    private final Set<String> highVolumePairs = ConcurrentHashMap.newKeySet();
    
    public void addHighVolumePair(String symbol) {
        highVolumePairs.add(symbol);
    }
    
    public void removeHighVolumePair(String symbol) {
        highVolumePairs.remove(symbol);
    }
    
    public boolean isHighVolumePair(String symbol) {
        return highVolumePairs.contains(symbol);
    }
    
    public Set<String> getHighVolumePairs() {
        return Set.copyOf(highVolumePairs);
    }
}