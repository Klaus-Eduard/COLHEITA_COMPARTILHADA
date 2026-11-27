package br.com.colheitacompartilhada.denuncia;

import static br.com.colheitacompartilhada.usuario.ContaDeTeste.csrf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import br.com.colheitacompartilhada.IntegracaoTest;
import br.com.colheitacompartilhada.anuncio.Anuncio;
import br.com.colheitacompartilhada.anuncio.AnuncioRepository;
import br.com.colheitacompartilhada.anuncio.CenarioAnuncios;
import br.com.colheitacompartilhada.anuncio.Resultado;
import br.com.colheitacompartilhada.anuncio.StatusAnuncio;
import br.com.colheitacompartilhada.denuncia.dto.ResolucaoRequest;
import br.com.colheitacompartilhada.usuario.ContaDeTeste;
import br.com.colheitacompartilhada.usuario.Usuario;
import br.com.colheitacompartilhada.usuario.UsuarioRepository;
import br.com.colheitacompartilhada.usuario.UsuarioService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

/** UC11 Moderar denúncias e UC12 Bloquear usuário — RF09, RN10, RN11, RN13 [T08]. */
class ModeracaoTest extends IntegracaoTest {

    private static final String MOTIVO = "Anúncio de produto industrializado, fora do objetivo.";

    @Autowired
    CenarioAnuncios cenario;

    @Autowired
    AnuncioRepository anuncios;

    @Autowired
    DenunciaRepository denuncias;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    UsuarioService usuarioService;

    @Autowired
    DenunciaService denunciaService;

    @Autowired
    TransactionTemplate transacao;

    private Usuario dono;
    private Anuncio anuncio;
    private MockHttpSession sessaoDoDono;
    private MockHttpSession sessaoDoAutor;
    private MockHttpSession sessaoDoAdmin;
    private UUID idDoAdmin;

    @BeforeEach
    void preparar() throws Exception {
        cenario.limparAnuncios();
        String emailDoDono = ContaDeTeste.cadastrar(mvc);
        sessaoDoDono = ContaDeTeste.entrar(mvc, emailDoDono);
        ContaDeTeste.autorizarContato(mvc, sessaoDoDono);
        dono = usuarios.findByEmailIgnoreCase(emailDoDono).orElseThrow();
        anuncio = cenario.doacaoDeFrutas(dono);
        sessaoDoAutor = ContaDeTeste.entrar(mvc, ContaDeTeste.cadastrar(mvc));
        String emailDoAdmin = ContaDeTeste.criarAdmin(usuarioService);
        sessaoDoAdmin = ContaDeTeste.entrar(mvc, emailDoAdmin);
        idDoAdmin = usuarios.findByEmailIgnoreCase(emailDoAdmin).orElseThrow().getId();
    }

    // ---------- UC11 ----------

    // Deixa o banco sem anúncios e denúncias: o ColheitaApplicationTest espera a fila de abertas vazia.
    @AfterEach
    void limparDepois() {
        cenario.limparAnuncios();
    }

