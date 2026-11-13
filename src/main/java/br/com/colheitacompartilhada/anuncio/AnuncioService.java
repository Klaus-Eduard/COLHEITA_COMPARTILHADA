package br.com.colheitacompartilhada.anuncio;

import static br.com.colheitacompartilhada.anuncio.AnuncioSpecs.comTexto;
import static br.com.colheitacompartilhada.anuncio.AnuncioSpecs.daCategoria;
import static br.com.colheitacompartilhada.anuncio.AnuncioSpecs.daModalidade;
import static br.com.colheitacompartilhada.anuncio.AnuncioSpecs.doBairro;
import static br.com.colheitacompartilhada.anuncio.AnuncioSpecs.visivel;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import br.com.colheitacompartilhada.comum.NaoEncontradoException;
import br.com.colheitacompartilhada.comum.Pagina;
import br.com.colheitacompartilhada.config.ClockConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnuncioService {

    private static final Sort MAIS_RECENTES = Sort.by(Sort.Order.desc("criadoEm"), Sort.Order.desc("id"));

    private final AnuncioRepository anuncios;
    private final Clock clock;

    public AnuncioService(AnuncioRepository anuncios, Clock clock) {
        this.anuncios = anuncios;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<Anuncio> buscar(FiltroAnuncios filtro, int pagina) {
        Specification<Anuncio> criterios = visivel(agora(), hoje())
                .and(comTexto(filtro.texto()))
                .and(doBairro(filtro.bairroId()))
                .and(daCategoria(filtro.categoriaId()))
                .and(daModalidade(filtro.modalidade()));
        Pageable paginacao = PageRequest.of(pagina, Pagina.TAMANHO, MAIS_RECENTES);
        return anuncios.findAll(criterios, paginacao);
    }

    // RN08: anúncio não visível é tratado como inexistente.
    @Transactional(readOnly = true)
    public Anuncio detalhar(UUID id) {
        return anuncios.findComAssociacoesById(id)
                .filter(anuncio -> anuncio.estaVisivel(agora(), hoje()))
                .orElseThrow(() -> new NaoEncontradoException("Anúncio não encontrado."));
    }

    private Instant agora() {
        return clock.instant();
    }

    private LocalDate hoje() {
        return LocalDate.now(clock.withZone(ClockConfig.FUSO_NEGOCIO));
    }
}
