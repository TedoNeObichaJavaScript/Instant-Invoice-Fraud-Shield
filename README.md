# Instant Invoice: Fraud Shield - FinLab Challenge

🛡️ **A fully containerized microservices solution for preventing invoice fraud through supplier payment validation**

## Overview

**Instant Invoice: Fraud Shield** is a secure API that checks outgoing payments to suppliers against a crowdsourced database of "risky IBANs" and marks anomalies in ≤ 200ms. Built for the FinLab challenge with a focus on preventing invoice fraud through real-time supplier payment validation.

## 🏗️ Architecture

### Microservices Components

1. **Frontend + Nginx Reverse Proxy** (`frontend/`)
   - Modern HTML/CSS/JS dashboard for supplier payment validation
   - Nginx with TLS/SSL, HTTP/2, gzip compression
   - Self-signed certificates for development
   - Ports: 80 (HTTP redirect), 443 (HTTPS)

2. **API Gateway** (`api-gateway/`)
   - Spring Boot 3.2 with Java 21
   - JWT authentication and validation
   - Request forwarding to microservices
   - Redis caching for JWT tokens
   - Audit logging for all requests

3. **Accounts Microservice** (`accounts-service/`)
   - Supplier payment fraud detection
   - IBAN risk assessment against crowdsourced database
   - Anomaly detection in payment patterns
   - <200ms response time guarantee
   - X-API-KEY authentication

4. **PostgreSQL Database** (`database/`)
   - 1 million randomly generated valid Bulgarian IBANs
   - Risk level classification (ALLOW/REVIEW/BLOCK)
   - Stored procedures for fast risk assessment
   - Flyway migrations for schema management

5. **Redis Cache** (`redis/`)
   - JWT token storage and validation
   - Session management
   - High-performance caching

6. **JMeter Stress Tests** (`stress_tests/`)
   - Normal load: 50 concurrent users
   - Extreme load: 100 concurrent users
   - <200ms response time validation
   - HTML reports and performance metrics

## 🚀 Quick Start

### Prerequisites
- Docker and Docker Compose installed
- Java 21 (for local development)
- Maven 3.9+ (for local development)

### 1. Clone and Setup
```bash
git clone https://github.com/TedoNeObichaJavaScript/Instant-Invoice-Fraud-Shield.git
cd Instant-Invoice-Fraud-Shield
```

### 2. Start All Services
```bash
# Start all microservices
docker compose up --build -d

# Check service status
docker compose ps

# View logs
docker compose logs -f
```

### 3. Access the Application
- **Frontend Dashboard:** https://localhost (accept self-signed certificate)
- **API Gateway:** https://localhost/api
- **Health Check:** https://localhost/health

### 4. Run Stress Tests
```bash
# Run comprehensive stress tests
docker compose --profile testing up --build -d

# Or use the provided scripts
./stress_tests/run_stress_tests.sh  # Linux/Mac
stress_tests\run_stress_tests.bat   # Windows
```

## 🔧 Configuration

### Environment Variables
Create a `.env` file for custom configuration:

```env
# Database Configuration
POSTGRES_DB=microservices_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres123

# JWT Configuration
JWT_SECRET=your-super-secret-jwt-key-change-in-production

# API Key for internal communication
API_KEY=YWJjZGVmZ2hpams7bG1ub3BxcnN0dXZ3eHl6MTIzNDU2Nzg5MA==

# Redis Configuration
REDIS_HOST=redis
REDIS_PORT=6379
```

### Service Ports
- **Frontend (Nginx):** 80, 443
- **API Gateway:** 8080 (internal)
- **Accounts Service:** 8081 (internal)
- **PostgreSQL:** 5432 (internal)
- **Redis:** 6379 (internal)

## 📊 API Endpoints

### Authentication
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

### Supplier Payment Validation
```http
POST /api/v1/suppliers/payment-validation
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "supplierIban": "BG11BANK99991234567890",
  "invoiceId": "INV-2025-001",
  "supplierName": "ABC Supplies Ltd.",
  "paymentAmount": 1000.00,
  "currency": "EUR",
  "invoiceNumber": "INV-2025-001",
  "supplierReference": "SUP-001"
}
```

### Response Format
```json
{
  "invoiceId": "INV-2025-001",
  "supplierIban": "BG11BANK99991234567890",
  "supplierName": "ABC Supplies Ltd.",
  "fraudStatus": "SAFE",
  "riskLevel": "LOW",
  "anomalies": ["No anomalies detected"],
  "recommendation": "APPROVE",
  "responseTimeMs": 150,
  "acceptableResponseTime": true,
  "timestamp": "2025-01-21T10:30:00Z"
}
```

## 🛡️ Fraud Detection Logic

### Risk Assessment Criteria
1. **IBAN Risk Level** (from crowdsourced database)
   - `ALLOW`: Low risk, approved suppliers
   - `REVIEW`: Medium risk, requires manual review
   - `BLOCK`: High risk, blocked suppliers

