package br.com.colheitacompartilhada.denuncia;

import java.time.Clock;
import java.util.UUID;

import br.com.colheitacompartilhada.anuncio.Anuncio;
import br.com.colheitacompartilhada.anuncio.AnuncioService;
import br.com.colheitacompartilhada.comum.AcessoNegadoException;
import br.com.colheitacompartilhada.comum.ConflitoException;
import br.com.colheitacompartilhada.comum.NaoEncontradoException;
import br.com.colheitacompartilhada.comum.Pagina;
import br.com.colheitacompartilhada.denuncia.dto.ResolucaoRequest;
import br.com.colheitacompartilhada.usuario.Usuario;
import br.com.colheitacompartilhada.usuario.UsuarioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DenunciaService {

    private final DenunciaRepository denuncias;
    private final AnuncioService anuncioService;
    private final UsuarioService usuarioService;
    private final Clock clock;

    public DenunciaService(DenunciaRepository denuncias, AnuncioService anuncioService,
            UsuarioService usuarioService, Clock clock) {
        this.denuncias = denuncias;
        this.anuncioService = anuncioService;
        this.usuarioService = usuarioService;
        this.clock = clock;
    }

    /** RN10.1 a RN10.3. Anúncio não visível é tratado como inexistente (404). */
    @Transactional
    public Denuncia abrir(UUID anuncioId, UUID autorId, String motivo) {
        Anuncio anuncio = anuncioService.detalhar(anuncioId);
        Usuario autor = usuarioService.buscar(autorId);
        Denuncia denuncia = Denuncia.abrir(anuncio, autor, motivo, clock.instant());
        if (denuncias.existsByAnuncioIdAndAutorIdAndStatus(anuncioId, autorId, StatusDenuncia.ABERTA)) {
            throw new ConflitoException("DENUNCIA_DUPLICADA", "Você já tem uma denúncia aberta para este anúncio.");
        }
        return denuncias.save(denuncia);
    }

    @Transactional(readOnly = true)
    public Page<Denuncia> listar(UUID moderadorId, StatusDenuncia status, int pagina) {
        exigirAdmin(moderadorId);
        return denuncias.findByStatusOrderByCriadoEmAsc(status, PageRequest.of(pagina, Pagina.TAMANHO));
    }

    @Transactional(readOnly = true)
    public Denuncia detalhar(UUID id, UUID moderadorId) {
        exigirAdmin(moderadorId);
        return comAssociacoes(id);
    }

    /** RN10.6: remover o anúncio, bloquear o anunciante e fechar a denúncia acontecem na mesma transação. */
    @Transactional
    public Denuncia resolver(UUID id, UUID moderadorId, ResolucaoRequest pedido) {
        Usuario moderador = exigirAdmin(moderadorId);
        // Lock pessimista: duas resoluções simultâneas da mesma denúncia acontecem uma depois da outra.
        Denuncia denuncia = denuncias.findByIdParaAtualizar(id).orElseThrow(DenunciaService::naoEncontrada);
        denuncia.resolver(moderador, pedido.procedente(), pedido.decisao(), pedido.bloquearAnunciante(),
                clock.instant());
        return comAssociacoes(id);
    }

    // A autorização por papel também é conferida aqui, além da regra de /admin/** na SecurityConfig.
    private Usuario exigirAdmin(UUID usuarioId) {
        Usuario usuario = usuarioService.buscar(usuarioId);
        if (!usuario.isAdmin()) {
            throw new AcessoNegadoException("Somente administradores podem moderar denúncias.");
        }
        return usuario;
    }

    // Recarrega com as associações: a resposta é montada fora da transação (open-in-view desligado).
    private Denuncia comAssociacoes(UUID id) {
        return denuncias.findComAssociacoesById(id).orElseThrow(DenunciaService::naoEncontrada);
    }

    private static NaoEncontradoException naoEncontrada() {
        return new NaoEncontradoException("Denúncia não encontrada.");
    }
}
