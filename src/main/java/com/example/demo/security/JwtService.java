package com.example.demo.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private static final long DEFAULT_EXPIRATION_TIME = 1000 * 60 * 60 * 24; // 24 hours

    private final SecretKey key;

    private final long expirationTime;

    /**
     * The signing secret is configuration, not code: it is read from
     * {@code phone-shop.jwt.secret}, which is supplied by the environment
     * (PHONE_SHOP_JWT_SECRET) or by the untracked application-local.properties.
     * Token lifetime comes from {@code phone-shop.jwt.expiration-ms}.
     */
    public JwtService(
            @Value("${phone-shop.jwt.secret:}") String secret,
            @Value("${phone-shop.jwt.expiration-ms:"
                    + DEFAULT_EXPIRATION_TIME + "}") long expirationTime
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "The JWT signing secret is not configured. Set the "
                            + "PHONE_SHOP_JWT_SECRET environment variable or add "
                            + "phone-shop.jwt.secret to application-local.properties."
            );
        }

        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationTime = expirationTime;
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        Date expiration = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();

        return expiration.before(new Date());
    }

}
