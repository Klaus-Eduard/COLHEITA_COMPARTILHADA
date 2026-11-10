package br.com.colheitacompartilhada.comum;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResposta(int status, String codigo, String mensagem, Map<String, String> campos, String statusAtual) {

    public static ErroResposta de(int status, String codigo, String mensagem) {
        return new ErroResposta(status, codigo, mensagem, null, null);
    }

    public static ErroResposta comCampos(int status, String codigo, String mensagem, Map<String, String> campos) {
        return new ErroResposta(status, codigo, mensagem, campos, null);
    }

    public static ErroResposta estadoInvalido(String mensagem, String statusAtual) {
        return new ErroResposta(409, "ESTADO_INVALIDO", mensagem, null, statusAtual);
    }
}
