# Simple SQL Injection Test Suite
# Tests endpoints for SQL injection vulnerabilities

Write-Host "=== SQL INJECTION TEST SUITE ===" -ForegroundColor Red
Write-Host "Testing endpoints for SQL injection vulnerabilities..." -ForegroundColor Yellow

$baseUrl = "http://localhost:8080"
$testResults = @()

# Simple test cases for SQL injection
$sqlTests = @(
    "'; DROP TABLE users; --",
    "' OR '1'='1",
    "' UNION SELECT * FROM users --",
    "' OR 1=1 --",
    "'; EXEC xp_cmdshell('dir'); --",
    "' UNION SELECT password FROM users --",
    "'; WAITFOR DELAY '00:00:05'; --",
    "' AND (SELECT COUNT(*) FROM users) > 0 --",
    "' UNION SELECT NULL,username,password FROM users --",
    "'; DROP TABLE test; --",
    "0x27204f5220313d3120",
    "/* comment */",
    "-- comment",
    "# comment",
    "'; SELECT @@version; --",
    "' UNION SELECT * FROM information_schema.tables --",
    "<script>alert('xss')</script>",
    "javascript:alert('xss')",
    "'",
    '"',
    ";",
    "--",
    "/*",
    "*/"
)

# Function to test an endpoint
function Test-Endpoint {
    param(
        [string]$Method,
        [string]$Url,
        [string]$Body = $null,
        [hashtable]$Headers = @{},
        [string]$TestName
    )
    
    try {
        $params = @{
            Uri = $Url
            Method = $Method
            Headers = $Headers
            TimeoutSec = 10
        }
        
        if ($Body) {
            $params.Body = $Body
            $params.ContentType = "application/json"
        }
        
        $response = Invoke-WebRequest @params
        $statusCode = $response.StatusCode
        
        # Check for SQL injection indicators
        $isVulnerable = $false
        $indicators = @()
        
        if ($response.Content -match "error|exception|sql|mysql|postgresql|database|syntax") {
            $isVulnerable = $true
            $indicators += "Database error in response"
        }
        
        if ($response.Content -match "information_schema|pg_tables|mysql\.user") {
            $isVulnerable = $true
            $indicators += "System table data exposed"
        }
        
        if ($response.Content -match "version|@@version|user\(\)|database\(\)") {
            $isVulnerable = $true
            $indicators += "System information exposed"
        }
        
        if ($statusCode -eq 500) {
            $isVulnerable = $true
            $indicators += "Server error (possible SQL injection)"
        }
        
        return @{
            TestName = $testName
            Url = $Url
            StatusCode = $statusCode
            IsVulnerable = $isVulnerable
            Indicators = $indicators
            ResponseLength = $response.Content.Length
        }
    }
    catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        return @{
            TestName = $testName
            Url = $Url
            StatusCode = $statusCode
            IsVulnerable = $true
            Indicators = @("Request failed: $($_.Exception.Message)")
            ResponseLength = 0
        }
    }
}

Write-Host "`n1. Testing Authentication Endpoint..." -ForegroundColor Cyan

# Test login endpoint with SQL injection
foreach ($test in $sqlTests) {
    $testName = "Login SQL Injection: $($test.Substring(0, [Math]::Min(30, $test.Length)))"
    $body = @{
        username = $test
        password = "password123"
        rememberMe = $false
    } | ConvertTo-Json
    
    $result = Test-Endpoint -Method "POST" -Url "$baseUrl/api/auth/login" -Body $body -TestName $testName
    $testResults += $result
    
    if ($result.IsVulnerable) {
        Write-Host "  VULNERABLE: $($result.TestName)" -ForegroundColor Red
        Write-Host "    Indicators: $($result.Indicators -join ', ')" -ForegroundColor Yellow
    } else {
        Write-Host "  SAFE: $($result.TestName)" -ForegroundColor Green
    }
}

Write-Host "`n2. Testing Fraud Detection Endpoint..." -ForegroundColor Cyan

