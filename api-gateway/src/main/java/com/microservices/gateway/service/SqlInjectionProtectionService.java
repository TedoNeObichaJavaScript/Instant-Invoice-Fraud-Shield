package com.microservices.gateway.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;

/**
 * SQL Injection Protection Service for API Gateway
 * Provides comprehensive protection against SQL injection attacks
 */
@Service
public class SqlInjectionProtectionService {

    // Common SQL injection patterns - More targeted and less aggressive
    private static final Pattern[] SQL_INJECTION_PATTERNS = {
        // UNION-based attacks
        Pattern.compile("(?i).*union.*select.*", Pattern.CASE_INSENSITIVE),
        
        // Comment-based attacks
        Pattern.compile("(?i).*(--|#|/\\*|\\*/).*", Pattern.CASE_INSENSITIVE),
        
        // Boolean-based blind SQL injection
        Pattern.compile("(?i).*(or|and).*\\d+\\s*=\\s*\\d+.*", Pattern.CASE_INSENSITIVE),
        
        // Time-based blind SQL injection
        Pattern.compile("(?i).*(sleep|waitfor|delay|benchmark).*", Pattern.CASE_INSENSITIVE),
        
        // Stacked queries (but allow single semicolon at end)
        Pattern.compile("(?i).*;.*;.*", Pattern.CASE_INSENSITIVE),
        
        // Function-based attacks
        Pattern.compile("(?i).*(load_file|into\\s+outfile|into\\s+dumpfile).*", Pattern.CASE_INSENSITIVE),
        
        // Information schema attacks
        Pattern.compile("(?i).*information_schema.*", Pattern.CASE_INSENSITIVE),
        
        // System table attacks
        Pattern.compile("(?i).*(sys\\.|pg_|mysql\\.).*", Pattern.CASE_INSENSITIVE),
        
        // Hex encoding attacks
        Pattern.compile("(?i).*0x[0-9a-f]+.*", Pattern.CASE_INSENSITIVE),
        
        // Char function attacks
        Pattern.compile("(?i).*char\\s*\\(.*", Pattern.CASE_INSENSITIVE),
        
        // ASCII function attacks
        Pattern.compile("(?i).*ascii\\s*\\(.*", Pattern.CASE_INSENSITIVE),
        
        // Substring function attacks
        Pattern.compile("(?i).*substring\\s*\\(.*", Pattern.CASE_INSENSITIVE),
        
        // Length function attacks
        Pattern.compile("(?i).*length\\s*\\(.*", Pattern.CASE_INSENSITIVE),
        
        // Cast function attacks
        Pattern.compile("(?i).*cast\\s*\\(.*", Pattern.CASE_INSENSITIVE),
        
        // Convert function attacks
        Pattern.compile("(?i).*convert\\s*\\(.*", Pattern.CASE_INSENSITIVE),
        
        // SQL keywords in suspicious contexts
        Pattern.compile("(?i).*(drop|create|alter|truncate|delete|insert|update).*table.*", Pattern.CASE_INSENSITIVE),
        
        // Script injection attempts
        Pattern.compile("(?i).*<script.*>.*", Pattern.CASE_INSENSITIVE),
        
        // SQL injection with quotes
        Pattern.compile("(?i).*'.*(or|and).*'.*", Pattern.CASE_INSENSITIVE)
    };

    // Dangerous characters that should be escaped or rejected - More targeted
    private static final String[] DANGEROUS_CHARS = {
        "'", "\"", "--", "/*", "*/", "xp_", "sp_", "exec", "execute",
        "union", "script", "<script", "javascript:", "vbscript:", 
        "onload", "onerror", "onclick", "0x", "information_schema",
        "drop", "create", "alter", "truncate", "delete", "insert", "update",
        "grant", "revoke", "deny", "backup", "restore", "shutdown"
    };

    // Maximum input length to prevent buffer overflow attacks
    private static final int MAX_INPUT_LENGTH = 1000;

