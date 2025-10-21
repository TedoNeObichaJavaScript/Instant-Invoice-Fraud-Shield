package com.microservices.gateway.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Service
public class ApiGatewayService {

    private final RestTemplate restTemplate;
    private final String accountsServiceUrl;
    private final String apiKey;

    public ApiGatewayService(@Value("${gateway.accounts-service-url}") String accountsServiceUrl,
                           @Value("${gateway.api-key}") String apiKey) {
        this.accountsServiceUrl = accountsServiceUrl;
        this.apiKey = apiKey;
        
        this.restTemplate = new RestTemplate();
    }

    public String forwardToAccountsService(String path, HttpMethod method, String requestBody) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-KEY", apiKey);
            
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            
            String url = accountsServiceUrl + path;
            ResponseEntity<String> response = restTemplate.exchange(url, method, entity, String.class);
            
            return response.getBody();
        } catch (Exception e) {
            return "{\"error\": \"Service unavailable\", \"message\": \"" + e.getMessage() + "\"}";
        }
    }

    public String forwardGetRequest(String path) {
        return forwardToAccountsService(path, HttpMethod.GET, null);
    }

    public String forwardPostRequest(String path, String requestBody) {
        return forwardToAccountsService(path, HttpMethod.POST, requestBody);
    }

    public String forwardPutRequest(String path, String requestBody) {
        return forwardToAccountsService(path, HttpMethod.PUT, requestBody);
    }

    public String forwardDeleteRequest(String path) {
        return forwardToAccountsService(path, HttpMethod.DELETE, null);
    }
}
