package br.com.junior.esig.taskmanager;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@OpenAPIDefinition(
		info = @Info(
				title = "Task Manager API",
				version = "1.0",
				description = "API para gerenciamento de tarefas"
		)
)

public class TaskManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaskManagerApplication.class, args);
		System.out.println("🚀 Task Manager Backend iniciado!");
		System.out.println("📚 Swagger UI: http://localhost:8081/api/swagger-ui.html");
		System.out.println("🔑 Autenticação: POST http://localhost:8081/api/auth/login");
		System.out.println("👤 Registro: POST http://localhost:8081/api/auth/register");
	}

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/api/**")
						.allowedOrigins("http://localhost:4200")
						.allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
						.allowedHeaders("*")
						.allowCredentials(true)
						.maxAge(3600);
			}
		};
	}
}