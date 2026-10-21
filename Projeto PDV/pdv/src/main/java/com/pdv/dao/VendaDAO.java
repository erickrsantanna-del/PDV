package com.pdv.dao;

import com.pdv.config.Database;
import com.pdv.model.ItemVenda;
import com.pdv.model.MetodoPagamento;
import com.pdv.model.OrigemConfirmacao;
import com.pdv.util.Numeros;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Fluxo da venda:
 *   1) criarVenda                  -> AGUARDANDO_PAGAMENTO (estoque NÃO muda)
 *   2) registrarPagamentoPendente  -> pagamento PENDENTE
 *   3) confirmarPagamento          -> pagamento CONFIRMADO; se cobrir o total, a venda vira
 *                                     CONCLUIDA e o estoque baixa (tudo na mesma transação)
 *   cancelarVenda                  -> CANCELADA (se ainda não concluída)
 */
public class VendaDAO {

    /** Cria a venda com seus itens aguardando pagamento. Retorna o id da venda. */
    public long criarVenda(List<ItemVenda> itens, BigDecimal desconto, int usuarioId)
            throws SQLException, VendaException {
        if (itens.isEmpty()) {
            throw new VendaException("O carrinho está vazio.");
        }
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemVenda i : itens) {
            subtotal = subtotal.add(i.getTotal());
        }
        if (desconto.signum() < 0 || desconto.compareTo(subtotal) > 0) {
            throw new VendaException("Desconto inválido.");
        }
        BigDecimal total = subtotal.subtract(desconto);

