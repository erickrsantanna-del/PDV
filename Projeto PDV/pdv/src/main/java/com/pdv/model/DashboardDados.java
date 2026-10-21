package com.pdv.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Todos os números exibidos no dashboard, carregados de uma vez. */
public class DashboardDados {

    public static class ItemTop {
        public String nome;
        public BigDecimal quantidade;
        public BigDecimal total;
    }

    public static class ItemCritico {
        public String nome;
        public BigDecimal atual;
        public BigDecimal minimo;
        public String unidade;
    }

    public LocalDate hoje = LocalDate.now();

    public BigDecimal vendasHoje = BigDecimal.ZERO;
    public int qtdVendasHoje;

    public BigDecimal vendasMes = BigDecimal.ZERO;
    public int qtdVendasMes;
    public BigDecimal lucroMes = BigDecimal.ZERO;

    public int produtosAtivos;
    public int estoqueBaixo;
    public BigDecimal valorCusto = BigDecimal.ZERO;
    public BigDecimal valorVenda = BigDecimal.ZERO;

    public List<LocalDate> dias = new ArrayList<>();
    public List<BigDecimal> valoresDias = new ArrayList<>();

    public List<ItemTop> maisVendidos = new ArrayList<>();
    public List<ItemCritico> criticos = new ArrayList<>();

    public BigDecimal ticketMedioMes() {
        if (qtdVendasMes == 0) return BigDecimal.ZERO;
        return vendasMes.divide(BigDecimal.valueOf(qtdVendasMes), 2, java.math.RoundingMode.HALF_UP);
    }
}
