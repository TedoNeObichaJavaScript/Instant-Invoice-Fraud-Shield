# Stress Tests - Instant Invoice: Fraud Shield

This directory contains comprehensive stress tests for the supplier payment fraud detection system, designed to validate the <200ms response time requirement.

## Test Scenarios

### 1. Normal Load Test
- **Threads:** 50 concurrent users
- **Duration:** 10 iterations per user
- **Ramp-up:** 30 seconds
- **Scenario:** Safe supplier payments with normal amounts
- **Purpose:** Validate system performance under typical load

### 2. Extreme Load Test
- **Threads:** 100 concurrent users
- **Duration:** 20 iterations per user
- **Ramp-up:** 60 seconds
- **Scenario:** High-risk supplier payments with suspicious patterns
- **Purpose:** Test system resilience under peak load

## Test Data

### Safe Payment Pattern
```json
{
  "supplierIban": "BG11BANK99991234567890",
  "invoiceId": "INV-{random}-{random}",
  "supplierName": "ABC Supplies Ltd.",
  "paymentAmount": {random 100-5000},
  "currency": "EUR",
  "invoiceNumber": "INV-{random}",
  "supplierReference": "SUP-{random}"
}
```

### High-Risk Payment Pattern
```json
{
  "supplierIban": "BG11BANK99991234567890",
  "invoiceId": "INV-{random}-{random}",
  "supplierName": "SUSPICIOUS SUPPLIER",
  "paymentAmount": {random 50000-100000},
  "currency": "USD",
  "invoiceNumber": "TEST-{random}",
  "supplierReference": "RISK-{random}"
}
```

## Running Tests

### Option 1: Using Docker Compose (Recommended)
```bash
# Run all services including JMeter
docker compose --profile testing up --build -d

# Check results
ls stress_tests/results/
```

### Option 2: Using Scripts
```bash
# Linux/Mac
chmod +x stress_tests/run_stress_tests.sh
./stress_tests/run_stress_tests.sh

# Windows
stress_tests\run_stress_tests.bat
```

### Option 3: Manual JMeter Execution
```bash
# Start services first
docker compose up --build -d

# Run JMeter manually
docker exec microservices-jmeter jmeter -n -t /tests/load_test.jmx -l /results/results.jtl -e -o /results/html-report
```

## Test Assertions

### Response Time Requirements
- ✅ **<200ms response time** for 95% of requests
- ✅ **<500ms response time** for 99% of requests
- ✅ **No timeouts** under normal load

### Success Rate Requirements
- ✅ **>95% success rate** for all requests
- ✅ **Proper error handling** for invalid requests
- ✅ **Consistent performance** across all test scenarios

### Fraud Detection Accuracy
- ✅ **Correct fraud status** classification
- ✅ **Anomaly detection** working properly
- ✅ **Risk level assessment** accurate

## Results Analysis

### Key Metrics
1. **Response Time Distribution**
   - Average response time
   - 95th percentile response time
   - Maximum response time

2. **Throughput**
   - Requests per second
   - Peak throughput
   - Sustained throughput

3. **Error Rate**
   - Failed requests percentage
   - Error types and frequencies
   - System stability under load

4. **Fraud Detection Performance**
   - Detection accuracy
   - False positive rate
   - Processing time for different risk levels

### Expected Results
- **Normal Load:** <100ms average response time
- **Extreme Load:** <200ms average response time
- **Success Rate:** >99% for both scenarios
- **Fraud Detection:** 100% accuracy for test patterns

## Troubleshooting

### Common Issues
1. **Connection Refused**
   - Ensure all services are running: `docker compose ps`
   - Check service health: `docker compose logs`

2. **SSL Certificate Errors**
   - Accept self-signed certificate in browser
   - Use `-k` flag for curl tests

3. **High Response Times**
   - Check database performance
   - Monitor system resources
   - Verify network connectivity

### Debug Commands
```bash
# Check service status
docker compose ps

# View logs
docker compose logs api-gateway
docker compose logs accounts-service

# Monitor resources
docker stats

# Test individual endpoints
curl -k -X POST https://localhost/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

## Performance Tuning

### Database Optimization
- Ensure proper indexing on IBAN lookup table
- Monitor query execution times
- Consider connection pooling settings

### Application Tuning
- Adjust JVM heap size
- Optimize database queries
- Enable response caching where appropriate

### Infrastructure Tuning
- Increase container resources if needed
- Optimize network configuration
- Consider load balancing for production

## Reports

After test completion, detailed reports are available in:
- **HTML Report:** `stress_tests/results/html-report/index.html`
- **Raw Data:** `stress_tests/results/results.jtl`
- **Logs:** Docker container logs

The HTML report includes:
- Response time graphs
- Throughput analysis
- Error rate trends
- Performance statistics
- Detailed request/response data
