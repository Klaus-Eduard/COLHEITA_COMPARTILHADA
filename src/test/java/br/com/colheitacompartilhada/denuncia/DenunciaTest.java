package br.com.colheitacompartilhada.denuncia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import br.com.colheitacompartilhada.anuncio.Anuncio;
import br.com.colheitacompartilhada.anuncio.DadosAnuncio;
import br.com.colheitacompartilhada.anuncio.Modalidade;
import br.com.colheitacompartilhada.anuncio.Resultado;
import br.com.colheitacompartilhada.anuncio.StatusAnuncio;
import br.com.colheitacompartilhada.anuncio.Unidade;
import br.com.colheitacompartilhada.catalogo.Bairro;
import br.com.colheitacompartilhada.catalogo.Categoria;
import br.com.colheitacompartilhada.comum.EstadoInvalidoException;
import br.com.colheitacompartilhada.comum.RegraNegocioException;
import br.com.colheitacompartilhada.usuario.Papel;
import br.com.colheitacompartilhada.usuario.TipoParticipante;
import br.com.colheitacompartilhada.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

/** RN10 e RN11 na entidade (UC10, UC11, UC12). Datas fixas: o relógio é sempre passado como argumento. */
class DenunciaTest {

    private static final Instant AGORA = Instant.parse("2026-10-20T15:00:00Z");
    private static final LocalDate HOJE = LocalDate.parse("2026-10-20");

    private Usuario anunciante;
    private Usuario autor;
    private Usuario moderador;
    private Anuncio anuncio;

    @BeforeEach
    void preparar() {
        anunciante = usuario("Ana", Papel.USUARIO);
        autor = usuario("Bruno", Papel.USUARIO);
        moderador = usuario("Moderadora", Papel.ADMIN);
        anuncio = Anuncio.publicar(anunciante, dadosDeDoacao(), AGORA, HOJE);
    }

    @Test
    void uc10_abrirCriaDenunciaAbertaSemRemoverOAnuncio() {
        Denuncia denuncia = Denuncia.abrir(anuncio, autor, "  Produto industrializado, fora do objetivo.  ", AGORA);

        assertThat(denuncia.getStatus()).isEqualTo(StatusDenuncia.ABERTA);
        assertThat(denuncia.getMotivo()).isEqualTo("Produto industrializado, fora do objetivo.");
        assertThat(denuncia.getCriadoEm()).isEqualTo(AGORA);
        assertThat(denuncia.getModerador()).isNull();
        assertThat(denuncia.getDecisao()).isNull();
        assertThat(anuncio.getStatus()).isEqualTo(StatusAnuncio.ATIVO);
    }

    @Test
    void uc10_1_denunciarOProprioAnuncioFalhaComDenunciaProprioAnuncio() {
        assertThatThrownBy(() -> Denuncia.abrir(anuncio, anunciante, "Motivo com tamanho suficiente.", AGORA))
                .isInstanceOfSatisfying(RegraNegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("DENUNCIA_PROPRIO_ANUNCIO"));
    }

    @Test
    void uc11_2_improcedenteResolveSemMudarOAnuncio() {
        Denuncia denuncia = denunciaAberta();

        denuncia.resolver(moderador, false, "  Sem problema.  ", false, AGORA.plusSeconds(60));

        assertThat(denuncia.getStatus()).isEqualTo(StatusDenuncia.RESOLVIDA);
        assertThat(denuncia.getModerador()).isSameAs(moderador);
        assertThat(denuncia.getDecisao()).isEqualTo("Sem problema.");
        assertThat(denuncia.getResolvidaEm()).isEqualTo(AGORA.plusSeconds(60));
        assertThat(anuncio.getStatus()).isEqualTo(StatusAnuncio.ATIVO);
        assertThat(anunciante.isAtivo()).isTrue();
    }

    @Test
    void uc11_3_procedenteRemoveOAnuncioAtivo() {
        Denuncia denuncia = denunciaAberta();

        denuncia.resolver(moderador, true, "Produto industrializado.", false, AGORA);

        assertThat(anuncio.getStatus()).isEqualTo(StatusAnuncio.REMOVIDO);
        assertThat(denuncia.getStatus()).isEqualTo(StatusDenuncia.RESOLVIDA);
        assertThat(anunciante.isAtivo()).isTrue();
    }

    @Test
    void uc11_4_procedenteEmAnuncioEncerradoRemoveELimpaResultadoEDataDeEncerramento() {
        Denuncia denuncia = denunciaAberta();
        anuncio.encerrar(Resultado.DOADO, AGORA.plusSeconds(30));

        denuncia.resolver(moderador, true, "Produto industrializado.", false, AGORA.plusSeconds(60));

        assertThat(anuncio.getStatus()).isEqualTo(StatusAnuncio.REMOVIDO);
        assertThat(anuncio.getResultado()).isNull();
        assertThat(anuncio.getEncerradoEm()).isNull();
    }

