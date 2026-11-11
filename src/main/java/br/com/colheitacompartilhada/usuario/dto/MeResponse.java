package br.com.colheitacompartilhada.usuario.dto;

import java.time.Instant;
import java.util.UUID;

import br.com.colheitacompartilhada.usuario.Papel;
import br.com.colheitacompartilhada.usuario.TipoParticipante;
import br.com.colheitacompartilhada.usuario.Usuario;

public record MeResponse(UUID id, String nome, String email, TipoParticipante tipo, Papel papel, String whatsapp,
        Instant aceiteContatoEm, boolean podePublicar) {

    public static MeResponse de(Usuario usuario) {
        return new MeResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getTipo(),
                usuario.getPapel(), usuario.getWhatsapp(), usuario.getAceiteContatoEm(), usuario.podePublicar());
    }
}
