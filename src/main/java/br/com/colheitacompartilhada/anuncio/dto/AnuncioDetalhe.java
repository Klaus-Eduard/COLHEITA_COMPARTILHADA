package br.com.colheitacompartilhada.anuncio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import br.com.colheitacompartilhada.anuncio.Anuncio;
import br.com.colheitacompartilhada.anuncio.Modalidade;
import br.com.colheitacompartilhada.anuncio.Unidade;
import br.com.colheitacompartilhada.usuario.TipoParticipante;

/** Detalhe público. Do anunciante saem só nome e tipo: WhatsApp, e-mail e ID nunca entram aqui. */
public record AnuncioDetalhe(UUID id, String titulo, ItemCatalogo categoria, ItemCatalogo bairro,
        Modalidade modalidade, BigDecimal preco, BigDecimal quantidade, Unidade unidade, String fotoUrl,
        Instant expiraEm, Instant criadoEm, String descricao, String conservacao, LocalDate validadeAlimento,
        String origemProduto, String inspecao, Anunciante anunciante, boolean souDono) {

    public record Anunciante(String nome, TipoParticipante tipo) {
    }

    public static AnuncioDetalhe de(Anuncio anuncio, boolean souDono) {
        Anunciante anunciante = new Anunciante(anuncio.getUsuario().getNome(), anuncio.getUsuario().getTipo());
        return new AnuncioDetalhe(anuncio.getId(), anuncio.getTitulo(), ItemCatalogo.de(anuncio.getCategoria()),
                ItemCatalogo.de(anuncio.getBairro()), anuncio.getModalidade(), anuncio.getPreco(),
                anuncio.getQuantidade(), anuncio.getUnidade(), anuncio.getFotoUrl(), anuncio.getExpiraEm(),
                anuncio.getCriadoEm(), anuncio.getDescricao(), anuncio.getConservacao(),
                anuncio.getValidadeAlimento(), anuncio.getOrigemProduto(), anuncio.getInspecao(), anunciante,
                souDono);
    }
}
