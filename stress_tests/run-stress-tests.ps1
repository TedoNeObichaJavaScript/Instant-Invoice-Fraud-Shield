# Stress Testing Script for Instant Invoice: Fraud Shield
# PowerShell version for Windows

Write-Host "🚀 Starting Stress Tests for Instant Invoice: Fraud Shield" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green

# Create results directory with timestamp
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$resultsDir = "/tests/results/$timestamp"
New-Item -ItemType Directory -Path $resultsDir -Force | Out-Null

Write-Host "📊 Results will be saved to: $resultsDir" -ForegroundColor Yellow
Write-Host ""

# Function to run a test
function Run-Test {
    param(
        [string]$TestName,
        [string]$TestFile,
        [string]$OutputPrefix
    )
    
    Write-Host "🧪 Running $TestName..." -ForegroundColor Cyan
    Write-Host "   Test file: $TestFile" -ForegroundColor Gray
    Write-Host "   Output: ${OutputPrefix}-${timestamp}" -ForegroundColor Gray
    Write-Host ""
    
    $jtlFile = "$resultsDir/${OutputPrefix}.jtl"
    $reportDir = "$resultsDir/${OutputPrefix}-report"
    $logFile = "$resultsDir/${OutputPrefix}.log"
    
    jmeter -n -t "/tests/test-plans/$TestFile" `
           -l $jtlFile `
           -e -o $reportDir `
           -j $logFile
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ $TestName completed successfully" -ForegroundColor Green
    } else {
        Write-Host "❌ $TestName failed" -ForegroundColor Red
    }
    Write-Host ""
}

# Wait for API Gateway to be ready
Write-Host "⏳ Waiting for API Gateway to be ready..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

# Run Normal Load Test
Run-Test "Normal Load Test" "normal-load-test.jmx" "normal-load"

# Wait between tests
Write-Host "⏳ Waiting 60 seconds before extreme load test..." -ForegroundColor Yellow
Start-Sleep -Seconds 60

# Run Extreme Load Test
Run-Test "Extreme Load Test" "extreme-load-test.jmx" "extreme-load"

Write-Host "🎉 All stress tests completed!" -ForegroundColor Green
Write-Host "📁 Results saved in: $resultsDir" -ForegroundColor Yellow
Write-Host ""
Write-Host "📊 Test Summary:" -ForegroundColor Cyan
Write-Host "   - Normal Load: 50 users, 10 iterations each (500 total requests)" -ForegroundColor White
Write-Host "   - Extreme Load: 200 users, 20 iterations each, 5 minutes duration (4000+ requests)" -ForegroundColor White
Write-Host ""
Write-Host "🔍 Check the HTML reports in the results directory for detailed analysis." -ForegroundColor Yellow