    @Test
    void rn10_6_procedenteEmAnuncioJaRemovidoSoFechaADenuncia() {
        Denuncia denuncia = denunciaAberta();
        anuncio.remover(AGORA);

        denuncia.resolver(moderador, true, "Já removido por outra denúncia.", false, AGORA);

        assertThat(anuncio.getStatus()).isEqualTo(StatusAnuncio.REMOVIDO);
        assertThat(denuncia.getStatus()).isEqualTo(StatusDenuncia.RESOLVIDA);
    }

    @Test
    void uc11_5_resolverDenunciaJaResolvidaFalhaComEstadoInvalido() {
        Denuncia denuncia = denunciaAberta();
        denuncia.resolver(moderador, false, "Sem problema.", false, AGORA);

        assertThatThrownBy(() -> denuncia.resolver(moderador, true, "Outra decisão.", false, AGORA))
                .isInstanceOfSatisfying(EstadoInvalidoException.class,
                        e -> assertThat(e.getStatusAtual()).isEqualTo("RESOLVIDA"));
        assertThat(anuncio.getStatus()).isEqualTo(StatusAnuncio.ATIVO);
    }

    @Test
    void uc12_1_procedenteComBloqueioDesativaOAnuncianteSemMudarOStatusDoAnuncio() {
        Denuncia denuncia = denunciaAberta();

        denuncia.resolver(moderador, true, "Reincidente.", true, AGORA);

        assertThat(anunciante.isAtivo()).isFalse();
        assertThat(denuncia.getStatus()).isEqualTo(StatusDenuncia.RESOLVIDA);
    }

    @Test
    void uc12_3_bloquearSemSerProcedenteFalhaENadaMuda() {
        Denuncia denuncia = denunciaAberta();

        assertThatThrownBy(() -> denuncia.resolver(moderador, false, "Sem problema.", true, AGORA))
                .isInstanceOfSatisfying(RegraNegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("BLOQUEIO_NAO_PERMITIDO"));
        assertNadaMudou(denuncia);
    }

    @Test
    void uc12_3_bloquearUmAdminFalhaENadaMuda() {
        Usuario outroAdmin = usuario("Outro admin", Papel.ADMIN);
        Anuncio doAdmin = Anuncio.publicar(outroAdmin, dadosDeDoacao(), AGORA, HOJE);
        Denuncia denuncia = Denuncia.abrir(doAdmin, autor, "Motivo com tamanho suficiente.", AGORA);

        assertThatThrownBy(() -> denuncia.resolver(moderador, true, "Decisão.", true, AGORA))
                .isInstanceOfSatisfying(RegraNegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("BLOQUEIO_NAO_PERMITIDO"));
        assertThat(doAdmin.getStatus()).isEqualTo(StatusAnuncio.ATIVO);
        assertThat(outroAdmin.isAtivo()).isTrue();
        assertThat(denuncia.getStatus()).isEqualTo(StatusDenuncia.ABERTA);
    }

    @Test
    void uc12_4_bloquearQuemJaEstaBloqueadoNaoDaErro() {
        anunciante.bloquear();
        Denuncia denuncia = denunciaAberta();

        denuncia.resolver(moderador, true, "Reincidente.", true, AGORA);

        assertThat(anunciante.isAtivo()).isFalse();
        assertThat(denuncia.getStatus()).isEqualTo(StatusDenuncia.RESOLVIDA);
    }

    private void assertNadaMudou(Denuncia denuncia) {
        assertThat(denuncia.getStatus()).isEqualTo(StatusDenuncia.ABERTA);
        assertThat(denuncia.getModerador()).isNull();
        assertThat(anuncio.getStatus()).isEqualTo(StatusAnuncio.ATIVO);
        assertThat(anunciante.isAtivo()).isTrue();
    }

    private Denuncia denunciaAberta() {
        return Denuncia.abrir(anuncio, autor, "Motivo com tamanho suficiente.", AGORA);
    }

    private static Usuario usuario(String nome, Papel papel) {
        Usuario usuario = Usuario.criar(nome, nome.toLowerCase().replace(' ', '.') + "@exemplo.com", "{bcrypt}x",
                TipoParticipante.MORADOR, papel, AGORA.minusSeconds(86400));
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
        usuario.atualizarPerfil(nome, TipoParticipante.MORADOR, "5547999998888", true, AGORA.minusSeconds(3600));
        return usuario;
    }

    private static DadosAnuncio dadosDeDoacao() {
        Categoria categoria = BeanUtils.instantiateClass(Categoria.class);
        ReflectionTestUtils.setField(categoria, "id", 1L);
        ReflectionTestUtils.setField(categoria, "nome", "Frutas");
        ReflectionTestUtils.setField(categoria, "origemAnimal", false);
        Bairro bairro = BeanUtils.instantiateClass(Bairro.class);
        ReflectionTestUtils.setField(bairro, "id", 5L);
        ReflectionTestUtils.setField(bairro, "nome", "Centro");
        return new DadosAnuncio(categoria, bairro, "Laranjas do quintal", "Laranjas colhidas hoje, sem agrotóxico.",
                new BigDecimal("5"), Unidade.KG, Modalidade.DOACAO, null, "Manter em local fresco", null, null, null,
                HOJE.plusDays(2), true);
    }
}
