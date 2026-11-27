package br.com.colheitacompartilhada.denuncia.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpo de PATCH /admin/denuncias/{id} (04_API #19). A decisão conta sem os espaços das pontas (RN10.6). */
public record ResolucaoRequest(
        @NotNull(message = "Informe se a denúncia é procedente.") Boolean procedente,
        @NotNull(message = DECISAO) @Size(min = 1, max = 500, message = DECISAO) String decisao,
        boolean bloquearAnunciante) {

    private static final String DECISAO = "Informe a decisão (1 a 500 caracteres).";

    public ResolucaoRequest {
        decisao = decisao == null ? null : decisao.trim();
    }
}
