package br.com.colheitacompartilhada.denuncia.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import br.com.colheitacompartilhada.anuncio.dto.MeuAnuncio;
import br.com.colheitacompartilhada.denuncia.Denuncia;
import br.com.colheitacompartilhada.denuncia.StatusDenuncia;
import br.com.colheitacompartilhada.denuncia.dto.DenunciaResumo.AnuncioDenunciado;
import br.com.colheitacompartilhada.denuncia.dto.DenunciaResumo.Pessoa;
import br.com.colheitacompartilhada.usuario.Papel;
import br.com.colheitacompartilhada.usuario.Usuario;

/** Denúncia aberta pelo moderador (04_API #18): o anúncio aparece completo, mesmo removido. */
public record DenunciaDetalhe(UUID id, String motivo, StatusDenuncia status, Instant criadoEm, Pessoa autor,
        AnuncioDenunciado anuncio, Anunciante anunciante, MeuAnuncio anuncioDetalhe, String decisao,
        Pessoa moderador, Instant resolvidaEm) {

    /** Aqui o anunciante inclui o e-mail, que a fila não mostra. */
    public record Anunciante(UUID id, String nome, String email, Papel papel, boolean ativo) {

        static Anunciante de(Usuario usuario) {
            return new Anunciante(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPapel(),
                    usuario.isAtivo());
        }
    }

    public static DenunciaDetalhe de(Denuncia d, Instant agora, LocalDate hoje) {
        var anuncio = d.getAnuncio();
        return new DenunciaDetalhe(d.getId(), d.getMotivo(), d.getStatus(), d.getCriadoEm(), Pessoa.de(d.getAutor()),
                new AnuncioDenunciado(anuncio.getId(), anuncio.getTitulo(), anuncio.getStatus()),
                Anunciante.de(anuncio.getUsuario()), MeuAnuncio.de(anuncio, agora, hoje), d.getDecisao(),
                d.getModerador() == null ? null : Pessoa.de(d.getModerador()), d.getResolvidaEm());
    }
}
