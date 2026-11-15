package br.com.colheitacompartilhada.anuncio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import br.com.colheitacompartilhada.anuncio.Anuncio;
import br.com.colheitacompartilhada.anuncio.Modalidade;
import br.com.colheitacompartilhada.anuncio.Unidade;

public record AnuncioResumo(UUID id, String titulo, ItemCatalogo categoria, ItemCatalogo bairro,
        Modalidade modalidade, BigDecimal preco, BigDecimal quantidade, Unidade unidade, String fotoUrl,
        Instant expiraEm, Instant criadoEm) {

    public static AnuncioResumo de(Anuncio anuncio) {
        return new AnuncioResumo(anuncio.getId(), anuncio.getTitulo(), ItemCatalogo.de(anuncio.getCategoria()),
                ItemCatalogo.de(anuncio.getBairro()), anuncio.getModalidade(), anuncio.getPreco(),
                anuncio.getQuantidade(), anuncio.getUnidade(), anuncio.getFotoUrl(), anuncio.getExpiraEm(),
                anuncio.getCriadoEm());
    }
}
