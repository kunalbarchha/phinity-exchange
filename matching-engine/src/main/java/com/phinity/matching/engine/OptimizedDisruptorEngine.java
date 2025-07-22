package com.phinity.matching.engine;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.PhasedBackoffWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.phinity.common.dto.enums.OrderType;
import com.phinity.common.dto.enums.Side;
import com.phinity.common.dto.models.PendingOrders;
import com.phinity.matching.engine.core.OrderBook;
import com.phinity.matching.engine.core.Trade;
import com.phinity.matching.engine.service.EventPublisher;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Getter
public class OptimizedDisruptorEngine implements EventHandler<OrderEvent> {
    private final String symbol;
    private final Disruptor<OrderEvent> disruptor;
    private final RingBuffer<OrderEvent> ringBuffer;
    private final OrderBook book;
    private long processedOrders = 0;

    public OptimizedDisruptorEngine(String symbol) {
        this.symbol = symbol;
        this.book = new OrderBook();

        // Corrected: Provide a ThreadFactory, not an ExecutorService
        ThreadFactory threadFactory = r -> new Thread(r, "Disruptor-" + symbol);

        this.disruptor = new Disruptor<>(
                OrderEvent::new,
                1024 * 64, // 64k buffer size
                threadFactory, // Corrected argument
                ProducerType.SINGLE,
                new PhasedBackoffWaitStrategy(1, 1, TimeUnit.MILLISECONDS, new YieldingWaitStrategy())
        );

        disruptor.handleEventsWith(this);
        this.ringBuffer = disruptor.start();
    }

    public CompletableFuture<List<Trade>> processOrder(String orderId, String symbol, Side side, BigDecimal price, BigDecimal quantity, OrderType orderType) {
        CompletableFuture<List<Trade>> future = new CompletableFuture<>();
        long sequence = ringBuffer.next();
        try {
            OrderEvent event = ringBuffer.get(sequence);
            event.set(orderId, symbol, side, price, quantity, orderType, future);
        } finally {
            ringBuffer.publish(sequence);
        }
        return future;
    }

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) throws Exception {
        try {
            PendingOrders order = event.toOrder();
            List<Trade> trades = book.matchOrder(order);
            event.getFuture().complete(trades);
            processedOrders++;
        } catch (Exception e) {
            event.getFuture().completeExceptionally(e);
        }
    }

    public void setEventPublisher(EventPublisher eventPublisher) {
        this.book.setEventPublisher(eventPublisher);
    }

    public long getProcessedOrdersCount() {
        return processedOrders;
    }

    public OrderBook getOrderBook() {
        return book;
    }

    public void shutdown() {
        disruptor.shutdown();
    }
}