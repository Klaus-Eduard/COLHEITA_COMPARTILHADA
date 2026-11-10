package br.com.colheitacompartilhada.usuario;

import br.com.colheitacompartilhada.usuario.dto.CadastroRequest;
import br.com.colheitacompartilhada.usuario.dto.CsrfResponse;
import br.com.colheitacompartilhada.usuario.dto.LoginRequest;
import br.com.colheitacompartilhada.usuario.dto.MeResponse;
import br.com.colheitacompartilhada.usuario.dto.UsuarioCriadoResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final UsuarioService usuarioService;
    private final SessaoService sessaoService;

    public AuthController(UsuarioService usuarioService, SessaoService sessaoService) {
        this.usuarioService = usuarioService;
        this.sessaoService = sessaoService;
    }

    @GetMapping("/csrf")
    CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getToken(), token.getHeaderName());
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    UsuarioCriadoResponse cadastrar(@Valid @RequestBody CadastroRequest dados) {
        return UsuarioCriadoResponse.de(usuarioService.cadastrar(dados));
    }

    @PostMapping("/sessao")
    MeResponse entrar(@Valid @RequestBody LoginRequest dados, HttpServletRequest request,
            HttpServletResponse response) {
        return MeResponse.de(sessaoService.entrar(dados.email(), dados.senha(), request, response));
    }

    @DeleteMapping("/sessao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void sair(HttpServletRequest request, HttpServletResponse response) {
        sessaoService.sair(request, response);
    }
}
