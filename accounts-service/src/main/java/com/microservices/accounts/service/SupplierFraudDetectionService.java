package com.microservices.accounts.service;

import com.microservices.accounts.model.SupplierPaymentRequest;
import com.microservices.accounts.model.FraudDetectionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SupplierFraudDetectionService {

    private final JdbcTemplate jdbcTemplate;
    private final int maxResponseTimeMs;

    public SupplierFraudDetectionService(JdbcTemplate jdbcTemplate,
                                       @Value("${accounts.max-response-time-ms:200}") int maxResponseTimeMs) {
        this.jdbcTemplate = jdbcTemplate;
        this.maxResponseTimeMs = maxResponseTimeMs;
    }

    public FraudDetectionResponse detectFraud(SupplierPaymentRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Check IBAN against risky database
            String ibanRiskLevel = checkIbanRisk(request.getSupplierIban());
            
            // Detect anomalies
            List<String> anomalies = detectAnomalies(request, ibanRiskLevel);
            
            // Determine fraud status and recommendation
            String fraudStatus = determineFraudStatus(ibanRiskLevel, anomalies);
            String riskLevel = determineRiskLevel(ibanRiskLevel, anomalies);
            String recommendation = determineRecommendation(fraudStatus, riskLevel);
            
            long totalResponseTime = System.currentTimeMillis() - startTime;
            
            return new FraudDetectionResponse(
                request.getInvoiceId(),
                request.getSupplierIban(),
                request.getSupplierName(),
                fraudStatus,
                riskLevel,
                anomalies,
                recommendation,
                (int) totalResponseTime
            );
            
        } catch (Exception e) {
            long totalResponseTime = System.currentTimeMillis() - startTime;
            
            // Fallback to high risk on error
            List<String> errorAnomalies = new ArrayList<>();
            errorAnomalies.add("System error: " + e.getMessage());
            
            return new FraudDetectionResponse(
                request.getInvoiceId(),
                request.getSupplierIban(),
                request.getSupplierName(),
                "HIGH_RISK",
                "CRITICAL",
                errorAnomalies,
                "REVIEW",
                (int) totalResponseTime
            );
        }
    }

    private String checkIbanRisk(String supplierIban) {
        try {
            String sql = "SELECT risk_level FROM risk.iban_risk_lookup WHERE iban = ?";
            String riskLevel = jdbcTemplate.queryForObject(sql, String.class, supplierIban);
            return riskLevel != null ? riskLevel : "UNKNOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private List<String> detectAnomalies(SupplierPaymentRequest request, String ibanRiskLevel) {
        List<String> anomalies = new ArrayList<>();
        
        // IBAN-based anomalies
        if ("HIGH".equals(ibanRiskLevel) || "BLOCKED".equals(ibanRiskLevel)) {
            anomalies.add("Supplier IBAN flagged as high risk in crowdsourced database");
        }
        
        // Amount-based anomalies
        if (request.getPaymentAmount() != null) {
            if (request.getPaymentAmount() > 50000.0) {
                anomalies.add("Unusually high payment amount: " + request.getPaymentAmount());
            }
            if (request.getPaymentAmount() < 1.0) {
                anomalies.add("Suspiciously low payment amount: " + request.getPaymentAmount());
            }
        }
        
        // Supplier name anomalies
        if (request.getSupplierName() != null) {
            String supplierName = request.getSupplierName().toLowerCase();
            if (supplierName.contains("test") || supplierName.contains("dummy")) {
                anomalies.add("Suspicious supplier name: " + request.getSupplierName());
            }
        }
        
        // Invoice number anomalies
        if (request.getInvoiceNumber() != null) {
            String invoiceNumber = request.getInvoiceNumber().toLowerCase();
            if (invoiceNumber.contains("test") || invoiceNumber.contains("dummy")) {
                anomalies.add("Suspicious invoice number: " + request.getInvoiceNumber());
            }
        }
        
        // Currency anomalies
        if (request.getCurrency() != null && !"EUR".equals(request.getCurrency()) && 
            !"USD".equals(request.getCurrency()) && !"BGN".equals(request.getCurrency())) {
            anomalies.add("Unusual currency for supplier payment: " + request.getCurrency());
        }
        
        return anomalies;
    }

    private String determineFraudStatus(String ibanRiskLevel, List<String> anomalies) {
        if ("BLOCKED".equals(ibanRiskLevel)) {
            return "BLOCKED";
        }
        
        if (anomalies.size() >= 3) {
            return "HIGH_RISK";
        }
        
        if (anomalies.size() >= 1) {
            return "SUSPICIOUS";
        }
        
        return "SAFE";
    }

    private String determineRiskLevel(String ibanRiskLevel, List<String> anomalies) {
        if ("BLOCKED".equals(ibanRiskLevel) || anomalies.size() >= 3) {
            return "CRITICAL";
        }
        
        if ("HIGH".equals(ibanRiskLevel) || anomalies.size() >= 2) {
            return "HIGH";
        }
        
        if ("MEDIUM".equals(ibanRiskLevel) || anomalies.size() >= 1) {
            return "MEDIUM";
        }
        
        return "LOW";
    }

    private String determineRecommendation(String fraudStatus, String riskLevel) {
        if ("BLOCKED".equals(fraudStatus) || "CRITICAL".equals(riskLevel)) {
            return "BLOCK";
        }
        
        if ("HIGH_RISK".equals(fraudStatus) || "HIGH".equals(riskLevel)) {
            return "REVIEW";
        }
        
        if ("SUSPICIOUS".equals(fraudStatus) || "MEDIUM".equals(riskLevel)) {
            return "REVIEW";
        }
        
        return "APPROVE";
    }

    public boolean isResponseTimeAcceptable(int responseTimeMs) {
        return responseTimeMs <= maxResponseTimeMs;
    }

    public FraudDetectionResponse getHealthCheck() {
        long startTime = System.currentTimeMillis();
        
        try {
            // Simple health check query
            String sql = "SELECT COUNT(*) FROM risk.iban_risk_lookup LIMIT 1";
            jdbcTemplate.queryForObject(sql, Integer.class);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            return new FraudDetectionResponse(
                "health-check",
                "BG11BANK99991234567890",
                "Health Check",
                "SAFE",
                "LOW",
                new ArrayList<>(),
                "APPROVE",
                (int) responseTime
            );
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            
            return new FraudDetectionResponse(
                "health-check",
                "BG11BANK99991234567890",
                "Health Check",
                "HIGH_RISK",
                "CRITICAL",
                List.of("Service unhealthy: " + e.getMessage()),
                "REVIEW",
                (int) responseTime
            );
        }
    }
}