2. **Payment Amount Analysis**
   - Unusually high amounts (>50,000)
   - Suspiciously low amounts (<1.00)
   - Currency validation

3. **Supplier Pattern Analysis**
   - Suspicious supplier names (test, dummy)
   - Invoice number patterns
   - Reference validation

4. **Anomaly Detection**
   - Multiple risk factors
   - Pattern recognition
   - Historical analysis

### Response Time Guarantee
- **Target:** <200ms for 95% of requests
- **Maximum:** <500ms for 99% of requests
- **Monitoring:** Real-time performance tracking

## 🧪 Testing

### Manual Testing
1. **Login:** Use `admin`/`admin123` credentials
2. **Validate Payment:** Enter supplier details in the dashboard
3. **Check Results:** Review fraud detection results and anomalies

### Automated Testing
```bash
# Run all tests
docker compose --profile testing up --build -d

# Check test results
ls stress_tests/results/
open stress_tests/results/html-report/index.html
```

### Performance Validation
- **Load Testing:** 50-100 concurrent users
- **Stress Testing:** Peak load scenarios
- **Response Time:** <200ms requirement validation
- **Accuracy Testing:** Fraud detection precision

## 📈 Monitoring and Metrics

### Key Performance Indicators
- **Response Time:** Average, 95th percentile, maximum
- **Throughput:** Requests per second
- **Success Rate:** Percentage of successful validations
- **Fraud Detection Accuracy:** True positive/negative rates

### Health Checks
- **API Gateway:** `/api/health`
- **Accounts Service:** `/api/v1/suppliers/health`
- **Database:** Connection and query performance
- **Redis:** Cache hit rates and response times

## 🔒 Security Features

### Authentication & Authorization
- **JWT Tokens:** Secure, stateless authentication
- **API Keys:** Internal service communication
- **Password Hashing:** BCrypt encryption
- **Session Management:** Redis-based token storage

### Data Protection
- **TLS/SSL:** End-to-end encryption
- **Input Validation:** Comprehensive request validation
- **SQL Injection Prevention:** Parameterized queries
- **Rate Limiting:** API request throttling

### Audit & Compliance
- **Request Logging:** All API calls logged
- **Audit Trail:** Complete transaction history
- **Error Tracking:** Detailed error logging
- **Performance Monitoring:** Real-time metrics

## 🚀 Deployment

### Production Considerations
1. **Environment Variables:** Use secure secrets management
2. **SSL Certificates:** Replace self-signed with valid certificates
3. **Database Security:** Enable encryption and access controls
4. **Monitoring:** Implement comprehensive logging and alerting
5. **Scaling:** Consider horizontal scaling for high availability

### Cloud Deployment
- **Container Registry:** Push images to cloud registry
- **Orchestration:** Use Kubernetes or similar
- **Load Balancing:** Implement proper load balancing
- **Auto-scaling:** Configure based on load metrics

## 📚 Technical Details

### Technology Stack
- **Backend:** Java 21, Spring Boot 3.2, Spring MVC
- **Database:** PostgreSQL 15, Flyway migrations
- **Cache:** Redis 7
- **Frontend:** HTML5, CSS3, JavaScript (ES6+)
- **Reverse Proxy:** Nginx with TLS/SSL
- **Testing:** JMeter 5.5, Docker Compose
- **Build:** Maven 3.9, Multi-stage Docker builds

### Database Schema
- **Users:** Authentication and user management
- **JWT Tokens:** Token storage and validation
- **Audit Logs:** Request and response logging
- **IBAN Risk Lookup:** 1M+ risky IBAN database
- **Risk Assessment:** Stored procedures for fast lookups

### Performance Optimizations
- **Database Indexing:** Optimized for IBAN lookups
- **Connection Pooling:** Efficient database connections
- **Caching Strategy:** Redis for frequently accessed data
- **Query Optimization:** Stored procedures for complex logic
- **Response Compression:** Gzip compression for API responses

## 🤝 Contributing

### Development Setup
1. **Clone Repository:** `git clone <repo-url>`
2. **Install Dependencies:** Java 21, Maven, Docker
3. **Run Locally:** `mvn spring-boot:run` in each service
4. **Test Changes:** Run test suite and stress tests

### Code Standards
- **Java:** Follow Spring Boot best practices
- **Database:** Use Flyway for all schema changes
- **Testing:** Maintain >80% code coverage
- **Documentation:** Update README for significant changes

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 🏆 FinLab Challenge

**Project:** Instant Invoice: Fraud Shield  
**Challenge:** FinLab 2025  
**Focus:** Supplier payment fraud prevention  
**Performance:** <200ms response time guarantee  
**Architecture:** Containerized microservices  

---

**Built with ❤️ for the FinLab Challenge**  
*Preventing invoice fraud through intelligent supplier payment validation*