package com.pdv;

import com.formdev.flatlaf.FlatLightLaf;
import com.pdv.config.Database;
import com.pdv.view.ProdutosFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.sql.Connection;

public class Main {

    public static void main(String[] args) {
        FlatLightLaf.setup();

        SwingUtilities.invokeLater(() -> {
            // Testa a conexão antes de abrir a tela
            try (Connection con = Database.getConnection()) {
                new ProdutosFrame().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Falha ao conectar no banco:\n" + e.getMessage(),
                        "PDV - Erro", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
