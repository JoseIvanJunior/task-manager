package br.com.junior.esig.taskmanager.integration;

import br.com.junior.esig.taskmanager.domain.model.User;
import br.com.junior.esig.taskmanager.domain.enums.Role;
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

        // Limpa e prepara o banco para cada teste
        userRepository.deleteAll();

        // Cria um usuário de teste
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
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getToken());

        // Verifica se o usuário foi salvo no banco
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

        // Verifica se o token é válido
        String token = response.getBody().getToken();
        assertEquals("testuser", jwtUtil.getUsernameFromToken(token));
    }

    @Test
    void shouldRejectLoginWithWrongPassword() {
        // Given
        LoginRequest loginRequest = new LoginRequest("testuser", "wrongpassword");

        // When
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                baseUrl + "/auth/login",
                loginRequest,
                LoginResponse.class
        );

        // Then - Deve retornar UNAUTHORIZED
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().getToken());
    }

    @Test
    void shouldRejectLoginWithNonExistentUser() {
        // Given
        LoginRequest loginRequest = new LoginRequest("nonexistent", "password");

        // When
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                baseUrl + "/auth/login",
                loginRequest,
                LoginResponse.class
        );

        // Then - Deve retornar UNAUTHORIZED
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().getToken());
    }

    @Test
    void shouldPreventDuplicateUsernameRegistration() {
        // Given - Usuário já existe
        LoginRequest registerRequest = new LoginRequest("testuser", "password123");

        // When
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                baseUrl + "/auth/register",
                registerRequest,
                LoginResponse.class
        );

        // Then - Deve retornar BAD_REQUEST
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().getToken());
    }

    @Test
    void shouldProtectEndpointsWithoutToken() {
        // When - Tenta acessar endpoint protegido sem token
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/tasks",
                String.class
        );

        // Then - Deve retornar FORBIDDEN ou UNAUTHORIZED
        assertTrue(response.getStatusCode() == HttpStatus.FORBIDDEN ||
                response.getStatusCode() == HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldAccessProtectedEndpointWithValidToken() {
        // Given - Gera um token válido
        String token = jwtUtil.generateToken("testuser");

        // When - Acessa endpoint com token
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/tasks",
                HttpMethod.GET,
                entity,
                String.class
        );

        // Then - Deve conseguir acesso
        assertTrue(response.getStatusCode().is2xxSuccessful());
    }

    @Test
    void shouldRejectAccessWithInvalidToken() {
        // Given - Token inválido
        String invalidToken = "invalid.token.here";

        // When - Tenta acessar com token inválido
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(invalidToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/tasks",
                HttpMethod.GET,
                entity,
                String.class
        );

        // Then - Deve retornar FORBIDDEN ou UNAUTHORIZED
        assertTrue(response.getStatusCode() == HttpStatus.FORBIDDEN ||
                response.getStatusCode() == HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldRejectAccessWithMalformedToken() {
        // Given - Token malformado
        String malformedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                "eyJzdWIiOiJ0ZXN0dXNlciIsImlhdCI6MTUxNjIzOTAyMiwiZXhwIjoxNTE2MjM5MDIyfQ." +
                "invalid_signature";

        // When - Tenta acessar com token malformado
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(malformedToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/tasks",
                HttpMethod.GET,
                entity,
                String.class
        );

        // Then - Deve retornar FORBIDDEN ou UNAUTHORIZED
        assertTrue(response.getStatusCode() == HttpStatus.FORBIDDEN ||
                response.getStatusCode() == HttpStatus.UNAUTHORIZED);
    }
}