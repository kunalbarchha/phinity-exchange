# AWS KMS Local Development Setup

## Prerequisites
- AWS Account with KMS access
- AWS CLI installed
- Java 17+ and Maven
- Phinity Exchange wallet-service

## Step 1: AWS Console Setup

### 1.1 Create IAM User for Development
1. **Go to IAM Console**: https://console.aws.amazon.com/iam/
2. **Create User**:
   - Username: `phinity-wallet-dev`
   - Access type: ✅ Programmatic access
3. **Attach Policies**:
   - Create custom policy or use PowerUserAccess for development

### 1.2 Create Custom KMS Policy (Recommended)
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PhinityWalletKMSAccess",
      "Effect": "Allow",
      "Action": [
        "kms:CreateKey",
        "kms:DescribeKey",
        "kms:GetPublicKey",
        "kms:Sign",
        "kms:ListKeys",
        "kms:ListAliases",
        "kms:TagResource",
        "kms:UntagResource",
        "kms:ScheduleKeyDeletion",
        "kms:CancelKeyDeletion"
      ],
      "Resource": "*",
      "Condition": {
        "StringEquals": {
          "kms:KeyUsage": "SIGN_VERIFY"
        }
      }
    }
  ]
}
```

### 1.3 Get Access Keys
1. **After user creation**, download the CSV file with:
   - Access Key ID
   - Secret Access Key
2. **⚠️ Keep these secure** - never commit to version control

## Step 2: Local Environment Configuration

### 2.1 Install AWS CLI
```bash
# macOS
brew install awscli

# Ubuntu/Debian
sudo apt install awscli

# Windows
# Download from: https://aws.amazon.com/cli/
```

### 2.2 Configure AWS CLI
```bash
aws configure
```
Enter when prompted:
- **AWS Access Key ID**: Your access key from Step 1.3
- **AWS Secret Access Key**: Your secret key from Step 1.3
- **Default region**: `us-east-1` (or your preferred region)
- **Default output format**: `json`

### 2.3 Test AWS KMS Access
```bash
# List existing KMS keys
aws kms list-keys

# Should return JSON with keys array (may be empty)
{
  "Keys": []
}
```

## Step 3: Application Configuration

### 3.1 Update application.yml
```yaml
# wallet-service/src/main/resources/application.yml
wallet:
  provider: self-hosted
  self-hosted:
    utxo:
      network: testnet  # Use testnet for development
    evm:
      network: sepolia  # Use testnet for development

# AWS KMS Configuration
aws:
  kms:
    region: us-east-1  # Match your AWS CLI region
```

### 3.2 Set Environment Variables
```bash
# Option 1: Export in terminal
export AWS_REGION=us-east-1
export UTXO_NETWORK=testnet
export EVM_NETWORK=sepolia

# Option 2: Create .env file (add to .gitignore)
echo "AWS_REGION=us-east-1" >> .env
echo "UTXO_NETWORK=testnet" >> .env
echo "EVM_NETWORK=sepolia" >> .env

# Option 3: IDE Environment Variables
# Set in your IDE run configuration
```

## Step 4: Development Testing

### 4.1 Start the Application
```bash
cd wallet-service
mvn spring-boot:run
```

### 4.2 Test KMS Integration
```bash
# Test wallet creation endpoint
curl -X POST http://localhost:8008/wallet/api/create \
  -H "Content-Type: application/json" \
  -H "userId: test-user-123" \
  -d '{
    "asset": "BTC",
    "network": "testnet"
  }'
```

### 4.3 Expected Response
```json
{
  "success": true,
  "data": {
    "address": "tb1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
    "asset": "BTC",
    "network": "testnet",
    "providerId": "uuid-here",
    "status": "CREATED"
  },
  "message": "Wallet created successfully"
}
```

### 4.4 Verify in AWS Console
1. **Go to KMS Console**: https://console.aws.amazon.com/kms/
2. **Check Customer managed keys**
3. **Should see new key** with description: "Wallet key for user test-user-123 - BTC address tb1q..."

## Step 5: Debugging

### 5.1 Enable Debug Logging
```yaml
# application.yml
logging:
  level:
    com.phinity.wallet: DEBUG
    software.amazon.awssdk: DEBUG
