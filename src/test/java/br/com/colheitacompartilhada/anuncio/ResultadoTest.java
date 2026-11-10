package br.com.colheitacompartilhada.anuncio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ResultadoTest {

    @Test
    void doadoSoCombinaComDoacao() {
        assertThat(Resultado.DOADO.compativelCom(Modalidade.DOACAO)).isTrue();
        assertThat(Resultado.DOADO.compativelCom(Modalidade.VENDA)).isFalse();
    }

    @Test
    void vendidoSoCombinaComVenda() {
        assertThat(Resultado.VENDIDO.compativelCom(Modalidade.VENDA)).isTrue();
        assertThat(Resultado.VENDIDO.compativelCom(Modalidade.DOACAO)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(Modalidade.class)
    void naoInformadoCombinaComQualquerModalidade(Modalidade modalidade) {
        assertThat(Resultado.NAO_INFORMADO.compativelCom(modalidade)).isTrue();
    }
}
