package br.com.colheitacompartilhada.anuncio.dto;

import br.com.colheitacompartilhada.anuncio.Resultado;
import jakarta.validation.constraints.NotNull;

public record EncerramentoRequest(@NotNull(message = "Selecione o resultado.") Resultado resultado) {
}
