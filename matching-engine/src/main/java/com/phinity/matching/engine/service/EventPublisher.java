package com.phinity.matching.engine.service;

import com.phinity.common.dto.constants.KafkaTopic;
import com.phinity.common.dto.models.OrderBookUpdateEvent;
import com.phinity.common.dto.models.PendingOrders;
import com.phinity.common.dto.models.TradeExecutionEvent;
import com.phinity.kafka.producer.KafkaMessageProducer;
import com.phinity.matching.engine.core.OrderBook;
import com.phinity.matching.engine.core.Trade;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EventPublisher {
    private final KafkaMessageProducer kafkaProducer;
    
    public EventPublisher(KafkaMessageProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }
    
    public void publishTradeExecution(String symbol, List<Trade> trades) {
        if (trades.isEmpty()) return;
        
        List<TradeExecutionEvent.TradeInfo> tradeInfos = trades.stream()
            .map(trade -> {
                TradeExecutionEvent.TradeInfo tradeInfo = new TradeExecutionEvent.TradeInfo(
                    trade.getTradeId(),
                    trade.getBuyOrderId(),
                    trade.getSellOrderId(),
                    trade.getPrice(),
                    trade.getQuantity()
                );
                tradeInfo.setMakerOrderId(trade.getMakerOrderId());
                tradeInfo.setTakerOrderId(trade.getTakerOrderId());
                return tradeInfo;
            })
            .toList();
            
        TradeExecutionEvent event = new TradeExecutionEvent(symbol, tradeInfos);
        kafkaProducer.send(KafkaTopic.TRADE_EXECUTED, symbol, event);
    }
    
    public void publishOrderBookUpdate(String symbol, OrderBook orderBook) {
        // Aggregate bids by price level (highest price first)
        List<OrderBookUpdateEvent.OrderLevel> bids = orderBook.getBids().stream()
            .collect(Collectors.groupingBy(
                PendingOrders::getPrice,
                LinkedHashMap::new,
                Collectors.toList()
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<BigDecimal, List<PendingOrders>>comparingByKey().reversed())
            .limit(10)
            .map(entry -> {
                BigDecimal price = entry.getKey();
                List<PendingOrders> orders = entry.getValue();
                BigDecimal totalQuantity = orders.stream()
                    .map(PendingOrders::getRemainingQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                return new OrderBookUpdateEvent.OrderLevel(price, totalQuantity, orders.size());
            })
            .toList();
            
        // Aggregate asks by price level (lowest price first)
        List<OrderBookUpdateEvent.OrderLevel> asks = orderBook.getAsks().stream()
            .collect(Collectors.groupingBy(
                PendingOrders::getPrice,
                LinkedHashMap::new,
                Collectors.toList()
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<BigDecimal, List<PendingOrders>>comparingByKey())
            .limit(10)
            .map(entry -> {
                BigDecimal price = entry.getKey();
                List<PendingOrders> orders = entry.getValue();
                BigDecimal totalQuantity = orders.stream()
                    .map(PendingOrders::getRemainingQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                return new OrderBookUpdateEvent.OrderLevel(price, totalQuantity, orders.size());
            })
            .toList();
            
        OrderBookUpdateEvent event = new OrderBookUpdateEvent(symbol, bids, asks);
        kafkaProducer.send(KafkaTopic.ORDERBOOK_UPDATE, symbol, event);
    }
}