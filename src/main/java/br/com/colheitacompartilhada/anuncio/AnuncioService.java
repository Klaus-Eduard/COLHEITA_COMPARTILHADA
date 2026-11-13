package br.com.colheitacompartilhada.anuncio;

import static br.com.colheitacompartilhada.anuncio.AnuncioSpecs.comTexto;
import static br.com.colheitacompartilhada.anuncio.AnuncioSpecs.daCategoria;
import static br.com.colheitacompartilhada.anuncio.AnuncioSpecs.daModalidade;
import static br.com.colheitacompartilhada.anuncio.AnuncioSpecs.doBairro;
import static br.com.colheitacompartilhada.anuncio.AnuncioSpecs.visivel;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import br.com.colheitacompartilhada.anuncio.dto.AnuncioForm;
import br.com.colheitacompartilhada.catalogo.Bairro;
import br.com.colheitacompartilhada.catalogo.CatalogoService;
import br.com.colheitacompartilhada.catalogo.Categoria;
import br.com.colheitacompartilhada.comum.AcessoNegadoException;
import br.com.colheitacompartilhada.comum.NaoEncontradoException;
import br.com.colheitacompartilhada.comum.Pagina;
import br.com.colheitacompartilhada.comum.RegraNegocioException;
import br.com.colheitacompartilhada.config.ClockConfig;
import br.com.colheitacompartilhada.usuario.Usuario;
import br.com.colheitacompartilhada.usuario.UsuarioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AnuncioService {

    private static final Sort MAIS_RECENTES = Sort.by(Sort.Order.desc("criadoEm"), Sort.Order.desc("id"));

    private final AnuncioRepository anuncios;
    private final FotoService fotoService;
    private final UsuarioService usuarioService;
    private final CatalogoService catalogoService;
    private final Clock clock;

    public AnuncioService(AnuncioRepository anuncios, FotoService fotoService, UsuarioService usuarioService,
            CatalogoService catalogoService, Clock clock) {
        this.anuncios = anuncios;
        this.fotoService = fotoService;
        this.usuarioService = usuarioService;
        this.catalogoService = catalogoService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<Anuncio> buscar(FiltroAnuncios filtro, int pagina) {
        Specification<Anuncio> criterios = visivel(agora(), hoje())
                .and(comTexto(filtro.texto()))
                .and(doBairro(filtro.bairroId()))
                .and(daCategoria(filtro.categoriaId()))
                .and(daModalidade(filtro.modalidade()));
        Pageable paginacao = PageRequest.of(pagina, Pagina.TAMANHO, MAIS_RECENTES);
        return anuncios.findAll(criterios, paginacao);
    }

    // RN08: anúncio não visível é tratado como inexistente.
    @Transactional(readOnly = true)
    public Anuncio detalhar(UUID id) {
        return anuncios.findComAssociacoesById(id)
                .filter(anuncio -> anuncio.estaVisivel(agora(), hoje()))
                .orElseThrow(AnuncioService::naoEncontrado);
    }

    @Transactional
    public Anuncio publicar(UUID autorId, AnuncioForm form, MultipartFile foto) {
        Usuario autor = usuarioService.buscar(autorId);
        Anuncio anuncio = anuncios.save(Anuncio.publicar(autor, dados(form), agora(), hoje()));
        if (temArquivo(foto)) {
            fotoService.salvarPara(anuncio, foto);
        }
        return anuncio;
    }

    @Transactional
    public Anuncio editar(UUID anuncioId, UUID usuarioId, AnuncioForm form, MultipartFile foto) {
        Anuncio anuncio = carregarDoDonoParaAlterar(anuncioId, usuarioId);
        anuncio.editar(dados(form), agora(), hoje());
        if (temArquivo(foto)) {
            fotoService.salvarPara(anuncio, foto);
        } else if (form.removerFoto()) {
            fotoService.removerDe(anuncio);
        }
        return comAssociacoes(anuncioId);
    }

    @Transactional
    public Anuncio encerrar(UUID anuncioId, UUID usuarioId, Resultado resultado) {
        Anuncio anuncio = carregarDoDonoParaAlterar(anuncioId, usuarioId);
        anuncio.encerrar(resultado, agora());
        return comAssociacoes(anuncioId);
    }

    @Transactional(readOnly = true)
    public Page<Anuncio> listarDoDono(UUID usuarioId, int pagina) {
        return anuncios.findByUsuarioIdOrderByCriadoEmDesc(usuarioId, PageRequest.of(pagina, Pagina.TAMANHO));
    }

    @Transactional(readOnly = true)
    public Anuncio buscarDoDono(UUID anuncioId, UUID usuarioId) {
        Anuncio anuncio = comAssociacoes(anuncioId);
        exigirDono(anuncio, usuarioId);
        return anuncio;
    }

    // Lock pessimista (D08): duas alterações simultâneas do mesmo anúncio acontecem uma depois da outra.
    private Anuncio carregarDoDonoParaAlterar(UUID anuncioId, UUID usuarioId) {
        Anuncio anuncio = anuncios.findByIdParaAtualizar(anuncioId).orElseThrow(AnuncioService::naoEncontrado);
        exigirDono(anuncio, usuarioId);
        return anuncio;
    }

    // RN09.1: o ID do usuário vem da sessão; ADMIN também não altera anúncio alheio.
    private static void exigirDono(Anuncio anuncio, UUID usuarioId) {
        if (!anuncio.pertenceA(usuarioId)) {
            throw new AcessoNegadoException("Somente o dono pode alterar este anúncio.");
        }
    }

    // Recarrega com categoria, bairro e usuário: a resposta é montada fora da transação (open-in-view desligado).
    private Anuncio comAssociacoes(UUID anuncioId) {
        return anuncios.findComAssociacoesById(anuncioId).orElseThrow(AnuncioService::naoEncontrado);
    }

    private DadosAnuncio dados(AnuncioForm form) {
        Categoria categoria = catalogoService.categoria(form.categoriaId())
                .orElseThrow(() -> new RegraNegocioException("VALIDACAO", "categoriaId", "Selecione a categoria."));
        Bairro bairro = catalogoService.bairro(form.bairroId())
                .orElseThrow(() -> new RegraNegocioException("VALIDACAO", "bairroId", "Selecione o bairro."));
        return new DadosAnuncio(categoria, bairro, form.titulo(), form.descricao(), form.quantidade(),
                form.unidade(), form.modalidade(), form.preco(), form.conservacao(), form.validadeAlimento(),
                form.origemProduto(), form.inspecao(), form.disponivelAte(), form.excedenteDeclarado());
    }

    private static boolean temArquivo(MultipartFile foto) {
        return foto != null && !foto.isEmpty();
    }

    private static NaoEncontradoException naoEncontrado() {
        return new NaoEncontradoException("Anúncio não encontrado.");
    }

    private Instant agora() {
        return clock.instant();
    }

    private LocalDate hoje() {
        return ClockConfig.hoje(clock);
    }
}
