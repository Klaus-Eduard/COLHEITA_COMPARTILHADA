package br.com.colheitacompartilhada.anuncio;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ExpiracaoJob {

    private static final Logger log = LoggerFactory.getLogger(ExpiracaoJob.class);

    private final AnuncioRepository anuncioRepository;
    private final Clock clock;

    public ExpiracaoJob(AnuncioRepository anuncioRepository, Clock clock) {
        this.anuncioRepository = anuncioRepository;
        this.clock = clock;
    }

    @Scheduled(
        fixedDelayString = "${app.expiracao-intervalo:PT1H}",
        initialDelayString = "PT1M"
    )
    @Transactional
    public void expirarAnunciosVencidos() {
        Instant agora = Instant.now(clock);
        int quantidade = anuncioRepository.expirarVencidos(agora);

        log.info("Expiração automática concluída. Anúncios expirados: {}", quantidade);
    }
}