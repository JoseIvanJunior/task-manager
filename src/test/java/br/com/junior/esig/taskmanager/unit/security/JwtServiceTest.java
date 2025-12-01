package br.com.junior.esig.taskmanager.unit.security;

import br.com.junior.esig.taskmanager.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        userDetails = User.builder()
                .username("testuser")
                .password("encodedPassword")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    @Test
    void shouldGenerateTokenSuccessfully() {
        // When
        String token = jwtService.generateToken(userDetails);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        System.out.println("Token gerado: " + token);
    }

    @Test
    void shouldExtractUsernameFromToken() {
        // Given
        String token = jwtService.generateToken(userDetails);

        // When
        String username = jwtService.extractUsername(token);

        // Then
        assertEquals("testuser", username);
    }

    @Test
    void shouldValidateTokenSuccessfully() {
        // Given
        String token = jwtService.generateToken(userDetails);

        // When & Then
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void shouldRejectInvalidToken() {
        // Given - Token completamente inválido
        String invalidToken = "invalid.token.here";

        // When & Then - Deve retornar false sem lançar exceção
        assertFalse(jwtService.isTokenValid(invalidToken, userDetails));
    }

    @Test
    void shouldRejectMalformedToken() {
        // Given - Token com estrutura inválida
        String malformedToken = "header.payload.signature";

        // When & Then - Deve retornar false sem lançar exceção
        assertFalse(jwtService.isTokenValid(malformedToken, userDetails));
    }

    @Test
    void shouldRejectEmptyToken() {
        // Given - Token vazio
        String emptyToken = "";

        // When & Then - Deve retornar false sem lançar exceção
        assertFalse(jwtService.isTokenValid(emptyToken, userDetails));
    }

    @Test
    void shouldRejectNullToken() {
        // Given - Token nulo
        String nullToken = null;

        // When & Then - Deve retornar false sem lançar exceção
        assertFalse(jwtService.isTokenValid(nullToken, userDetails));
    }

    @Test
    void shouldRejectTokenForDifferentUser() {
        // Given
        String token = jwtService.generateToken(userDetails);

        UserDetails differentUser = User.builder()
                .username("differentuser")
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        // When & Then
        assertFalse(jwtService.isTokenValid(token, differentUser));
    }

    @Test
    void shouldExtractUsernameFromValidToken() {
        // Given
        String token = jwtService.generateToken(userDetails);

        // When
        String username = jwtService.extractUsername(token);

        // Then
        assertEquals("testuser", username);
    }
}