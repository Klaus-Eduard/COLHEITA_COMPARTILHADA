package br.com.colheitacompartilhada.config;

import br.com.colheitacompartilhada.usuario.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** E07: o primeiro ADMIN nasce das variáveis de ambiente ADMIN_EMAIL e ADMIN_PASSWORD, nunca do formulário público. */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UsuarioService usuarioService;
    private final String email;
    private final String senha;
    private final String nome;

    public AdminBootstrap(UsuarioService usuarioService,
            @Value("${app.admin.email}") String email,
            @Value("${app.admin.senha}") String senha,
            @Value("${app.admin.nome}") String nome) {
        this.usuarioService = usuarioService;
        this.email = email;
        this.senha = senha;
        this.nome = nome;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || senha.isBlank()) {
            return;
        }
        if (usuarioService.criarAdminSeNaoExistir(nome, email, senha)) {
            log.info("Administrador inicial criado");
        }
    }
}
