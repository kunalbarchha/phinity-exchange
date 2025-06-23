package com.phinity.matching.engine;

import com.phinity.common.dto.enums.Side;
import com.phinity.common.dto.models.PendingOrders;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

public class OrderBook {
    private final ConcurrentSkipListMap<BigDecimal, Queue<PendingOrders>> bids = new ConcurrentSkipListMap<>(Collections.reverseOrder());
    private final ConcurrentSkipListMap<BigDecimal, Queue<PendingOrders>> asks = new ConcurrentSkipListMap<>();
    private final AtomicLong tradeIdCounter = new AtomicLong(0);

    public List<Trade> matchOrder(PendingOrders order) {
        List<Trade> trades = new ArrayList<>();
        
        if (order.getSide() == Side.BUY) {
            matchBuyOrder(order, trades);
        } else {
            matchSellOrder(order, trades);
        }
        
        // Only add to book if it's a limit order and not fully filled
        if (!order.isFilled() && order.getOrderType() != com.phinity.common.dto.enums.OrderType.MARKET) {
            addOrderToBook(order);
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
                tradeQuantity
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
                tradeQuantity
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
}