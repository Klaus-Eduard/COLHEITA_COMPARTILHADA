package br.com.colheitacompartilhada.anuncio;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import br.com.colheitacompartilhada.RelogioAjustavel;
import br.com.colheitacompartilhada.catalogo.Bairro;
import br.com.colheitacompartilhada.catalogo.BairroRepository;
import br.com.colheitacompartilhada.catalogo.Categoria;
import br.com.colheitacompartilhada.catalogo.CategoriaRepository;
import br.com.colheitacompartilhada.config.ClockConfig;
import br.com.colheitacompartilhada.usuario.ContaDeTeste;
import br.com.colheitacompartilhada.usuario.Usuario;
import br.com.colheitacompartilhada.usuario.UsuarioRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Monta dados para os testes de integração de anúncios. Estados que a API não produz de imediato
 * (prazo ou validade vencidos, usuário bloqueado) são gravados direto por SQL.
 */
@Component
public class CenarioAnuncios {

    public static final long FRUTAS = 1L;
    public static final long OVOS = 3L;

    private final MockMvc mvc;
    private final JdbcTemplate jdbc;
    private final UsuarioRepository usuarios;
    private final AnuncioRepository anuncios;
    private final CategoriaRepository categorias;
    private final BairroRepository bairros;
    private final RelogioAjustavel relogio;

    public CenarioAnuncios(MockMvc mvc, JdbcTemplate jdbc, UsuarioRepository usuarios, AnuncioRepository anuncios,
            CategoriaRepository categorias, BairroRepository bairros, RelogioAjustavel relogio) {
        this.mvc = mvc;
        this.jdbc = jdbc;
        this.usuarios = usuarios;
        this.anuncios = anuncios;
        this.categorias = categorias;
        this.bairros = bairros;
        this.relogio = relogio;
    }

    public void limparAnuncios() {
        jdbc.update("delete from denuncia");
        jdbc.update("delete from foto");
        jdbc.update("delete from anuncio");
    }

    /** Usuário que cadastrou WhatsApp e autorizou o contato pelo próprio PATCH /me (UC03). */
    public Usuario anunciante() throws Exception {
        String email = ContaDeTeste.cadastrar(mvc);
        ContaDeTeste.autorizarContato(mvc, ContaDeTeste.entrar(mvc, email));
        return usuarios.findByEmailIgnoreCase(email).orElseThrow();
    }

    public Anuncio doacaoDeFrutas(Usuario autor) {
        return publicar(autor, new Dados());
    }

    public Anuncio publicar(Usuario autor, Dados dados) {
        return anuncios.save(Anuncio.publicar(autor, dados.montar(), dados.criadoEm, hoje()));
    }

    public Anuncio salvar(Anuncio anuncio) {
        return anuncios.saveAndFlush(anuncio);
    }

    /** Simula um anúncio ATIVO cujo prazo passou antes de a rotina de expiração rodar. */
    public void vencerPrazo(Anuncio anuncio) {
        jdbc.update("update anuncio set criado_em = now() - interval '3 days', expira_em = now() - interval '1 hour' "
                + "where id = ?", anuncio.getId());
    }

    @Transactional
    public int expirarVencidos() {
        return anuncios.expirarVencidos(agora());
    }

    public void vencerValidade(Anuncio anuncio) {
        jdbc.update("update anuncio set validade_alimento = current_date - 2 where id = ?", anuncio.getId());
    }

    public void bloquear(Usuario usuario) {
        jdbc.update("update usuario set ativo = false where id = ?", usuario.getId());
    }

    public void revogarAceite(Usuario usuario) {
        jdbc.update("update usuario set aceite_contato_em = null where id = ?", usuario.getId());
    }

    public Instant agora() {
        return relogio.instant();
    }

    public LocalDate hoje() {
        return LocalDate.now(relogio.withZone(ClockConfig.FUSO_NEGOCIO));
    }

    public Categoria categoria(long id) {
        return categorias.findById(id).orElseThrow();
    }

    public Bairro bairro(String nome) {
        return bairros.findAll().stream().filter(b -> b.getNome().equals(nome)).findFirst().orElseThrow();
    }

    /** Doação de frutas válida; cada teste muda só o que precisa. */
    public class Dados {
        public Categoria categoria = categoria(FRUTAS);
        public Bairro bairro = bairro("Centro");
        public String titulo = "Laranjas do quintal";
        public String descricao = "Laranjas colhidas hoje, sem agrotóxico.";
        public BigDecimal quantidade = new BigDecimal("5");
        public Unidade unidade = Unidade.KG;
        public Modalidade modalidade = Modalidade.DOACAO;
        public BigDecimal preco;
        public LocalDate validadeAlimento;
        public String origemProduto;
        public String inspecao;
        public Instant criadoEm = agora();

        public Dados vendaDeOvos() {
            categoria = categoria(OVOS);
            titulo = "Ovos caipiras";
            descricao = "Uma dúzia de ovos de galinhas soltas.";
            quantidade = new BigDecimal("2");
            unidade = Unidade.DUZIA;
            modalidade = Modalidade.VENDA;
            preco = new BigDecimal("12.00");
            validadeAlimento = hoje().plusDays(10);
            origemProduto = "Galinhas caipiras do quintal";
            inspecao = "SIM 0123 (fictício)";
            return this;
        }

        DadosAnuncio montar() {
            return new DadosAnuncio(categoria, bairro, titulo, descricao, quantidade, unidade, modalidade, preco,
                    "Manter em local fresco", validadeAlimento, origemProduto, inspecao, hoje().plusDays(2), true);
        }
    }
}
