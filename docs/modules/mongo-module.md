# MongoDB Module Documentation

## Overview

The MongoDB Module provides document-based data persistence for the Phinity Exchange platform. It handles storage of user data, announcements, and other non-transactional information that benefits from a flexible schema. This module encapsulates MongoDB configuration, entity definitions, and repository interfaces for data access.

## Key Features

- **User Data Storage**: Complete user profile and account information
- **Flexible Document Schema**: Support for nested objects and arrays
- **Repository Pattern**: Simple data access through Spring Data repositories
- **Custom Converters**: Type conversion for special data types like BigDecimal
- **Audit Information**: Automatic tracking of creation and modification timestamps
- **Query Methods**: Predefined methods for common data access patterns

## Configuration

The module uses the following configuration properties, which can be set in `application.yml` or through environment variables:

```yaml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/phinity}
      auto-index-creation: true
```

## Usage Examples

### Accessing User Data

```java
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public void saveUser(User user) {
        userRepository.save(user);
    }
    
    public List<User> findUsersWithPendingKyc() {
        return userRepository.findByVerificationsKycStatusEquals("PENDING");
    }
}
```

### Working with Announcements

```java
@Service
public class AnnouncementService {
    @Autowired
    private AnnouncementRepository announcementRepository;
    
    public List<Announcement> getActiveAnnouncements() {
        return announcementRepository.findByActiveTrue();
    }
    
    public void createAnnouncement(Announcement announcement) {
        announcement.setCreatedAt(System.currentTimeMillis());
        announcementRepository.save(announcement);
    }
}
```

## Integration Points

- **User Service**: Stores and retrieves user profiles and account information
- **Admin Service**: Manages system announcements and user verification data
- **KYC Service**: Updates user verification status and documents
- **Auth Service**: Validates user credentials during login

## Dependencies

- Spring Data MongoDB
- MongoDB Java Driver
- Spring Boot
- Lombok for reducing boilerplate code
- DTO Module for shared data models

## Description in Simple Non-Tech Language

Think of the MongoDB Module as the filing cabinet for our exchange platform. It stores all the information that needs to be kept for the long term, like user accounts, verification documents, and system announcements.

Unlike a traditional filing cabinet with rigid forms, MongoDB is more like a flexible notebook where we can store different types of information in whatever structure makes the most sense. This is perfect for user profiles, which might have different fields depending on the user's verification level or account type.

The MongoDB Module provides easy ways to put information into this filing system and get it back out again when needed. It handles all the complex details of connecting to the database, organizing the data, and retrieving it efficiently.

When another part of the system needs information about a user or needs to update a user's profile, it doesn't have to know how the filing system works—it just asks the MongoDB Module for what it needs, and the module takes care of the rest.