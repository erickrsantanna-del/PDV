package com.pdv.dao;

/** Erro de regra de negócio da venda (estoque insuficiente, status inválido...). A mensagem pode ser exibida. */
public class VendaException extends Exception {
    public VendaException(String mensagem) {
        super(mensagem);
    }
}
