package br.com.colheitacompartilhada.anuncio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import br.com.colheitacompartilhada.anuncio.Anuncio;
import br.com.colheitacompartilhada.anuncio.Modalidade;
import br.com.colheitacompartilhada.anuncio.Resultado;
import br.com.colheitacompartilhada.anuncio.StatusAnuncio;
import br.com.colheitacompartilhada.anuncio.Unidade;

/** Anúncio visto pelo próprio dono (04_API #7): AnuncioDetalhe + situação, em qualquer status. */
public record MeuAnuncio(UUID id, String titulo, ItemCatalogo categoria, ItemCatalogo bairro,
        Modalidade modalidade, BigDecimal preco, BigDecimal quantidade, Unidade unidade, String fotoUrl,
        Instant expiraEm, Instant criadoEm, String descricao, String conservacao, LocalDate validadeAlimento,
        String origemProduto, String inspecao, AnuncioDetalhe.Anunciante anunciante, boolean souDono,
        StatusAnuncio status, Resultado resultado, Instant encerradoEm, Instant atualizadoEm, boolean visivel,
        boolean podeEditar) {

    public static MeuAnuncio de(Anuncio anuncio, Instant agora, LocalDate hoje) {
        AnuncioDetalhe d = AnuncioDetalhe.de(anuncio, true);
        return new MeuAnuncio(d.id(), d.titulo(), d.categoria(), d.bairro(), d.modalidade(), d.preco(),
                d.quantidade(), d.unidade(), d.fotoUrl(), d.expiraEm(), d.criadoEm(), d.descricao(),
                d.conservacao(), d.validadeAlimento(), d.origemProduto(), d.inspecao(), d.anunciante(), true,
                anuncio.getStatus(), anuncio.getResultado(), anuncio.getEncerradoEm(), anuncio.getAtualizadoEm(),
                anuncio.estaVisivel(agora, hoje), anuncio.podeSerAlterado(agora));
    }
}
