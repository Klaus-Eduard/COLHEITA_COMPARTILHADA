package br.com.colheitacompartilhada.catalogo;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogoService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final CategoriaRepository categorias;
    private final BairroRepository bairros;

    public CatalogoService(CategoriaRepository categorias, BairroRepository bairros) {
        this.categorias = categorias;
        this.bairros = bairros;
    }

    public List<Categoria> listarCategorias() {
        return categorias.findAll(Sort.by("id"));
    }

    // Ordena no Java, em português: no banco, a posição de "Água Verde" dependeria da collation de cada ambiente.
    public List<Bairro> listarBairros() {
        Collator ordemAlfabetica = Collator.getInstance(PT_BR);
        return bairros.findAll().stream()
                .sorted(Comparator.comparing(Bairro::getNome, ordemAlfabetica))
                .toList();
    }
}
