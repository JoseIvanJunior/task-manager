// src/test/java/br/com/junior/esig/taskmanager/integration/SimpleJwtIntegrationTest.java
package br.com.junior.esig.taskmanager.integration;

import br.com.junior.esig.taskmanager.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class SimpleJwtIntegrationTest {

    @Autowired
    private JwtService jwtService;

    @Test
    void contextLoads() {
        assertNotNull(jwtService);
    }

    @Test
    void jwtServiceShouldBeConfigured() {
        assertNotNull(jwtService);
        // Se chegou aqui, significa que as propriedades foram carregadas corretamente
    }
}