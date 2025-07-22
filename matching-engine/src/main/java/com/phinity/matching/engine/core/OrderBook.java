package com.phinity.matching.engine.core;

import com.phinity.common.dto.enums.Side;
import com.phinity.common.dto.enums.TimeInForce;
import com.phinity.common.dto.models.OrderBookUpdateEvent;
import com.phinity.common.dto.models.PendingOrders;
import com.phinity.matching.engine.metrics.MetricsCollector;
import com.phinity.matching.engine.service.EventPublisher;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Slf4j
public class OrderBook {

    private final ConcurrentSkipListMap<BigDecimal, Queue<PendingOrders>> bids = new ConcurrentSkipListMap<>(Collections.reverseOrder());
    private final ConcurrentSkipListMap<BigDecimal, Queue<PendingOrders>> asks = new ConcurrentSkipListMap<>();
    private final ConcurrentHashMap<String, PendingOrders> allOrders = new ConcurrentHashMap<>();

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final AtomicLong tradeIdCounter = new AtomicLong(0);
    private EventPublisher eventPublisher;

    public void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public List<Trade> matchOrder(PendingOrders order) {
        lock.writeLock().lock();
        try {
            List<Trade> trades = new ArrayList<>();
            TimeInForce timeInForce = getTimeInForce(order);

            if (timeInForce == TimeInForce.FOK && !canFillCompletely(order)) {
                MetricsCollector.getInstance().recordRejectedOrder();
                return trades;
            }

            if (order.getSide() == Side.BUY) {
                match(order, asks, trades);
            } else {
                match(order, bids, trades);
            }

            boolean shouldAddToBook = !order.isFilled() &&
                    order.getOrderType() != com.phinity.common.dto.enums.OrderType.MARKET &&
                    timeInForce == TimeInForce.GTC;

            if (shouldAddToBook) {
                addOrderToBook(order);
            }

            if (eventPublisher != null) {
                if (!trades.isEmpty()) {
                    log.debug("DEBUG: Publishing trade execution event");
                    eventPublisher.publishTradeExecution(order.getSymbol(), trades);
                }
                eventPublisher.publishOrderBookUpdate(order.getSymbol(), this);
            }

            return trades;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void match(PendingOrders takerOrder, ConcurrentSkipListMap<BigDecimal, Queue<PendingOrders>> book, List<Trade> trades) {
        while (!takerOrder.isFilled() && !book.isEmpty()) {
            Map.Entry<BigDecimal, Queue<PendingOrders>> bestPriceEntry = book.firstEntry();
            BigDecimal bestPrice = bestPriceEntry.getKey();

            if (takerOrder.getOrderType() == com.phinity.common.dto.enums.OrderType.LIMIT) {
                if (takerOrder.getSide() == Side.BUY && takerOrder.getPrice().compareTo(bestPrice) < 0) {
                    break;
                }
                if (takerOrder.getSide() == Side.SELL && takerOrder.getPrice().compareTo(bestPrice) > 0) {
                    break;
                }
            }

            Queue<PendingOrders> ordersAtPrice = bestPriceEntry.getValue();
            PendingOrders makerOrder = ordersAtPrice.peek();

            if (makerOrder == null) {
                book.remove(bestPrice);
                continue;
            }

            BigDecimal tradeQuantity = takerOrder.getRemainingQuantity().min(makerOrder.getRemainingQuantity());

            Trade trade = new Trade(
                    String.valueOf(tradeIdCounter.incrementAndGet()),
                    takerOrder.getSymbol(),
                    takerOrder.getSide() == Side.BUY ? takerOrder.getOrderId() : makerOrder.getOrderId(),
                    takerOrder.getSide() == Side.BUY ? makerOrder.getOrderId() : takerOrder.getOrderId(),
                    bestPrice,
                    tradeQuantity,
                    makerOrder.getOrderId(),
                    takerOrder.getOrderId(),
                    takerOrder.getSide() == Side.BUY ? takerOrder.getUserId() : makerOrder.getUserId(),
                    takerOrder.getSide() == Side.BUY ? makerOrder.getUserId() : takerOrder.getUserId()
            );
            trades.add(trade);

            takerOrder.reduceQuantity(tradeQuantity);
            makerOrder.reduceQuantity(tradeQuantity);

            if (makerOrder.isFilled()) {
                ordersAtPrice.poll();
                allOrders.remove(makerOrder.getOrderId());
                if (ordersAtPrice.isEmpty()) {
                    book.remove(bestPrice);
                }
            }
        }
    }

    private void addOrderToBook(PendingOrders order) {
        ConcurrentSkipListMap<BigDecimal, Queue<PendingOrders>> book = order.getSide() == Side.BUY ? bids : asks;
        book.computeIfAbsent(order.getPrice(), k -> new ConcurrentLinkedQueue<>()).offer(order);
        allOrders.put(order.getOrderId(), order);
    }

    public List<PendingOrders> getBids() {
        lock.readLock().lock();
        try {
            return bids.values().stream()
                    .flatMap(Queue::stream)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<PendingOrders> getAsks() {
        lock.readLock().lock();
        try {
            return asks.values().stream()
                    .flatMap(Queue::stream)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<OrderBookUpdateEvent.OrderLevel> getAggregatedDepth(Side side, int depth) {
        lock.readLock().lock();
        try {
            ConcurrentSkipListMap<BigDecimal, Queue<PendingOrders>> book = (side == Side.BUY) ? bids : asks;
            List<OrderBookUpdateEvent.OrderLevel> depthLevels = new ArrayList<>();

            for (Map.Entry<BigDecimal, Queue<PendingOrders>> entry : book.entrySet()) {
                if (depthLevels.size() >= depth) {
                    break;
                }

                BigDecimal price = entry.getKey();
                Queue<PendingOrders> orders = entry.getValue();

                if (orders == null || orders.isEmpty()) {
                    continue;
                }

                BigDecimal totalQuantity = orders.stream()
                        .map(PendingOrders::getRemainingQuantity)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                depthLevels.add(new OrderBookUpdateEvent.OrderLevel(price, totalQuantity, orders.size()));
            }
            return depthLevels;
        } finally {
            lock.readLock().unlock();
        }
    }


    public boolean cancelOrder(String orderId) {
        lock.writeLock().lock();
        try {
            PendingOrders order = allOrders.remove(orderId);
            if (order == null) {
                return false;
            }

            ConcurrentSkipListMap<BigDecimal, Queue<PendingOrders>> book = order.getSide() == Side.BUY ? bids : asks;
            Queue<PendingOrders> ordersAtPrice = book.get(order.getPrice());

            if (ordersAtPrice != null && ordersAtPrice.remove(order)) {
                if (ordersAtPrice.isEmpty()) {
                    book.remove(order.getPrice());
                }
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public PendingOrders modifyOrder(String orderId, BigDecimal newPrice, BigDecimal newQuantity) {
        lock.writeLock().lock();
        try {
            PendingOrders existingOrder = findOrder(orderId);
            if (existingOrder == null) {
                return null;
            }

            if ((newPrice == null || newPrice.compareTo(existingOrder.getPrice()) == 0) &&
                (newQuantity != null && newQuantity.compareTo(existingOrder.getRemainingQuantity()) <= 0)) {
                
                BigDecimal reduction = existingOrder.getRemainingQuantity().subtract(newQuantity);
                existingOrder.reduceQuantity(reduction);

                if (eventPublisher != null) {
                    eventPublisher.publishOrderBookUpdate(existingOrder.getSymbol(), this);
                }
                return existingOrder;
            }

            if (cancelOrder(orderId)) {
                PendingOrders newOrder = new PendingOrders(
                        orderId,
                        existingOrder.getSymbol(),
                        existingOrder.getSide(),
                        newPrice != null ? newPrice : existingOrder.getPrice(),
                        newQuantity != null ? newQuantity : existingOrder.getQuantity()
                );
                newOrder.setOrderType(existingOrder.getOrderType());
                newOrder.setTimeInForce(existingOrder.getTimeInForce());
                newOrder.setUserId(existingOrder.getUserId());

                if (!newOrder.isFilled()) {
                    addOrderToBook(newOrder);
                    if (eventPublisher != null) {
                        eventPublisher.publishOrderBookUpdate(newOrder.getSymbol(), this);
                    }
                }
                return newOrder;
            }
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private PendingOrders findOrder(String orderId) {
        return allOrders.get(orderId);
    }

    private TimeInForce getTimeInForce(PendingOrders order) {
        return order.getTimeInForce() != null ? order.getTimeInForce() : TimeInForce.GTC;
    }

    private boolean canFillCompletely(PendingOrders order) {
        lock.readLock().lock();
        try {
            BigDecimal availableQuantity = BigDecimal.ZERO;
            ConcurrentSkipListMap<BigDecimal, Queue<PendingOrders>> book = order.getSide() == Side.BUY ? asks : bids;

            for (Map.Entry<BigDecimal, Queue<PendingOrders>> entry : book.entrySet()) {
                if (order.getSide() == Side.BUY && order.getPrice().compareTo(entry.getKey()) < 0) break;
                if (order.getSide() == Side.SELL && order.getPrice().compareTo(entry.getKey()) > 0) break;

                for (PendingOrders makerOrder : entry.getValue()) {
                    availableQuantity = availableQuantity.add(makerOrder.getRemainingQuantity());
                    if (availableQuantity.compareTo(order.getRemainingQuantity()) >= 0) {
                        return true;
                    }
                }
            }
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }
}