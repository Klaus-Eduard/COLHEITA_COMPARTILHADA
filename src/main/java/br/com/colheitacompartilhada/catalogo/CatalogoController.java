package br.com.colheitacompartilhada.catalogo;

import java.util.List;

import br.com.colheitacompartilhada.catalogo.dto.BairroResponse;
import br.com.colheitacompartilhada.catalogo.dto.CategoriaResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping("/categorias")
    List<CategoriaResponse> categorias() {
        return catalogoService.listarCategorias().stream().map(CategoriaResponse::de).toList();
    }

    @GetMapping("/bairros")
    List<BairroResponse> bairros() {
        return catalogoService.listarBairros().stream().map(BairroResponse::de).toList();
    }
}
