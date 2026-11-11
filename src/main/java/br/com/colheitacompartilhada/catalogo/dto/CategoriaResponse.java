package br.com.colheitacompartilhada.catalogo.dto;

import br.com.colheitacompartilhada.catalogo.Categoria;

public record CategoriaResponse(Long id, String nome, boolean origemAnimal) {

    public static CategoriaResponse de(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNome(), categoria.isOrigemAnimal());
    }
}