    @Test
    void uc11_1_usuarioComumRecebe403EVisitanteRecebe401NaFila() throws Exception {
        mvc.perform(get("/admin/denuncias").session(sessaoDoAutor)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/denuncias")).andExpect(status().isUnauthorized());
    }

    @Test
    void uc11_filaMostraAbertasDaMaisAntigaParaAMaisNovaEFiltraPorStatus() throws Exception {
        String primeira = idDe(denunciar(sessaoDoAutor, anuncio.getId(), MOTIVO).andExpect(status().isCreated()));
        MockHttpSession outroAutor = ContaDeTeste.entrar(mvc, ContaDeTeste.cadastrar(mvc));
        String segunda = idDe(denunciar(outroAutor, anuncio.getId(), MOTIVO).andExpect(status().isCreated()));

        mvc.perform(get("/admin/denuncias").session(sessaoDoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").value(hasSize(2)))
                .andExpect(jsonPath("$.conteudo[0].id").value(primeira))
                .andExpect(jsonPath("$.conteudo[1].id").value(segunda))
                .andExpect(jsonPath("$.conteudo[0].anuncio.titulo").value("Laranjas do quintal"))
                .andExpect(jsonPath("$.conteudo[0].anunciante.papel").value("USUARIO"))
                .andExpect(jsonPath("$.conteudo[0].anunciante.email").doesNotExist());

        resolver(primeira, "{\"procedente\":false,\"decisao\":\"Sem problema.\",\"bloquearAnunciante\":false}")
                .andExpect(status().isOk());
        mvc.perform(get("/admin/denuncias").session(sessaoDoAdmin))
                .andExpect(jsonPath("$.conteudo").value(hasSize(1)));
        mvc.perform(get("/admin/denuncias").param("status", "RESOLVIDA").session(sessaoDoAdmin))
                .andExpect(jsonPath("$.conteudo").value(hasSize(1)))
                .andExpect(jsonPath("$.conteudo[0].id").value(primeira));
    }

    @Test
    void uc11_2_improcedenteResolveADenunciaEOAnuncioNaoMuda() throws Exception {
        String id = denunciaAberta();

        resolver(id, "{\"procedente\":false,\"decisao\":\"Sem problema.\",\"bloquearAnunciante\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVIDA"))
                .andExpect(jsonPath("$.decisao").value("Sem problema."))
                .andExpect(jsonPath("$.moderador.nome").value("Moderadora"))
                .andExpect(jsonPath("$.resolvidaEm").exists());

        assertThat(anuncioAtual().getStatus()).isEqualTo(StatusAnuncio.ATIVO);
    }

    @Test
    void uc11_3_procedenteEmAnuncioAtivoRemoveOAnuncioEResolveADenuncia() throws Exception {
        String id = denunciaAberta();

        resolver(id, "{\"procedente\":true,\"decisao\":\"Produto industrializado. Removido.\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVIDA"))
                .andExpect(jsonPath("$.anuncioDetalhe.status").value("REMOVIDO"));

        assertThat(anuncioAtual().getStatus()).isEqualTo(StatusAnuncio.REMOVIDO);
        mvc.perform(get("/anuncios/" + anuncio.getId())).andExpect(status().isNotFound());
    }

    @Test
    void uc11_4_procedenteEmAnuncioEncerradoRemoveELimpaResultadoEDataDeEncerramento() throws Exception {
        String id = denunciaAberta();
        Anuncio encerrado = anuncioAtual();
        encerrado.encerrar(Resultado.DOADO, cenario.agora());
        cenario.salvar(encerrado);

        resolver(id, "{\"procedente\":true,\"decisao\":\"Removido após o encerramento.\"}")
                .andExpect(status().isOk());

        Anuncio depois = anuncioAtual();
        assertThat(depois.getStatus()).isEqualTo(StatusAnuncio.REMOVIDO);
        assertThat(depois.getResultado()).isNull();
        assertThat(depois.getEncerradoEm()).isNull();
    }

    @Test
    void uc11_5_resolverDenunciaJaResolvidaRecebe409EstadoInvalido() throws Exception {
        String id = denunciaAberta();
        resolver(id, "{\"procedente\":false,\"decisao\":\"Sem problema.\"}").andExpect(status().isOk());

        resolver(id, "{\"procedente\":true,\"decisao\":\"Mudei de ideia.\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ESTADO_INVALIDO"));
        assertThat(anuncioAtual().getStatus()).isEqualTo(StatusAnuncio.ATIVO);
    }

