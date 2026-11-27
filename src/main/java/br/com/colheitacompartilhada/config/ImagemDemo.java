package br.com.colheitacompartilhada.config;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * Gera as fotos fictícias dos dados de demonstração: um degradê vertical com um círculo. Usa só a biblioteca
 * padrão do Java (Deflater e CRC32), então não é preciso nenhuma dependência nem arquivo em disco.
 */
final class ImagemDemo {

    private static final byte[] ASSINATURA_PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    private ImagemDemo() {
    }

    /** Cores em RGB (0xRRGGBB): topo e base do degradê e cor do círculo. */
    static byte[] png(int largura, int altura, int topo, int base, int destaque) {
        try {
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            saida.write(ASSINATURA_PNG);
            escreverBloco(saida, "IHDR", cabecalho(largura, altura));
            escreverBloco(saida, "IDAT", compactar(pixels(largura, altura, topo, base, destaque)));
            escreverBloco(saida, "IEND", new byte[0]);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] cabecalho(int largura, int altura) {
        return ByteBuffer.allocate(13).putInt(largura).putInt(altura)
                .put((byte) 8)   // 8 bits por canal
                .put((byte) 2)   // RGB
                .put((byte) 0).put((byte) 0).put((byte) 0)
                .array();
    }

    // Cada linha começa com o byte de filtro 0 (sem filtro), seguido de largura x 3 bytes RGB.
    private static byte[] pixels(int largura, int altura, int topo, int base, int destaque) {
        byte[] dados = new byte[altura * (1 + largura * 3)];
        int centroX = largura / 2;
        int centroY = altura / 2;
        int raio = Math.min(largura, altura) / 4;
        int posicao = 0;
        for (int y = 0; y < altura; y++) {
            dados[posicao++] = 0;
            int corDaLinha = misturar(topo, base, y, altura - 1);
            for (int x = 0; x < largura; x++) {
                long dx = x - centroX;
                long dy = y - centroY;
                int cor = dx * dx + dy * dy <= (long) raio * raio ? destaque : corDaLinha;
                dados[posicao++] = (byte) (cor >> 16);
                dados[posicao++] = (byte) (cor >> 8);
                dados[posicao++] = (byte) cor;
            }
        }
        return dados;
    }

    private static int misturar(int de, int ate, int passo, int total) {
        int vermelho = canal(de, ate, 16, passo, total);
        int verde = canal(de, ate, 8, passo, total);
        int azul = canal(de, ate, 0, passo, total);
        return (vermelho << 16) | (verde << 8) | azul;
    }

    private static int canal(int de, int ate, int deslocamento, int passo, int total) {
        int a = (de >> deslocamento) & 0xFF;
        int b = (ate >> deslocamento) & 0xFF;
        return total == 0 ? a : a + (b - a) * passo / total;
    }

    private static byte[] compactar(byte[] dados) throws IOException {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try (DeflaterOutputStream compactador = new DeflaterOutputStream(saida, deflater)) {
            compactador.write(dados);
        } finally {
            deflater.end();
        }
        return saida.toByteArray();
    }

    // Bloco PNG: tamanho, tipo, dados e CRC32 calculado sobre tipo + dados.
    private static void escreverBloco(ByteArrayOutputStream saida, String tipo, byte[] dados) throws IOException {
        byte[] nome = tipo.getBytes(StandardCharsets.US_ASCII);
        CRC32 crc = new CRC32();
        crc.update(nome);
        crc.update(dados);
        saida.write(ByteBuffer.allocate(4).putInt(dados.length).array());
        saida.write(nome);
        saida.write(dados);
        saida.write(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
    }
}
