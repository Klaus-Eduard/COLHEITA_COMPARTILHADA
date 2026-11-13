package br.com.colheitacompartilhada.anuncio;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import br.com.colheitacompartilhada.catalogo.Bairro;
import br.com.colheitacompartilhada.catalogo.Categoria;
import br.com.colheitacompartilhada.usuario.Papel;
import br.com.colheitacompartilhada.usuario.TipoParticipante;
import br.com.colheitacompartilhada.usuario.Usuario;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Objetos prontos para os testes unitários do anúncio. Usa reflexão onde a entidade ainda não tem
 * método público para chegar ao estado desejado (catálogo vem só de migração; perfil é a issue 5).
 */
final class Fixtures {

    static final Instant AGORA = Instant.parse("2026-10-20T15:00:00Z");
    static final LocalDate HOJE = LocalDate.parse("2026-10-20");

    private Fixtures() {
    }

    static Usuario anunciante() {
        Usuario usuario = usuarioSemContato();
        ReflectionTestUtils.setField(usuario, "whatsapp", "5547999998888");
        ReflectionTestUtils.setField(usuario, "aceiteContatoEm", AGORA.minusSeconds(3600));
        return usuario;
    }

    static Usuario usuarioSemContato() {
        Usuario usuario = Usuario.criar("Ana Souza", "ana@exemplo.com", "{bcrypt}x", TipoParticipante.MORADOR,
                Papel.USUARIO, AGORA.minusSeconds(86400));
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
        return usuario;
    }

    static Categoria frutas() {
        return categoria(1L, "Frutas", false);
    }

    static Categoria ovos() {
        return categoria(3L, "Ovos", true);
    }

    static Bairro bairro() {
        Bairro bairro = BeanUtils.instantiateClass(Bairro.class);
        ReflectionTestUtils.setField(bairro, "id", 5L);
        ReflectionTestUtils.setField(bairro, "nome", "Centro");
        return bairro;
    }

    static Dados doacaoDeFrutas() {
        return new Dados();
    }

    static Dados vendaDeOvos() {
        Dados dados = new Dados();
        dados.categoria = ovos();
        dados.titulo = "Ovos caipiras";
        dados.quantidade = new BigDecimal("2");
        dados.unidade = Unidade.DUZIA;
        dados.modalidade = Modalidade.VENDA;
        dados.preco = new BigDecimal("12.00");
        dados.validadeAlimento = HOJE.plusDays(10);
        dados.origemProduto = "Quintal próprio";
        dados.inspecao = "SIM 0123 (fictício)";
        return dados;
    }

    private static Categoria categoria(Long id, String nome, boolean origemAnimal) {
        Categoria categoria = BeanUtils.instantiateClass(Categoria.class);
        ReflectionTestUtils.setField(categoria, "id", id);
        ReflectionTestUtils.setField(categoria, "nome", nome);
        ReflectionTestUtils.setField(categoria, "origemAnimal", origemAnimal);
        return categoria;
    }

    /** Formulário válido que cada teste altera só no campo que quer exercitar. */
    static final class Dados {
        Categoria categoria = frutas();
        Bairro bairro = Fixtures.bairro();
        String titulo = "Laranjas do quintal";
        String descricao = "Laranjas colhidas hoje, sem agrotóxico.";
        BigDecimal quantidade = new BigDecimal("1.5");
        Unidade unidade = Unidade.KG;
        Modalidade modalidade = Modalidade.DOACAO;
        BigDecimal preco;
        String conservacao = "Manter em local fresco";
        LocalDate validadeAlimento;
        String origemProduto;
        String inspecao;
        LocalDate disponivelAte = HOJE.plusDays(2);
        boolean excedenteDeclarado = true;

        DadosAnuncio montar() {
            return new DadosAnuncio(categoria, bairro, titulo, descricao, quantidade, unidade, modalidade, preco,
                    conservacao, validadeAlimento, origemProduto, inspecao, disponivelAte, excedenteDeclarado);
        }
    }
}
