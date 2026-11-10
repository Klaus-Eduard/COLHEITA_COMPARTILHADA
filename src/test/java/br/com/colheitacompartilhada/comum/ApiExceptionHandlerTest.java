package br.com.colheitacompartilhada.comum;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class ApiExceptionHandlerTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ControllerDeTeste())
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    @Test
    void corpoComCampoInvalidoRetorna400ComCampos() throws Exception {
        mvc.perform(post("/teste/corpo").contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.mensagem").value("Verifique os campos destacados."))
                .andExpect(jsonPath("$.campos.titulo").value("Informe o título."));
    }

    @Test
    void jsonMalformadoRetorna400SemCampos() throws Exception {
        mvc.perform(post("/teste/corpo").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.mensagem").value("Requisição inválida."))
                .andExpect(jsonPath("$.campos").doesNotExist());
    }

    @Test
    void parametroDeTipoErradoRetorna400() throws Exception {
        mvc.perform(get("/teste/numero/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.mensagem").value("Requisição inválida."));
    }

    @Test
    void regraLigadaACampoVaiEmCampos() throws Exception {
        mvc.perform(get("/teste/regra-com-campo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos.preco").value("Informe um preço maior que zero."));
    }

    @Test
    void regraSemCampoUsaOCodigoEAMensagemDaExcecao() throws Exception {
        mvc.perform(get("/teste/regra-sem-campo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PERFIL_SEM_CONTATO"))
                .andExpect(jsonPath("$.mensagem").value("Complete o perfil."))
                .andExpect(jsonPath("$.campos").doesNotExist());
    }

    @Test
    void naoEncontradoRetorna404() throws Exception {
        mvc.perform(get("/teste/nao-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    void acessoNegadoRetorna403() throws Exception {
        mvc.perform(get("/teste/acesso-negado"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    void estadoInvalidoRetorna409ComStatusAtual() throws Exception {
        mvc.perform(get("/teste/estado-invalido"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ESTADO_INVALIDO"))
                .andExpect(jsonPath("$.statusAtual").value("ENCERRADO"));
    }

    @Test
    void conflitoRetorna409ComOCodigoDaExcecao() throws Exception {
        mvc.perform(get("/teste/conflito"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EMAIL_JA_CADASTRADO"));
    }

    @Test
    void violacaoDoIndiceDeEmailRetorna409() throws Exception {
        mvc.perform(get("/teste/integridade/uq_usuario_email"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EMAIL_JA_CADASTRADO"));
    }

    @Test
    void violacaoDoIndiceDeDenunciaAbertaRetorna409() throws Exception {
        mvc.perform(get("/teste/integridade/uq_denuncia_aberta"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DENUNCIA_DUPLICADA"));
    }

    @Test
    void outraViolacaoDeIntegridadeRetorna500SemDetalhe() throws Exception {
        mvc.perform(get("/teste/integridade/anuncio_check"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"))
                .andExpect(content().string(not(containsString("anuncio_check"))));
    }

    @Test
    void arquivoGrandeDemaisRetorna413() throws Exception {
        mvc.perform(get("/teste/arquivo-grande"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.codigo").value("ARQUIVO_MUITO_GRANDE"));
    }

    @Test
    void metodoNaoSuportadoMantemOStatusDoSpring() throws Exception {
        mvc.perform(post("/teste/conflito"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.mensagem").value("Requisição inválida."));
    }

    @Test
    void erroInesperadoRetorna500SemVazarODetalhe() throws Exception {
        mvc.perform(get("/teste/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"))
                .andExpect(jsonPath("$.mensagem").value("Serviço indisponível. Tente novamente."))
                .andExpect(content().string(not(containsString("detalhe secreto"))));
    }

    record Corpo(@NotBlank(message = "Informe o título.") String titulo) {
    }

    @RestController
    static class ControllerDeTeste {

        @PostMapping("/teste/corpo")
        void corpo(@Valid @RequestBody Corpo corpo) {
        }

        @GetMapping("/teste/numero/{n}")
        void numero(@PathVariable int n) {
        }

        @GetMapping("/teste/regra-com-campo")
        void regraComCampo() {
            throw new RegraNegocioException("VALIDACAO", "preco", "Informe um preço maior que zero.");
        }

        @GetMapping("/teste/regra-sem-campo")
        void regraSemCampo() {
            throw new RegraNegocioException("PERFIL_SEM_CONTATO", null, "Complete o perfil.");
        }

        @GetMapping("/teste/nao-encontrado")
        void naoEncontrado() {
            throw new NaoEncontradoException("Anúncio não encontrado.");
        }

        @GetMapping("/teste/acesso-negado")
        void acessoNegado() {
            throw new AcessoNegadoException("Acesso negado.");
        }

        @GetMapping("/teste/estado-invalido")
        void estadoInvalido() {
            throw new EstadoInvalidoException("ENCERRADO", "O anúncio não está ativo.");
        }

        @GetMapping("/teste/conflito")
        void conflito() {
            throw new ConflitoException("EMAIL_JA_CADASTRADO", "Este e-mail já está cadastrado.");
        }

        @GetMapping("/teste/integridade/{restricao}")
        void integridade(@PathVariable String restricao) {
            throw new DataIntegrityViolationException("erro",
                    new RuntimeException("violates constraint \"" + restricao + "\""));
        }

        @GetMapping("/teste/arquivo-grande")
        void arquivoGrande() {
            throw new MaxUploadSizeExceededException(2_097_152);
        }

        @GetMapping("/teste/inesperado")
        void inesperado() {
            throw new IllegalStateException("detalhe secreto");
        }
    }
}
