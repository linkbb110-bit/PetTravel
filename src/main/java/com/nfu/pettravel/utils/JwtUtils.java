package com.nfu.pettravel.utils;

import com.nfu.pettravel.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import javax.crypto.SecretKey;

@Component
public class JwtUtils {
    private final SecretKey key;
    private final Duration expiration;

    public JwtUtils(
            @Value("${pettravel.jwt.secret}") String secret,
            @Value("${pettravel.jwt.expiration:7d}") Duration expiration) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generateToken(Long userId, String username, UserRole role) {
        Date issuedAt = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role", role.getCode())
                .issuedAt(issuedAt)
                .expiration(new Date(issuedAt.getTime() + expiration.toMillis()))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
