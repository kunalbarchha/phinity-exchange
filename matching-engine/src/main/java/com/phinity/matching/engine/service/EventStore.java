package com.phinity.matching.engine.service;

import com.phinity.matching.engine.events.OrderEvent;
import com.phinity.kafka.producer.KafkaMessageProducer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventStore {
    private final KafkaMessageProducer kafkaProducer;
    private final List<OrderEvent> eventLog = new CopyOnWriteArrayList<>();
    private static final String EVENT_STORE_TOPIC = "order-event-store";
    
    public EventStore(KafkaMessageProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }
    
    public void storeEvent(OrderEvent event) {
        // Store in memory for fast access
        eventLog.add(event);
        
        // Persist to Kafka for durability
        if (kafkaProducer != null) {
            kafkaProducer.send(EVENT_STORE_TOPIC, event.getOrderId(), event);
        }
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