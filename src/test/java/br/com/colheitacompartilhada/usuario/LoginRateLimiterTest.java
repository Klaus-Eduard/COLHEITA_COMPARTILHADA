package br.com.colheitacompartilhada.usuario;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import br.com.colheitacompartilhada.RelogioAjustavel;
import br.com.colheitacompartilhada.comum.MuitasTentativasException;
import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {

    private final RelogioAjustavel relogio = new RelogioAjustavel();
    private final LoginRateLimiter limiter = new LoginRateLimiter(relogio);

    @Test
    void permiteAteQuatroFalhas() {
        falhar("ana@x.com", 4);

        assertThatCode(() -> limiter.verificar("ana@x.com")).doesNotThrowAnyException();
    }

    @Test
    void bloqueiaDepoisDeCincoFalhas() {
        falhar("ana@x.com", 5);

        assertThatThrownBy(() -> limiter.verificar("ana@x.com")).isInstanceOf(MuitasTentativasException.class);
    }

    @Test
    void contaPorEmailSemDiferenciarMaiusculas() {
        falhar("ana@x.com", 5);

        assertThatThrownBy(() -> limiter.verificar(" ANA@X.com ")).isInstanceOf(MuitasTentativasException.class);
        assertThatCode(() -> limiter.verificar("bia@x.com")).doesNotThrowAnyException();
    }

    @Test
    void liberaQuandoAsFalhasSaemDaJanelaDe15Minutos() {
        falhar("ana@x.com", 5);
        relogio.avancar(Duration.ofMinutes(15));

        assertThatCode(() -> limiter.verificar("ana@x.com")).doesNotThrowAnyException();
    }

    @Test
    void continuaBloqueadoUmSegundoAntesDeCompletar15Minutos() {
        falhar("ana@x.com", 5);
        relogio.avancar(Duration.ofMinutes(15).minusSeconds(1));

        assertThatThrownBy(() -> limiter.verificar("ana@x.com")).isInstanceOf(MuitasTentativasException.class);
    }

    @Test
    void loginBemSucedidoZeraOContador() {
        falhar("ana@x.com", 4);
        limiter.limpar("ana@x.com");
        falhar("ana@x.com", 4);

        assertThatCode(() -> limiter.verificar("ana@x.com")).doesNotThrowAnyException();
    }

    private void falhar(String email, int vezes) {
        for (int i = 0; i < vezes; i++) {
            limiter.registrarFalha(email);
        }
    }
}
