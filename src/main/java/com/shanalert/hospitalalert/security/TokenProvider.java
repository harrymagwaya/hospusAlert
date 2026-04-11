package com.shanalert.hospitalalert.security;

import com.shanalert.hospitalalert.config.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.SignatureAlgorithm;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;


import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long expiration;


    public Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        Date now  = new Date();

        return Jwts.builder()
                .setSubject(userPrincipal.getId().toString())
                .claim("role", userPrincipal.getUserRole().name())  // ⭐ user type
                .claim("status", userPrincipal.getStatus().name())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expiration))
                .setIssuer("SHAN")
                .setAudience("SHAN-HOSPUS")
                .signWith(SignatureAlgorithm.HS512, getSigningKey())
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    public UUID getUserIdFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            log.info("validated token");
            return true;
        } catch (Exception e) {
            log.error("failed to validate token: {}", e.getMessage());
            return false;
        }
    }

    public long getExpiration(String token ) {
        return extractAllClaims(token).getExpiration().getTime();
    }
}
