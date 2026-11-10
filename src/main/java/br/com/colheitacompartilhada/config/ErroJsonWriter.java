package br.com.colheitacompartilhada.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import br.com.colheitacompartilhada.comum.ErroResposta;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/** Escreve o ErroResposta nos pontos da segurança que respondem antes de a requisição chegar a um controller. */
@Component
public class ErroJsonWriter {

    private final ObjectMapper objectMapper;

    public ErroJsonWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void escrever(HttpServletResponse response, ErroResposta erro) throws IOException {
        response.setStatus(erro.status());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), erro);
    }

    public void naoAutenticado(HttpServletResponse response) throws IOException {
        escrever(response, ErroResposta.de(401, "NAO_AUTENTICADO", "Entre para continuar."));
    }

    public void acessoNegado(HttpServletResponse response) throws IOException {
        escrever(response, ErroResposta.de(403, "ACESSO_NEGADO", "Acesso negado."));
    }
}
