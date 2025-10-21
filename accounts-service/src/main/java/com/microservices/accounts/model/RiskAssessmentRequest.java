package com.microservices.accounts.model;

import java.time.LocalDateTime;

public class RiskAssessmentRequest {
    private String iban;
    private String invoiceId;
    private Double amount;
    private String currency;
    private String merchantId;
    private LocalDateTime timestamp;

    // Constructors
    public RiskAssessmentRequest() {}

    public RiskAssessmentRequest(String iban, String invoiceId, Double amount, String currency, String merchantId) {
        this.iban = iban;
        this.invoiceId = invoiceId;
        this.amount = amount;
        this.currency = currency;
        this.merchantId = merchantId;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public String getInvoiceId() { return invoiceId; }
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
