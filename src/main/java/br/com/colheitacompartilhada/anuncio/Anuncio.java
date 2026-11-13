package br.com.colheitacompartilhada.anuncio;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import br.com.colheitacompartilhada.catalogo.Bairro;
import br.com.colheitacompartilhada.catalogo.Categoria;
import br.com.colheitacompartilhada.comum.EstadoInvalidoException;
import br.com.colheitacompartilhada.comum.RegraNegocioException;
import br.com.colheitacompartilhada.config.ClockConfig;
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

    private static final Duration PRAZO_MAXIMO = Duration.ofDays(7);
    private static final BigDecimal QUANTIDADE_MAXIMA = new BigDecimal("9999999.999");
    private static final BigDecimal PRECO_MAXIMO = new BigDecimal("99999999.99");

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

    public static Anuncio publicar(Usuario autor, DadosAnuncio dados, Instant agora, LocalDate hoje) {
        if (!autor.podePublicar()) {
            throw new RegraNegocioException("PERFIL_SEM_CONTATO", null,
                    "Para publicar, cadastre seu WhatsApp e autorize o contato.");
        }
        validar(dados, hoje);
        Anuncio anuncio = new Anuncio();
        anuncio.usuario = autor;
        anuncio.status = StatusAnuncio.ATIVO;
        anuncio.criadoEm = agora;
        anuncio.atualizadoEm = agora;
        anuncio.expiraEm = anuncio.calcularExpiraEm(dados.disponivelAte());
        anuncio.preencher(dados);
        return anuncio;
    }

    public void editar(DadosAnuncio dados, Instant agora, LocalDate hoje) {
        exigirAtivoENaoVencido(agora);
        validar(dados, hoje);
        Instant novoExpiraEm = calcularExpiraEm(dados.disponivelAte());
        if (novoExpiraEm.isAfter(expiraEm)) {
            throw invalido("disponivelAte", "A edição não pode ampliar o prazo do anúncio.");
        }
        this.expiraEm = novoExpiraEm;
        this.atualizadoEm = agora;
        preencher(dados);
    }

    public void encerrar(Resultado resultado, Instant agora) {
        exigirAtivoENaoVencido(agora);
        if (!resultado.compativelCom(modalidade)) {
            throw invalido("resultado", "O resultado não combina com a modalidade do anúncio.");
        }
        this.status = StatusAnuncio.ENCERRADO;
        this.resultado = resultado;
        this.encerradoEm = agora;
        this.atualizadoEm = agora;
    }

    public void expirar(Instant agora) {
        if (status != StatusAnuncio.ATIVO || expiraEm.isAfter(agora)) {
            throw estadoInvalido("O anúncio não está vencido.");
        }
        this.status = StatusAnuncio.EXPIRADO;
        this.atualizadoEm = agora;
    }

    // RN11: ao remover um anúncio ENCERRADO, resultado e encerradoEm voltam a null (CHECK do banco).
    public void remover(Instant agora) {
        if (status == StatusAnuncio.REMOVIDO) {
            throw estadoInvalido("O anúncio já foi removido.");
        }
        this.status = StatusAnuncio.REMOVIDO;
        this.resultado = null;
        this.encerradoEm = null;
        this.atualizadoEm = agora;
    }

    /** RN08. A mesma regra existe como consulta em AnuncioSpecs.visivel; as duas devem mudar juntas. */
    public boolean estaVisivel(Instant agora, LocalDate hoje) {
        return status == StatusAnuncio.ATIVO
                && expiraEm.isAfter(agora)
                && (validadeAlimento == null || !validadeAlimento.isBefore(hoje))
                && usuario.isAtivo()
                && usuario.getAceiteContatoEm() != null;
    }

    public boolean pertenceA(UUID usuarioId) {
        return usuario.getId().equals(usuarioId);
    }

    private void exigirAtivoENaoVencido(Instant agora) {
        if (status != StatusAnuncio.ATIVO || !expiraEm.isAfter(agora)) {
            throw estadoInvalido("O anúncio não está mais ativo.");
        }
    }

    // RN07: vale o que vier primeiro, o fim do dia escolhido (em São Paulo) ou 7 dias corridos desde a criação.
    private Instant calcularExpiraEm(LocalDate disponivelAte) {
        Instant fimDoDia = disponivelAte.plusDays(1).atStartOfDay(ClockConfig.FUSO_NEGOCIO).toInstant();
        Instant limite = criadoEm.plus(PRAZO_MAXIMO);
        return fimDoDia.isBefore(limite) ? fimDoDia : limite;
    }

    private void preencher(DadosAnuncio dados) {
        this.categoria = dados.categoria();
        this.bairro = dados.bairro();
        this.titulo = dados.titulo().trim();
        this.descricao = dados.descricao().trim();
        this.quantidade = dados.quantidade();
        this.unidade = dados.unidade();
        this.modalidade = dados.modalidade();
        this.preco = dados.preco();
        this.conservacao = dados.conservacao().trim();
        this.validadeAlimento = dados.validadeAlimento();
        this.origemProduto = textoOuNull(dados.origemProduto());
        this.inspecao = textoOuNull(dados.inspecao());
        this.excedenteDeclarado = dados.excedenteDeclarado();
    }

    private static void validar(DadosAnuncio dados, LocalDate hoje) {
        validarPreco(dados);
        validarQuantidade(dados);
        validarOrigemAnimal(dados);
        validarPrazo(dados, hoje);
        if (!dados.excedenteDeclarado()) {
            throw invalido("excedenteDeclarado", "Confirme que é excedente próprio.");
        }
    }

    // RN03
    private static void validarPreco(DadosAnuncio dados) {
        BigDecimal preco = dados.preco();
        if (preco == null) {
            return;
        }
        if (dados.modalidade() == Modalidade.DOACAO) {
            throw invalido("preco", "Doação não tem preço.");
        }
        if (preco.signum() <= 0) {
            throw invalido("preco", "O preço deve ser maior que zero.");
        }
        if (casasDecimais(preco) > 2 || preco.compareTo(PRECO_MAXIMO) > 0) {
            throw invalido("preco", "Use no máximo 2 casas decimais e até 99.999.999,99.");
        }
    }

    // RN06
    private static void validarQuantidade(DadosAnuncio dados) {
        BigDecimal quantidade = dados.quantidade();
        if (quantidade.signum() <= 0) {
            throw invalido("quantidade", "Informe uma quantidade maior que zero.");
        }
        if (casasDecimais(quantidade) > 3 || quantidade.compareTo(QUANTIDADE_MAXIMA) > 0) {
            throw invalido("quantidade", "Use no máximo 3 casas decimais e até 9.999.999,999.");
        }
        if (dados.unidade().isInteira() && casasDecimais(quantidade) > 0) {
            throw invalido("quantidade", "Para esta unidade, use número inteiro.");
        }
    }

    // RN05
    private static void validarOrigemAnimal(DadosAnuncio dados) {
        if (!dados.categoria().isOrigemAnimal()) {
            return;
        }
        if (dados.validadeAlimento() == null) {
            throw invalido("validadeAlimento", "Informe a validade.");
        }
        if (textoOuNull(dados.origemProduto()) == null) {
            throw invalido("origemProduto", "Informe a origem do produto.");
        }
        if (textoOuNull(dados.inspecao()) == null) {
            throw invalido("inspecao", "Informe a identificação da inspeção.");
        }
    }

    // RN07
    private static void validarPrazo(DadosAnuncio dados, LocalDate hoje) {
        LocalDate disponivelAte = dados.disponivelAte();
        LocalDate validade = dados.validadeAlimento();
        if (disponivelAte == null || disponivelAte.isBefore(hoje) || disponivelAte.isAfter(hoje.plusDays(7))) {
            throw invalido("disponivelAte", "Escolha uma data entre hoje e 7 dias.");
        }
        if (validade != null && validade.isBefore(hoje)) {
            throw invalido("validadeAlimento", "A validade já passou.");
        }
        if (validade != null && disponivelAte.isAfter(validade)) {
            throw invalido("disponivelAte", "Não pode passar da validade.");
        }
    }

    private static int casasDecimais(BigDecimal valor) {
        return Math.max(valor.stripTrailingZeros().scale(), 0);
    }

    private static String textoOuNull(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static RegraNegocioException invalido(String campo, String mensagem) {
        return new RegraNegocioException("VALIDACAO", campo, mensagem);
    }

    private EstadoInvalidoException estadoInvalido(String mensagem) {
        return new EstadoInvalidoException(status.name(), mensagem);
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
