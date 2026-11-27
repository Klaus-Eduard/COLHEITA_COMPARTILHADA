package br.com.colheitacompartilhada.denuncia;

import java.time.Clock;
import java.util.UUID;

import br.com.colheitacompartilhada.anuncio.Anuncio;
import br.com.colheitacompartilhada.anuncio.AnuncioService;
import br.com.colheitacompartilhada.comum.ConflitoException;
import br.com.colheitacompartilhada.usuario.Usuario;
import br.com.colheitacompartilhada.usuario.UsuarioService;
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
}
