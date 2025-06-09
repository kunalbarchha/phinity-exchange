# Kafka Module Documentation

## Overview

The Kafka Module provides a standardized way to interact with Apache Kafka across the Phinity Exchange platform. It handles message publishing and consumption, allowing different services to communicate asynchronously through events. This module abstracts away the complexity of Kafka configuration and provides simple interfaces for producing and consuming messages.

## Key Features

- **Centralized Topic Management**: All Kafka topics are defined as constants in a single location
- **Standardized Message Production**: Simple API for sending messages to any topic
- **Consistent Consumer Pattern**: Abstract base class for creating message consumers
- **Error Handling**: Built-in error handling and logging for message processing
- **Dynamic Topic Creation**: Automatic creation of topics from constants
- **Configurable Settings**: Externalized configuration for Kafka connection and behavior

## Configuration

The module uses the following configuration properties, which can be set in `application.yml` or through environment variables:

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      acks: all
      retries: 3
    consumer:
      group-id: ${KAFKA_GROUP_ID:phinity-group}
      auto-offset-reset: earliest
    topic:
      num-partitions: ${KAFKA_TOPIC_PARTITIONS:3}
      replication-factor: ${KAFKA_REPLICATION_FACTOR:1}
```

## Usage Examples

### Producing Messages

```java
@Service
public class UserService {
    @Autowired
    private KafkaMessageProducer kafkaProducer;
    
    public void registerUser(User user) {
        // Business logic...
        
        // Publish event to Kafka
        kafkaProducer.send(KafkaTopic.USER_REGISTRATION, user.getId(), user);
    }
}
```

### Consuming Messages

```java
@Service
public class UserRegistrationConsumer extends AbstractKafkaConsumer {
    @KafkaListener(topics = "#{T(com.phinity.common.dto.constants.KafkaTopic).USER_REGISTRATION}")
    public void consume(ConsumerRecord<String, Object> record) {
        processMessage(record);
    }
    
    @Override
    protected void handleMessage(ConsumerRecord<String, Object> record) {
        User user = kafkaUtil.extractPayload(record, User.class);
        if (user != null) {
            // Process the user registration
        }
    }
}
```

## Integration Points

- **User Service**: Publishes user registration and status change events
- **Order Service**: Publishes order creation and update events
- **Matching Engine**: Consumes order events and publishes trade events
- **Notification Service**: Consumes various events to send notifications

## Dependencies

- Spring Kafka
- Spring Boot
- Jackson for JSON serialization/deserialization
- Lombok for reducing boilerplate code
- DTO Module for shared data models and constants

## Description in Simple Non-Tech Language

Think of the Kafka Module as the postal service for our exchange platform. When one part of the system needs to tell another part that something happened (like a new user signed up or an order was placed), it writes a message and puts it in a specific mailbox (called a "topic"). 

Other parts of the system that care about those events check their assigned mailboxes regularly. When they find a new message, they read it and take appropriate action. This way, different parts of the system can communicate without having to directly call each other.

The Kafka Module makes this process easy by providing pre-made envelopes (message formats), a directory of all available mailboxes (topics), and handling all the details of making sure messages get delivered properly.