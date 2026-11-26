package br.com.colheitacompartilhada.usuario;

import java.util.regex.Pattern;

import br.com.colheitacompartilhada.comum.RegraNegocioException;

/** E05: WhatsApp gravado só com dígitos, 55 + DDD + número. A tela pode enviar com máscara. */
public final class Whatsapp {

    private static final Pattern FORMATO_FINAL = Pattern.compile("^55[1-9][0-9]{9,10}$");

    private Whatsapp() {
    }

    /** Devolve null quando o número veio vazio (remover o WhatsApp é permitido). */
    public static String normalizar(String informado) {
        if (informado == null || informado.isBlank()) {
            return null;
        }
        String digitos = informado.replaceAll("\\D", "");
        String comPais = digitos.length() == 10 || digitos.length() == 11 ? "55" + digitos : digitos;
        if (!FORMATO_FINAL.matcher(comPais).matches()) {
            throw new RegraNegocioException("VALIDACAO", "whatsapp", "Informe o WhatsApp com DDD.");
        }
        return comPais;
    }

    /** Mostra o número gravado (55 + DDD + número) como "(47) 99999-8888". */
    public static String formatar(String gravado) {
        String semPais = gravado.substring(2);
        String ddd = semPais.substring(0, 2);
        String numero = semPais.substring(2);
        int corte = numero.length() - 4;
        return "(" + ddd + ") " + numero.substring(0, corte) + "-" + numero.substring(corte);
    }
}
