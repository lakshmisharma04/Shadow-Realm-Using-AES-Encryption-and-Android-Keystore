package com.shadowrealm.backend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;

@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(
            @Value("${jwt.secret}") String secret
    ) {
        Objects.requireNonNull(secret, "JWT_SECRET must be configured");
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                    "JWT_SECRET must contain at least 32 UTF-8 bytes"
            );
        }
        this.key = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }


    // =========================================================
    // GENERATE TOKEN
    // =========================================================

    public String generateToken(String email) {
        return generateToken(email, 0);
    }

    public String generateToken(String email, int tokenVersion) {

        return Jwts.builder()

                .subject(email)
                .claim("ver", tokenVersion)

                .issuedAt(new Date())

                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + 86400000
                        )
                )

                .signWith(key)

                .compact();
    }


    // =========================================================
    // EXTRACT EMAIL
    // =========================================================

    public String extractEmail(String token) {

        return extractAllClaims(token)
                .getSubject();
    }

    public int extractTokenVersion(String token) {
        Object version = extractAllClaims(token).get("ver");
        return version instanceof Number ? ((Number) version).intValue() : 0;
    }


    // =========================================================
    // VALIDATE TOKEN
    // =========================================================

    public boolean isTokenValid(String token) {

        try {

            extractAllClaims(token);

            return true;

        } catch (JwtException | IllegalArgumentException e) {

            return false;
        }
    }


    // =========================================================
    // EXTRACT CLAIMS
    // =========================================================

    private Claims extractAllClaims(String token) {

        return Jwts.parser()

                .verifyWith(key)

                .build()

                .parseSignedClaims(token)

                .getPayload();
    }
}