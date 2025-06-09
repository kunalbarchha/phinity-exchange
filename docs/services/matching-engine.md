# Matching Engine Documentation

## Overview

The Matching Engine is the core component of the Phinity Exchange platform that processes orders and executes trades. It maintains order books for each trading pair, matches buy and sell orders according to price-time priority, and ensures fair and efficient trade execution. The Matching Engine is designed for high performance, low latency, and consistent behavior even under high load.

## Key Features

- **Order Book Management**: Maintains bid and ask sides for each trading pair
- **Price-Time Priority**: Processes orders in fair sequence based on price and time
- **Multiple Order Types**: Support for limit, market, and stop orders
- **Partial Fills**: Handles partial order execution
- **Trade Generation**: Creates trade records when orders match
- **Atomic Execution**: Ensures consistent state during order processing
- **High Performance**: Optimized for low latency and high throughput

## Configuration

The service uses the following configuration properties, which can be set in `application.yml` or through environment variables:

```yaml
server:
  port: ${MATCHING_ENGINE_PORT:8083}

spring:
  application:
    name: matching-engine

eureka:
  client:
    serviceUrl:
      defaultZone: ${EUREKA_URI:http://localhost:8761/eureka}

matching:
  trading-pairs: ${TRADING_PAIRS:BTC-USDT,ETH-USDT,XRP-USDT}
  instance-assignment: ${INSTANCE_ASSIGNMENT:true}
```

## Usage Examples

The Matching Engine primarily consumes events from Kafka rather than exposing direct API endpoints:

### Consuming Order Events

```java
@Service
public class OrderConsumer extends AbstractKafkaConsumer {
    @KafkaListener(topics = "#{T(com.phinity.common.dto.constants.KafkaTopic).ORDER_CREATED}")
    public void consumeNewOrder(ConsumerRecord<String, Object> record) {
        processMessage(record);
    }
    
    @Override
    protected void handleMessage(ConsumerRecord<String, Object> record) {
        Order order = kafkaUtil.extractPayload(record, Order.class);
        if (order != null && tradingPairAssignmentService.canHandlePair(order.getTradingPair())) {
            matchingEngineManager.processOrder(order);
        }
    }
}
```

### Publishing Trade Events

```java
@Service
public class TradePublisher {
    @Autowired
    private KafkaMessageProducer kafkaProducer;
    
    public void publishTrade(Trade trade) {
        kafkaProducer.send(KafkaTopic.TRADE_EXECUTED, trade.getId(), trade);
    }
}
```

## Integration Points

- **Order Service**: Receives orders via Kafka
- **Trade Service**: Publishes executed trades via Kafka
- **Market Data Service**: Updates order book and ticker information
- **Wallet Service**: Triggers balance updates after trades
- **Redis Module**: Caches recent trade data
- **PostgreSQL Module**: Persists trade records

## Dependencies

- Spring Boot
- Kafka Module
- Redis Module
- PostgreSQL Module
- Eureka Client

## Description in Simple Non-Tech Language

Think of the Matching Engine as the trading floor of a stock exchange. When buyers and sellers submit orders to buy or sell cryptocurrencies, the Matching Engine is responsible for finding matches between these orders and executing trades.

Just like traders on a physical trading floor follow specific rules about which orders get priority, the Matching Engine uses a "price-time priority" system. This means that orders with better prices get matched first, and when prices are the same, the order that arrived first gets priority.

The Matching Engine maintains an "order book" for each cryptocurrency pair (like BTC/USDT or ETH/USDT). This order book has two sides: the "bid" side with buy orders and the "ask" side with sell orders. When a new order arrives, the engine checks if it can match with existing orders on the opposite side of the book.

For example, if someone submits an order to buy Bitcoin at $50,000, the engine checks if there are any sell orders at $50,000 or lower. If it finds a match, it creates a trade and notifies both the buyer and seller.

The Matching Engine is designed to be extremely fast and reliable, processing thousands of orders per second while ensuring that all trades are fair and follow the rules of the exchange.