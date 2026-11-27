package br.com.colheitacompartilhada.denuncia.dto;

import java.time.Instant;
import java.util.UUID;

import br.com.colheitacompartilhada.anuncio.StatusAnuncio;
import br.com.colheitacompartilhada.denuncia.Denuncia;
import br.com.colheitacompartilhada.denuncia.StatusDenuncia;
import br.com.colheitacompartilhada.usuario.Papel;
import br.com.colheitacompartilhada.usuario.Usuario;

/** Item da fila do moderador (04_API #17). */
public record DenunciaResumo(UUID id, String motivo, StatusDenuncia status, Instant criadoEm, Pessoa autor,
        AnuncioDenunciado anuncio, Anunciante anunciante) {

    public record Pessoa(UUID id, String nome) {

        public static Pessoa de(Usuario usuario) {
            return new Pessoa(usuario.getId(), usuario.getNome());
        }
    }

    public record AnuncioDenunciado(UUID id, String titulo, StatusAnuncio status) {
    }

    public record Anunciante(UUID id, String nome, Papel papel, boolean ativo) {

        public static Anunciante de(Usuario usuario) {
            return new Anunciante(usuario.getId(), usuario.getNome(), usuario.getPapel(), usuario.isAtivo());
        }
    }

    public static DenunciaResumo de(Denuncia d) {
        return new DenunciaResumo(d.getId(), d.getMotivo(), d.getStatus(), d.getCriadoEm(),
                Pessoa.de(d.getAutor()),
                new AnuncioDenunciado(d.getAnuncio().getId(), d.getAnuncio().getTitulo(), d.getAnuncio().getStatus()),
                Anunciante.de(d.getAnuncio().getUsuario()));
    }
}
