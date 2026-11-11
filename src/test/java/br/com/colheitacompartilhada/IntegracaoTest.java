package br.com.colheitacompartilhada;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base dos testes de integração: um único PostgreSQL (Testcontainers) e um único contexto Spring
 * para todas as classes, o que deixa a suíte rápida. Como o banco é compartilhado, cada teste
 * cria os próprios dados com e-mails únicos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegracaoTest.RelogioConfig.class)
public abstract class IntegracaoTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected RelogioAjustavel relogio;

    @AfterEach
    void reiniciarRelogio() {
        relogio.reiniciar();
    }

    @TestConfiguration
    static class RelogioConfig {

        @Bean
        @Primary
        RelogioAjustavel relogioAjustavel() {
            return new RelogioAjustavel();
        }
    }
}
