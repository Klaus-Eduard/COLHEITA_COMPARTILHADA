package br.com.colheitacompartilhada.config;

import static br.com.colheitacompartilhada.usuario.ContaDeTeste.emailUnico;
import static br.com.colheitacompartilhada.usuario.ContaDeTeste.entrar;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.colheitacompartilhada.IntegracaoTest;
import br.com.colheitacompartilhada.usuario.Papel;
import br.com.colheitacompartilhada.usuario.TipoParticipante;
import br.com.colheitacompartilhada.usuario.Usuario;
import br.com.colheitacompartilhada.usuario.UsuarioRepository;
import br.com.colheitacompartilhada.usuario.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

class AdminBootstrapTest extends IntegracaoTest {

    @Autowired
    UsuarioService usuarioService;

    @Autowired
    UsuarioRepository usuarios;

    @Test
    void criaOAdminQuandoEmailESenhaEstaoDefinidosEEleConsegueEntrar() throws Exception {
        String email = emailUnico();

        new AdminBootstrap(usuarioService, email, "senha-do-admin", "Administrador").run(null);

        Usuario admin = usuarios.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(admin.getPapel()).isEqualTo(Papel.ADMIN);
        assertThat(admin.getTipo()).isEqualTo(TipoParticipante.MORADOR);
        assertThat(admin.isAtivo()).isTrue();
        assertThat(admin.getSenhaHash()).startsWith("{bcrypt}");
        entrar(mvc, new MockHttpSession(), email, "senha-do-admin")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("ADMIN"));
    }

    @Test
    void naoDuplicaNemAlteraQuandoOEmailJaExiste() {
        String email = emailUnico();
        new AdminBootstrap(usuarioService, email, "senha-do-admin", "Administrador").run(null);
        String hashOriginal = usuarios.findByEmailIgnoreCase(email).orElseThrow().getSenhaHash();

        new AdminBootstrap(usuarioService, email, "outra-senha-123", "Outro Nome").run(null);

        assertThat(usuarios.findAll()).filteredOn(u -> u.getEmail().equals(email)).hasSize(1);
        assertThat(usuarios.findByEmailIgnoreCase(email).orElseThrow().getSenhaHash()).isEqualTo(hashOriginal);
    }

    @Test
    void naoFazNadaSemEmailOuSemSenha() {
        long antes = usuarios.count();

        new AdminBootstrap(usuarioService, "", "senha-do-admin", "Administrador").run(null);
        new AdminBootstrap(usuarioService, emailUnico(), "", "Administrador").run(null);

        assertThat(usuarios.count()).isEqualTo(antes);
    }
}
