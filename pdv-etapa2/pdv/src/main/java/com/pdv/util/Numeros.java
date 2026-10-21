package com.pdv.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public final class Numeros {

    private static final Locale BR = new Locale("pt", "BR");

    private Numeros() { }

    /** Aceita "12,50", "12.50" e "1.234,56". Vazio vira zero. */
    public static BigDecimal parse(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        String t = texto.trim().replace("R$", "").replace(" ", "");
        if (t.contains(",")) {
            t = t.replace(".", "").replace(",", ".");
        }
        return new BigDecimal(t);
    }

    public static String moeda(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(BR).format(valor);
    }

    /** Formata para campos de edição: 12,50 */
    public static String decimal(BigDecimal valor, int casas) {
        return valor.setScale(casas, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}