```

### 5.2 Common Issues & Solutions

#### Issue: "Access Denied" Error
```
Error: User: arn:aws:iam::123456789012:user/phinity-wallet-dev is not authorized to perform: kms:CreateKey
```
**Solution**: Check IAM policy has `kms:CreateKey` permission

#### Issue: "Invalid KeySpec" Error
```
Error: KeySpec ECC_SECG_P256_K1 is not supported
```
**Solution**: Verify AWS region supports secp256k1 keys (most regions do)

#### Issue: "Region Not Found" Error
```
Error: The security token included in the request is invalid
```
**Solution**: Check AWS_REGION environment variable matches AWS CLI configuration

### 5.3 Debug Commands
```bash
# Check AWS credentials
aws sts get-caller-identity

# List KMS keys
aws kms list-keys --region us-east-1

# Describe specific key
aws kms describe-key --key-id your-key-id --region us-east-1

# Check key policy
aws kms get-key-policy --key-id your-key-id --policy-name default --region us-east-1
```

## Step 6: Development Best Practices

### 6.1 Key Management
```bash
# List all development keys
aws kms list-keys --query 'Keys[*].KeyId' --output table

# Clean up test keys (⚠️ Use carefully)
aws kms schedule-key-deletion --key-id your-test-key-id --pending-window-in-days 7
```

### 6.2 Cost Management
- **Monitor KMS usage** in AWS Billing Console
- **Delete test keys** after development
- **Use testnet** cryptocurrencies only
- **Limit key creation** during testing

### 6.3 Security
- **Never commit AWS keys** to version control
- **Use different keys** for each developer
- **Rotate keys regularly**
- **Monitor CloudTrail** for unusual activity

## Step 7: IDE Configuration

### 7.1 IntelliJ IDEA
1. **Run Configuration** → Environment Variables:
   ```
   AWS_REGION=us-east-1
   UTXO_NETWORK=testnet
   EVM_NETWORK=sepolia
   ```

### 7.2 VS Code
1. **Create .vscode/launch.json**:
   ```json
   {
     "version": "0.2.0",
     "configurations": [
       {
         "type": "java",
         "name": "WalletApplication",
         "request": "launch",
         "mainClass": "com.phinity.wallet.WalletApplication",
         "env": {
           "AWS_REGION": "us-east-1",
           "UTXO_NETWORK": "testnet",
           "EVM_NETWORK": "sepolia"
         }
       }
     ]
   }
   ```

## Step 8: Testing Checklist

### ✅ Pre-Development Checklist
- [ ] AWS account access confirmed
- [ ] IAM user created with KMS permissions
- [ ] AWS CLI configured and tested
- [ ] Application starts without errors
- [ ] KMS connectivity verified

### ✅ Development Testing
- [ ] Bitcoin testnet wallet creation works
- [ ] KMS key appears in AWS Console
- [ ] Address validation passes
- [ ] Logs show successful KMS operations
- [ ] No private keys in logs or database

### ✅ Cleanup Checklist
- [ ] Test KMS keys scheduled for deletion
- [ ] AWS credentials secured
- [ ] Debug logging disabled
- [ ] Environment variables documented

## Troubleshooting Quick Reference

| Error | Cause | Solution |
|-------|-------|----------|
| Access Denied | Missing IAM permissions | Add KMS permissions to IAM user |
| Invalid Region | Wrong AWS region | Check AWS_REGION environment variable |
| Key Creation Failed | Invalid KeySpec | Verify region supports ECC_SECG_P256_K1 |
| Connection Timeout | Network issues | Check internet connection and AWS endpoints |
| Invalid Credentials | Wrong access keys | Verify AWS CLI configuration |

## Next Steps

1. **Test wallet creation** with different assets
2. **Implement transaction signing** (future feature)
3. **Add error handling** for production scenarios
4. **Set up monitoring** and alerting
5. **Plan production deployment** with IAM roles

---

**🚀 Ready to develop!** Your local environment is now configured for AWS KMS integration testing.

**⚠️ Remember**: Always use testnet for development and never commit AWS credentials to version control.