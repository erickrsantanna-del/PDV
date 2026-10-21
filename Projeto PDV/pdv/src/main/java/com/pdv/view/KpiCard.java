package com.pdv.view;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Cartão de indicador: título, valor grande, texto de apoio e ícone colorido. */
public class KpiCard extends JPanel {

    private final JLabel lblValor = new JLabel("-");
    private final JLabel lblSub = new JLabel(" ");

    public KpiCard(String titulo, Icones.Tipo icone, Color cor) {
        super(new BorderLayout(10, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setForeground(Estilo.MUTED);
        lblValor.setFont(lblValor.getFont().deriveFont(Font.BOLD, 24f));
        lblValor.setForeground(Estilo.TEXTO);
        lblSub.setFont(lblSub.getFont().deriveFont(Font.PLAIN, 12f));
        lblSub.setForeground(Estilo.MUTED);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.add(lblTitulo);
        textos.add(Box.createVerticalStrut(4));
        textos.add(lblValor);
        textos.add(Box.createVerticalStrut(2));
        textos.add(lblSub);

        add(textos, BorderLayout.CENTER);
        add(new Distintivo(icone, cor), BorderLayout.EAST);
    }

    public void setValor(String valor, String subtitulo) {
        lblValor.setText(valor);
        lblSub.setText(subtitulo == null || subtitulo.isEmpty() ? " " : subtitulo);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Estilo.pintarCartao(g, this);
        super.paintComponent(g);
    }

    /** Círculo colorido translúcido com o ícone no centro. */
    private static class Distintivo extends JComponent {
        private final Color fundo;
        private final Icon icone;

        Distintivo(Icones.Tipo tipo, Color cor) {
            this.fundo = new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 36);
            this.icone = Icones.criar(tipo, 22, cor);
            setPreferredSize(new Dimension(48, 48));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int d = 44;
            int x = (getWidth() - d) / 2;
            int y = 0;
            g2.setColor(fundo);
            g2.fillOval(x, y, d, d);
            icone.paintIcon(this, g2, x + (d - 22) / 2, y + (d - 22) / 2);
            g2.dispose();
        }
    }
}
