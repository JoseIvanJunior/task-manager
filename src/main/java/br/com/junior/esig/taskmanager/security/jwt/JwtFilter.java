package br.com.junior.esig.taskmanager.security.jwt;

import br.com.junior.esig.taskmanager.security.config.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    // ✅ LISTA COMPLETA DE URLS PÚBLICAS
    private static final List<String> PUBLIC_URLS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/api-docs",
            "/api/api-docs/**",
            "/api/swagger-ui",
            "/api/swagger-ui/**",
            "/api/swagger-ui.html",
            "/api/v3/api-docs",
            "/api/v3/api-docs/**",
            "/api/swagger-resources",
            "/api/swagger-resources/**",
            "/api/configuration/ui",
            "/api/configuration/security",
            "/api/webjars/**",
            "/api/favicon.ico"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        log.debug("Processando requisição: {}", requestURI);

        // ✅ SE FOR URL PÚBLICA, PASSE DIRETO SEM PROCESSAR JWT
        if (isPublicUrl(requestURI)) {
            log.debug("URL pública, ignorando JWT: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = extractJwtFromRequest(request);

            if (jwt != null && jwtUtil.validateToken(jwt)) {
                String username = jwtUtil.getUsernameFromToken(jwt);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("Usuário '{}' autenticado via JWT para: {}", username, requestURI);
                }
            } else if (jwt != null) {
                log.warn("Token JWT inválido para: {}", requestURI);
            }
        } catch (Exception e) {
            log.error("Erro ao processar JWT para {}: {}", requestURI, e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicUrl(String requestURI) {
        return PUBLIC_URLS.stream().anyMatch(requestURI::startsWith);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTH_HEADER);

        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}