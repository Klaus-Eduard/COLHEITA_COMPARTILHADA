package br.com.colheitacompartilhada.comum;

public class EstadoInvalidoException extends RuntimeException {

    private final String statusAtual;

    public EstadoInvalidoException(String statusAtual, String mensagem) {
        super(mensagem);
        this.statusAtual = statusAtual;
    }

    public String getStatusAtual() {
        return statusAtual;
    }
}
