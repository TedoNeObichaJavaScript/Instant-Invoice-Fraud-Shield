package com.microservices.accounts.service;

import com.microservices.accounts.model.RiskAssessmentRequest;
import com.microservices.accounts.model.RiskAssessmentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class RiskAssessmentService {

    private final JdbcTemplate jdbcTemplate;
    private final int maxResponseTimeMs;

    public RiskAssessmentService(JdbcTemplate jdbcTemplate,
                               @Value("${accounts.max-response-time-ms:200}") int maxResponseTimeMs) {
        this.jdbcTemplate = jdbcTemplate;
        this.maxResponseTimeMs = maxResponseTimeMs;
    }

    public RiskAssessmentResponse assessRisk(RiskAssessmentRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Call the stored procedure for IBAN risk check
            String sql = "SELECT * FROM risk.check_iban_risk(?)";
            
            Map<String, Object> result = jdbcTemplate.queryForMap(sql, request.getIban());
            
            String riskLevel = (String) result.get("risk_level");
            String decision = (String) result.get("decision");
            Integer dbResponseTime = (Integer) result.get("response_time_ms");
            
            // Additional business logic for amount-based risk
            String finalDecision = enhanceDecisionWithAmountLogic(decision, request.getAmount());
            String reason = generateReason(riskLevel, finalDecision, request.getAmount());
            
            long totalResponseTime = System.currentTimeMillis() - startTime;
            
            return new RiskAssessmentResponse(
                request.getInvoiceId(),
                finalDecision,
                riskLevel,
                reason,
                (int) totalResponseTime
            );
            
        } catch (Exception e) {
            long totalResponseTime = System.currentTimeMillis() - startTime;
            
            // Fallback to REVIEW decision on error
            return new RiskAssessmentResponse(
                request.getInvoiceId(),
                "REVIEW",
                "UNKNOWN",
                "Risk assessment failed: " + e.getMessage(),
                (int) totalResponseTime
            );
        }
    }

    private String enhanceDecisionWithAmountLogic(String baseDecision, Double amount) {
        // Additional risk logic based on amount
        if (amount == null) {
            return "REVIEW";
        }
        
        // High amount transactions require additional scrutiny
        if (amount > 10000.0) {
            if ("ALLOW".equals(baseDecision)) {
                return "REVIEW";
            }
        }
        
        // Very high amount transactions are always reviewed
        if (amount > 50000.0) {
            return "REVIEW";
        }
        
        return baseDecision;
    }

    private String generateReason(String riskLevel, String decision, Double amount) {
        StringBuilder reason = new StringBuilder();
        
        reason.append("IBAN Risk Level: ").append(riskLevel);
        
        if (amount != null && amount > 10000.0) {
            reason.append(", High Amount Transaction");
        }
        
        switch (decision) {
            case "ALLOW":
                reason.append(" - Transaction approved");
                break;
            case "REVIEW":
                reason.append(" - Manual review required");
                break;
            case "BLOCK":
                reason.append(" - Transaction blocked");
                break;
            default:
                reason.append(" - Unknown decision");
        }
        
        return reason.toString();
    }

    public boolean isResponseTimeAcceptable(int responseTimeMs) {
        return responseTimeMs <= maxResponseTimeMs;
    }

    public RiskAssessmentResponse getHealthCheck() {
        long startTime = System.currentTimeMillis();
        
        try {
            // Simple health check query
            String sql = "SELECT COUNT(*) FROM risk.iban_risk_lookup LIMIT 1";
            jdbcTemplate.queryForObject(sql, Integer.class);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            return new RiskAssessmentResponse(
                "health-check",
                "ALLOW",
                "LOW",
                "Service healthy",
                (int) responseTime
            );
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            
            return new RiskAssessmentResponse(
                "health-check",
                "BLOCK",
                "HIGH",
                "Service unhealthy: " + e.getMessage(),
                (int) responseTime
            );
        }
    }
}
