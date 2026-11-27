package br.com.colheitacompartilhada.comum;

import static br.com.colheitacompartilhada.usuario.ContaDeTeste.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import br.com.colheitacompartilhada.IntegracaoTest;
import br.com.colheitacompartilhada.anuncio.Anuncio;
import br.com.colheitacompartilhada.anuncio.CenarioAnuncios;
import br.com.colheitacompartilhada.usuario.ContaDeTeste;
import br.com.colheitacompartilhada.usuario.Usuario;
import br.com.colheitacompartilhada.usuario.UsuarioRepository;
import br.com.colheitacompartilhada.usuario.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Matriz de autorização (04_API §4) [T02, T07, T08]: cada linha da tabela é executada para os quatro tipos de
 * acesso. Todos os usuários logados têm WhatsApp autorizado, para que a RN02 não interfira em POST /anuncios.
 */
class MatrizAutorizacaoTest extends IntegracaoTest {

    enum Ator { VISITANTE, USUARIO, DONO, ADMIN }

    /** O que cada linha espera de cada ator, na ordem: visitante, usuário, dono do anúncio, admin. */
    record Caso(String descricao, Function<Contexto, MockHttpServletRequestBuilder> pedido, int visitante,
            int usuario, int dono, int admin) {

        int esperado(Ator ator) {
            return switch (ator) {
                case VISITANTE -> visitante;
                case USUARIO -> usuario;
                case DONO -> dono;
                case ADMIN -> admin;
            };
        }

        @Override
        public String toString() {
            return descricao;
        }
    }

    record Contexto(UUID anuncioId, UUID denunciaId, Long bairroId, String dadosDoAnuncio) {
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    CenarioAnuncios cenario;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    UsuarioService usuarioService;

    private final Map<Ator, MockHttpSession> sessoes = new EnumMap<>(Ator.class);
    private Contexto contexto;

