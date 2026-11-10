package br.com.colheitacompartilhada.comum;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PaginaTest {

    @Test
    void converteAPaginaDoSpringParaOFormatoDoContrato() {
        var page = new PageImpl<>(List.of("a", "b"), PageRequest.of(2, Pagina.TAMANHO), 42);

        Pagina<String> pagina = Pagina.de(page);

        assertThat(pagina).isEqualTo(new Pagina<>(List.of("a", "b"), 2, 20, 42, 3));
    }
}
