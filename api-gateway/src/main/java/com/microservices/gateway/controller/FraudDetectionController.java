package com.microservices.gateway.controller;

import com.microservices.gateway.model.FraudDetectionRequest;
import com.microservices.gateway.model.FraudDetectionResponse;
import com.microservices.gateway.service.FraudDetectionService;
import com.microservices.gateway.service.AuditService;
import com.microservices.gateway.service.JwtService;
import com.microservices.gateway.service.SqlInjectionProtectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/fraud-detection")
public class FraudDetectionController {

    private final FraudDetectionService fraudDetectionService;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final JdbcTemplate jdbcTemplate;
    private final SqlInjectionProtectionService sqlInjectionProtection;

    public FraudDetectionController(FraudDetectionService fraudDetectionService,
                                  JwtService jwtService,
                                  AuditService auditService,
                                  JdbcTemplate jdbcTemplate,
                                  SqlInjectionProtectionService sqlInjectionProtection) {
        this.fraudDetectionService = fraudDetectionService;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.jdbcTemplate = jdbcTemplate;
        this.sqlInjectionProtection = sqlInjectionProtection;
    }

    /**
     * Validate a payment transaction for fraud before it's processed
     * This is the main API endpoint that external systems call
     */
    @PostMapping("/validate-payment")
    public ResponseEntity<FraudDetectionResponse> validatePayment(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody FraudDetectionRequest request,
            HttpServletRequest httpRequest) {
        
        long startTime = System.currentTimeMillis();
        UUID userId = null;
        FraudDetectionResponse response = null;
        int responseStatus = 500;

        try {
            // SQL injection protection for request data
            if (request.getSupplierIban() != null && !sqlInjectionProtection.isValidIban(request.getSupplierIban())) {
                responseStatus = 400;
                response = FraudDetectionResponse.builder()
                    .invoiceId(request.getInvoiceId())
                    .riskStatus("ERROR")
                    .reason("Invalid IBAN format")
                    .build();
                sqlInjectionProtection.logSecurityEvent("SQL_INJECTION_ATTEMPT", "Invalid IBAN: " + request.getSupplierIban());
                return ResponseEntity.status(responseStatus).body(response);
            }
            
            if (request.getSupplierName() != null && !sqlInjectionProtection.isInputSafe(request.getSupplierName())) {
                responseStatus = 400;
                response = FraudDetectionResponse.builder()
                    .invoiceId(request.getInvoiceId())
                    .riskStatus("ERROR")
                    .reason("Invalid supplier name format")
                    .build();
                sqlInjectionProtection.logSecurityEvent("SQL_INJECTION_ATTEMPT", "Invalid supplier name: " + request.getSupplierName());
                return ResponseEntity.status(responseStatus).body(response);
            }

            // Validate JWT token if provided
            if (authHeader != null && !authHeader.isEmpty()) {
                String token = extractTokenFromHeader(authHeader);
                if (!jwtService.validateToken(token)) {
                    responseStatus = 401;
                    response = FraudDetectionResponse.builder()
                        .invoiceId(request.getInvoiceId())
                        .riskStatus("ERROR")
                        .reason("Invalid or expired authentication token")
                        .build();
                    
                    auditService.logRequest(null, httpRequest.getRequestURI(), "POST", 
                        getClientIpAddress(httpRequest), httpRequest.getHeader("User-Agent"), 
                        request.toString(), responseStatus, (int)(System.currentTimeMillis() - startTime));
                    
                    return ResponseEntity.status(responseStatus).body(response);
                }

                userId = jwtService.getUserIdFromToken(token);
            }

            // Perform fraud detection analysis
            response = fraudDetectionService.analyzePayment(request);
            responseStatus = 200;

            // Log successful validation
            auditService.logRequest(userId, httpRequest.getRequestURI(), "POST", 
                getClientIpAddress(httpRequest), httpRequest.getHeader("User-Agent"), 
                request.toString(), responseStatus, (int)(System.currentTimeMillis() - startTime));

        } catch (IllegalArgumentException e) {
            responseStatus = 401;
            response = FraudDetectionResponse.builder()
                .invoiceId(request.getInvoiceId())
                .riskStatus("ERROR")
                .reason("Invalid authorization header")
                .build();
        } catch (Exception e) {
            responseStatus = 500;
            response = FraudDetectionResponse.builder()
                .invoiceId(request.getInvoiceId())
                .riskStatus("ERROR")
                .reason("Internal server error: " + e.getMessage())
                .build();
        } finally {
            // Log the request
            if (response == null) {
                response = FraudDetectionResponse.builder()
                    .invoiceId(request.getInvoiceId())
                    .riskStatus("ERROR")
                    .reason("Unknown error occurred")
                    .build();
            }
            
            auditService.logRequest(userId, httpRequest.getRequestURI(), "POST", 
                getClientIpAddress(httpRequest), httpRequest.getHeader("User-Agent"), 
                request.toString(), responseStatus, (int)(System.currentTimeMillis() - startTime));
        }

        return ResponseEntity.status(responseStatus).body(response);
    }

    /**
     * Analyze a payment for fraud detection (alias for validate-payment)
     */
    @PostMapping("/analyze")
    public ResponseEntity<FraudDetectionResponse> analyzePayment(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody FraudDetectionRequest request,
            HttpServletRequest httpRequest) {
        return validatePayment(authHeader, request, httpRequest);
    }

