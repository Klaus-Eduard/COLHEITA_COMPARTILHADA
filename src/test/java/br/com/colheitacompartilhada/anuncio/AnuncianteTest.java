package br.com.colheitacompartilhada.anuncio;

import static br.com.colheitacompartilhada.usuario.ContaDeTeste.cadastrar;
import static br.com.colheitacompartilhada.usuario.ContaDeTeste.csrf;
import static br.com.colheitacompartilhada.usuario.ContaDeTeste.entrar;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import br.com.colheitacompartilhada.IntegracaoTest;
import br.com.colheitacompartilhada.usuario.ContaDeTeste;
import br.com.colheitacompartilhada.usuario.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

/** API do anunciante: UC06 Publicar, UC07 Editar, UC08 Encerrar e meus anúncios (04_API #7, 7b, 12–14). */
class AnuncianteTest extends IntegracaoTest {

    @Autowired
    CenarioAnuncios cenario;

    @Autowired
    ObjectMapper json;

    @Autowired
    JdbcTemplate jdbc;

    private Usuario dono;
    private MockHttpSession sessaoDoDono;

    @BeforeEach
    void anuncianteLogado() throws Exception {
        dono = cenario.anunciante();
        sessaoDoDono = entrar(mvc, dono.getEmail());
    }

    @Nested
    class Publicar {

        @Test
        void publicacaoValidaRetorna201ComLocationEDetalhe() throws Exception {
            String resposta = publicar(sessaoDoDono, doacao(), null)
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", startsWith("/anuncios/")))
                    .andExpect(jsonPath("$.titulo").value("Laranjas do quintal"))
                    .andExpect(jsonPath("$.categoria.nome").value("Frutas"))
                    .andExpect(jsonPath("$.anunciante.nome").value("Ana Souza"))
                    .andExpect(jsonPath("$.souDono").value(true))
                    .andExpect(jsonPath("$.fotoUrl").value((Object) null))
                    .andReturn().getResponse().getContentAsString();
            String id = JsonPath.read(resposta, "$.id");

            mvc.perform(get("/anuncios/" + id)).andExpect(status().isOk());
        }

        @Test
        void uc06_1_usuarioSemWhatsappOuSemAceiteRecebePerfilSemContato() throws Exception {
            MockHttpSession semContato = entrar(mvc, cadastrar(mvc));

            publicar(semContato, doacao(), null)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("PERFIL_SEM_CONTATO"));
        }

        @Test
        void uc06_2_doacaoComPrecoRetorna400NoCampoPreco() throws Exception {
            Map<String, Object> dados = doacao();
            dados.put("preco", 5);

            publicar(sessaoDoDono, dados, null)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.preco").value("Doação não tem preço."));
        }

        @Test
        void uc06_2_vendaComPrecoZeroOuNegativoRetorna400() throws Exception {
            for (Object preco : new Object[] {0, -1}) {
                Map<String, Object> dados = venda();
                dados.put("preco", preco);

                publicar(sessaoDoDono, dados, null)
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.campos.preco").exists());
            }
        }

        @Test
        void uc06_2_vendaSemPrecoOuComPrecoDeDuasCasasRetorna201() throws Exception {
            Map<String, Object> semPreco = venda();
            semPreco.remove("preco");
            Map<String, Object> comPreco = venda();
            comPreco.put("preco", 12.50);

            publicar(sessaoDoDono, semPreco, null)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.preco").value((Object) null));
            publicar(sessaoDoDono, comPreco, null)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.preco").value(12.5));
        }

        @Test
        void uc06_3_ovosSemValidadeOrigemOuInspecaoRetorna400NoCampoCorrespondente() throws Exception {
            for (String campo : new String[] {"validadeAlimento", "origemProduto", "inspecao"}) {
                Map<String, Object> dados = venda();
                dados.remove(campo);

                publicar(sessaoDoDono, dados, null)
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.campos." + campo).exists());
            }
        }

