package com.microservices.accounts.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyValidationService {

    private final String expectedApiKey;

    public ApiKeyValidationService(@Value("${accounts.api-key}") String expectedApiKey) {
        this.expectedApiKey = expectedApiKey;
    }

    public boolean validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }
        
        return expectedApiKey.equals(apiKey.trim());
    }

    public String getExpectedApiKey() {
        return expectedApiKey;
    }
}
