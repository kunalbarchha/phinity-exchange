# Environment Variables Configuration

This document lists all environment variables used across the Phinity Exchange platform, organized by service/module.

## User Service

### Security Credentials
| Environment Variable | Description | Default Value |
|---------------------|-------------|---------------|
| `SPRING_MAIL_USERNAME` | SMTP username | kb9344@gmail.com |
| `SPRING_MAIL_PASSWORD` | SMTP password | auivrdgdislfguzr |
| `AWS_S3_KEY` | AWS S3 access key | AKIAZEXUHT6RCRYWOYH3 |
| `AWS_S3_SECRET` | AWS S3 secret key | 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 |

### Database Configuration
| Environment Variable | Description | Default Value |
|---------------------|-------------|---------------|
| `SPRING_DATA_MONGODB_HOST` | MongoDB host | localhost |
| `SPRING_DATA_MONGODB_PORT` | MongoDB port | 27017 |
| `SPRING_DATA_MONGODB_DATABASE` | MongoDB database name | phinity-user |
| `SPRING_DATA_MONGODB_AUTHENTICATION_DATABASE` | MongoDB auth database | admin |
| `SPRING_DATA_MONGODB_USERNAME` | MongoDB username | *Not set* |
| `SPRING_DATA_MONGODB_PASSWORD` | MongoDB password | *Not set* |

### Service Configuration
| Environment Variable | Description | Default Value |
|---------------------|-------------|---------------|
| `SERVER_PORT` | Server port | 8002 |
| `EUREKA_URI` | Eureka service URL | http://localhost:8000/eureka/ |

### AWS Configuration
| Environment Variable | Description | Default Value |
|---------------------|-------------|---------------|
| `AWS_S3_BUCKET_NAME` | S3 bucket name | phinity |
| `AWS_S3_REGION` | AWS region | eu-north-1 |
| `AWS_S3_BUCKET_URL` | S3 bucket URL | https://phinity.s3.eu-north-1.amazonaws.com/ |

### External Service Configuration
| Environment Variable | Description | Default Value |
|---------------------|-------------|---------------|
| `SPRING_MAIL_HOST` | SMTP host | smtp.gmail.com |
| `SPRING_MAIL_PORT` | SMTP port | 587 |

## Gateway Service

### Service Configuration
| Environment Variable | Description | Default Value |
|---------------------|-------------|---------------|
| `SERVER_PORT` | Server port | 8001 |
| `EUREKA_URI` | Eureka service URL | http://localhost:8000/eureka/ |

### Security Configuration
| Environment Variable | Description | Default Value |
|---------------------|-------------|---------------|
| `JWT_SECRET` | Secret key for JWT token validation | *Not visible in config* |

## Eureka Service

### Service Configuration
| Environment Variable | Description | Default Value |
|---------------------|-------------|---------------|
| `SERVER_PORT` | Server port | 8000 |
| `EUREKA_SELF_PRESERVATION` | Enable self-preservation mode | true |

## Kafka Module

### Kafka Configuration
| Environment Variable | Description | Default Value |
|---------------------|-------------|---------------|
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker addresses | localhost:9092 |
| `KAFKA_GROUP_ID` | Kafka consumer group ID | phinity-group |
| `KAFKA_TOPIC_PARTITIONS` | Number of partitions per topic | 3 |
| `KAFKA_REPLICATION_FACTOR` | Replication factor for topics | 1 |

## Redis Module

### Redis Configuration
| Environment Variable | Description | Default Value |
|---------------------|-------------|---------------|
| `SPRING_REDIS_HOST` | Redis host | localhost |
| `SPRING_REDIS_PORT` | Redis port | 6379 |
| `SPRING_REDIS_PASSWORD` | Redis password | *Not set* |

## Utils Module

### Security Configuration
| Environment Variable | Description | Default Value |
|---------------------|-------------|---------------|
| `ENCRYPTION_SECRET` | Secret key for AES encryption | *Required, no default* |

## SMS Module

### Twilio Configuration
| Environment Variable | Description | Default Value |
|---------------------|-------------|---------------|
| `TWILIO_SMS_ACCOUNT_SID` | Twilio account SID | AC39f5543a5c647a4e150d990a0368d166 |
| `TWILIO_SMS_AUTHKEY` | Twilio auth key | 0b53b84c1d508793247e00c8fa1f84ef |
| `TWILIO_SMS_NUMBER` | Twilio phone number | 16814338655 |

## Matching Engine

### Service Configuration
| Environment Variable | Description | Default Value |
|---------------------|-------------|---------------|
| `MATCHING_ENGINE_PORT` | Server port | 8083 |
| `EUREKA_URI` | Eureka service URL | http://localhost:8761/eureka |
| `TRADING_PAIRS` | Comma-separated list of trading pairs | BTC-USDT,ETH-USDT,XRP-USDT |
| `INSTANCE_ASSIGNMENT` | Enable automatic trading pair assignment | true |

## Recommendations

1. **Development**: Create a `.env.template` file with these variables (without values) for local development
2. **Production**: Use a secrets manager like AWS Secrets Manager or HashiCorp Vault
3. **CI/CD**: Configure your pipeline to inject these variables during deployment
4. **Security**: Remove all hardcoded credentials from configuration files
5. **Management**: Consider implementing Spring Cloud Config Server for centralized configuration