package br.com.junior.esig.taskmanager.controller;

import br.com.junior.esig.taskmanager.domain.model.User;
import br.com.junior.esig.taskmanager.domain.enums.Role;
import br.com.junior.esig.taskmanager.dto.auth.LoginRequest;
import br.com.junior.esig.taskmanager.dto.auth.LoginResponse;
import br.com.junior.esig.taskmanager.repository.UserRepository;
import br.com.junior.esig.taskmanager.security.jwt.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        try {
            System.out.println("Tentativa de login para usuário: " + loginRequest.getUsername());

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = jwtUtil.generateToken(loginRequest.getUsername());

            System.out.println("Login bem-sucedido para usuário: " + loginRequest.getUsername());
            return ResponseEntity.ok(new LoginResponse(token));

        } catch (BadCredentialsException e) {
            System.out.println("Falha no login para usuário " + loginRequest.getUsername() + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(null));
        } catch (Exception e) {
            System.out.println("Erro inesperado no login: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(null));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@RequestBody LoginRequest loginRequest) {
        try {
            System.out.println("Tentativa de registro para usuário: " + loginRequest.getUsername());

            if (userRepository.findByUsername(loginRequest.getUsername()).isPresent()) {
                System.out.println("Tentativa de registro com usuário já existente: " + loginRequest.getUsername());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new LoginResponse(null));
            }

            User user = User.builder()
                    .username(loginRequest.getUsername())
                    .password(passwordEncoder.encode(loginRequest.getPassword()))
                    .role(Role.ROLE_USER)
                    .build();

            userRepository.save(user);

            String token = jwtUtil.generateToken(loginRequest.getUsername());

            System.out.println("Usuário registrado com sucesso: " + loginRequest.getUsername());
            return ResponseEntity.ok(new LoginResponse(token));

        } catch (Exception e) {
            System.out.println("Erro no registro: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new LoginResponse(null));
        }
    }
}