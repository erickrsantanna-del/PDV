package com.pdv.view;

import com.pdv.dao.DashboardDAO;
import com.pdv.model.DashboardDados;
import com.pdv.util.Numeros;

import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Tela inicial: indicadores de vendas, estoque e gráficos. */
public class DashboardPanel extends JPanel {

    private static final Locale BR = new Locale("pt", "BR");

    private final DashboardDAO dao = new DashboardDAO();

    private final KpiCard kHoje = new KpiCard("Vendas hoje", Icones.Tipo.CARRINHO, Estilo.ACCENT);
    private final KpiCard kMes = new KpiCard("Vendas no mês", Icones.Tipo.GRAFICO, Estilo.ROXO);
    private final KpiCard kLucro = new KpiCard("Lucro no mês", Icones.Tipo.LUCRO, Estilo.VERDE);
    private final KpiCard kTicket = new KpiCard("Ticket médio", Icones.Tipo.TICKET, Estilo.LARANJA);

    private final KpiCard kProdutos = new KpiCard("Produtos ativos", Icones.Tipo.PRODUTOS, Estilo.ACCENT);
    private final KpiCard kBaixo = new KpiCard("Estoque baixo", Icones.Tipo.ALERTA, Estilo.VERMELHO);
    private final KpiCard kCusto = new KpiCard("Estoque (custo)", Icones.Tipo.ESTOQUE, Estilo.CIANO);
    private final KpiCard kVenda = new KpiCard("Estoque (venda)", Icones.Tipo.DINHEIRO, Estilo.VERDE);

    private final GraficoBarras grafico = new GraficoBarras();
    private final JPanel listaTop = new JPanel();
    private final JPanel listaCritico = new JPanel();

    public DashboardPanel() {
        super(new BorderLayout());
        setOpaque(false);

        JButton btnAtualizar = new JButton("Atualizar");
        btnAtualizar.addActionListener(e -> recarregar());

        String data = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy", BR));
        data = Character.toUpperCase(data.charAt(0)) + data.substring(1);
        add(Estilo.cabecalho("Dashboard", data, btnAtualizar), BorderLayout.NORTH);

        grafico.setMensagemVazio("Sem vendas nos últimos 7 dias");
        for (JPanel lista : new JPanel[]{listaTop, listaCritico}) {
            lista.setOpaque(false);
            lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        }

        JPanel corpo = new JPanel(new GridBagLayout());
        corpo.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.BOTH;

        g.gridy = 0;
        g.weighty = 0;
        g.insets = new Insets(0, 0, 16, 0);
        corpo.add(linhaKpis(kHoje, kMes, kLucro, kTicket), g);

        g.gridy = 1;
        corpo.add(linhaKpis(kProdutos, kBaixo, kCusto, kVenda), g);

        JPanel inferior = new JPanel(new GridLayout(1, 3, 16, 0));
        inferior.setOpaque(false);
        inferior.add(new Cartao("Vendas nos últimos 7 dias", grafico));
        inferior.add(new Cartao("Mais vendidos (30 dias)", listaTop));
        inferior.add(new Cartao("Estoque crítico", listaCritico));

        g.gridy = 2;
        g.weighty = 1;
        g.insets = new Insets(0, 0, 0, 0);
        corpo.add(inferior, g);

        add(corpo, BorderLayout.CENTER);
        recarregar();
    }

    private JPanel linhaKpis(KpiCard a, KpiCard b, KpiCard c, KpiCard d) {
        JPanel p = new JPanel(new GridLayout(1, 4, 16, 0));
        p.setOpaque(false);
        p.add(a);
        p.add(b);
        p.add(c);
        p.add(d);
        return p;
    }

