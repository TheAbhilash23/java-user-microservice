package com.example.user.core.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class JwtUtility {

    @Value("${spring.application.secret-key-jwt}")
    private String SECRET_KEY;

    @Value("${spring.application.jwt.access-token-lifetime}")
    private Integer ACCESS_TOKEN_LIFETIME;

    private SecretKey getSecretKey() {
        byte[] keyBytes = SECRET_KEY.getBytes(); // based on key length the algorithm will be decided.
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Optional<Claims> extractAllSignedClaims(String rawToken) {
        try {
            JwtParser jwtParser = Jwts.parserBuilder().setSigningKey(getSecretKey()).build();
            Claims claims = jwtParser.parseClaimsJws(rawToken).getBody();
            if (claims.getExpiration().before(new Date())) {
                throw new JwtException("Token Expired get a new token");
            }
            return Optional.of(claims);
        } catch (JwtException e) {
            System.out.println("Bad Token");
            return Optional.empty();
        }
    }

    private String createToken(Map<String, Object> claims, String username) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setHeader(Map.of("TYP", "JWT"))
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + (long) (ACCESS_TOKEN_LIFETIME) * 60 * 1000))
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }

}
