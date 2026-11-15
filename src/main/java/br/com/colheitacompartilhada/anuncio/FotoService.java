package br.com.colheitacompartilhada.anuncio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.Arrays;
import java.util.UUID;

import br.com.colheitacompartilhada.comum.NaoEncontradoException;
import br.com.colheitacompartilhada.comum.RegraNegocioException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

/**
 * Foto do anúncio guardada no PostgreSQL (D13). Os métodos que gravam são chamados dentro da transação
 * do AnuncioService: se a gravação do anúncio falhar, a foto também não fica no banco (UC06.10).
 */
@Service
public class FotoService {

    static final int TAMANHO_MAXIMO = 2 * 1024 * 1024;

    private static final byte[] INICIO_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] INICIO_PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    private final FotoRepository fotos;
    private final Clock clock;

    public FotoService(FotoRepository fotos, Clock clock) {
        this.fotos = fotos;
        this.clock = clock;
    }

    /** RN06: o tipo vem dos bytes iniciais, nunca da extensão nem do Content-Type enviado. */
    public String validar(MultipartFile arquivo) {
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new MaxUploadSizeExceededException(TAMANHO_MAXIMO);
        }
        byte[] conteudo = ler(arquivo);
        if (comecaCom(conteudo, INICIO_JPEG)) {
            return MediaType.IMAGE_JPEG_VALUE;
        }
        if (comecaCom(conteudo, INICIO_PNG)) {
            return MediaType.IMAGE_PNG_VALUE;
        }
        throw new RegraNegocioException("ARQUIVO_INVALIDO", null, "A foto deve ser uma imagem JPEG ou PNG.");
    }

    // O flush é obrigatório: sem ele o Hibernate faria o INSERT antes do DELETE e violaria o UNIQUE(anuncio_id).
    @Transactional
    public void salvarPara(Anuncio anuncio, MultipartFile arquivo) {
        String tipo = validar(arquivo);
        fotos.deleteByAnuncioId(anuncio.getId());
        fotos.flush();
        Foto foto = fotos.save(Foto.criar(anuncio.getId(), tipo, ler(arquivo), clock.instant()));
        anuncio.atribuirFoto(foto.getId());
    }

    @Transactional
    public void removerDe(Anuncio anuncio) {
        fotos.deleteByAnuncioId(anuncio.getId());
        anuncio.removerFoto();
    }

    // O id vem da URL como texto: um valor que não é UUID é tratado como foto inexistente (04_API #20).
    @Transactional(readOnly = true)
    public Foto buscar(String id) {
        try {
            return fotos.findById(UUID.fromString(id)).orElseThrow(FotoService::naoEncontrada);
        } catch (IllegalArgumentException idInvalido) {
            throw naoEncontrada();
        }
    }

    private static NaoEncontradoException naoEncontrada() {
        return new NaoEncontradoException("Foto não encontrada.");
    }

    private static boolean comecaCom(byte[] conteudo, byte[] inicio) {
        return conteudo.length >= inicio.length
                && Arrays.equals(conteudo, 0, inicio.length, inicio, 0, inicio.length);
    }

    private static byte[] ler(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException falha) {
            throw new UncheckedIOException(falha);
        }
    }
}
