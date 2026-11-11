package br.com.colheitacompartilhada.anuncio;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "foto")
public class Foto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "anuncio_id", nullable = false)
    private UUID anuncioId;

    @Column(nullable = false, length = 10)
    private String tipo;

    @Column(nullable = false)
    private int tamanho;

    @Column(nullable = false)
    private byte[] conteudo;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected Foto() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getAnuncioId() {
        return anuncioId;
    }

    public String getTipo() {
        return tipo;
    }

    public int getTamanho() {
        return tamanho;
    }

    public byte[] getConteudo() {
        return conteudo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
