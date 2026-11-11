package br.com.colheitacompartilhada.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class UsuarioTest {

    private static final Instant AGORA = Instant.parse("2026-10-20T15:00:00Z");

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
}
