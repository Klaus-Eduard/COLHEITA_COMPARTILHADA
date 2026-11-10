package br.com.colheitacompartilhada.usuario.dto;

import java.util.UUID;

import br.com.colheitacompartilhada.usuario.TipoParticipante;
import br.com.colheitacompartilhada.usuario.Usuario;

public record UsuarioCriadoResponse(UUID id, String nome, String email, TipoParticipante tipo) {

    public static UsuarioCriadoResponse de(Usuario usuario) {
        return new UsuarioCriadoResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getTipo());
    }
}
