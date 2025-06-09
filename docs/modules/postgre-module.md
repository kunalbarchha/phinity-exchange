# PostgreSQL Module Documentation

## Overview

The PostgreSQL Module provides relational database persistence for the Phinity Exchange platform. It handles storage of transactional data, order books, trade history, and other structured information that requires ACID compliance and complex querying capabilities. This module encapsulates PostgreSQL configuration, entity mappings, and repository interfaces for data access.

## Key Features

- **Transactional Data Storage**: Orders, trades, and financial records
- **ACID Compliance**: Ensures data integrity for financial transactions
- **Relationship Mapping**: Maintains relationships between different data entities
- **Repository Pattern**: Simple data access through Spring Data repositories
- **Transaction Management**: Support for atomic operations across multiple tables
- **Complex Queries**: Ability to perform joins and aggregations for reporting

## Configuration

The module uses the following configuration properties, which can be set in `application.yml` or through environment variables:

```yaml
spring:
  datasource:
    url: ${POSTGRES_URL:jdbc:postgresql://localhost:5432/phinity}
    username: ${POSTGRES_USER:postgres}
    password: ${POSTGRES_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

## Usage Examples

### Managing Orders

```java
@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;
    
    @Transactional
    public Order createOrder(Order order) {
        return orderRepository.save(order);
    }
    
    public List<Order> findUserActiveOrders(String userId) {
        return orderRepository.findByUserIdAndStatus(userId, "ACTIVE");
    }
    
    public Page<Order> getUserOrderHistory(String userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}
```

### Processing Trades

```java
@Service
public class TradeService {
    @Autowired
    private TradeRepository tradeRepository;
    
    @Transactional
    public void recordTrade(Trade trade) {
        tradeRepository.save(trade);
    }
    
    public List<Trade> getMarketTrades(String symbol, int limit) {
        return tradeRepository.findBySymbolOrderByTimestampDesc(symbol, PageRequest.of(0, limit));
    }
}
```

## Integration Points

- **Order Service**: Stores and manages user orders
- **Matching Engine**: Records executed trades
- **Reporting Service**: Queries historical data for reports and analytics
- **Wallet Service**: Manages user balances and transactions

## Dependencies

- Spring Data JPA
- PostgreSQL JDBC Driver
- HikariCP Connection Pool
- Spring Boot
- Lombok for reducing boilerplate code
- DTO Module for shared data models

## Description in Simple Non-Tech Language

Think of the PostgreSQL Module as the accounting ledger for our exchange platform. It keeps track of all the financial transactions and trading activities that happen on the exchange, making sure that everything is recorded accurately and can be audited later if needed.

Unlike the more flexible MongoDB storage, PostgreSQL is like a structured spreadsheet with strict rules about what data goes where. This is perfect for financial data where we need to ensure that money is never created or destroyed incorrectly, and where we need to be able to run complex reports (like "show me all trades for this user in the last month").

The PostgreSQL Module provides safe and reliable ways to record new transactions and retrieve historical data. It ensures that when multiple things happen at once (like two orders matching to create a trade), either all the necessary records are updated or none of them are—there's never a situation where only half of a transaction is recorded.

When another part of the system needs to create an order or look up a user's trading history, it works through the PostgreSQL Module, which handles all the details of connecting to the database and ensuring data integrity.