# Phinity Exchange Deployment Guide

## Overview

This guide covers the complete deployment process for Phinity Exchange services using the automated build and deployment scripts.

## Prerequisites

- Java 17+
- Maven 3.6+
- MongoDB running on localhost:27017
- Redis running on localhost:6379
- Kafka running on localhost:9092

## Quick Start

### 1. Build Services
```bash
# Build and package all services
./package.sh

# Build specific service only
./package.sh user-service
./package.sh matching-engine

# Build specific module only
./package.sh dto-module
./package.sh mongo-module

# Custom deployment directory
./package.sh /path/to/custom/deployment
./package.sh /path/to/custom/deployment user-service
```

### 2. Deploy Services
```bash
# Start all services
./deploy.sh start

# Check status
./deploy.sh status
```

## Service Ports

| Service | Port | Description |
|---------|------|-------------|
| eureka-service | 8000 | Service Discovery |
| gateway-service | 8001 | API Gateway |
| user-service | 8002 | User Management |
| market-service | 8003 | Market Data |
| admin-service | 8004 | Admin Panel |
| matching-service | 8005 | Order Matching |
| order-service | 8006 | Order Management |
| tradingview-service | 8007 | TradingView Integration |
| wallet-service | 8008 | Wallet Management |
| websocket-service | 8009 | Real-time Streaming |

## Build Commands

### Package All Components
```bash
# Build all services and modules
./package.sh

# Build with custom deployment directory
./package.sh /custom/deployment/path
```

### Package Individual Services
```bash
# Build specific services
./package.sh eureka-service
./package.sh gateway-service
./package.sh user-service
./package.sh market-service
./package.sh admin-service
./package.sh matching-service
./package.sh order-service
./package.sh tradingview-service
./package.sh wallet-service
./package.sh websocket-service
./package.sh matching-engine
```

### Package Individual Modules
```bash
# Build specific modules
./package.sh dto-module
./package.sh config-module
./package.sh utils-module
./package.sh jwt-module
./package.sh email-module
./package.sh sms-module
./package.sh file-module
./package.sh mongo-module
./package.sh redis-module
./package.sh postgre-module
./package.sh influx-module
./package.sh kafka-module
```

### Package with Custom Directory
```bash
# Build specific components with custom path
./package.sh /custom/path user-service
./package.sh /custom/path dto-module
./package.sh /custom/path matching-engine
```

## Deployment Commands

### Service Management
```bash
# Start all services in dependency order
./deploy.sh start

# Start specific service
./deploy.sh start eureka-service
./deploy.sh start user-service

# Stop all services
./deploy.sh stop

# Stop specific service
./deploy.sh stop websocket-service

# Restart all services
./deploy.sh restart

# Restart specific service
./deploy.sh restart gateway-service
```

### Monitoring
```bash
# Show service status
./deploy.sh status

# View real-time logs (Ctrl+C to exit)
./deploy.sh logs user-service
./deploy.sh logs order-service

# Health check all services
./deploy.sh health

# Show help
./deploy.sh help
```

### Custom Deployment Directory
```bash
# Use custom directory
./deploy.sh /custom/path start
./deploy.sh /custom/path status
./deploy.sh /custom/path logs user-service
```

## File Structure

```
deployment-directory/
├── *.jar                    # Service JAR files
├── pids/                    # Process ID files
│   ├── eureka-service.pid
│   ├── user-service.pid
│   └── ...
├── logs/                    # Service logs
│   ├── eureka-service.log
│   ├── user-service.log
│   └── ...
└── services.conf           # Configuration file
```

## Configuration

Edit `services.conf` in deployment directory:
```bash
# Java options for all services
JAVA_OPTS="-Xms512m -Xmx1024m"

# Spring profile
SPRING_PROFILES_ACTIVE=local
```

## Startup Sequence

Services start in dependency order:
1. eureka-service (Service Discovery)
2. gateway-service (API Gateway)
3. user-service
4. market-service
5. admin-service
6. matching-service
7. order-service
8. tradingview-service
9. wallet-service
10. websocket-service

## Typical Workflows

