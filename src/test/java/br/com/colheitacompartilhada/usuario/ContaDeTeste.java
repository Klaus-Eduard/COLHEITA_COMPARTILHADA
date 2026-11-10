package br.com.colheitacompartilhada.usuario;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Atalhos dos testes de integração para cadastrar e entrar pela própria API. */
public final class ContaDeTeste {

    public static final String SENHA = "segredo123";

    private ContaDeTeste() {
    }

    /**
     * CSRF como o navegador faz: o mesmo token no cookie XSRF-TOKEN e no cabeçalho X-XSRF-TOKEN.
     * Não usamos o csrf() do spring-security-test porque ele substitui o repositório de tokens da aplicação.
     */
    public static RequestPostProcessor csrf() {
        return request -> {
            request.setCookies(new Cookie("XSRF-TOKEN", "token-de-teste"));
            request.addHeader("X-XSRF-TOKEN", "token-de-teste");
            return request;
        };
    }

    public static String emailUnico() {
        return "u" + UUID.randomUUID().toString().replace("-", "") + "@exemplo.com";
    }

    public static ResultActions cadastrar(MockMvc mvc, String json) throws Exception {
        return mvc.perform(post("/usuarios").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    public static String cadastrar(MockMvc mvc) throws Exception {
        String email = emailUnico();
        cadastrar(mvc, cadastroJson("Ana Souza", email, SENHA)).andExpect(status().isCreated());
        return email;
    }

    public static ResultActions entrar(MockMvc mvc, MockHttpSession sessao, String email, String senha)
            throws Exception {
        return mvc.perform(post("/sessao").with(csrf()).session(sessao)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha)));
    }

    public static MockHttpSession entrar(MockMvc mvc, String email) throws Exception {
        MockHttpSession sessao = new MockHttpSession();
        entrar(mvc, sessao, email, SENHA).andExpect(status().isOk());
        return sessao;
    }

    public static String cadastroJson(String nome, String email, String senha) {
        return "{\"nome\":\"%s\",\"email\":\"%s\",\"senha\":\"%s\",\"tipo\":\"MORADOR\"}".formatted(nome, email, senha);
    }
}
