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
        // Send raw orderbook data - let websocket-service handle aggregation
        List<OrderBookUpdateEvent.OrderLevel> bids = orderBook.getBids().stream()
            .map(order -> new OrderBookUpdateEvent.OrderLevel(order.getPrice(), order.getRemainingQuantity()))
            .toList();
            
        List<OrderBookUpdateEvent.OrderLevel> asks = orderBook.getAsks().stream()
            .map(order -> new OrderBookUpdateEvent.OrderLevel(order.getPrice(), order.getRemainingQuantity()))
            .toList();
            
        OrderBookUpdateEvent event = new OrderBookUpdateEvent(symbol, bids, asks);
        kafkaProducer.send(KafkaTopic.ORDERBOOK_UPDATE, symbol, event);
    }
}