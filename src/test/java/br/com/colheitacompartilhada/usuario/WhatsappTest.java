package br.com.colheitacompartilhada.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.colheitacompartilhada.comum.RegraNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** E05 e tabela de validação (02): normalização do WhatsApp. */
class WhatsappTest {

    @ParameterizedTest
    @CsvSource({
            "'(47) 99999-8888', 5547999998888",
            "47999998888, 5547999998888",
            "'(47) 3333-4444', 554733334444",
            "'+55 47 99999-8888', 5547999998888",
            "554733334444, 554733334444"})
    void normalizaParaDdiMaisDddMaisNumero(String informado, String gravado) {
        assertThat(Whatsapp.normalizar(informado)).isEqualTo(gravado);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void vazioRemoveONumero(String informado) {
        assertThat(Whatsapp.normalizar(informado)).isNull();
        assertThat(Whatsapp.normalizar(null)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"99999-8888", "(07) 99999-8888", "1 47 99999-8888", "5547999998888123", "abc"})
    void numeroSemDddOuForaDoFormatoERecusado(String informado) {
        assertThatThrownBy(() -> Whatsapp.normalizar(informado))
                .isInstanceOfSatisfying(RegraNegocioException.class, erro -> {
                    assertThat(erro.getCampo()).isEqualTo("whatsapp");
                    assertThat(erro.getMessage()).isEqualTo("Informe o WhatsApp com DDD.");
                });
    }
}
