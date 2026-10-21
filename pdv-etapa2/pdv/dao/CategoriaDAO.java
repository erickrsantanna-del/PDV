package com.pdv.dao;

import com.pdv.config.Database;
import com.pdv.model.Categoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CategoriaDAO {

    public List<Categoria> listar() throws SQLException {
        List<Categoria> lista = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT id, nome FROM categorias ORDER BY nome");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Categoria(rs.getInt("id"), rs.getString("nome")));
            }
        }
        return lista;
    }

    /** Retorna o id da categoria com esse nome, criando se ainda não existir. */
    public int buscarOuCriar(String nome) throws SQLException {
        String nomeLimpo = nome.trim();
        try (Connection con = Database.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT id FROM categorias WHERE nome = ?")) {
                ps.setString(1, nomeLimpo);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO categorias (nome) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, nomeLimpo);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        }
    }
}
