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
        String riskLevel = "GOOD";
        String reason = "No anomalies detected";
        boolean requiresManualReview = false;

        try {
            // 1. Check IBAN in database first
            System.out.println("DEBUG: Analyzing payment for IBAN: " + request.getSupplierIban());
            String dbRiskLevel = getIbanRiskLevel(request.getSupplierIban());
            System.out.println("DEBUG: Database returned risk level: " + dbRiskLevel);
            
            if (dbRiskLevel != null) {
                switch (dbRiskLevel) {
                    case "BLOCK":
                        riskStatus = "BLOCK";
                        riskLevel = "BLOCK";
                        reason = "IBAN is flagged as blocked in database";
                        anomalies.add("IBAN found in blocked IBANs database");
                        requiresManualReview = true;
                        System.out.println("DEBUG: Setting BLOCK status");
                        break;
                    case "REVIEW":
                        riskStatus = "REVIEW";
                        riskLevel = "REVIEW";
                        reason = "IBAN requires manual review";
                        anomalies.add("IBAN flagged for review in database");
                        requiresManualReview = true;
                        System.out.println("DEBUG: Setting REVIEW status");
                        break;
                    case "GOOD":
                        riskStatus = "ALLOW";
                        riskLevel = "GOOD";
                        reason = "IBAN verified as good in database";
                        System.out.println("DEBUG: Setting ALLOW status");
                        break;
                    default:
                        // Unknown risk level, treat as review
                        riskStatus = "REVIEW";
                        riskLevel = "REVIEW";
                        reason = "IBAN has unknown risk level in database";
                        anomalies.add("IBAN has unknown risk classification");
                        requiresManualReview = true;
                        System.out.println("DEBUG: Setting REVIEW status for unknown risk level: " + dbRiskLevel);
                        break;
                }
            } else {
                // IBAN not found in database - treat as review
                riskStatus = "REVIEW";
                riskLevel = "REVIEW";
                reason = "IBAN not found in risk database - manual review required";
                anomalies.add("IBAN not found in risk database");
                requiresManualReview = true;
            }

            // 2. Validate IBAN format and checksum
            if (!isValidIban(request.getSupplierIban())) {
                anomalies.add("Invalid IBAN format or checksum");
                riskStatus = "BLOCK";
                riskLevel = "BLOCK";
                reason = "Invalid IBAN format";
                requiresManualReview = true;
            }

            // 3. Additional checks only if not already blocked and not explicitly marked as GOOD by database
            if (!"BLOCK".equals(riskStatus) && !"GOOD".equals(riskLevel)) {
                // Check for suspicious patterns
                if (hasSuspiciousPatterns(request)) {
                    anomalies.add("Suspicious payment pattern detected");
                    if ("ALLOW".equals(riskStatus)) {
                        riskStatus = "REVIEW";
                        riskLevel = "REVIEW";
                        reason = "Suspicious payment pattern requires review";
                        requiresManualReview = true;
                    }
                }

                // Check amount thresholds
                if (isAmountSuspicious(request.getAmount())) {
                    anomalies.add("Suspicious amount detected");
                    if ("ALLOW".equals(riskStatus)) {
                        riskStatus = "REVIEW";
                        riskLevel = "REVIEW";
                        reason = "Amount exceeds normal thresholds";
                        requiresManualReview = true;
                    }
                }

                // Check supplier consistency
                if (hasSupplierInconsistency(request)) {
                    anomalies.add("Supplier information inconsistency");
                    if ("ALLOW".equals(riskStatus)) {
                        riskStatus = "REVIEW";
                        riskLevel = "REVIEW";
                        reason = "Supplier information requires verification";
                        requiresManualReview = true;
                    }
                }

                // Check for duplicate payments
                if (hasDuplicatePayment(request)) {
                    anomalies.add("Potential duplicate payment detected");
                    if ("ALLOW".equals(riskStatus)) {
                        riskStatus = "REVIEW";
                        riskLevel = "REVIEW";
                        reason = "Duplicate payment pattern detected";
                        requiresManualReview = true;
                    }
                }

                // Check for unusual timing
                if (hasUnusualTiming(request)) {
                    anomalies.add("Unusual payment timing detected");
                    if ("ALLOW".equals(riskStatus)) {
                        riskStatus = "REVIEW";
                        riskLevel = "REVIEW";
                        reason = "Payment timing requires review";
                        requiresManualReview = true;
                    }
                }

                // Check for velocity anomalies
                if (hasVelocityAnomaly(request)) {
                    anomalies.add("High-frequency payment pattern detected");
                    if ("ALLOW".equals(riskStatus)) {
                        riskStatus = "REVIEW";
                        riskLevel = "REVIEW";
                        reason = "High payment velocity detected";
                        requiresManualReview = true;
                    }
                }
            }

            // 7. Log the analysis result
            logFraudAnalysis(request, riskStatus, riskLevel, anomalies, transactionId);

        } catch (Exception e) {
            riskStatus = "REVIEW";
            riskLevel = "REVIEW";
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
     * Get the risk level of an IBAN from the database
     */
    private String getIbanRiskLevel(String iban) {
        try {
            String sql = "SELECT risk_level FROM risk.iban_risk_lookup WHERE iban = ?";
            String result = jdbcTemplate.queryForObject(sql, String.class, iban);
            System.out.println("DEBUG: Found IBAN " + iban + " with risk level: " + result);
            return result;
        } catch (Exception e) {
            System.out.println("DEBUG: Error querying IBAN " + iban + ": " + e.getMessage());
            // If database error or IBAN not found, return null
            return null;
        }
    }

    /**
     * Check if IBAN is in the risky IBANs database
     */
    private boolean isIbanRisky(String iban) {
        try {
            String sql = "SELECT COUNT(*) FROM iban_risk_lookup WHERE iban = ? AND risk_level = 'BLOCK'";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, iban);
            return count != null && count > 0;
        } catch (Exception e) {
            // If database error, assume not risky to avoid blocking legitimate payments
            return false;
        }
    }

    /**
     * Validate IBAN format and checksum using Modulo 97-10 algorithm (ISO 13616)
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
                    // Fix: A=10, B=11, ..., Z=35
                    int value = c - 'A' + 10;
                    numericString.append(value);
                }
            }
            
            // Calculate mod 97 using big integer approach for large numbers
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
        // Check for very small amounts (potential test)
        if (request.getAmount().compareTo(BigDecimal.valueOf(1)) < 0) {
            return true;
        }
        
        // Check for very large amounts (potential fraud)
        if (request.getAmount().compareTo(BigDecimal.valueOf(100000)) > 0) {
            return true;
        }
        
        // Check for amounts ending in .99 (common fraud pattern)
        if (request.getAmount().toString().endsWith(".99")) {
            return true;
        }
        
        // Check for suspicious amount patterns (but not for GOOD IBANs)
        // GOOD IBANs are expected to have rounded amounts, so don't flag them
        String ibanRiskLevel = getIbanRiskLevel(request.getSupplierIban());
        if (!"GOOD".equals(ibanRiskLevel)) {
            // Check for round numbers (potential test payments) - only for non-GOOD IBANs
            if (request.getAmount().remainder(BigDecimal.valueOf(1000)).compareTo(BigDecimal.ZERO) == 0) {
                return true;
            }
            
            // Check for suspicious amount patterns - only for non-GOOD IBANs
            if (request.getAmount().remainder(BigDecimal.valueOf(100)).compareTo(BigDecimal.ZERO) == 0) {
                return true; // Round hundreds
            }
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

    /**
     * Check for duplicate payment patterns
     */
    private boolean hasDuplicatePayment(FraudDetectionRequest request) {
        try {
            // Check for same amount and IBAN within last 24 hours
            String sql = """
                SELECT COUNT(*) FROM fraud_analysis_log 
                WHERE supplier_iban = ? AND amount = ? 
                AND created_at > NOW() - INTERVAL '24 hours'
                """;
            
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, 
                request.getSupplierIban(), request.getAmount());
            
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check for unusual payment timing
     */
    private boolean hasUnusualTiming(FraudDetectionRequest request) {
        // Check if payment is made outside business hours (simplified)
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        
        // Flag payments made between 11 PM and 6 AM
        return hour >= 23 || hour <= 6;
    }

    /**
     * Check for velocity anomalies (high frequency payments)
     */
    private boolean hasVelocityAnomaly(FraudDetectionRequest request) {
        try {
            // Check for multiple payments from same IBAN in last hour
            String sql = """
                SELECT COUNT(*) FROM fraud_analysis_log 
                WHERE supplier_iban = ? 
                AND created_at > NOW() - INTERVAL '1 hour'
                """;
            
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, 
                request.getSupplierIban());
            
            // Flag if more than 3 payments in last hour
            return count != null && count > 3;
        } catch (Exception e) {
            return false;
        }
    }
}
