package com.microservices.accounts.controller;

import com.microservices.accounts.model.SupplierPaymentRequest;
import com.microservices.accounts.model.FraudDetectionResponse;
import com.microservices.accounts.service.ApiKeyValidationService;
import com.microservices.accounts.service.SupplierFraudDetectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/suppliers")
public class SupplierPaymentController {

    private final SupplierFraudDetectionService fraudDetectionService;
    private final ApiKeyValidationService apiKeyValidationService;

    public SupplierPaymentController(SupplierFraudDetectionService fraudDetectionService,
                                  ApiKeyValidationService apiKeyValidationService) {
        this.fraudDetectionService = fraudDetectionService;
        this.apiKeyValidationService = apiKeyValidationService;
    }

    @PostMapping("/payment-validation")
    public ResponseEntity<Map<String, Object>> validateSupplierPayment(@RequestBody SupplierPaymentRequest request,
                                                                     @RequestHeader("X-API-KEY") String apiKey,
                                                                     HttpServletRequest httpRequest) {
        
        // Validate API key
        if (!apiKeyValidationService.validateApiKey(apiKey)) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Invalid API key");
            response.put("status", "UNAUTHORIZED");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // Validate request
        if (request.getSupplierIban() == null || request.getSupplierIban().trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Supplier IBAN is required");
            response.put("status", "BAD_REQUEST");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            // Perform fraud detection
            FraudDetectionResponse detection = fraudDetectionService.detectFraud(request);
            
            // Check if response time is acceptable
            boolean acceptableResponseTime = fraudDetectionService.isResponseTimeAcceptable(detection.getResponseTimeMs());
            
            Map<String, Object> response = new HashMap<>();
            response.put("invoiceId", detection.getInvoiceId());
            response.put("supplierIban", detection.getSupplierIban());
            response.put("supplierName", detection.getSupplierName());
            response.put("fraudStatus", detection.getFraudStatus());
            response.put("riskLevel", detection.getRiskLevel());
            response.put("anomalies", detection.getAnomalies());
            response.put("recommendation", detection.getRecommendation());
            response.put("responseTimeMs", detection.getResponseTimeMs());
            response.put("acceptableResponseTime", acceptableResponseTime);
            response.put("timestamp", detection.getTimestamp());
            response.put("status", "SUCCESS");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Fraud detection failed");
            response.put("message", e.getMessage());
            response.put("status", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck(@RequestHeader("X-API-KEY") String apiKey) {
        
        // Validate API key
        if (!apiKeyValidationService.validateApiKey(apiKey)) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Invalid API key");
            response.put("status", "UNAUTHORIZED");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        try {
            FraudDetectionResponse healthCheck = fraudDetectionService.getHealthCheck();
            
            Map<String, Object> response = new HashMap<>();
            response.put("service", "supplier-fraud-detection");
            response.put("status", "UP");
            response.put("fraudStatus", healthCheck.getFraudStatus());
            response.put("responseTimeMs", healthCheck.getResponseTimeMs());
            response.put("timestamp", healthCheck.getTimestamp());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("service", "supplier-fraud-detection");
            response.put("status", "DOWN");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(@RequestHeader("X-API-KEY") String apiKey) {
        
        // Validate API key
        if (!apiKeyValidationService.validateApiKey(apiKey)) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Invalid API key");
            response.put("status", "UNAUTHORIZED");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("service", "supplier-fraud-detection");
        response.put("maxResponseTimeMs", 200);
        response.put("version", "1.0.0");
        response.put("purpose", "Invoice fraud prevention for supplier payments");
        response.put("timestamp", java.time.LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
}
