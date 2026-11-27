package br.com.colheitacompartilhada.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

/** Fotos fictícias da demonstração (issue #19): precisam ser PNG válidos e leves, sem arquivo em disco. */
class ImagemDemoTest {

    @Test
    void geraUmPngValidoComAsDimensoesPedidas() throws Exception {
        byte[] png = ImagemDemo.png(640, 480, 0xFFFFFF, 0xF4A300, 0xFFFFFF);

        BufferedImage imagem = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(imagem).isNotNull();
        assertThat(imagem.getWidth()).isEqualTo(640);
        assertThat(imagem.getHeight()).isEqualTo(480);
    }

    @Test
    void comecaComAAssinaturaPngQueOFotoServiceReconhece() {
        byte[] png = ImagemDemo.png(64, 48, 0xFFFFFF, 0x3F8F3F, 0xFFFFFF);

        assertThat(png).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, (byte) 0x0D, (byte) 0x0A,
                (byte) 0x1A, (byte) 0x0A);
    }

    @Test
    void ficaMuitoAbaixoDoLimiteDe2MbDaFoto() {
        assertThat(ImagemDemo.png(640, 480, 0xFFFFFF, 0xB08968, 0xFFFFFF).length).isLessThan(100_000);
    }

    @Test
    void oDegradeVaiDaCorDoTopoParaACorDaBase() throws Exception {
        BufferedImage imagem = ImageIO.read(new ByteArrayInputStream(
                ImagemDemo.png(100, 100, 0xFF0000, 0x0000FF, 0x00FF00)));

        assertThat(imagem.getRGB(2, 0) & 0xFFFFFF).isEqualTo(0xFF0000);
        assertThat(imagem.getRGB(2, 99) & 0xFFFFFF).isEqualTo(0x0000FF);
        assertThat(imagem.getRGB(50, 50) & 0xFFFFFF).isEqualTo(0x00FF00);
    }
}
