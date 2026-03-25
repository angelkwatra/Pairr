package com.connect.pairr.auth;

import com.connect.pairr.model.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-hours:24}")
    private long expirationHours;

    private static final long MILLISECONDS_PER_HOUR = 1000L * 60 * 60;

    public String generateToken(User user) {
        long expirationMillis = expirationHours * MILLISECONDS_PER_HOUR;

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                .expiration(
                        new Date(System.currentTimeMillis() + expirationMillis)
                )
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Parses and validates the JWT token.
     * The parser automatically validates expiration - expired tokens will throw ExpiredJwtException.
     *
     * @param token JWT token string
     * @return Claims payload if token is valid
     * @throws io.jsonwebtoken.ExpiredJwtException if token is expired
     * @throws JwtException if token is invalid, malformed, or cannot be verified
     */
    public Claims getPayload(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}