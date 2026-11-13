package br.com.colheitacompartilhada.anuncio;

import org.springframework.mock.web.MockMultipartFile;

/** Arquivos de teste com a assinatura real de PNG ou JPEG e o tamanho pedido. */
public final class Imagens {

    private static final byte[] INICIO_PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] INICIO_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};

    private Imagens() {
    }

    public static MockMultipartFile png(int tamanho) {
        return new MockMultipartFile("foto", "foto.png", "image/png", comInicio(INICIO_PNG, tamanho));
    }

    public static MockMultipartFile jpeg(int tamanho) {
        return new MockMultipartFile("foto", "foto.jpg", "image/jpeg", comInicio(INICIO_JPEG, tamanho));
    }

    private static byte[] comInicio(byte[] inicio, int tamanho) {
        byte[] conteudo = new byte[tamanho];
        System.arraycopy(inicio, 0, conteudo, 0, inicio.length);
        return conteudo;
    }
}
