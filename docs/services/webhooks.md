# Webhook Integration

## Overview

The Phinity Exchange platform supports webhook integrations with KYC and AML providers to receive real-time updates on verification status changes. This document explains how webhooks are configured and processed.

## Supported Webhook Endpoints

### ComplyCube Webhook

**Endpoint**: `/webhooks/complycube`

**Method**: POST

**Headers**:
- `X-CC-Webhook-Signature`: HMAC-SHA256 signature for payload verification

**Events Handled**:
- `check.completed`: Verification check has been completed
- `check.expired`: Verification check has expired

## Webhook Flow

1. KYC provider sends a webhook notification to our endpoint
2. System verifies the webhook signature using the configured secret
3. System processes the event based on its type
4. For completed checks:
   - If approved, system updates user status and initiates AML check
   - If rejected, system updates user status with rejection reason
5. For expired checks:
   - System updates user status to VERIFICATION_TIMEOUT

## Configuration

Webhook secrets are configured in the application properties:

```yaml
kyc:
  complycube:
    webhook-secret: ${COMPLYCUBE_WEBHOOK_SECRET}
```

## Setting Up Webhooks in Provider Dashboards

### ComplyCube

1. Log in to the ComplyCube dashboard
2. Navigate to Settings > Webhooks
3. Add a new webhook with the URL: `https://your-domain.com/webhooks/complycube`
4. Generate a webhook secret and save it
5. Select the events to subscribe to: `check.completed` and `check.expired`
6. Save the webhook configuration

## Security Considerations

- Webhook endpoints are publicly accessible but verify the signature of incoming requests
- Webhook secrets should be stored securely as environment variables
- Failed signature verifications are logged and the request is rejected

## Troubleshooting

If webhooks are not being processed correctly:

1. Check that the webhook URL is correctly configured in the provider dashboard
2. Verify that the webhook secret matches between the provider and your application
3. Check the application logs for signature verification failures
4. Ensure the webhook endpoint is publicly accessible and not blocked by firewalls