    /** Busca os números no banco sem travar a tela e depois atualiza os componentes. */
    public void recarregar() {
        new SwingWorker<DashboardDados, Void>() {
            @Override
            protected DashboardDados doInBackground() throws Exception {
                return dao.carregar();
            }

            @Override
            protected void done() {
                try {
                    aplicar(get());
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(DashboardPanel.this,
                            "Erro ao carregar o dashboard:\n" + e.getMessage(),
                            "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void aplicar(DashboardDados d) {
        kHoje.setValor(Numeros.moeda(d.vendasHoje),
                d.qtdVendasHoje + (d.qtdVendasHoje == 1 ? " venda hoje" : " vendas hoje"));
        kMes.setValor(Numeros.moeda(d.vendasMes),
                d.qtdVendasMes + (d.qtdVendasMes == 1 ? " venda no mês" : " vendas no mês"));

        String margem = "Sem vendas no mês";
        if (d.vendasMes.signum() > 0) {
            BigDecimal pct = d.lucroMes.multiply(BigDecimal.valueOf(100))
                    .divide(d.vendasMes, 1, RoundingMode.HALF_UP);
            margem = "Margem de " + pct.toPlainString().replace('.', ',') + "%";
        }
        kLucro.setValor(Numeros.moeda(d.lucroMes), margem);
        kTicket.setValor(Numeros.moeda(d.ticketMedioMes()), "Média por venda no mês");

        kProdutos.setValor(String.valueOf(d.produtosAtivos), "Cadastrados e ativos");
        kBaixo.setValor(String.valueOf(d.estoqueBaixo),
                d.estoqueBaixo == 0 ? "Tudo em ordem" : "Produtos para repor");
        kCusto.setValor(Numeros.moeda(d.valorCusto), "Capital parado em estoque");
        kVenda.setValor(Numeros.moeda(d.valorVenda),
                "Lucro potencial: " + Numeros.moeda(d.valorVenda.subtract(d.valorCusto)));

        // Gráfico
        List<String> rotulos = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        for (LocalDate dia : d.dias) {
            rotulos.add(dia.equals(d.hoje) ? "Hoje" : dia.format(fmt));
        }
        grafico.setDados(rotulos, d.valoresDias);

        // Mais vendidos
        listaTop.removeAll();
        for (DashboardDados.ItemTop it : d.maisVendidos) {
            listaTop.add(linha(qtd(it.quantidade) + "x  " + it.nome, Numeros.moeda(it.total), Estilo.TEXTO));
        }
        if (d.maisVendidos.isEmpty()) {
            listaTop.add(vazio("Ainda não há vendas registradas."));
        }
        listaTop.add(Box.createVerticalGlue());

        // Estoque crítico
        listaCritico.removeAll();
        for (DashboardDados.ItemCritico it : d.criticos) {
            Color cor = it.atual.signum() <= 0 ? Estilo.VERMELHO : Estilo.LARANJA;
            listaCritico.add(linha(it.nome, qtd(it.atual) + " " + it.unidade, cor));
        }
        if (d.criticos.isEmpty()) {
            listaCritico.add(vazio("Nenhum produto com estoque baixo."));
        }
        listaCritico.add(Box.createVerticalGlue());

        listaTop.revalidate();
        listaTop.repaint();
        listaCritico.revalidate();
        listaCritico.repaint();
    }

    private JPanel linha(String esquerda, String direita, Color corDireita) {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Estilo.BORDA),
                BorderFactory.createEmptyBorder(9, 0, 9, 0)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        p.setAlignmentX(LEFT_ALIGNMENT);

        JLabel a = new JLabel(esquerda);
        a.setForeground(Estilo.TEXTO);
        JLabel b = new JLabel(direita);
        b.setFont(b.getFont().deriveFont(Font.BOLD));
        b.setForeground(corDireita);
        p.add(a, BorderLayout.CENTER);
        p.add(b, BorderLayout.EAST);
        return p;
    }

    private JLabel vazio(String texto) {
        JLabel l = new JLabel(texto);
        l.setForeground(Estilo.MUTED);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    /** 12,000 -> "12"; 2,500 -> "2,5" */
    private String qtd(BigDecimal q) {
        return q.stripTrailingZeros().toPlainString().replace('.', ',');
    }
}
