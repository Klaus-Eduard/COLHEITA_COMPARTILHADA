package br.com.colheitacompartilhada.config;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    public static final ZoneId FUSO_NEGOCIO = ZoneId.of("America/Sao_Paulo");

    /** "Hoje" do negócio: a data em São Paulo, nunca a do servidor (que roda em UTC). */
    public static LocalDate hoje(Clock clock) {
        return LocalDate.now(clock.withZone(FUSO_NEGOCIO));
    }

    // Precisão de microssegundos, a mesma do timestamptz: o instante devolvido pela API é igual ao gravado no banco.
    @Bean
    Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
    }
}
