package com.microservices.gateway.service;

import com.microservices.gateway.model.AuditLog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuditService {

    private final JdbcTemplate jdbcTemplate;

    public AuditService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void logRequest(UUID userId, String endpoint, String method, String ipAddress, 
                          String userAgent, String requestBody, Integer responseStatus, 
                          Integer responseTimeMs) {
        
        String sql = """
            INSERT INTO audit.audit_logs 
            (user_id, endpoint, method, ip_address, user_agent, request_body, response_status, response_time_ms, created_at)
            VALUES (?, ?, ?, ?::inet, ?, ?, ?, ?, ?)
            """;
        
        jdbcTemplate.update(sql, userId, endpoint, method, ipAddress, userAgent, 
                           requestBody, responseStatus, responseTimeMs, LocalDateTime.now());
    }

    public List<AuditLog> getAuditLogs(UUID userId, int limit, int offset) {
        String sql = """
            SELECT id, user_id, endpoint, method, ip_address, user_agent, request_body, 
                   response_status, response_time_ms, created_at
            FROM audit.audit_logs 
            WHERE user_id = ?
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
            """;
        
        return jdbcTemplate.query(sql, new Object[]{userId, limit, offset}, auditLogRowMapper());
    }

    public List<AuditLog> getAllAuditLogs(int limit, int offset) {
        String sql = """
            SELECT id, user_id, endpoint, method, ip_address, user_agent, request_body, 
                   response_status, response_time_ms, created_at
            FROM audit.audit_logs 
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
            """;
        
        return jdbcTemplate.query(sql, new Object[]{limit, offset}, auditLogRowMapper());
    }

    public long getAuditLogCount(UUID userId) {
        String sql = "SELECT COUNT(*) FROM audit.audit_logs WHERE user_id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, userId);
        return count != null ? count : 0;
    }

    public long getTotalAuditLogCount() {
        String sql = "SELECT COUNT(*) FROM audit.audit_logs";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    private RowMapper<AuditLog> auditLogRowMapper() {
        return (rs, rowNum) -> {
            AuditLog auditLog = new AuditLog();
            auditLog.setId(UUID.fromString(rs.getString("id")));
            
            String userIdStr = rs.getString("user_id");
            if (userIdStr != null) {
                auditLog.setUserId(UUID.fromString(userIdStr));
            }
            
            auditLog.setEndpoint(rs.getString("endpoint"));
            auditLog.setMethod(rs.getString("method"));
            auditLog.setIpAddress(rs.getString("ip_address"));
            auditLog.setUserAgent(rs.getString("user_agent"));
            auditLog.setRequestBody(rs.getString("request_body"));
            auditLog.setResponseStatus(rs.getInt("response_status"));
            auditLog.setResponseTimeMs(rs.getInt("response_time_ms"));
            auditLog.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            
            return auditLog;
        };
    }
}
