# Phinity Matching Engine - Core Components

## Production Classes (ACTIVE)

### Core Engine Components
- **HybridEngineManager.java** - Main engine manager (USE THIS)
- **OptimizedDisruptorEngine.java** - High-performance Disruptor-based engine
- **EngineManager.java** - Standard engine manager (fallback)
- **EnginePool.java** - Thread pool management

### Order Processing
- **Order.java** - Order data model
- **Trade.java** - Trade result model  
- **OrderBook.java** - Price-time priority matching logic
- **OrderEvent.java** - Disruptor event wrapper

### Configuration
- **PairConfigurationManager.java** - Dynamic pair configuration

## Usage

```java
// Initialize the hybrid engine manager
HybridEngineManager manager = new HybridEngineManager();

// Configure high-volume pairs for Disruptor
manager.configureHighVolumePair("BTC/USD", true);

// Process orders using PendingOrders from dto-module
CompletableFuture<List<Trade>> result = manager.processOrder(
    "ORDER-123", "BTC/USD", Side.BUY, 
    BigDecimal.valueOf(50000), BigDecimal.valueOf(1.0)
);

// Shutdown cleanly
manager.shutdown();
```

## Performance
- **21K+ orders/sec** with realistic matching (77.6% rate)
- **228K orders/sec** peak single-threaded performance
- **Sub-5ms latency** for order processing

## Tests
All test files moved to `src/test/java/com/phinity/matching/engine/tests/`