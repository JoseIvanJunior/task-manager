package br.com.junior.esig.taskmanager.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String tokenType = "Bearer";
    private String role;

    // Construtor para compatibilidade
    public LoginResponse(String token) {
        this.token = token;
        this.tokenType = "Bearer";
        this.role = "ROLE_USER";
    }

    public LoginResponse(String token, String role) {
        this.token = token;
        this.tokenType = "Bearer";
        this.role = role;
    }
}