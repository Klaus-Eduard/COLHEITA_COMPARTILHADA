package br.com.colheitacompartilhada.denuncia.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpo de POST /anuncios/{id}/denuncias (04_API #16). O tamanho conta sem os espaços das pontas (RN10.4). */
public record DenunciaRequest(
        @NotNull(message = MOTIVO) @Size(min = 10, max = 500, message = MOTIVO) String motivo) {

    private static final String MOTIVO = "Descreva o motivo (10 a 500 caracteres).";

    public DenunciaRequest {
        motivo = motivo == null ? null : motivo.trim();
    }
}
