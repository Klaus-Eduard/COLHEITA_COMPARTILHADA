package br.com.colheitacompartilhada.anuncio;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import br.com.colheitacompartilhada.catalogo.Bairro;
import br.com.colheitacompartilhada.catalogo.Categoria;
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
@Table(name = "anuncio")
public class Anuncio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bairro_id", nullable = false)
    private Bairro bairro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false, length = 100)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Unidade unidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Modalidade modalidade;

    @Column(precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusAnuncio status;

    @Column(name = "validade_alimento")
    private LocalDate validadeAlimento;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Column(name = "encerrado_em")
    private Instant encerradoEm;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Resultado resultado;

    @Column(nullable = false, length = 300)
    private String conservacao;

    @Column(name = "origem_produto", length = 200)
    private String origemProduto;

    @Column(length = 200)
    private String inspecao;

    @Column(name = "excedente_declarado", nullable = false)
    private boolean excedenteDeclarado;

    protected Anuncio() {
    }

    public UUID getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Bairro getBairro() {
        return bairro;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public Unidade getUnidade() {
        return unidade;
    }

    public Modalidade getModalidade() {
        return modalidade;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public StatusAnuncio getStatus() {
        return status;
    }

    public LocalDate getValidadeAlimento() {
        return validadeAlimento;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public Instant getEncerradoEm() {
        return encerradoEm;
    }

    public Resultado getResultado() {
        return resultado;
    }

    public String getConservacao() {
        return conservacao;
    }

    public String getOrigemProduto() {
        return origemProduto;
    }

    public String getInspecao() {
        return inspecao;
    }

    public boolean isExcedenteDeclarado() {
        return excedenteDeclarado;
    }
}
