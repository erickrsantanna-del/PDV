package com.pdv.view;

import com.pdv.config.Sessao;
import com.pdv.dao.EstoqueDAO;
import com.pdv.dao.EstoqueException;
import com.pdv.dao.ProdutoDAO;
import com.pdv.model.MovimentacaoEstoque;
import com.pdv.model.Produto;
import com.pdv.model.TipoMovimentacao;
import com.pdv.util.Numeros;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Tela de controle de estoque: saldo dos produtos, lançamentos e histórico. */
public class EstoquePanel extends JPanel {

    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final EstoqueDAO estoqueDAO = new EstoqueDAO();

    private List<Produto> produtos = new ArrayList<>();
    private Produto selecionado;

    // Busca
    private final JTextField txtBusca = new JTextField(25);
    private final JCheckBox chkBaixo = new JCheckBox("Somente estoque baixo ou zerado");

    // Tabela de produtos
    private final DefaultTableModel modeloProdutos = new DefaultTableModel(
            new String[]{"ID", "Nome", "Estoque", "Mínimo", "Un", "Situação"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable tabelaProdutos = new JTable(modeloProdutos);

    // Histórico
    private final DefaultTableModel modeloHist = new DefaultTableModel(
            new String[]{"Data/hora", "Tipo", "Quantidade", "Usuário", "Observação"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable tabelaHist = new JTable(modeloHist);

    // Formulário
    private final JLabel lblProduto = new JLabel("Selecione um produto na lista");
    private final JLabel lblSaldo = new JLabel(" ");
    private final JComboBox<TipoMovimentacao> cmbTipo = new JComboBox<>(new TipoMovimentacao[]{
            TipoMovimentacao.ENTRADA, TipoMovimentacao.DEVOLUCAO,
            TipoMovimentacao.PERDA, TipoMovimentacao.AJUSTE});
    private final JLabel lblQtd = new JLabel("Quantidade");
    private final JTextField txtQtd = new JTextField();
    private final JTextField txtObs = new JTextField();
    private final JLabel lblAjuda = new JLabel(" ");
    private final JButton btnRegistrar = new JButton("Registrar movimentação");

    public EstoquePanel() {
        super(new BorderLayout());
        setOpaque(false);

        add(Estilo.comCabecalho("Estoque", "Entradas, perdas, ajustes e histórico de movimentações", montarTopo()),
                BorderLayout.NORTH);
        add(montarCentro(), BorderLayout.CENTER);
        add(montarFormulario(), BorderLayout.EAST);

        atualizarAjuda();
        habilitarFormulario(false);
        carregarProdutos(null);
    }

    // ---------------------------------------------------------------- UI

    private JPanel montarTopo() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        p.setOpaque(false);
        p.add(new JLabel("Buscar (nome ou código):"));
        p.add(txtBusca);
        p.add(chkBaixo);
        JButton btnBuscar = new JButton("Buscar");
        p.add(btnBuscar);

        btnBuscar.addActionListener(e -> carregarProdutos(selecionado == null ? null : selecionado.getId()));
        txtBusca.addActionListener(e -> carregarProdutos(selecionado == null ? null : selecionado.getId()));
        chkBaixo.addActionListener(e -> carregarProdutos(selecionado == null ? null : selecionado.getId()));
        return p;
    }

    private JSplitPane montarCentro() {
        // --- tabela de produtos
        tabelaProdutos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaProdutos.setRowHeight(26);
        tabelaProdutos.getTableHeader().setReorderingAllowed(false);
        int[] larg = {50, 340, 100, 100, 50, 90};
        for (int i = 0; i < larg.length; i++) {
            tabelaProdutos.getColumnModel().getColumn(i).setPreferredWidth(larg[i]);
        }
        tabelaProdutos.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foco, row, col);
                if (!sel && row < produtos.size()) {
                    Produto p = produtos.get(row);
                    if (p.getEstoqueAtual().signum() <= 0) {
                        c.setForeground(new Color(200, 30, 30));
                    } else if (p.isEstoqueBaixo()) {
                        c.setForeground(new Color(200, 110, 0));
                    } else {
                        c.setForeground(t.getForeground());
                    }
                }
                setHorizontalAlignment(col == 2 || col == 3 ? SwingConstants.RIGHT : SwingConstants.LEFT);
                return c;
            }
        });
        tabelaProdutos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tabelaProdutos.getSelectedRow();
                selecionarProduto(row >= 0 && row < produtos.size() ? produtos.get(row) : null);
            }
        });
        JScrollPane scrollProdutos = new JScrollPane(tabelaProdutos);
        scrollProdutos.setBorder(BorderFactory.createLineBorder(Estilo.BORDA));

        // --- tabela de histórico
        tabelaHist.setRowHeight(24);
        tabelaHist.getTableHeader().setReorderingAllowed(false);
        int[] largH = {130, 90, 100, 130, 380};
        for (int i = 0; i < largH.length; i++) {
            tabelaHist.getColumnModel().getColumn(i).setPreferredWidth(largH[i]);
        }
        tabelaHist.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foco, row, col);
                setHorizontalAlignment(col == 2 ? SwingConstants.RIGHT : SwingConstants.LEFT);
                if (!sel) {
                    String q = String.valueOf(t.getValueAt(row, 2));
                    if (col == 2) {
                        c.setForeground(q.startsWith("-") ? new Color(200, 30, 30) : new Color(20, 130, 50));
                    } else {
                        c.setForeground(t.getForeground());
                    }
                }
                return c;
            }
        });
        JPanel painelHist = new JPanel(new BorderLayout());
        painelHist.setOpaque(false);
        JLabel tituloHist = new JLabel("  Histórico de movimentações do produto selecionado");
        tituloHist.setFont(tituloHist.getFont().deriveFont(Font.BOLD));
        tituloHist.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        painelHist.add(tituloHist, BorderLayout.NORTH);
        painelHist.add(new JScrollPane(tabelaHist), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollProdutos, painelHist);
        split.setResizeWeight(0.55);
        split.setBorder(null);
        split.setOpaque(false);
        return split;
    }

    private JPanel montarFormulario() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, Estilo.BORDA),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        p.setPreferredSize(new Dimension(340, 0));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        g.insets = new Insets(2, 0, 2, 0);

        JLabel titulo = new JLabel("Lançar movimentação");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));
        lblProduto.setFont(lblProduto.getFont().deriveFont(Font.BOLD));
        lblAjuda.setForeground(Color.GRAY);

        int y = 0;
        g.gridy = y++; p.add(titulo, g);
        g.gridy = y++; p.add(lblProduto, g);
        g.gridy = y++; p.add(lblSaldo, g);
        g.gridy = y++; g.insets = new Insets(12, 0, 2, 0); p.add(new JLabel("Tipo"), g);
        g.insets = new Insets(2, 0, 2, 0);
        g.gridy = y++; p.add(cmbTipo, g);
        g.gridy = y++; p.add(lblQtd, g);
        g.gridy = y++; p.add(txtQtd, g);
        g.gridy = y++; p.add(new JLabel("Observação"), g);
        g.gridy = y++; p.add(txtObs, g);
        g.gridy = y++; p.add(lblAjuda, g);
        g.gridy = y++; g.insets = new Insets(14, 0, 2, 0); p.add(btnRegistrar, g);

        g.gridy = y;
        g.weighty = 1;
        g.fill = GridBagConstraints.BOTH;
        JPanel espaco = new JPanel();
        espaco.setOpaque(false);
        p.add(espaco, g);

        cmbTipo.addActionListener(e -> atualizarAjuda());
        btnRegistrar.addActionListener(e -> registrar());
        txtQtd.addActionListener(e -> registrar());
        return p;
    }

    // ------------------------------------------------------------ Dados

    /** Chamado quando o usuário volta para esta tela. */
    public void recarregar() {
        carregarProdutos(selecionado == null ? null : selecionado.getId());
    }

    private void carregarProdutos(Integer idParaSelecionar) {
        List<Produto> todos;
        try {
            todos = produtoDAO.listar(txtBusca.getText(), false);
        } catch (SQLException e) {
            erro("Erro ao carregar produtos", e);
            return;
        }
        produtos = new ArrayList<>();
        for (Produto p : todos) {
            if (!chkBaixo.isSelected() || p.getEstoqueAtual().signum() <= 0 || p.isEstoqueBaixo()) {
                produtos.add(p);
            }
        }

        modeloProdutos.setRowCount(0);
        int rowSel = -1;
        for (int i = 0; i < produtos.size(); i++) {
            Produto p = produtos.get(i);
            String situacao = p.getEstoqueAtual().signum() <= 0 ? "ZERADO" : (p.isEstoqueBaixo() ? "BAIXO" : "OK");
            modeloProdutos.addRow(new Object[]{
                    p.getId(), p.getNome(),
                    Numeros.decimal(p.getEstoqueAtual(), 3),
                    Numeros.decimal(p.getEstoqueMinimo(), 3),
                    p.getUnidade(), situacao});
            if (idParaSelecionar != null && idParaSelecionar.equals(p.getId())) {
                rowSel = i;
            }
        }
        if (rowSel >= 0) {
            tabelaProdutos.setRowSelectionInterval(rowSel, rowSel);
        } else {
            selecionarProduto(null);
        }
    }

    private void selecionarProduto(Produto p) {
        selecionado = p;
        modeloHist.setRowCount(0);
        if (p == null) {
            lblProduto.setText("Selecione um produto na lista");
            lblSaldo.setText(" ");
            habilitarFormulario(false);
            return;
        }
        lblProduto.setText(p.getNome());
        lblSaldo.setText("Estoque atual: " + Numeros.decimal(p.getEstoqueAtual(), 3) + " " + p.getUnidade());
        habilitarFormulario(true);

        try {
            for (MovimentacaoEstoque m : estoqueDAO.listarPorProduto(p.getId(), 300)) {
                BigDecimal q = m.getQuantidade();
                modeloHist.addRow(new Object[]{
                        m.getCriadaEm().format(FMT_DATA),
                        m.getTipo().getRotulo(),
                        (q.signum() > 0 ? "+" : "") + Numeros.decimal(q, 3),
                        m.getUsuarioNome(),
                        m.getObservacao() == null ? "" : m.getObservacao()});
            }
        } catch (SQLException e) {
            erro("Erro ao carregar o histórico", e);
        }
    }

    private void habilitarFormulario(boolean ativo) {
        cmbTipo.setEnabled(ativo);
        txtQtd.setEnabled(ativo);
        txtObs.setEnabled(ativo);
        btnRegistrar.setEnabled(ativo);
    }

    private void atualizarAjuda() {
        TipoMovimentacao tipo = (TipoMovimentacao) cmbTipo.getSelectedItem();
        if (tipo == TipoMovimentacao.AJUSTE) {
            lblQtd.setText("Quantidade REAL contada");
            lblAjuda.setText("<html>O sistema calcula a diferença e corrige o saldo.<br>Observação obrigatória.</html>");
        } else if (tipo == TipoMovimentacao.PERDA) {
            lblQtd.setText("Quantidade perdida");
            lblAjuda.setText("<html>Diminui o estoque.<br>Observação obrigatória (motivo).</html>");
        } else if (tipo == TipoMovimentacao.DEVOLUCAO) {
            lblQtd.setText("Quantidade devolvida");
            lblAjuda.setText("<html>Aumenta o estoque (cliente devolveu o item).</html>");
        } else {
            lblQtd.setText("Quantidade recebida");
            lblAjuda.setText("<html>Aumenta o estoque (compra ou reposição).</html>");
        }
    }

    // ------------------------------------------------------- Registrar

    private void registrar() {
        if (selecionado == null) return;
        TipoMovimentacao tipo = (TipoMovimentacao) cmbTipo.getSelectedItem();

        BigDecimal qtd;
        try {
            qtd = Numeros.parse(txtQtd.getText());
        } catch (NumberFormatException e) {
            aviso("Quantidade inválida. Use apenas números, por exemplo 10 ou 2,5.");
            return;
        }
        if (txtQtd.getText().trim().isEmpty()) {
            aviso("Informe a quantidade.");
            txtQtd.requestFocusInWindow();
            return;
        }
        if (tipo == TipoMovimentacao.AJUSTE ? qtd.signum() < 0 : qtd.signum() <= 0) {
            aviso(tipo == TipoMovimentacao.AJUSTE
                    ? "A quantidade contada não pode ser negativa."
                    : "A quantidade deve ser maior que zero.");
            return;
        }

        String obs = txtObs.getText().trim();
        if ((tipo == TipoMovimentacao.AJUSTE || tipo == TipoMovimentacao.PERDA) && obs.isEmpty()) {
            aviso("Informe uma observação (motivo) para este tipo de movimentação.");
            txtObs.requestFocusInWindow();
            return;
        }

        if (tipo == TipoMovimentacao.AJUSTE) {
            int r = JOptionPane.showConfirmDialog(this,
                    "O estoque de \"" + selecionado.getNome() + "\" vai mudar de "
                            + Numeros.decimal(selecionado.getEstoqueAtual(), 3) + " para "
                            + Numeros.decimal(qtd, 3) + ".\nConfirma?",
                    "Confirmar ajuste", JOptionPane.YES_NO_OPTION);
            if (r != JOptionPane.YES_OPTION) return;
        }

        int idProduto = selecionado.getId();
        try {
            estoqueDAO.registrar(idProduto, tipo, qtd, Sessao.usuarioId, obs);
        } catch (EstoqueException e) {
            aviso(e.getMessage());
            return;
        } catch (SQLException e) {
            erro("Erro ao registrar a movimentação", e);
            return;
        }

        txtQtd.setText("");
        txtObs.setText("");
        carregarProdutos(idProduto);
        txtQtd.requestFocusInWindow();
    }

    // ---------------------------------------------------------- Mensagens

    private void aviso(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Atenção", JOptionPane.WARNING_MESSAGE);
    }

    private void erro(String titulo, Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, titulo + ":\n" + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
    }
}
