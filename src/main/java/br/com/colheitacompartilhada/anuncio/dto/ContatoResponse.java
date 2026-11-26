package br.com.colheitacompartilhada.anuncio.dto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import br.com.colheitacompartilhada.anuncio.Anuncio;
import br.com.colheitacompartilhada.usuario.Whatsapp;

/** Contato externo do anunciante (04_API #15). Nada é gravado: não existe pedido nem reserva. */
public record ContatoResponse(String whatsapp, String whatsappFormatado, String link) {

    public static ContatoResponse de(Anuncio anuncio) {
        String numero = anuncio.getUsuario().getWhatsapp();
        String mensagem = "Olá! Vi seu anúncio \"" + anuncio.getTitulo() + "\" no Colheita Compartilhada.";
        // URLEncoder usa "+" para espaço; no link do WhatsApp o espaço precisa ser "%20".
        String texto = URLEncoder.encode(mensagem, StandardCharsets.UTF_8).replace("+", "%20");
        return new ContatoResponse(numero, Whatsapp.formatar(numero), "https://wa.me/" + numero + "?text=" + texto);
    }
}
