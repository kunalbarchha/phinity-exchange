# Utils Module Documentation

## Overview

The Utils Module provides common utility functions and helper classes used across the Phinity Exchange platform. It contains reusable code for encryption, date handling, string manipulation, validation, and other general-purpose operations. This module helps maintain consistency and reduces code duplication across services.

## Key Features

- **Encryption Utilities**: AES encryption/decryption for sensitive data
- **Date Utilities**: Date formatting, conversion, and manipulation
- **String Utilities**: String processing and manipulation functions
- **Validation Utilities**: Common validation logic
- **Number Utilities**: Currency and number formatting
- **Collection Utilities**: Helper methods for working with collections
- **Exception Handling**: Common exception types and handlers

## Configuration

The Utils Module generally doesn't require specific configuration as it provides stateless utility functions. However, some components like encryption utilities may use environment variables:

```
ENCRYPTION_SECRET=your-secret-key
```

## Usage Examples

### Using AES Encryption

```java
@Service
public class EmailService {
    @Autowired
    private EmailSender emailSender;
    
    public void sendSecureEmail(String email, String content) {
        // Encrypt sensitive content
        String encryptedContent = AESUtils.encrypt(content);
        
        // Send email with encrypted content
        emailSender.send(email, "Secure Message", encryptedContent);
    }
    
    public String decryptContent(String encryptedContent) {
        return AESUtils.decrypt(encryptedContent);
    }
}
```

### Using Date Utilities

```java
@Service
public class ReportService {
    public Report generateDailyReport(LocalDate date) {
        // Format date for display
        String formattedDate = DateUtils.formatDate(date, "yyyy-MM-dd");
        
        // Calculate date ranges
        LocalDateTime startOfDay = DateUtils.startOfDay(date);
        LocalDateTime endOfDay = DateUtils.endOfDay(date);
        
        // Generate report using date range
        return createReport(formattedDate, startOfDay, endOfDay);
    }
}
```

## Integration Points

- **All Services**: Use utility functions as needed
- **Common Modules**: Import and use utility classes
- **Security Components**: Use encryption utilities

## Dependencies

- Java Standard Library
- Apache Commons (optional)
- Spring Framework Core (optional)
- Lombok for reducing boilerplate code

## Description in Simple Non-Tech Language

Think of the Utils Module as a toolbox filled with common tools that all the builders (developers) working on our exchange platform can use. Instead of each builder having to craft their own hammer or screwdriver every time they need one, they can just grab a standardized tool from this shared toolbox.

The toolbox contains tools for many common tasks:
- Security tools for encrypting sensitive information
- Calendar tools for working with dates and times
- Text tools for formatting and processing strings
- Math tools for calculations and number formatting
- Organizational tools for working with lists and collections

By sharing these common tools, we ensure that everyone on the team solves similar problems in the same way, which makes the code more consistent and easier to maintain. It also saves time since developers don't have to reinvent solutions to common problems.

For example, when we need to encrypt user emails for storage in the database, any service can use the same encryption tool from this module, ensuring that the encryption is done consistently across the entire platform.