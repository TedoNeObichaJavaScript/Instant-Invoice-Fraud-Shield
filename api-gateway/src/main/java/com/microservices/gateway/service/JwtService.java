package com.microservices.gateway.service;

import com.microservices.gateway.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final long jwtRefreshExpiration;

    public JwtService(JdbcTemplate jdbcTemplate, 
                      RedisTemplate<String, Object> redisTemplate,
                      @Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration}") long jwtExpiration,
                      @Value("${jwt.refresh-expiration}") long jwtRefreshExpiration) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = new BCryptPasswordEncoder();
        
        // Handle both base64-encoded and plain text secrets
        SecretKey tempSecretKey;
        try {
            // Try to decode as base64 first
            tempSecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        } catch (Exception e) {
            // If base64 decoding fails, use the secret as plain text
            tempSecretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        this.secretKey = tempSecretKey;
        this.jwtExpiration = jwtExpiration;
        this.jwtRefreshExpiration = jwtRefreshExpiration;
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        String token = Jwts.builder()
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();

        // Store token in database
        storeTokenInDatabase(user.getId(), token, expiryDate);
        
        // Store token in Redis for fast validation
        storeTokenInRedis(token, user.getId(), jwtExpiration);

        return token;
    }

    public String generateToken(User user, boolean rememberMe) {
        Date now = new Date();
        long expirationTime = rememberMe ? jwtRefreshExpiration : jwtExpiration;
        Date expiryDate = new Date(now.getTime() + expirationTime);

        String token = Jwts.builder()
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .claim("rememberMe", rememberMe)
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();

        // Store token in database
        storeTokenInDatabase(user.getId(), token, expiryDate);
        
        // Store token in Redis for fast validation
        storeTokenInRedis(token, user.getId(), expirationTime);

        return token;
    }

    public boolean validateToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return false;
            }
            
            // First validate JWT signature and expiration
            Claims claims = getClaimsFromToken(token);
            if (claims.getExpiration().before(new Date())) {
                return false; // Token expired
            }

            // Check Redis cache (with error handling)
            try {
                if (isTokenInRedis(token)) {
                    return true;
                }
            } catch (Exception e) {
                // Redis might be down, continue to database check
            }

            // Check database (with error handling)
            try {
                if (isTokenInDatabase(token)) {
                    // Refresh Redis cache if possible
                    try {
                        UUID userId = UUID.fromString(claims.getSubject());
                        storeTokenInRedis(token, userId, jwtExpiration);
                    } catch (Exception e) {
                        // Redis might be down, but token is still valid
                    }
                    return true;
                }
            } catch (Exception e) {
                // Database might be down, but we can still validate JWT signature
                return true; // If JWT is valid, allow access
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public void revokeToken(String token) {
        try {
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

    public User findUserByUsername(String username) {
        try {
            String sql = "SELECT id, username, email, password_hash, is_active, created_at, updated_at FROM users WHERE username = ? AND is_active = true";
            
            List<User> users = jdbcTemplate.query(sql, new Object[]{username}, userRowMapper());
            
            return users.isEmpty() ? null : users.get(0);
        } catch (Exception e) {
            // Database might be down, return null
            return null;
        }
    }

    public String extractUsername(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.get("username", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public UUID getUserIdFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return UUID.fromString(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration()
                    .before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public User authenticateUser(String username, String password) {
        try {
            String sql = "SELECT id, username, email, password_hash, is_active, created_at, updated_at FROM users WHERE username = ? AND is_active = true";
            
            List<User> users = jdbcTemplate.query(sql, new Object[]{username}, userRowMapper());
            
            if (users.isEmpty()) {
                return null;
            }
            
            User user = users.get(0);
            
            boolean passwordMatches = passwordEncoder.matches(password, user.getPasswordHash());
            
            if (passwordMatches) {
                return user;
            }
            
            return null;
        } catch (Exception e) {
            // Database might be down, return null
            return null;
        }
    }

    private void storeTokenInDatabase(UUID userId, String token, Date expiresAt) {
        try {
            String tokenHash = hashToken(token);
            LocalDateTime expiresAtLocal = expiresAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            
            jdbcTemplate.update(
                "INSERT INTO jwt_tokens (user_id, token_hash, expires_at, is_revoked, created_at) VALUES (?, ?, ?, false, ?)",
                userId, tokenHash, expiresAtLocal, LocalDateTime.now()
            );
        } catch (Exception e) {
            // Database might be down, but token generation can still work
        }
    }

    private boolean isTokenInDatabase(String token) {
        try {
            String tokenHash = hashToken(token);
            String sql = "SELECT COUNT(*) FROM jwt_tokens WHERE token_hash = ? AND is_revoked = false AND expires_at > ?";
            
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tokenHash, LocalDateTime.now());
            return count != null && count > 0;
        } catch (Exception e) {
            // Database might be down, return false
            return false;
        }
    }

    private void storeTokenInRedis(String token, UUID userId, long expirationMs) {
        try {
            String key = "jwt:" + token;
            redisTemplate.opsForValue().set(key, userId.toString(), expirationMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // Redis might be down, but token generation can still work
        }
    }

    private boolean isTokenInRedis(String token) {
        try {
            String key = "jwt:" + token;
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            // Redis might be down, return false
            return false;
        }
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