        @Test
        void uc06_3_ovosComOsTresCamposEFrutasSemElesRetornam201() throws Exception {
            publicar(sessaoDoDono, venda(), null).andExpect(status().isCreated());
            publicar(sessaoDoDono, doacao(), null).andExpect(status().isCreated());
        }

        @Test
        void uc06_4_categoriaInexistenteRetorna400NoCampoCategoria() throws Exception {
            Map<String, Object> dados = doacao();
            dados.put("categoriaId", 99);

            publicar(sessaoDoDono, dados, null)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.categoriaId").value("Selecione a categoria."));
        }

        @Test
        void bairroInexistenteRetorna400NoCampoBairro() throws Exception {
            Map<String, Object> dados = doacao();
            dados.put("bairroId", 9999);

            publicar(sessaoDoDono, dados, null)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.bairroId").value("Selecione o bairro."));
        }

        @Test
        void uc06_5_duziaFracionadaRetorna400EQuiloFracionadoRetorna201() throws Exception {
            Map<String, Object> duzia = venda();
            duzia.put("quantidade", 1.5);
            Map<String, Object> quilo = doacao();
            quilo.put("quantidade", 1.5);

            publicar(sessaoDoDono, duzia, null)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.quantidade").value("Para esta unidade, use número inteiro."));
            publicar(sessaoDoDono, quilo, null).andExpect(status().isCreated());
        }

        @Test
        void uc06_6_prazoForaDoIntervaloOuDepoisDaValidadeOuValidadeVencidaRetornam400() throws Exception {
            Map<String, Object> oitoDias = doacao();
            oitoDias.put("disponivelAte", hoje().plusDays(8).toString());
            Map<String, Object> depoisDaValidade = venda();
            depoisDaValidade.put("validadeAlimento", hoje().plusDays(1).toString());
            depoisDaValidade.put("disponivelAte", hoje().plusDays(2).toString());
            Map<String, Object> validadeOntem = venda();
            validadeOntem.put("validadeAlimento", hoje().minusDays(1).toString());
            validadeOntem.put("disponivelAte", hoje().toString());

            publicar(sessaoDoDono, oitoDias, null).andExpect(jsonPath("$.campos.disponivelAte").exists());
            publicar(sessaoDoDono, depoisDaValidade, null).andExpect(jsonPath("$.campos.disponivelAte").exists());
            publicar(sessaoDoDono, validadeOntem, null).andExpect(jsonPath("$.campos.validadeAlimento").exists());
        }

        @Test
        void uc06_7_expiraEmVemCalculadoPeloServidor() throws Exception {
            Map<String, Object> dados = doacao();
            dados.put("disponivelAte", hoje().toString());
            Instant fimDeHoje = hoje().plusDays(1).atStartOfDay(java.time.ZoneId.of("America/Sao_Paulo")).toInstant();

            publicar(sessaoDoDono, dados, null)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.expiraEm").value(fimDeHoje.toString()));
        }

        @Test
        void uc06_8_excedenteNaoDeclaradoRetorna400() throws Exception {
            Map<String, Object> dados = doacao();
            dados.put("excedenteDeclarado", false);

            publicar(sessaoDoDono, dados, null)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.excedenteDeclarado").exists());
        }

        @Test
        void camposDeTextoForaDoTamanhoVoltamTodosDeUmaVez() throws Exception {
            Map<String, Object> dados = doacao();
            dados.put("titulo", "  ab  ");
            dados.put("descricao", "curta");
            dados.put("conservacao", "   ");
            dados.remove("bairroId");

            publicar(sessaoDoDono, dados, null)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.titulo").value("O título deve ter de 3 a 100 caracteres."))
                    .andExpect(jsonPath("$.campos.descricao").value("A descrição deve ter de 10 a 1000 caracteres."))
                    .andExpect(jsonPath("$.campos.conservacao").value("Informe como o alimento deve ser conservado."))
                    .andExpect(jsonPath("$.campos.bairroId").value("Selecione o bairro."));
        }

