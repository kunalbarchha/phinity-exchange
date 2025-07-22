package com.phinity.matching.engine;

import com.phinity.common.dto.enums.OrderType;
import com.phinity.common.dto.enums.Side;
import com.phinity.common.dto.models.PendingOrders;
import com.phinity.matching.engine.core.Trade;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class OrderEvent {
    private String orderId;
    private String symbol;
    private Side side;
    private BigDecimal price;
    private BigDecimal quantity;
    private OrderType orderType;
    private CompletableFuture<List<Trade>> future;

    public void set(String orderId, String symbol, Side side, BigDecimal price, BigDecimal quantity, OrderType orderType, CompletableFuture<List<Trade>> future) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.side = side;
        this.price = price;
        this.quantity = quantity;
        this.orderType = orderType;
        this.future = future;
    }

    public PendingOrders toOrder() {
        PendingOrders order = new PendingOrders(orderId, symbol, side, price, quantity);
        order.setOrderType(orderType);
        return order;
    }

    public CompletableFuture<List<Trade>> getFuture() {
        return future;
    }

    public void clear() {
        orderId = null;
        symbol = null;
        side = null;
        price = null;
        quantity = null;
        orderType = null;
        future = null;
    }
}
