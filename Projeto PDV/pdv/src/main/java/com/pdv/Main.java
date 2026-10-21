package com.pdv;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.pdv.config.Database;
import com.pdv.view.MainFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.sql.Connection;
import java.util.Collections;

public class Main {

    public static void main(String[] args) {
        // Cor de destaque e aparência geral
        FlatLaf.setGlobalExtraDefaults(Collections.singletonMap("@accentColor", "#2563EB"));
        FlatLightLaf.setup();

        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 12);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
        UIManager.put("Table.gridColor", new Color(0xE2E8F0));
        UIManager.put("Table.selectionBackground", new Color(0xDBEAFE));
        UIManager.put("Table.selectionForeground", new Color(0x0F172A));

        SwingUtilities.invokeLater(() -> {
            // Testa a conexão antes de abrir a janela
            try (Connection con = Database.getConnection()) {
                new MainFrame().setVisible(true);
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
