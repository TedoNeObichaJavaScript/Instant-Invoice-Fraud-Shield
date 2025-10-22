package com.microservices.accounts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Security Filter for SQL Injection Protection
 * Provides additional layer of protection at the HTTP request level
 */
@Configuration
public class SecurityFilterConfig {

    // Common SQL injection patterns for HTTP request filtering
    private static final List<Pattern> SQL_INJECTION_PATTERNS = Arrays.asList(
        Pattern.compile("(?i).*union.*select.*"),
        Pattern.compile("(?i).*(--|#|/\\*|\\*/).*"),
        Pattern.compile("(?i).*(or|and).*\\d+\\s*=\\s*\\d+.*"),
        Pattern.compile("(?i).*(sleep|waitfor|delay|benchmark).*"),
        Pattern.compile("(?i).*;.*"),
        Pattern.compile("(?i).*(load_file|into\\s+outfile|into\\s+dumpfile).*"),
        Pattern.compile("(?i).*information_schema.*"),
        Pattern.compile("(?i).*(sys\\.|pg_|mysql\\.).*"),
        Pattern.compile("(?i).*0x[0-9a-f]+.*"),
        Pattern.compile("(?i).*char\\s*\\(.*"),
        Pattern.compile("(?i).*ascii\\s*\\(.*"),
        Pattern.compile("(?i).*substring\\s*\\(.*"),
        Pattern.compile("(?i).*length\\s*\\(.*"),
        Pattern.compile("(?i).*cast\\s*\\(.*"),
        Pattern.compile("(?i).*convert\\s*\\(.*")
    );

    @Bean
    public OncePerRequestFilter sqlInjectionProtectionFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, 
                                          HttpServletResponse response, 
                                          FilterChain filterChain) throws ServletException, IOException {
                
                // Wrap request and response for content caching
                ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
                ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
                
                try {
                    // Check for SQL injection patterns in request parameters
                    if (containsSqlInjection(request)) {
                        logSecurityEvent("SQL Injection attempt detected in HTTP request", request);
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Security violation detected\",\"status\":\"BLOCKED\"}");
                        return;
                    }
                    
                    // Continue with the filter chain
                    filterChain.doFilter(requestWrapper, responseWrapper);
                    
                } finally {
                    // Copy response content back to original response
                    responseWrapper.copyBodyToResponse();
                }
            }
            
            private boolean containsSqlInjection(HttpServletRequest request) {
                // Check query parameters
                request.getParameterMap().values().forEach(values -> {
                    for (String value : values) {
                        if (isSqlInjectionPattern(value)) {
                            throw new SecurityException("SQL injection pattern detected in parameter: " + value);
                        }
                    }
                });
                
                // Check headers (except standard ones)
                request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
                    if (!isStandardHeader(headerName)) {
                        String headerValue = request.getHeader(headerName);
                        if (isSqlInjectionPattern(headerValue)) {
                            throw new SecurityException("SQL injection pattern detected in header: " + headerName);
                        }
                    }
                });
                
                return false;
            }
            
            private boolean isSqlInjectionPattern(String input) {
                if (input == null || input.trim().isEmpty()) {
                    return false;
                }
                
                return SQL_INJECTION_PATTERNS.stream()
                    .anyMatch(pattern -> pattern.matcher(input).matches());
            }
            
            private boolean isStandardHeader(String headerName) {
                List<String> standardHeaders = Arrays.asList(
                    "accept", "accept-encoding", "accept-language", "authorization",
                    "cache-control", "connection", "content-length", "content-type",
                    "cookie", "host", "user-agent", "x-api-key", "x-forwarded-for",
                    "x-real-ip", "x-requested-with"
                );
                return standardHeaders.contains(headerName.toLowerCase());
            }
            
            private void logSecurityEvent(String event, HttpServletRequest request) {
                System.err.println("SECURITY ALERT: " + event + 
                    " - IP: " + getClientIpAddress(request) +
                    " - User-Agent: " + request.getHeader("User-Agent") +
                    " - URI: " + request.getRequestURI());
            }
            
            private String getClientIpAddress(HttpServletRequest request) {
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        };
    }
}
