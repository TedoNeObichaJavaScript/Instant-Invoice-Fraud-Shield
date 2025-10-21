# Instant Invoice: Fraud Shield

A containerized microservices system for invoice payment risk assessment, built for the FinLab challenge.

## Project Overview

This system provides real-time fraud detection and risk assessment for invoice payments using a microservices architecture with the following components:

- **Frontend & Reverse Proxy** - Nginx with TLS serving HTML/CSS/JS
- **API Gateway** - Spring Boot with JWT authentication and audit logging
- **Accounts Microservice** - Risk assessment with <200ms latency requirement
- **Cache Service** - Redis for JWT token storage
- **Database** - PostgreSQL with 1M IBAN records for risk lookup

## Tech Stack

- **Java 21** with Spring Boot
- **Maven** for dependency management
- **JdbcTemplate** (no JPA/Hibernate)
- **PostgreSQL** with Flyway migrations
- **Redis** for caching
- **Docker** with multi-stage builds
- **Nginx** with TLS/SSL
- **JMeter** for stress testing

## Quick Start

```bash
# 1. Clone the repository
git clone <repository-url>
cd instant-invoice-fraud-shield

# 2. Copy environment configuration
cp .env.example .env

# 3. Build and start all services
docker compose up --build -d

# 4. Access the application
# Frontend: https://localhost
# API Gateway: http://localhost:8080
```

## Architecture

The system uses a microservices architecture with internal Docker networking:

```
[Frontend/Nginx:443] → [API Gateway:8080] → [Accounts Service:8081]
                                    ↓
                              [Redis:6379] + [PostgreSQL:5432]
```

## Features

- **JWT Authentication** - Stateful authentication with Redis caching
- **Risk Assessment** - Real-time IBAN risk lookup with <200ms response time
- **Audit Logging** - Complete request/response logging
- **TLS Security** - HTTPS with self-signed certificates
- **Load Testing** - JMeter stress tests included
- **Containerized** - Full Docker deployment

## Development

Built for the FinLab challenge with focus on:
- Performance (sub-200ms risk assessment)
- Security (JWT + TLS)
- Scalability (microservices architecture)
- Monitoring (audit logs + health checks)

## License

Apache License 2.0
