package br.com.colheitacompartilhada.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;

import br.com.colheitacompartilhada.IntegracaoTest;
import br.com.colheitacompartilhada.anuncio.AnuncioRepository;
import br.com.colheitacompartilhada.anuncio.AnuncioService;
import br.com.colheitacompartilhada.anuncio.CenarioAnuncios;
import br.com.colheitacompartilhada.anuncio.StatusAnuncio;
import br.com.colheitacompartilhada.catalogo.CatalogoService;
import br.com.colheitacompartilhada.usuario.UsuarioRepository;
import br.com.colheitacompartilhada.usuario.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/** Dados de demonstração (issue #19, 05_ARQUITETURA §7): passam pelas regras reais e podem rodar de novo. */
@ActiveProfiles("demo")
@TestPropertySource(properties = "app.demo.senha=senha-demo-1234")
class DemoDataLoaderTest extends IntegracaoTest {

    @Autowired
    CenarioAnuncios cenario;

    @Autowired
    UsuarioService usuarioService;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    AnuncioService anuncioService;

    @Autowired
    AnuncioRepository anuncios;

    @Autowired
    CatalogoService catalogo;

    @Autowired
    Clock clock;

    @BeforeEach
    void limpar() {
        cenario.limparAnuncios();
    }

    // Estes testes deixam anúncios vencidos e ainda ATIVOS; sem limpar, o ColheitaApplicationTest (que espera
    // zero anúncios a expirar) falha dependendo da ordem em que as classes rodam.
    @AfterEach
    void limparDepois() {
        cenario.limparAnuncios();
    }

    @Test
    void criaSeisAnunciosVisiveisNaBuscaPublica() throws Exception {
        int criados = carregador("senha-demo-1234").carregar();

        assertThat(criados).isEqualTo(6);
        mvc.perform(get("/anuncios")).andExpect(status().isOk())
                .andExpect(content().string(containsString("\"totalElementos\":6")));
    }

    @Test
    void osUsuariosFicticiosEntramComADemoPasswordEPodemPublicar() throws Exception {
        carregador("senha-demo-1234").carregar();

        assertThat(usuarios.findByEmailIgnoreCase(DemoDataLoader.EMAIL_MARIA)).get()
                .satisfies(u -> assertThat(u.podePublicar()).isTrue());
        assertThat(usuarios.findByEmailIgnoreCase(DemoDataLoader.EMAIL_JOAO)).get()
                .satisfies(u -> assertThat(u.podePublicar()).isTrue());
        br.com.colheitacompartilhada.usuario.ContaDeTeste.entrar(mvc, new org.springframework.mock.web.MockHttpSession(),
                DemoDataLoader.EMAIL_MARIA, "senha-demo-1234").andExpect(status().isOk());
    }

    @Test
    void osAnunciosDeOvosEMelTemOrigemEInspecaoFicticiasEFotoNoBanco() {
        carregador("senha-demo-1234").carregar();

        var animais = anuncios.findAll().stream().filter(a -> a.getInspecao() != null).toList();
        assertThat(animais).hasSize(2).allSatisfy(a -> {
            assertThat(a.getInspecao()).contains("fictício");
            assertThat(a.getValidadeAlimento()).isNotNull();
        });
        assertThat(anuncios.findAll()).filteredOn(a -> a.getFotoUrl() != null).hasSize(4)
                .allSatisfy(a -> assertThat(a.getFotoUrl()).startsWith("/fotos/"));
    }

    @Test
    void naoCarregaDeNovoQuandoJaExisteAnuncioAtivoNoPrazo() {
        DemoDataLoader carregador = carregador("senha-demo-1234");
        carregador.run(new DefaultApplicationArguments());
        long antes = anuncios.count();

        carregador.run(new DefaultApplicationArguments());

        assertThat(anuncios.count()).isEqualTo(antes);
        assertThat(anuncios.findAll()).allSatisfy(a -> assertThat(a.getStatus()).isEqualTo(StatusAnuncio.ATIVO));
    }

    @Test
    void senhaForaDe8A72CaracteresNaoCarregaNadaENaoLancaErro() {
        long antes = anuncios.count();

        carregador("curta").run(new DefaultApplicationArguments());

        assertThat(anuncios.count()).isEqualTo(antes);
    }

    @Test
    void ePossivelRodarDeNovoDepoisQueOsAnunciosVencem() {
        DemoDataLoader carregador = carregador("senha-demo-1234");
        carregador.run(new DefaultApplicationArguments());
        anuncios.findAll().forEach(cenario::vencerPrazo);

        carregador.run(new DefaultApplicationArguments());

        assertThat(anuncios.findAll().stream().filter(a -> a.getExpiraEm().isAfter(clock.instant())).count())
                .isEqualTo(6);
        assertThat(usuarios.findAll().stream().filter(u -> u.getEmail().endsWith(".demo@exemplo.com")).count())
                .isEqualTo(2);
    }

    @Test
    void naoAplicaOPerfilDemoSemAtivaLoNosOutrosTestes() {
        assertThat(DemoDataLoader.class.getAnnotation(org.springframework.context.annotation.Profile.class).value())
                .containsExactly("demo");
    }

    private DemoDataLoader carregador(String senha) {
        return new DemoDataLoader(usuarioService, usuarios, anuncioService, anuncios, catalogo, clock, senha);
    }
}
