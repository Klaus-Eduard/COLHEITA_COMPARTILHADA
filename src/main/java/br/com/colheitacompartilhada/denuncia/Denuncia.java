package br.com.colheitacompartilhada.denuncia;

import java.time.Instant;
import java.util.UUID;

import br.com.colheitacompartilhada.anuncio.Anuncio;
import br.com.colheitacompartilhada.anuncio.StatusAnuncio;
import br.com.colheitacompartilhada.comum.EstadoInvalidoException;
import br.com.colheitacompartilhada.comum.RegraNegocioException;
import br.com.colheitacompartilhada.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "denuncia")
public class Denuncia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "anuncio_id", nullable = false)
    private Anuncio anuncio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderador_id")
    private Usuario moderador;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusDenuncia status;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(length = 500)
    private String decisao;

    @Column(name = "resolvida_em")
    private Instant resolvidaEm;

    protected Denuncia() {
    }

    /** RN10.2 e RN10.5: não se denuncia o próprio anúncio, e denunciar não remove nada. */
    public static Denuncia abrir(Anuncio anuncio, Usuario autor, String motivo, Instant agora) {
        if (anuncio.pertenceA(autor.getId())) {
            throw new RegraNegocioException("DENUNCIA_PROPRIO_ANUNCIO", null,
                    "Você não pode denunciar o seu próprio anúncio.");
        }
        Denuncia denuncia = new Denuncia();
        denuncia.anuncio = anuncio;
        denuncia.autor = autor;
        denuncia.motivo = motivo.trim();
        denuncia.status = StatusDenuncia.ABERTA;
        denuncia.criadoEm = agora;
        return denuncia;
    }

    /**
     * RN10.6, tudo ou nada: primeiro confere todas as condições, depois muda anúncio, anunciante e denúncia.
     * A transação do service garante que, se algo falhar depois, nada é gravado.
     */
    public void resolver(Usuario moderador, boolean procedente, String decisao, boolean bloquearAnunciante,
            Instant agora) {
        if (status != StatusDenuncia.ABERTA) {
            throw new EstadoInvalidoException(status.name(), "Esta denúncia já foi resolvida.");
        }
        Usuario anunciante = anuncio.getUsuario();
        if (bloquearAnunciante) {
            exigirBloqueioPermitido(procedente, anunciante, moderador);
        }
        if (procedente && anuncio.getStatus() != StatusAnuncio.REMOVIDO) {
            anuncio.remover(agora);
        }
        if (bloquearAnunciante) {
            anunciante.bloquear();
        }
        this.moderador = moderador;
        this.decisao = decisao.trim();
        this.status = StatusDenuncia.RESOLVIDA;
        this.resolvidaEm = agora;
    }

    private static void exigirBloqueioPermitido(boolean procedente, Usuario anunciante, Usuario moderador) {
        if (!procedente) {
            throw bloqueioNaoPermitido("Só é possível bloquear o anunciante quando a denúncia é procedente.");
        }
        if (anunciante.isAdmin() || anunciante.getId().equals(moderador.getId())) {
            throw bloqueioNaoPermitido("Não é possível bloquear um administrador.");
        }
    }

    private static RegraNegocioException bloqueioNaoPermitido(String mensagem) {
        return new RegraNegocioException("BLOQUEIO_NAO_PERMITIDO", null, mensagem);
    }

    public UUID getId() {
        return id;
    }

    public Anuncio getAnuncio() {
        return anuncio;
    }

    public Usuario getAutor() {
        return autor;
    }

    public Usuario getModerador() {
        return moderador;
    }

    public String getMotivo() {
        return motivo;
    }

    public StatusDenuncia getStatus() {
        return status;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public String getDecisao() {
        return decisao;
    }

    public Instant getResolvidaEm() {
        return resolvidaEm;
    }
}