### Initial Deployment
```bash
# 1. Build all services and modules
./package.sh

# 2. Start all services
./deploy.sh start

# 3. Verify deployment
./deploy.sh status
./deploy.sh health
```

### Development Workflow
```bash
# 1. Build specific service after changes
./package.sh user-service

# 2. Restart only that service
./deploy.sh restart user-service

# 3. Check logs
./deploy.sh logs user-service
```

### Module Development
```bash
# 1. Build updated module
./package.sh dto-module

# 2. Build dependent services
./package.sh user-service
./package.sh order-service

# 3. Restart affected services
./deploy.sh restart user-service
./deploy.sh restart order-service
```

### Daily Operations
```bash
# Check running services
./deploy.sh

# View service logs
./deploy.sh logs user-service

# Restart problematic service
./deploy.sh restart user-service
```

### Maintenance
```bash
# Stop all services
./deploy.sh stop

# Perform maintenance...

# Restart services
./deploy.sh start

# Verify health
./deploy.sh health
```

### Troubleshooting
```bash
# Check service status
./deploy.sh status

# View recent logs
./deploy.sh logs service-name

# Check if ports are accessible
./deploy.sh health

# Rebuild and restart problematic service
./package.sh service-name
./deploy.sh restart service-name

# Show available services/modules for packaging
./package.sh help
```

## Service URLs

After deployment, services are accessible at:

- **Eureka Dashboard**: http://localhost:8000
- **API Gateway**: http://localhost:8001
- **Admin Panel**: http://localhost:8004/admin
- **WebSocket Test**: http://localhost:8009/streams

## Environment Variables

Services support these environment variables:
- `EUREKA_URI`: Eureka server URL (default: http://localhost:8000/eureka/)
- `MONGODB_HOST`: MongoDB host (default: localhost)
- `REDIS_HOST`: Redis host (default: localhost)
- `KAFKA_BROKERS`: Kafka brokers (default: localhost:9092)

## Logs

Service logs are stored in `{deployment-dir}/logs/`:
- Real-time viewing: `./deploy.sh logs service-name`
- Log files: `tail -f logs/service-name.log`
- Log rotation: Handled by Spring Boot

## Process Management

- PIDs stored in `{deployment-dir}/pids/`
- Graceful shutdown with 30-second timeout
- Force kill if graceful shutdown fails
- Automatic PID cleanup on service stop

## Health Checks

The health check verifies:
- Service process is running
- Service port is accessible
- Basic connectivity test

## Security Notes

- Services run with current user permissions
- No root privileges required
- PID files prevent duplicate service instances
- Log files contain sensitive information - secure appropriately

## Available Components

### Services
- eureka-service
- gateway-service  
- user-service
- market-service
- admin-service
- matching-service
- order-service
- tradingview-service
- wallet-service
- websocket-service
- matching-engine

### Modules
- dto-module (Common DTOs)
- config-module (Configuration)
- utils-module (Utilities)
- jwt-module (JWT Authentication)
- email-module (Email Service)
- sms-module (SMS Service)
- file-module (File Operations)
- mongo-module (MongoDB Integration)
- redis-module (Redis Integration)
- postgre-module (PostgreSQL Integration)
- influx-module (InfluxDB Integration)
- kafka-module (Kafka Integration)

## Development Tips

### Efficient Development
```bash
# Only rebuild what changed
./package.sh dto-module          # After DTO changes
./package.sh user-service        # After user service changes
./deploy.sh restart user-service # Restart only affected service
```

### Module Dependencies
- Build modules before dependent services
- Common modules (dto, config, utils) affect most services
- Database modules (mongo, redis) affect data-related services

### Performance Optimization
- Use individual service packaging during development
- Build all components only for production deployment
- Monitor service logs for startup issues

## Support

For issues:
1. Check service logs: `./deploy.sh logs service-name`
2. Verify service status: `./deploy.sh status`
3. Run health check: `./deploy.sh health`
4. Rebuild specific component: `./package.sh service-name`
5. Check process: `ps aux | grep java`
6. Check ports: `netstat -tlnp | grep :800`
7. Show help: `./package.sh help` or `./deploy.sh help`