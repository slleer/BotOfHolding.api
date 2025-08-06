// Create new file: api/src/main/java/com/botofholding/api/Security/JwtService.java
package com.botofholding.api.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    // It is critical to store this secret securely and not in the code.
    // We will read it from application.properties.
    @Value("${jwt.secret}")
    private String secret;

    // The principal name we will use for the bot's token.
    private static final String BOT_PRINCIPAL_NAME = "bot-service-account";

    /**
     * Generates a long-lived JWT for the bot service account.
     */
    public String generateBotToken() {
        //TODO
        // This token is designed to be very long-lived. In a real production system,
        // you might have a mechanism to rotate it.
        long expirationTime = 1000L * 60 * 60 * 24 * 365 * 10; // 10 years
        return Jwts.builder()
                .subject(BOT_PRINCIPAL_NAME)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSignInKey())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSignInKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            // Log the exception if needed, but for validation, just return false.
            return false;
        }
    }

    public String getPrincipalFromToken(String token) {
        return getClaim(token, Claims::getSubject);
    }

    private <T> T getClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }

    private SecretKey getSignInKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}