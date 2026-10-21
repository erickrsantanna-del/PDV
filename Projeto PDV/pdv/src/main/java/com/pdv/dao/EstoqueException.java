package com.pdv.dao;

/** Erro de regra de negócio do estoque (ex.: saldo insuficiente). A mensagem pode ser mostrada ao usuário. */
public class EstoqueException extends Exception {
    public EstoqueException(String mensagem) {
        super(mensagem);
    }
}
