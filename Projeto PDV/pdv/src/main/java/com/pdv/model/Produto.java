package com.pdv.model;

import java.math.BigDecimal;

public class Produto {
    private Integer id;
    private String codigoBarras;
    private String nome;
    private Integer categoriaId;
    private String categoriaNome;
    private BigDecimal precoCusto = BigDecimal.ZERO;
    private BigDecimal precoVenda = BigDecimal.ZERO;
    private BigDecimal estoqueAtual = BigDecimal.ZERO;
    private BigDecimal estoqueMinimo = BigDecimal.ZERO;
    private String unidade = "UN";
    private boolean ativo = true;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCodigoBarras() { return codigoBarras; }
    public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public Integer getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Integer categoriaId) { this.categoriaId = categoriaId; }

    public String getCategoriaNome() { return categoriaNome; }
    public void setCategoriaNome(String categoriaNome) { this.categoriaNome = categoriaNome; }

    public BigDecimal getPrecoCusto() { return precoCusto; }
    public void setPrecoCusto(BigDecimal precoCusto) { this.precoCusto = precoCusto; }

    public BigDecimal getPrecoVenda() { return precoVenda; }
    public void setPrecoVenda(BigDecimal precoVenda) { this.precoVenda = precoVenda; }

    public BigDecimal getEstoqueAtual() { return estoqueAtual; }
    public void setEstoqueAtual(BigDecimal estoqueAtual) { this.estoqueAtual = estoqueAtual; }

    public BigDecimal getEstoqueMinimo() { return estoqueMinimo; }
    public void setEstoqueMinimo(BigDecimal estoqueMinimo) { this.estoqueMinimo = estoqueMinimo; }

    public String getUnidade() { return unidade; }
    public void setUnidade(String unidade) { this.unidade = unidade; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public boolean isEstoqueBaixo() {
        return estoqueMinimo.signum() > 0 && estoqueAtual.compareTo(estoqueMinimo) <= 0;
    }
}
