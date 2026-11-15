package br.com.colheitacompartilhada.anuncio;

import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FotoController {

    // Pode ser longo e imutável: uma foto nova sempre recebe um id novo.
    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();

    private final FotoService fotoService;

    public FotoController(FotoService fotoService) {
        this.fotoService = fotoService;
    }

    @GetMapping("/fotos/{id}")
    ResponseEntity<byte[]> foto(@PathVariable String id) {
        Foto foto = fotoService.buscar(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.getTipo()))
                .contentLength(foto.getTamanho())
                .cacheControl(CACHE)
                .body(foto.getConteudo());
    }
}
