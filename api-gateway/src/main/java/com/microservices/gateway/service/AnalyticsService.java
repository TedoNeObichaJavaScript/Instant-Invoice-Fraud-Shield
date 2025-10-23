package com.microservices.gateway.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public AnalyticsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> getRiskDistribution() {
        String sql = "SELECT good_count, review_count, block_count FROM risk_distribution_log ORDER BY timestamp DESC LIMIT 1";
        return jdbcTemplate.queryForMap(sql);
    }

    public Map<String, Object> getPaymentTrends() {
        String sql = "SELECT hour_label, payments_count FROM payment_trends_log ORDER BY hour_label ASC";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        
        Map<String, Object> result = new HashMap<>();
        result.put("labels", rows.stream().map(row -> row.get("hour_label")).collect(Collectors.toList()));
        result.put("payments_count", rows.stream().map(row -> row.get("payments_count")).collect(Collectors.toList()));
        return result;
    }

    public Map<String, Object> getResponseTimes() {
        String sql = "SELECT hour_label, avg_response_time_ms FROM response_time_log ORDER BY hour_label ASC";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        Map<String, Object> result = new HashMap<>();
        result.put("labels", rows.stream().map(row -> row.get("hour_label")).collect(Collectors.toList()));
        result.put("avg_response_time_ms", rows.stream().map(row -> row.get("avg_response_time_ms")).collect(Collectors.toList()));
        return result;
    }

    public Map<String, Object> getSystemHealth() {
        String sql = "SELECT uptime_percentage, performance_score, security_score, reliability_score, efficiency_score FROM system_health_log ORDER BY timestamp DESC LIMIT 1";
        return jdbcTemplate.queryForMap(sql);
    }
}