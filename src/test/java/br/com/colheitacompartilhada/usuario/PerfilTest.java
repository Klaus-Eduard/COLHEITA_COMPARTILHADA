package br.com.colheitacompartilhada.usuario;

import static br.com.colheitacompartilhada.usuario.ContaDeTeste.atualizarPerfil;
import static br.com.colheitacompartilhada.usuario.ContaDeTeste.cadastrar;
import static br.com.colheitacompartilhada.usuario.ContaDeTeste.csrf;
import static br.com.colheitacompartilhada.usuario.ContaDeTeste.entrar;
import static br.com.colheitacompartilhada.usuario.ContaDeTeste.perfilJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;

import br.com.colheitacompartilhada.IntegracaoTest;
import br.com.colheitacompartilhada.anuncio.Anuncio;
import br.com.colheitacompartilhada.anuncio.CenarioAnuncios;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

/** UC03 Atualizar perfil — RF02, RN02 [T07]. */
class PerfilTest extends IntegracaoTest {

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    CenarioAnuncios cenario;

    @Test
    void uc03_1_numeroComMascaraEAceiteGravaSoDigitosEADataDoAceite() throws Exception {
        String email = cadastrar(mvc);
        MockHttpSession sessao = entrar(mvc, email);
        Instant antes = relogio.instant();

        atualizarPerfil(mvc, sessao, perfilJson("(47) 99999-8888", true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsapp").value("5547999998888"))
                .andExpect(jsonPath("$.aceiteContatoEm").isNotEmpty())
                .andExpect(jsonPath("$.podePublicar").value(true))
                .andExpect(jsonPath("$.email").value(email));

        Usuario salvo = usuarios.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(salvo.getWhatsapp()).isEqualTo("5547999998888");
        assertThat(salvo.getAceiteContatoEm()).isBetween(antes.minusMillis(1), relogio.instant());
    }

    @Test
    void uc03_2_mesmoNumeroComAceiteNaoMudaADataDoAceite() throws Exception {
        String email = cadastrar(mvc);
        MockHttpSession sessao = entrar(mvc, email);
        atualizarPerfil(mvc, sessao, perfilJson("(47) 99999-8888", true)).andExpect(status().isOk());
        Instant aceiteOriginal = aceiteDe(email);

        relogio.avancar(Duration.ofDays(1));
        atualizarPerfil(mvc, sessao, perfilJson("47999998888", true)).andExpect(status().isOk());

        assertThat(aceiteDe(email)).isEqualTo(aceiteOriginal);
    }

    @Test
    void uc03_3_numeroAlteradoSemAceiteApagaOAceite() throws Exception {
        String email = cadastrar(mvc);
        MockHttpSession sessao = entrar(mvc, email);
        atualizarPerfil(mvc, sessao, perfilJson("(47) 99999-8888", true)).andExpect(status().isOk());

        atualizarPerfil(mvc, sessao, perfilJson("(47) 98888-7777", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsapp").value("5547988887777"))
                .andExpect(jsonPath("$.aceiteContatoEm").value((Object) null))
                .andExpect(jsonPath("$.podePublicar").value(false));

        assertThat(aceiteDe(email)).isNull();
    }

    @Test
    void uc03_4_aceiteSemWhatsappRetorna400NoCampoWhatsapp() throws Exception {
        MockHttpSession sessao = entrar(mvc, cadastrar(mvc));

        atualizarPerfil(mvc, sessao, perfilJson(null, true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos.whatsapp").value("Informe o WhatsApp com DDD."));
        atualizarPerfil(mvc, sessao, perfilJson("", true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.whatsapp").exists());
    }

    @Test
    void uc03_5_revogarOAceiteEscondeOAnuncioERenovarOMostraDeNovo() throws Exception {
        cenario.limparAnuncios();
        Usuario autor = cenario.anunciante();
        Anuncio anuncio = cenario.doacaoDeFrutas(autor);
        MockHttpSession sessao = entrar(mvc, autor.getEmail());

        atualizarPerfil(mvc, sessao, perfilJson("(47) 99999-8888", false)).andExpect(status().isOk());

        mvc.perform(get("/anuncios")).andExpect(jsonPath("$.totalElementos").value(0));
        mvc.perform(get("/anuncios/" + anuncio.getId())).andExpect(status().isNotFound());

        atualizarPerfil(mvc, sessao, perfilJson("(47) 99999-8888", true)).andExpect(status().isOk());

        mvc.perform(get("/anuncios")).andExpect(jsonPath("$.totalElementos").value(1));
        mvc.perform(get("/anuncios/" + anuncio.getId())).andExpect(status().isOk());
    }

    @Test
    void removerONumeroRemoveTambemOAceite() throws Exception {
        String email = cadastrar(mvc);
        MockHttpSession sessao = entrar(mvc, email);
        atualizarPerfil(mvc, sessao, perfilJson("(47) 99999-8888", true)).andExpect(status().isOk());

        atualizarPerfil(mvc, sessao, perfilJson("", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsapp").value((Object) null))
                .andExpect(jsonPath("$.aceiteContatoEm").value((Object) null));
    }

    @Test
    void numeroSemDddRetorna400() throws Exception {
        MockHttpSession sessao = entrar(mvc, cadastrar(mvc));

        atualizarPerfil(mvc, sessao, perfilJson("99999-8888", false))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.whatsapp").value("Informe o WhatsApp com DDD."));
    }

    @Test
    void nomeETipoSaoValidadosEAtualizados() throws Exception {
        MockHttpSession sessao = entrar(mvc, cadastrar(mvc));

        atualizarPerfil(mvc, sessao, "{\"nome\":\"A\",\"tipo\":null,\"whatsapp\":null,\"aceiteContato\":false}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nome").value("Informe seu nome (2 a 120 caracteres)."))
                .andExpect(jsonPath("$.campos.tipo").value("Selecione o tipo de participante."));
        atualizarPerfil(mvc, sessao,
                "{\"nome\":\" Ana Lima \",\"tipo\":\"PEQUENO_PRODUTOR\",\"whatsapp\":null,\"aceiteContato\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Lima"))
                .andExpect(jsonPath("$.tipo").value("PEQUENO_PRODUTOR"));
    }

    @Test
    void emailEPapelEnviadosNoCorpoSaoIgnorados() throws Exception {
        String email = cadastrar(mvc);
        MockHttpSession sessao = entrar(mvc, email);

        atualizarPerfil(mvc, sessao, "{\"nome\":\"Ana Souza\",\"tipo\":\"MORADOR\",\"whatsapp\":null,"
                + "\"aceiteContato\":false,\"email\":\"outro@exemplo.com\",\"papel\":\"ADMIN\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.papel").value("USUARIO"));
    }

    @Test
    void visitanteNaoAtualizaPerfil() throws Exception {
        mvc.perform(patch("/me").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(perfilJson("(47) 99999-8888", true)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    private Instant aceiteDe(String email) {
        return usuarios.findByEmailIgnoreCase(email).orElseThrow().getAceiteContatoEm();
    }
}
