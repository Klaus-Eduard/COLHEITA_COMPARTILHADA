package br.com.colheitacompartilhada.anuncio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import br.com.colheitacompartilhada.IntegracaoTest;
import br.com.colheitacompartilhada.usuario.ContaDeTeste;
import br.com.colheitacompartilhada.usuario.Usuario;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

/** UC04 Buscar anúncios e UC05 Consultar detalhes — RF03, RF04, RN08 [T05]. */
class BuscaEDetalheTest extends IntegracaoTest {

    @Autowired
    CenarioAnuncios cenario;

    @BeforeEach
    void limpar() {
        cenario.limparAnuncios();
    }

    @Test
    void uc04_1_com25AnunciosAPrimeiraPaginaTraz20EASegunda5DoMaisNovoParaOMaisAntigo() throws Exception {
        Usuario autor = cenario.anunciante();
        List<String> doMaisNovoAoMaisAntigo = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            CenarioAnuncios.Dados dados = cenario.new Dados();
            dados.criadoEm = cenario.agora().minusSeconds(i);
            doMaisNovoAoMaisAntigo.add(cenario.publicar(autor, dados).getId().toString());
        }

        String pagina0 = mvc.perform(get("/anuncios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").value(hasSize(20)))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(20))
                .andExpect(jsonPath("$.totalElementos").value(25))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andReturn().getResponse().getContentAsString();
        String pagina1 = mvc.perform(get("/anuncios").param("pagina", "1"))
                .andExpect(jsonPath("$.conteudo").value(hasSize(5)))
                .andExpect(jsonPath("$.pagina").value(1))
                .andReturn().getResponse().getContentAsString();

        List<String> ids = new ArrayList<>(JsonPath.<List<String>>read(pagina0, "$.conteudo[*].id"));
        ids.addAll(JsonPath.<List<String>>read(pagina1, "$.conteudo[*].id"));
        assertThat(ids).containsExactlyElementsOf(doMaisNovoAoMaisAntigo);
    }

    @Test
    void uc04_1_tamanhoDaPaginaEFixoMesmoComParametroDeTamanho() throws Exception {
        cenario.doacaoDeFrutas(cenario.anunciante());

        mvc.perform(get("/anuncios").param("tamanho", "500").param("size", "500"))
                .andExpect(jsonPath("$.tamanho").value(20));
    }

    @Test
    void uc04_2_filtrosCumulativosRetornamSomenteAIntersecao() throws Exception {
        Usuario autor = cenario.anunciante();
        CenarioAnuncios.Dados alvo = cenario.new Dados().vendaDeOvos();
        alvo.bairro = cenario.bairro("Garcia");
        String idAlvo = cenario.publicar(autor, alvo).getId().toString();
        CenarioAnuncios.Dados outroBairro = cenario.new Dados().vendaDeOvos();
        cenario.publicar(autor, outroBairro);
        CenarioAnuncios.Dados outraCategoria = cenario.new Dados();
        outraCategoria.bairro = cenario.bairro("Garcia");
        outraCategoria.titulo = "Ovos de codorna? Não, laranjas";
        cenario.publicar(autor, outraCategoria);
        CenarioAnuncios.Dados outraModalidade = cenario.new Dados().vendaDeOvos();
        outraModalidade.bairro = cenario.bairro("Garcia");
        outraModalidade.modalidade = Modalidade.DOACAO;
        outraModalidade.preco = null;
        cenario.publicar(autor, outraModalidade);
        CenarioAnuncios.Dados outroTexto = cenario.new Dados().vendaDeOvos();
        outroTexto.bairro = cenario.bairro("Garcia");
        outroTexto.titulo = "Dúzia fresca";
        outroTexto.descricao = "Direto do galinheiro para você.";
        cenario.publicar(autor, outroTexto);

        mvc.perform(get("/anuncios")
                        .param("bairroId", cenario.bairro("Garcia").getId().toString())
                        .param("categoriaId", String.valueOf(CenarioAnuncios.OVOS))
                        .param("modalidade", "VENDA")
                        .param("texto", "CAIPIRAS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[*].id").value(contains(idAlvo)));
    }

    @Test
    void uc04_2_textoIgnoraMaiusculasEProcuraNoTituloENaDescricao() throws Exception {
        Usuario autor = cenario.anunciante();
        CenarioAnuncios.Dados noTitulo = cenario.new Dados();
        noTitulo.titulo = "Bergamotas maduras";
        String idTitulo = cenario.publicar(autor, noTitulo).getId().toString();
        CenarioAnuncios.Dados naDescricao = cenario.new Dados();
        naDescricao.descricao = "Sobraram algumas BERGAMOTAS do pomar.";
        String idDescricao = cenario.publicar(autor, naDescricao).getId().toString();
        cenario.doacaoDeFrutas(autor);

        mvc.perform(get("/anuncios").param("texto", "  bergaMOTAS "))
                .andExpect(jsonPath("$.conteudo[*].id").value(containsInAnyOrder(idTitulo, idDescricao)));
    }

    @Test
    void uc04_3_anunciosNaoVisiveisNaoAparecemNaBusca() throws Exception {
        String visivel = cenario.doacaoDeFrutas(cenario.anunciante()).getId().toString();
        naoVisiveis();

        mvc.perform(get("/anuncios"))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[*].id").value(contains(visivel)));
    }

    @Test
    void uc04_4_curingasDigitadosNaBuscaSaoTratadosComoTexto() throws Exception {
        Usuario autor = cenario.anunciante();
        cenario.doacaoDeFrutas(autor);
        CenarioAnuncios.Dados comPorcento = cenario.new Dados();
        comPorcento.titulo = "Suco 100% laranja";
        String idComPorcento = cenario.publicar(autor, comPorcento).getId().toString();

        mvc.perform(get("/anuncios").param("texto", "%"))
                .andExpect(jsonPath("$.conteudo[*].id").value(contains(idComPorcento)));
        mvc.perform(get("/anuncios").param("texto", "_"))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void uc04_parametrosInvalidosRetornam400() throws Exception {
        mvc.perform(get("/anuncios").param("texto", "a".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.texto").exists());
        mvc.perform(get("/anuncios").param("pagina", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/anuncios").param("modalidade", "TROCA")).andExpect(status().isBadRequest());
    }

    @Test
    void uc04_resumoSegueOContratoESemDadosDoAnunciante() throws Exception {
        Anuncio anuncio = cenario.publicar(cenario.anunciante(), cenario.new Dados().vendaDeOvos());

        mvc.perform(get("/anuncios"))
                .andExpect(jsonPath("$.conteudo[0].id").value(anuncio.getId().toString()))
                .andExpect(jsonPath("$.conteudo[0].titulo").value("Ovos caipiras"))
                .andExpect(jsonPath("$.conteudo[0].categoria.id").value(3))
                .andExpect(jsonPath("$.conteudo[0].categoria.nome").value("Ovos"))
                .andExpect(jsonPath("$.conteudo[0].bairro.nome").value("Centro"))
                .andExpect(jsonPath("$.conteudo[0].modalidade").value("VENDA"))
                .andExpect(jsonPath("$.conteudo[0].preco").value(12.00))
                .andExpect(jsonPath("$.conteudo[0].quantidade").value(2))
                .andExpect(jsonPath("$.conteudo[0].unidade").value("DUZIA"))
                .andExpect(jsonPath("$.conteudo[0].expiraEm").isNotEmpty())
                .andExpect(jsonPath("$.conteudo[0].criadoEm").isNotEmpty())
                .andExpect(jsonPath("$.conteudo[0].descricao").doesNotExist())
                .andExpect(jsonPath("$.conteudo[0].anunciante").doesNotExist())
                .andExpect(content().string(not(containsString("5547999998888"))));
    }

    @Test
    void uc05_1_detalheDeAnuncioNaoVisivelRetorna404() throws Exception {
        for (Map.Entry<String, UUID> caso : naoVisiveis().entrySet()) {
            int statusHttp = mvc.perform(get("/anuncios/" + caso.getValue())).andReturn().getResponse().getStatus();

            assertThat(statusHttp).as(caso.getKey()).isEqualTo(404);
        }
    }

    @Test
    void uc05_1_detalheDeAnuncioInexistenteRetorna404() throws Exception {
        mvc.perform(get("/anuncios/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    void uc05_2_detalheSegueOContratoENaoExpoeContatoDoAnunciante() throws Exception {
        Usuario autor = cenario.anunciante();
        Anuncio anuncio = cenario.publicar(autor, cenario.new Dados().vendaDeOvos());

        mvc.perform(get("/anuncios/" + anuncio.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(anuncio.getId().toString()))
                .andExpect(jsonPath("$.titulo").value("Ovos caipiras"))
                .andExpect(jsonPath("$.descricao").value("Uma dúzia de ovos de galinhas soltas."))
                .andExpect(jsonPath("$.conservacao").value("Manter em local fresco"))
                .andExpect(jsonPath("$.validadeAlimento").value(cenario.hoje().plusDays(10).toString()))
                .andExpect(jsonPath("$.origemProduto").value("Galinhas caipiras do quintal"))
                .andExpect(jsonPath("$.inspecao").value("SIM 0123 (fictício)"))
                .andExpect(jsonPath("$.anunciante.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.anunciante.tipo").value("MORADOR"))
                .andExpect(jsonPath("$.anunciante.length()").value(2))
                .andExpect(jsonPath("$.souDono").value(false))
                .andExpect(jsonPath("$.whatsapp").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(content().string(not(containsString("whatsapp"))))
                .andExpect(content().string(not(containsString("email"))))
                .andExpect(content().string(not(containsString("senha"))))
                .andExpect(content().string(not(containsString("5547999998888"))))
                .andExpect(content().string(not(containsString(autor.getEmail()))))
                .andExpect(content().string(not(containsString(autor.getId().toString()))));
    }

    @Test
    void uc05_souDonoEVerdadeiroSoParaASessaoDoAnunciante() throws Exception {
        Usuario autor = cenario.anunciante();
        Anuncio anuncio = cenario.doacaoDeFrutas(autor);
        MockHttpSession sessaoDoDono = ContaDeTeste.entrar(mvc, autor.getEmail());
        MockHttpSession sessaoDeOutro = ContaDeTeste.entrar(mvc, ContaDeTeste.cadastrar(mvc));

        mvc.perform(get("/anuncios/" + anuncio.getId()).session(sessaoDoDono))
                .andExpect(jsonPath("$.souDono").value(true));
        mvc.perform(get("/anuncios/" + anuncio.getId()).session(sessaoDeOutro))
                .andExpect(jsonPath("$.souDono").value(false));
    }

    /** Um anúncio para cada caso do UC04.3. */
    private Map<String, UUID> naoVisiveis() throws Exception {
        Map<String, UUID> casos = new LinkedHashMap<>();
        Usuario autor = cenario.anunciante();

        Anuncio encerrado = cenario.doacaoDeFrutas(autor);
        encerrado.encerrar(Resultado.DOADO, cenario.agora());
        casos.put("ENCERRADO", cenario.salvar(encerrado).getId());

        Anuncio removido = cenario.doacaoDeFrutas(autor);
        removido.remover(cenario.agora());
        casos.put("REMOVIDO", cenario.salvar(removido).getId());

        Anuncio expirado = cenario.doacaoDeFrutas(autor);
        cenario.vencerPrazo(expirado);
        casos.put("EXPIRADO", expirado.getId());
        assertThat(cenario.expirarVencidos()).isEqualTo(1);

        Anuncio vencido = cenario.doacaoDeFrutas(autor);
        cenario.vencerPrazo(vencido);
        casos.put("ATIVO com prazo vencido", vencido.getId());

        Anuncio validadeVencida = cenario.publicar(autor, cenario.new Dados().vendaDeOvos());
        cenario.vencerValidade(validadeVencida);
        casos.put("ATIVO com validade vencida", validadeVencida.getId());

        Usuario bloqueado = cenario.anunciante();
        casos.put("ATIVO de usuário bloqueado", cenario.doacaoDeFrutas(bloqueado).getId());
        cenario.bloquear(bloqueado);

        Usuario semAceite = cenario.anunciante();
        casos.put("ATIVO de usuário sem aceite", cenario.doacaoDeFrutas(semAceite).getId());
        cenario.revogarAceite(semAceite);

        return casos;
    }
}
