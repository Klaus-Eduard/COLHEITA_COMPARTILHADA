package br.com.colheitacompartilhada.anuncio;

import static br.com.colheitacompartilhada.anuncio.Fixtures.AGORA;
import static br.com.colheitacompartilhada.anuncio.Fixtures.HOJE;
import static br.com.colheitacompartilhada.anuncio.Fixtures.anunciante;
import static br.com.colheitacompartilhada.anuncio.Fixtures.doacaoDeFrutas;
import static br.com.colheitacompartilhada.anuncio.Fixtures.vendaDeOvos;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import br.com.colheitacompartilhada.anuncio.Fixtures.Dados;
import br.com.colheitacompartilhada.comum.EstadoInvalidoException;
import br.com.colheitacompartilhada.comum.RegraNegocioException;
import br.com.colheitacompartilhada.config.ClockConfig;
import br.com.colheitacompartilhada.usuario.Usuario;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

/** RN02–RN11 na entidade. Relógio fixo em 2026-10-20T15:00Z (meio-dia em São Paulo). */
class AnuncioTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-20T15:00:00Z"), ZoneOffset.UTC);

    @Test
    void relogioDosTestesCorrespondeAoHojeDeSaoPaulo() {
        assertThat(Instant.now(CLOCK)).isEqualTo(AGORA);
        assertThat(LocalDate.now(CLOCK.withZone(ClockConfig.FUSO_NEGOCIO))).isEqualTo(HOJE);
    }

    @Nested
    class Publicar {

        @Test
        void publicacaoValidaNasceAtivaComOsDadosInformados() {
            Usuario autor = anunciante();
            Dados dados = doacaoDeFrutas();
            dados.titulo = "  Laranjas do quintal  ";
            dados.origemProduto = "   ";

            Anuncio anuncio = Anuncio.publicar(autor, dados.montar(), AGORA, HOJE);

            assertThat(anuncio.getStatus()).isEqualTo(StatusAnuncio.ATIVO);
            assertThat(anuncio.getUsuario()).isSameAs(autor);
            assertThat(anuncio.getTitulo()).isEqualTo("Laranjas do quintal");
            assertThat(anuncio.getOrigemProduto()).isNull();
            assertThat(anuncio.getCriadoEm()).isEqualTo(AGORA);
            assertThat(anuncio.getAtualizadoEm()).isEqualTo(AGORA);
            assertThat(anuncio.getEncerradoEm()).isNull();
            assertThat(anuncio.getResultado()).isNull();
            assertThat(anuncio.isExcedenteDeclarado()).isTrue();
            assertThat(anuncio.getExpiraEm()).isAfter(anuncio.getCriadoEm());
        }

        @Test
        void uc06_1_usuarioSemWhatsappOuSemAceiteNaoPublica() {
            assertThatThrownBy(() -> Anuncio.publicar(Fixtures.usuarioSemContato(), doacaoDeFrutas().montar(), AGORA, HOJE))
                    .isInstanceOfSatisfying(RegraNegocioException.class, erro -> {
                        assertThat(erro.getCodigo()).isEqualTo("PERFIL_SEM_CONTATO");
                        assertThat(erro.getCampo()).isNull();
                    });
        }

        @Test
        void uc06_1_usuarioBloqueadoNaoPublica() {
            Usuario autor = anunciante();
            autor.bloquear();

            assertThatThrownBy(() -> Anuncio.publicar(autor, doacaoDeFrutas().montar(), AGORA, HOJE))
                    .isInstanceOfSatisfying(RegraNegocioException.class,
                            erro -> assertThat(erro.getCodigo()).isEqualTo("PERFIL_SEM_CONTATO"));
        }

        @Test
        void uc06_2_rn03_doacaoComPrecoERecusada() {
            Dados dados = doacaoDeFrutas();
            dados.preco = new BigDecimal("5.00");

            assertInvalido(() -> publicar(dados), "preco", "Doação não tem preço.");
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "-1"})
        void uc06_2_rn03_vendaComPrecoZeroOuNegativoERecusada(String preco) {
            Dados dados = vendaDeOvos();
            dados.preco = new BigDecimal(preco);

            assertInvalido(() -> publicar(dados), "preco", "O preço deve ser maior que zero.");
        }

        @Test
        void uc06_2_rn03_vendaSemPrecoEAceita() {
            Dados dados = vendaDeOvos();
            dados.preco = null;

            assertThat(publicar(dados).getPreco()).isNull();
        }

        @Test
        void uc06_2_rn03_vendaComPrecoDeDuasCasasEAceita() {
            Dados dados = vendaDeOvos();
            dados.preco = new BigDecimal("12.50");

            assertThat(publicar(dados).getPreco()).isEqualByComparingTo("12.50");
        }

        @Test
        void rn03_precoComTresCasasDecimaisERecusado() {
            Dados dados = vendaDeOvos();
            dados.preco = new BigDecimal("12.505");

            assertInvalido(() -> publicar(dados), "preco");
        }

        @Test
        void uc06_3_rn05_ovosSemValidadeERecusado() {
            Dados dados = vendaDeOvos();
            dados.validadeAlimento = null;

            assertInvalido(() -> publicar(dados), "validadeAlimento", "Informe a validade.");
        }

        @Test
        void uc06_3_rn05_ovosSemOrigemERecusado() {
            Dados dados = vendaDeOvos();
            dados.origemProduto = " ";

            assertInvalido(() -> publicar(dados), "origemProduto", "Informe a origem do produto.");
        }

        @Test
        void uc06_3_rn05_ovosSemInspecaoERecusado() {
            Dados dados = vendaDeOvos();
            dados.inspecao = null;

            assertInvalido(() -> publicar(dados), "inspecao", "Informe a identificação da inspeção.");
        }

        @Test
        void uc06_3_rn05_ovosComOsTresCamposEAceito() {
            Anuncio anuncio = publicar(vendaDeOvos());

            assertThat(anuncio.getOrigemProduto()).isEqualTo("Quintal próprio");
            assertThat(anuncio.getInspecao()).isEqualTo("SIM 0123 (fictício)");
        }

        @Test
        void uc06_3_rn05_frutasSemCamposDeOrigemAnimalEAceito() {
            assertThatCode(() -> publicar(doacaoDeFrutas())).doesNotThrowAnyException();
        }

        @Test
        void uc06_5_rn06_duziaComQuantidadeFracionadaERecusada() {
            Dados dados = vendaDeOvos();
            dados.quantidade = new BigDecimal("1.5");

            assertInvalido(() -> publicar(dados), "quantidade", "Para esta unidade, use número inteiro.");
        }

        @Test
        void uc06_5_rn06_quiloComQuantidadeFracionadaEAceito() {
            assertThat(publicar(doacaoDeFrutas()).getQuantidade()).isEqualByComparingTo("1.5");
        }

        @Test
        void rn06_unidadeInteiraAceitaNumeroInteiroEscritoComZeros() {
            Dados dados = vendaDeOvos();
            dados.quantidade = new BigDecimal("2.000");

            assertThatCode(() -> publicar(dados)).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "-0.5"})
        void rn06_quantidadeZeroOuNegativaERecusada(String quantidade) {
            Dados dados = doacaoDeFrutas();
            dados.quantidade = new BigDecimal(quantidade);

            assertInvalido(() -> publicar(dados), "quantidade", "Informe uma quantidade maior que zero.");
        }

        @ParameterizedTest
        @ValueSource(strings = {"1.2345", "10000000"})
        void rn06_quantidadeForaDoFormatoDoBancoERecusada(String quantidade) {
            Dados dados = doacaoDeFrutas();
            dados.quantidade = new BigDecimal(quantidade);

            assertInvalido(() -> publicar(dados), "quantidade");
        }

        @Test
        void uc06_6_rn07_disponivelAteOitoDiasAFrenteERecusado() {
            Dados dados = doacaoDeFrutas();
            dados.disponivelAte = HOJE.plusDays(8);

            assertInvalido(() -> publicar(dados), "disponivelAte", "Escolha uma data entre hoje e 7 dias.");
        }

        @Test
        void rn07_disponivelAteOntemERecusado() {
            Dados dados = doacaoDeFrutas();
            dados.disponivelAte = HOJE.minusDays(1);

            assertInvalido(() -> publicar(dados), "disponivelAte", "Escolha uma data entre hoje e 7 dias.");
        }

        @Test
        void rn07_disponivelAteHojeEEmSeteDiasSaoAceitos() {
            Dados hoje = doacaoDeFrutas();
            hoje.disponivelAte = HOJE;
            Dados seteDias = doacaoDeFrutas();
            seteDias.disponivelAte = HOJE.plusDays(7);

            assertThatCode(() -> publicar(hoje)).doesNotThrowAnyException();
            assertThatCode(() -> publicar(seteDias)).doesNotThrowAnyException();
        }

        @Test
        void uc06_6_rn07_disponivelAteDepoisDaValidadeERecusado() {
            Dados dados = doacaoDeFrutas();
            dados.validadeAlimento = HOJE.plusDays(1);
            dados.disponivelAte = HOJE.plusDays(2);

            assertInvalido(() -> publicar(dados), "disponivelAte", "Não pode passar da validade.");
        }

        @Test
        void uc06_6_rn07_validadeOntemERecusada() {
            Dados dados = doacaoDeFrutas();
            dados.validadeAlimento = HOJE.minusDays(1);
            dados.disponivelAte = HOJE;

            assertInvalido(() -> publicar(dados), "validadeAlimento", "A validade já passou.");
        }

        @Test
        void uc06_7_rn07_expiraNoLimiteDeSeteDiasCorridos() {
            Dados dados = doacaoDeFrutas();
            dados.disponivelAte = LocalDate.parse("2026-10-27");

            assertThat(publicar(dados).getExpiraEm()).isEqualTo(Instant.parse("2026-10-27T15:00:00Z"));
        }

        @Test
        void uc06_7_rn07_expiraNoFimDoDiaEmSaoPaulo() {
            Dados dados = doacaoDeFrutas();
            dados.disponivelAte = LocalDate.parse("2026-10-22");

            assertThat(publicar(dados).getExpiraEm()).isEqualTo(Instant.parse("2026-10-23T03:00:00Z"));
        }

        @Test
        void uc06_8_excedenteNaoDeclaradoERecusado() {
            Dados dados = doacaoDeFrutas();
            dados.excedenteDeclarado = false;

            assertInvalido(() -> publicar(dados), "excedenteDeclarado", "Confirme que é excedente próprio.");
        }
    }

    @Nested
    class Editar {

        private final Instant depois = AGORA.plus(Duration.ofHours(1));

        @Test
        void uc07_2_edicaoAlteraOsDadosEAtualizadoEm() {
            Anuncio anuncio = publicar(doacaoDeFrutas());
            Dados novos = doacaoDeFrutas();
            novos.titulo = "Laranjas e limões";

            anuncio.editar(novos.montar(), depois, HOJE);

            assertThat(anuncio.getTitulo()).isEqualTo("Laranjas e limões");
            assertThat(anuncio.getAtualizadoEm()).isEqualTo(depois);
            assertThat(anuncio.getCriadoEm()).isEqualTo(AGORA);
            assertThat(anuncio.getStatus()).isEqualTo(StatusAnuncio.ATIVO);
        }

        @Test
        void uc07_3_rn07_edicaoNaoPodeAmpliarOPrazo() {
            Anuncio anuncio = publicar(doacaoDeFrutas());
            Instant expiraAntes = anuncio.getExpiraEm();
            Dados novos = doacaoDeFrutas();
            novos.disponivelAte = HOJE.plusDays(3);

            assertInvalido(() -> anuncio.editar(novos.montar(), depois, HOJE), "disponivelAte",
                    "A edição não pode ampliar o prazo do anúncio.");
            assertThat(anuncio.getExpiraEm()).isEqualTo(expiraAntes);
        }

        @Test
        void uc07_3_rn07_edicaoPodeReduzirOPrazo() {
            Anuncio anuncio = publicar(doacaoDeFrutas());
            Dados novos = doacaoDeFrutas();
            novos.disponivelAte = HOJE.plusDays(1);

            anuncio.editar(novos.montar(), depois, HOJE);

            assertThat(anuncio.getExpiraEm()).isEqualTo(Instant.parse("2026-10-22T03:00:00Z"));
        }

        @Test
        void rn07_edicaoComOMesmoPrazoMantemExpiraEm() {
            Dados dados = doacaoDeFrutas();
            dados.disponivelAte = HOJE.plusDays(7);
            Anuncio anuncio = publicar(dados);
            Instant expiraAntes = anuncio.getExpiraEm();

            anuncio.editar(dados.montar(), depois, HOJE);

            assertThat(anuncio.getExpiraEm()).isEqualTo(expiraAntes);
        }

        @Test
        void rn07_edicaoUsaOHojeDoMomentoDaEdicao() {
            Anuncio anuncio = publicar(doacaoDeFrutas());
            Dados novos = doacaoDeFrutas();
            novos.disponivelAte = HOJE;

            assertInvalido(() -> anuncio.editar(novos.montar(), AGORA.plus(Duration.ofDays(1)), HOJE.plusDays(1)),
                    "disponivelAte", "Escolha uma data entre hoje e 7 dias.");
        }

        @Test
        void uc07_4_rn09_editarAnuncioEncerradoExpiradoOuRemovidoEEstadoInvalido() {
            Anuncio encerrado = publicar(doacaoDeFrutas());
            encerrado.encerrar(Resultado.DOADO, depois);
            Anuncio expirado = publicar(doacaoDeFrutas());
            expirado.expirar(expirado.getExpiraEm());
            Anuncio removido = publicar(doacaoDeFrutas());
            removido.remover(depois);

            assertEstadoInvalido(() -> encerrado.editar(doacaoDeFrutas().montar(), depois, HOJE), "ENCERRADO");
            assertEstadoInvalido(() -> expirado.editar(doacaoDeFrutas().montar(), depois, HOJE), "EXPIRADO");
            assertEstadoInvalido(() -> removido.editar(doacaoDeFrutas().montar(), depois, HOJE), "REMOVIDO");
        }

        @Test
        void uc07_4_rn09_editarAnuncioAtivoMasVencidoEEstadoInvalido() {
            Anuncio anuncio = publicar(doacaoDeFrutas());
            Instant noVencimento = anuncio.getExpiraEm();

            assertEstadoInvalido(() -> anuncio.editar(doacaoDeFrutas().montar(), noVencimento, HOJE.plusDays(3)), "ATIVO");
        }

        @Test
        void uc07_5_trocarParaCategoriaAnimalSemOrigemReaplicaAsValidacoes() {
            Anuncio anuncio = publicar(doacaoDeFrutas());
            Dados novos = doacaoDeFrutas();
            novos.categoria = Fixtures.ovos();
            novos.validadeAlimento = HOJE.plusDays(10);

            assertInvalido(() -> anuncio.editar(novos.montar(), depois, HOJE), "origemProduto");
            assertThat(anuncio.getCategoria().getNome()).isEqualTo("Frutas");
        }
    }

    @Nested
    class Encerrar {

        private final Instant depois = AGORA.plus(Duration.ofHours(2));

        @Test
        void uc08_1_rn09_doacaoEncerradaComoDoado() {
            Anuncio anuncio = publicar(doacaoDeFrutas());

            anuncio.encerrar(Resultado.DOADO, depois);

            assertThat(anuncio.getStatus()).isEqualTo(StatusAnuncio.ENCERRADO);
            assertThat(anuncio.getResultado()).isEqualTo(Resultado.DOADO);
            assertThat(anuncio.getEncerradoEm()).isEqualTo(depois);
            assertThat(anuncio.getAtualizadoEm()).isEqualTo(depois);
        }

        @Test
        void uc08_2_rn09_doacaoNaoPodeSerEncerradaComoVendido() {
            Anuncio anuncio = publicar(doacaoDeFrutas());

            assertInvalido(() -> anuncio.encerrar(Resultado.VENDIDO, depois), "resultado");
            assertThat(anuncio.getStatus()).isEqualTo(StatusAnuncio.ATIVO);
        }

        @Test
        void rn09_vendaAceitaVendidoENaoInformadoMasNaoDoado() {
            Anuncio vendido = publicar(vendaDeOvos());
            Anuncio naoInformado = publicar(vendaDeOvos());
            Anuncio doado = publicar(vendaDeOvos());

            vendido.encerrar(Resultado.VENDIDO, depois);
            naoInformado.encerrar(Resultado.NAO_INFORMADO, depois);

            assertThat(vendido.getResultado()).isEqualTo(Resultado.VENDIDO);
            assertThat(naoInformado.getResultado()).isEqualTo(Resultado.NAO_INFORMADO);
            assertInvalido(() -> doado.encerrar(Resultado.DOADO, depois), "resultado");
        }

        @Test
        void uc08_3_rn09_encerrarDuasVezesEEstadoInvalidoENaoAlteraNada() {
            Anuncio anuncio = publicar(doacaoDeFrutas());
            anuncio.encerrar(Resultado.DOADO, depois);

            assertEstadoInvalido(() -> anuncio.encerrar(Resultado.NAO_INFORMADO, depois.plusSeconds(60)), "ENCERRADO");
            assertThat(anuncio.getEncerradoEm()).isEqualTo(depois);
            assertThat(anuncio.getResultado()).isEqualTo(Resultado.DOADO);
        }

        @Test
        void rn09_encerrarAnuncioVencidoEEstadoInvalido() {
            Anuncio anuncio = publicar(doacaoDeFrutas());

            assertEstadoInvalido(() -> anuncio.encerrar(Resultado.DOADO, anuncio.getExpiraEm()), "ATIVO");
        }
    }

    @Nested
    class ExpirarERemover {

        @Test
        void expirarMudaAnuncioVencidoParaExpiradoSemResultado() {
            Anuncio anuncio = publicar(doacaoDeFrutas());
            Instant vencimento = anuncio.getExpiraEm();

            anuncio.expirar(vencimento);

            assertThat(anuncio.getStatus()).isEqualTo(StatusAnuncio.EXPIRADO);
            assertThat(anuncio.getAtualizadoEm()).isEqualTo(vencimento);
            assertThat(anuncio.getEncerradoEm()).isNull();
            assertThat(anuncio.getResultado()).isNull();
        }

        @Test
        void expirarAntesDoVencimentoEEstadoInvalido() {
            Anuncio anuncio = publicar(doacaoDeFrutas());

            assertEstadoInvalido(() -> anuncio.expirar(AGORA.plusSeconds(60)), "ATIVO");
        }

        @Test
        void rn11_estadosFinaisNuncaVoltamAExpirar() {
            Anuncio encerrado = publicar(doacaoDeFrutas());
            encerrado.encerrar(Resultado.DOADO, AGORA.plusSeconds(60));

            assertEstadoInvalido(() -> encerrado.expirar(encerrado.getExpiraEm()), "ENCERRADO");
        }

        @Test
        void rn11_removerFuncionaAPartirDeAtivoEExpirado() {
            Anuncio ativo = publicar(doacaoDeFrutas());
            Anuncio expirado = publicar(doacaoDeFrutas());
            expirado.expirar(expirado.getExpiraEm());

            ativo.remover(AGORA.plusSeconds(60));
            expirado.remover(expirado.getExpiraEm().plusSeconds(60));

            assertThat(ativo.getStatus()).isEqualTo(StatusAnuncio.REMOVIDO);
            assertThat(ativo.getAtualizadoEm()).isEqualTo(AGORA.plusSeconds(60));
            assertThat(expirado.getStatus()).isEqualTo(StatusAnuncio.REMOVIDO);
        }

        @Test
        void rn11_removerAnuncioEncerradoLimpaResultadoEEncerradoEm() {
            Anuncio anuncio = publicar(doacaoDeFrutas());
            anuncio.encerrar(Resultado.DOADO, AGORA.plusSeconds(60));

            anuncio.remover(AGORA.plusSeconds(120));

            assertThat(anuncio.getStatus()).isEqualTo(StatusAnuncio.REMOVIDO);
            assertThat(anuncio.getResultado()).isNull();
            assertThat(anuncio.getEncerradoEm()).isNull();
        }

        @Test
        void rn11_removerDuasVezesEEstadoInvalido() {
            Anuncio anuncio = publicar(doacaoDeFrutas());
            anuncio.remover(AGORA.plusSeconds(60));

            assertEstadoInvalido(() -> anuncio.remover(AGORA.plusSeconds(120)), "REMOVIDO");
        }

        @Test
        void rn11_anuncioRemovidoNaoPodeSerEncerrado() {
            Anuncio anuncio = publicar(doacaoDeFrutas());
            anuncio.remover(AGORA.plusSeconds(60));

            assertEstadoInvalido(() -> anuncio.encerrar(Resultado.DOADO, AGORA.plusSeconds(120)), "REMOVIDO");
        }
    }

    @Nested
    class Visibilidade {

        @Test
        void rn08_anuncioAtivoDentroDoPrazoDeAnuncianteComAceiteEVisivel() {
            assertThat(publicar(doacaoDeFrutas()).estaVisivel(AGORA, HOJE)).isTrue();
        }

        @Test
        void rn08_anuncioNaoEVisivelNoInstanteEmQueExpira() {
            Anuncio anuncio = publicar(doacaoDeFrutas());

            assertThat(anuncio.estaVisivel(anuncio.getExpiraEm().minusSeconds(1), HOJE.plusDays(2))).isTrue();
            assertThat(anuncio.estaVisivel(anuncio.getExpiraEm(), HOJE.plusDays(3))).isFalse();
        }

        @Test
        void rn08_anuncioEncerradoOuRemovidoNaoEVisivel() {
            Anuncio encerrado = publicar(doacaoDeFrutas());
            encerrado.encerrar(Resultado.DOADO, AGORA);
            Anuncio removido = publicar(doacaoDeFrutas());
            removido.remover(AGORA);

            assertThat(encerrado.estaVisivel(AGORA, HOJE)).isFalse();
            assertThat(removido.estaVisivel(AGORA, HOJE)).isFalse();
        }

        @Test
        void rn08_validadeDoAlimentoVencidaEscondeOAnuncio() {
            Dados dados = doacaoDeFrutas();
            dados.validadeAlimento = HOJE.plusDays(2);
            Anuncio anuncio = publicar(dados);
            ReflectionTestUtils.setField(anuncio, "validadeAlimento", HOJE);

            assertThat(anuncio.estaVisivel(AGORA, HOJE)).isTrue();
            assertThat(anuncio.estaVisivel(AGORA, HOJE.plusDays(1))).isFalse();
        }

        @Test
        void rn08_rn11_anuncioDeUsuarioBloqueadoSomeSemMudarDeStatus() {
            Usuario autor = anunciante();
            Anuncio anuncio = Anuncio.publicar(autor, doacaoDeFrutas().montar(), AGORA, HOJE);

            autor.bloquear();

            assertThat(anuncio.estaVisivel(AGORA, HOJE)).isFalse();
            assertThat(anuncio.getStatus()).isEqualTo(StatusAnuncio.ATIVO);
        }

        @Test
        void rn08_anuncioDeQuemRevogouOAceiteNaoEVisivel() {
            Usuario autor = anunciante();
            Anuncio anuncio = Anuncio.publicar(autor, doacaoDeFrutas().montar(), AGORA, HOJE);

            ReflectionTestUtils.setField(autor, "aceiteContatoEm", null);

            assertThat(anuncio.estaVisivel(AGORA, HOJE)).isFalse();
        }

        @Test
        void rn09_pertenceASomenteAoAutor() {
            Usuario autor = anunciante();
            Anuncio anuncio = Anuncio.publicar(autor, doacaoDeFrutas().montar(), AGORA, HOJE);

            assertThat(anuncio.pertenceA(autor.getId())).isTrue();
            assertThat(anuncio.pertenceA(UUID.randomUUID())).isFalse();
        }
    }

    private static Anuncio publicar(Dados dados) {
        return Anuncio.publicar(anunciante(), dados.montar(), AGORA, HOJE);
    }

    private static void assertInvalido(ThrowingCallable acao, String campo) {
        assertThatThrownBy(acao).isInstanceOfSatisfying(RegraNegocioException.class, erro -> {
            assertThat(erro.getCodigo()).isEqualTo("VALIDACAO");
            assertThat(erro.getCampo()).isEqualTo(campo);
        });
    }

    private static void assertInvalido(ThrowingCallable acao, String campo, String mensagem) {
        assertInvalido(acao, campo);
        assertThatThrownBy(acao).hasMessage(mensagem);
    }

    private static void assertEstadoInvalido(ThrowingCallable acao, String statusAtual) {
        assertThatThrownBy(acao).isInstanceOfSatisfying(EstadoInvalidoException.class,
                erro -> assertThat(erro.getStatusAtual()).isEqualTo(statusAtual));
    }
}
