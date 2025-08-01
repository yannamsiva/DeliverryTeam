package com.example.DeliveryTeamDashboard.config;

// ...existing code...
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// ...existing code...
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
// ...existing code...


@Component
public class JwtUtil {

    // private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class); // Removed unused field
//
//    private final Key secretKey;
//    private final long expirationTime;
//
//    public JwtUtil(@Value("${jwt.secret:#{null}}") String secret,
//                   @Value("${jwt.expiration:36000000}") long expirationTime) {
//        this.secretKey = secret != null ? Keys.hmacShaKeyFor(secret.getBytes()) : Keys.secretKeyFor(SignatureAlgorithm.HS512);
//        this.expirationTime = expirationTime; // Default: 10 hours (36000000 ms)
//    }

//    public String generateToken(String username, String role) {
//        Map<String, Object> claims = new HashMap<>();
//        claims.put("role", role);
//        return createToken(claims, username);
//    }
//
//    private String createToken(Map<String, Object> claims, String subject) {
//        return Jwts.builder()
//                .setClaims(claims)
//                .setSubject(subject)
//                .setIssuedAt(new Date(System.currentTimeMillis()))
//                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
//                .signWith(secretKey)
//                .compact();
//    }
//
//    public String extractUsername(String token) {
//        return extractClaim(token, Claims::getSubject);
//    }
//
//    public String extractRole(String token) {
//        return extractClaim(token, claims -> claims.get("role", String.class));
//    }
//
//    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
//        final Claims claims = extractAllClaims(token);
//        return claimsResolver.apply(claims);
//    }
//
//    private Claims extractAllClaims(String token) {
//        try {
//            if (token == null || token.trim().isEmpty()) {
//                logger.error("JWT token is null or empty");
//                throw new IllegalArgumentException("JWT token cannot be null or empty");
//            }
//            return Jwts.parserBuilder()
//                    .setSigningKey(secretKey)
//                    .build()
//                    .parseClaimsJws(token)
//                    .getBody();
//        } catch (ExpiredJwtException e) {
//            logger.warn("JWT token expired: {}", e.getMessage());
//            throw new RuntimeException("JWT token is expired", e);
//        } catch (UnsupportedJwtException e) {
//            logger.error("Unsupported JWT token: {}", e.getMessage());
//            throw new RuntimeException("JWT token is unsupported", e);
//        } catch (MalformedJwtException e) {
//            logger.error("Invalid JWT token: {}", e.getMessage());
//            throw new RuntimeException("Invalid JWT token", e);
//        } catch (SignatureException e) {
//            logger.error("JWT signature verification failed: {}", e.getMessage());
//            throw new RuntimeException("JWT signature verification failed", e);
//        } catch (IllegalArgumentException e) {
//            logger.error("Invalid JWT token: {}", e.getMessage());
//            throw new RuntimeException("JWT token is invalid", e);
//        }
//    }
//
//    public boolean isTokenValid(String token, String username) {
//        try {
//            final String extractedUsername = extractUsername(token);
//            boolean isValid = extractedUsername.equals(username) && !isTokenExpired(token);
//            if (!isValid) {
//                logger.warn("Invalid token for user {}: username mismatch or expired", username);
//            }
//            return isValid;
//        } catch (Exception e) {
//            logger.error("Token validation failed: {}", e.getMessage());
//            return false;
//        }
//    }
//
//    private boolean isTokenExpired(String token) {
//        return extractExpiration(token).before(new Date());
//    }
//
//    private Date extractExpiration(String token) {
//        return extractClaim(token, Claims::getExpiration);
//    }
    
    private static final String SECRET_KEY = "yQoLgh5CXQ3t7SgTwkUnCjOmBN8s2gqytgSbJ2vAe5YnhcOgBZtkJ35bHlplWjC5bJTKAeXRLS4xcDLCtyCZkA==";
    private static final long JWT_EXPIRATION_MS = 3600000; // 1 hour

    public String generateToken(String email, String role, Long employeeId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        if (employeeId != null) {
            claims.put("employeeId", employeeId);
        }
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION_MS))
                .signWith(SignatureAlgorithm.HS512, SECRET_KEY) // Deprecated, consider updating to new API if possible
                .compact();
    }

    public String generateToken(String email, String role) {
        return generateToken(email, role, null);
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public Long extractEmployeeId(String token) {
        return extractClaim(token, claims -> claims.get("employeeId", Long.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser() // Deprecated, consider updating to new API if possible
                .setSigningKey(SECRET_KEY) // Deprecated, consider updating to new API if possible
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String token, String email) {
        final String extractedEmail = extractEmail(token);
        return (extractedEmail.equals(email) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}