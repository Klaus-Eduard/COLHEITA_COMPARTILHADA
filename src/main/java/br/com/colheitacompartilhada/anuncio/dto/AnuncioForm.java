package br.com.colheitacompartilhada.anuncio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.colheitacompartilhada.anuncio.Modalidade;
import br.com.colheitacompartilhada.anuncio.Unidade;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Parte "dados" de POST /anuncios e PATCH /anuncios/{id} (04_API #12 e #13). Aqui ficam só formato e
 * tamanho dos campos; as regras de negócio (RN03–RN07) estão em Anuncio.publicar e Anuncio.editar.
 */
public record AnuncioForm(
        @NotNull(message = TITULO) @Size(min = 3, max = 100, message = TITULO) String titulo,
        @NotNull(message = DESCRICAO) @Size(min = 10, max = 1000, message = DESCRICAO) String descricao,
        @NotNull(message = "Selecione a categoria.") Long categoriaId,
        @NotNull(message = "Selecione o bairro.") Long bairroId,
        @NotNull(message = "Informe uma quantidade maior que zero.") BigDecimal quantidade,
        @NotNull(message = "Selecione a unidade.") Unidade unidade,
        @NotNull(message = "Selecione doação ou venda.") Modalidade modalidade,
        BigDecimal preco,
        @NotNull(message = CONSERVACAO) @Size(min = 1, max = 300, message = CONSERVACAO) String conservacao,
        LocalDate validadeAlimento,
        @Size(max = 200, message = "A origem deve ter até 200 caracteres.") String origemProduto,
        @Size(max = 200, message = "A inspeção deve ter até 200 caracteres.") String inspecao,
        @NotNull(message = "Escolha uma data entre hoje e 7 dias.") LocalDate disponivelAte,
        boolean excedenteDeclarado,
        boolean removerFoto) {

    private static final String TITULO = "O título deve ter de 3 a 100 caracteres.";
    private static final String DESCRICAO = "A descrição deve ter de 10 a 1000 caracteres.";
    private static final String CONSERVACAO = "Informe como o alimento deve ser conservado.";

    // Os tamanhos da tabela de validação contam o texto sem os espaços das pontas.
    public AnuncioForm {
        titulo = aparar(titulo);
        descricao = aparar(descricao);
        conservacao = aparar(conservacao);
        origemProduto = aparar(origemProduto);
        inspecao = aparar(inspecao);
    }

    private static String aparar(String texto) {
        return texto == null ? null : texto.trim();
    }
}
