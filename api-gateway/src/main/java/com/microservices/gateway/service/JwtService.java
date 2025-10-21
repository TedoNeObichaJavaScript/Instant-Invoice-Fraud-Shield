package com.microservices.gateway.service;

import com.microservices.gateway.model.JwtToken;
import com.microservices.gateway.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class JwtService {

    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecretKey secretKey;
    private final long jwtExpiration;
    private final long refreshExpiration;

    public JwtService(JdbcTemplate jdbcTemplate, 
                      RedisTemplate<String, Object> redisTemplate,
                      @Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration}") long jwtExpiration,
                      @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpiration = jwtExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        String token = Jwts.builder()
                .setSubject(user.getId().toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();

        // Store token in database
        storeTokenInDatabase(user.getId(), token, expiryDate);
        
        // Store token in Redis for fast validation
        storeTokenInRedis(token, user.getId(), jwtExpiration);

        return token;
    }

    public boolean validateToken(String token) {
        try {
            // First check Redis cache
            if (isTokenInRedis(token)) {
                return true;
            }

            // Check database
            if (isTokenInDatabase(token)) {
                // Refresh Redis cache
                Claims claims = getClaimsFromToken(token);
                UUID userId = UUID.fromString(claims.getSubject());
                storeTokenInRedis(token, userId, jwtExpiration);
                return true;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return UUID.fromString(claims.getSubject());
    }

    public void revokeToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            UUID userId = UUID.fromString(claims.getSubject());
            
            // Revoke in database
            jdbcTemplate.update(
                "UPDATE jwt_tokens SET is_revoked = true, revoked_at = ? WHERE token_hash = ?",
                LocalDateTime.now(), hashToken(token)
            );
            
            // Remove from Redis
            redisTemplate.delete("jwt:" + token);
            
        } catch (Exception e) {
            // Log error but don't throw
        }
    }

    public User authenticateUser(String username, String password) {
        String sql = "SELECT id, username, email, password_hash, is_active, created_at, updated_at FROM users WHERE username = ? AND is_active = true";
        
        List<User> users = jdbcTemplate.query(sql, new Object[]{username}, userRowMapper());
        
        if (users.isEmpty()) {
            return null;
        }
        
        User user = users.get(0);
        if (passwordEncoder.matches(password, user.getPasswordHash())) {
            return user;
        }
        
        return null;
    }

    private void storeTokenInDatabase(UUID userId, String token, Date expiresAt) {
        String tokenHash = hashToken(token);
        LocalDateTime expiresAtLocal = expiresAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        
        jdbcTemplate.update(
            "INSERT INTO jwt_tokens (user_id, token_hash, expires_at, is_revoked, created_at) VALUES (?, ?, ?, false, ?)",
            userId, tokenHash, expiresAtLocal, LocalDateTime.now()
        );
    }

    private boolean isTokenInDatabase(String token) {
        String tokenHash = hashToken(token);
        String sql = "SELECT COUNT(*) FROM jwt_tokens WHERE token_hash = ? AND is_revoked = false AND expires_at > ?";
        
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tokenHash, LocalDateTime.now());
        return count != null && count > 0;
    }

    private void storeTokenInRedis(String token, UUID userId, long expirationMs) {
        String key = "jwt:" + token;
        redisTemplate.opsForValue().set(key, userId.toString(), expirationMs, TimeUnit.MILLISECONDS);
    }

    private boolean isTokenInRedis(String token) {
        String key = "jwt:" + token;
        return redisTemplate.hasKey(key);
    }

    private String hashToken(String token) {
        return passwordEncoder.encode(token);
    }

    private RowMapper<User> userRowMapper() {
        return (rs, rowNum) -> {
            User user = new User();
            user.setId(UUID.fromString(rs.getString("id")));
            user.setUsername(rs.getString("username"));
            user.setEmail(rs.getString("email"));
            user.setPasswordHash(rs.getString("password_hash"));
            user.setActive(rs.getBoolean("is_active"));
            user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            user.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            return user;
        };
    }
}
