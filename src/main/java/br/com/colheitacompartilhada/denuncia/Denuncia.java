package br.com.colheitacompartilhada.denuncia;

import java.time.Instant;
import java.util.UUID;

import br.com.colheitacompartilhada.anuncio.Anuncio;
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
