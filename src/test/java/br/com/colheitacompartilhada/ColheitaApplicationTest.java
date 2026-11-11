package br.com.colheitacompartilhada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import br.com.colheitacompartilhada.anuncio.AnuncioRepository;
import br.com.colheitacompartilhada.anuncio.FotoRepository;
import br.com.colheitacompartilhada.catalogo.BairroRepository;
import br.com.colheitacompartilhada.catalogo.CategoriaRepository;
import br.com.colheitacompartilhada.denuncia.DenunciaRepository;
import br.com.colheitacompartilhada.denuncia.StatusDenuncia;
import br.com.colheitacompartilhada.usuario.UsuarioRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

class ColheitaApplicationTest extends IntegracaoTest {

    @Autowired
    Flyway flyway;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    BairroRepository bairros;

    @Autowired
    CategoriaRepository categorias;

    @Autowired
    AnuncioRepository anuncios;

    @Autowired
    FotoRepository fotos;

    @Autowired
    DenunciaRepository denuncias;

    @Test
    void aplicaAsMigracoesV1aV3() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("3");
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    void livenessRespondeUp() throws Exception {
        mvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void catalogoVemDasMigracoes() {
        assertThat(categorias.count()).isEqualTo(4);
        assertThat(bairros.count()).isEqualTo(35);
    }

    @Test
    @Transactional
    void consultasDosRepositoriosExecutamNoPostgres() {
        UUID id = UUID.randomUUID();
        var primeiraPagina = PageRequest.of(0, 20);

        assertThat(usuarios.findByEmailIgnoreCase("ninguem@exemplo.com")).isEmpty();
        assertThat(usuarios.existsByEmailIgnoreCase("ninguem@exemplo.com")).isFalse();
        assertThat(anuncios.findByUsuarioIdOrderByCriadoEmDesc(id, primeiraPagina)).isEmpty();
        assertThat(anuncios.findByIdParaAtualizar(id)).isEmpty();
        assertThat(anuncios.expirarVencidos(Instant.now())).isZero();
        assertThat(denuncias.findByStatusOrderByCriadoEmAsc(StatusDenuncia.ABERTA, primeiraPagina)).isEmpty();
        assertThat(denuncias.existsByAnuncioIdAndAutorIdAndStatus(id, id, StatusDenuncia.ABERTA)).isFalse();
        fotos.deleteByAnuncioId(id);
    }
}
