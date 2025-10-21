package com.microservices.gateway.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class FraudDetectionRequest {
    
    @NotBlank(message = "Invoice ID is required")
    @Size(max = 50, message = "Invoice ID must not exceed 50 characters")
    private String invoiceId;
    
    @NotBlank(message = "Supplier IBAN is required")
    @Pattern(regexp = "^[A-Z]{2}[0-9]{2}[A-Z0-9]{4}[0-9]{7}([A-Z0-9]?){0,16}$", 
             message = "Invalid IBAN format")
    private String supplierIban;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    @NotBlank(message = "Supplier name is required")
    @Size(max = 100, message = "Supplier name must not exceed 100 characters")
    private String supplierName;
    
    // Optional fields for enhanced fraud detection
    private String supplierCountry;
    private String paymentPurpose;
    private String previousInvoiceId;
    
    // Constructors
    public FraudDetectionRequest() {}
    
    public FraudDetectionRequest(String invoiceId, String supplierIban, BigDecimal amount, String supplierName) {
        this.invoiceId = invoiceId;
        this.supplierIban = supplierIban;
        this.amount = amount;
        this.supplierName = supplierName;
    }
    
    // Getters and Setters
    public String getInvoiceId() {
        return invoiceId;
    }
    
    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }
    
    public String getSupplierIban() {
        return supplierIban;
    }
    
    public void setSupplierIban(String supplierIban) {
        this.supplierIban = supplierIban;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getSupplierName() {
        return supplierName;
    }
    
    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }
    
    public String getSupplierCountry() {
        return supplierCountry;
    }
    
    public void setSupplierCountry(String supplierCountry) {
        this.supplierCountry = supplierCountry;
    }
    
    public String getPaymentPurpose() {
        return paymentPurpose;
    }
    
    public void setPaymentPurpose(String paymentPurpose) {
        this.paymentPurpose = paymentPurpose;
    }
    
    public String getPreviousInvoiceId() {
        return previousInvoiceId;
    }
    
    public void setPreviousInvoiceId(String previousInvoiceId) {
        this.previousInvoiceId = previousInvoiceId;
    }
    
    @Override
    public String toString() {
        return "FraudDetectionRequest{" +
                "invoiceId='" + invoiceId + '\'' +
                ", supplierIban='" + supplierIban + '\'' +
                ", amount=" + amount +
                ", supplierName='" + supplierName + '\'' +
                ", supplierCountry='" + supplierCountry + '\'' +
                ", paymentPurpose='" + paymentPurpose + '\'' +
                ", previousInvoiceId='" + previousInvoiceId + '\'' +
                '}';
    }
}
