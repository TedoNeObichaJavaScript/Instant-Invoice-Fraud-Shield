package com.microservices.gateway.controller;

import com.microservices.gateway.service.ApiGatewayService;
import com.microservices.gateway.service.AuditService;
import com.microservices.gateway.service.JwtService;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class GatewayController {

    private final ApiGatewayService apiGatewayService;
    private final JwtService jwtService;
    private final AuditService auditService;

    public GatewayController(ApiGatewayService apiGatewayService, 
                           JwtService jwtService, 
                           AuditService auditService) {
        this.apiGatewayService = apiGatewayService;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    @GetMapping("/suppliers/**")
    public ResponseEntity<String> forwardGetRequest(@RequestHeader("Authorization") String authHeader,
                                                  HttpServletRequest request) {
        return handleRequest(authHeader, request, HttpMethod.GET, null);
    }

    @PostMapping("/suppliers/**")
    public ResponseEntity<String> forwardPostRequest(@RequestHeader("Authorization") String authHeader,
                                                   @RequestBody(required = false) String requestBody,
                                                   HttpServletRequest request) {
        return handleRequest(authHeader, request, HttpMethod.POST, requestBody);
    }

    @PutMapping("/suppliers/**")
    public ResponseEntity<String> forwardPutRequest(@RequestHeader("Authorization") String authHeader,
                                                  @RequestBody(required = false) String requestBody,
                                                  HttpServletRequest request) {
        return handleRequest(authHeader, request, HttpMethod.PUT, requestBody);
    }

    @DeleteMapping("/suppliers/**")
    public ResponseEntity<String> forwardDeleteRequest(@RequestHeader("Authorization") String authHeader,
                                                     HttpServletRequest request) {
        return handleRequest(authHeader, request, HttpMethod.DELETE, null);
    }

    private ResponseEntity<String> handleRequest(String authHeader, 
                                                HttpServletRequest request, 
                                                HttpMethod method, 
                                                String requestBody) {
        long startTime = System.currentTimeMillis();
        UUID userId = null;
        int responseStatus = 500;
        String responseBody = "{\"error\": \"Internal server error\"}";

        try {
            // Validate JWT token
            String token = extractTokenFromHeader(authHeader);
            if (!jwtService.validateToken(token)) {
                responseStatus = 401;
                responseBody = "{\"error\": \"Invalid or expired token\"}";
                
                auditService.logRequest(null, request.getRequestURI(), method.name(), 
                    getClientIpAddress(request), request.getHeader("User-Agent"), 
                    requestBody, responseStatus, (int)(System.currentTimeMillis() - startTime));
                
                return ResponseEntity.status(responseStatus).body(responseBody);
            }

            userId = jwtService.getUserIdFromToken(token);

            // Forward request to accounts service
            String path = extractPathFromRequest(request);
            String forwardedResponse = apiGatewayService.forwardToAccountsService(path, method, requestBody);
            
            responseStatus = 200;
            responseBody = forwardedResponse;

        } catch (IllegalArgumentException e) {
            responseStatus = 401;
            responseBody = "{\"error\": \"Invalid authorization header\"}";
        } catch (Exception e) {
            responseStatus = 500;
            responseBody = "{\"error\": \"Service unavailable\", \"message\": \"" + e.getMessage() + "\"}";
        } finally {
            // Log the request
            auditService.logRequest(userId, request.getRequestURI(), method.name(), 
                getClientIpAddress(request), request.getHeader("User-Agent"), 
                requestBody, responseStatus, (int)(System.currentTimeMillis() - startTime));
        }

        return ResponseEntity.status(responseStatus).body(responseBody);
    }

    private String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid authorization header");
    }

    private String extractPathFromRequest(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        // Remove /api/v1 prefix to get the path for forwarding
        return requestUri.replaceFirst("/api/v1", "");
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
