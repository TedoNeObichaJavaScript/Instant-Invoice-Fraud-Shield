package com.microservices.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;

@Configuration
public class RateLimitingConfig {

    @Value("${rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    @Value("${rate-limit.burst-capacity:100}")
    private int burstCapacity;

    @Bean
    public RateLimitingService rateLimitingService(StringRedisTemplate redisTemplate) {
        return new RateLimitingService(redisTemplate, requestsPerMinute, burstCapacity);
    }

    public static class RateLimitingService {
        private final StringRedisTemplate redisTemplate;
        private final int requestsPerMinute;

        public RateLimitingService(StringRedisTemplate redisTemplate, int requestsPerMinute, int burstCapacity) {
            this.redisTemplate = redisTemplate;
            this.requestsPerMinute = requestsPerMinute;
            // burstCapacity parameter kept for future use
        }

        public boolean isAllowed(String key) {
            String redisKey = "rate_limit:" + key;
            String currentCount = redisTemplate.opsForValue().get(redisKey);
            
            if (currentCount == null) {
                redisTemplate.opsForValue().set(redisKey, "1", Duration.ofMinutes(1));
                return true;
            }
            
            int count = Integer.parseInt(currentCount);
            if (count >= requestsPerMinute) {
                return false;
            }
            
            redisTemplate.opsForValue().increment(redisKey);
            if (count == 0) {
                redisTemplate.expire(redisKey, Duration.ofMinutes(1));
            }
            
            return true;
        }
    }
}