    /**
     * Validates input against SQL injection patterns
     * @param input The input string to validate
     * @return true if input is safe, false if potentially malicious
     */
    public boolean isInputSafe(String input) {
        if (!StringUtils.hasText(input)) {
            return true; // Empty or null input is safe
        }

        // Check input length
        if (input.length() > MAX_INPUT_LENGTH) {
            return false;
        }

        // Check against SQL injection patterns
        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            if (pattern.matcher(input).matches()) {
                return false;
            }
        }

        // Check for dangerous characters
        String lowerInput = input.toLowerCase();
        for (String dangerousChar : DANGEROUS_CHARS) {
            if (lowerInput.contains(dangerousChar.toLowerCase())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Sanitizes input by removing or escaping dangerous characters
     * @param input The input string to sanitize
     * @return Sanitized input string
     */
    public String sanitizeInput(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }

        String sanitized = input;
        
        // Remove or escape dangerous characters
        for (String dangerousChar : DANGEROUS_CHARS) {
            sanitized = sanitized.replaceAll("(?i)" + Pattern.quote(dangerousChar), "");
        }

        // Remove SQL injection patterns
        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            sanitized = pattern.matcher(sanitized).replaceAll("");
        }

        // Trim whitespace
        sanitized = sanitized.trim();

        return sanitized;
    }

    /**
     * Validates and sanitizes a list of inputs
     * @param inputs List of input strings
     * @return List of sanitized inputs
     * @throws SecurityException if any input contains malicious content
     */
    public List<String> validateAndSanitizeInputs(List<String> inputs) {
        List<String> sanitizedInputs = new ArrayList<>();
        
        for (String input : inputs) {
            if (!isInputSafe(input)) {
                throw new SecurityException("Potentially malicious input detected: " + input);
            }
            sanitizedInputs.add(sanitizeInput(input));
        }
        
        return sanitizedInputs;
    }

    /**
     * Validates IBAN format to prevent injection through malformed IBANs
     * @param iban The IBAN to validate
     * @return true if IBAN format is valid and safe
     */
    public boolean isValidIban(String iban) {
        if (!StringUtils.hasText(iban)) {
            return false;
        }

        // Basic IBAN format validation (2 letters + 2 digits + up to 30 alphanumeric)
        if (!iban.matches("^[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}$")) {
            return false;
        }

        // Check for SQL injection patterns in IBAN
        return isInputSafe(iban);
    }

    /**
     * Logs security events for monitoring
     * @param eventType Type of security event
     * @param details Event details
     */
    public void logSecurityEvent(String eventType, String details) {
        // In a real implementation, this would log to a security monitoring system
        System.err.println("SECURITY EVENT: " + eventType + " - " + details);
    }

    /**
     * Validates numeric input to prevent injection
     * @param input The input to validate
     * @return true if input is a safe number
     */
    public boolean isValidNumber(String input) {
        if (!StringUtils.hasText(input)) {
            return false;
        }

        try {
            // Check if it's a valid number
            Double.parseDouble(input);
            
            // Check for SQL injection patterns
            return isInputSafe(input);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validates username format to prevent injection
     * @param username The username to validate
     * @return true if username is valid and safe
     */
    public boolean isValidUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }

        // Username should be 3-20 characters, alphanumeric and underscore only
        if (!username.matches("^[a-zA-Z0-9_]{3,20}$")) {
            return false;
        }

        // Check for SQL injection patterns
        return isInputSafe(username);
    }

    /**
     * Validates password format to prevent injection
     * @param password The password to validate
     * @return true if password is valid and safe
     */
    public boolean isValidPassword(String password) {
        if (!StringUtils.hasText(password)) {
            return false;
        }

        // Password should be 6-128 characters
        if (password.length() < 6 || password.length() > 128) {
            return false;
        }

        // Check for SQL injection patterns
        return isInputSafe(password);
    }
}
