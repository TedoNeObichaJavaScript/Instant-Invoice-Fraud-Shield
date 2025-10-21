package com.microservices.gateway.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class ApiGatewayService {

    private final WebClient webClient;
    private final String accountsServiceUrl;
    private final String apiKey;

    public ApiGatewayService(@Value("${gateway.accounts-service-url}") String accountsServiceUrl,
                           @Value("${gateway.api-key}") String apiKey) {
        this.accountsServiceUrl = accountsServiceUrl;
        this.apiKey = apiKey;
        
        this.webClient = WebClient.builder()
                .baseUrl(accountsServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-API-KEY", apiKey)
                .build();
    }

    public Mono<String> forwardToAccountsService(String path, HttpMethod method, String requestBody) {
        WebClient.RequestBodySpec requestSpec = webClient
                .method(method)
                .uri(path);

        if (requestBody != null && !requestBody.isEmpty()) {
            requestSpec.bodyValue(requestBody);
        }

        return requestSpec
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(5000))
                .onErrorResume(throwable -> {
                    // Log error and return error response
                    return Mono.just("{\"error\": \"Service unavailable\", \"message\": \"" + 
                                   throwable.getMessage() + "\"}");
                });
    }

    public Mono<String> forwardGetRequest(String path) {
        return forwardToAccountsService(path, HttpMethod.GET, null);
    }

    public Mono<String> forwardPostRequest(String path, String requestBody) {
        return forwardToAccountsService(path, HttpMethod.POST, requestBody);
    }

    public Mono<String> forwardPutRequest(String path, String requestBody) {
        return forwardToAccountsService(path, HttpMethod.PUT, requestBody);
    }

    public Mono<String> forwardDeleteRequest(String path) {
        return forwardToAccountsService(path, HttpMethod.DELETE, null);
    }
}
