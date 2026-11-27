package br.com.colheitacompartilhada.denuncia;

import java.net.URI;
import java.util.UUID;

import br.com.colheitacompartilhada.denuncia.dto.DenunciaCriada;
import br.com.colheitacompartilhada.denuncia.dto.DenunciaRequest;
import br.com.colheitacompartilhada.usuario.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DenunciaController {

    private final DenunciaService denunciaService;
    public DenunciaController(DenunciaService denunciaService) {
        this.denunciaService = denunciaService;
    }

    @PostMapping("/anuncios/{id}/denuncias")
    ResponseEntity<DenunciaCriada> denunciar(@PathVariable UUID id, @AuthenticationPrincipal UsuarioAutenticado eu,
            @Valid @RequestBody DenunciaRequest pedido) {
        Denuncia denuncia = denunciaService.abrir(id, eu.id(), pedido.motivo());
        return ResponseEntity.created(URI.create("/admin/denuncias/" + denuncia.getId()))
                .body(DenunciaCriada.de(denuncia));
    }
}
