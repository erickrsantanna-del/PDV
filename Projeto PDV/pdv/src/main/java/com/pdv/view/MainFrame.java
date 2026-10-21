package com.pdv.view;

import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/** Janela principal: menu lateral + área de conteúdo que troca entre as telas. */
public class MainFrame extends JFrame {

    private static final Color TEXTO_MENU = new Color(0xCBD5E1);

    private final CardLayout cards = new CardLayout();
    private final JPanel conteudo = new JPanel(cards);
    private final List<NavButton> botoes = new ArrayList<>();

    private DashboardPanel dashboard;
    private PdvPanel pdv;
    private ProdutosPanel produtos;
    private EstoquePanel estoque;

    private NavButton btnDashboard;
    private NavButton btnProdutos;
    private NavButton btnEstoque;
    private NavButton btnPdv;

    public MainFrame() {
        super("PDV - Sistema de Vendas");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1150, 700));
        setSize(1366, 768);
        setLocationRelativeTo(null);
        setExtendedState(MAXIMIZED_BOTH);

        getContentPane().setBackground(Estilo.FUNDO);
        add(montarSidebar(), BorderLayout.WEST);

        conteudo.setBackground(Estilo.FUNDO);
        conteudo.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        add(conteudo, BorderLayout.CENTER);

        mostrarDashboard();
    }

    // ---------------------------------------------------------- Navegação

    private void mostrarDashboard() {
        if (dashboard == null) {
            dashboard = new DashboardPanel();
            conteudo.add(dashboard, "dashboard");
        } else {
            dashboard.recarregar();
        }
        cards.show(conteudo, "dashboard");
        marcar(btnDashboard);
    }

    private void mostrarPdv() {
        if (pdv == null) {
            pdv = new PdvPanel();
            conteudo.add(pdv, "pdv");
        }
        cards.show(conteudo, "pdv");
        marcar(btnPdv);
        pdv.focarBusca();
    }

    private void mostrarProdutos() {
        if (produtos == null) {
            produtos = new ProdutosPanel();
            conteudo.add(produtos, "produtos");
        } else {
            produtos.recarregar();
        }
        cards.show(conteudo, "produtos");
        marcar(btnProdutos);
    }

    private void mostrarEstoque() {
        if (estoque == null) {
            estoque = new EstoquePanel();
            conteudo.add(estoque, "estoque");
        } else {
            estoque.recarregar();
        }
        cards.show(conteudo, "estoque");
        marcar(btnEstoque);
    }

    private void marcar(NavButton ativo) {
        for (NavButton b : botoes) {
            b.setSelecionado(b == ativo);
        }
    }

    private void sair() {
        int r = JOptionPane.showConfirmDialog(this, "Deseja realmente sair do sistema?",
                "Sair", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    // ------------------------------------------------------------ Sidebar

    private JPanel montarSidebar() {
        JPanel s = new JPanel();
        s.setLayout(new BoxLayout(s, BoxLayout.Y_AXIS));
        s.setBackground(Estilo.SIDEBAR);
        s.setBorder(BorderFactory.createEmptyBorder(22, 16, 16, 16));
        s.setPreferredSize(new Dimension(250, 0));

        // Logo
        JPanel logo = new JPanel(new BorderLayout(12, 0));
        logo.setOpaque(false);
        logo.setAlignmentX(LEFT_ALIGNMENT);
        logo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        logo.add(new Logo(), BorderLayout.WEST);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        JLabel nome = new JLabel("PDV");
        nome.setFont(nome.getFont().deriveFont(Font.BOLD, 20f));
        nome.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Gestão de vendas");
        sub.setFont(sub.getFont().deriveFont(Font.PLAIN, 12f));
        sub.setForeground(new Color(0x94A3B8));
        textos.add(Box.createVerticalGlue());
        textos.add(nome);
        textos.add(sub);
        textos.add(Box.createVerticalGlue());
        logo.add(textos, BorderLayout.CENTER);
        s.add(logo);

        s.add(Box.createVerticalStrut(30));
        JLabel secao = new JLabel("MENU");
        secao.setFont(secao.getFont().deriveFont(Font.BOLD, 11f));
        secao.setForeground(new Color(0x64748B));
        secao.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        secao.setAlignmentX(LEFT_ALIGNMENT);
        s.add(secao);
        s.add(Box.createVerticalStrut(8));

        btnDashboard = novoBotao("Dashboard", Icones.Tipo.DASHBOARD, true);
        btnPdv = novoBotao("Nova venda", Icones.Tipo.CARRINHO, true);
        btnProdutos = novoBotao("Produtos", Icones.Tipo.PRODUTOS, true);
        btnEstoque = novoBotao("Estoque", Icones.Tipo.ESTOQUE, true);

        btnDashboard.addActionListener(e -> mostrarDashboard());
        btnPdv.addActionListener(e -> mostrarPdv());
        btnProdutos.addActionListener(e -> mostrarProdutos());
        btnEstoque.addActionListener(e -> mostrarEstoque());

        for (NavButton b : botoes) {
            s.add(b);
            s.add(Box.createVerticalStrut(4));
        }

        s.add(Box.createVerticalGlue());

        // Rodapé: usuário + sair
        JLabel usuario = new JLabel("Administrador");
        usuario.setFont(usuario.getFont().deriveFont(Font.BOLD, 13f));
        usuario.setForeground(Color.WHITE);
        usuario.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        usuario.setAlignmentX(LEFT_ALIGNMENT);
        JLabel perfil = new JLabel("Perfil: ADMIN");
        perfil.setFont(perfil.getFont().deriveFont(Font.PLAIN, 12f));
        perfil.setForeground(new Color(0x94A3B8));
        perfil.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        perfil.setAlignmentX(LEFT_ALIGNMENT);
        s.add(usuario);
        s.add(perfil);
        s.add(Box.createVerticalStrut(10));

        NavButton btnSair = new NavButton("Sair", Icones.Tipo.SAIR, true);
        btnSair.addActionListener(e -> sair());
        s.add(btnSair);

        return s;
    }

    private NavButton novoBotao(String texto, Icones.Tipo icone, boolean ativo) {
        NavButton b = new NavButton(texto, icone, ativo);
        botoes.add(b);
        return b;
    }

    // ---------------------------------------------------- Componentes

    /** Quadrado azul arredondado com o ícone do carrinho. */
    private static class Logo extends JComponent {
        private final Icon icone = Icones.criar(Icones.Tipo.CARRINHO, 24, Color.WHITE);

        Logo() {
            setPreferredSize(new Dimension(44, 44));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Estilo.ACCENT);
            g2.fillRoundRect(0, 0, 44, 44, 14, 14);
            icone.paintIcon(this, g2, 10, 10);
            g2.dispose();
        }
    }

    /** Botão do menu lateral: ícone + texto, destaque azul quando selecionado. */
    private static class NavButton extends JButton {
        private final Icon normal;
        private final Icon branco;
        private boolean selecionado;
        private boolean hover;

        NavButton(String texto, Icones.Tipo tipo, boolean ativo) {
            super(texto);
            normal = Icones.criar(tipo, 20, TEXTO_MENU);
            branco = Icones.criar(tipo, 20, Color.WHITE);
            setIcon(normal);
            setDisabledIcon(Icones.criar(tipo, 20, new Color(0x64748B)));
            setEnabled(ativo);

            setHorizontalAlignment(LEFT);
            setIconTextGap(14);
            setFont(getFont().deriveFont(Font.PLAIN, 14f));
            setForeground(TEXTO_MENU);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(11, 14, 11, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        void setSelecionado(boolean s) {
            this.selecionado = s;
            setIcon(s ? branco : normal);
            setForeground(s ? Color.WHITE : TEXTO_MENU);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (selecionado) {
                g2.setColor(Estilo.ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            } else if (hover && isEnabled()) {
                g2.setColor(Estilo.SIDEBAR_HOVER);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
