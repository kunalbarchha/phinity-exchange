# AWS KMS Integration - Comprehensive Documentation

## Overview

The Phinity Exchange Wallet Service integrates with AWS Key Management Service (KMS) to provide enterprise-grade security for cryptocurrency private keys. This implementation ensures private keys never leave AWS hardware security modules (HSMs) while supporting multiple blockchain protocols.

## Architecture

### Security Model
- **Individual KMS Keys**: Each wallet gets its own dedicated KMS key
- **Maximum Isolation**: Key compromise affects only one wallet
- **Hardware Security**: Private keys generated and stored in AWS HSMs
- **Zero Exposure**: Private keys never exist outside KMS environment

### Key Generation Flow
```
1. User requests wallet creation
2. KMS generates secp256k1 private key (never exposed)
3. KMS returns public key bytes
4. Application derives Bitcoin address from public key
5. Address validation ensures Bitcoin network compatibility
6. Store: address + KMS Key ID (no private key data)
```

### Transaction Signing Flow
```
1. Create transaction hash
2. Send hash to KMS for signing
3. KMS signs with private key (never exposed)
4. Return signature for transaction broadcast
5. Private key remains secure in KMS
```

## Supported Cryptocurrencies

### Current Support
| Chain Type | Cryptocurrencies | KMS KeySpec | Curve |
|------------|------------------|-------------|-------|
| UTXO | Bitcoin, Litecoin, Dogecoin | ECC_SECG_P256_K1 | secp256k1 |
| EVM | Ethereum, BSC, Polygon | ECC_SECG_P256_K1 | secp256k1 |

### Future Support
| Chain | Status | KMS Support | Alternative |
|-------|--------|-------------|-------------|
| Solana | Planned | ❌ (ed25519) | Hybrid approach |
| Cardano | Planned | ❌ (ed25519) | Hybrid approach |
| Ripple | Planned | ✅ (secp256k1) | Direct KMS |

## KMS Service API

### Core Methods

#### `createKmsKey(userId, asset, address, keySpec)`
- Creates individual KMS key for wallet
- Uses appropriate elliptic curve for blockchain
- Returns KMS Key ID for future operations

#### `getPublicKey(keyId)`
- Retrieves public key from KMS
- Used for address derivation
- Private key never exposed

#### `signTransactionHash(keyId, transactionHash)`
- Signs transaction hash directly in KMS
- Uses ECDSA_SHA_256 signing algorithm
- Returns signature bytes

#### `deleteKmsKey(keyId)` ⚠️
- **DANGER**: Permanently deletes KMS key
- 7-day pending deletion period
- Only use for account closure
- Makes encrypted data unrecoverable

### Key Specifications by Chain

```java
public KeySpec getKeySpecForChain(Chain chain) {
    return switch (chain) {
        case UTXO -> KeySpec.ECC_SECG_P256_K1;  // Bitcoin, Litecoin
        case EVM -> KeySpec.ECC_SECG_P256_K1;   // Ethereum, BSC, Polygon
    };
}
```

## Database Schema

### Wallet Storage
```javascript
// MongoDB Collection: wallets
{
  "_id": ObjectId,
  "userId": "user123",
  "address": "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
  "asset": "BTC",
  "network": "mainnet",
  "kmsKeyId": "arn:aws:kms:us-east-1:123456789012:key/12345678-1234-1234-1234-123456789012",
  "providerId": "uuid",
  "status": "CREATED",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

**Security Notes:**
- ✅ No private key data stored
- ✅ Only KMS Key ID and public address
- ✅ KMS Key ID is safe to store (cannot derive private key)

## Configuration

### Application Configuration
```yaml
# application.yml
wallet:
  provider: self-hosted
  self-hosted:
    utxo:
      network: ${UTXO_NETWORK:testnet}  # mainnet or testnet
    evm:
      network: ${EVM_NETWORK:sepolia}   # mainnet, goerli, sepolia

# AWS KMS Configuration
aws:
  kms:
    region: ${AWS_REGION:us-east-1}
    # Credentials: Use IAM roles in production, environment variables for development
```

### Environment Variables
```bash
# Production (IAM Roles - Recommended)
AWS_REGION=us-east-1

# Development (Access Keys)
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
AWS_REGION=us-east-1

