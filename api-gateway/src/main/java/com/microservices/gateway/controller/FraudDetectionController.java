package com.microservices.gateway.controller;

import com.microservices.gateway.model.FraudDetectionRequest;
import com.microservices.gateway.model.FraudDetectionResponse;
import com.microservices.gateway.service.FraudDetectionService;
import com.microservices.gateway.service.AuditService;
import com.microservices.gateway.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/fraud-detection")
public class FraudDetectionController {

    private final FraudDetectionService fraudDetectionService;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final JdbcTemplate jdbcTemplate;

    public FraudDetectionController(FraudDetectionService fraudDetectionService,
                                  JwtService jwtService,
                                  AuditService auditService,
                                  JdbcTemplate jdbcTemplate) {
        this.fraudDetectionService = fraudDetectionService;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.jdbcTemplate = jdbcTemplate;
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
     * Health check endpoint for the fraud detection service
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("{\"status\": \"UP\", \"service\": \"fraud-detection\"}");
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

    /**
     * Get random IBANs from the database for testing purposes
     */
    @GetMapping("/ibans/random")
    public ResponseEntity<Map<String, Object>> getRandomIBANs(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "2") int count,
            HttpServletRequest httpRequest) {
        
        long startTime = System.currentTimeMillis();
        UUID userId = null;
        Map<String, Object> response = new HashMap<>();
        int responseStatus = 500;

        try {
            // Validate JWT token
            String token = extractTokenFromHeader(authHeader);
            if (!jwtService.validateToken(token)) {
                responseStatus = 401;
                response.put("error", "Invalid or expired authentication token");
                return ResponseEntity.status(responseStatus).body(response);
            }

            userId = jwtService.getUserIdFromToken(token);

            // Limit count to prevent abuse
            if (count > 10) {
                count = 10;
            }

            // Get random IBANs from database
            String sql = "SELECT iban FROM iban_risk_lookup ORDER BY RANDOM() LIMIT ?";
            List<String> ibans = jdbcTemplate.queryForList(sql, String.class, count);

            response.put("ibans", ibans);
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
}
