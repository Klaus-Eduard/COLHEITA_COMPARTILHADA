package br.com.colheitacompartilhada.anuncio;

public enum Resultado {
    DOADO, VENDIDO, NAO_INFORMADO;

    public boolean compativelCom(Modalidade modalidade) {
        return switch (this) {
            case DOADO -> modalidade == Modalidade.DOACAO;
            case VENDIDO -> modalidade == Modalidade.VENDA;
            case NAO_INFORMADO -> true;
        };
    }
}
