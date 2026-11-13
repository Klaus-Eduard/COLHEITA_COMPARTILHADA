package br.com.colheitacompartilhada.anuncio;

import java.net.URI;
import java.time.Clock;
import java.util.UUID;

import br.com.colheitacompartilhada.anuncio.dto.AnuncioDetalhe;
import br.com.colheitacompartilhada.anuncio.dto.AnuncioForm;
import br.com.colheitacompartilhada.anuncio.dto.AnuncioResumo;
import br.com.colheitacompartilhada.anuncio.dto.EncerramentoRequest;
import br.com.colheitacompartilhada.anuncio.dto.MeuAnuncio;
import br.com.colheitacompartilhada.comum.Pagina;
import br.com.colheitacompartilhada.config.ClockConfig;
import br.com.colheitacompartilhada.usuario.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AnuncioController {

    private final AnuncioService anuncioService;
    private final Clock clock;

    public AnuncioController(AnuncioService anuncioService, Clock clock) {
        this.anuncioService = anuncioService;
        this.clock = clock;
    }

    @GetMapping("/anuncios")
    Pagina<AnuncioResumo> buscar(
            @RequestParam(required = false) @Size(max = 100, message = "Use até 100 caracteres na busca.") String texto,
            @RequestParam(required = false) Long bairroId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Modalidade modalidade,
            @RequestParam(defaultValue = "0") @PositiveOrZero(message = "Página inválida.") int pagina) {
        FiltroAnuncios filtro = new FiltroAnuncios(texto, bairroId, categoriaId, modalidade);
        return Pagina.de(anuncioService.buscar(filtro, pagina).map(AnuncioResumo::de));
    }

    @GetMapping("/anuncios/{id}")
    AnuncioDetalhe detalhar(@PathVariable UUID id, @AuthenticationPrincipal UsuarioAutenticado eu) {
        Anuncio anuncio = anuncioService.detalhar(id);
        return AnuncioDetalhe.de(anuncio, eu != null && anuncio.pertenceA(eu.id()));
    }

    @PostMapping(path = "/anuncios", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<AnuncioDetalhe> publicar(@AuthenticationPrincipal UsuarioAutenticado eu,
            @Valid @RequestPart("dados") AnuncioForm dados,
            @RequestPart(name = "foto", required = false) MultipartFile foto) {
        Anuncio anuncio = anuncioService.publicar(eu.id(), dados, foto);
        return ResponseEntity.created(URI.create("/anuncios/" + anuncio.getId()))
                .body(AnuncioDetalhe.de(anuncio, true));
    }

    @PatchMapping(path = "/anuncios/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    AnuncioDetalhe editar(@PathVariable UUID id, @AuthenticationPrincipal UsuarioAutenticado eu,
            @Valid @RequestPart("dados") AnuncioForm dados,
            @RequestPart(name = "foto", required = false) MultipartFile foto) {
        return AnuncioDetalhe.de(anuncioService.editar(id, eu.id(), dados, foto), true);
    }

    @PostMapping("/anuncios/{id}/encerramento")
    MeuAnuncio encerrar(@PathVariable UUID id, @AuthenticationPrincipal UsuarioAutenticado eu,
            @Valid @RequestBody EncerramentoRequest pedido) {
        Anuncio anuncio = anuncioService.encerrar(id, eu.id(), pedido.resultado());
        return MeuAnuncio.de(anuncio, clock.instant(), ClockConfig.hoje(clock));
    }
}
