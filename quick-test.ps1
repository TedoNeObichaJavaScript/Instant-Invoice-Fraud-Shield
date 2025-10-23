# Quick Test Suite for Instant Invoice Fraud Shield
Write-Host "🚀 Starting Quick Test Suite..." -ForegroundColor Green

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

# Test 1: Docker Services
Write-Host "`n🐳 Testing Docker Services..." -ForegroundColor Cyan
try {
    $containers = docker ps --format "table {{.Names}}\t{{.Status}}" | Select-String "microservices"
    $apiGateway = $containers | Select-String "api-gateway"
    $postgres = $containers | Select-String "postgres"
    $redis = $containers | Select-String "redis"
    
    Test-Result "API Gateway Running" ($apiGateway -and $apiGateway.ToString().Contains("Up"))
    Test-Result "PostgreSQL Running" ($postgres -and $postgres.ToString().Contains("Up"))
    Test-Result "Redis Running" ($redis -and $redis.ToString().Contains("Up"))
} catch {
    Test-Result "Docker Services" $false "Docker not available"
}

# Test 2: API Health
Write-Host "`n🌐 Testing API Health..." -ForegroundColor Cyan
try {
    $healthResponse = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -Method GET -TimeoutSec 10
    Test-Result "Health Endpoint" ($healthResponse.StatusCode -eq 200)
} catch {
    Test-Result "Health Endpoint" $false "API not accessible"
}

# Test 3: Login
Write-Host "`n🔐 Testing Authentication..." -ForegroundColor Cyan
try {
    $loginBody = @{
        username = "admin"
        password = "admin123"
    } | ConvertTo-Json
    
    $loginResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json" -TimeoutSec 10
    Test-Result "Login Success" ($loginResponse.StatusCode -eq 200)
    
    # Test security headers
    $headers = $loginResponse.Headers
    Test-Result "Security Headers Present" ($headers.ContainsKey("X-Content-Type-Options"))
} catch {
    Test-Result "Authentication" $false "Login failed"
}

# Test 4: Frontend
Write-Host "`n🖥️ Testing Frontend..." -ForegroundColor Cyan
try {
    $frontendResponse = Invoke-WebRequest -Uri "http://localhost" -Method GET -TimeoutSec 10
    Test-Result "Frontend Accessible" ($frontendResponse.StatusCode -eq 200)
    Test-Result "Frontend Has Login Form" ($frontendResponse.Content -like "*login*")
} catch {
    Test-Result "Frontend" $false "Frontend not accessible"
}

# Test 5: Database
Write-Host "`n🗄️ Testing Database..." -ForegroundColor Cyan
try {
    $dbTest = docker exec microservices-postgres psql -U postgres -d microservices_db -c "SELECT COUNT(*) FROM users;" 2>$null
    Test-Result "Database Connection" ($dbTest -and $dbTest -like "*1*")
} catch {
    Test-Result "Database" $false "Database not accessible"
}

# Test 6: Redis
Write-Host "`n🔴 Testing Redis..." -ForegroundColor Cyan
try {
    $redisTest = docker exec microservices-redis redis-cli ping 2>$null
    Test-Result "Redis Connection" ($redisTest -eq "PONG")
} catch {
    Test-Result "Redis" $false "Redis not accessible"
}

# Test 7: File Structure
Write-Host "`n📁 Testing File Structure..." -ForegroundColor Cyan
$requiredFiles = @(
    "docker-compose.yml",
    "frontend/html/index.html",
    "frontend/js/app.js",
    "frontend/css/styles.css",
    "api-gateway/pom.xml"
)

foreach ($file in $requiredFiles) {
    $exists = Test-Path $file
    Test-Result "File: $file" $exists
}

# Summary
Write-Host "`n📊 Test Results Summary" -ForegroundColor Green
Write-Host "===============================================" -ForegroundColor Green
Write-Host "Total Tests: $totalTests" -ForegroundColor White
Write-Host "Passed: $passedTests" -ForegroundColor Green
Write-Host "Failed: $($totalTests - $passedTests)" -ForegroundColor Red
Write-Host "Success Rate: $([math]::Round(($passedTests / $totalTests) * 100, 2))%" -ForegroundColor Yellow

Write-Host "`nQuick Test Complete!" -ForegroundColor Green
