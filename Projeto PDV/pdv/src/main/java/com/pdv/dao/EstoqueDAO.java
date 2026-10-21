package com.pdv.dao;

import com.pdv.config.Database;
import com.pdv.model.MovimentacaoEstoque;
import com.pdv.model.TipoMovimentacao;
import com.pdv.util.Numeros;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EstoqueDAO {

    /**
     * Registra uma movimentação e atualiza o estoque do produto, tudo em uma transação.
     *
     * @param quantidade ENTRADA/DEVOLUCAO/PERDA: quantidade movimentada (sempre positiva).
     *                   AJUSTE: quantidade REAL contada; o sistema calcula a diferença.
     * @return o novo estoque do produto
     */
    public BigDecimal registrar(int produtoId, TipoMovimentacao tipo, BigDecimal quantidade,
                                int usuarioId, String observacao) throws SQLException, EstoqueException {
        try (Connection con = Database.getConnection()) {
            con.setAutoCommit(false);
            try {
                // Trava a linha do produto para evitar duas alterações ao mesmo tempo
                BigDecimal atual;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT estoque_atual FROM produtos WHERE id = ? FOR UPDATE")) {
                    ps.setInt(1, produtoId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new EstoqueException("Produto não encontrado.");
                        }
                        atual = rs.getBigDecimal(1);
                    }
                }

                BigDecimal delta;
                switch (tipo) {
                    case ENTRADA:
                    case DEVOLUCAO:
                        delta = quantidade;
                        break;
                    case PERDA:
                        delta = quantidade.negate();
                        break;
                    case AJUSTE:
                        delta = quantidade.subtract(atual);
                        break;
                    default:
                        throw new IllegalArgumentException("Tipo não permitido aqui: " + tipo);
                }

                if (delta.signum() == 0) {
                    throw new EstoqueException("Não há diferença: o estoque já é " + Numeros.decimal(atual, 3) + ".");
                }
                BigDecimal novo = atual.add(delta);
                if (novo.signum() < 0) {
                    throw new EstoqueException("Estoque insuficiente. Estoque atual: "
                            + Numeros.decimal(atual, 3) + ".");
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE produtos SET estoque_atual = ? WHERE id = ?")) {
                    ps.setBigDecimal(1, novo);
                    ps.setInt(2, produtoId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO movimentacoes_estoque (produto_id, tipo, quantidade, usuario_id, observacao) "
                      + "VALUES (?,?,?,?,?)")) {
                    ps.setInt(1, produtoId);
                    ps.setString(2, tipo.name());
                    ps.setBigDecimal(3, delta);
                    ps.setInt(4, usuarioId);
                    ps.setString(5, (observacao == null || observacao.trim().isEmpty()) ? null : observacao.trim());
                    ps.executeUpdate();
                }

                con.commit();
                return novo;
            } catch (SQLException | EstoqueException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public List<MovimentacaoEstoque> listarPorProduto(int produtoId, int limite) throws SQLException {
        String sql = "SELECT m.id, m.produto_id, p.nome AS produto_nome, m.tipo, m.quantidade, "
                   + "u.nome AS usuario_nome, m.observacao, m.criada_em "
                   + "FROM movimentacoes_estoque m "
                   + "JOIN produtos p ON p.id = m.produto_id "
                   + "JOIN usuarios u ON u.id = m.usuario_id "
                   + "WHERE m.produto_id = ? "
                   + "ORDER BY m.criada_em DESC, m.id DESC LIMIT ?";
        List<MovimentacaoEstoque> lista = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, produtoId);
            ps.setInt(2, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MovimentacaoEstoque m = new MovimentacaoEstoque();
                    m.setId(rs.getLong("id"));
                    m.setProdutoId(rs.getInt("produto_id"));
                    m.setProdutoNome(rs.getString("produto_nome"));
                    m.setTipo(TipoMovimentacao.valueOf(rs.getString("tipo")));
                    m.setQuantidade(rs.getBigDecimal("quantidade"));
                    m.setUsuarioNome(rs.getString("usuario_nome"));
                    m.setObservacao(rs.getString("observacao"));
                    m.setCriadaEm(rs.getTimestamp("criada_em").toLocalDateTime());
                    lista.add(m);
                }
            }
        }
        return lista;
    }
}
