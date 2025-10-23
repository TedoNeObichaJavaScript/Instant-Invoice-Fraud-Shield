package com.microservices.accounts.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;

/**
 * SQL Injection Protection Service
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
        "onload", "onerror", "onclick", "0x", "information_schema"
    };

    /**
     * Validates input against SQL injection patterns
     * @param input The input string to validate
     * @return true if input is safe, false if potentially malicious
     */
    public boolean isInputSafe(String input) {
        if (!StringUtils.hasText(input)) {
            return true; // Empty or null input is safe
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
     * Validates IBAN format and content
     * @param iban The IBAN to validate
     * @return true if IBAN is valid and safe
     */
    public boolean isValidIban(String iban) {
        if (!StringUtils.hasText(iban)) {
            return false;
        }

        // Check for SQL injection first
        if (!isInputSafe(iban)) {
            return false;
        }

        // Basic IBAN format validation (2 letters + 2 digits + up to 30 alphanumeric)
        return iban.matches("^[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}$");
    }

    /**
     * Validates invoice number format and content
     * @param invoiceNumber The invoice number to validate
     * @return true if invoice number is valid and safe
     */
    public boolean isValidInvoiceNumber(String invoiceNumber) {
        if (!StringUtils.hasText(invoiceNumber)) {
            return false;
        }

        // Check for SQL injection first
        if (!isInputSafe(invoiceNumber)) {
            return false;
        }

        // Invoice number should be alphanumeric with common separators
        return invoiceNumber.matches("^[A-Za-z0-9\\-_/]{1,50}$");
    }

    /**
     * Validates supplier name format and content
     * @param supplierName The supplier name to validate
     * @return true if supplier name is valid and safe
     */
    public boolean isValidSupplierName(String supplierName) {
        if (!StringUtils.hasText(supplierName)) {
            return false;
        }

        // Check for SQL injection first
        if (!isInputSafe(supplierName)) {
            return false;
        }

        // Supplier name should contain only letters, numbers, spaces, and common punctuation
        return supplierName.matches("^[A-Za-z0-9\\s\\-\\.,&()]{1,100}$");
    }

    /**
     * Validates payment amount
     * @param amount The payment amount to validate
     * @return true if amount is valid
     */
    public boolean isValidPaymentAmount(Double amount) {
        if (amount == null) {
            return false;
        }

        // Amount should be positive and reasonable (not too large)
        return amount > 0 && amount <= 1000000; // Max 1 million
    }

    /**
     * Logs security events for monitoring
     * @param event The security event description
     * @param input The suspicious input that triggered the event
     */
    public void logSecurityEvent(String event, String input) {
        // In a real application, this would log to a security monitoring system
        System.err.println("SECURITY ALERT: " + event + " - Input: " + 
            (input != null ? input.substring(0, Math.min(input.length(), 100)) : "null"));
    }
}
