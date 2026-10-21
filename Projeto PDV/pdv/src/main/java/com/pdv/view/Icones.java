package com.pdv.view;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Ícones vetoriais desenhados em código (sem arquivos de imagem).
 * Ficam nítidos em qualquer tamanho e aceitam qualquer cor.
 */
public final class Icones {

    public enum Tipo { DASHBOARD, PRODUTOS, ESTOQUE, CARRINHO, DINHEIRO, GRAFICO, ALERTA, SAIR, TICKET, LUCRO }

    private Icones() { }

    public static Icon criar(Tipo tipo, int tamanho, Color cor) {
        return new IconeVetorial(tipo, tamanho, cor);
    }

    private static final class IconeVetorial implements Icon {
        private final Tipo tipo;
        private final int tamanho;
        private final Color cor;

        IconeVetorial(Tipo tipo, int tamanho, Color cor) {
            this.tipo = tipo;
            this.tamanho = tamanho;
            this.cor = cor;
        }

        @Override public int getIconWidth() { return tamanho; }
        @Override public int getIconHeight() { return tamanho; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.translate(x, y);
                double escala = tamanho / 24.0;
                g2.scale(escala, escala);
                g2.setColor(cor);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                desenhar(g2);
            } finally {
                g2.dispose();
            }
        }

        // Todos os desenhos usam uma grade de 24 x 24
        private void desenhar(Graphics2D g2) {
            switch (tipo) {
                case DASHBOARD:
                    g2.draw(new RoundRectangle2D.Double(3, 3, 7, 9, 2, 2));
                    g2.draw(new RoundRectangle2D.Double(14, 3, 7, 5, 2, 2));
                    g2.draw(new RoundRectangle2D.Double(14, 12, 7, 9, 2, 2));
                    g2.draw(new RoundRectangle2D.Double(3, 16, 7, 5, 2, 2));
                    break;
                case PRODUTOS:
                    poly(g2, true, 12, 2, 21, 7, 21, 17, 12, 22, 3, 17, 3, 7);
                    poly(g2, false, 3, 7, 12, 12, 21, 7);
                    poly(g2, false, 12, 12, 12, 22);
                    break;
                case ESTOQUE:
                    poly(g2, true, 12, 2, 22, 8, 12, 14, 2, 8);
                    poly(g2, false, 2, 12, 12, 18, 22, 12);
                    poly(g2, false, 2, 16, 12, 22, 22, 16);
                    break;
                case CARRINHO:
                    poly(g2, false, 1, 3, 5, 3, 7.7, 15.4, 19, 15.4, 21, 7, 6, 7);
                    g2.draw(new Ellipse2D.Double(7.4, 18.4, 3.2, 3.2));
                    g2.draw(new Ellipse2D.Double(16.4, 18.4, 3.2, 3.2));
                    break;
                case DINHEIRO:
                    g2.draw(new RoundRectangle2D.Double(2, 6, 20, 12, 3, 3));
                    g2.draw(new Ellipse2D.Double(9, 9, 6, 6));
                    break;
                case GRAFICO:
                    poly(g2, false, 3, 17, 9, 11, 13, 15, 21, 6);
                    poly(g2, false, 15, 6, 21, 6, 21, 12);
                    break;
                case ALERTA:
                    poly(g2, true, 12, 3, 22, 20, 2, 20);
                    poly(g2, false, 12, 9, 12, 14);
                    g2.fill(new Ellipse2D.Double(11, 16.2, 2, 2));
                    break;
                case SAIR:
                    poly(g2, false, 9, 3, 4, 3, 4, 21, 9, 21);
                    poly(g2, false, 10, 12, 21, 12);
                    poly(g2, false, 17, 8, 21, 12, 17, 16);
                    break;
                case TICKET:
                    g2.draw(new RoundRectangle2D.Double(5, 2, 14, 20, 3, 3));
                    poly(g2, false, 8, 8, 16, 8);
                    poly(g2, false, 8, 12, 16, 12);
                    poly(g2, false, 8, 16, 13, 16);
                    break;
                case LUCRO:
                    g2.draw(new Ellipse2D.Double(3, 3, 18, 18));
                    poly(g2, false, 12, 16, 12, 8);
                    poly(g2, false, 8.5, 11.5, 12, 8, 15.5, 11.5);
                    break;
                default:
                    break;
            }
        }

        private void poly(Graphics2D g2, boolean fechar, double... p) {
            Path2D.Double path = new Path2D.Double();
            path.moveTo(p[0], p[1]);
            for (int i = 2; i + 1 < p.length; i += 2) {
                path.lineTo(p[i], p[i + 1]);
            }
            if (fechar) {
                path.closePath();
            }
            g2.draw(path);
        }
    }
}
