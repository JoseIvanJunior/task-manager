package br.com.junior.esig.taskmanager.security;

import br.com.junior.esig.taskmanager.security.jwt.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void shouldGenerateAndValidateToken() {
        // Given
        String username = "testuser";

        // When
        String token = jwtUtil.generateToken(username);

        // Then
        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
        assertEquals(username, jwtUtil.getUsernameFromToken(token));
    }

    @Test
    void shouldRejectInvalidToken() {
        assertFalse(jwtUtil.validateToken("token.invalido.aqui"));
    }

    @Test
    void shouldRejectMalformedToken() {
        assertFalse(jwtUtil.validateToken("eyJhbGciOiJIUzI1NiJ9.malformed.token"));
    }

    @Test
    void shouldRejectEmptyToken() {
        assertFalse(jwtUtil.validateToken(""));
    }
}