    @BeforeEach
    void montarCenario() throws Exception {
        cenario.limparAnuncios();
        String emailDoDono = ContaDeTeste.cadastrar(mvc);
        sessoes.put(Ator.DONO, logadoComContato(emailDoDono));
        sessoes.put(Ator.USUARIO, logadoComContato(ContaDeTeste.cadastrar(mvc)));
        sessoes.put(Ator.ADMIN, logadoComContato(ContaDeTeste.criarAdmin(usuarioService)));
        Usuario dono = usuarios.findByEmailIgnoreCase(emailDoDono).orElseThrow();
        Anuncio anuncio = cenario.doacaoDeFrutas(dono);
        MockHttpSession autor = logadoComContato(ContaDeTeste.cadastrar(mvc));
        String resposta = mvc.perform(post("/anuncios/" + anuncio.getId() + "/denuncias").with(csrf()).session(autor)
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Motivo com tamanho suficiente.\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        contexto = new Contexto(anuncio.getId(), UUID.fromString(JsonPath.read(resposta, "$.id")),
                cenario.bairro("Centro").getId(), dadosDoAnuncio(cenario.bairro("Centro").getId()));
    }

    // Deixa o banco sem anúncios e denúncias: o ColheitaApplicationTest espera a fila de abertas vazia.
    @AfterEach
    void limparDepois() {
        cenario.limparAnuncios();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("linhasDaMatriz")
    void cadaLinhaDaMatrizRespondeConformeOPapelDeQuemPede(Caso caso) throws Exception {
        for (Ator ator : Ator.values()) {
            MockHttpServletRequestBuilder pedido = caso.pedido().apply(contexto).with(csrf());
            if (ator != Ator.VISITANTE) {
                pedido.session(sessoes.get(ator));
            }
            mvc.perform(pedido).andExpect(
                    status().is(caso.esperado(ator)));
        }
    }

    static Stream<Caso> linhasDaMatriz() {
        return Stream.of(
                // Públicas
                new Caso("GET /anuncios", c -> get("/anuncios"), 200, 200, 200, 200),
                new Caso("GET /anuncios/{id}", c -> get("/anuncios/" + c.anuncioId()), 200, 200, 200, 200),
                new Caso("GET /categorias", c -> get("/categorias"), 200, 200, 200, 200),
                new Caso("GET /bairros", c -> get("/bairros"), 200, 200, 200, 200),
                new Caso("GET /csrf", c -> get("/csrf"), 200, 200, 200, 200),
                new Caso("GET /fotos/{id}", c -> get("/fotos/" + UUID.randomUUID()), 404, 404, 404, 404),
                new Caso("POST /usuarios", c -> post("/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(ContaDeTeste.cadastroJson("Pessoa", ContaDeTeste.emailUnico(), ContaDeTeste.SENHA)),
                        201, 201, 201, 201),
                // Exigem login
                new Caso("GET /me", c -> get("/me"), 401, 200, 200, 200),
                new Caso("PATCH /me", c -> patch("/me").contentType(MediaType.APPLICATION_JSON)
                        .content(ContaDeTeste.perfilJson("(47) 99999-8888", true)), 401, 200, 200, 200),
                new Caso("GET /me/anuncios", c -> get("/me/anuncios"), 401, 200, 200, 200),
                new Caso("DELETE /sessao", c -> delete("/sessao"), 401, 204, 204, 204),
                // Dono do anúncio
                new Caso("GET /me/anuncios/{id}", c -> get("/me/anuncios/" + c.anuncioId()), 401, 403, 200, 403),
                new Caso("PATCH /anuncios/{id}", c -> multipart(HttpMethod.PATCH, "/anuncios/" + c.anuncioId())
                        .file(new MockMultipartFile("dados", "", MediaType.APPLICATION_JSON_VALUE,
                                c.dadosDoAnuncio().getBytes(java.nio.charset.StandardCharsets.UTF_8))),
                        401, 403, 200, 403),
                new Caso("POST /anuncios/{id}/encerramento", c -> post("/anuncios/" + c.anuncioId() + "/encerramento")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"resultado\":\"DOADO\"}"),
                        401, 403, 200, 403),
                // Qualquer usuário logado (RN02: precisa de WhatsApp autorizado, o que o cenário garante)
                new Caso("POST /anuncios", c -> multipart("/anuncios")
                        .file(new MockMultipartFile("dados", "", MediaType.APPLICATION_JSON_VALUE,
                                c.dadosDoAnuncio().getBytes(java.nio.charset.StandardCharsets.UTF_8))),
                        401, 201, 201, 201),
                new Caso("GET /anuncios/{id}/contato", c -> get("/anuncios/" + c.anuncioId() + "/contato"),
                        401, 200, 200, 200),
                new Caso("POST /anuncios/{id}/denuncias", c -> post("/anuncios/" + c.anuncioId() + "/denuncias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Anúncio fora do objetivo do site.\"}"), 401, 201, 400, 201),
                // Somente ADMIN
                new Caso("GET /admin/denuncias", c -> get("/admin/denuncias"), 401, 403, 403, 200),
                new Caso("GET /admin/denuncias/{id}", c -> get("/admin/denuncias/" + c.denunciaId()),
                        401, 403, 403, 200),
                new Caso("PATCH /admin/denuncias/{id}", c -> patch("/admin/denuncias/" + c.denunciaId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"procedente\":false,\"decisao\":\"Sem problema.\"}"), 401, 403, 403, 200));
    }

    private MockHttpSession logadoComContato(String email) throws Exception {
        MockHttpSession sessao = ContaDeTeste.entrar(mvc, email);
        ContaDeTeste.autorizarContato(mvc, sessao);
        return sessao;
    }

    private String dadosDoAnuncio(Long bairroId) throws Exception {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("titulo", "Laranjas do quintal");
        dados.put("descricao", "Laranjas colhidas hoje, sem agrotóxico.");
        dados.put("categoriaId", CenarioAnuncios.FRUTAS);
        dados.put("bairroId", bairroId);
        dados.put("quantidade", 5);
        dados.put("unidade", "KG");
        dados.put("modalidade", "DOACAO");
        dados.put("conservacao", "Manter em local fresco");
        dados.put("disponivelAte", cenario.hoje().plusDays(2).toString());
        dados.put("excedenteDeclarado", true);
        return JSON.writeValueAsString(dados);
    }
}
