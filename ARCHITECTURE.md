# Instant Invoice: Fraud Shield - Architecture Overview

## Quick Start
```bash
# 1. Start all services
docker-compose up --build -d

# 2. Access the application
# Frontend: https://localhost
# Health Check: https://localhost/health

# 3. Run stress tests (optional)
docker-compose --profile testing up jmeter
```

## System Architecture

### Frontend + Nginx (Port 443/80)
- **Technology**: HTML5, CSS3, Vanilla JavaScript, Nginx
- **Features**:
  - Modern responsive UI with professional design
  - Payment generation and validation interface
  - Real-time fraud detection dashboard
  - Authentication flow with login/logout
  - SSL/TLS termination with self-signed certificates
  - HTTP/2 and gzip compression
  - Rate limiting and security headers

### API Gateway (Port 8080 - Internal)
- **Technology**: Java 21, Spring Boot, Spring MVC
- **Features**:
  - JWT authentication and validation
  - Fraud detection logic with IBAN validation
  - Risk assessment (GOOD/REVIEW/BLOCK)
  - Audit logging and request tracking
  - Request forwarding with X-API-KEY
  - Rate limiting and security controls
  - MOD-97-10 IBAN validation algorithm

### Database (PostgreSQL - Internal)
- **Technology**: PostgreSQL 15, Flyway migrations
- **Features**:
  - 1 million valid Bulgarian IBANs (BG11BANK99991234567890)
  - Risk level distribution: 33.33% GOOD, 33.33% REVIEW, 33.33% BLOCK
  - Risk scores (0-100) for granular analysis
  - Timestamps (created_at, updated_at)
  - Optimized indexes for performance
  - Audit logs storage

### Cache (Redis - Internal)
- **Technology**: Redis 7
- **Features**:
  - JWT token storage and validation
  - Session management
  - Performance optimization
  - Sub-millisecond access times

### JMeter Testing (Optional)
- **Technology**: Apache JMeter
- **Features**:
  - Normal load testing scenarios
  - Extreme load testing scenarios
  - Performance metrics and reporting
  - Response time analysis

## Security Features

### Authentication & Authorization
- **JWT Stateful Authentication**: Tokens stored in Redis
- **API Key Validation**: X-API-KEY header for microservice communication
- **Session Management**: Secure token handling
- **Login Flow**: Proper authentication required before dashboard access

### Network Security
- **Internal Docker Network**: Isolated communication
- **No Exposed Internal Ports**: Only frontend (443/80) exposed
- **SSL/TLS Encryption**: HTTPS with self-signed certificates
- **Security Headers**: HSTS, XSS protection, content type validation

### Data Protection
- **Input Validation**: IBAN format validation using MOD-97-10
- **SQL Injection Prevention**: JdbcTemplate with parameterized queries
- **Rate Limiting**: API endpoint protection
- **Audit Logging**: Complete request/response tracking

## Performance Requirements

### Response Times
- **Accounts Service**: <200ms (as per requirements)
- **JWT Validation**: <50ms
- **Database Queries**: Optimized with proper indexes
- **Redis Caching**: Sub-millisecond access
- **Frontend Loading**: <2 seconds initial load

### Scalability
- **Horizontal Scaling**: Stateless microservices
- **Database Optimization**: Indexed queries, connection pooling
- **Caching Strategy**: Redis for session and performance data
- **Load Balancing**: Nginx reverse proxy ready

## Data Flow

1. **User Authentication**:
   - User enters credentials → API Gateway → JWT generation → Redis storage

2. **Payment Generation**:
   - Frontend → API Gateway → Database (random IBAN) → Risk assessment

3. **Fraud Detection**:
   - IBAN validation → Database lookup → Risk level determination → Response

4. **Dashboard Updates**:
   - Real-time stats → Recent validations → Blocked payments display

## Technology Stack

### Frontend
- HTML5, CSS3, Vanilla JavaScript (ES6+)
- Nginx 1.25 (Alpine)
- Responsive design with modern UI/UX

### Backend
- Java 21, Spring Boot 3.x
- Spring MVC, JdbcTemplate
- Maven build system

### Database
- PostgreSQL 15
- Flyway migrations
- Optimized queries and indexes

### Infrastructure
- Docker & Docker Compose
- Redis 7 for caching
- Apache JMeter for testing

## Deployment

### Development
```bash
docker-compose up --build -d
```

### Production Ready
- Environment-based configuration
- Health checks and monitoring
- Logging and error handling
- Security best practices implemented

## Monitoring & Observability

- **Health Endpoints**: `/health` for service status
- **Audit Logging**: Complete request/response tracking
- **Performance Metrics**: Response times and success rates
- **Error Handling**: Graceful degradation and user feedback

---

**Built for FinLab Challenge** - Advanced Payment Fraud Detection System