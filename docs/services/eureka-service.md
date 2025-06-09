# Eureka Service Documentation

## Overview

The Eureka Service provides service discovery and registration for the Phinity Exchange microservices architecture. It maintains a registry of all running service instances, their locations, and health status. This allows services to find and communicate with each other without hardcoded URLs, enabling dynamic scaling and failover capabilities.

## Key Features

- **Service Registration**: Automatically registers service instances
- **Service Discovery**: Allows services to find each other
- **Health Monitoring**: Tracks the health status of registered services
- **Load Balancing**: Provides information for client-side load balancing
- **Failover Support**: Enables automatic failover to healthy instances
- **Self-Preservation**: Protects registry during network partitions
- **Dashboard**: Visual interface for monitoring service status

## Configuration

The service uses the following configuration properties, which can be set in `application.yml` or through environment variables:

```yaml
server:
  port: ${EUREKA_PORT:8761}

spring:
  application:
    name: eureka-service

eureka:
  client:
    registerWithEureka: false
    fetchRegistry: false
  server:
    enableSelfPreservation: ${EUREKA_SELF_PRESERVATION:true}
    renewalPercentThreshold: 0.85
```

## Usage Examples

### Service Registration

Services automatically register with Eureka by including the Eureka client dependency and configuration:

```yaml
# In a microservice's application.yml
eureka:
  client:
    serviceUrl:
      defaultZone: ${EUREKA_URI:http://localhost:8761/eureka}
  instance:
    preferIpAddress: true
```

### Service Discovery

Services can discover and call other services using Eureka:

```java
@Service
public class ServiceCaller {
    @Autowired
    private DiscoveryClient discoveryClient;
    
    @Autowired
    private RestTemplate restTemplate;
    
    public Object callUserService(String userId) {
        // Get instances of user-service
        List<ServiceInstance> instances = discoveryClient.getInstances("USER-SERVICE");
        
        if (instances.isEmpty()) {
            throw new ServiceUnavailableException("USER-SERVICE");
        }
        
        // Get the first available instance
        ServiceInstance serviceInstance = instances.get(0);
        String baseUrl = serviceInstance.getUri().toString();
        
        // Call the service
        return restTemplate.getForObject(baseUrl + "/api/users/" + userId, User.class);
    }
}
```

## Integration Points

- **All Microservices**: Register themselves with Eureka
- **Gateway Service**: Uses Eureka to route requests to services
- **Infrastructure Monitoring**: Can query Eureka for service health

## Dependencies

- Spring Cloud Netflix Eureka Server
- Spring Boot Actuator

## Description in Simple Non-Tech Language

Think of the Eureka Service as a phone directory for all the different services in our exchange platform. When a new service starts up, it "calls" Eureka and says, "Hi, I'm the User Service, and you can reach me at this address." Eureka writes this down in its directory.

When one service needs to talk to another service, instead of having to know exactly where that service is located (which might change if we add more servers or if a server goes down), it asks Eureka, "Can you tell me where I can find the User Service?" Eureka checks its directory and provides the current address.

Eureka also regularly checks if services are still available by sending a "Are you still there?" message. If a service doesn't respond, Eureka marks it as unavailable and stops directing traffic to it.

This system makes our platform much more flexible and resilient. We can add new instances of services when there's high demand, take instances down for maintenance, or automatically replace failed instances, and the rest of the system will adapt automatically without any disruption to users.