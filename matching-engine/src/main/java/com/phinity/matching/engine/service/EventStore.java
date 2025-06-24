package com.phinity.matching.engine.service;

import com.phinity.matching.engine.events.OrderEvent;
import com.phinity.kafka.producer.KafkaMessageProducer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventStore {

    private final List<OrderEvent> eventLog = new CopyOnWriteArrayList<>();

    public void storeEvent(OrderEvent event) {
        eventLog.add(event);
    }
    
    public List<OrderEvent> getEventsForOrder(String orderId) {
        return eventLog.stream()
            .filter(event -> event.getOrderId().equals(orderId))
            .toList();
    }
    
    public List<OrderEvent> getEventsForSymbol(String symbol) {
        return eventLog.stream()
            .filter(event -> event.getSymbol().equals(symbol))
            .toList();
    }
    
    public List<OrderEvent> getAllEvents() {
        return List.copyOf(eventLog);
    }
}