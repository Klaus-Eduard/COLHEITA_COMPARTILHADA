package br.com.colheitacompartilhada.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Informe o e-mail.") @Size(max = 254, message = "Informe um e-mail válido.") String email,
        @NotBlank(message = "Informe a senha.") String senha) {
}
