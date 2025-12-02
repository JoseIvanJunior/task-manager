package br.com.junior.esig.taskmanager.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    private SecretKey getSigningKey() {
        try {
            // Se o secret estiver em Base64, decode; caso contrário, usa como está
            if (secret.matches("^[A-Za-z0-9+/]+=*$") && secret.length() % 4 == 0) {
                byte[] keyBytes = Decoders.BASE64.decode(secret);
                return Keys.hmacShaKeyFor(keyBytes);
            } else {
                // Se não for Base64, converte a string para bytes
                byte[] keyBytes = secret.getBytes();
                return Keys.hmacShaKeyFor(keyBytes);
            }
        } catch (Exception e) {
            log.error("Erro ao criar chave de assinatura JWT: {}", e.getMessage());
            throw new IllegalArgumentException("Chave JWT inválida", e);
        }
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Assinatura ou Token JWT inválido: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Token JWT expirado: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Token JWT não suportado: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Claims JWT vazios: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao validar token: {}", e.getMessage());
        }
        return false;
    }

    public String getUsernameFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (Exception e) {
            log.error("Erro ao extrair username do token: {}", e.getMessage());
            throw new JwtException("Token inválido");
        }
    }
}