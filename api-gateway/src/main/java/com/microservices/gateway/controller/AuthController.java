package com.microservices.gateway.controller;

import com.microservices.gateway.model.LoginRequest;
import com.microservices.gateway.model.User;
import com.microservices.gateway.service.AuditService;
import com.microservices.gateway.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthController(JwtService jwtService, AuditService auditService) {
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody @Valid LoginRequest loginRequest, 
                                                     HttpServletRequest request) {
        // long startTime = System.currentTimeMillis(); // For future audit logging
        
        // Input validation
        if (loginRequest.getUsername() == null || loginRequest.getUsername().trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Username is required");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Password is required");
            return ResponseEntity.badRequest().body(response);
        }
        
        // Sanitize inputs
        String username = loginRequest.getUsername().trim();
        String password = loginRequest.getPassword();
        
        // Additional validation
        if (username.length() < 3 || username.length() > 20) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Username must be between 3 and 20 characters");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (password.length() < 6 || password.length() > 128) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Password must be between 6 and 128 characters");
            return ResponseEntity.badRequest().body(response);
        }
        
        // Debug logging
        System.out.println("DEBUG: LoginRequest - username: " + username + 
                          ", rememberMe: " + loginRequest.isRememberMe());
        
        try {
            User user = jwtService.authenticateUser(loginRequest.getUsername(), loginRequest.getPassword());
            
            if (user == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Invalid credentials");
                
                // Log failed login attempt - temporarily disabled for debugging
                // auditService.logRequest(null, "/api/auth/login", "POST", 
                //     getClientIpAddress(request), request.getHeader("User-Agent"), 
                //     "***REDACTED***", 401, (int)(System.currentTimeMillis() - startTime));
                
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String token = jwtService.generateToken(user, loginRequest.isRememberMe());
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail()
            ));
            
            // Log successful login - temporarily disabled for debugging
            // auditService.logRequest(user.getId(), "/api/auth/login", "POST", 
            //     getClientIpAddress(request), request.getHeader("User-Agent"), 
            //     "***REDACTED***", 200, (int)(System.currentTimeMillis() - startTime));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Login failed");
            response.put("message", e.getMessage());
            
            // auditService.logRequest(null, "/api/auth/login", "POST", 
            //     getClientIpAddress(request), request.getHeader("User-Agent"), 
            //     "***REDACTED***", 500, (int)(System.currentTimeMillis() - startTime));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader("Authorization") String authHeader,
                                                    HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            String token = extractTokenFromHeader(authHeader);
            UUID userId = jwtService.getUserIdFromToken(token);
            
            jwtService.revokeToken(token);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Logged out successfully");
            
            // Log logout
            auditService.logRequest(userId, "/api/auth/logout", "POST", 
                getClientIpAddress(request), request.getHeader("User-Agent"), 
                null, 200, (int)(System.currentTimeMillis() - startTime));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Logout failed");
            response.put("message", e.getMessage());
            
            auditService.logRequest(null, "/api/auth/logout", "POST", 
                getClientIpAddress(request), request.getHeader("User-Agent"), 
                null, 500, (int)(System.currentTimeMillis() - startTime));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestHeader("Authorization") String authHeader,
                                                            HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            String token = extractTokenFromHeader(authHeader);
            boolean isValid = jwtService.validateToken(token);
            
            Map<String, Object> response = new HashMap<>();
            if (isValid) {
                UUID userId = jwtService.getUserIdFromToken(token);
                response.put("valid", true);
                response.put("userId", userId);
                
                auditService.logRequest(userId, "/api/auth/validate", "GET", 
                    getClientIpAddress(request), request.getHeader("User-Agent"), 
                    null, 200, (int)(System.currentTimeMillis() - startTime));
            } else {
                response.put("valid", false);
                
                auditService.logRequest(null, "/api/auth/validate", "GET", 
                    getClientIpAddress(request), request.getHeader("User-Agent"), 
                    null, 401, (int)(System.currentTimeMillis() - startTime));
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("error", e.getMessage());
            
            auditService.logRequest(null, "/api/auth/validate", "GET", 
                getClientIpAddress(request), request.getHeader("User-Agent"), 
                null, 500, (int)(System.currentTimeMillis() - startTime));
            
            return ResponseEntity.ok(response);
        }
    }

    private String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid authorization header");
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
