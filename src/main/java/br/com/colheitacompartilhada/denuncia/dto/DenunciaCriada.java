package br.com.colheitacompartilhada.denuncia.dto;

import java.time.Instant;
import java.util.UUID;

import br.com.colheitacompartilhada.denuncia.Denuncia;
import br.com.colheitacompartilhada.denuncia.StatusDenuncia;

public record DenunciaCriada(UUID id, StatusDenuncia status, Instant criadoEm) {

    public static DenunciaCriada de(Denuncia denuncia) {
        return new DenunciaCriada(denuncia.getId(), denuncia.getStatus(), denuncia.getCriadoEm());
    }
}