    /**
     * Generate sample payment data for testing
     */
    @PostMapping("/generate-payment")
    public ResponseEntity<Map<String, Object>> generatePayment(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "1") int count,
            HttpServletRequest httpRequest) {
        
        long startTime = System.currentTimeMillis();
        UUID userId = null;
        Map<String, Object> response = new HashMap<>();
        int responseStatus = 500;

        try {
            // Input validation and SQL injection protection
            if (count < 1) {
                responseStatus = 400;
                response.put("error", "Count must be at least 1");
                return ResponseEntity.status(responseStatus).body(response);
            }
            
            if (count > 5) {
                responseStatus = 400;
                response.put("error", "Count cannot exceed 5");
                return ResponseEntity.status(responseStatus).body(response);
            }

            // Validate JWT token if provided (optional for testing)
            if (authHeader != null && !authHeader.isEmpty()) {
                String token = extractTokenFromHeader(authHeader);
                if (!jwtService.validateToken(token)) {
                    responseStatus = 401;
                    response.put("error", "Invalid or expired authentication token");
                    return ResponseEntity.status(responseStatus).body(response);
                }
                userId = jwtService.getUserIdFromToken(token);
            }

            List<Map<String, Object>> payments = new ArrayList<>();
            
            // Get random IBANs with risk levels
            String sql = "SELECT iban, risk_level FROM risk.iban_risk_lookup ORDER BY RANDOM() LIMIT ?";
            List<Map<String, Object>> ibanData = jdbcTemplate.queryForList(sql, count);
            
            for (Map<String, Object> row : ibanData) {
                Map<String, Object> payment = new HashMap<>();
                payment.put("invoiceId", "INV-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000));
                payment.put("supplierName", "Test Supplier " + (int)(Math.random() * 100));
                payment.put("amount", Math.round((Math.random() * 10000 + 100) * 100.0) / 100.0);
                payment.put("iban", row.get("iban"));
                payment.put("description", "Generated test payment");
                payment.put("riskLevel", row.get("risk_level"));
                payments.add(payment);
            }

            response.put("payments", payments);
            response.put("count", payments.size());
            responseStatus = 200;

            // Log the request
            auditService.logRequest(userId, httpRequest.getRequestURI(), "POST", 
                getClientIpAddress(httpRequest), httpRequest.getHeader("User-Agent"), 
                "count=" + count, responseStatus, (int)(System.currentTimeMillis() - startTime));

        } catch (IllegalArgumentException e) {
            responseStatus = 401;
            response.put("error", "Invalid authorization header");
        } catch (Exception e) {
            responseStatus = 500;
            response.put("error", "Internal server error: " + e.getMessage());
        }

        return ResponseEntity.status(responseStatus).body(response);
    }

    /**
     * Validate a payment (alias for validate-payment)
     */
    @PostMapping("/validate")
    public ResponseEntity<FraudDetectionResponse> validate(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody FraudDetectionRequest request,
            HttpServletRequest httpRequest) {
        return validatePayment(authHeader, request, httpRequest);
    }

    /**
     * Health check endpoint for the fraud detection service
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("{\"status\": \"UP\", \"service\": \"fraud-detection\"}");
    }

    /**
     * Get random IBANs from the database for testing purposes
     */
    @GetMapping("/ibans/random")
    public ResponseEntity<Map<String, Object>> getRandomIBANs(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "2") int count,
            HttpServletRequest httpRequest) {
        
        long startTime = System.currentTimeMillis();
        UUID userId = null;
        Map<String, Object> response = new HashMap<>();
        int responseStatus = 500;

        try {
            // Input validation and SQL injection protection
            if (count < 1) {
                responseStatus = 400;
                response.put("error", "Count must be at least 1");
                return ResponseEntity.status(responseStatus).body(response);
            }
            
            if (count > 10) {
                responseStatus = 400;
                response.put("error", "Count cannot exceed 10");
                return ResponseEntity.status(responseStatus).body(response);
            }

            // Validate JWT token if provided (optional for testing)
            if (authHeader != null && !authHeader.isEmpty()) {
                String token = extractTokenFromHeader(authHeader);
                if (!jwtService.validateToken(token)) {
                    responseStatus = 401;
                    response.put("error", "Invalid or expired authentication token");
                    return ResponseEntity.status(responseStatus).body(response);
                }
                userId = jwtService.getUserIdFromToken(token);
            } else {
                // For testing purposes, allow unauthenticated access
                userId = null;
            }

            // Get random IBANs from database with their risk levels for realistic testing
            String sql = "SELECT iban, risk_level FROM risk.iban_risk_lookup ORDER BY RANDOM() LIMIT ?";
            List<Map<String, Object>> ibanData = jdbcTemplate.queryForList(sql, count);
            
            List<String> ibans = new ArrayList<>();
            List<String> riskLevels = new ArrayList<>();
            
            for (Map<String, Object> row : ibanData) {
                ibans.add((String) row.get("iban"));
                riskLevels.add((String) row.get("risk_level"));
            }

            response.put("ibans", ibans);
            response.put("riskLevels", riskLevels);
            response.put("count", ibans.size());
            responseStatus = 200;

            // Log the request
            auditService.logRequest(userId, httpRequest.getRequestURI(), "GET", 
                getClientIpAddress(httpRequest), httpRequest.getHeader("User-Agent"), 
                "count=" + count, responseStatus, (int)(System.currentTimeMillis() - startTime));

        } catch (IllegalArgumentException e) {
            responseStatus = 401;
            response.put("error", "Invalid authorization header");
        } catch (Exception e) {
            responseStatus = 500;
            response.put("error", "Internal server error: " + e.getMessage());
        }

        return ResponseEntity.status(responseStatus).body(response);
    }

    private String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid authorization header");
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}