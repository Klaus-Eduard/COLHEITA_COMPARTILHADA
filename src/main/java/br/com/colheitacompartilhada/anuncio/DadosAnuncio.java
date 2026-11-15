package br.com.colheitacompartilhada.anuncio;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.colheitacompartilhada.catalogo.Bairro;
import br.com.colheitacompartilhada.catalogo.Categoria;

/** Dados de publicação ou edição já com categoria e bairro carregados; o service monta a partir do formulário. */
public record DadosAnuncio(
        Categoria categoria,
        Bairro bairro,
        String titulo,
        String descricao,
        BigDecimal quantidade,
        Unidade unidade,
        Modalidade modalidade,
        BigDecimal preco,
        String conservacao,
        LocalDate validadeAlimento,
        String origemProduto,
        String inspecao,
        LocalDate disponivelAte,
        boolean excedenteDeclarado) {
}
