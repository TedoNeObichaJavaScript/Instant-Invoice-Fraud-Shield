#!/bin/sh
# Stress Testing Script for Instant Invoice: Fraud Shield
# This script runs both normal and extreme load tests

echo "🚀 Starting Stress Tests for Instant Invoice: Fraud Shield"
echo "=================================================="

# Create results directory with timestamp
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RESULTS_DIR="/tests/results/${TIMESTAMP}"
mkdir -p "$RESULTS_DIR"

echo "📊 Results will be saved to: $RESULTS_DIR"
echo ""

# Function to run a test
run_test() {
    local test_name=$1
    local test_file=$2
    local output_prefix=$3
    
    echo "🧪 Running $test_name..."
    echo "   Test file: $test_file"
    echo "   Output: ${output_prefix}-${TIMESTAMP}"
    echo ""
    
    jmeter -n -t "/tests/test-plans/$test_file" \
           -l "$RESULTS_DIR/${output_prefix}.jtl" \
           -e -o "$RESULTS_DIR/${output_prefix}-report" \
           -j "$RESULTS_DIR/${output_prefix}.log"
    
    if [ $? -eq 0 ]; then
        echo "✅ $test_name completed successfully"
    else
        echo "❌ $test_name failed"
    fi
    echo ""
}

# Wait for API Gateway to be ready
echo "⏳ Waiting for API Gateway to be ready..."
sleep 30

# Run Normal Load Test
run_test "Normal Load Test" "normal-load-test.jmx" "normal-load"

# Wait between tests
echo "⏳ Waiting 60 seconds before extreme load test..."
sleep 60

# Run Extreme Load Test
run_test "Extreme Load Test" "extreme-load-test.jmx" "extreme-load"

echo "🎉 All stress tests completed!"
echo "📁 Results saved in: $RESULTS_DIR"
echo ""
echo "📊 Test Summary:"
echo "   - Normal Load: 50 users, 10 iterations each (500 total requests)"
echo "   - Extreme Load: 200 users, 20 iterations each, 5 minutes duration (4000+ requests)"
echo ""
echo "🔍 Check the HTML reports in the results directory for detailed analysis."
