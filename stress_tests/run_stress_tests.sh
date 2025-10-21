#!/bin/bash
# Instant Invoice: Fraud Shield - Stress Test Runner
# This script runs JMeter stress tests for supplier payment fraud detection

echo "🛡️ Instant Invoice: Fraud Shield - Stress Testing"
echo "=================================================="

# Check if JMeter container is running
if ! docker ps | grep -q microservices-jmeter; then
    echo "❌ JMeter container is not running. Starting stress tests..."
    echo "Running: docker compose --profile testing up --build -d"
    docker compose --profile testing up --build -d
    echo "⏳ Waiting for services to be ready..."
    sleep 30
fi

echo "🚀 Starting stress tests..."

# Run JMeter tests
docker exec microservices-jmeter jmeter -n -t /tests/load_test.jmx -l /results/results.jtl -e -o /results/html-report

echo "📊 Test Results:"
echo "================"

# Check if results were generated
if [ -f "stress_tests/results/results.jtl" ]; then
    echo "✅ Test completed successfully!"
    echo "📁 Results saved to: stress_tests/results/"
    echo "🌐 HTML Report: stress_tests/results/html-report/index.html"
    
    # Show basic statistics
    echo ""
    echo "📈 Quick Stats:"
    echo "==============="
    
    # Count total requests
    total_requests=$(wc -l < stress_tests/results/results.jtl)
    echo "Total Requests: $total_requests"
    
    # Count successful requests
    success_requests=$(grep -c "true" stress_tests/results/results.jtl)
    echo "Successful Requests: $success_requests"
    
    # Calculate success rate
    if [ $total_requests -gt 0 ]; then
        success_rate=$((success_requests * 100 / total_requests))
        echo "Success Rate: $success_rate%"
    fi
    
    # Show response time statistics
    echo ""
    echo "⏱️ Response Time Analysis:"
    echo "========================="
    
    # Extract response times and calculate average
    if command -v awk >/dev/null 2>&1; then
        avg_response_time=$(awk -F',' 'NR>1 {sum+=$2; count++} END {if(count>0) print sum/count; else print "N/A"}' stress_tests/results/results.jtl)
        echo "Average Response Time: ${avg_response_time}ms"
        
        # Count requests under 200ms
        under_200ms=$(awk -F',' 'NR>1 && $2<200 {count++} END {print count+0}' stress_tests/results/results.jtl)
        echo "Requests under 200ms: $under_200ms"
        
        # Calculate percentage under 200ms
        if [ $total_requests -gt 0 ]; then
            under_200ms_pct=$((under_200ms * 100 / total_requests))
            echo "Percentage under 200ms: $under_200ms_pct%"
        fi
    fi
    
    echo ""
    echo "🎯 Key Metrics:"
    echo "==============="
    echo "✅ <200ms Response Time Requirement: $([ $under_200ms_pct -ge 95 ] && echo "PASSED" || echo "NEEDS IMPROVEMENT")"
    echo "✅ System Stability: $([ $success_rate -ge 95 ] && echo "PASSED" || echo "NEEDS IMPROVEMENT")"
    
else
    echo "❌ Test failed or no results generated"
    echo "Check Docker logs: docker logs microservices-jmeter"
fi

echo ""
echo "🔍 To view detailed results:"
echo "1. Open stress_tests/results/html-report/index.html in your browser"
echo "2. Check stress_tests/results/results.jtl for raw data"
echo ""
echo "🛑 To stop tests: docker compose --profile testing down"
