package com.microservices.accounts.model;

import java.time.LocalDateTime;

public class SupplierPaymentRequest {
    private String supplierIban;
    private String invoiceId;
    private String supplierName;
    private Double paymentAmount;
    private String currency;
    private String invoiceNumber;
    private String supplierReference;
    private LocalDateTime paymentDate;

    // Constructors
    public SupplierPaymentRequest() {}

    public SupplierPaymentRequest(String supplierIban, String invoiceId, String supplierName, 
                                Double paymentAmount, String currency, String invoiceNumber, 
                                String supplierReference) {
        this.supplierIban = supplierIban;
        this.invoiceId = invoiceId;
        this.supplierName = supplierName;
        this.paymentAmount = paymentAmount;
        this.currency = currency;
        this.invoiceNumber = invoiceNumber;
        this.supplierReference = supplierReference;
        this.paymentDate = LocalDateTime.now();
    }

    // Getters and Setters
    public String getSupplierIban() { return supplierIban; }
    public void setSupplierIban(String supplierIban) { this.supplierIban = supplierIban; }

    public String getInvoiceId() { return invoiceId; }
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public Double getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(Double paymentAmount) { this.paymentAmount = paymentAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public String getSupplierReference() { return supplierReference; }
    public void setSupplierReference(String supplierReference) { this.supplierReference = supplierReference; }

    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
}
