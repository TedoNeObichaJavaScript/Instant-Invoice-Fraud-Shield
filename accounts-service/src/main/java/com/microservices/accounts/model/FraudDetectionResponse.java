package com.microservices.accounts.model;

import java.time.LocalDateTime;
import java.util.List;

public class FraudDetectionResponse {
    private String invoiceId;
    private String supplierIban;
    private String supplierName;
    private String fraudStatus; // SAFE, SUSPICIOUS, HIGH_RISK, BLOCKED
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
    private List<String> anomalies;
    private String recommendation; // APPROVE, REVIEW, BLOCK
    private Integer responseTimeMs;
    private LocalDateTime timestamp;
    private String reason;

    // Constructors
    public FraudDetectionResponse() {}

    public FraudDetectionResponse(String invoiceId, String supplierIban, String supplierName,
                                String fraudStatus, String riskLevel, List<String> anomalies,
                                String recommendation, Integer responseTimeMs) {
        this.invoiceId = invoiceId;
        this.supplierIban = supplierIban;
        this.supplierName = supplierName;
        this.fraudStatus = fraudStatus;
        this.riskLevel = riskLevel;
        this.anomalies = anomalies;
        this.recommendation = recommendation;
        this.responseTimeMs = responseTimeMs;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getInvoiceId() { return invoiceId; }
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }

    public String getSupplierIban() { return supplierIban; }
    public void setSupplierIban(String supplierIban) { this.supplierIban = supplierIban; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getFraudStatus() { return fraudStatus; }
    public void setFraudStatus(String fraudStatus) { this.fraudStatus = fraudStatus; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public List<String> getAnomalies() { return anomalies; }
    public void setAnomalies(List<String> anomalies) { this.anomalies = anomalies; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public Integer getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Integer responseTimeMs) { this.responseTimeMs = responseTimeMs; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
