package com.pdv.view;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Gráfico de barras simples (sem bibliotecas externas). */
public class GraficoBarras extends JComponent {

    private static final Locale BR = new Locale("pt", "BR");

    private List<String> rotulos = new ArrayList<>();
    private List<BigDecimal> valores = new ArrayList<>();
    private String mensagemVazio = "Sem dados no período";

    public GraficoBarras() {
        setPreferredSize(new Dimension(420, 220));
    }

    public void setMensagemVazio(String mensagem) {
        this.mensagemVazio = mensagem;
    }

    public void setDados(List<String> rotulos, List<BigDecimal> valores) {
        this.rotulos = rotulos;
        this.valores = valores;
        repaint();
    }

    private static String compacto(BigDecimal v) {
        if (v.compareTo(BigDecimal.valueOf(1000)) >= 0) {
            return String.format(BR, "%.1fk", v.doubleValue() / 1000.0);
        }
        return v.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        FontMetrics fm = g2.getFontMetrics();

        BigDecimal max = BigDecimal.ZERO;
        for (BigDecimal v : valores) {
            if (v.compareTo(max) > 0) max = v;
        }

        if (valores.isEmpty() || max.signum() == 0) {
            g2.setColor(Estilo.MUTED);
            int tw = fm.stringWidth(mensagemVazio);
            g2.drawString(mensagemVazio, (w - tw) / 2, h / 2);
            g2.dispose();
            return;
        }

        int base = h - 26;
        int topo = 24;
        int n = valores.size();
        int slot = w / n;
        int largura = Math.max(10, Math.min(52, slot - 18));

        g2.setColor(Estilo.BORDA);
        g2.drawLine(0, base, w, base);

        for (int i = 0; i < n; i++) {
            BigDecimal v = valores.get(i);
            int altura = (int) Math.round((base - topo) * v.doubleValue() / max.doubleValue());
            int x = i * slot + (slot - largura) / 2;
            int y = base - altura;
            int cx = x + largura / 2;

            g2.setColor(i == n - 1 ? Estilo.ACCENT : new Color(0x93C5FD));
            g2.fillRoundRect(x, y, largura, Math.max(altura, 3), 8, 8);

            if (v.signum() > 0) {
                String t = compacto(v);
                g2.setColor(Estilo.TEXTO);
                g2.drawString(t, cx - fm.stringWidth(t) / 2, y - 6);
            }

            String rotulo = rotulos.get(i);
            g2.setColor(Estilo.MUTED);
            g2.drawString(rotulo, cx - fm.stringWidth(rotulo) / 2, base + 17);
        }
        g2.dispose();
    }
}
