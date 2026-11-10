package br.com.colheitacompartilhada.usuario.dto;

import br.com.colheitacompartilhada.usuario.TipoParticipante;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CadastroRequest(
        @NotNull(message = NOME) @Size(min = 2, max = 120, message = NOME) String nome,
        @NotBlank(message = EMAIL) @Email(message = EMAIL) @Size(max = 254, message = EMAIL) String email,
        @NotNull(message = SENHA) @Size(min = 8, max = 72, message = SENHA) String senha,
        @NotNull(message = "Selecione o tipo de participante.") TipoParticipante tipo) {

    private static final String NOME = "Informe seu nome (2 a 120 caracteres).";
    private static final String EMAIL = "Informe um e-mail válido.";
    private static final String SENHA = "A senha deve ter de 8 a 72 caracteres.";

    public CadastroRequest {
        nome = nome == null ? null : nome.trim();
        email = email == null ? null : email.trim();
    }
}
