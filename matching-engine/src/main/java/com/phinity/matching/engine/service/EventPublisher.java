package com.phinity.matching.engine.service;

import com.phinity.common.dto.constants.KafkaTopic;
import com.phinity.common.dto.models.OrderBookUpdateEvent;
import com.phinity.common.dto.models.TradeExecutionEvent;
import com.phinity.kafka.producer.KafkaMessageProducer;
import com.phinity.matching.engine.core.OrderBook;
import com.phinity.matching.engine.core.Trade;

import java.util.List;

public class EventPublisher {
    private final KafkaMessageProducer kafkaProducer;
    
    public EventPublisher(KafkaMessageProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }
    
    public void publishTradeExecution(String symbol, List<Trade> trades) {
        if (trades.isEmpty()) return;
        
        List<TradeExecutionEvent.TradeInfo> tradeInfos = trades.stream()
            .map(trade -> new TradeExecutionEvent.TradeInfo(
                trade.getTradeId(),
                trade.getBuyOrderId(),
                trade.getSellOrderId(),
                trade.getPrice(),
                trade.getQuantity()
            ))
            .toList();
            
        TradeExecutionEvent event = new TradeExecutionEvent(symbol, tradeInfos);
        kafkaProducer.send(KafkaTopic.TRADE_EXECUTED, symbol, event);
    }
    
    public void publishOrderBookUpdate(String symbol, OrderBook orderBook) {
        List<OrderBookUpdateEvent.OrderLevel> bids = orderBook.getBids().stream()
            .map(order -> new OrderBookUpdateEvent.OrderLevel(order.getPrice(), order.getQuantity()))
            .toList();
            
        List<OrderBookUpdateEvent.OrderLevel> asks = orderBook.getAsks().stream()
            .map(order -> new OrderBookUpdateEvent.OrderLevel(order.getPrice(), order.getQuantity()))
            .toList();
            
        OrderBookUpdateEvent event = new OrderBookUpdateEvent(symbol, bids, asks);
        kafkaProducer.send(KafkaTopic.ORDERBOOK_UPDATE, symbol, event);
    }
}