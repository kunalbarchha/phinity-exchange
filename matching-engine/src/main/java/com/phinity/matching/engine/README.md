# Phinity Matching Engine - Enterprise Trading System

## Architecture Overview

The Phinity Matching Engine is a high-performance, enterprise-grade order matching system designed for billion-dollar trading volumes with comprehensive features including event sourcing, horizontal scaling, and advanced order types.

## Package Structure

```
com.phinity.matching.engine/
├── core/                    # Core matching components
│   ├── OrderBook.java      # Price-time priority matching logic
│   ├── Trade.java          # Trade result model
│   └── MatchingEngine.java # Single engine instance
├── manager/                 # Engine management layer
│   ├── HybridEngineManager.java    # Main engine manager (USE THIS)
│   ├── EngineManager.java          # Standard engine manager
│   ├── EnginePool.java            # Thread pool management
│   └── OptimizedDisruptorEngine.java # High-performance Disruptor engine
├── service/                 # Services and utilities
│   ├── EventPublisher.java # Kafka event publishing
│   └── EventStore.java     # Event sourcing storage
├── config/                  # Configuration management
│   └── PairConfigurationManager.java # Dynamic pair configuration
├── metrics/                 # Performance monitoring
│   └── MetricsCollector.java # Technical & business metrics
├── snapshot/                # State persistence
│   └── OrderBookSnapshot.java # Orderbook snapshots
└── events/                  # Event sourcing
    ├── OrderEvent.java
    ├── OrderReceivedEvent.java
    ├── OrderMatchedEvent.java
    └── OrderRejectedEvent.java
```

## Key Features

### 🚀 **High Performance**
- **21K+ orders/sec** with realistic matching (77.6% rate)
- **228K orders/sec** peak single-threaded performance
- **Sub-5ms latency** for order processing
- **LMAX Disruptor** for high-frequency trading pairs

### 📊 **Advanced Order Types**
- **LIMIT Orders** - Price-constrained execution
- **MARKET Orders** - Immediate execution at best price
- **Time-in-Force Support**:
  - **GTC** (Good Till Cancel) - Default behavior
  - **IOC** (Immediate or Cancel) - No resting in orderbook
  - **FOK** (Fill or Kill) - Complete fill or rejection

### 🔄 **Event Sourcing & Recovery**
- **Complete audit trail** of all order events
- **Kafka-backed persistence** for durability
- **MongoDB storage** for permanent event history
- **Snapshot + WAL recovery** for fast startup
- **Automatic orderbook restoration** after maintenance

### 📈 **Metrics & Monitoring**
- **Technical Metrics**: Latency, throughput, engine usage
- **Business Metrics**: Volume by symbol, trade counts
- **REST endpoints**: `/metrics/technical`, `/metrics/business`
- **Real-time collection** with minimal overhead

### ⚖️ **Horizontal Scaling**
- **Eureka-based service discovery**
- **Consistent hashing** for symbol distribution
- **Automatic rebalancing** when instances join/leave
- **150 virtual nodes** per instance for even distribution

## Usage Examples

### Basic Order Processing
```java
// Initialize the hybrid engine manager
HybridEngineManager manager = new HybridEngineManager();

// Configure high-volume pairs for Disruptor
manager.configureHighVolumePair("BTC/USD", true);

// Process orders with Time-in-Force
PendingOrders order = new PendingOrders("ORDER-123", "BTC/USD", Side.BUY, 
    BigDecimal.valueOf(50000), BigDecimal.valueOf(1.0));
order.setTimeInForce(TimeInForce.IOC);

CompletableFuture<List<Trade>> result = manager.processOrder(
    order.getOrderId(), order.getSymbol(), order.getSide(),
    order.getPrice(), order.getQuantity(), order.getOrderType()
);

// Shutdown cleanly
manager.shutdown();
```

### Event Sourcing Setup
```java
// Initialize event store with Kafka
EventStore eventStore = new EventStore(kafkaProducer);
manager.setEventStore(eventStore);

// Events are automatically stored for audit trail
// Recovery happens automatically on startup
```

### Metrics Collection
```java
// Access metrics programmatically
MetricsCollector metrics = MetricsCollector.getInstance();
long totalOrders = metrics.getTotalOrdersProcessed();
double avgLatency = metrics.getAverageProcessingTimeMs();

// Or via REST endpoints
// GET /metrics/technical - Technical performance metrics
// GET /metrics/business - Trading volume and business metrics
```

## Production Deployment

### Service Configuration
- **Port**: 8006 (configurable)
- **Context Path**: `/order`
- **Eureka Registration**: Automatic
- **Database**: MongoDB + Redis
- **Message Queue**: Kafka

### Scaling Strategy
1. **Symbol Distribution**: Automatic via consistent hashing
2. **Instance Discovery**: Eureka-based service registry
3. **Rebalancing**: Every 60 seconds automatic check
4. **Recovery**: Snapshot + event replay on startup

### Monitoring Endpoints
- `/metrics/all` - Complete metrics overview
- `/actuator/health` - Service health status
- Kafka topics: `order-event-store`, `trade-events`, `orderbook-updates`

## Performance Characteristics

| Metric | Value |
|--------|-------|
| **Peak Throughput** | 228K orders/sec |
| **Realistic Throughput** | 21K+ orders/sec (77.6% match rate) |
| **Average Latency** | < 5ms |
| **Memory Usage** | Optimized with object pooling |
| **Recovery Time** | < 30 seconds (snapshot + events) |

## Dependencies

- **LMAX Disruptor 3.4.4** - High-performance event processing
- **Spring Boot** - Service framework
- **Kafka** - Event streaming and persistence
- **MongoDB** - Event and snapshot storage
- **Redis** - Caching layer
- **Eureka** - Service discovery

## Enterprise Features

✅ **Audit Compliance** - Complete event sourcing trail  
✅ **High Availability** - Multi-instance deployment  
✅ **Disaster Recovery** - Automatic state restoration  
✅ **Real-time Monitoring** - Comprehensive metrics  
✅ **Horizontal Scaling** - Automatic load distribution  
✅ **Zero-downtime Deployment** - Graceful instance management  

---

*Built for enterprise-scale cryptocurrency exchanges handling billions in daily trading volume.*