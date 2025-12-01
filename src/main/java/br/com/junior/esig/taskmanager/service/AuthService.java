package br.com.junior.esig.taskmanager.service;

import br.com.junior.esig.taskmanager.domain.enums.Role;
import br.com.junior.esig.taskmanager.domain.model.User;
import br.com.junior.esig.taskmanager.dto.auth.LoginRequest;
import br.com.junior.esig.taskmanager.dto.auth.LoginResponse;
import br.com.junior.esig.taskmanager.repository.UserRepository;
import br.com.junior.esig.taskmanager.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public LoginResponse login(LoginRequest request) {
        log.info("Autenticando usuário: {}", request.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtUtil.generateToken(request.getUsername());

        return new LoginResponse(token);
    }

    public LoginResponse register(LoginRequest request) {
        return createUser(request, Role.ROLE_USER);
    }

    public void createAdmin(LoginRequest request) {
        createUser(request, Role.ROLE_ADMIN);
        log.info("Novo ADMINISTRADOR criado: {}", request.getUsername());
    }

    private LoginResponse createUser(LoginRequest request, Role role) {
        log.info("Criando novo usuário: {} com perfil {}", request.getUsername(), role);

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Erro: Usuário já existe!");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername());
        return new LoginResponse(token);
    }
}