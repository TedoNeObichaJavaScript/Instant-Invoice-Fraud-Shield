# Comprehensive Test Suite for Instant Invoice Fraud Shield
# This script tests all aspects of the application

Write-Host "🚀 Starting Comprehensive Test Suite..." -ForegroundColor Green
Write-Host "===============================================" -ForegroundColor Green

$testResults = @()
$totalTests = 0
$passedTests = 0

function Test-Result {
    param($testName, $success, $details = "")
    $totalTests++
    if ($success) {
        $passedTests++
        Write-Host "✅ $testName" -ForegroundColor Green
    } else {
        Write-Host "❌ $testName" -ForegroundColor Red
        if ($details) { Write-Host "   Details: $details" -ForegroundColor Yellow }
    }
    $script:testResults += [PSCustomObject]@{
        Test = $testName
        Passed = $success
        Details = $details
    }
}

# Test 1: Docker Services Health
Write-Host "`n🐳 Testing Docker Services..." -ForegroundColor Cyan
try {
    $containers = docker ps --format "table {{.Names}}\t{{.Status}}" | Select-String "microservices"
    $apiGateway = $containers | Select-String "api-gateway"
    $postgres = $containers | Select-String "postgres"
    $redis = $containers | Select-String "redis"
    
    Test-Result "API Gateway Container Running" ($apiGateway -and $apiGateway.ToString().Contains("Up"))
    Test-Result "PostgreSQL Container Running" ($postgres -and $postgres.ToString().Contains("Up"))
    Test-Result "Redis Container Running" ($redis -and $redis.ToString().Contains("Up"))
} catch {
    Test-Result "Docker Services Check" $false "Docker not available or containers not running"
}

# Test 2: API Endpoints
Write-Host "`n🌐 Testing API Endpoints..." -ForegroundColor Cyan
try {
    # Health Check
    $healthResponse = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -Method GET -TimeoutSec 10
    Test-Result "Health Endpoint" ($healthResponse.StatusCode -eq 200)
    
    # Login Test
    $loginBody = @{
        username = "admin"
        password = "admin123"
    } | ConvertTo-Json
    
    $loginResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json" -TimeoutSec 10
    Test-Result "Login Endpoint" ($loginResponse.StatusCode -eq 200)
    
    # Extract token for authenticated tests
    $loginData = $loginResponse.Content | ConvertFrom-Json
    $token = $loginData.token
    
    # Fraud Detection Test
    $fraudBody = @{
        invoiceId = "TEST-001"
        supplierIban = "BG13UNCR0027956363"
        amount = 100.50
        supplierName = "Test Supplier"
        supplierCountry = "Bulgaria"
        paymentPurpose = "Test Payment"
    } | ConvertTo-Json
    
    $fraudResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/fraud-detection/validate-payment" -Method POST -Body $fraudBody -ContentType "application/json" -TimeoutSec 10
    Test-Result "Fraud Detection Endpoint" ($fraudResponse.StatusCode -eq 200)
    
    # Analytics Test
    $analyticsResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/analytics/dashboard" -Method GET -Headers @{"Authorization" = "Bearer $token"} -TimeoutSec 10
    Test-Result "Analytics Endpoint" ($analyticsResponse.StatusCode -eq 200)
    
} catch {
    Test-Result "API Endpoints" $false "API not accessible: $($_.Exception.Message)"
}

# Test 3: Security Headers
Write-Host "`n🔒 Testing Security Headers..." -ForegroundColor Cyan
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json" -TimeoutSec 10
    $headers = $response.Headers
    
    Test-Result "X-Content-Type-Options Header" ($headers.ContainsKey("X-Content-Type-Options"))
    Test-Result "X-Frame-Options Header" ($headers.ContainsKey("X-Frame-Options"))
    Test-Result "X-XSS-Protection Header" ($headers.ContainsKey("X-XSS-Protection"))
    Test-Result "Cache-Control Header" ($headers.ContainsKey("Cache-Control"))
    
} catch {
    Test-Result "Security Headers" $false "Could not test headers: $($_.Exception.Message)"
}

# Test 4: Input Validation
Write-Host "`n🛡️ Testing Input Validation..." -ForegroundColor Cyan
try {
    # XSS Test
    $xssBody = @{
        username = "<script>alert('xss')</script>"
        password = "test"
    } | ConvertTo-Json
    
    $xssResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" -Method POST -Body $xssBody -ContentType "application/json" -TimeoutSec 10
    Test-Result "XSS Protection" ($xssResponse.StatusCode -eq 400 -or $xssResponse.StatusCode -eq 401)
    
    # SQL Injection Test
    $sqlUsername = "admin'; DROP TABLE users; --"
    $sqlBody = @{
        username = $sqlUsername
        password = "test"
    } | ConvertTo-Json
    
    $sqlResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" -Method POST -Body $sqlBody -ContentType "application/json" -TimeoutSec 10
    Test-Result "SQL Injection Protection" ($sqlResponse.StatusCode -eq 400 -or $sqlResponse.StatusCode -eq 401)
    
} catch {
    Test-Result "Input Validation" $false "Could not test input validation: $($_.Exception.Message)"
}

