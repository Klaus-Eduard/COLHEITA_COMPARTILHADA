package br.com.colheitacompartilhada.anuncio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;

import br.com.colheitacompartilhada.comum.RegraNegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** RN06: validação da foto pelos bytes iniciais e pelo tamanho. */
class FotoServiceTest {

    private final FotoService fotoService = new FotoService(null, Clock.systemUTC());

    @Test
    void reconheceJpegEPngPelosBytesIniciais() {
        assertThat(fotoService.validar(Imagens.jpeg(100))).isEqualTo("image/jpeg");
        assertThat(fotoService.validar(Imagens.png(100))).isEqualTo("image/png");
    }

    @Test
    void ignoraExtensaoEContentTypeEnviados() throws Exception {
        MockMultipartFile jpegDisfarcado = new MockMultipartFile("foto", "documento.txt", "text/plain",
                Imagens.jpeg(100).getBytes());

        assertThat(fotoService.validar(jpegDisfarcado)).isEqualTo("image/jpeg");
    }

    @Test
    void uc06_9_arquivoPngComConteudoDeTextoEInvalido() {
        MockMultipartFile texto = new MockMultipartFile("foto", "foto.png", "image/png",
                "isto não é uma imagem".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> fotoService.validar(texto))
                .isInstanceOfSatisfying(RegraNegocioException.class, erro -> {
                    assertThat(erro.getCodigo()).isEqualTo("ARQUIVO_INVALIDO");
                    assertThat(erro.getCampo()).isNull();
                });
    }

    @Test
    void arquivoVazioOuMenorQueAAssinaturaEInvalido() {
        assertThatThrownBy(() -> fotoService.validar(new MockMultipartFile("foto", new byte[0])))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> fotoService.validar(new MockMultipartFile("foto", new byte[] {(byte) 0x89, 0x50})))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void uc06_9_arquivoDeTresMegasEGrandeDemais() {
        assertThatThrownBy(() -> fotoService.validar(Imagens.png(3 * 1024 * 1024)))
                .isInstanceOf(MaxUploadSizeExceededException.class);
    }

    @Test
    void aceitaExatamenteDoisMegasERecusaUmByteAMais() {
        assertThatCode(() -> fotoService.validar(Imagens.png(FotoService.TAMANHO_MAXIMO))).doesNotThrowAnyException();
        assertThatThrownBy(() -> fotoService.validar(Imagens.png(FotoService.TAMANHO_MAXIMO + 1)))
                .isInstanceOf(MaxUploadSizeExceededException.class);
    }
}
