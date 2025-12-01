package br.com.junior.esig.taskmanager.integration;

import br.com.junior.esig.taskmanager.domain.enums.Priority;
import br.com.junior.esig.taskmanager.domain.enums.Role;
import br.com.junior.esig.taskmanager.domain.enums.TaskStatus;
import br.com.junior.esig.taskmanager.domain.model.User;
import br.com.junior.esig.taskmanager.dto.task.TaskRequest;
import br.com.junior.esig.taskmanager.dto.task.TaskResponse;
import br.com.junior.esig.taskmanager.repository.TaskRepository;
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

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
class TaskIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String baseUrl;
    private String tokenUser1;
    private String tokenUser2;
    private String tokenAdmin;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";
        taskRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Cria Usuário Comum 1
        User user1 = createUser("user1", Role.ROLE_USER);
        tokenUser1 = jwtUtil.generateToken(user1.getUsername());

        // 2. Cria Usuário Comum 2 (O "Invasor")
        User user2 = createUser("user2", Role.ROLE_USER);
        tokenUser2 = jwtUtil.generateToken(user2.getUsername());

        // 3. Cria Admin
        User admin = createUser("admin", Role.ROLE_ADMIN);
        tokenAdmin = jwtUtil.generateToken(admin.getUsername());
    }

    // ============================================================================================
    // 1. TESTES DE HAPPY PATH (Caminho Feliz: CRUD)
    // ============================================================================================

    @Test
    void shouldCreateTaskSuccessfully() {
        // Given
        TaskRequest request = createTaskRequest("Minha Tarefa");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenUser1);

        // When
        ResponseEntity<TaskResponse> response = restTemplate.postForEntity(
                baseUrl + "/tasks",
                new HttpEntity<>(request, headers),
                TaskResponse.class
        );

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Minha Tarefa", response.getBody().getTitle());
        assertEquals(TaskStatus.TODO, response.getBody().getStatus());
    }

    @Test
    void shouldUpdateTaskSuccessfully() {
        // Given - Cria uma tarefa inicial
        TaskRequest createRequest = createTaskRequest("Tarefa Original");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenUser1);

        ResponseEntity<TaskResponse> createResponse = restTemplate.postForEntity(
                baseUrl + "/tasks",
                new HttpEntity<>(createRequest, headers),
                TaskResponse.class
        );
        Long taskId = createResponse.getBody().getId();

        // When - Atualiza (PUT)
        TaskRequest updateRequest = createTaskRequest("Tarefa Atualizada");
        updateRequest.setPriority(Priority.HIGH); // Mudando prioridade

        ResponseEntity<TaskResponse> updateResponse = restTemplate.exchange(
                baseUrl + "/tasks/" + taskId,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, headers),
                TaskResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertEquals("Tarefa Atualizada", updateResponse.getBody().getTitle());
        assertEquals(Priority.HIGH, updateResponse.getBody().getPriority());
    }

    @Test
    void shouldCompleteTaskSuccessfully() {
        // Given
        TaskRequest createRequest = createTaskRequest("Tarefa Pendente");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenUser1);

        ResponseEntity<TaskResponse> createResponse = restTemplate.postForEntity(
                baseUrl + "/tasks",
                new HttpEntity<>(createRequest, headers),
                TaskResponse.class
        );
        Long taskId = createResponse.getBody().getId();

        // When - PATCH para completar
        ResponseEntity<TaskResponse> completeResponse = restTemplate.exchange(
                baseUrl + "/tasks/" + taskId + "/complete",
                HttpMethod.PATCH,
                new HttpEntity<>(headers),
                TaskResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, completeResponse.getStatusCode());
        assertEquals(TaskStatus.DONE, completeResponse.getBody().getStatus());
    }

    // ============================================================================================
    // 2. TESTES DE SEGURANÇA
    // ============================================================================================

    @Test
    void userShouldNOTDeleteOtherUsersTask() {
        // 1. User1 cria uma tarefa
        TaskRequest request = createTaskRequest("Tarefa do User 1");
        HttpHeaders headerUser1 = new HttpHeaders();
        headerUser1.setBearerAuth(tokenUser1);

        ResponseEntity<TaskResponse> createdResponse = restTemplate.postForEntity(
                baseUrl + "/tasks",
                new HttpEntity<>(request, headerUser1),
                TaskResponse.class
        );
        Long taskId = createdResponse.getBody().getId();

        // 2. User2 tenta deletar a tarefa do User1
        HttpHeaders headerUser2 = new HttpHeaders();
        headerUser2.setBearerAuth(tokenUser2);

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                baseUrl + "/tasks/" + taskId,
                HttpMethod.DELETE,
                new HttpEntity<>(headerUser2),
                Void.class
        );

        // 3. Deve ser proibido (403)
        assertEquals(HttpStatus.FORBIDDEN, deleteResponse.getStatusCode());

        // 4. Verifica se a tarefa ainda existe no banco
        assertTrue(taskRepository.existsById(taskId));
    }

    @Test
    void adminSHOULDDeleteAnyTask() {
        // 1. User1 cria uma tarefa
        TaskRequest request = createTaskRequest("Tarefa para o Admin apagar");
        HttpHeaders headerUser1 = new HttpHeaders();
        headerUser1.setBearerAuth(tokenUser1);

        ResponseEntity<TaskResponse> createdResponse = restTemplate.postForEntity(
                baseUrl + "/tasks",
                new HttpEntity<>(request, headerUser1),
                TaskResponse.class
        );
        Long taskId = createdResponse.getBody().getId();

        // 2. Admin tenta deletar
        HttpHeaders headerAdmin = new HttpHeaders();
        headerAdmin.setBearerAuth(tokenAdmin);

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                baseUrl + "/tasks/" + taskId,
                HttpMethod.DELETE,
                new HttpEntity<>(headerAdmin),
                Void.class
        );

        // 3. Deve permitir (204 No Content)
        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());

        // 4. Tarefa deve ter sumido
        assertFalse(taskRepository.existsById(taskId));
    }

    // ============================================================================================
    // 3. TESTES DE VALIDAÇÃO E ERRO
    // ============================================================================================

    @Test
    void shouldFailToCreateTaskWithoutTitle() {
        // Given - Tarefa sem título (violando @NotBlank)
        TaskRequest invalidRequest = createTaskRequest(null);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenUser1);

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/tasks",
                new HttpEntity<>(invalidRequest, headers),
                String.class
        );

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturn404WhenTaskNotFound() {
        // Given
        Long nonExistentId = 9999L;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenUser1);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/tasks/" + nonExistentId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ============================================================================================
    // MÉTODOS AUXILIARES
    // ============================================================================================

    private User createUser(String username, Role role) {
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode("password"))
                .role(role)
                .build();
        return userRepository.save(user);
    }

    private TaskRequest createTaskRequest(String title) {
        TaskRequest req = new TaskRequest();
        req.setTitle(title);
        req.setDescription("Descrição teste");
        req.setResponsible("Tester");
        req.setPriority(Priority.MEDIUM);
        req.setDeadline(LocalDate.now().plusDays(5));
        req.setStatus(TaskStatus.TODO);
        req.setUserId(1L); // Irrelevante para user comum
        return req;
    }
}