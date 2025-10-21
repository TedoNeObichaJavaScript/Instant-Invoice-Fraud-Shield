package com.microservices.gateway.service;

import com.microservices.gateway.model.FraudDetectionRequest;
import com.microservices.gateway.model.FraudDetectionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FraudDetectionService {

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;
    private final String accountsServiceUrl;

    @Autowired
    public FraudDetectionService(JdbcTemplate jdbcTemplate, RestTemplate restTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.restTemplate = restTemplate;
        this.accountsServiceUrl = "http://accounts-service:8081";
    }

    /**
     * Main fraud detection analysis method
     * Analyzes payment request and returns risk assessment
     */
    public FraudDetectionResponse analyzePayment(FraudDetectionRequest request) {
        String transactionId = UUID.randomUUID().toString();
        List<String> anomalies = new ArrayList<>();
        String riskStatus = "ALLOW";
        String riskLevel = "LOW";
        String reason = "No anomalies detected";
        boolean requiresManualReview = false;

        try {
            // 1. Check if IBAN is in risky IBANs database
            if (isIbanRisky(request.getSupplierIban())) {
                anomalies.add("IBAN found in risky IBANs database");
                riskStatus = "BLOCK";
                riskLevel = "CRITICAL";
                reason = "IBAN is flagged as high-risk";
                requiresManualReview = true;
            }

            // 2. Validate IBAN format and checksum
            if (!isValidIban(request.getSupplierIban())) {
                anomalies.add("Invalid IBAN format or checksum");
                riskStatus = "BLOCK";
                riskLevel = "CRITICAL";
                reason = "Invalid IBAN format";
            }

            // 3. Check for suspicious patterns
            if (hasSuspiciousPatterns(request)) {
                anomalies.add("Suspicious payment pattern detected");
                if (!"BLOCK".equals(riskStatus)) {
                    riskStatus = "REVIEW";
                    riskLevel = "HIGH";
                    reason = "Suspicious payment pattern requires review";
                    requiresManualReview = true;
                }
            }

            // 4. Check amount thresholds
            if (isAmountSuspicious(request.getAmount())) {
                anomalies.add("Suspicious amount detected");
                if (!"BLOCK".equals(riskStatus)) {
                    riskStatus = "REVIEW";
                    riskLevel = "MEDIUM";
                    reason = "Amount exceeds normal thresholds";
                    requiresManualReview = true;
                }
            }

            // 5. Check supplier consistency
            if (hasSupplierInconsistency(request)) {
                anomalies.add("Supplier information inconsistency");
                if (!"BLOCK".equals(riskStatus)) {
                    riskStatus = "REVIEW";
                    riskLevel = "MEDIUM";
                    reason = "Supplier information requires verification";
                    requiresManualReview = true;
                }
            }

            // 6. If no specific issues, check for general risk factors
            if (anomalies.isEmpty()) {
                riskLevel = "LOW";
                reason = "Payment appears legitimate";
            }

            // 7. Log the analysis result
            logFraudAnalysis(request, riskStatus, riskLevel, anomalies, transactionId);

        } catch (Exception e) {
            riskStatus = "REVIEW";
            riskLevel = "HIGH";
            reason = "Error during analysis - manual review required";
            requiresManualReview = true;
            anomalies.add("Analysis error: " + e.getMessage());
        }

        return FraudDetectionResponse.builder()
                .invoiceId(request.getInvoiceId())
                .riskStatus(riskStatus)
                .reason(reason)
                .riskLevel(riskLevel)
                .anomalies(anomalies)
                .transactionId(transactionId)
                .requiresManualReview(requiresManualReview)
                .recommendation(generateRecommendation(riskStatus, riskLevel))
                .build();
    }

    /**
     * Check if IBAN is in the risky IBANs database
     */
    private boolean isIbanRisky(String iban) {
        try {
            String sql = "SELECT COUNT(*) FROM iban_risk_lookup WHERE iban = ? AND risk_level IN ('HIGH', 'CRITICAL')";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, iban);
            return count != null && count > 0;
        } catch (Exception e) {
            // If database error, assume not risky to avoid blocking legitimate payments
            return false;
        }
    }

    /**
     * Validate IBAN format and checksum using Modulo 97-10 algorithm
     */
    private boolean isValidIban(String iban) {
        if (iban == null || iban.length() < 15 || iban.length() > 34) {
            return false;
        }

        try {
            // Remove spaces and convert to uppercase
            iban = iban.replaceAll("\\s", "").toUpperCase();
            
            // Check basic format (2 letters + 2 digits + up to 30 alphanumeric)
            if (!iban.matches("^[A-Z]{2}[0-9]{2}[A-Z0-9]+$")) {
                return false;
            }

            // Extract check digits
            String checkDigits = iban.substring(2, 4);
            
            // Rearrange: move first 4 characters to end
            String rearranged = iban.substring(4) + iban.substring(0, 4);
            
            // Convert letters to numbers (A=10, B=11, ..., Z=35)
            StringBuilder numericString = new StringBuilder();
            for (char c : rearranged.toCharArray()) {
                if (Character.isDigit(c)) {
                    numericString.append(c);
                } else {
                    numericString.append(Character.getNumericValue(c) - 10 + 10);
                }
            }
            
            // Calculate mod 97
            String number = numericString.toString();
            int remainder = 0;
            for (int i = 0; i < number.length(); i++) {
                remainder = (remainder * 10 + Character.getNumericValue(number.charAt(i))) % 97;
            }
            
            return remainder == 1;
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check for suspicious payment patterns
     */
    private boolean hasSuspiciousPatterns(FraudDetectionRequest request) {
        // Check for round numbers (potential test payments)
        if (request.getAmount().remainder(BigDecimal.valueOf(1000)).compareTo(BigDecimal.ZERO) == 0) {
            return true;
        }
        
        // Check for very small amounts (potential test)
        if (request.getAmount().compareTo(BigDecimal.valueOf(1)) < 0) {
            return true;
        }
        
        // Check for very large amounts (potential fraud)
        if (request.getAmount().compareTo(BigDecimal.valueOf(100000)) > 0) {
            return true;
        }
        
        return false;
    }

    /**
     * Check if amount is suspicious
     */
    private boolean isAmountSuspicious(BigDecimal amount) {
        // Define suspicious amount thresholds
        BigDecimal veryHighAmount = new BigDecimal("50000");
        BigDecimal veryLowAmount = new BigDecimal("0.01");
        
        return amount.compareTo(veryHighAmount) > 0 || amount.compareTo(veryLowAmount) < 0;
    }

    /**
     * Check for supplier information inconsistency
     */
    private boolean hasSupplierInconsistency(FraudDetectionRequest request) {
        // Check if supplier name contains suspicious patterns
        String supplierName = request.getSupplierName().toLowerCase();
        
        // Check for generic names
        if (supplierName.contains("test") || supplierName.contains("demo") || 
            supplierName.contains("example") || supplierName.contains("fake")) {
            return true;
        }
        
        // Check for very short names
        if (supplierName.length() < 3) {
            return true;
        }
        
        return false;
    }

    /**
     * Generate recommendation based on risk assessment
     */
    private String generateRecommendation(String riskStatus, String riskLevel) {
        switch (riskStatus) {
            case "ALLOW":
                return "Payment can be processed immediately";
            case "REVIEW":
                return "Manual review recommended before processing";
            case "BLOCK":
                return "Payment should be blocked - high fraud risk";
            default:
                return "Additional verification required";
        }
    }

    /**
     * Log fraud analysis result
     */
    private void logFraudAnalysis(FraudDetectionRequest request, String riskStatus, 
                                 String riskLevel, List<String> anomalies, String transactionId) {
        try {
            String sql = """
                INSERT INTO fraud_analysis_log 
                (transaction_id, invoice_id, supplier_iban, amount, supplier_name, 
                 risk_status, risk_level, anomalies, created_at) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            jdbcTemplate.update(sql, 
                transactionId,
                request.getInvoiceId(),
                request.getSupplierIban(),
                request.getAmount(),
                request.getSupplierName(),
                riskStatus,
                riskLevel,
                String.join("; ", anomalies),
                LocalDateTime.now()
            );
        } catch (Exception e) {
            // Log error but don't fail the analysis
            System.err.println("Failed to log fraud analysis: " + e.getMessage());
        }
    }
}
