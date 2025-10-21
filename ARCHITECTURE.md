# Microservices Project Structure

## Quick Start
```bash
# 1. Copy environment file
cp .env.example .env

# 2. Build and start all services
docker compose up --build -d

# 3. Run stress tests (optional)
docker compose --profile testing up jmeter
```

## Services Architecture

### Frontend + Nginx (Port 443/80)
- Serves static HTML/CSS/JS files
- Reverse proxy to API Gateway
- TLS/SSL termination
- HTTP/2 and gzip compression

### API Gateway (Internal)
- JWT authentication and validation
- Audit logging
- Request forwarding with X-API-KEY
- Rate limiting and security

### Accounts Microservice (Internal)
- Risk assessment for IBAN payments
- <200ms response time requirement
- X-API-KEY validation

### Database (PostgreSQL)
- Flyway migrations
- 1M IBAN records for risk lookup
- Audit logs storage

### Cache (Redis)
- JWT token storage
- Session management
- Performance optimization

### JMeter Testing
- Normal load testing
- Extreme load testing
- Performance metrics

## Security Features
- Internal Docker network
- No exposed internal ports
- Environment-based configuration
- JWT stateful authentication
- API key validation

## Performance Requirements
- Accounts service: <200ms response time
- JWT validation: <50ms
- Database queries: Optimized with indexes
- Redis caching: Sub-millisecond access
