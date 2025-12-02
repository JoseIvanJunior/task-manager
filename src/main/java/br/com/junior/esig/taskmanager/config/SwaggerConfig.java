package br.com.junior.esig.taskmanager.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081/api")
                                .description("Servidor Local com Context Path /api")
                ))
                .info(new Info()
                        .title("Task Manager API - ESIG Challenge")
                        .description("""
                            API completa de gerenciamento de tarefas com autenticação JWT.
                            
                            ## Acesso:
                            - **Swagger UI**: http://localhost:8081/api/swagger-ui.html
                            - **OpenAPI JSON**: http://localhost:8081/api/api-docs
                            
                            ## Autenticação:
                            1. Faça login em `/auth/login` ou registre-se em `/auth/register`
                            2. Copie o token retornado
                            3. Clique no botão "Authorize" no topo do Swagger
                            4. Digite: `Bearer seu_token_aqui`
                            """)
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("JWT"))
                .components(new Components()
                        .addSecuritySchemes("JWT",
                                new SecurityScheme()
                                        .name("JWT")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Insira o token JWT no formato: Bearer {token}")
                        ));
    }
}