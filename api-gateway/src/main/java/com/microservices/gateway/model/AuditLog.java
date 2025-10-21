package com.microservices.gateway.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class AuditLog {
    private UUID id;
    private UUID userId;
    private String endpoint;
    private String method;
    private String ipAddress;
    private String userAgent;
    private String requestBody;
    private Integer responseStatus;
    private Integer responseTimeMs;
    private LocalDateTime createdAt;

    // Constructors
    public AuditLog() {}

    public AuditLog(UUID userId, String endpoint, String method, String ipAddress, 
                    String userAgent, String requestBody, Integer responseStatus, 
                    Integer responseTimeMs) {
        this.userId = userId;
        this.endpoint = endpoint;
        this.method = method;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.requestBody = requestBody;
        this.responseStatus = responseStatus;
        this.responseTimeMs = responseTimeMs;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }

    public Integer getResponseStatus() { return responseStatus; }
    public void setResponseStatus(Integer responseStatus) { this.responseStatus = responseStatus; }

    public Integer getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Integer responseTimeMs) { this.responseTimeMs = responseTimeMs; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