# Network Configuration
UTXO_NETWORK=testnet
EVM_NETWORK=sepolia
```

## Security Best Practices

### Production Deployment

#### 1. IAM Roles (Recommended)
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "kms:CreateKey",
        "kms:GetPublicKey",
        "kms:Sign",
        "kms:DescribeKey"
      ],
      "Resource": "*",
      "Condition": {
        "StringEquals": {
          "kms:KeyUsage": "SIGN_VERIFY",
          "kms:KeySpec": "ECC_SECG_P256_K1"
        }
      }
    }
  ]
}
```

#### 2. Network Security
- Deploy in private subnets
- Use VPC endpoints for KMS access
- Enable CloudTrail for audit logging
- Implement IP whitelisting

#### 3. Key Management
- Regular key rotation policies
- Backup KMS Key IDs securely
- Monitor key usage patterns
- Implement key deletion safeguards

### Compliance Features

#### Audit Trail
- All KMS operations logged in CloudTrail
- Key creation, signing, and access events
- User attribution for all operations
- Immutable audit logs

#### Access Control
- Fine-grained IAM permissions
- Service-specific key policies
- Multi-factor authentication support
- Role-based access control

## Monitoring & Alerting

### Key Metrics
```yaml
# CloudWatch Metrics
- kms_key_creation_rate
- kms_signing_operations
- kms_key_access_patterns
- failed_kms_operations

# Application Metrics
- wallet_creation_success_rate
- address_validation_failures
- kms_integration_errors
- transaction_signing_latency
```

### Alerts
- Failed KMS operations
- Unusual key access patterns
- Key deletion attempts
- High error rates

## Disaster Recovery

### Key Backup Strategy
- KMS keys are automatically backed up by AWS
- Cross-region key replication available
- Key metadata stored in application database
- Recovery procedures documented

### Recovery Process
1. Identify affected KMS keys
2. Verify key accessibility
3. Restore from KMS backup if needed
4. Validate wallet functionality
5. Update application configuration

## Cost Optimization

### KMS Pricing (Approximate)
- Key creation: $1 per key per month
- API requests: $0.03 per 10,000 requests
- Cross-region replication: Additional charges

### Cost Management
- Monitor key usage patterns
- Implement key lifecycle policies
- Regular cost analysis
- Optimize API call patterns

## Troubleshooting

### Common Issues

#### 1. Key Creation Failures
```
Error: Access denied creating KMS key
Solution: Check IAM permissions for kms:CreateKey
```

#### 2. Invalid Signatures
```
Error: Transaction rejected by network
Solution: Verify KeySpec matches blockchain requirements
```

#### 3. Address Validation Failures
```
Error: Generated address is not Bitcoin-compatible
Solution: Check KMS public key format and network configuration
```

### Debug Commands
```bash
# Test KMS connectivity
aws kms list-keys --region us-east-1

# Verify key permissions
aws kms describe-key --key-id your-key-id

# Check key usage
aws kms get-key-policy --key-id your-key-id --policy-name default
```

## Development vs Production

### Development
- Use AWS access keys
- Single region deployment
- Testnet cryptocurrencies
- Relaxed security policies

### Production
- IAM roles only
- Multi-region deployment
- Mainnet cryptocurrencies
- Strict security policies
- Comprehensive monitoring

## Migration Guide

### From Other Key Management
1. **Assessment**: Inventory existing keys
2. **Planning**: Design KMS key structure
3. **Migration**: Gradual key migration
4. **Validation**: Verify functionality
5. **Cleanup**: Secure old key disposal

### Rollback Plan
1. Maintain parallel systems during migration
2. Keep old keys accessible during transition
3. Implement feature flags for quick rollback
4. Document rollback procedures

## Compliance Standards

### Supported Standards
- **SOC 2 Type II**: AWS KMS compliance
- **PCI DSS**: Payment card industry standards
- **FIPS 140-2 Level 2**: Hardware security modules
- **Common Criteria**: EAL4+ certification

### Audit Requirements
- Regular security assessments
- Penetration testing
- Compliance reporting
- Third-party audits

---

**⚠️ Security Warning**: This system handles cryptocurrency private keys. Any misconfiguration could result in permanent loss of funds. Always test thoroughly in development environments before production deployment.

**📞 Support**: For technical issues or security concerns, contact the development team immediately.