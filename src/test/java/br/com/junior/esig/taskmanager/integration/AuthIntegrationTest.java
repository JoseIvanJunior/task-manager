package br.com.junior.esig.taskmanager.integration;

import br.com.junior.esig.taskmanager.domain.enums.Role;
import br.com.junior.esig.taskmanager.domain.model.User;
import br.com.junior.esig.taskmanager.dto.auth.LoginRequest;
import br.com.junior.esig.taskmanager.dto.auth.LoginResponse;
import br.com.junior.esig.taskmanager.repository.UserRepository;
import br.com.junior.esig.taskmanager.security.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
class AuthIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";

        // Limpa o banco antes de cada teste para garantir isolamento
        userRepository.deleteAll();

        // Cria um usuário base para testes de login
        User testUser = User.builder()
                .username("testuser")
                .password(passwordEncoder.encode("password123"))
                .role(Role.ROLE_USER)
                .build();
        userRepository.save(testUser);
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        // Given
        LoginRequest registerRequest = new LoginRequest("newuser", "newpassword123");

        // When
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                baseUrl + "/auth/register",
                registerRequest,
                LoginResponse.class
        );

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getToken());

        // Verifica se persistiu no banco
        assertTrue(userRepository.findByUsername("newuser").isPresent());
    }

    @Test
    void shouldLoginUserSuccessfully() {
        // Given
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");

        // When
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                baseUrl + "/auth/login",
                loginRequest,
                LoginResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getToken());

        // Verifica validade do token
        String token = response.getBody().getToken();
        assertEquals("testuser", jwtUtil.getUsernameFromToken(token));
    }

    @Test
    void shouldRejectLoginWithWrongPassword() {
        // Given
        LoginRequest loginRequest = new LoginRequest("testuser", "wrongpassword");

        // When - Usamos String.class porque o erro não retorna LoginResponse JSON
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/auth/login",
                loginRequest,
                String.class
        );

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void shouldRejectLoginWithNonExistentUser() {
        // Given
        LoginRequest loginRequest = new LoginRequest("nonexistent", "password");

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/auth/login",
                loginRequest,
                String.class
        );

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void shouldPreventDuplicateUsernameRegistration() {
        // Given - Usuário "testuser" já existe (criado no setUp)
        LoginRequest registerRequest = new LoginRequest("testuser", "password123");

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/auth/register",
                registerRequest,
                String.class
        );

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldProtectEndpointsWithoutToken() {
        // When - Tenta acessar endpoint protegido sem token
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/tasks",
                String.class
        );

        // Then
        assertTrue(response.getStatusCode() == HttpStatus.FORBIDDEN ||
                response.getStatusCode() == HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldAccessProtectedEndpointWithValidToken() {
        // Given
        String token = jwtUtil.generateToken("testuser");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/tasks",
                HttpMethod.GET,
                entity,
                String.class
        );

        // Then - Espera OK (200) pois o usuário existe e tem token válido
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldRejectAccessWithInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(invalidToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/tasks",
                HttpMethod.GET,
                entity,
                String.class
        );

        // Then
        assertTrue(response.getStatusCode() == HttpStatus.FORBIDDEN ||
                response.getStatusCode() == HttpStatus.UNAUTHORIZED);
    }
}