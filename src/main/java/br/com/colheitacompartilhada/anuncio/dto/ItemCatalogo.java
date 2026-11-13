package br.com.colheitacompartilhada.anuncio.dto;

import br.com.colheitacompartilhada.catalogo.Bairro;
import br.com.colheitacompartilhada.catalogo.Categoria;

public record ItemCatalogo(Long id, String nome) {

    public static ItemCatalogo de(Categoria categoria) {
        return new ItemCatalogo(categoria.getId(), categoria.getNome());
    }

    public static ItemCatalogo de(Bairro bairro) {
        return new ItemCatalogo(bairro.getId(), bairro.getNome());
    }
}
