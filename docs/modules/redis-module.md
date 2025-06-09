# Redis Module Documentation

## Overview

The Redis Module provides fast in-memory data storage and retrieval capabilities for the Phinity Exchange platform. It's primarily used for session management, caching, and real-time data that needs quick access. This module abstracts Redis operations into simple service interfaces that other components can use without dealing with low-level Redis commands.

## Key Features

- **User Session Management**: Store and retrieve user session data
- **Token Storage**: Manage access tokens for authentication
- **Status Tracking**: Track and update user statuses
- **Pub/Sub Messaging**: Real-time notifications for status changes
- **Configurable Expiry**: Automatic expiration for session data
- **Serialization**: JSON serialization for complex objects

## Configuration

The module uses the following configuration properties, which can be set in `application.yml` or through environment variables:

```yaml
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    # Optional password if Redis requires authentication
    # password: ${REDIS_PASSWORD}
```

## Usage Examples

### Managing User Sessions

```java
@Service
public class AuthenticationService {
    @Autowired
    private UserSessionService userSessionService;
    
    public void loginUser(String userId, User user, String accessToken) {
        // Store user session data and token
        userSessionService.storeUserSession(userId, user, accessToken);
    }
    
    public void logoutUser(String userId) {
        // Remove user session
        userSessionService.removeUserSession(userId);
    }
    
    public Object getUserData(String userId) {
        // Retrieve user session data
        return userSessionService.getUserSession(userId);
    }
}
```

### User Status Updates with Notifications

```java
@Service
public class UserStatusService {
    @Autowired
    private UserSessionService userSessionService;
    
    public void updateUserKycStatus(String userId, String status) {
        // Update status and trigger notification
        userSessionService.updateUserStatus(userId, status);
    }
}
```

## Integration Points

- **User Service**: Stores user session data and authentication tokens
- **Gateway Service**: Validates tokens for API requests
- **Admin Service**: Updates user statuses and permissions
- **Socket Service**: Listens for status change events to notify clients

## Dependencies

- Spring Data Redis
- Jedis (Redis client)
- Spring Boot
- Jackson for JSON serialization/deserialization
- Lombok for reducing boilerplate code

## Description in Simple Non-Tech Language

Think of the Redis Module as a high-speed notepad for our exchange platform. When users log in, we need to remember who they are and what they're allowed to do, but we need this information to be instantly available across the entire system.

Redis acts like a shared notepad that any part of the system can check very quickly. We use it to keep track of who's logged in, what permissions they have, and other information that needs to be accessed frequently and quickly.

When something important changes (like an admin suspending a user's account), Redis can also act like an announcement system, immediately telling other parts of the application about the change so they can react accordingly (like logging the user out).

The Redis Module makes all of this easy by providing simple methods to write to, read from, and listen for changes in this shared notepad, without having to worry about the technical details of how Redis works.