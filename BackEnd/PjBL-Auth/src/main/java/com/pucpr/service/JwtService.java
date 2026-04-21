package com.pucpr.service;

import com.pucpr.model.Usuario;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtService {

    private final String SECRET_KEY;

    public JwtService() {
        String envKey = System.getenv("JWT_SECRET");
        if (envKey == null || envKey.trim().isEmpty()) {
            throw new IllegalStateException(
                "Variável de ambiente JWT_SECRET não definida. " +
                "Defina-a com pelo menos 32 caracteres antes de iniciar o servidor."
            );
        }
        if (envKey.getBytes().length < 32) {
            throw new IllegalStateException(
                "JWT_SECRET deve ter pelo menos 32 bytes para o algoritmo HS256."
            );
        }
        this.SECRET_KEY = envKey;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    /**
     * Gera um JWT assinado com HS256 contendo o e-mail (subject), role e expiração de 15 min.
     */
    public String generateToken(Usuario user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900_000)) // 15 min
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extrai o e-mail (subject) de um token válido.
     */
    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Valida assinatura e expiração do token.
     * Retorna true se válido, false caso contrário.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.err.println("Token inválido: " + e.getMessage());
            return false;
        }
    }
}
