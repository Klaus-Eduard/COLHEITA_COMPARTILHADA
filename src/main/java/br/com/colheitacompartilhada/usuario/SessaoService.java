package br.com.colheitacompartilhada.usuario;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class SessaoService {

    private final AuthenticationManager authenticationManager;
    private final LoginRateLimiter rateLimiter;
    private final UsuarioService usuarioService;
    private final SecurityContextRepository contextos = new HttpSessionSecurityContextRepository();

    public SessaoService(AuthenticationManager authenticationManager, LoginRateLimiter rateLimiter,
            UsuarioService usuarioService) {
        this.authenticationManager = authenticationManager;
        this.rateLimiter = rateLimiter;
        this.usuarioService = usuarioService;
    }

    public Usuario entrar(String email, String senha, HttpServletRequest request, HttpServletResponse response) {
        rateLimiter.verificar(email);
        Authentication autenticacao = autenticar(email, senha);
        rateLimiter.limpar(email);
        abrirSessao(autenticacao, request, response);
        return usuarioService.buscar(((UsuarioAutenticado) autenticacao.getPrincipal()).id());
    }

    public void sair(HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, null);
        new CookieClearingLogoutHandler("JSESSIONID").logout(request, response, null);
    }

    // Senha errada, e-mail inexistente e conta bloqueada falham do mesmo jeito: quem tenta não descobre qual foi o caso.
    private Authentication autenticar(String email, String senha) {
        try {
            return authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email.trim(), senha));
        } catch (AuthenticationException falha) {
            rateLimiter.registrarFalha(email);
            throw falha;
        }
    }

    private void abrirSessao(Authentication autenticacao, HttpServletRequest request, HttpServletResponse response) {
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacao);
        SecurityContextHolder.setContext(contexto);
        request.getSession(true);
        request.changeSessionId();
        contextos.saveContext(contexto, request, response);
    }
}