    @Test
    void uc11_6_seAResolucaoFalharDepoisDeRemoverNadaEGravado() throws Exception {
        UUID id = UUID.fromString(denunciaAberta());
        var pedido = new ResolucaoRequest(true, "Removido.", true);

        assertThatThrownBy(() -> transacao.executeWithoutResult(status -> {
            denunciaService.resolver(id, idDoAdmin, pedido);
            throw new IllegalStateException("falha simulada depois de remover e bloquear");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(anuncioAtual().getStatus()).isEqualTo(StatusAnuncio.ATIVO);
        assertThat(usuarios.findById(dono.getId()).orElseThrow().isAtivo()).isTrue();
        assertThat(denuncias.findById(id).orElseThrow().getStatus()).isEqualTo(StatusDenuncia.ABERTA);
    }

    @Test
    void uc11_detalheMostraOAnuncioCompletoMesmoRemovidoEOEmailDoAnunciante() throws Exception {
        String id = denunciaAberta();
        resolver(id, "{\"procedente\":true,\"decisao\":\"Removido.\"}").andExpect(status().isOk());

        mvc.perform(get("/admin/denuncias/" + id).session(sessaoDoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anuncioDetalhe.titulo").value("Laranjas do quintal"))
                .andExpect(jsonPath("$.anuncioDetalhe.status").value("REMOVIDO"))
                .andExpect(jsonPath("$.anunciante.email").value(dono.getEmail()))
                .andExpect(jsonPath("$.decisao").value("Removido."));
        mvc.perform(get("/admin/denuncias/" + UUID.randomUUID()).session(sessaoDoAdmin))
                .andExpect(status().isNotFound());
    }

    @Test
    void uc11_resolverSemDecisaoOuSemProcedenteRecebe400() throws Exception {
        String id = denunciaAberta();

        resolver(id, "{\"procedente\":false,\"decisao\":\"   \"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.decisao").exists());
        resolver(id, "{\"decisao\":\"Sem problema.\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.procedente").exists());
        assertThat(denuncias.findById(UUID.fromString(id)).orElseThrow().getStatus())
                .isEqualTo(StatusDenuncia.ABERTA);
    }

    // ---------- UC12 ----------

    @Test
    void uc12_1_procedenteComBloqueioDesativaOAnuncianteEEsconderSeusAnunciosSemMudarOStatus() throws Exception {
        Anuncio outro = cenario.doacaoDeFrutas(dono);
        String id = denunciaAberta();

        resolver(id, "{\"procedente\":true,\"decisao\":\"Reincidente.\",\"bloquearAnunciante\":true}")
                .andExpect(status().isOk());

        assertThat(usuarios.findById(dono.getId()).orElseThrow().isAtivo()).isFalse();
        mvc.perform(get("/anuncios")).andExpect(jsonPath("$.totalElementos").value(0));
        assertThat(anuncios.findById(outro.getId()).orElseThrow().getStatus()).isEqualTo(StatusAnuncio.ATIVO);
    }

    @Test
    void uc12_2_sessaoAbertaDoUsuarioBloqueadoRecebe401NaProximaRequisicao() throws Exception {
        String id = denunciaAberta();
        mvc.perform(get("/me").session(sessaoDoDono)).andExpect(status().isOk());

        resolver(id, "{\"procedente\":true,\"decisao\":\"Reincidente.\",\"bloquearAnunciante\":true}")
                .andExpect(status().isOk());

        mvc.perform(get("/me").session(sessaoDoDono)).andExpect(status().isUnauthorized());
    }

    @Test
    void uc12_3_bloquearSemSerProcedenteRecebe400ENadaMuda() throws Exception {
        String id = denunciaAberta();

        resolver(id, "{\"procedente\":false,\"decisao\":\"Sem problema.\",\"bloquearAnunciante\":true}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("BLOQUEIO_NAO_PERMITIDO"));

        assertThat(usuarios.findById(dono.getId()).orElseThrow().isAtivo()).isTrue();
        assertThat(denuncias.findById(UUID.fromString(id)).orElseThrow().getStatus())
                .isEqualTo(StatusDenuncia.ABERTA);
    }

    @Test
    void uc12_3_bloquearUmAdminRecebe400BloqueioNaoPermitido() throws Exception {
        String emailDoOutroAdmin = ContaDeTeste.criarAdmin(usuarioService);
        MockHttpSession sessaoDoOutroAdmin = ContaDeTeste.entrar(mvc, emailDoOutroAdmin);
        ContaDeTeste.autorizarContato(mvc, sessaoDoOutroAdmin);
        Anuncio doAdmin = cenario.doacaoDeFrutas(usuarios.findByEmailIgnoreCase(emailDoOutroAdmin).orElseThrow());
        String id = idDe(denunciar(sessaoDoAutor, doAdmin.getId(), MOTIVO).andExpect(status().isCreated()));

        resolver(id, "{\"procedente\":true,\"decisao\":\"Decisão.\",\"bloquearAnunciante\":true}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("BLOQUEIO_NAO_PERMITIDO"));

        assertThat(anuncios.findById(doAdmin.getId()).orElseThrow().getStatus()).isEqualTo(StatusAnuncio.ATIVO);
        mvc.perform(get("/me").session(sessaoDoOutroAdmin)).andExpect(status().isOk());
    }

    @Test
    void uc12_4_bloquearQuemJaEstaBloqueadoRecebe200() throws Exception {
        String id = denunciaAberta();
        cenario.bloquear(dono);

        resolver(id, "{\"procedente\":true,\"decisao\":\"Reincidente.\",\"bloquearAnunciante\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anunciante.ativo").value(false));
    }

    // ---------- apoio ----------

    private String denunciaAberta() throws Exception {
        return idDe(denunciar(sessaoDoAutor, anuncio.getId(), MOTIVO).andExpect(status().isCreated()));
    }

    private ResultActions denunciar(MockHttpSession sessao, UUID anuncioId, String motivo) throws Exception {
        return mvc.perform(post("/anuncios/" + anuncioId + "/denuncias").with(csrf()).session(sessao)
                .contentType(MediaType.APPLICATION_JSON).content(corpoDenuncia(motivo)));
    }

    private ResultActions resolver(String denunciaId, String json) throws Exception {
        return mvc.perform(patch("/admin/denuncias/" + denunciaId).with(csrf()).session(sessaoDoAdmin)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String corpoDenuncia(String motivo) {
        return "{\"motivo\":\"" + motivo + "\"}";
    }

    private static String idDe(ResultActions resposta) throws Exception {
        return JsonPath.read(resposta.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private Anuncio anuncioAtual() {
        return anuncios.findById(anuncio.getId()).orElseThrow();
    }

}
