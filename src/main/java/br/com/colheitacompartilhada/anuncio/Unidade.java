package br.com.colheitacompartilhada.anuncio;

public enum Unidade {
    KG(false), UNIDADE(true), LITRO(false), MACO(true), DUZIA(true);

    private final boolean inteira;

    Unidade(boolean inteira) {
        this.inteira = inteira;
    }

    public boolean isInteira() {
        return inteira;
    }
}
