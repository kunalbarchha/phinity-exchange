# Gateway Service Documentation

## Overview

The Gateway Service acts as the entry point for all client requests to the Phinity Exchange platform. It routes incoming API calls to the appropriate microservices, handles authentication verification, and provides cross-cutting concerns like rate limiting and request logging. This service ensures that clients have a single endpoint to interact with while the backend architecture remains flexible.

## Key Features

- **API Routing**: Directs requests to appropriate microservices
- **Authentication**: Validates JWT tokens for protected endpoints
- **Rate Limiting**: Prevents abuse by limiting request frequency
- **Request Logging**: Records API calls for monitoring and debugging
- **Load Balancing**: Distributes traffic across service instances
- **Circuit Breaking**: Prevents cascading failures when services are down
- **Request/Response Transformation**: Modifies headers or payloads as needed

## Configuration

The service uses the following configuration properties, which can be set in `application.yml` or through environment variables:

```yaml
server:
  port: ${GATEWAY_PORT:8080}

spring:
  application:
    name: gateway-service
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://USER-SERVICE
          predicates:
            - Path=/api/users/**
        - id: order-service
          uri: lb://ORDER-SERVICE
          predicates:
            - Path=/api/orders/**
        # Additional routes...

eureka:
  client:
    serviceUrl:
      defaultZone: ${EUREKA_URI:http://localhost:8761/eureka}

jwt:
  secret: ${JWT_SECRET:your-secret-key}
```

## Usage Examples

The Gateway Service doesn't have explicit API endpoints of its own. Instead, it routes requests to other services based on the URL path:

### User Registration (routed to User Service)

```
POST /api/users/register
Host: api.phinity-exchange.com
```

### Place Order (routed to Order Service)

```
POST /api/orders
Host: api.phinity-exchange.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Get Market Data (routed to Market Data Service)

```
GET /api/market/ticker/BTC-USDT
Host: api.phinity-exchange.com
```

## Integration Points

- **All Microservices**: Routes requests to appropriate services
- **Eureka Service**: Discovers available service instances
- **Redis Module**: Validates authentication tokens
- **User Service**: Verifies user permissions

## Dependencies

- Spring Cloud Gateway
- Spring Cloud Netflix Eureka Client
- Spring Security
- JWT Authentication
- Redis Module

## Description in Simple Non-Tech Language

Think of the Gateway Service as the front desk of a large office building. When visitors (API requests) arrive, they don't need to know which floor or room to go to—they just tell the receptionist (Gateway Service) what they need, and the receptionist directs them to the right department.

The Gateway Service also acts as a security guard, checking visitors' ID badges (authentication tokens) to make sure they're allowed to enter the building and access specific areas. It keeps track of how many times someone visits (rate limiting) to prevent any single visitor from overwhelming the staff.

Additionally, the Gateway Service maintains a visitor log, recording who came in, what they asked for, and when. This helps with troubleshooting if there are any issues later.

For the developers and users of the exchange platform, the Gateway Service provides a huge benefit: they only need to remember one address to access any service, and the gateway takes care of finding the right service to handle each request. This makes the system much easier to use while allowing the backend to change and evolve without disrupting clients.