package com.exchange.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.nio.charset.StandardCharsets;

@Service
public class JwtService {

    private final String secretKey;
    private static final long EXPIRATION_TIME = 86400000; // 24 hours

    public JwtService(@Value("${app.security.jwt.secret}") String secretKey) {
        this.secretKey = secretKey;
    }

    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractUsername(String token) {
        return extractClaim(token, claims -> claims.get("username", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(String userId, String username) {
        return generateToken(new HashMap<>(), userId, username, 0);
    }

    public String generateToken(String userId, String username, int tokenVersion) {
        return generateToken(new HashMap<>(), userId, username, tokenVersion);
    }

    public String generateToken(Map<String, Object> extraClaims, String userId, String username) {
        return generateToken(extraClaims, userId, username, 0);
    }

    public String generateToken(Map<String, Object> extraClaims, String userId, String username, int tokenVersion) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userId)
                .claim("username", username)
                .claim("tokenVersion", tokenVersion)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, String userId, String username, int tokenVersion) {
        final String extractedUserId = extractUserId(token);
        final String extractedUsername = extractUsername(token);
        final int extractedTokenVersion = extractTokenVersion(token);
        return extractedUserId.equals(userId)
                && extractedUsername.equals(username)
                && extractedTokenVersion == tokenVersion
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private int extractTokenVersion(String token) {
        Integer tokenVersion = extractClaim(token, claims -> claims.get("tokenVersion", Integer.class));
        return tokenVersion != null ? tokenVersion : 0;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}