# Test 5: Rate Limiting
Write-Host "`n⚡ Testing Rate Limiting..." -ForegroundColor Cyan
try {
    $rateLimitHit = $false
    for ($i = 1; $i -le 10; $i++) {
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json" -TimeoutSec 5
            if ($response.StatusCode -eq 429) {
                $rateLimitHit = $true
                break
            }
        } catch {
            if ($_.Exception.Message -like "*429*") {
                $rateLimitHit = $true
                break
            }
        }
        Start-Sleep -Milliseconds 100
    }
    Test-Result "Rate Limiting Active" $rateLimitHit
} catch {
    Test-Result "Rate Limiting" $false "Could not test rate limiting: $($_.Exception.Message)"
}

# Test 6: Frontend Accessibility
Write-Host "`n🖥️ Testing Frontend..." -ForegroundColor Cyan
try {
    $frontendResponse = Invoke-WebRequest -Uri "http://localhost" -Method GET -TimeoutSec 10
    Test-Result "Frontend Accessible" ($frontendResponse.StatusCode -eq 200)
    Test-Result "Frontend Contains Login Form" ($frontendResponse.Content -like "*login*")
    Test-Result "Frontend Contains Dashboard" ($frontendResponse.Content -like "*dashboard*")
} catch {
    Test-Result "Frontend" $false "Frontend not accessible: $($_.Exception.Message)"
}

# Test 7: Database Connectivity
Write-Host "`n🗄️ Testing Database..." -ForegroundColor Cyan
try {
    $dbTest = docker exec microservices-postgres psql -U postgres -d microservices_db -c "SELECT COUNT(*) FROM users;" 2>$null
    Test-Result "Database Connection" ($dbTest -and $dbTest -like "*1*")
    
    $ibanTest = docker exec microservices-postgres psql -U postgres -d microservices_db -c "SELECT COUNT(*) FROM iban_risk_lookup;" 2>$null
    Test-Result "IBAN Data Available" ($ibanTest -and $ibanTest -like "*1000000*")
} catch {
    Test-Result "Database" $false "Database not accessible: $($_.Exception.Message)"
}

# Test 8: Redis Connectivity
Write-Host "`n🔴 Testing Redis..." -ForegroundColor Cyan
try {
    $redisTest = docker exec microservices-redis redis-cli ping 2>$null
    Test-Result "Redis Connection" ($redisTest -eq "PONG")
} catch {
    Test-Result "Redis" $false "Redis not accessible: $($_.Exception.Message)"
}

# Test 9: File Structure
Write-Host "`n📁 Testing File Structure..." -ForegroundColor Cyan
$requiredFiles = @(
    "Instant-Invoice-Fraud-Shield/docker-compose.yml",
    "Instant-Invoice-Fraud-Shield/frontend/html/index.html",
    "Instant-Invoice-Fraud-Shield/frontend/js/app.js",
    "Instant-Invoice-Fraud-Shield/frontend/css/styles.css",
    "Instant-Invoice-Fraud-Shield/api-gateway/pom.xml",
    "Instant-Invoice-Fraud-Shield/accounts-service/pom.xml"
)

foreach ($file in $requiredFiles) {
    $exists = Test-Path $file
    Test-Result "File Exists: $file" $exists
}

# Test 10: Configuration Files
Write-Host "`n⚙️ Testing Configuration..." -ForegroundColor Cyan
try {
    $dockerCompose = Get-Content "Instant-Invoice-Fraud-Shield/docker-compose.yml" -Raw
    Test-Result "Docker Compose Valid" ($dockerCompose -like "*api-gateway*" -and $dockerCompose -like "*postgres*" -and $dockerCompose -like "*redis*")
    
    $appConfig = Get-Content "Instant-Invoice-Fraud-Shield/api-gateway/src/main/resources/application.yml" -Raw
    Test-Result "Application Config Valid" ($appConfig -like "*jwt*" -and $appConfig -like "*database*")
} catch {
    Test-Result "Configuration" $false "Configuration files not accessible: $($_.Exception.Message)"
}

# Summary
Write-Host "`n📊 Test Results Summary" -ForegroundColor Green
Write-Host "===============================================" -ForegroundColor Green
Write-Host "Total Tests: $totalTests" -ForegroundColor White
Write-Host "Passed: $passedTests" -ForegroundColor Green
Write-Host "Failed: $($totalTests - $passedTests)" -ForegroundColor Red
Write-Host "Success Rate: $([math]::Round(($passedTests / $totalTests) * 100, 2))%" -ForegroundColor Yellow

# Detailed Results
Write-Host "`n📋 Detailed Results:" -ForegroundColor Cyan
$testResults | Format-Table -AutoSize

# Performance Test
Write-Host "`n⚡ Performance Test..." -ForegroundColor Cyan
try {
    $startTime = Get-Date
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json" -TimeoutSec 10
    $endTime = Get-Date
    $responseTime = ($endTime - $startTime).TotalMilliseconds
    Test-Result "API Response Time < 1000ms" ($responseTime -lt 1000) "Response time: $([math]::Round($responseTime, 2))ms"
} catch {
    Test-Result "Performance Test" $false "Could not measure performance: $($_.Exception.Message)"
}

Write-Host "`n🎉 Test Suite Complete!" -ForegroundColor Green
