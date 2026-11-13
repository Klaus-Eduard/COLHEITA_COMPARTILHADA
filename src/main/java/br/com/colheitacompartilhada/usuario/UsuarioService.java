package br.com.colheitacompartilhada.usuario;

import java.time.Clock;
import java.util.UUID;

import br.com.colheitacompartilhada.comum.ConflitoException;
import br.com.colheitacompartilhada.comum.NaoEncontradoException;
import br.com.colheitacompartilhada.usuario.dto.CadastroRequest;
import br.com.colheitacompartilhada.usuario.dto.PerfilRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, Clock clock) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public Usuario cadastrar(CadastroRequest dados) {
        if (usuarios.existsByEmailIgnoreCase(dados.email())) {
            throw new ConflitoException("EMAIL_JA_CADASTRADO", "E-mail já cadastrado.");
        }
        return criar(dados.nome(), dados.email(), dados.senha(), dados.tipo(), Papel.USUARIO);
    }

    @Transactional
    public boolean criarAdminSeNaoExistir(String nome, String email, String senha) {
        if (usuarios.existsByEmailIgnoreCase(email.trim())) {
            return false;
        }
        criar(nome, email, senha, TipoParticipante.MORADOR, Papel.ADMIN);
        return true;
    }

    @Transactional
    public Usuario atualizarPerfil(UUID id, PerfilRequest dados) {
        Usuario usuario = buscar(id);
        String whatsapp = Whatsapp.normalizar(dados.whatsapp());
        usuario.atualizarPerfil(dados.nome(), dados.tipo(), whatsapp, dados.aceiteContato(), clock.instant());
        return usuario;
    }

    @Transactional(readOnly = true)
    public Usuario buscar(UUID id) {
        return usuarios.findById(id).orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado."));
    }

    private Usuario criar(String nome, String email, String senha, TipoParticipante tipo, Papel papel) {
        String senhaHash = passwordEncoder.encode(senha);
        return usuarios.save(Usuario.criar(nome, email, senhaHash, tipo, papel, clock.instant()));
    }
}
