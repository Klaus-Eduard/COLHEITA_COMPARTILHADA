package br.com.colheitacompartilhada.anuncio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class UnidadeTest {

    @ParameterizedTest
    @EnumSource(value = Unidade.class, names = {"UNIDADE", "MACO", "DUZIA"})
    void unidadesContaveisSaoInteiras(Unidade unidade) {
        assertThat(unidade.isInteira()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = Unidade.class, names = {"KG", "LITRO"})
    void unidadesDeMedidaAceitamFracao(Unidade unidade) {
        assertThat(unidade.isInteira()).isFalse();
    }

    @Test
    void temExatamenteAsUnidadesDoSchema() {
        assertThat(Unidade.values()).extracting(Enum::name)
                .containsExactlyInAnyOrder("KG", "UNIDADE", "LITRO", "MACO", "DUZIA");
    }
}
