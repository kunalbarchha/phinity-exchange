package com.phinity.matching.engine;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.phinity.common.dto.enums.Side;
import com.phinity.common.dto.models.PendingOrders;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class OptimizedDisruptorEngine {
    private final String symbol;
    private final Disruptor<OrderEvent> disruptor;
    private final RingBuffer<OrderEvent> ringBuffer;
    private final Book book;
    private final AtomicLong processedOrders = new AtomicLong(0);

    public OptimizedDisruptorEngine(String symbol) {
        this.symbol = symbol;
        this.book = new Book();
        
        ThreadFactory threadFactory = r -> {
            Thread t = new Thread(r, "OptimizedEngine-" + symbol);
            t.setDaemon(false);
            return t;
        };

        // Use YieldingWaitStrategy for better performance
        this.disruptor = new Disruptor<>(
            OrderEvent::new,
            1024 * 1024,
            threadFactory,
            ProducerType.MULTI,
            new YieldingWaitStrategy() // Better than BlockingWaitStrategy
        );

        disruptor.handleEventsWith(this::handleOrderEvent);
        disruptor.start();
        this.ringBuffer = disruptor.getRingBuffer();
    }

    public List<Trade> processOrderSync(String orderId, String symbol,
                                        Side side, BigDecimal price, BigDecimal quantity, 
                                        com.phinity.common.dto.enums.OrderType orderType) {
        long sequence = ringBuffer.next();
        try {
            OrderEvent event = ringBuffer.get(sequence);
            event.set(orderId, symbol, side, price, quantity, orderType);
            
            ringBuffer.publish(sequence);
            
            // Optimized spin-wait
            while (!event.isProcessed()) {
                Thread.onSpinWait();
            }
            
            Trade[] trades = event.getTrades();
            return trades != null ? List.of(trades).subList(0, event.getTradeCount()) : List.of();
            
        } catch (Exception e) {
            ringBuffer.publish(sequence);
            return List.of();
        }
    }
    
    // Backward compatibility method
    public List<Trade> processOrderSync(String orderId, String symbol,
                                        Side side, BigDecimal price, BigDecimal quantity) {
        return processOrderSync(orderId, symbol, side, price, quantity, com.phinity.common.dto.enums.OrderType.LIMIT);
    }

    private void handleOrderEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        try {
            PendingOrders order = event.toOrder();
            List<Trade> trades = book.matchOrder(order);
            
            Trade[] tradeArray = trades.toArray(new Trade[0]);
            event.setResult(tradeArray, trades.size());
            
            processedOrders.incrementAndGet();
            
        } catch (Exception e) {
            event.setResult(new Trade[0], 0);
        }
    }

    public void setEventPublisher(EventPublisher eventPublisher) {
        this.book.setEventPublisher(eventPublisher);
    }
    
    public void setEventStore(EventStore eventStore) {
        this.book.setEventStore(eventStore);
    }
    
    public String getSymbol() { return symbol; }
    public long getProcessedOrdersCount() { return processedOrders.get(); }
    public Book getOrderBook() { return book; }
    public void shutdown() { disruptor.shutdown(); }
}