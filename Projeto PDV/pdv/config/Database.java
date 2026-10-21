package com.pdv.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Gerencia o pool de conexões com o MySQL.
 * Uso: try (Connection con = Database.getConnection()) { ... }
 */
public final class Database {

    private static HikariDataSource dataSource;

    private Database() { }

    private static synchronized void init() {
        if (dataSource != null) return;

        Properties props = new Properties();
        try (InputStream in = Database.class.getResourceAsStream("/database.properties")) {
            if (in == null) {
                throw new IllegalStateException("Arquivo database.properties não encontrado em resources.");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao ler database.properties", e);
        }

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(props.getProperty("db.url"));
        cfg.setUsername(props.getProperty("db.user"));
        cfg.setPassword(props.getProperty("db.password"));
        cfg.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.size", "5")));
        cfg.setPoolName("pdv-pool");
        dataSource = new HikariDataSource(cfg);
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) init();
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }
}
