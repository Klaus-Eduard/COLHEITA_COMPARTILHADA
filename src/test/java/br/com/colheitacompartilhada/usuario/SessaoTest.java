package br.com.colheitacompartilhada.usuario;

import static br.com.colheitacompartilhada.usuario.ContaDeTeste.SENHA;
import static br.com.colheitacompartilhada.usuario.ContaDeTeste.cadastrar;
import static br.com.colheitacompartilhada.usuario.ContaDeTeste.csrf;
import static br.com.colheitacompartilhada.usuario.ContaDeTeste.emailUnico;
import static br.com.colheitacompartilhada.usuario.ContaDeTeste.entrar;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import br.com.colheitacompartilhada.IntegracaoTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

/** UC02 Autenticar e sair — RF01, RNF01 [T01]; RN13. */
class SessaoTest extends IntegracaoTest {

    @Autowired
    UsuarioRepository usuarios;

    @Test
    void uc02_1_credenciaisValidasAbremSessaoEMeResponde200() throws Exception {
        String email = cadastrar(mvc);
        MockHttpSession sessao = new MockHttpSession();

        entrar(mvc, sessao, email, SENHA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.papel").value("USUARIO"))
                .andExpect(jsonPath("$.podePublicar").value(false))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());

        mvc.perform(get("/me").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.tipo").value("MORADOR"));
    }

    @Test
    void uc02_1_meSemSessaoRetorna401EmJson() throws Exception {
        mvc.perform(get("/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void uc02_2_senhaErradaEEmailInexistenteRecebemAMesmaResposta() throws Exception {
        String email = cadastrar(mvc);

        String senhaErrada = entrar(mvc, new MockHttpSession(), email, "senha-errada")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"))
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos."))
                .andReturn().getResponse().getContentAsString();
        String emailInexistente = entrar(mvc, new MockHttpSession(), emailUnico(), SENHA)
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(emailInexistente).isEqualTo(senhaErrada);
    }

    @Test
    void uc02_3_usuarioBloqueadoComSenhaCorretaRecebe401() throws Exception {
        String email = cadastrar(mvc);
        bloquear(email);

        entrar(mvc, new MockHttpSession(), email, SENHA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"));
    }

    @Test
    void uc02_4_sextaTentativaFalhaRetorna429EVoltaAAceitarApos15Minutos() throws Exception {
        String email = cadastrar(mvc);
        for (int tentativa = 1; tentativa <= 5; tentativa++) {
            entrar(mvc, new MockHttpSession(), email, "senha-errada").andExpect(status().isUnauthorized());
        }

        entrar(mvc, new MockHttpSession(), email, "senha-errada")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigo").value("MUITAS_TENTATIVAS"));
        entrar(mvc, new MockHttpSession(), email, SENHA).andExpect(status().isTooManyRequests());

        relogio.avancar(Duration.ofMinutes(15));

        entrar(mvc, new MockHttpSession(), email, SENHA).andExpect(status().isOk());
    }

    @Test
    void uc02_5_depoisDeSairMeRetorna401() throws Exception {
        MockHttpSession sessao = entrar(mvc, cadastrar(mvc));

        mvc.perform(delete("/sessao").with(csrf()).session(sessao))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("JSESSIONID", 0));

        assertThat(sessao.isInvalid()).isTrue();
        mvc.perform(get("/me").session(sessao)).andExpect(status().isUnauthorized());
    }

    @Test
    void uc02_5_sairSemSessaoRetorna401() throws Exception {
        mvc.perform(delete("/sessao").with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test
    void uc02_6_postComCookieMasSemCabecalhoCsrfRetorna403() throws Exception {
        mvc.perform(post("/sessao").cookie(new Cookie("XSRF-TOKEN", "token-de-teste"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@exemplo.com\",\"senha\":\"segredo123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void uc02_6_postSemCabecalhoCsrfRetorna403EmJson() throws Exception {
        mvc.perform(post("/sessao").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@exemplo.com\",\"senha\":\"segredo123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    void uc02_6_tokenDeGetCsrfEnviadoNoCabecalhoComOCookieEAceito() throws Exception {
        MvcResult csrf = mvc.perform(get("/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false))
                .andReturn();
        String token = JsonPath.read(csrf.getResponse().getContentAsString(), "$.token");
        Cookie cookieCsrf = csrf.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookieCsrf.getValue()).isEqualTo(token);

        mvc.perform(post("/usuarios").cookie(cookieCsrf).header("X-XSRF-TOKEN", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ContaDeTeste.cadastroJson("Ana Souza", emailUnico(), SENHA)))
                .andExpect(status().isCreated());
    }

    @Test
    void uc02_7_idDaSessaoMudaAposOLogin() throws Exception {
        String email = cadastrar(mvc);
        MockHttpSession sessao = new MockHttpSession();
        String idAntes = sessao.getId();

        entrar(mvc, sessao, email, SENHA).andExpect(status().isOk());

        assertThat(sessao.getId()).isNotEqualTo(idAntes);
    }

    @Test
    void rn13_usuarioBloqueadoComSessaoAbertaRecebe401EASessaoEEncerrada() throws Exception {
        String email = cadastrar(mvc);
        MockHttpSession sessao = entrar(mvc, email);
        bloquear(email);

        mvc.perform(get("/me").session(sessao))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));

        assertThat(sessao.isInvalid()).isTrue();
    }

    @Test
    void rotaDeAdminRetorna401ParaVisitanteE403ParaUsuarioComum() throws Exception {
        mvc.perform(get("/admin/denuncias")).andExpect(status().isUnauthorized());

        MockHttpSession sessao = entrar(mvc, cadastrar(mvc));
        mvc.perform(get("/admin/denuncias").session(sessao))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    private void bloquear(String email) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow();
        usuario.bloquear();
        usuarios.save(usuario);
    }
}
