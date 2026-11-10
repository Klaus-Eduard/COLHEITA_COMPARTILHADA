package br.com.colheitacompartilhada.usuario;

import br.com.colheitacompartilhada.usuario.dto.MeResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PerfilController {

    private final UsuarioService usuarioService;

    public PerfilController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/me")
    MeResponse me(@AuthenticationPrincipal UsuarioAutenticado eu) {
        return MeResponse.de(usuarioService.buscar(eu.id()));
    }
}