# Test fraud detection endpoint with SQL injection
foreach ($test in $sqlTests) {
    $testName = "Fraud Detection SQL Injection: $($test.Substring(0, [Math]::Min(30, $test.Length)))"
    $body = @{
        invoiceId = "INV-123"
        supplierIban = $test
        supplierName = "Test Supplier"
        amount = 1000.00
        paymentPurpose = "Test Payment"
    } | ConvertTo-Json
    
    $result = Test-Endpoint -Method "POST" -Url "$baseUrl/api/v1/fraud-detection/validate-payment" -Body $body -TestName $testName
    $testResults += $result
    
    if ($result.IsVulnerable) {
        Write-Host "  VULNERABLE: $($result.TestName)" -ForegroundColor Red
        Write-Host "    Indicators: $($result.Indicators -join ', ')" -ForegroundColor Yellow
    } else {
        Write-Host "  SAFE: $($result.TestName)" -ForegroundColor Green
    }
}

Write-Host "`n3. Testing IBAN Random Endpoint..." -ForegroundColor Cyan

# Test IBAN random endpoint with SQL injection in count parameter
$countTests = @("1; DROP TABLE users; --", "1' OR '1'='1", "1 UNION SELECT * FROM users", "1; INSERT INTO test VALUES ('hacked'); --")

foreach ($test in $countTests) {
    $testName = "IBAN Random SQL Injection: $test"
    $result = Test-Endpoint -Method "GET" -Url "$baseUrl/api/v1/fraud-detection/ibans/random?count=$test" -TestName $testName
    $testResults += $result
    
    if ($result.IsVulnerable) {
        Write-Host "  VULNERABLE: $($result.TestName)" -ForegroundColor Red
        Write-Host "    Indicators: $($result.Indicators -join ', ')" -ForegroundColor Yellow
    } else {
        Write-Host "  SAFE: $($result.TestName)" -ForegroundColor Green
    }
}

Write-Host "`n4. Testing Generate Payment Endpoint..." -ForegroundColor Cyan

# Test generate payment endpoint with SQL injection in count parameter
foreach ($test in $countTests) {
    $testName = "Generate Payment SQL Injection: $test"
    $result = Test-Endpoint -Method "POST" -Url "$baseUrl/api/v1/fraud-detection/generate-payment?count=$test" -TestName $testName
    $testResults += $result
    
    if ($result.IsVulnerable) {
        Write-Host "  VULNERABLE: $($result.TestName)" -ForegroundColor Red
        Write-Host "    Indicators: $($result.Indicators -join ', ')" -ForegroundColor Yellow
    } else {
        Write-Host "  SAFE: $($result.TestName)" -ForegroundColor Green
    }
}

# Summary
Write-Host "`n=== TEST SUMMARY ===" -ForegroundColor Red
$totalTests = $testResults.Count
$vulnerableTests = ($testResults | Where-Object { $_.IsVulnerable }).Count
$safeTests = $totalTests - $vulnerableTests

Write-Host "Total Tests: $totalTests" -ForegroundColor White
Write-Host "Vulnerable: $vulnerableTests" -ForegroundColor Red
Write-Host "Safe: $safeTests" -ForegroundColor Green

if ($vulnerableTests -gt 0) {
    Write-Host "`nVULNERABILITIES FOUND:" -ForegroundColor Red
    $testResults | Where-Object { $_.IsVulnerable } | ForEach-Object {
        Write-Host "  - $($_.TestName)" -ForegroundColor Yellow
        Write-Host "    URL: $($_.Url)" -ForegroundColor Gray
        Write-Host "    Status: $($_.StatusCode)" -ForegroundColor Gray
        Write-Host "    Indicators: $($_.Indicators -join ', ')" -ForegroundColor Gray
        Write-Host ""
    }
} else {
    Write-Host "`n✅ ALL TESTS PASSED - NO SQL INJECTION VULNERABILITIES FOUND!" -ForegroundColor Green
}

Write-Host "`n=== RECOMMENDATIONS ===" -ForegroundColor Cyan
Write-Host "1. Use parameterized queries for all database operations" -ForegroundColor White
Write-Host "2. Validate and sanitize all user inputs" -ForegroundColor White
Write-Host "3. Implement proper error handling (don't expose database errors)" -ForegroundColor White
Write-Host "4. Use least privilege principle for database connections" -ForegroundColor White
Write-Host "5. Regular security testing and code reviews" -ForegroundColor White
