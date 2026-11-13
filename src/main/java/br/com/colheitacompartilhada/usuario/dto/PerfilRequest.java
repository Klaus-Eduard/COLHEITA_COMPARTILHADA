package br.com.colheitacompartilhada.usuario.dto;

import br.com.colheitacompartilhada.usuario.TipoParticipante;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Formulário completo do perfil (04_API #6). E-mail e senha não fazem parte: não podem ser alterados. */
public record PerfilRequest(
        @NotNull(message = NOME) @Size(min = 2, max = 120, message = NOME) String nome,
        @NotNull(message = "Selecione o tipo de participante.") TipoParticipante tipo,
        String whatsapp,
        boolean aceiteContato) {

    private static final String NOME = "Informe seu nome (2 a 120 caracteres).";

    public PerfilRequest {
        nome = nome == null ? null : nome.trim();
    }
}
