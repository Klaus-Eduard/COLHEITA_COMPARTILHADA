package br.com.colheitacompartilhada.usuario;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import br.com.colheitacompartilhada.comum.MuitasTentativasException;
import org.springframework.stereotype.Component;

/** Limite de login em memória (D11): funciona porque existe uma única instância da aplicação (D15). */
@Component
public class LoginRateLimiter {

    static final int MAX_FALHAS = 5;
    static final Duration JANELA = Duration.ofMinutes(15);

    private final Map<String, Deque<Instant>> falhasPorEmail = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public void verificar(String email) {
        Deque<Instant> falhas = falhasPorEmail.computeIfPresent(chave(email), (chave, atuais) -> recentes(atuais));
        if (falhas != null && falhas.size() >= MAX_FALHAS) {
            throw new MuitasTentativasException("Muitas tentativas. Tente novamente em alguns minutos.");
        }
    }

    public void registrarFalha(String email) {
        falhasPorEmail.compute(chave(email), (chave, atuais) -> {
            Deque<Instant> falhas = atuais == null ? new ArrayDeque<>() : atuais;
            falhas.addLast(clock.instant());
            return falhas;
        });
    }

    public void limpar(String email) {
        falhasPorEmail.remove(chave(email));
    }

    // Devolver null remove a entrada do mapa: e-mails sem falha recente não ocupam memória.
    private Deque<Instant> recentes(Deque<Instant> falhas) {
        Instant limite = clock.instant().minus(JANELA);
        falhas.removeIf(falha -> !falha.isAfter(limite));
        return falhas.isEmpty() ? null : falhas;
    }

    private String chave(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
