package com.pdv.model;

/** Formas de pagamento (nomes iguais ao ENUM da tabela pagamentos). */
public enum MetodoPagamento {
    DINHEIRO("Dinheiro"),
    CARTAO_DEBITO("Cartão de débito"),
    CARTAO_CREDITO("Cartão de crédito"),
    PIX("PIX");

    private final String rotulo;

    MetodoPagamento(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() { return rotulo; }

    @Override
    public String toString() { return rotulo; }
}
