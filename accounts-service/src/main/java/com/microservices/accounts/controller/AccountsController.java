package com.microservices.accounts.controller;

import com.microservices.accounts.model.RiskAssessmentRequest;
import com.microservices.accounts.model.RiskAssessmentResponse;
import com.microservices.accounts.service.ApiKeyValidationService;
import com.microservices.accounts.service.RiskAssessmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountsController {

    private final RiskAssessmentService riskAssessmentService;
    private final ApiKeyValidationService apiKeyValidationService;

    public AccountsController(RiskAssessmentService riskAssessmentService,
                           ApiKeyValidationService apiKeyValidationService) {
        this.riskAssessmentService = riskAssessmentService;
        this.apiKeyValidationService = apiKeyValidationService;
    }

    @PostMapping("/risk-assessment")
    public ResponseEntity<Map<String, Object>> assessRisk(@RequestBody RiskAssessmentRequest request,
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
        if (request.getIban() == null || request.getIban().trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "IBAN is required");
            response.put("status", "BAD_REQUEST");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            // Perform risk assessment
            RiskAssessmentResponse assessment = riskAssessmentService.assessRisk(request);
            
            // Check if response time is acceptable
            boolean acceptableResponseTime = riskAssessmentService.isResponseTimeAcceptable(assessment.getResponseTimeMs());
            
            Map<String, Object> response = new HashMap<>();
            response.put("invoiceId", assessment.getInvoiceId());
            response.put("decision", assessment.getDecision());
            response.put("riskLevel", assessment.getRiskLevel());
            response.put("reason", assessment.getReason());
            response.put("responseTimeMs", assessment.getResponseTimeMs());
            response.put("acceptableResponseTime", acceptableResponseTime);
            response.put("timestamp", assessment.getTimestamp());
            response.put("status", "SUCCESS");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Risk assessment failed");
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
            RiskAssessmentResponse healthCheck = riskAssessmentService.getHealthCheck();
            
            Map<String, Object> response = new HashMap<>();
            response.put("service", "accounts-service");
            response.put("status", "UP");
            response.put("decision", healthCheck.getDecision());
            response.put("responseTimeMs", healthCheck.getResponseTimeMs());
            response.put("timestamp", healthCheck.getTimestamp());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("service", "accounts-service");
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
        response.put("service", "accounts-service");
        response.put("maxResponseTimeMs", 200);
        response.put("version", "1.0.0");
        response.put("timestamp", java.time.LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
}
