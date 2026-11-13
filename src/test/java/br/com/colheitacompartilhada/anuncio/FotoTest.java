package br.com.colheitacompartilhada.anuncio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import br.com.colheitacompartilhada.IntegracaoTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/** Foto no banco: UC06.9–11 no nível do serviço e 04_API #20. O envio por HTTP entra com POST /anuncios (issue 9). */
class FotoTest extends IntegracaoTest {

    @Autowired
    CenarioAnuncios cenario;

    @Autowired
    FotoService fotoService;

    @Autowired
    AnuncioRepository anuncios;

    @Autowired
    TransactionTemplate transacao;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void uc06_9_fotoPngDeUmMegaFicaAcessivelPelaFotoUrl() throws Exception {
        UUID anuncioId = novoAnuncio();
        MockMultipartFile png = Imagens.png(1024 * 1024);

        String fotoUrl = salvarFoto(anuncioId, png);

        assertThat(fotoUrl).startsWith("/fotos/");
        mvc.perform(get(fotoUrl))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().longValue("Content-Length", 1024 * 1024))
                .andExpect(header().string("Cache-Control", "max-age=31536000, public, immutable"))
                .andExpect(content().bytes(png.getBytes()));
    }

    @Test
    void uc06_9_fotoJpegEServidaComOTipoCerto() throws Exception {
        String fotoUrl = salvarFoto(novoAnuncio(), Imagens.jpeg(2048));

        mvc.perform(get(fotoUrl)).andExpect(header().string("Content-Type", "image/jpeg"));
    }

    @Test
    void uc06_9_arquivoInvalidoNaoEGravado() {
        UUID anuncioId = novoAnuncio();
        MockMultipartFile texto = new MockMultipartFile("foto", "foto.png", "image/png", "texto".getBytes());

        assertThatThrownBy(() -> salvarFoto(anuncioId, texto)).hasMessageContaining("JPEG ou PNG");

        assertThat(fotosDo(anuncioId)).isZero();
        assertThat(anuncios.findById(anuncioId).orElseThrow().getFotoUrl()).isNull();
    }

    @Test
    void uc06_10_seAGravacaoDoAnuncioFalharNenhumaFotoFicaNoBanco() {
        UUID anuncioId = novoAnuncio();

        assertThatThrownBy(() -> transacao.executeWithoutResult(status -> {
            Anuncio anuncio = anuncios.findById(anuncioId).orElseThrow();
            fotoService.salvarPara(anuncio, Imagens.png(1024));
            throw new IllegalStateException("falha simulada ao gravar o anúncio");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(fotosDo(anuncioId)).isZero();
        assertThat(anuncios.findById(anuncioId).orElseThrow().getFotoUrl()).isNull();
    }

    @Test
    void uc06_11_trocarAFotoMudaAFotoUrlEMantemUmaUnicaLinha() throws Exception {
        UUID anuncioId = novoAnuncio();
        String primeira = salvarFoto(anuncioId, Imagens.png(1024));

        String segunda = salvarFoto(anuncioId, Imagens.jpeg(2048));

        assertThat(segunda).isNotEqualTo(primeira);
        assertThat(fotosDo(anuncioId)).isEqualTo(1);
        mvc.perform(get(segunda)).andExpect(status().isOk()).andExpect(header().string("Content-Type", "image/jpeg"));
        mvc.perform(get(primeira)).andExpect(status().isNotFound());
    }

    @Test
    void removerApagaALinhaELimpaAFotoUrl() throws Exception {
        UUID anuncioId = novoAnuncio();
        String fotoUrl = salvarFoto(anuncioId, Imagens.png(1024));

        transacao.executeWithoutResult(status -> fotoService.removerDe(anuncios.findById(anuncioId).orElseThrow()));

        assertThat(fotosDo(anuncioId)).isZero();
        assertThat(anuncios.findById(anuncioId).orElseThrow().getFotoUrl()).isNull();
        mvc.perform(get(fotoUrl)).andExpect(status().isNotFound());
    }

    @Test
    void fotoInexistenteOuComIdInvalidoRetorna404() throws Exception {
        mvc.perform(get("/fotos/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
        mvc.perform(get("/fotos/nao-e-uuid")).andExpect(status().isNotFound());
    }

    private UUID novoAnuncio() {
        try {
            return cenario.doacaoDeFrutas(cenario.anunciante()).getId();
        } catch (Exception falha) {
            throw new IllegalStateException(falha);
        }
    }

    private String salvarFoto(UUID anuncioId, MultipartFile arquivo) {
        return transacao.execute(status -> {
            Anuncio anuncio = anuncios.findById(anuncioId).orElseThrow();
            fotoService.salvarPara(anuncio, arquivo);
            return anuncio.getFotoUrl();
        });
    }

    private int fotosDo(UUID anuncioId) {
        return jdbc.queryForObject("select count(*) from foto where anuncio_id = ?", Integer.class, anuncioId);
    }
}
