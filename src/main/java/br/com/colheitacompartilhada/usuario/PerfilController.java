package br.com.colheitacompartilhada.usuario;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import br.com.colheitacompartilhada.anuncio.AnuncioService;
import br.com.colheitacompartilhada.anuncio.dto.MeuAnuncio;
import br.com.colheitacompartilhada.comum.Pagina;
import br.com.colheitacompartilhada.config.ClockConfig;
import br.com.colheitacompartilhada.usuario.dto.MeResponse;
import br.com.colheitacompartilhada.usuario.dto.PerfilRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PerfilController {

    private final UsuarioService usuarioService;
    private final AnuncioService anuncioService;
    private final Clock clock;

    public PerfilController(UsuarioService usuarioService, AnuncioService anuncioService, Clock clock) {
        this.usuarioService = usuarioService;
        this.anuncioService = anuncioService;
        this.clock = clock;
    }

    @GetMapping("/me")
    MeResponse me(@AuthenticationPrincipal UsuarioAutenticado eu) {
        return MeResponse.de(usuarioService.buscar(eu.id()));
    }

    @PatchMapping("/me")
    MeResponse atualizar(@AuthenticationPrincipal UsuarioAutenticado eu, @Valid @RequestBody PerfilRequest dados) {
        return MeResponse.de(usuarioService.atualizarPerfil(eu.id(), dados));
    }

    @GetMapping("/me/anuncios")
    Pagina<MeuAnuncio> meusAnuncios(@AuthenticationPrincipal UsuarioAutenticado eu,
            @RequestParam(defaultValue = "0") @PositiveOrZero(message = "Página inválida.") int pagina) {
        Instant agora = clock.instant();
        LocalDate hoje = ClockConfig.hoje(clock);
        return Pagina.de(anuncioService.listarDoDono(eu.id(), pagina).map(a -> MeuAnuncio.de(a, agora, hoje)));
    }

    @GetMapping("/me/anuncios/{id}")
    MeuAnuncio meuAnuncio(@AuthenticationPrincipal UsuarioAutenticado eu, @PathVariable UUID id) {
        return MeuAnuncio.de(anuncioService.buscarDoDono(id, eu.id()), clock.instant(), ClockConfig.hoje(clock));
    }
}
