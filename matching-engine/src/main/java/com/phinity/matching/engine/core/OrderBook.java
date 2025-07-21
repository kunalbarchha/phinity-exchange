package com.phinity.matching.engine.core;

import com.phinity.common.dto.enums.Side;
import com.phinity.common.dto.enums.TimeInForce;
import com.phinity.common.dto.models.PendingOrders;
import com.phinity.matching.engine.service.EventPublisher;
import com.phinity.matching.engine.metrics.MetricsCollector;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class OrderBook {
    private final ConcurrentSkipListMap<BigDecimal, Queue<PendingOrders>> bids = new ConcurrentSkipListMap<>(Collections.reverseOrder());
    private final ConcurrentSkipListMap<BigDecimal, Queue<PendingOrders>> asks = new ConcurrentSkipListMap<>();
    private final AtomicLong tradeIdCounter = new AtomicLong(0);
    private EventPublisher eventPublisher;

    public void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    
    public List<Trade> matchOrder(PendingOrders order) {
        List<Trade> trades = new ArrayList<>();
        TimeInForce timeInForce = getTimeInForce(order);

        // For FOK, check if order can be completely filled first
        if (timeInForce == TimeInForce.FOK && !canFillCompletely(order)) {
            MetricsCollector.getInstance().recordRejectedOrder();
            return trades; // Return empty trades - order rejected
        }
        
        if (order.getSide() == Side.BUY) {
            matchBuyOrder(order, trades);
        } else {
            matchSellOrder(order, trades);
        }

        // Handle Time-in-Force logic
        boolean shouldAddToBook = !order.isFilled() && 
                                 order.getOrderType() != com.phinity.common.dto.enums.OrderType.MARKET &&
                                 timeInForce == TimeInForce.GTC;
        
        if (shouldAddToBook) {
            addOrderToBook(order);
        }
        
        // Publish events if there are trades or orderbook changed
        if (eventPublisher != null) {
            if (!trades.isEmpty()) {
                log.debug("DEBUG: Publishing trade execution event");
                eventPublisher.publishTradeExecution(order.getSymbol(), trades);
            }
            eventPublisher.publishOrderBookUpdate(order.getSymbol(), this);
        }
        
        return trades;
    }

    private synchronized void matchBuyOrder(PendingOrders buyOrder, List<Trade> trades) {
        while (!buyOrder.isFilled() && !asks.isEmpty()) {
            Map.Entry<BigDecimal, Queue<PendingOrders>> bestAsk = asks.firstEntry();
            
            // For limit orders, check price constraint
            if (buyOrder.getOrderType() == com.phinity.common.dto.enums.OrderType.LIMIT && 
                buyOrder.getPrice().compareTo(bestAsk.getKey()) < 0) break;
            
            Queue<PendingOrders> orders = bestAsk.getValue();
            PendingOrders sellOrder = orders.peek();
            
            if (sellOrder == null) {
                asks.remove(bestAsk.getKey());
                continue;
            }
            
            BigDecimal tradeQuantity = buyOrder.getRemainingQuantity().min(sellOrder.getRemainingQuantity());
            Trade trade = new Trade(
                String.valueOf(tradeIdCounter.incrementAndGet()),
                buyOrder.getSymbol(),
                buyOrder.getOrderId(),
                sellOrder.getOrderId(),
                sellOrder.getPrice(),
                tradeQuantity,
                sellOrder.getOrderId(), // Maker: order that was in book
                buyOrder.getOrderId(),  // Taker: order that triggered match
                buyOrder.getUserId(),   // Buyer user ID
                sellOrder.getUserId()   // Seller user ID
            );
            trades.add(trade);
            
            buyOrder.reduceQuantity(tradeQuantity);
            sellOrder.reduceQuantity(tradeQuantity);
            
            if (sellOrder.isFilled()) {
                orders.poll();
                if (orders.isEmpty()) {
                    asks.remove(bestAsk.getKey());
                }
            }
        }
    }

    private synchronized void matchSellOrder(PendingOrders sellOrder, List<Trade> trades) {
        while (!sellOrder.isFilled() && !bids.isEmpty()) {
            Map.Entry<BigDecimal, Queue<PendingOrders>> bestBid = bids.firstEntry();
            
            // For limit orders, check price constraint
            if (sellOrder.getOrderType() == com.phinity.common.dto.enums.OrderType.LIMIT && 
                sellOrder.getPrice().compareTo(bestBid.getKey()) > 0) break;
            
            Queue<PendingOrders> orders = bestBid.getValue();
            PendingOrders buyOrder = orders.peek();
            
            if (buyOrder == null) {
                bids.remove(bestBid.getKey());
                continue;
            }
            
            BigDecimal tradeQuantity = sellOrder.getRemainingQuantity().min(buyOrder.getRemainingQuantity());
            Trade trade = new Trade(
                String.valueOf(tradeIdCounter.incrementAndGet()),
                sellOrder.getSymbol(),
                buyOrder.getOrderId(),
                sellOrder.getOrderId(),
                buyOrder.getPrice(),
                tradeQuantity,
                buyOrder.getOrderId(),  // Maker: order that was in book
                sellOrder.getOrderId(), // Taker: order that triggered match
                buyOrder.getUserId(),   // Buyer user ID
                sellOrder.getUserId()   // Seller user ID
            );
            trades.add(trade);
            
            sellOrder.reduceQuantity(tradeQuantity);
            buyOrder.reduceQuantity(tradeQuantity);
            
            if (buyOrder.isFilled()) {
                orders.poll();
                if (orders.isEmpty()) {
                    bids.remove(bestBid.getKey());
                }
            }
        }
    }

    private synchronized void addOrderToBook(PendingOrders order) {
        ConcurrentSkipListMap<BigDecimal, Queue<PendingOrders>> book =
            order.getSide() == Side.BUY ? bids : asks;
        
        book.computeIfAbsent(order.getPrice(), k -> new LinkedList<>()).offer(order);
    }
    
    public List<PendingOrders> getBids() {
        return bids.values().stream()
            .flatMap(Queue::stream)
            .toList();
    }
    
    public List<PendingOrders> getAsks() {
        return asks.values().stream()
            .flatMap(Queue::stream)
            .toList();
    }
    
    public synchronized boolean cancelOrder(String orderId) {
        // Search in bids
        for (Map.Entry<BigDecimal, Queue<PendingOrders>> entry : bids.entrySet()) {
            Queue<PendingOrders> orders = entry.getValue();
            if (orders.removeIf(order -> order.getOrderId().equals(orderId))) {
                if (orders.isEmpty()) {
                    bids.remove(entry.getKey());
                }
                return true;
            }
        }
        
        // Search in asks
        for (Map.Entry<BigDecimal, Queue<PendingOrders>> entry : asks.entrySet()) {
            Queue<PendingOrders> orders = entry.getValue();
            if (orders.removeIf(order -> order.getOrderId().equals(orderId))) {
                if (orders.isEmpty()) {
                    asks.remove(entry.getKey());
                }
                return true;
            }
        }
        
        return false; // Order not found
    }
    
    /**
     * Modifies an existing order in the order book
     * @param orderId The ID of the order to modify
     * @param newPrice The new price (null to keep existing price)
     * @param newQuantity The new quantity (null to keep existing quantity)
     * @return The modified order if successful, null if order not found
     */
    public synchronized PendingOrders modifyOrder(String orderId, BigDecimal newPrice, BigDecimal newQuantity) {
        // Find the order in both books
        PendingOrders existingOrder = findOrder(orderId);
        if (existingOrder == null) {
            return null; // Order not found
        }
        
        // If only reducing quantity, we can modify in place to maintain time priority
        if (newPrice == null || newPrice.compareTo(existingOrder.getPrice()) == 0) {
            if (newQuantity != null && newQuantity.compareTo(existingOrder.getRemainingQuantity()) <= 0) {
                // Reducing quantity - maintain time priority
                BigDecimal reduction = existingOrder.getRemainingQuantity().subtract(newQuantity);
                existingOrder.reduceQuantity(reduction);
                
                // Publish orderbook update if needed
                if (eventPublisher != null) {
                    eventPublisher.publishOrderBookUpdate(existingOrder.getSymbol(), this);
                }
                
                return existingOrder;
            }
        }
        
        // For price change or quantity increase, cancel and replace
        if (cancelOrder(orderId)) {
            // Create new order with updated parameters
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
            
            // Add to book if not filled
            if (!newOrder.isFilled()) {
                addOrderToBook(newOrder);
                
                // Publish orderbook update
                if (eventPublisher != null) {
                    eventPublisher.publishOrderBookUpdate(newOrder.getSymbol(), this);
                }
            }
            
            return newOrder;
        }
        
        return null;
    }
    
    /**
     * Finds an order in the order book by ID
     * @param orderId The order ID to find
     * @return The order if found, null otherwise
     */
    private PendingOrders findOrder(String orderId) {
        // Search in bids
        for (Queue<PendingOrders> orders : bids.values()) {
            for (PendingOrders order : orders) {
                if (order.getOrderId().equals(orderId)) {
                    return order;
                }
            }
        }
        
        // Search in asks
        for (Queue<PendingOrders> orders : asks.values()) {
            for (PendingOrders order : orders) {
                if (order.getOrderId().equals(orderId)) {
                    return order;
                }
            }
        }
        
        return null; // Order not found
    }
    
    private TimeInForce getTimeInForce(PendingOrders order) {
        // Default to GTC if not specified
        return order.getTimeInForce() != null ? order.getTimeInForce() : TimeInForce.GTC;
    }
    
    private boolean canFillCompletely(PendingOrders order) {
        BigDecimal availableQuantity = BigDecimal.ZERO;
        
        if (order.getSide() == Side.BUY) {
            for (Map.Entry<BigDecimal, Queue<PendingOrders>> entry : asks.entrySet()) {
                if (order.getPrice().compareTo(entry.getKey()) < 0) break;
                
                for (PendingOrders sellOrder : entry.getValue()) {
                    availableQuantity = availableQuantity.add(sellOrder.getRemainingQuantity());
                    if (availableQuantity.compareTo(order.getRemainingQuantity()) >= 0) {
                        return true;
                    }
                }
            }
        } else {
            for (Map.Entry<BigDecimal, Queue<PendingOrders>> entry : bids.entrySet()) {
                if (order.getPrice().compareTo(entry.getKey()) > 0) break;
                
                for (PendingOrders buyOrder : entry.getValue()) {
                    availableQuantity = availableQuantity.add(buyOrder.getRemainingQuantity());
                    if (availableQuantity.compareTo(order.getRemainingQuantity()) >= 0) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
}