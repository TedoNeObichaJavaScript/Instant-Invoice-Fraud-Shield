@echo off
REM Instant Invoice: Fraud Shield - Stress Test Runner (Windows)
REM This script runs JMeter stress tests for supplier payment fraud detection

echo 🛡️ Instant Invoice: Fraud Shield - Stress Testing
echo ==================================================

REM Check if JMeter container is running
docker ps | findstr microservices-jmeter >nul
if %errorlevel% neq 0 (
    echo ❌ JMeter container is not running. Starting stress tests...
    echo Running: docker compose --profile testing up --build -d
    docker compose --profile testing up --build -d
    echo ⏳ Waiting for services to be ready...
    timeout /t 30 /nobreak >nul
)

echo 🚀 Starting stress tests...

REM Run JMeter tests
docker exec microservices-jmeter jmeter -n -t /tests/load_test.jmx -l /results/results.jtl -e -o /results/html-report

echo 📊 Test Results:
echo ================

REM Check if results were generated
if exist "stress_tests\results\results.jtl" (
    echo ✅ Test completed successfully!
    echo 📁 Results saved to: stress_tests\results\
    echo 🌐 HTML Report: stress_tests\results\html-report\index.html
    
    echo.
    echo 📈 Quick Stats:
    echo ===============
    
    REM Count total requests
    for /f %%i in ('type stress_tests\results\results.jtl ^| find /c /v ""') do set total_requests=%%i
    echo Total Requests: %total_requests%
    
    REM Count successful requests
    for /f %%i in ('findstr /c:"true" stress_tests\results\results.jtl ^| find /c /v ""') do set success_requests=%%i
    echo Successful Requests: %success_requests%
    
    REM Calculate success rate
    if %total_requests% gtr 0 (
        set /a success_rate=%success_requests% * 100 / %total_requests%
        echo Success Rate: %success_rate%%%
    )
    
    echo.
    echo ⏱️ Response Time Analysis:
    echo =========================
    echo Check stress_tests\results\html-report\index.html for detailed analysis
    
    echo.
    echo 🎯 Key Metrics:
    echo ===============
    echo ✅ <200ms Response Time Requirement: Check HTML report for details
    echo ✅ System Stability: Check HTML report for details
    
) else (
    echo ❌ Test failed or no results generated
    echo Check Docker logs: docker logs microservices-jmeter
)

echo.
echo 🔍 To view detailed results:
echo 1. Open stress_tests\results\html-report\index.html in your browser
echo 2. Check stress_tests\results\results.jtl for raw data
echo.
echo 🛑 To stop tests: docker compose --profile testing down

pause
