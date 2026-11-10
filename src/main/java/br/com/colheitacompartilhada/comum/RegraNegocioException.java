package br.com.colheitacompartilhada.comum;

public class RegraNegocioException extends RuntimeException {

    private final String codigo;
    private final String campo;

    public RegraNegocioException(String codigo, String campo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
        this.campo = campo;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getCampo() {
        return campo;
    }
}
