package com.phinity.matching.engine.service;

import com.phinity.common.dto.constants.KafkaTopic;
import com.phinity.common.dto.enums.Side;
import com.phinity.common.dto.models.OrderBookUpdateEvent;
import com.phinity.common.dto.models.TradeExecutionEvent;
import com.phinity.kafka.producer.KafkaMessageProducer;
import com.phinity.matching.engine.core.OrderBook;
import com.phinity.matching.engine.core.Trade;

import java.util.List;

public class EventPublisher {
    private final KafkaMessageProducer kafkaProducer;
    private static final int ORDER_BOOK_DEPTH = 50; // Standard depth for broadcast

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
                    tradeInfo.setBuyerUserId(trade.getBuyerUserId());
                    tradeInfo.setSellerUserId(trade.getSellerUserId());
                    tradeInfo.setBuyerMaker(trade.getBuyOrderId().equals(trade.getMakerOrderId()));
                    return tradeInfo;
                })
                .toList();

        TradeExecutionEvent event = new TradeExecutionEvent(symbol, tradeInfos);
        kafkaProducer.send(KafkaTopic.TRADE_EXECUTED, symbol, event);
    }

    public void publishOrderBookUpdate(String symbol, OrderBook orderBook) {
        // Get aggregated, depth-limited view of the order book
        List<OrderBookUpdateEvent.OrderLevel> bids = orderBook.getAggregatedDepth(Side.BUY, ORDER_BOOK_DEPTH);
        List<OrderBookUpdateEvent.OrderLevel> asks = orderBook.getAggregatedDepth(Side.SELL, ORDER_BOOK_DEPTH);

        // Create the event with the summarized data
        OrderBookUpdateEvent event = new OrderBookUpdateEvent(symbol, bids, asks);

        // Send the much smaller event to Kafka
        kafkaProducer.send(KafkaTopic.ORDERBOOK_UPDATE, symbol, event);
    }
}
