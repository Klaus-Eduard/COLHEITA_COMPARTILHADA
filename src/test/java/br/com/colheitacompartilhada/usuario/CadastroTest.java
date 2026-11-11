package br.com.colheitacompartilhada.usuario;

import static br.com.colheitacompartilhada.usuario.ContaDeTeste.cadastrar;
import static br.com.colheitacompartilhada.usuario.ContaDeTeste.cadastroJson;
import static br.com.colheitacompartilhada.usuario.ContaDeTeste.emailUnico;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;

import br.com.colheitacompartilhada.IntegracaoTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** UC01 Cadastrar conta — RF01 [T01]. */
class CadastroTest extends IntegracaoTest {

    @Autowired
    UsuarioRepository usuarios;

    @Test
    void uc01_1_cadastroValidoCriaUsuarioAtivoComSenhaEmBcryptESemExporSenha() throws Exception {
        String email = emailUnico();

        cadastrar(mvc, cadastroJson("Ana Souza", email, "segredo123"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.tipo").value("MORADOR"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist())
                .andExpect(content().string(not(containsString("segredo123"))))
                .andExpect(content().string(not(containsString("{bcrypt}"))));

        Usuario salvo = usuarios.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(salvo.getPapel()).isEqualTo(Papel.USUARIO);
        assertThat(salvo.isAtivo()).isTrue();
        assertThat(salvo.getSenhaHash()).startsWith("{bcrypt}");
    }

    @Test
    void uc01_2_emailRepetidoComOutraCaixaRetorna409EMantemUmaConta() throws Exception {
        String email = emailUnico();
        cadastrar(mvc, cadastroJson("Ana Souza", email, "segredo123")).andExpect(status().isCreated());

        cadastrar(mvc, cadastroJson("Outra Ana", email.toUpperCase(Locale.ROOT), "segredo123"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EMAIL_JA_CADASTRADO"));

        assertThat(usuarios.findAll()).filteredOn(u -> u.getEmail().equals(email)).hasSize(1);
    }

    @Test
    void uc01_2_emailEGravadoEmMinusculas() throws Exception {
        String email = emailUnico();

        cadastrar(mvc, cadastroJson("Ana Souza", email.toUpperCase(Locale.ROOT), "segredo123"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void uc01_3_nomeSenhaEEmailInvalidosRetornam400ComOsTresCampos() throws Exception {
        cadastrar(mvc, cadastroJson("A", "email-invalido", "1234567"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos.nome").value("Informe seu nome (2 a 120 caracteres)."))
                .andExpect(jsonPath("$.campos.senha").value("A senha deve ter de 8 a 72 caracteres."))
                .andExpect(jsonPath("$.campos.email").value("Informe um e-mail válido."));
    }

    @Test
    void uc01_3_nomeSoComEspacosNaoConta() throws Exception {
        cadastrar(mvc, cadastroJson("  A  ", emailUnico(), "segredo123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nome").exists());
    }

    @Test
    void uc01_4_papelEnviadoNoCorpoEIgnorado() throws Exception {
        String email = emailUnico();
        String json = "{\"nome\":\"Ana Souza\",\"email\":\"%s\",\"senha\":\"segredo123\",\"tipo\":\"MORADOR\",\"papel\":\"ADMIN\"}"
                .formatted(email);

        cadastrar(mvc, json).andExpect(status().isCreated());

        assertThat(usuarios.findByEmailIgnoreCase(email).orElseThrow().getPapel()).isEqualTo(Papel.USUARIO);
    }
}
