package br.com.colheitacompartilhada.anuncio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import br.com.colheitacompartilhada.IntegracaoTest;
import br.com.colheitacompartilhada.catalogo.BairroRepository;
import br.com.colheitacompartilhada.catalogo.CategoriaRepository;
import br.com.colheitacompartilhada.config.ClockConfig;
import br.com.colheitacompartilhada.usuario.ContaDeTeste;
import br.com.colheitacompartilhada.usuario.Usuario;
import br.com.colheitacompartilhada.usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Garante que os estados produzidos pela entidade são aceitos pelos CHECKs do schema (V1). */
class AnuncioPersistenciaTest extends IntegracaoTest {

    @Autowired
    AnuncioRepository anuncios;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    CategoriaRepository categorias;

    @Autowired
    BairroRepository bairros;

    @Test
    void anuncioPublicadoEncerradoERemovidoPassaPelosChecksDoBanco() throws Exception {
        Instant agora = relogio.instant();
        LocalDate hoje = LocalDate.now(relogio.withZone(ClockConfig.FUSO_NEGOCIO));
        Anuncio anuncio = anuncios.save(Anuncio.publicar(anuncianteSalvo(), vendaDeOvos(hoje), agora, hoje));

        Anuncio publicado = anuncios.findById(anuncio.getId()).orElseThrow();
        assertThat(publicado.getStatus()).isEqualTo(StatusAnuncio.ATIVO);
        assertThat(publicado.getQuantidade()).isEqualByComparingTo("2");
        assertThat(publicado.getPreco()).isEqualByComparingTo("12.00");

        publicado.encerrar(Resultado.VENDIDO, agora.plusSeconds(60));
        Anuncio encerrado = anuncios.saveAndFlush(publicado);
        assertThat(encerrado.getStatus()).isEqualTo(StatusAnuncio.ENCERRADO);

        encerrado.remover(agora.plusSeconds(120));
        Anuncio removido = anuncios.saveAndFlush(encerrado);
        assertThat(removido.getStatus()).isEqualTo(StatusAnuncio.REMOVIDO);
        assertThat(removido.getResultado()).isNull();
    }

    private Usuario anuncianteSalvo() throws Exception {
        String email = ContaDeTeste.cadastrar(mvc);
        ContaDeTeste.autorizarContato(mvc, ContaDeTeste.entrar(mvc, email));
        return usuarios.findByEmailIgnoreCase(email).orElseThrow();
    }

    private DadosAnuncio vendaDeOvos(LocalDate hoje) {
        return new DadosAnuncio(categorias.findById(3L).orElseThrow(), bairros.findAll().get(0),
                "Ovos caipiras", "Uma dúzia de ovos de galinhas soltas.", new BigDecimal("2"), Unidade.DUZIA,
                Modalidade.VENDA, new BigDecimal("12.00"), "Manter em local fresco", hoje.plusDays(10),
                "Quintal próprio", "SIM 0123 (fictício)", hoje.plusDays(2), true);
    }
}
