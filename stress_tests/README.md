# JMeter Stress Testing for Instant Invoice Fraud Shield

This directory contains comprehensive stress testing setup for the Instant Invoice Fraud Shield microservices application using Apache JMeter.

## 📁 Directory Structure

```
stress_tests/
├── Dockerfile                    # JMeter Docker container
├── README.md                     # This file
├── test-plans/                   # JMeter test plans (.jmx files)
│   ├── normal-load-test.jmx     # Normal load test (50 users, 5 min)
│   └── extreme-load-test.jmx    # Extreme load test (200 users, 10 min)
├── test-data/                    # Test data files
│   ├── users.csv                # User credentials for testing
│   └── test-ibans.csv           # IBAN and payment data
└── results/                      # Test results output
    ├── normal-load-results/      # Normal load test results
    └── extreme-load-results/     # Extreme load test results
```

## 🚀 Quick Start

### Prerequisites
- Docker and Docker Compose installed
- The main application services running (`docker-compose up -d`)

### 1. Start the Application Services
```bash
# Navigate to the project root
cd Instant-Invoice-Fraud-Shield

# Start all services
docker-compose up -d

# Verify services are healthy
docker-compose ps
```

### 2. Run Normal Load Test
```bash
# Run normal load test (50 users, 5 minutes)
docker-compose run --rm jmeter \
  -n -t /tests/test-plans/normal-load-test.jmx \
  -l /tests/results/normal-load-results/normal-results.jtl \
  -e -o /tests/results/normal-load-results/html-report
```

### 3. Run Extreme Load Test
```bash
# Run extreme load test (200 users, 10 minutes)
docker-compose run --rm jmeter \
  -n -t /tests/test-plans/extreme-load-test.jmx \
  -l /tests/results/extreme-load-results/extreme-results.jtl \
  -e -o /tests/results/extreme-load-results/html-report
```

### 4. View Results
```bash
# Open HTML reports in browser
# Normal load results
start stress_tests/results/normal-load-results/html-report/index.html

# Extreme load results  
start stress_tests/results/extreme-load-results/html-report/index.html
```

## 📊 Test Scenarios

### Normal Load Test
- **Users:** 50 concurrent users
- **Duration:** 5 minutes
- **Ramp-up:** 30 seconds
- **Target Error Rate:** 20-30%
- **Response Time Threshold:** < 2 seconds
- **Throughput Target:** > 100 requests/minute

### Extreme Load Test
- **Users:** 200 concurrent users
- **Duration:** 10 minutes
- **Ramp-up:** 60 seconds
- **Target Error Rate:** 50-70%
- **Response Time Threshold:** < 5 seconds
- **Throughput Target:** > 200 requests/minute

## 🧪 Test Flow

Each test follows this sequence:

1. **Health Check** - Verify API Gateway is responding
2. **Login** - Authenticate with test credentials
3. **Get Random IBANs** - Retrieve IBAN data for testing
4. **Generate Payment** - Create payment transactions
5. **Validate Payment** - Process payment validation

## 📈 Key Metrics Monitored

- **Response Time:** Average, 90th percentile, 95th percentile
- **Throughput:** Requests per second/minute
- **Error Rate:** Percentage of failed requests
- **Response Codes:** Distribution of HTTP status codes
- **Active Threads:** Concurrent user simulation
- **CPU/Memory Usage:** System resource utilization

## 🔧 Configuration

### JMeter Properties
- **Output Format:** XML for detailed analysis
- **Response Data:** Enabled for debugging
- **Sampler Data:** Enabled for request details
- **Headers:** Request and response headers captured

### Test Data
- **Users:** Mix of valid and invalid credentials
- **IBANs:** Realistic international bank account numbers
- **Amounts:** Various payment amounts for testing
- **Suppliers:** Different supplier names for validation

## 📋 Assertions

### Response Code Assertions
- Health Check: 200 OK
- Login: 200 OK (successful) or 401/400 (expected failures)
- API Endpoints: 200 OK or expected error codes

### Response Time Assertions
- Normal Load: < 2000ms
- Extreme Load: < 5000ms

### Content Assertions
- JSON response format validation
- Required fields presence
- Error message format validation

## 🐛 Troubleshooting

### Common Issues

1. **Connection Refused**
   ```bash
   # Ensure API Gateway is running
   docker-compose ps
   docker-compose logs api-gateway
   ```

2. **High Error Rate**
   - Check if rate limiting is too aggressive
   - Verify database connections
   - Monitor system resources

3. **Test Plan Not Found**
   ```bash
   # Verify test plans exist
   ls -la stress_tests/test-plans/
   ```

4. **Permission Denied**
   ```bash
   # Fix permissions on results directory
   chmod -R 755 stress_tests/results/
   ```

### Debug Mode
```bash
# Run with verbose logging
docker-compose run --rm jmeter \
  -n -t /tests/test-plans/normal-load-test.jmx \
  -l /tests/results/debug-results.jtl \
  -Jjmeter.logger.level=DEBUG
```

## 📊 Expected Results

### Normal Load Test
- **Success Rate:** 70-80%
- **Average Response Time:** 500-1500ms
- **Throughput:** 100-200 requests/minute
- **Error Rate:** 20-30% (acceptable for stress testing)

### Extreme Load Test
- **Success Rate:** 30-50%
- **Average Response Time:** 1000-3000ms
- **Throughput:** 200-400 requests/minute
- **Error Rate:** 50-70% (expected under extreme load)

## 🔒 Security Considerations

- Tests use test credentials only
- No real financial data in test scenarios
- Rate limiting may cause intentional failures
- SQL injection attempts are blocked (expected behavior)

## 📝 Customization

### Modifying Test Parameters
Edit the `.jmx` files to adjust:
- Number of users
- Test duration
- Ramp-up time
- Request intervals
- Assertion thresholds

### Adding New Test Scenarios
1. Create new `.jmx` file in `test-plans/`
2. Add corresponding results directory
3. Update this README with new scenario details

## 🎯 Performance Targets

| Metric | Normal Load | Extreme Load |
|--------|-------------|--------------|
| Users | 50 | 200 |
| Duration | 5 min | 10 min |
| Success Rate | 70-80% | 30-50% |
| Avg Response Time | < 2s | < 5s |
| Throughput | > 100 req/min | > 200 req/min |
| Error Rate | < 30% | < 70% |

## 📞 Support

For issues with the stress testing setup:
1. Check Docker logs: `docker-compose logs jmeter`
2. Verify test plan syntax
3. Ensure all services are healthy
4. Review JMeter documentation for advanced configuration