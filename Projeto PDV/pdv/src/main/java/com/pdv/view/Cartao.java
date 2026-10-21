package com.pdv.view;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Graphics;

/** Painel branco arredondado com título, usado para agrupar conteúdo. */
public class Cartao extends JPanel {

    public Cartao(String titulo, JComponent conteudo) {
        super(new BorderLayout(0, 10));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        if (titulo != null) {
            JLabel l = new JLabel(titulo);
            l.setFont(l.getFont().deriveFont(Font.BOLD, 15f));
            l.setForeground(Estilo.TEXTO);
            add(l, BorderLayout.NORTH);
        }
        add(conteudo, BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Estilo.pintarCartao(g, this);
        super.paintComponent(g);
    }
}
