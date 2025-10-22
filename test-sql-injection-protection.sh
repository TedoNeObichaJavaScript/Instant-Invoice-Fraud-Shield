#!/bin/bash

# SQL Injection Protection Test Script
echo "🔒 Testing SQL Injection Protection..."

# Test 1: Normal request (should work)
echo "✅ Test 1: Normal IBAN request"
curl -s -X POST "https://localhost/api/v1/fraud-detection/validate-payment" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{
    "supplierIban": "BG11BANK99991234567890",
    "invoiceId": "INV-001",
    "supplierName": "Test Supplier",
    "paymentAmount": 1000.0,
    "currency": "EUR",
    "invoiceNumber": "INV-001"
  }' \
  -k | jq '.status' 2>/dev/null || echo "Request completed"

echo ""

# Test 2: SQL injection in IBAN (should be blocked)
echo "🚫 Test 2: SQL injection in IBAN field"
curl -s -X POST "https://localhost/api/v1/fraud-detection/validate-payment" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{
    "supplierIban": "BG11BANK99991234567890'\''; DROP TABLE users; --",
    "invoiceId": "INV-002",
    "supplierName": "Test Supplier",
    "paymentAmount": 1000.0,
    "currency": "EUR",
    "invoiceNumber": "INV-002"
  }' \
  -k | jq '.status' 2>/dev/null || echo "Request completed"

echo ""

# Test 3: SQL injection in supplier name (should be blocked)
echo "🚫 Test 3: SQL injection in supplier name"
curl -s -X POST "https://localhost/api/v1/fraud-detection/validate-payment" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{
    "supplierIban": "BG11BANK99991234567890",
    "invoiceId": "INV-003",
    "supplierName": "Test'\'' UNION SELECT * FROM users --",
    "paymentAmount": 1000.0,
    "currency": "EUR",
    "invoiceNumber": "INV-003"
  }' \
  -k | jq '.status' 2>/dev/null || echo "Request completed"

echo ""

# Test 4: SQL injection in invoice number (should be blocked)
echo "🚫 Test 4: SQL injection in invoice number"
curl -s -X POST "https://localhost/api/v1/fraud-detection/validate-payment" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{
    "supplierIban": "BG11BANK99991234567890",
    "invoiceId": "INV-004",
    "supplierName": "Test Supplier",
    "paymentAmount": 1000.0,
    "currency": "EUR",
    "invoiceNumber": "INV-004'\'' OR 1=1 --"
  }' \
  -k | jq '.status' 2>/dev/null || echo "Request completed"

echo ""

# Test 5: Invalid IBAN format (should be blocked)
echo "🚫 Test 5: Invalid IBAN format"
curl -s -X POST "https://localhost/api/v1/fraud-detection/validate-payment" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{
    "supplierIban": "INVALID-IBAN-FORMAT",
    "invoiceId": "INV-005",
    "supplierName": "Test Supplier",
    "paymentAmount": 1000.0,
    "currency": "EUR",
    "invoiceNumber": "INV-005"
  }' \
  -k | jq '.status' 2>/dev/null || echo "Request completed"

echo ""

# Test 6: Negative payment amount (should be blocked)
echo "🚫 Test 6: Negative payment amount"
curl -s -X POST "https://localhost/api/v1/fraud-detection/validate-payment" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{
    "supplierIban": "BG11BANK99991234567890",
    "invoiceId": "INV-006",
    "supplierName": "Test Supplier",
    "paymentAmount": -1000.0,
    "currency": "EUR",
    "invoiceNumber": "INV-006"
  }' \
  -k | jq '.status' 2>/dev/null || echo "Request completed"

echo ""
echo "🔒 SQL Injection Protection Test Complete!"
echo "Check the application logs for security alerts."
