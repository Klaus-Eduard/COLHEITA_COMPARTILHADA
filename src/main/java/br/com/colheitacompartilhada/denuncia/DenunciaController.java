package br.com.colheitacompartilhada.denuncia;

import java.net.URI;
import java.time.Clock;
import java.util.UUID;

import br.com.colheitacompartilhada.comum.Pagina;
import br.com.colheitacompartilhada.config.ClockConfig;
import br.com.colheitacompartilhada.denuncia.dto.DenunciaCriada;
import br.com.colheitacompartilhada.denuncia.dto.DenunciaDetalhe;
import br.com.colheitacompartilhada.denuncia.dto.DenunciaRequest;
import br.com.colheitacompartilhada.denuncia.dto.DenunciaResumo;
import br.com.colheitacompartilhada.denuncia.dto.ResolucaoRequest;
import br.com.colheitacompartilhada.usuario.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DenunciaController {

    private final DenunciaService denunciaService;
    private final Clock clock;

    public DenunciaController(DenunciaService denunciaService, Clock clock) {
        this.denunciaService = denunciaService;
        this.clock = clock;
    }

    @PostMapping("/anuncios/{id}/denuncias")
    ResponseEntity<DenunciaCriada> denunciar(@PathVariable UUID id, @AuthenticationPrincipal UsuarioAutenticado eu,
            @Valid @RequestBody DenunciaRequest pedido) {
        Denuncia denuncia = denunciaService.abrir(id, eu.id(), pedido.motivo());
        return ResponseEntity.created(URI.create("/admin/denuncias/" + denuncia.getId()))
                .body(DenunciaCriada.de(denuncia));
    }

    @GetMapping("/admin/denuncias")
    Pagina<DenunciaResumo> listar(@AuthenticationPrincipal UsuarioAutenticado eu,
            @RequestParam(defaultValue = "ABERTA") StatusDenuncia status,
            @RequestParam(defaultValue = "0") @PositiveOrZero(message = "Página inválida.") int pagina) {
        return Pagina.de(denunciaService.listar(eu.id(), status, pagina).map(DenunciaResumo::de));
    }

    @GetMapping("/admin/denuncias/{id}")
    DenunciaDetalhe detalhar(@PathVariable UUID id, @AuthenticationPrincipal UsuarioAutenticado eu) {
        return detalhe(denunciaService.detalhar(id, eu.id()));
    }

    @PatchMapping("/admin/denuncias/{id}")
    DenunciaDetalhe resolver(@PathVariable UUID id, @AuthenticationPrincipal UsuarioAutenticado eu,
            @Valid @RequestBody ResolucaoRequest pedido) {
        return detalhe(denunciaService.resolver(id, eu.id(), pedido));
    }

    private DenunciaDetalhe detalhe(Denuncia denuncia) {
        return DenunciaDetalhe.de(denuncia, clock.instant(), ClockConfig.hoje(clock));
    }
}
