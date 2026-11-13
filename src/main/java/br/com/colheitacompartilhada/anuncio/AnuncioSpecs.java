package br.com.colheitacompartilhada.anuncio;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

import jakarta.persistence.criteria.Path;
import org.springframework.data.jpa.domain.Specification;

/** Filtros da busca pública. Um filtro não informado devolve null, que o Spring Data ignora ao combinar. */
public final class AnuncioSpecs {

    private static final char ESCAPE = '\\';

    private AnuncioSpecs() {
    }

    /** RN08. A mesma regra existe para um anúncio já carregado em Anuncio.estaVisivel; as duas devem mudar juntas. */
    public static Specification<Anuncio> visivel(Instant agora, LocalDate hoje) {
        return (anuncio, query, cb) -> {
            Path<Object> usuario = anuncio.get("usuario");
            return cb.and(
                    cb.equal(anuncio.get("status"), StatusAnuncio.ATIVO),
                    cb.greaterThan(anuncio.get("expiraEm"), agora),
                    cb.or(cb.isNull(anuncio.get("validadeAlimento")),
                            cb.greaterThanOrEqualTo(anuncio.get("validadeAlimento"), hoje)),
                    cb.isTrue(usuario.get("ativo")),
                    cb.isNotNull(usuario.get("aceiteContatoEm")));
        };
    }

    public static Specification<Anuncio> comTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String padrao = "%" + escapar(texto.trim().toLowerCase(Locale.ROOT)) + "%";
        return (anuncio, query, cb) -> cb.or(
                cb.like(cb.lower(anuncio.get("titulo")), padrao, ESCAPE),
                cb.like(cb.lower(anuncio.get("descricao")), padrao, ESCAPE));
    }

    public static Specification<Anuncio> doBairro(Long bairroId) {
        return bairroId == null ? null : (anuncio, query, cb) -> cb.equal(anuncio.get("bairro").get("id"), bairroId);
    }

    public static Specification<Anuncio> daCategoria(Long categoriaId) {
        return categoriaId == null ? null
                : (anuncio, query, cb) -> cb.equal(anuncio.get("categoria").get("id"), categoriaId);
    }

    public static Specification<Anuncio> daModalidade(Modalidade modalidade) {
        return modalidade == null ? null : (anuncio, query, cb) -> cb.equal(anuncio.get("modalidade"), modalidade);
    }

    // Sem isto, um "%" digitado na busca casaria com todos os anúncios.
    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
