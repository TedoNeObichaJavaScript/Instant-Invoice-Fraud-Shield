package com.microservices.accounts.model;

import java.time.LocalDateTime;

public class RiskAssessmentResponse {
    private String invoiceId;
    private String decision; // ALLOW, REVIEW, BLOCK
    private String riskLevel; // LOW, MEDIUM, HIGH, BLOCKED
    private String reason;
    private Integer responseTimeMs;
    private LocalDateTime timestamp;

    // Constructors
    public RiskAssessmentResponse() {}

    public RiskAssessmentResponse(String invoiceId, String decision, String riskLevel, String reason, Integer responseTimeMs) {
        this.invoiceId = invoiceId;
        this.decision = decision;
        this.riskLevel = riskLevel;
        this.reason = reason;
        this.responseTimeMs = responseTimeMs;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getInvoiceId() { return invoiceId; }
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Integer getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Integer responseTimeMs) { this.responseTimeMs = responseTimeMs; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
