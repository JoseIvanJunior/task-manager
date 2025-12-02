package br.com.junior.esig.taskmanager.controller;

import br.com.junior.esig.taskmanager.dto.auth.LoginRequest;
import br.com.junior.esig.taskmanager.dto.auth.LoginResponse;
import br.com.junior.esig.taskmanager.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints para autenticação e registro")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Login de usuário", description = "Autentica um usuário e retorna um token JWT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas"),
            @ApiResponse(responseCode = "500", description = "Erro interno")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse response = authService.login(loginRequest);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Usuário ou senha inválidos");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno: " + e.getMessage());
        }
    }

    @Operation(summary = "Registro de usuário", description = "Registra um novo usuário comum")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou usuário já existe"),
            @ApiResponse(responseCode = "500", description = "Erro interno")
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse response = authService.register(loginRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao registrar usuário");
        }
    }

    @Operation(summary = "Criar administrador", description = "Cria um novo usuário administrador (apenas para ADMINs)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Administrador criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou usuário já existe"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - apenas ADMINs"),
            @ApiResponse(responseCode = "500", description = "Erro interno")
    })
    @PostMapping("/create-admin")
    public ResponseEntity<?> createAdmin(@RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse response = authService.createAdmin(loginRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao criar administrador");
        }
    }
}