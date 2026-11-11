package br.com.colheitacompartilhada.catalogo.dto;

import br.com.colheitacompartilhada.catalogo.Bairro;

public record BairroResponse(Long id, String nome) {

    public static BairroResponse de(Bairro bairro) {
        return new BairroResponse(bairro.getId(), bairro.getNome());
    }
}
