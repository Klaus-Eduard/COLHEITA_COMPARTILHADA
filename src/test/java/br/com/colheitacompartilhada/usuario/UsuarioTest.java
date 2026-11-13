package br.com.colheitacompartilhada.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import br.com.colheitacompartilhada.comum.RegraNegocioException;
import org.junit.jupiter.api.Test;

class UsuarioTest {

    private static final Instant AGORA = Instant.parse("2026-10-20T15:00:00Z");
    private static final Instant DEPOIS = AGORA.plusSeconds(3600);

    @Test
    void criarNasceAtivoComEmailEmMinusculasENomeSemEspacosNasPontas() {
        Usuario usuario = Usuario.criar("  Ana Souza ", " ANA@Exemplo.com ", "{bcrypt}x", TipoParticipante.MORADOR,
                Papel.USUARIO, AGORA);

        assertThat(usuario.getNome()).isEqualTo("Ana Souza");
        assertThat(usuario.getEmail()).isEqualTo("ana@exemplo.com");
        assertThat(usuario.isAtivo()).isTrue();
        assertThat(usuario.isAdmin()).isFalse();
        assertThat(usuario.getCriadoEm()).isEqualTo(AGORA);
    }

    @Test
    void usuarioSemWhatsappNemAceiteNaoPodePublicar() {
        Usuario usuario = Usuario.criar("Ana", "ana@x.com", "{bcrypt}x", TipoParticipante.MORADOR, Papel.USUARIO, AGORA);

        assertThat(usuario.podePublicar()).isFalse();
    }

    @Test
    void bloquearDesativaEEIdempotente() {
        Usuario usuario = Usuario.criar("Ana", "ana@x.com", "{bcrypt}x", TipoParticipante.MORADOR, Papel.ADMIN, AGORA);

        usuario.bloquear();
        usuario.bloquear();

        assertThat(usuario.isAtivo()).isFalse();
        assertThat(usuario.isAdmin()).isTrue();
    }

    @Test
    void rn02_aceiteComNumeroGravaADataDoAceiteELiberaAPublicacao() {
        Usuario usuario = usuario();

        usuario.atualizarPerfil(" Ana Lima ", TipoParticipante.PEQUENO_PRODUTOR, "5547999998888", true, AGORA);

        assertThat(usuario.getNome()).isEqualTo("Ana Lima");
        assertThat(usuario.getTipo()).isEqualTo(TipoParticipante.PEQUENO_PRODUTOR);
        assertThat(usuario.getWhatsapp()).isEqualTo("5547999998888");
        assertThat(usuario.getAceiteContatoEm()).isEqualTo(AGORA);
        assertThat(usuario.podePublicar()).isTrue();
    }

    @Test
    void rn02_2_aceiteSemNumeroERecusadoNoCampoWhatsapp() {
        Usuario usuario = usuario();

        assertThatThrownBy(() -> usuario.atualizarPerfil("Ana", TipoParticipante.MORADOR, null, true, AGORA))
                .isInstanceOfSatisfying(RegraNegocioException.class,
                        erro -> assertThat(erro.getCampo()).isEqualTo("whatsapp"));
        assertThat(usuario.getNome()).isEqualTo("Ana");
    }

    @Test
    void rn02_3_numeroAlteradoComAceiteRenovaADataDoAceite() {
        Usuario usuario = comAceite();

        usuario.atualizarPerfil("Ana", TipoParticipante.MORADOR, "5547988887777", true, DEPOIS);

        assertThat(usuario.getAceiteContatoEm()).isEqualTo(DEPOIS);
    }

    @Test
    void rn02_3_numeroAlteradoSemAceiteApagaOAceite() {
        Usuario usuario = comAceite();

        usuario.atualizarPerfil("Ana", TipoParticipante.MORADOR, "5547988887777", false, DEPOIS);

        assertThat(usuario.getWhatsapp()).isEqualTo("5547988887777");
        assertThat(usuario.getAceiteContatoEm()).isNull();
        assertThat(usuario.podePublicar()).isFalse();
    }

    @Test
    void rn02_4_mesmoNumeroComAceiteMantemADataOriginal() {
        Usuario usuario = comAceite();

        usuario.atualizarPerfil("Ana Lima", TipoParticipante.MORADOR, "5547999998888", true, DEPOIS);

        assertThat(usuario.getAceiteContatoEm()).isEqualTo(AGORA);
    }

    @Test
    void rn02_4_mesmoNumeroSemAceiteAnteriorGravaAgora() {
        Usuario usuario = usuario();
        usuario.atualizarPerfil("Ana", TipoParticipante.MORADOR, "5547999998888", false, AGORA);

        usuario.atualizarPerfil("Ana", TipoParticipante.MORADOR, "5547999998888", true, DEPOIS);

        assertThat(usuario.getAceiteContatoEm()).isEqualTo(DEPOIS);
    }

    @Test
    void rn02_5_revogarOAceiteMantemONumero() {
        Usuario usuario = comAceite();

        usuario.atualizarPerfil("Ana", TipoParticipante.MORADOR, "5547999998888", false, DEPOIS);

        assertThat(usuario.getWhatsapp()).isEqualTo("5547999998888");
        assertThat(usuario.getAceiteContatoEm()).isNull();
    }

    @Test
    void rn02_6_removerONumeroRemoveOAceite() {
        Usuario usuario = comAceite();

        usuario.atualizarPerfil("Ana", TipoParticipante.MORADOR, null, false, DEPOIS);

        assertThat(usuario.getWhatsapp()).isNull();
        assertThat(usuario.getAceiteContatoEm()).isNull();
    }

    private static Usuario usuario() {
        return Usuario.criar("Ana", "ana@x.com", "{bcrypt}x", TipoParticipante.MORADOR, Papel.USUARIO, AGORA);
    }

    private static Usuario comAceite() {
        Usuario usuario = usuario();
        usuario.atualizarPerfil("Ana", TipoParticipante.MORADOR, "5547999998888", true, AGORA);
        return usuario;
    }
}
