package br.com.colheitacompartilhada.comum;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private static final String VALIDACAO = "VALIDACAO";
    private static final String MSG_CAMPOS = "Verifique os campos destacados.";
    private static final String MSG_REQUISICAO = "Requisição inválida.";
    private static final String MSG_NAO_ENCONTRADO = "Recurso não encontrado.";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroResposta> corpoInvalido(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        return responder(ErroResposta.comCampos(400, VALIDACAO, MSG_CAMPOS, campos));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ErroResposta> parametroInvalido(HandlerMethodValidationException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(resultado -> resultado.getResolvableErrors().stream()
                .findFirst()
                .ifPresent(erro -> campos.putIfAbsent(
                        resultado.getMethodParameter().getParameterName(), erro.getDefaultMessage())));
        return responder(ErroResposta.comCampos(400, VALIDACAO, MSG_CAMPOS, campos));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErroResposta> requisicaoInvalida(Exception ex) {
        return responder(ErroResposta.de(400, VALIDACAO, MSG_REQUISICAO));
    }

    @ExceptionHandler(RegraNegocioException.class)
    ResponseEntity<ErroResposta> regraNegocio(RegraNegocioException ex) {
        if (ex.getCampo() == null) {
            return responder(ErroResposta.de(400, ex.getCodigo(), ex.getMessage()));
        }
        return responder(ErroResposta.comCampos(400, ex.getCodigo(), MSG_CAMPOS,
                Map.of(ex.getCampo(), ex.getMessage())));
    }

    @ExceptionHandler(NaoEncontradoException.class)
    ResponseEntity<ErroResposta> naoEncontrado(NaoEncontradoException ex) {
        return responder(ErroResposta.de(404, "NAO_ENCONTRADO", ex.getMessage()));
    }

    @ExceptionHandler(AcessoNegadoException.class)
    ResponseEntity<ErroResposta> acessoNegado(AcessoNegadoException ex) {
        return responder(ErroResposta.de(403, "ACESSO_NEGADO", ex.getMessage()));
    }

    @ExceptionHandler(EstadoInvalidoException.class)
    ResponseEntity<ErroResposta> estadoInvalido(EstadoInvalidoException ex) {
        return responder(ErroResposta.estadoInvalido(ex.getMessage(), ex.getStatusAtual()));
    }

    @ExceptionHandler(ConflitoException.class)
    ResponseEntity<ErroResposta> conflito(ConflitoException ex) {
        return responder(ErroResposta.de(409, ex.getCodigo(), ex.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErroResposta> credenciaisInvalidas(AuthenticationException ex) {
        return responder(ErroResposta.de(401, "CREDENCIAIS_INVALIDAS", "E-mail ou senha inválidos."));
    }

    @ExceptionHandler(MuitasTentativasException.class)
    ResponseEntity<ErroResposta> muitasTentativas(MuitasTentativasException ex) {
        return responder(ErroResposta.de(429, "MUITAS_TENTATIVAS", ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErroResposta> violacaoDeIntegridade(DataIntegrityViolationException ex) {
        String detalhe = String.valueOf(ex.getMostSpecificCause().getMessage());
        if (detalhe.contains("uq_usuario_email")) {
            return responder(ErroResposta.de(409, "EMAIL_JA_CADASTRADO", "E-mail já cadastrado."));
        }
        if (detalhe.contains("uq_denuncia_aberta")) {
            return responder(ErroResposta.de(409, "DENUNCIA_DUPLICADA",
                    "Você já tem uma denúncia aberta para este anúncio."));
        }
        return violacaoNaoMapeada(ex);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErroResposta> arquivoMuitoGrande(MaxUploadSizeExceededException ex) {
        return responder(ErroResposta.de(413, "ARQUIVO_MUITO_GRANDE", "A foto deve ter no máximo 2 MB."));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErroResposta> inesperado(Exception ex) {
        if (ex instanceof ErrorResponse erroDoSpring && erroDoSpring.getStatusCode().is4xxClientError()) {
            return erroDeRequisicao(erroDoSpring.getStatusCode().value());
        }
        return erroInterno(ex);
    }

    private ResponseEntity<ErroResposta> erroDeRequisicao(int status) {
        if (status == HttpStatus.NOT_FOUND.value()) {
            return responder(ErroResposta.de(404, "NAO_ENCONTRADO", MSG_NAO_ENCONTRADO));
        }
        return responder(ErroResposta.de(status, VALIDACAO, MSG_REQUISICAO));
    }

    // A mensagem do banco pode trazer a linha inteira (e-mail, WhatsApp, hash); por isso só o nome da restrição vai para o log.
    private ResponseEntity<ErroResposta> violacaoNaoMapeada(DataIntegrityViolationException ex) {
        String restricao = ex.getCause() instanceof ConstraintViolationException causa ? causa.getConstraintName() : null;
        log.error("Violação de integridade não mapeada. Restrição: {}", restricao);
        return responder(erroInterno());
    }

    private ResponseEntity<ErroResposta> erroInterno(Exception ex) {
        log.error("Erro inesperado", ex);
        return responder(erroInterno());
    }

    private ErroResposta erroInterno() {
        return ErroResposta.de(500, "ERRO_INTERNO", "Serviço indisponível. Tente novamente.");
    }

    private ResponseEntity<ErroResposta> responder(ErroResposta erro) {
        return ResponseEntity.status(erro.status()).body(erro);
    }
}
