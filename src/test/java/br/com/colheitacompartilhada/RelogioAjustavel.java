package br.com.colheitacompartilhada;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * Relógio dos testes: começa no horário real e pode ser adiantado para simular a passagem do tempo.
 * Mesma precisão de microssegundos do relógio da aplicação (ClockConfig).
 */
public class RelogioAjustavel extends Clock {

    private Duration adiantado = Duration.ZERO;

    public void avancar(Duration duracao) {
        adiantado = adiantado.plus(duracao);
    }

    public void reiniciar() {
        adiantado = Duration.ZERO;
    }

    @Override
    public Instant instant() {
        return Instant.now().plus(adiantado).truncatedTo(ChronoUnit.MICROS);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}
