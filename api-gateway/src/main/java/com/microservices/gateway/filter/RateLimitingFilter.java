package com.microservices.gateway.filter;

import com.microservices.gateway.config.RateLimitingConfig.RateLimitingService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1)
public class RateLimitingFilter implements Filter {

    @Autowired
    private RateLimitingService rateLimitingService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Skip rate limiting for health checks and data fetching endpoints
        String requestUri = httpRequest.getRequestURI();
        if (requestUri.contains("/actuator/health") || 
            requestUri.contains("/api/v1/fraud-detection/ibans/random") ||
            requestUri.contains("/api/v1/fraud-detection/health")) {
            chain.doFilter(request, response);
            return;
        }

        // Get client IP for rate limiting
        String clientIp = getClientIpAddress(httpRequest);
        
        // Check if request is allowed
        if (rateLimitingService.isAllowed(clientIp)) {
            chain.doFilter(request, response);
        } else {
            httpResponse.setStatus(429); // Too Many Requests
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
