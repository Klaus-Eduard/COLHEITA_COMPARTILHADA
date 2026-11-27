package br.com.colheitacompartilhada.denuncia;

import static br.com.colheitacompartilhada.usuario.ContaDeTeste.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import br.com.colheitacompartilhada.IntegracaoTest;
import br.com.colheitacompartilhada.anuncio.Anuncio;
import br.com.colheitacompartilhada.anuncio.AnuncioRepository;
import br.com.colheitacompartilhada.anuncio.CenarioAnuncios;
import br.com.colheitacompartilhada.usuario.ContaDeTeste;
import br.com.colheitacompartilhada.usuario.Usuario;
import br.com.colheitacompartilhada.usuario.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.jdbc.core.JdbcTemplate;

/** UC10 Denunciar anúncio — RF08, RN10 [T08]. */
class DenunciarTest extends IntegracaoTest {

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
    JdbcTemplate jdbc;

    private Usuario dono;
    private Anuncio anuncio;
    private MockHttpSession sessaoDoDono;
    private MockHttpSession sessaoDoAutor;

    @BeforeEach
    void preparar() throws Exception {
        cenario.limparAnuncios();
        String emailDoDono = ContaDeTeste.cadastrar(mvc);
        sessaoDoDono = ContaDeTeste.entrar(mvc, emailDoDono);
        ContaDeTeste.autorizarContato(mvc, sessaoDoDono);
        dono = usuarios.findByEmailIgnoreCase(emailDoDono).orElseThrow();
        anuncio = cenario.doacaoDeFrutas(dono);
        sessaoDoAutor = ContaDeTeste.entrar(mvc, ContaDeTeste.cadastrar(mvc));
    }

    // ---------- UC10 ----------

    // Estes testes deixam anúncios vencidos e ainda ATIVOS; sem limpar, o ColheitaApplicationTest (que espera
    // zero anúncios a expirar) falha dependendo da ordem em que as classes rodam.
    @AfterEach
    void limparDepois() {
        cenario.limparAnuncios();
    }

    @Test
    void uc10_1_denunciarOProprioAnuncioRecebe400DenunciaProprioAnuncio() throws Exception {
        denunciar(sessaoDoDono, anuncio.getId(), MOTIVO)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DENUNCIA_PROPRIO_ANUNCIO"));
    }

    @Test
    void uc10_2_segundaDenunciaAbertaDoMesmoAutorRecebe409EDepoisDeResolvidaUmaNovaEAceita() throws Exception {
        String primeira = idDe(denunciar(sessaoDoAutor, anuncio.getId(), MOTIVO).andExpect(status().isCreated()));

        denunciar(sessaoDoAutor, anuncio.getId(), MOTIVO)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DENUNCIA_DUPLICADA"));

        resolverPorSql(primeira);
        denunciar(sessaoDoAutor, anuncio.getId(), MOTIVO).andExpect(status().isCreated());
    }

    @Test
    void uc10_3_motivoCom9CaracteresRecebe400NoCampoMotivo() throws Exception {
        denunciar(sessaoDoAutor, anuncio.getId(), "123456789")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.motivo").exists());
        denunciar(sessaoDoAutor, anuncio.getId(), "   123456789   ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.motivo").exists());
        denunciar(sessaoDoAutor, anuncio.getId(), "1234567890").andExpect(status().isCreated());
    }

    @Test
    void uc10_4_depoisDeDenunciarOAnuncioContinuaVisivel() throws Exception {
        denunciar(sessaoDoAutor, anuncio.getId(), MOTIVO)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ABERTA"))
                .andExpect(jsonPath("$.id").exists());

        mvc.perform(get("/anuncios/" + anuncio.getId())).andExpect(status().isOk());
    }

    @Test
    void rn10_1_visitanteRecebe401EAnuncioNaoVisivelOuInexistenteRecebe404() throws Exception {
        mvc.perform(post("/anuncios/" + anuncio.getId() + "/denuncias").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(corpoDenuncia(MOTIVO)))
                .andExpect(status().isUnauthorized());

        denunciar(sessaoDoAutor, UUID.randomUUID(), MOTIVO).andExpect(status().isNotFound());
        cenario.vencerPrazo(anuncio);
        denunciar(sessaoDoAutor, anuncio.getId(), MOTIVO).andExpect(status().isNotFound());
    }

    // ---------- apoio ----------

    private ResultActions denunciar(MockHttpSession sessao, UUID anuncioId, String motivo) throws Exception {
        return mvc.perform(post("/anuncios/" + anuncioId + "/denuncias").with(csrf()).session(sessao)
                .contentType(MediaType.APPLICATION_JSON).content(corpoDenuncia(motivo)));
    }

    private static String corpoDenuncia(String motivo) {
        return "{\"motivo\":\"" + motivo + "\"}";
    }

    private static String idDe(ResultActions resposta) throws Exception {
        return JsonPath.read(resposta.andReturn().getResponse().getContentAsString(), "$.id");
    }

    // Simula o moderador já ter resolvido a denúncia; a resolução pela API é testada em ModeracaoTest.
    private void resolverPorSql(String denunciaId) {
        jdbc.update("update denuncia set status = 'RESOLVIDA', moderador_id = ?, decisao = 'Sem problema.', "
                + "resolvida_em = now() where id = ?", dono.getId(), UUID.fromString(denunciaId));
    }
}
