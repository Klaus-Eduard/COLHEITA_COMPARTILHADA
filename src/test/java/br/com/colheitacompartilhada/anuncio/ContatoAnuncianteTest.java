package br.com.colheitacompartilhada.anuncio;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import br.com.colheitacompartilhada.catalogo.CatalogoService;
import br.com.colheitacompartilhada.config.ClockConfig;
import br.com.colheitacompartilhada.config.ErroJsonWriter;
import br.com.colheitacompartilhada.config.SecurityConfig;
import br.com.colheitacompartilhada.usuario.Usuario;
import br.com.colheitacompartilhada.usuario.UsuarioAutenticado;
import br.com.colheitacompartilhada.usuario.UsuarioRepository;
import br.com.colheitacompartilhada.usuario.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

/** UC09 — endpoint de contato validado sem banco externo ou Docker. */
@WebMvcTest(AnuncioController.class)
@Import({ SecurityConfig.class, ErroJsonWriter.class,
        AnuncioService.class, ClockConfig.class })
class ContatoAnuncianteTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    Clock clock;

    @MockBean
    AnuncioRepository anuncios;

    @MockBean
    FotoService fotoService;

    @MockBean
    UsuarioService usuarioService;

    @MockBean
    CatalogoService catalogoService;

    @MockBean
    UsuarioRepository usuarios;

    private Usuario anunciante;
    private Anuncio anuncio;
    private UUID anuncioId;
    private Instant agora;
    private LocalDate hoje;

    @BeforeEach
    void prepararAnuncio() {
        agora = clock.instant();
        hoje = LocalDate.now(clock.withZone(ZoneId.of("America/Sao_Paulo")));
        anunciante = Fixtures.anunciante();
        Fixtures.Dados dados = Fixtures.doacaoDeFrutas();
        dados.disponivelAte = hoje.plusDays(2);
        anuncio = Anuncio.publicar(anunciante, dados.montar(), agora, hoje);
        anuncioId = UUID.randomUUID();
        given(anuncios.findComAssociacoesById(anuncioId)).willReturn(Optional.of(anuncio));
        given(usuarios.existsByIdAndAtivoTrue(anunciante.getId())).willReturn(true);
    }

    @Test
    void visitanteRecebe401ESemNumero() throws Exception {
        mvc.perform(get("/anuncios/" + anuncioId + "/contato"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void usuarioAutenticadoRecebeNumeroFormatadoELinkWaMe() throws Exception {
        mvc.perform(get("/anuncios/" + anuncioId + "/contato").with(autenticacaoDoAnunciante()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsapp").value("5547999998888"))
                .andExpect(jsonPath("$.whatsappFormatado").value("(47) 99999-8888"))
                .andExpect(jsonPath("$.link").value(org.hamcrest.Matchers.startsWith("https://wa.me/5547999998888?text=")));
    }

    @Test
    void aceiteRevogadoRetorna404MesmoComSessaoValida() throws Exception {
        anunciante.atualizarPerfil(anunciante.getNome(), anunciante.getTipo(), anunciante.getWhatsapp(), false, agora);

        mvc.perform(get("/anuncios/" + anuncioId + "/contato").with(autenticacaoDoAnunciante()))
                .andExpect(status().isNotFound());
    }

    @Test
    void anuncioQueDeixouDeEstarVisivelRetorna404() throws Exception {
        ReflectionTestUtils.setField(anuncio, "expiraEm", agora.minusSeconds(1));

        mvc.perform(get("/anuncios/" + anuncioId + "/contato").with(autenticacaoDoAnunciante()))
                .andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor autenticacaoDoAnunciante() {
        UsuarioAutenticado principal = new UsuarioAutenticado(anunciante);
        var autenticacao = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return authentication(autenticacao);
    }
}
