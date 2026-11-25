# Phinity Matching Engine

The order matching engine behind [Phinity Exchange](README.md), built by [Cryptified Solutions](https://cryptifiedsolutions.com).

---

## Summary

Phinity's matching engine is a plain-Java, price-time priority engine that runs one order book per trading pair. A Spring Boot service routes orders to it over Kafka. In an in-memory test it processed **50,000 orders across 497 trading pairs in 510 ms (≈98,000 orders/sec)**, with no lost or duplicated quantity.

---

## How it works

```
┌─────────────────┐    ┌──────────────────────┐
│  Matching       │    │   High-volume pool   │
│  Service        │───▶│   BTC/USD, ETH/USD…  │
│  (Spring Boot)  │    └──────────────────────┘
│  Kafka router   │    ┌──────────────────────┐
└────────┬────────┘───▶│   Standard pool      │
                       │   all other pairs    │
                       └──────────────────────┘
```

- **Matching Service (Spring Boot):** reads orders from Kafka, sends each to the engine for its pair, and publishes trades and order book updates.
- **Matching Engine (plain Java):** one order book per pair, matched by price and then time.
- **Two thread pools:** busy pairs get their own pool, so a spike on BTC/USD doesn't slow down smaller pairs.
- **Runtime pair management:** pairs are added, removed or moved between pools from the admin panel, without a restart.
- **Recovery:** on restart the engine reloads from a snapshot and replays events since it was taken.

The current version is built on `ConcurrentSkipListMap` order books with `CompletableFuture` for concurrency.

---

## Features

- Order types: LIMIT, MARKET, STOP LOSS, TRAILING STOP LOSS
- Time-in-force: GTC, IOC, FOK
- Partial fills
- Maker/taker fees
- Per-pair metrics
- A failure in one pair's engine doesn't stop the other pairs

---

## Benchmark

| Scenario | Orders/sec |
|----------|-----------:|
| Single pair | ~84,000 |
| 497 pairs | ~98,000 |
| Realistic mixed load | ~54,000 |
| Concurrent stress | ~68,000 |

**Correctness checks run with the tests:**
- Better-priced orders match first
- At the same price, earlier orders match first
- Partial fills leave the right quantity
- Total quantity is conserved
- No race conditions under concurrent load

<!-- TODO: add the machine these ran on, e.g. "Measured on an 8-core / 16 GB VPS, Java 17". -->

These figures measure the engine on its own, in memory. End-to-end throughput through the gateway, Kafka and the databases depends on the hardware and the deployment.

---

## Roadmap

- **Next:** LMAX Disruptor and off-heap order books, to reduce latency and garbage collection pauses
- **Later:** CPU pinning and NUMA-aware deployment for dedicated hardware

---

## Deployment

The matching engine is latency-sensitive, so it's best on a dedicated server or VPS. The other services run fine in Docker on any cloud. They can also run side by side: the engine on dedicated hardware, the rest in the cloud.

---

## Working with us

If you're building an exchange, brokerage or trading platform, we can:

- Walk you through the engine and the private code on a call
- Run a proof of concept against your own order flow and pairs
- Adapt the engine and the surrounding services to your requirements, then hand over the code at project handover

**[Contact Cryptified Solutions](https://cryptifiedsolutions.com/contact/)**, or email hello@cryptifiedsolutions.com.
