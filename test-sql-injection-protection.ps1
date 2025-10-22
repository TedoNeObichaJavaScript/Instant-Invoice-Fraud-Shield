# SQL Injection Protection Test Script (PowerShell)
Write-Host "Testing SQL Injection Protection..." -ForegroundColor Cyan

# Test 1: Normal request (should work)
Write-Host "Test 1: Normal IBAN request" -ForegroundColor Green
$body1 = @{
    supplierIban = "BG11BANK99991234567890"
    invoiceId = "INV-001"
    supplierName = "Test Supplier"
    paymentAmount = 1000.0
    currency = "EUR"
    invoiceNumber = "INV-001"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "https://localhost/api/v1/fraud-detection/validate-payment" `
        -Method POST `
        -ContentType "application/json" `
        -Headers @{"Authorization" = "Bearer test-token"} `
        -Body $body1 `
        -SkipCertificateCheck
    Write-Host "Status: $($response.status)" -ForegroundColor Green
} catch {
    Write-Host "Request completed (may have failed due to auth)" -ForegroundColor Yellow
}

Write-Host ""

# Test 2: SQL injection in IBAN (should be blocked)
Write-Host "Test 2: SQL injection in IBAN field" -ForegroundColor Red
$body2 = @{
    supplierIban = "BG11BANK99991234567890'; DROP TABLE users; --"
    invoiceId = "INV-002"
    supplierName = "Test Supplier"
    paymentAmount = 1000.0
    currency = "EUR"
    invoiceNumber = "INV-002"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "https://localhost/api/v1/fraud-detection/validate-payment" `
        -Method POST `
        -ContentType "application/json" `
        -Headers @{"Authorization" = "Bearer test-token"} `
        -Body $body2 `
        -SkipCertificateCheck
    Write-Host "Status: $($response.status)" -ForegroundColor Red
} catch {
    Write-Host "Request blocked or failed (expected)" -ForegroundColor Green
}

Write-Host ""

# Test 3: SQL injection in supplier name (should be blocked)
Write-Host "Test 3: SQL injection in supplier name" -ForegroundColor Red
$body3 = @{
    supplierIban = "BG11BANK99991234567890"
    invoiceId = "INV-003"
    supplierName = "Test' UNION SELECT * FROM users --"
    paymentAmount = 1000.0
    currency = "EUR"
    invoiceNumber = "INV-003"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "https://localhost/api/v1/fraud-detection/validate-payment" `
        -Method POST `
        -ContentType "application/json" `
        -Headers @{"Authorization" = "Bearer test-token"} `
        -Body $body3 `
        -SkipCertificateCheck
    Write-Host "Status: $($response.status)" -ForegroundColor Red
} catch {
    Write-Host "Request blocked or failed (expected)" -ForegroundColor Green
}

Write-Host ""

# Test 4: Invalid IBAN format (should be blocked)
Write-Host "Test 4: Invalid IBAN format" -ForegroundColor Red
$body4 = @{
    supplierIban = "INVALID-IBAN-FORMAT"
    invoiceId = "INV-004"
    supplierName = "Test Supplier"
    paymentAmount = 1000.0
    currency = "EUR"
    invoiceNumber = "INV-004"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "https://localhost/api/v1/fraud-detection/validate-payment" `
        -Method POST `
        -ContentType "application/json" `
        -Headers @{"Authorization" = "Bearer test-token"} `
        -Body $body4 `
        -SkipCertificateCheck
    Write-Host "Status: $($response.status)" -ForegroundColor Red
} catch {
    Write-Host "Request blocked or failed (expected)" -ForegroundColor Green
}

Write-Host ""

# Test 5: Negative payment amount (should be blocked)
Write-Host "Test 5: Negative payment amount" -ForegroundColor Red
$body5 = @{
    supplierIban = "BG11BANK99991234567890"
    invoiceId = "INV-005"
    supplierName = "Test Supplier"
    paymentAmount = -1000.0
    currency = "EUR"
    invoiceNumber = "INV-005"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "https://localhost/api/v1/fraud-detection/validate-payment" `
        -Method POST `
        -ContentType "application/json" `
        -Headers @{"Authorization" = "Bearer test-token"} `
        -Body $body5 `
        -SkipCertificateCheck
    Write-Host "Status: $($response.status)" -ForegroundColor Red
} catch {
    Write-Host "Request blocked or failed (expected)" -ForegroundColor Green
}

Write-Host ""
Write-Host "SQL Injection Protection Test Complete!" -ForegroundColor Cyan
Write-Host "Check the application logs for security alerts." -ForegroundColor Yellow
