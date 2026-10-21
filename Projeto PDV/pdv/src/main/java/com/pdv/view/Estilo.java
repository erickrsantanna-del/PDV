package com.pdv.view;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Cores e pequenos utilitários visuais compartilhados por todas as telas. */
public final class Estilo {

    public static final Color SIDEBAR = new Color(0x0F172A);
    public static final Color SIDEBAR_HOVER = new Color(0x1E293B);
    public static final Color FUNDO = new Color(0xF1F5F9);
    public static final Color CARD = Color.WHITE;
    public static final Color BORDA = new Color(0xE2E8F0);
    public static final Color TEXTO = new Color(0x0F172A);
    public static final Color MUTED = new Color(0x64748B);

    public static final Color ACCENT = new Color(0x2563EB);
    public static final Color VERDE = new Color(0x16A34A);
    public static final Color VERMELHO = new Color(0xDC2626);
    public static final Color LARANJA = new Color(0xEA580C);
    public static final Color ROXO = new Color(0x7C3AED);
    public static final Color CIANO = new Color(0x0891B2);

    private Estilo() { }

    /** Título grande + subtítulo, com um componente opcional à direita (ex.: botão). */
    public static JPanel cabecalho(String titulo, String subtitulo, JComponent direita) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));

        JLabel t = new JLabel(titulo);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 26f));
        t.setForeground(TEXTO);
        JLabel s = new JLabel(subtitulo);
        s.setForeground(MUTED);

        textos.add(t);
        textos.add(Box.createVerticalStrut(2));
        textos.add(s);
        p.add(textos, BorderLayout.WEST);
        if (direita != null) {
            p.add(direita, BorderLayout.EAST);
        }
        return p;
    }

    /** Cabeçalho em cima e o conteúdo logo abaixo. */
    public static JPanel comCabecalho(String titulo, String subtitulo, JComponent conteudo) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(cabecalho(titulo, subtitulo, null), BorderLayout.NORTH);
        if (conteudo != null) {
            p.add(conteudo, BorderLayout.CENTER);
        }
        return p;
    }

    /** Desenha o fundo branco arredondado com borda fina usado nos cartões. */
    public static void pintarCartao(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(CARD);
        g2.fillRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 18, 18);
        g2.setColor(BORDA);
        g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 18, 18);
        g2.dispose();
    }
}
