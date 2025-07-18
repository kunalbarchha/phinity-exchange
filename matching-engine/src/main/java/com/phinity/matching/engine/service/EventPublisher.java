package com.phinity.matching.engine.service;

import com.phinity.common.dto.constants.KafkaTopic;
import com.phinity.common.dto.models.OrderBookUpdateEvent;
import com.phinity.common.dto.models.PendingOrders;
import com.phinity.common.dto.models.TradeExecutionEvent;
import com.phinity.common.dto.responses.TickerDTO;
import com.phinity.database.redis.service.CachedMarketDataService;
import com.phinity.kafka.producer.KafkaMessageProducer;
import com.phinity.matching.engine.core.OrderBook;
import com.phinity.matching.engine.core.Trade;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EventPublisher {
    private final KafkaMessageProducer kafkaProducer;
    private final CachedMarketDataService marketDataService;
    
    public EventPublisher(KafkaMessageProducer kafkaProducer, CachedMarketDataService marketDataService) {
        this.kafkaProducer = kafkaProducer;
        this.marketDataService = marketDataService;
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
                // Set isBuyerMaker based on whether the buy order was the maker
                tradeInfo.setBuyerMaker(trade.getBuyOrderId().equals(trade.getMakerOrderId()));
                return tradeInfo;
            })
            .toList();
            
        TradeExecutionEvent event = new TradeExecutionEvent(symbol, tradeInfos);
        kafkaProducer.send(KafkaTopic.TRADE_EXECUTED, symbol, event);
        
        // Calculate and publish ticker using Redis
        publishTicker(symbol, trades);
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
    
    private void publishTicker(String symbol, List<Trade> trades) {
        try {
            TickerDTO existingTicker = marketDataService.getTicker(symbol, TickerDTO.class);
            
            if (existingTicker == null) {
                existingTicker = createInitialTicker(symbol);
            }
            
            updateTickerFromTrades(existingTicker, trades);
            marketDataService.updateTicker(symbol, existingTicker);
            // Publish symbol-specific ticker
            publishSymbolTicker(symbol, existingTicker);
            
            // Publish to all-ticker topic (for combined ticker displays)
            kafkaProducer.send(KafkaTopic.MARKET_TICKER, symbol, existingTicker);
            
        } catch (Exception e) {
            System.err.println("Failed to update ticker for " + symbol + ": " + e.getMessage());
        }
    }
    
    private TickerDTO createInitialTicker(String symbol) {
        TickerDTO ticker = new TickerDTO();
        ticker.setSymbol(symbol);
        ticker.setLastPrice(BigDecimal.ZERO);
        ticker.setVolume(BigDecimal.ZERO);
        ticker.setQuoteVolume(BigDecimal.ZERO);
        ticker.setHighPrice(BigDecimal.ZERO);
        ticker.setLowPrice(BigDecimal.valueOf(Double.MAX_VALUE));
        ticker.setOpenPrice(BigDecimal.ZERO);
        ticker.setPriceChange(BigDecimal.ZERO);
        ticker.setPriceChangePercent(BigDecimal.ZERO);
        ticker.setTradeCount(0);
        ticker.setWeightedAvgPrice(BigDecimal.ZERO);
        ticker.setTimestamp(Instant.now().toEpochMilli());
        return ticker;
    }
    
    private void updateTickerFromTrades(TickerDTO ticker, List<Trade> trades) {
        for (Trade trade : trades) {
            BigDecimal price = trade.getPrice();
            BigDecimal quantity = trade.getQuantity();
            BigDecimal quoteAmount = price.multiply(quantity); // price * quantity = quote volume
            
            // Update basic fields
            ticker.setLastPrice(price);
            ticker.setVolume(ticker.getVolume().add(quantity)); // Base volume
            ticker.setQuoteVolume(ticker.getQuoteVolume().add(quoteAmount)); // Quote volume
            ticker.setTradeCount(ticker.getTradeCount() + 1);
            
            // Update high/low
            if (ticker.getHighPrice().equals(BigDecimal.ZERO) || price.compareTo(ticker.getHighPrice()) > 0) {
                ticker.setHighPrice(price);
            }
            if (price.compareTo(ticker.getLowPrice()) < 0) {
                ticker.setLowPrice(price);
            }
            
            // Set open price if not set (first trade)
            if (ticker.getOpenPrice().equals(BigDecimal.ZERO)) {
                ticker.setOpenPrice(price);
            }
            
            ticker.setTimestamp(Instant.now().toEpochMilli());
        }
        
        // Calculate VWAP (Volume Weighted Average Price)
        if (ticker.getVolume().compareTo(BigDecimal.ZERO) > 0) {
            ticker.setWeightedAvgPrice(ticker.getQuoteVolume().divide(ticker.getVolume(), 8, java.math.RoundingMode.HALF_UP));
        }
        
        // Calculate price change
        if (!ticker.getOpenPrice().equals(BigDecimal.ZERO)) {
            BigDecimal change = ticker.getLastPrice().subtract(ticker.getOpenPrice());
            BigDecimal changePercent = change.divide(ticker.getOpenPrice(), 4, java.math.RoundingMode.HALF_UP)
                                           .multiply(BigDecimal.valueOf(100));
            ticker.setPriceChange(change);
            ticker.setPriceChangePercent(changePercent);
        }
    }
    
    /**
     * Publish ticker for specific symbol
     */
    public void publishSymbolTicker(String symbol, TickerDTO ticker) {
        kafkaProducer.send(KafkaTopic.SYMBOL_TICKER, symbol, ticker);
    }
}