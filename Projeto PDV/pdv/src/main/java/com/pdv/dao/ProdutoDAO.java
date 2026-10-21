package com.pdv.dao;

import com.pdv.config.Database;
import com.pdv.model.Produto;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ProdutoDAO {

    private static final String SELECT_BASE =
            "SELECT p.id, p.codigo_barras, p.nome, p.categoria_id, c.nome AS categoria_nome, "
          + "p.preco_custo, p.preco_venda, p.estoque_atual, p.estoque_minimo, p.unidade, p.ativo "
          + "FROM produtos p LEFT JOIN categorias c ON c.id = p.categoria_id ";

    public List<Produto> listar(String filtro, boolean incluirInativos) throws SQLException {
        String sql = SELECT_BASE
                + "WHERE (p.nome LIKE ? OR p.codigo_barras LIKE ?) "
                + (incluirInativos ? "" : "AND p.ativo = 1 ")
                + "ORDER BY p.nome LIMIT 1000";
        String like = "%" + (filtro == null ? "" : filtro.trim()) + "%";

        List<Produto> lista = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    /** Busca exata pelo código de barras, somente produtos ativos. Retorna null se não achar. */
    public Produto buscarPorCodigo(String codigo) throws SQLException {
        String sql = SELECT_BASE + "WHERE p.codigo_barras = ? AND p.ativo = 1";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public boolean existeCodigoBarras(String codigo, Integer idIgnorar) throws SQLException {
        String sql = "SELECT COUNT(*) FROM produtos WHERE codigo_barras = ? AND id <> ?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigo);
            ps.setInt(2, idIgnorar == null ? -1 : idIgnorar);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Insere o produto. Se houver estoque inicial, registra uma movimentação de ENTRADA
     * na mesma transação (ou grava tudo, ou não grava nada).
     */
    public int inserir(Produto p, BigDecimal estoqueInicial, int usuarioId) throws SQLException {
        String sql = "INSERT INTO produtos (codigo_barras, nome, categoria_id, preco_custo, preco_venda, "
                   + "estoque_atual, estoque_minimo, unidade, ativo) VALUES (?,?,?,?,?,?,?,?,1)";
        try (Connection con = Database.getConnection()) {
            con.setAutoCommit(false);
            try {
                int novoId;
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    preencher(ps, p);
                    ps.setBigDecimal(6, estoqueInicial);
                    ps.setBigDecimal(7, p.getEstoqueMinimo());
                    ps.setString(8, p.getUnidade());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        novoId = rs.getInt(1);
                    }
                }
                if (estoqueInicial.signum() > 0) {
                    String mov = "INSERT INTO movimentacoes_estoque (produto_id, tipo, quantidade, usuario_id, observacao) "
                               + "VALUES (?, 'ENTRADA', ?, ?, 'Estoque inicial no cadastro')";
                    try (PreparedStatement ps = con.prepareStatement(mov)) {
                        ps.setInt(1, novoId);
                        ps.setBigDecimal(2, estoqueInicial);
                        ps.setInt(3, usuarioId);
                        ps.executeUpdate();
                    }
                }
                con.commit();
                return novoId;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Atualiza os dados do produto. O estoque NÃO é alterado aqui (vai por movimentação, Etapa 4). */
    public void atualizar(Produto p) throws SQLException {
        String sql = "UPDATE produtos SET codigo_barras=?, nome=?, categoria_id=?, preco_custo=?, "
                   + "preco_venda=?, estoque_minimo=?, unidade=? WHERE id=?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            preencher(ps, p);
            ps.setBigDecimal(6, p.getEstoqueMinimo());
            ps.setString(7, p.getUnidade());
            ps.setInt(8, p.getId());
            ps.executeUpdate();
        }
    }

    public void definirAtivo(int id, boolean ativo) throws SQLException {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE produtos SET ativo=? WHERE id=?")) {
            ps.setBoolean(1, ativo);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /** Preenche os 5 primeiros parâmetros comuns: código, nome, categoria, custo, venda. */
    private void preencher(PreparedStatement ps, Produto p) throws SQLException {
        ps.setString(1, p.getCodigoBarras());
        ps.setString(2, p.getNome());
        if (p.getCategoriaId() == null) {
            ps.setNull(3, Types.INTEGER);
        } else {
            ps.setInt(3, p.getCategoriaId());
        }
        ps.setBigDecimal(4, p.getPrecoCusto());
        ps.setBigDecimal(5, p.getPrecoVenda());
    }

    private Produto mapear(ResultSet rs) throws SQLException {
        Produto p = new Produto();
        p.setId(rs.getInt("id"));
        p.setCodigoBarras(rs.getString("codigo_barras"));
        p.setNome(rs.getString("nome"));
        int cat = rs.getInt("categoria_id");
        p.setCategoriaId(rs.wasNull() ? null : cat);
        p.setCategoriaNome(rs.getString("categoria_nome"));
        p.setPrecoCusto(rs.getBigDecimal("preco_custo"));
        p.setPrecoVenda(rs.getBigDecimal("preco_venda"));
        p.setEstoqueAtual(rs.getBigDecimal("estoque_atual"));
        p.setEstoqueMinimo(rs.getBigDecimal("estoque_minimo"));
        p.setUnidade(rs.getString("unidade"));
        p.setAtivo(rs.getBoolean("ativo"));
        return p;
    }
}
