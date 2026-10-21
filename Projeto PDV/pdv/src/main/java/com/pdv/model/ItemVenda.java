package com.pdv.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Item do carrinho. Guarda o preço do momento em que foi adicionado. */
public class ItemVenda {
    private Produto produto;
    private BigDecimal quantidade;
    private final BigDecimal precoUnitario;

    public ItemVenda(Produto produto, BigDecimal quantidade) {
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitario = produto.getPrecoVenda();
    }

    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }

    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }

    public BigDecimal getPrecoUnitario() { return precoUnitario; }

    public BigDecimal getTotal() {
        return precoUnitario.multiply(quantidade).setScale(2, RoundingMode.HALF_UP);
    }
}
