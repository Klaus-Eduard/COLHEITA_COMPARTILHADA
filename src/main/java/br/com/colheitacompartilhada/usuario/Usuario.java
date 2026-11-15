package br.com.colheitacompartilhada.usuario;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import br.com.colheitacompartilhada.comum.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Column(length = 15)
    private String whatsapp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoParticipante tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Papel papel;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "aceite_contato_em")
    private Instant aceiteContatoEm;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected Usuario() {
    }

    public static Usuario criar(String nome, String email, String senhaHash, TipoParticipante tipo, Papel papel,
            Instant agora) {
        Usuario usuario = new Usuario();
        usuario.nome = nome.trim();
        usuario.email = email.trim().toLowerCase(Locale.ROOT);
        usuario.senhaHash = senhaHash;
        usuario.tipo = tipo;
        usuario.papel = papel;
        usuario.ativo = true;
        usuario.criadoEm = agora;
        return usuario;
    }

    /** RN02. Recebe o WhatsApp já normalizado (ou null para remover). */
    public void atualizarPerfil(String nome, TipoParticipante tipo, String whatsapp, boolean aceiteContato,
            Instant agora) {
        if (aceiteContato && whatsapp == null) {
            throw new RegraNegocioException("VALIDACAO", "whatsapp",
                    "Informe o WhatsApp com DDD.");
        }
        boolean numeroMudou = !Objects.equals(this.whatsapp, whatsapp);
        this.nome = nome.trim();
        this.tipo = tipo;
        this.whatsapp = whatsapp;
        this.aceiteContatoEm = novoAceite(aceiteContato, numeroMudou, agora);
    }

    // Trocar o número exige um aceite novo; mantendo o número, a data original do aceite é preservada.
    private Instant novoAceite(boolean aceiteContato, boolean numeroMudou, Instant agora) {
        if (!aceiteContato) {
            return null;
        }
        if (numeroMudou || aceiteContatoEm == null) {
            return agora;
        }
        return aceiteContatoEm;
    }

    public void bloquear() {
        this.ativo = false;
    }

    public boolean podePublicar() {
        return ativo && whatsapp != null && aceiteContatoEm != null;
    }

    public boolean isAdmin() {
        return papel == Papel.ADMIN;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public TipoParticipante getTipo() {
        return tipo;
    }

    public Papel getPapel() {
        return papel;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Instant getAceiteContatoEm() {
        return aceiteContatoEm;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