        @Test
        void uc06_9_fotoPngDeUmMegaRetorna201ComFotoUrlAcessivel() throws Exception {
            MockMultipartFile png = Imagens.png(1024 * 1024);

            String resposta = publicar(sessaoDoDono, doacao(), png)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.fotoUrl").value(startsWith("/fotos/")))
                    .andReturn().getResponse().getContentAsString();

            mvc.perform(get(JsonPath.<String>read(resposta, "$.fotoUrl")))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "image/png"));
        }

        @Test
        void uc06_9_arquivoPngComTextoRetorna400ArquivoInvalidoENadaEGravado() throws Exception {
            long antes = anunciosDoDono();
            MockMultipartFile texto = new MockMultipartFile("foto", "foto.png", "image/png",
                    "isto é texto".getBytes(StandardCharsets.UTF_8));

            publicar(sessaoDoDono, doacao(), texto)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("ARQUIVO_INVALIDO"));

            assertThat(anunciosDoDono()).isEqualTo(antes);
        }

        @Test
        void uc06_9_arquivoDeTresMegasRetorna413() throws Exception {
            publicar(sessaoDoDono, doacao(), Imagens.png(3 * 1024 * 1024))
                    .andExpect(status().isPayloadTooLarge())
                    .andExpect(jsonPath("$.codigo").value("ARQUIVO_MUITO_GRANDE"));
        }

        @Test
        void visitanteNaoPublica() throws Exception {
            publicar(new MockHttpSession(), doacao(), null).andExpect(status().isUnauthorized());
        }

        @Test
        void semAParteDadosRetorna400() throws Exception {
            mvc.perform(multipart("/anuncios").file(Imagens.png(100)).with(csrf()).session(sessaoDoDono))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
        }
    }

    @Nested
    class Editar {

        @Test
        void uc07_1_outroUsuarioRecebe403EOAnuncioFicaInalterado() throws Exception {
            String id = publicarComSucesso(doacao(), null);
            MockHttpSession outro = entrar(mvc, cenario.anunciante().getEmail());
            Map<String, Object> dados = doacao();
            dados.put("titulo", "Título trocado por outra pessoa");

            editar(outro, id, dados, null)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));

            mvc.perform(get("/anuncios/" + id)).andExpect(jsonPath("$.titulo").value("Laranjas do quintal"));
        }

        @Test
        void adminTambemNaoEditaAnuncioAlheio() throws Exception {
            String id = publicarComSucesso(doacao(), null);
            Usuario admin = cenario.anunciante();
            jdbc.update("update usuario set papel = 'ADMIN' where id = ?", admin.getId());

            editar(entrar(mvc, admin.getEmail()), id, doacao(), null).andExpect(status().isForbidden());
        }

        @Test
        void uc07_2_donoEditaOTituloEAtualizadoEmMuda() throws Exception {
            String id = publicarComSucesso(doacao(), null);
            String antes = campoDoMeuAnuncio(id, "$.atualizadoEm");
            relogio.avancar(java.time.Duration.ofMinutes(5));
            Map<String, Object> dados = doacao();
            dados.put("titulo", "Laranjas e limões");

            editar(sessaoDoDono, id, dados, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.titulo").value("Laranjas e limões"))
                    .andExpect(jsonPath("$.categoria.nome").value("Frutas"))
                    .andExpect(jsonPath("$.anunciante.nome").value("Ana Souza"));

            assertThat(campoDoMeuAnuncio(id, "$.atualizadoEm")).isNotEqualTo(antes);
        }

        @Test
        void uc07_3_aumentarOPrazoRetorna400EReduzirRetorna200() throws Exception {
            String id = publicarComSucesso(doacao(), null);
            Map<String, Object> maisLongo = doacao();
            maisLongo.put("disponivelAte", hoje().plusDays(5).toString());
            Map<String, Object> maisCurto = doacao();
            maisCurto.put("disponivelAte", hoje().toString());

            editar(sessaoDoDono, id, maisLongo, null)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.disponivelAte").value("A edição não pode ampliar o prazo do anúncio."));
            editar(sessaoDoDono, id, maisCurto, null).andExpect(status().isOk());
        }

        @Test
        void uc07_4_editarAnuncioEncerradoExpiradoRemovidoOuVencidoRetorna409() throws Exception {
            String encerrado = publicarComSucesso(doacao(), null);
            encerrar(sessaoDoDono, encerrado, "DOADO").andExpect(status().isOk());
            Anuncio expirado = cenario.doacaoDeFrutas(dono);
            cenario.vencerPrazo(expirado);
            cenario.expirarVencidos();
            Anuncio removido = cenario.doacaoDeFrutas(dono);
            removido.remover(cenario.agora());
            cenario.salvar(removido);
            Anuncio vencido = cenario.doacaoDeFrutas(dono);
            cenario.vencerPrazo(vencido);

            editar(sessaoDoDono, encerrado, doacao(), null)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("ESTADO_INVALIDO"))
                    .andExpect(jsonPath("$.statusAtual").value("ENCERRADO"));
            editar(sessaoDoDono, expirado.getId().toString(), doacao(), null)
                    .andExpect(jsonPath("$.statusAtual").value("EXPIRADO"));
            editar(sessaoDoDono, removido.getId().toString(), doacao(), null)
                    .andExpect(jsonPath("$.statusAtual").value("REMOVIDO"));
            editar(sessaoDoDono, vencido.getId().toString(), doacao(), null)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusAtual").value("ATIVO"));
        }

        @Test
        void uc07_5_trocarParaMelSemOrigemRetorna400() throws Exception {
            String id = publicarComSucesso(doacao(), null);
            Map<String, Object> dados = doacao();
            dados.put("categoriaId", 4);
            dados.put("validadeAlimento", hoje().plusDays(30).toString());
            dados.put("inspecao", "SIM 0456");

            editar(sessaoDoDono, id, dados, null)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.origemProduto").exists());
        }

        @Test
        void anuncioInexistenteRetorna404() throws Exception {
            editar(sessaoDoDono, UUID.randomUUID().toString(), doacao(), null).andExpect(status().isNotFound());
        }

        @Test
        void uc06_11_trocarAFotoMudaAUrlEMantemUmaUnicaLinha() throws Exception {
            String id = publicarComSucesso(doacao(), Imagens.png(1024));
            String primeira = campoDoMeuAnuncio(id, "$.fotoUrl");

            String resposta = editar(sessaoDoDono, id, doacao(), Imagens.jpeg(2048))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            String segunda = JsonPath.read(resposta, "$.fotoUrl");
            assertThat(segunda).startsWith("/fotos/").isNotEqualTo(primeira);
            assertThat(fotosDo(id)).isEqualTo(1);
            mvc.perform(get(primeira)).andExpect(status().isNotFound());
        }

        @Test
        void semFotoNovaMantemAFotoERemoverFotoApaga() throws Exception {
            String id = publicarComSucesso(doacao(), Imagens.png(1024));
            String foto = campoDoMeuAnuncio(id, "$.fotoUrl");
            Map<String, Object> remover = doacao();
            remover.put("removerFoto", true);

            editar(sessaoDoDono, id, doacao(), null).andExpect(jsonPath("$.fotoUrl").value(foto));
            editar(sessaoDoDono, id, remover, null).andExpect(jsonPath("$.fotoUrl").value((Object) null));

            assertThat(fotosDo(id)).isZero();
        }
    }

    @Nested
    class Encerrar {

        @Test
        void uc08_1_doacaoEncerradaComoDoado() throws Exception {
            String id = publicarComSucesso(doacao(), null);

            encerrar(sessaoDoDono, id, "DOADO")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ENCERRADO"))
                    .andExpect(jsonPath("$.resultado").value("DOADO"))
                    .andExpect(jsonPath("$.encerradoEm").isNotEmpty())
                    .andExpect(jsonPath("$.visivel").value(false))
                    .andExpect(jsonPath("$.podeEditar").value(false));

            mvc.perform(get("/anuncios/" + id)).andExpect(status().isNotFound());
        }

        @Test
        void uc08_2_doacaoEncerradaComoVendidoRetorna400() throws Exception {
            String id = publicarComSucesso(doacao(), null);

            encerrar(sessaoDoDono, id, "VENDIDO")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                    .andExpect(jsonPath("$.campos.resultado").exists());
        }

        @Test
        void uc08_3_segundoEncerramentoRetorna409ENaoMudaEncerradoEm() throws Exception {
            String id = publicarComSucesso(doacao(), null);
            String primeiro = encerrar(sessaoDoDono, id, "DOADO").andReturn().getResponse().getContentAsString();
            relogio.avancar(java.time.Duration.ofMinutes(5));

            encerrar(sessaoDoDono, id, "NAO_INFORMADO")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusAtual").value("ENCERRADO"));

            assertThat(campoDoMeuAnuncio(id, "$.encerradoEm"))
                    .isEqualTo(JsonPath.<String>read(primeiro, "$.encerradoEm"));
            assertThat(campoDoMeuAnuncio(id, "$.resultado")).isEqualTo("DOADO");
        }

        @Test
        void uc08_4_outroUsuarioRecebe403() throws Exception {
            String id = publicarComSucesso(doacao(), null);
            MockHttpSession outro = entrar(mvc, cenario.anunciante().getEmail());

            encerrar(outro, id, "DOADO").andExpect(status().isForbidden());

            assertThat(campoDoMeuAnuncio(id, "$.status")).isEqualTo("ATIVO");
        }

        @Test
        void resultadoAusenteOuDesconhecidoRetorna400() throws Exception {
            String id = publicarComSucesso(doacao(), null);

            mvc.perform(post("/anuncios/" + id + "/encerramento").with(csrf()).session(sessaoDoDono)
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.resultado").value("Selecione o resultado."));
            encerrar(sessaoDoDono, id, "TROCADO").andExpect(status().isBadRequest());
        }
    }

    @Nested
    class MeusAnuncios {

        @Test
        void listaTodosOsStatusDoDonoComVisivelEPodeEditar() throws Exception {
            String ativo = publicarComSucesso(doacao(), null);
            String encerrado = publicarComSucesso(doacao(), null);
            encerrar(sessaoDoDono, encerrado, "DOADO");
            Anuncio vencido = cenario.doacaoDeFrutas(dono);
            cenario.vencerPrazo(vencido);
            cenario.doacaoDeFrutas(cenario.anunciante());

            mvc.perform(get("/me/anuncios").session(sessaoDoDono))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElementos").value(3))
                    .andExpect(jsonPath("$.tamanho").value(20))
                    .andExpect(jsonPath("$.conteudo[*].id")
                            .value(containsInAnyOrder(ativo, encerrado, vencido.getId().toString())))
                    .andExpect(jsonPath("$.conteudo[?(@.id == '" + ativo + "')].visivel").value(true))
                    .andExpect(jsonPath("$.conteudo[?(@.id == '" + ativo + "')].podeEditar").value(true))
                    .andExpect(jsonPath("$.conteudo[?(@.id == '" + encerrado + "')].status").value("ENCERRADO"))
                    .andExpect(jsonPath("$.conteudo[?(@.id == '" + vencido.getId() + "')].status").value("ATIVO"))
                    .andExpect(jsonPath("$.conteudo[?(@.id == '" + vencido.getId() + "')].visivel").value(false))
                    .andExpect(jsonPath("$.conteudo[?(@.id == '" + vencido.getId() + "')].podeEditar").value(false));
        }

        @Test
        void anuncioOcultoPorFaltaDeAceiteContinuaNaListaDoDono() throws Exception {
            String id = publicarComSucesso(doacao(), null);
            cenario.revogarAceite(dono);

            mvc.perform(get("/me/anuncios/" + id).session(sessaoDoDono))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.visivel").value(false))
                    .andExpect(jsonPath("$.podeEditar").value(true))
                    .andExpect(jsonPath("$.souDono").value(true));
        }

        @Test
        void meuAnuncioDeOutroUsuarioRetorna403EInexistente404() throws Exception {
            String id = publicarComSucesso(doacao(), null);
            MockHttpSession outro = entrar(mvc, ContaDeTeste.cadastrar(mvc));

            mvc.perform(get("/me/anuncios/" + id).session(outro)).andExpect(status().isForbidden());
            mvc.perform(get("/me/anuncios/" + UUID.randomUUID()).session(sessaoDoDono)).andExpect(status().isNotFound());
        }

        @Test
        void visitanteRecebe401() throws Exception {
            mvc.perform(get("/me/anuncios")).andExpect(status().isUnauthorized());
        }
    }

    private Map<String, Object> doacao() {
        Map<String, Object> dados = new HashMap<>();
        dados.put("titulo", "Laranjas do quintal");
        dados.put("descricao", "Laranjas colhidas hoje, sem agrotóxico.");
        dados.put("categoriaId", CenarioAnuncios.FRUTAS);
        dados.put("bairroId", cenario.bairro("Centro").getId());
        dados.put("quantidade", 5);
        dados.put("unidade", "KG");
        dados.put("modalidade", "DOACAO");
        dados.put("conservacao", "Manter em local fresco");
        dados.put("disponivelAte", hoje().plusDays(2).toString());
        dados.put("excedenteDeclarado", true);
        return dados;
    }

    private Map<String, Object> venda() {
        Map<String, Object> dados = doacao();
        dados.put("titulo", "Ovos caipiras");
        dados.put("descricao", "Uma dúzia de ovos de galinhas soltas.");
        dados.put("categoriaId", CenarioAnuncios.OVOS);
        dados.put("quantidade", 2);
        dados.put("unidade", "DUZIA");
        dados.put("modalidade", "VENDA");
        dados.put("preco", 12.00);
        dados.put("validadeAlimento", hoje().plusDays(10).toString());
        dados.put("origemProduto", "Galinhas caipiras do quintal");
        dados.put("inspecao", "SIM 0123 (fictício)");
        return dados;
    }

    private LocalDate hoje() {
        return cenario.hoje();
    }

    private ResultActions publicar(MockHttpSession sessao, Map<String, Object> dados, MockMultipartFile foto)
            throws Exception {
        var pedido = multipart("/anuncios").file(parteDados(dados));
        if (foto != null) {
            pedido.file(foto);
        }
        return mvc.perform(pedido.with(csrf()).session(sessao));
    }

    private ResultActions editar(MockHttpSession sessao, String id, Map<String, Object> dados,
            MockMultipartFile foto) throws Exception {
        var pedido = multipart(HttpMethod.PATCH, "/anuncios/" + id).file(parteDados(dados));
        if (foto != null) {
            pedido.file(foto);
        }
        return mvc.perform(pedido.with(csrf()).session(sessao));
    }

    private ResultActions encerrar(MockHttpSession sessao, String id, String resultado) throws Exception {
        return mvc.perform(post("/anuncios/" + id + "/encerramento").with(csrf()).session(sessao)
                .contentType(MediaType.APPLICATION_JSON).content("{\"resultado\":\"" + resultado + "\"}"));
    }

    private String publicarComSucesso(Map<String, Object> dados, MockMultipartFile foto) throws Exception {
        String resposta = publicar(sessaoDoDono, dados, foto)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private String campoDoMeuAnuncio(String id, String caminho) throws Exception {
        String resposta = mvc.perform(get("/me/anuncios/" + id).session(sessaoDoDono))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, caminho);
    }

    private MockMultipartFile parteDados(Map<String, Object> dados) throws Exception {
        return new MockMultipartFile("dados", "", MediaType.APPLICATION_JSON_VALUE, json.writeValueAsBytes(dados));
    }

    private long anunciosDoDono() {
        return jdbc.queryForObject("select count(*) from anuncio where usuario_id = ?", Long.class, dono.getId());
    }

    private int fotosDo(String anuncioId) {
        return jdbc.queryForObject("select count(*) from foto where anuncio_id = ?::uuid", Integer.class, anuncioId);
    }
}
