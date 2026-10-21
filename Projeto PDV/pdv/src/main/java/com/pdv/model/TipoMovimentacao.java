package com.pdv.model;

/** Tipos de movimentação de estoque (iguais ao ENUM da tabela movimentacoes_estoque). */
public enum TipoMovimentacao {
    ENTRADA("Entrada", "Entrada (compra / reposição)"),
    DEVOLUCAO("Devolução", "Devolução de cliente"),
    PERDA("Perda", "Perda / quebra / vencimento"),
    AJUSTE("Ajuste", "Ajuste de inventário (contagem)"),
    SAIDA_VENDA("Venda", "Saída por venda");

    private final String rotulo;
    private final String descricao;

    TipoMovimentacao(String rotulo, String descricao) {
        this.rotulo = rotulo;
        this.descricao = descricao;
    }

    public String getRotulo() { return rotulo; }
    public String getDescricao() { return descricao; }

    /** Usado pelo combo box da tela. */
    @Override
    public String toString() { return descricao; }
}
