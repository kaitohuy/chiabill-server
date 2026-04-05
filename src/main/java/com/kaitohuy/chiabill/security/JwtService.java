package com.kaitohuy.chiabill.security;

import org.springframework.beans.factory.annotation.Value;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service

public class JwtService {

    @Value("${JWT_SECRET}")
    private String secret;

    private Key key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = secret.getBytes();
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "JWT_SECRET is too short (minimum 32 characters required for HS256)"
            );
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Long userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30)) // 30 ngày
                .signWith(key)
                .compact();
    }

    public Long extractUserId(String token) {
        return Long.parseLong(
                Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody()
                        .getSubject()
        );
    }

    public boolean isValid(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}