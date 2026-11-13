package br.com.colheitacompartilhada.anuncio;

import java.util.UUID;

import br.com.colheitacompartilhada.anuncio.dto.AnuncioDetalhe;
import br.com.colheitacompartilhada.anuncio.dto.AnuncioResumo;
import br.com.colheitacompartilhada.comum.Pagina;
import br.com.colheitacompartilhada.usuario.UsuarioAutenticado;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnuncioController {

    private final AnuncioService anuncioService;

    public AnuncioController(AnuncioService anuncioService) {
        this.anuncioService = anuncioService;
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
}
