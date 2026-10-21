package com.pdv.dao;

import com.pdv.config.Database;
import com.pdv.model.DashboardDados;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/** Consultas de estatísticas. Só considera vendas com status CONCLUIDA. */
public class DashboardDAO {

    private static final String INICIO_MES = "CURDATE() - INTERVAL (DAYOFMONTH(CURDATE()) - 1) DAY";

    public DashboardDados carregar() throws SQLException {
        DashboardDados d = new DashboardDados();

        try (Connection con = Database.getConnection()) {

            // Data de hoje segundo o banco (evita diferença de fuso)
            try (PreparedStatement ps = con.prepareStatement("SELECT CURDATE()");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                d.hoje = rs.getDate(1).toLocalDate();
            }

            // Vendas de hoje
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COALESCE(SUM(total),0), COUNT(*) FROM vendas "
                  + "WHERE status = 'CONCLUIDA' AND concluida_em >= CURDATE() "
                  + "AND concluida_em < CURDATE() + INTERVAL 1 DAY");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                d.vendasHoje = rs.getBigDecimal(1);
                d.qtdVendasHoje = rs.getInt(2);
            }

            // Vendas do mês
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COALESCE(SUM(total),0), COUNT(*) FROM vendas "
                  + "WHERE status = 'CONCLUIDA' AND concluida_em >= " + INICIO_MES);
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                d.vendasMes = rs.getBigDecimal(1);
                d.qtdVendasMes = rs.getInt(2);
            }

            // Lucro do mês = vendas (já com desconto) - custo dos itens vendidos
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COALESCE(SUM(vi.custo_unitario * vi.quantidade),0) "
                  + "FROM venda_itens vi JOIN vendas v ON v.id = vi.venda_id "
                  + "WHERE v.status = 'CONCLUIDA' AND v.concluida_em >= " + INICIO_MES);
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                d.lucroMes = d.vendasMes.subtract(rs.getBigDecimal(1));
            }

            // Produtos e valor do estoque
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COUNT(*), COALESCE(SUM(estoque_atual * preco_custo),0), "
                  + "COALESCE(SUM(estoque_atual * preco_venda),0), "
                  + "COALESCE(SUM(CASE WHEN estoque_atual <= 0 OR (estoque_minimo > 0 AND estoque_atual <= estoque_minimo) "
                  + "THEN 1 ELSE 0 END),0) FROM produtos WHERE ativo = 1");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                d.produtosAtivos = rs.getInt(1);
                d.valorCusto = rs.getBigDecimal(2);
                d.valorVenda = rs.getBigDecimal(3);
                d.estoqueBaixo = rs.getInt(4);
            }

            // Vendas dos últimos 7 dias
            Map<LocalDate, BigDecimal> porDia = new HashMap<>();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT DATE(concluida_em) AS dia, SUM(total) AS total FROM vendas "
                  + "WHERE status = 'CONCLUIDA' AND concluida_em >= CURDATE() - INTERVAL 6 DAY "
                  + "GROUP BY DATE(concluida_em)");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    porDia.put(rs.getDate("dia").toLocalDate(), rs.getBigDecimal("total"));
                }
            }
            for (int i = 6; i >= 0; i--) {
                LocalDate dia = d.hoje.minusDays(i);
                d.dias.add(dia);
                d.valoresDias.add(porDia.getOrDefault(dia, BigDecimal.ZERO));
            }

            // Mais vendidos (30 dias)
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT p.nome, SUM(vi.quantidade) AS qtd, SUM(vi.total_item) AS total "
                  + "FROM venda_itens vi "
                  + "JOIN vendas v ON v.id = vi.venda_id "
                  + "JOIN produtos p ON p.id = vi.produto_id "
                  + "WHERE v.status = 'CONCLUIDA' AND v.concluida_em >= CURDATE() - INTERVAL 29 DAY "
                  + "GROUP BY p.id, p.nome ORDER BY SUM(vi.total_item) DESC LIMIT 5");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DashboardDados.ItemTop it = new DashboardDados.ItemTop();
                    it.nome = rs.getString("nome");
                    it.quantidade = rs.getBigDecimal("qtd");
                    it.total = rs.getBigDecimal("total");
                    d.maisVendidos.add(it);
                }
            }

            // Estoque crítico
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT nome, estoque_atual, estoque_minimo, unidade FROM produtos "
                  + "WHERE ativo = 1 AND (estoque_atual <= 0 OR (estoque_minimo > 0 AND estoque_atual <= estoque_minimo)) "
                  + "ORDER BY estoque_atual ASC, nome LIMIT 6");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DashboardDados.ItemCritico it = new DashboardDados.ItemCritico();
                    it.nome = rs.getString("nome");
                    it.atual = rs.getBigDecimal("estoque_atual");
                    it.minimo = rs.getBigDecimal("estoque_minimo");
                    it.unidade = rs.getString("unidade");
                    d.criticos.add(it);
                }
            }
        }
        return d;
    }
}
