# KYC and AML Provider Integration

## Overview

The Phinity Exchange platform uses a modular KYC (Know Your Customer) and AML (Anti-Money Laundering) provider system that allows easy switching between different service providers. This document explains how the system works and how to configure different providers.

## Architecture

The system is built around the following components:

1. **KycProvider Interface**: Defines the contract that all KYC providers must implement
2. **AmlProvider Interface**: Defines the contract that all AML providers must implement
3. **Provider Factories**: Select the appropriate providers based on configuration
4. **KycService**: Orchestrates the KYC and AML workflow
5. **Provider Implementations**: Individual implementations for each supported provider

## KYC and AML Flow

1. User submits KYC information and documents
2. System validates inputs and initiates KYC verification with the configured provider
3. System periodically checks the status of pending verifications
4. When KYC is approved, system automatically initiates AML screening
5. User is fully approved only when both KYC and AML checks pass

## Supported KYC Providers

### ComplyCube

ComplyCube is a global identity verification platform that provides KYC and AML services.

**Configuration**:
```yaml
kyc:
  provider: complyCube
  complycube:
    api-key: your-api-key-here
```

### Trulioo

Trulioo offers global identity verification with coverage in over 195 countries.

**Configuration**:
```yaml
kyc:
  provider: trulioo
  trulioo:
    api-key: your-api-key-here
    api-secret: your-api-secret-here
    base-url: https://api.globaldatacompany.com
```

### Jumio

Jumio provides AI-powered identity verification and authentication solutions.

**Configuration**:
```yaml
kyc:
  provider: jumio
  jumio:
    api-token: your-api-token-here
    api-secret: your-api-secret-here
    base-url: https://api.jumio.com
```

### SumSub

SumSub is an identity verification platform that provides KYC, KYB, and AML services.

**Configuration**:
```yaml
kyc:
  provider: sumsub
  sumsub:
    api-key: your-sumsub-api-key
    api-secret: your-sumsub-secret
    base-url: https://api.sumsub.com
```

### Mock Provider

A mock provider for development and testing purposes. Always approves verifications after a 2-minute delay.

**Configuration**:
```yaml
kyc:
  provider: mock
```

## Supported AML Providers

### ComplyCube AML

Uses ComplyCube's AML screening capabilities.

**Configuration**:
```yaml
aml:
  provider: complyCubeAml
```

### Refinitiv World-Check

Refinitiv World-Check One provides comprehensive AML screening with global coverage.

**Configuration**:
```yaml
aml:
  provider: refinitiv
  refinitiv:
    api-key: your-api-key-here
    api-secret: your-api-secret-here
    base-url: https://api.refinitiv.com
```

### LexisNexis Risk Solutions

LexisNexis provides advanced risk analytics and AML screening.

**Configuration**:
```yaml
aml:
  provider: lexisNexis
  lexisnexis:
    api-key: your-api-key-here
    api-secret: your-api-secret-here
    base-url: https://api.lexisnexis.com/screening
```

### Mock AML Provider

A mock AML provider for development and testing. Approves 90% of screenings randomly.

**Configuration**:
```yaml
aml:
  provider: mockAml
```

## Configuration

The providers are configured in the `application.yml` file or through environment-specific configuration files:

```yaml
kyc:
  provider: complyCube
  status-check-interval: 60000
  complycube:
    api-key: ${COMPLYCUBE_API_KEY:your-api-key-here}
  trulioo:
    api-key: ${TRULIOO_API_KEY:your-api-key-here}
    api-secret: ${TRULIOO_API_SECRET:your-api-secret-here}
  jumio:
    api-token: ${JUMIO_API_TOKEN:your-api-token-here}
    api-secret: ${JUMIO_API_SECRET:your-api-secret-here}
  sumsub:
    api-key: ${SUMSUB_API_KEY:your-sumsub-api-key}
    api-secret: ${SUMSUB_SECRET:your-sumsub-secret}
    base-url: https://api.sumsub.com

aml:
  provider: refinitiv
  refinitiv:
    api-key: ${REFINITIV_API_KEY:your-api-key-here}
    api-secret: ${REFINITIV_API_SECRET:your-api-secret-here}
  lexisnexis:
    api-key: ${LEXISNEXIS_API_KEY:your-api-key-here}
    api-secret: ${LEXISNEXIS_API_SECRET:your-api-secret-here}
```

## Adding a New Provider

### Adding a New KYC Provider

1. Create a new class that implements the `KycProvider` interface
2. Annotate it with `@Service("providerName")`
3. Implement the required methods
4. Add configuration properties to `application.yml`

### Adding a New AML Provider

1. Create a new class that implements the `AmlProvider` interface
2. Annotate it with `@Service("providerName")`
3. Implement the required methods
4. Add configuration properties to `application.yml`

## Environment-Specific Configuration

Different environments can use different providers:

- **Development**: Use mock providers for faster testing
- **Staging**: Use real providers with test credentials
- **Production**: Use real providers with production credentials

This is configured through Spring profiles in `application-{profile}.yml` files.

## Provider Selection Criteria

When selecting a KYC/AML provider for a specific use case, consider:

1. **Geographic Coverage**: Different providers have varying coverage across regions
2. **Document Support**: Some providers specialize in certain document types
3. **Integration Complexity**: API complexity and documentation quality
4. **Pricing Model**: Per-check vs. subscription models
5. **Compliance Requirements**: Specific regulatory requirements in your target markets