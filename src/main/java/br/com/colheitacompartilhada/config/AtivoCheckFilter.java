package br.com.colheitacompartilhada.config;

import java.io.IOException;

import br.com.colheitacompartilhada.usuario.UsuarioAutenticado;
import br.com.colheitacompartilhada.usuario.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** RN13: em toda requisição autenticada, confere se o usuário continua ativo; se foi bloqueado, encerra a sessão. */
public class AtivoCheckFilter extends OncePerRequestFilter {

    private final UsuarioRepository usuarios;
    private final ErroJsonWriter erros;

    public AtivoCheckFilter(UsuarioRepository usuarios, ErroJsonWriter erros) {
        this.usuarios = usuarios;
        this.erros = erros;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (usuarioFoiBloqueado()) {
            encerrarSessao(request);
            erros.naoAutenticado(response);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean usuarioFoiBloqueado() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return autenticacao != null
                && autenticacao.getPrincipal() instanceof UsuarioAutenticado usuario
                && !usuarios.existsByIdAndAtivoTrue(usuario.id());
    }

    private void encerrarSessao(HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        if (sessao != null) {
            sessao.invalidate();
        }
        SecurityContextHolder.clearContext();
    }
}
