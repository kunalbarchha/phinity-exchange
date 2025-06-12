# User Service Documentation

## Overview

The User Service is responsible for managing user accounts and authentication in the Phinity Exchange platform. It handles user registration, login, profile management, and verification processes including KYC (Know Your Customer) and AML (Anti-Money Laundering) checks. This service acts as the central authority for user identity and access control.

## Key Features

- **User Registration**: Account creation with email verification
- **Authentication**: Secure login with JWT token generation
- **Profile Management**: User information storage and updates
- **KYC Processing**: Identity verification workflow
- **Role-Based Access**: Different permission levels for users
- **Security Features**: Password hashing, 2FA, IP whitelisting
- **Referral System**: Track and process user referrals

## Configuration

The service uses the following configuration properties, which can be set in `application.yml` or through environment variables:

```yaml
server:
  port: ${USER_SERVICE_PORT:8081}

spring:
  application:
    name: user-service

eureka:
  client:
    serviceUrl:
      defaultZone: ${EUREKA_URI:http://localhost:8761/eureka}

jwt:
  secret: ${JWT_SECRET:your-secret-key}
  expiration: ${JWT_EXPIRATION:86400}
```

## Usage Examples

### User Registration

```
POST /api/users/register
{
  "email": "user@example.com",
  "password": "securePassword123",
  "referralCode": "ABC123"  // Optional
}
```

### User Login

```
POST /api/users/login
{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

### Profile Update

```
PUT /api/users/profile
{
  "firstName": "John",
  "lastName": "Doe",
  "country": "US",
  "mobile": "+1234567890"
}
```

### KYC Submission

```
POST /users/kyc
{
  "fullName": "John Smith",
  "dateOfBirth": "01012000",  // Format: ddMMyyyy
  "documentType": "PASSPORT",
  "documentNumber": "AB123456"
}

// With multipart file uploads
frontImage: [binary file] (JPEG/PNG, max 1MB)
backImage: [binary file] (JPEG/PNG, max 1MB)
selfie: [binary file] (JPEG/PNG, max 1MB)
```

**Validation Rules:**
- All images must be JPEG or PNG format and under 1MB
- Full name, date of birth, document type and number are required
- Date of birth must be in ddMMyyyy format
- User must be at least 18 years old
- Date of birth cannot be in the future

## Integration Points

- **Gateway Service**: Routes user-related API requests
- **Email Module**: Sends verification emails and notifications
- **SMS Module**: Sends verification codes and alerts
- **Redis Module**: Stores session data and authentication tokens
- **MongoDB Module**: Persists user profile information
- **Kafka Module**: Publishes user events (registration, status changes)

## Dependencies

- Spring Boot
- Spring Security
- JWT Authentication
- MongoDB Module
- Redis Module
- Kafka Module
- Email Module
- SMS Module

## Description in Simple Non-Tech Language

Think of the User Service as the reception desk and security office of our exchange platform. When new users want to join, this service handles their registration, checks their information, and gives them an ID badge (authentication token) they can use to access different parts of the platform.

The User Service keeps track of who each user is, what they're allowed to do, and makes sure they are who they claim to be through verification processes like checking ID documents and confirming email addresses and phone numbers.

Just like a real reception desk might keep a visitor log, the User Service records when users log in and out and from which devices. It also handles requests to update personal information or change security settings.

When users want to upgrade their access level by completing KYC verification, the User Service collects their documents and either processes them directly or coordinates with external verification services to confirm the user's identity.

The User Service communicates with other parts of the system to let them know when a new user has registered or when a user's status has changed, so those other services can take appropriate actions.