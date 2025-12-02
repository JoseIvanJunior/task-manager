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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("Autenticando usuário: {}", request.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtUtil.generateToken(request.getUsername());

        // Obter role do usuário autenticado
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_USER");

        return new LoginResponse(token, role);
    }

    @Transactional
    public LoginResponse register(LoginRequest request) {
        return createUser(request, Role.ROLE_USER);
    }

    @Transactional
    public LoginResponse createAdmin(LoginRequest request) {
        // Verificar se o usuário atual é ADMIN
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new AccessDeniedException("Apenas administradores podem criar outros administradores");
        }

        LoginResponse response = createUser(request, Role.ROLE_ADMIN);
        log.info("Novo ADMINISTRADOR criado: {}", request.getUsername());
        return response;
    }

    @Transactional
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
        return new LoginResponse(token, role.name());
    }
}