        try (Connection con = Database.getConnection()) {
            con.setAutoCommit(false);
            try {
                // Confere se ainda há estoque (a baixa definitiva acontece na confirmação)
                for (ItemVenda i : itens) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT estoque_atual FROM produtos WHERE id = ? AND ativo = 1")) {
                        ps.setInt(1, i.getProduto().getId());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                throw new VendaException("Produto inativo ou inexistente: " + i.getProduto().getNome());
                            }
                            if (rs.getBigDecimal(1).compareTo(i.getQuantidade()) < 0) {
                                throw new VendaException("Estoque insuficiente para \"" + i.getProduto().getNome()
                                        + "\". Disponível: " + Numeros.decimal(rs.getBigDecimal(1), 3) + ".");
                            }
                        }
                    }
                }

                long vendaId;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO vendas (usuario_id, status, subtotal, desconto, total) "
                      + "VALUES (?, 'AGUARDANDO_PAGAMENTO', ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, usuarioId);
                    ps.setBigDecimal(2, subtotal);
                    ps.setBigDecimal(3, desconto);
                    ps.setBigDecimal(4, total);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        vendaId = rs.getLong(1);
                    }
                }

                // O custo é lido do cadastro no momento da venda (para o cálculo de lucro)
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO venda_itens (venda_id, produto_id, quantidade, preco_unitario, custo_unitario, total_item) "
                      + "SELECT ?, id, ?, ?, preco_custo, ? FROM produtos WHERE id = ?")) {
                    for (ItemVenda i : itens) {
                        ps.setLong(1, vendaId);
                        ps.setBigDecimal(2, i.getQuantidade());
                        ps.setBigDecimal(3, i.getPrecoUnitario());
                        ps.setBigDecimal(4, i.getTotal());
                        ps.setInt(5, i.getProduto().getId());
                        ps.executeUpdate();
                    }
                }

                con.commit();
                return vendaId;
            } catch (SQLException | VendaException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Registra o pagamento como PENDENTE. Retorna o id do pagamento. */
    public long registrarPagamentoPendente(long vendaId, MetodoPagamento metodo, BigDecimal valor, int parcelas)
            throws SQLException {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO pagamentos (venda_id, metodo, valor, parcelas, status) VALUES (?,?,?,?,'PENDENTE')",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, vendaId);
            ps.setString(2, metodo.name());
            ps.setBigDecimal(3, valor);
            ps.setInt(4, parcelas);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    /**
     * Confirma o pagamento. Se os pagamentos confirmados cobrirem o total, conclui a venda
     * e baixa o estoque. Se faltar estoque, nada é gravado e a VendaException explica o motivo.
     *
     * @return true se a venda foi concluída
     */
    public boolean confirmarPagamento(long pagamentoId, OrigemConfirmacao origem, Integer usuarioId,
                                      String motivoManual, String idExterno, String nsu, String autorizacao)
            throws SQLException, VendaException {
        try (Connection con = Database.getConnection()) {
            con.setAutoCommit(false);
            try {
                long vendaId;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT venda_id, status FROM pagamentos WHERE id = ? FOR UPDATE")) {
                    ps.setLong(1, pagamentoId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new VendaException("Pagamento não encontrado.");
                        }
                        if (!"PENDENTE".equals(rs.getString("status"))) {
                            throw new VendaException("Este pagamento não está pendente (status: " + rs.getString("status") + ").");
                        }
                        vendaId = rs.getLong("venda_id");
                    }
                }

                BigDecimal totalVenda;
                int usuarioVenda;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT status, total, usuario_id FROM vendas WHERE id = ? FOR UPDATE")) {
                    ps.setLong(1, vendaId);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        if (!"AGUARDANDO_PAGAMENTO".equals(rs.getString("status"))) {
                            throw new VendaException("A venda não está aguardando pagamento (status: "
                                    + rs.getString("status") + ").");
                        }
                        totalVenda = rs.getBigDecimal("total");
                        usuarioVenda = rs.getInt("usuario_id");
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE pagamentos SET status = 'CONFIRMADO', origem_confirmacao = ?, confirmado_por = ?, "
                      + "motivo_manual = ?, id_externo = COALESCE(?, id_externo), nsu = ?, codigo_autorizacao = ?, "
                      + "confirmado_em = NOW() WHERE id = ?")) {
                    ps.setString(1, origem.name());
                    if (usuarioId == null) {
                        ps.setNull(2, Types.INTEGER);
                    } else {
                        ps.setInt(2, usuarioId);
                    }
                    ps.setString(3, vazioParaNulo(motivoManual));
                    ps.setString(4, vazioParaNulo(idExterno));
                    ps.setString(5, vazioParaNulo(nsu));
                    ps.setString(6, vazioParaNulo(autorizacao));
                    ps.setLong(7, pagamentoId);
                    ps.executeUpdate();
                }

                BigDecimal pago;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT COALESCE(SUM(valor),0) FROM pagamentos WHERE venda_id = ? AND status = 'CONFIRMADO'")) {
                    ps.setLong(1, vendaId);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        pago = rs.getBigDecimal(1);
                    }
                }

                boolean concluida = pago.compareTo(totalVenda) >= 0;
                if (concluida) {
                    concluirVenda(con, vendaId, usuarioVenda);
                }
                con.commit();
                return concluida;
            } catch (SQLException | VendaException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Marca o pagamento como RECUSADO, CANCELADO ou EXPIRADO (a venda continua aguardando). */
    public void encerrarPagamento(long pagamentoId, String novoStatus) throws SQLException {
        if (!novoStatus.equals("RECUSADO") && !novoStatus.equals("CANCELADO") && !novoStatus.equals("EXPIRADO")) {
            throw new IllegalArgumentException("Status inválido: " + novoStatus);
        }
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE pagamentos SET status = ? WHERE id = ? AND status = 'PENDENTE'")) {
            ps.setString(1, novoStatus);
            ps.setLong(2, pagamentoId);
            ps.executeUpdate();
        }
    }

    /** Cancela a venda (e seus pagamentos pendentes), desde que ainda não esteja concluída. */
    public void cancelarVenda(long vendaId) throws SQLException {
        try (Connection con = Database.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE vendas SET status = 'CANCELADA' WHERE id = ? AND status IN ('ABERTA','AGUARDANDO_PAGAMENTO')")) {
                    ps.setLong(1, vendaId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE pagamentos SET status = 'CANCELADO' WHERE venda_id = ? AND status = 'PENDENTE'")) {
                    ps.setLong(1, vendaId);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // ------------------------------------------------------------------

    /** Baixa o estoque dos itens e marca a venda como CONCLUIDA (na transação já aberta). */
    private void concluirVenda(Connection con, long vendaId, int usuarioId) throws SQLException, VendaException {
        List<Integer> produtoIds = new ArrayList<>();
        List<BigDecimal> quantidades = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT produto_id, SUM(quantidade) FROM venda_itens WHERE venda_id = ? "
              + "GROUP BY produto_id ORDER BY produto_id")) {
            ps.setLong(1, vendaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    produtoIds.add(rs.getInt(1));
                    quantidades.add(rs.getBigDecimal(2));
                }
            }
        }

        for (int i = 0; i < produtoIds.size(); i++) {
            int produtoId = produtoIds.get(i);
            BigDecimal qtd = quantidades.get(i);

            String nome;
            BigDecimal atual;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT nome, estoque_atual FROM produtos WHERE id = ? FOR UPDATE")) {
                ps.setInt(1, produtoId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    nome = rs.getString(1);
                    atual = rs.getBigDecimal(2);
                }
            }
            if (atual.compareTo(qtd) < 0) {
                throw new VendaException("Estoque insuficiente para \"" + nome + "\". Disponível: "
                        + Numeros.decimal(atual, 3) + ".");
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE produtos SET estoque_atual = estoque_atual - ? WHERE id = ?")) {
                ps.setBigDecimal(1, qtd);
                ps.setInt(2, produtoId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO movimentacoes_estoque (produto_id, tipo, quantidade, venda_id, usuario_id, observacao) "
                  + "VALUES (?, 'SAIDA_VENDA', ?, ?, ?, ?)")) {
                ps.setInt(1, produtoId);
                ps.setBigDecimal(2, qtd.negate());
                ps.setLong(3, vendaId);
                ps.setInt(4, usuarioId);
                ps.setString(5, "Venda #" + vendaId);
                ps.executeUpdate();
            }
        }

        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE vendas SET status = 'CONCLUIDA', concluida_em = NOW() WHERE id = ?")) {
            ps.setLong(1, vendaId);
            ps.executeUpdate();
        }
    }

    private static String vazioParaNulo(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }
}
