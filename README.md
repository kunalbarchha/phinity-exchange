# Phinity Exchange

**A cryptocurrency exchange backend built as Java microservices: order management, a price-time priority matching engine, wallets, KYC, market data and real-time feeds.**

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.0-blue.svg)](https://spring.io/projects/spring-cloud)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-event%20driven-black.svg)](https://kafka.apache.org/)

Phinity is an in-house platform by [Cryptified Solutions](https://cryptifiedsolutions.com). We use it as the base for exchange, brokerage and trading-platform projects, so clients start from working, tested building blocks instead of a blank repository.

---

## What's in this repository

This repository holds the **architecture, documentation and the public modules**. The core trading services are kept in private repositories and linked here as Git submodules.

| Module | Visibility |
|--------|-----------|
| `gateway-service` | Public |
| `kafka-module` | Public |
| `matching-engine`, `matching-service`, `order-service`, `wallet-service`, `user-service`, `database-module` and the other services | Private |

If you're evaluating us for a project, we're happy to walk you through the private code on a call. [Get in touch](#built-by).

---

## Architecture

```
                      ┌──────────────────┐
   Clients ─────────▶ │  Gateway Service │ ◀──── Eureka (service discovery)
   (REST / WS)        └────────┬─────────┘
                               │
     ┌───────────────┬─────────┼──────────────┬────────────────┐
     ▼               ▼         ▼              ▼                ▼
┌──────────┐   ┌──────────┐ ┌──────────┐ ┌──────────┐   ┌──────────────┐
│  User    │   │  Order   │ │  Wallet  │ │  Market  │   │    Admin     │
│ Service  │   │ Service  │ │ Service  │ │ Service  │   │   Service    │
└──────────┘   └────┬─────┘ └────▲─────┘ └────▲─────┘   └──────────────┘
                    │            │            │
                    ▼            │            │
             ┌─────────────────────────────────────┐
             │          Apache Kafka (events)      │
             └─────────┬──────────────────▲────────┘
                       ▼                  │
             ┌──────────────────┐   ┌─────┴──────────────┐
             │ Matching Service │──▶│  WebSocket Service │──▶ live order book,
             │ + Matching Engine│   │ TradingView Service│    trades, candles
             └──────────────────┘   └────────────────────┘
```

Orders go in through the gateway, are validated and balance-checked by the order service, then routed over Kafka to the matching engine. Trades and order book changes are published back as events, which update wallets and stream to clients in real time.

---

## Services

| Service | Port | Responsibility |
|---------|------|----------------|
| Eureka Service | 8000 | Service discovery and health |
| Gateway Service | 8001 | Routing, JWT authentication, rate limiting |
| User Service | 8002 | Registration, login, KYC, sessions |
| Order Service | 8003 | Order validation, balance holds, fees |
| Market Service | 8004 | Trading pairs, prices, market statistics |
| Admin Service | 8005 | Pair management, users, configuration |
| Matching Service | 8006 | Routes orders to the right engine for each pair |
| Wallet Service | 8007 | Balances, deposits, withdrawals, history |
| WebSocket Service | 8008 | Live order book, trades and order updates |
| TradingView Service | 8009 | OHLCV candles for TradingView charts |

**Shared modules:** Kafka, database access (PostgreSQL / TimescaleDB, MongoDB, Redis), JWT, configuration, utilities and shared DTOs.

---

## Matching engine

- Price-time priority order books, one engine per trading pair
- Pairs are added or removed at runtime from the admin panel, with no restart
- High-volume pairs are placed in a separate pool from long-tail pairs
- Order types: **LIMIT, MARKET, STOP LOSS, TRAILING STOP LOSS**
- Time-in-force: **GTC, IOC, FOK**
- Partial fills, maker/taker fees, snapshot and event-replay recovery

### Benchmark

In-memory engine test: **50,000 orders across 497 trading pairs, processed in 510 ms (≈98,000 orders/sec)**, with no lost or duplicated quantity.

| Scenario | Orders/sec |
|----------|-----------:|
| Single pair | ~84,000 |
| 497 pairs | ~98,000 |
| Realistic mixed load | ~54,000 |

<!-- TODO: add the machine this ran on, e.g. "Measured on an 8-core / 16 GB VPS, Java 17". -->

These numbers measure the matching engine itself. End-to-end throughput through the gateway, Kafka and the databases depends on the deployment.

---

## Security

- JWT authentication with role-based access
- KYC integration with document upload ([providers](docs/services/kyc-providers.md))
- Rate limiting at the gateway
- IP whitelisting
- 2FA by email and SMS
- AWS KMS for key management ([details](docs/aws-kms-integration.md))
- Audit trail of order and balance events through Kafka

---

## Tech stack

Java 17 · Spring Boot 3.5 · Spring Cloud 2025 (Eureka, Gateway) · Apache Kafka · PostgreSQL / TimescaleDB · MongoDB · Redis · AWS S3 and KMS · Docker

---

## Documentation

**Services:**
- [Gateway](docs/services/gateway-service.md)
- [User](docs/services/user-service.md)
- [Eureka](docs/services/eureka-service.md)
- [Matching Engine](docs/services/matching-engine.md)
- [Webhooks](docs/services/webhooks.md)

**Modules:**
- [Kafka](docs/modules/kafka-module.md)
- [PostgreSQL](docs/modules/postgre-module.md)
- [MongoDB](docs/modules/mongo-module.md)
- [Redis](docs/modules/redis-module.md)
- [Utils](docs/modules/utils-module.md)

**Setup:**
- [Environment variables](docs/environment-values.md)
- [Deployment](DEPLOYMENT.md)
- [Hardware requirements](PRODUCTION-HARDWARE-REQUIREMENTS.md)
- [MongoDB setup](MONGODB-SETUP-GUIDE.md)

**Business:**
- [User onboarding](docs/business/user-onboarding.md)

---

## Building

Requires Java 17+, Maven 3.8+, and access to the private submodules.

```bash
git clone --recurse-submodules https://github.com/kunalbarchha/phinity-exchange.git
cd phinity-exchange
mvn clean install
```

Start Eureka first, then the gateway, then the other services. See [DEPLOYMENT.md](DEPLOYMENT.md) for the full order and the environment variables each service needs.

---

## Built by

**[Cryptified Solutions](https://cryptifiedsolutions.com)** builds exchanges, trading platforms, fintech backends, AI agents and enterprise integrations for businesses worldwide.

Need an exchange, brokerage backend or matching engine? Want to see the private code? [Contact us](https://cryptifiedsolutions.com/contact/) or email **hello@cryptifiedsolutions.com**.

---

© Cryptified Solutions (OPC) Pvt Ltd. All rights reserved.
