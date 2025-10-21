package com.microservices.gateway.model;

import java.time.LocalDateTime;
import java.util.List;

public class FraudDetectionResponse {
    
    private String invoiceId;
    private String riskStatus; // ALLOW, REVIEW, BLOCK
    private String reason;
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
    private List<String> anomalies;
    private LocalDateTime timestamp;
    private String transactionId;
    private boolean requiresManualReview;
    private String recommendation;
    
    // Constructors
    public FraudDetectionResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    public FraudDetectionResponse(String invoiceId, String riskStatus, String reason) {
        this();
        this.invoiceId = invoiceId;
        this.riskStatus = riskStatus;
        this.reason = reason;
    }
    
    // Builder pattern for easier construction
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private FraudDetectionResponse response = new FraudDetectionResponse();
        
        public Builder invoiceId(String invoiceId) {
            response.invoiceId = invoiceId;
            return this;
        }
        
        public Builder riskStatus(String riskStatus) {
            response.riskStatus = riskStatus;
            return this;
        }
        
        public Builder reason(String reason) {
            response.reason = reason;
            return this;
        }
        
        public Builder riskLevel(String riskLevel) {
            response.riskLevel = riskLevel;
            return this;
        }
        
        public Builder anomalies(List<String> anomalies) {
            response.anomalies = anomalies;
            return this;
        }
        
        public Builder transactionId(String transactionId) {
            response.transactionId = transactionId;
            return this;
        }
        
        public Builder requiresManualReview(boolean requiresManualReview) {
            response.requiresManualReview = requiresManualReview;
            return this;
        }
        
        public Builder recommendation(String recommendation) {
            response.recommendation = recommendation;
            return this;
        }
        
        public FraudDetectionResponse build() {
            return response;
        }
    }
    
    // Getters and Setters
    public String getInvoiceId() {
        return invoiceId;
    }
    
    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }
    
    public String getRiskStatus() {
        return riskStatus;
    }
    
    public void setRiskStatus(String riskStatus) {
        this.riskStatus = riskStatus;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getRiskLevel() {
        return riskLevel;
    }
    
    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
    
    public List<String> getAnomalies() {
        return anomalies;
    }
    
    public void setAnomalies(List<String> anomalies) {
        this.anomalies = anomalies;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public boolean isRequiresManualReview() {
        return requiresManualReview;
    }
    
    public void setRequiresManualReview(boolean requiresManualReview) {
        this.requiresManualReview = requiresManualReview;
    }
    
    public String getRecommendation() {
        return recommendation;
    }
    
    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }
    
    @Override
    public String toString() {
        return "FraudDetectionResponse{" +
                "invoiceId='" + invoiceId + '\'' +
                ", riskStatus='" + riskStatus + '\'' +
                ", reason='" + reason + '\'' +
                ", riskLevel='" + riskLevel + '\'' +
                ", anomalies=" + anomalies +
                ", timestamp=" + timestamp +
                ", transactionId='" + transactionId + '\'' +
                ", requiresManualReview=" + requiresManualReview +
                ", recommendation='" + recommendation + '\'' +
                '}';
    }
}
