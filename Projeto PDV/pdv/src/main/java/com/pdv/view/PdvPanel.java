package com.pdv.view;

import com.pdv.config.Sessao;
import com.pdv.dao.ProdutoDAO;
import com.pdv.dao.VendaDAO;
import com.pdv.dao.VendaException;
import com.pdv.model.ItemVenda;
import com.pdv.model.MetodoPagamento;
import com.pdv.model.OrigemConfirmacao;
import com.pdv.model.Produto;
import com.pdv.util.Numeros;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Tela do PDV: busca de produtos, carrinho, desconto e fechamento da venda. */
public class PdvPanel extends JPanel {

    /** Aceita "3*789123" (quantidade * código) ou "2,5*789123". */
    private static final Pattern QTD_CODIGO = Pattern.compile("^(\\d+(?:[.,]\\d+)?)\\*(.+)$");

    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final VendaDAO vendaDAO = new VendaDAO();

    private final List<ItemVenda> carrinho = new ArrayList<>();

    private final JTextField txtBusca = new JTextField();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"#", "Produto", "Qtd", "Preço", "Total"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabela = new JTable(modelo);

    private final JLabel lblItens = new JLabel("0");
    private final JLabel lblSubtotal = new JLabel(Numeros.moeda(BigDecimal.ZERO));
    private final JTextField txtDesconto = new JTextField();
    private final JLabel lblTotal = new JLabel(Numeros.moeda(BigDecimal.ZERO));
    private final JLabel lblUltimo = new JLabel(" ");
    private final JButton btnFinalizar = new JButton("Finalizar venda  (F9)");
    private final JButton btnLimpar = new JButton("Cancelar venda");

    public PdvPanel() {
        super(new BorderLayout());
        setOpaque(false);

        add(Estilo.cabecalho("Nova venda", "Leia o código de barras ou digite o nome do produto e tecle Enter", null),
                BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(16, 0));
        centro.setOpaque(false);
        centro.add(montarEsquerda(), BorderLayout.CENTER);

        JPanel direita = new Cartao("Resumo da venda", montarResumo());
        direita.setPreferredSize(new Dimension(380, 0));
        centro.add(direita, BorderLayout.EAST);
        add(centro, BorderLayout.CENTER);

        registrarAtalhos();
        recalcular();
    }

    /** Chamado quando o usuário abre a tela. */
    public void focarBusca() {
        SwingUtilities.invokeLater(() -> txtBusca.requestFocusInWindow());
    }

    // ---------------------------------------------------------------- UI

    private JPanel montarEsquerda() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setOpaque(false);

        txtBusca.setFont(txtBusca.getFont().deriveFont(18f));
        txtBusca.setPreferredSize(new Dimension(100, 48));
        txtBusca.putClientProperty("JTextField.placeholderText",
                "Código de barras ou nome do produto   (ex.: 3*7891234567890 para 3 unidades)");
        txtBusca.addActionListener(e -> processarBusca());
        p.add(txtBusca, BorderLayout.NORTH);

        tabela.setFont(tabela.getFont().deriveFont(15f));
        tabela.setRowHeight(36);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.getTableHeader().setReorderingAllowed(false);
        int[] larg = {40, 420, 90, 110, 120};
        for (int i = 0; i < larg.length; i++) {
            tabela.getColumnModel().getColumn(i).setPreferredWidth(larg[i]);
        }
        tabela.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foco, row, col);
                setHorizontalAlignment(col >= 2 ? SwingConstants.RIGHT : SwingConstants.LEFT);
                return c;
            }
        });
        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) alterarQuantidade();
            }
        });
        JScrollPane sp = new JScrollPane(tabela);
        sp.setBorder(BorderFactory.createLineBorder(Estilo.BORDA));
        p.add(sp, BorderLayout.CENTER);

        JLabel dicas = new JLabel("F4 Buscar   |   F6 Alterar quantidade   |   F7 Desconto   |   Delete Remover item   |   F9 Finalizar");
        dicas.setForeground(Estilo.MUTED);
        dicas.setFont(dicas.getFont().deriveFont(Font.PLAIN, 12f));
        p.add(dicas, BorderLayout.SOUTH);
        return p;
    }

    private JPanel montarResumo() {
        JPanel c = new JPanel();
        c.setOpaque(false);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));

        c.add(linha("Itens", lblItens));
        c.add(linha("Subtotal", lblSubtotal));

        txtDesconto.setPreferredSize(new Dimension(120, 34));
        txtDesconto.setHorizontalAlignment(SwingConstants.RIGHT);
        txtDesconto.putClientProperty("JTextField.placeholderText", "R$ ou %");
        txtDesconto.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { recalcular(); }
            @Override public void removeUpdate(DocumentEvent e) { recalcular(); }
            @Override public void changedUpdate(DocumentEvent e) { recalcular(); }
        });
        c.add(linha("Desconto", txtDesconto));

        c.add(Box.createVerticalStrut(10));
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        c.add(sep);
        c.add(Box.createVerticalStrut(12));

        JLabel tituloTotal = new JLabel("TOTAL A PAGAR");
        tituloTotal.setForeground(Estilo.MUTED);
        tituloTotal.setFont(tituloTotal.getFont().deriveFont(Font.BOLD, 12f));
        tituloTotal.setAlignmentX(LEFT_ALIGNMENT);
        c.add(tituloTotal);

        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD, 38f));
        lblTotal.setForeground(Estilo.ACCENT);
        lblTotal.setAlignmentX(LEFT_ALIGNMENT);
        c.add(lblTotal);
        c.add(Box.createVerticalStrut(22));

        btnFinalizar.setBackground(Estilo.VERDE);
        btnFinalizar.setForeground(Color.WHITE);
        btnFinalizar.setFont(btnFinalizar.getFont().deriveFont(Font.BOLD, 16f));
        btnFinalizar.setFocusPainted(false);
        ajustarBotao(btnFinalizar, 54);
        btnFinalizar.addActionListener(e -> finalizar());
        c.add(btnFinalizar);
        c.add(Box.createVerticalStrut(8));

        JButton btnQtd = new JButton("Alterar quantidade  (F6)");
        ajustarBotao(btnQtd, 40);
        btnQtd.addActionListener(e -> alterarQuantidade());
        c.add(btnQtd);
        c.add(Box.createVerticalStrut(6));

        JButton btnRemover = new JButton("Remover item  (Delete)");
        ajustarBotao(btnRemover, 40);
        btnRemover.addActionListener(e -> removerItem());
        c.add(btnRemover);
        c.add(Box.createVerticalStrut(6));

        ajustarBotao(btnLimpar, 40);
        btnLimpar.addActionListener(e -> limparCarrinho(true));
        c.add(btnLimpar);

        c.add(Box.createVerticalStrut(18));
        lblUltimo.setForeground(Estilo.MUTED);
        lblUltimo.setAlignmentX(LEFT_ALIGNMENT);
        c.add(lblUltimo);
        c.add(Box.createVerticalGlue());
        return c;
    }

    private JPanel linha(String rotulo, JComponent valor) {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        JLabel l = new JLabel(rotulo);
        l.setForeground(Estilo.MUTED);
        p.add(l, BorderLayout.WEST);
        if (valor instanceof JLabel) {
            ((JLabel) valor).setHorizontalAlignment(SwingConstants.RIGHT);
            valor.setFont(valor.getFont().deriveFont(Font.BOLD, 15f));
        }
        p.add(valor, BorderLayout.EAST);
        return p;
    }

    private void ajustarBotao(JButton b, int altura) {
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, altura));
        b.setPreferredSize(new Dimension(100, altura));
    }

    private void registrarAtalhos() {
        atalho(KeyEvent.VK_F4, "buscar", () -> {
            txtBusca.requestFocusInWindow();
            txtBusca.selectAll();
        });
        atalho(KeyEvent.VK_F6, "quantidade", this::alterarQuantidade);
        atalho(KeyEvent.VK_F7, "desconto", () -> txtDesconto.requestFocusInWindow());
        atalho(KeyEvent.VK_DELETE, "remover", this::removerItem);
        atalho(KeyEvent.VK_F9, "finalizar", this::finalizar);
    }

    private void atalho(int tecla, String nome, Runnable acao) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(tecla, 0), nome);
        getActionMap().put(nome, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { acao.run(); }
        });
    }

    // ----------------------------------------------------------- Carrinho

    private void processarBusca() {
        String texto = txtBusca.getText().trim();
        if (texto.isEmpty()) return;

        BigDecimal qtd = BigDecimal.ONE;
        Matcher m = QTD_CODIGO.matcher(texto);
        if (m.matches()) {
            qtd = Numeros.parse(m.group(1));
            texto = m.group(2).trim();
        }
        if (qtd.signum() <= 0) {
            aviso("A quantidade deve ser maior que zero.");
            return;
        }

        try {
            Produto p = produtoDAO.buscarPorCodigo(texto);
            if (p == null) {
                List<Produto> lista = produtoDAO.listar(texto, false);
                if (lista.isEmpty()) {
                    aviso("Nenhum produto encontrado para: " + texto);
                    txtBusca.selectAll();
                    return;
                }
                p = lista.size() == 1 ? lista.get(0) : BuscaProdutoDialog.escolher(this, lista);
                if (p == null) {
                    txtBusca.requestFocusInWindow();
                    return;
                }
            }
            adicionar(p, qtd);
        } catch (SQLException e) {
            erro("Erro ao buscar o produto", e);
            return;
        }
        txtBusca.setText("");
        txtBusca.requestFocusInWindow();
    }

    private void adicionar(Produto p, BigDecimal qtd) {
        if (p.getEstoqueAtual().signum() <= 0) {
            aviso("\"" + p.getNome() + "\" está sem estoque.");
            return;
        }

        ItemVenda existente = null;
        for (ItemVenda i : carrinho) {
            if (i.getProduto().getId().equals(p.getId())) {
                existente = i;
                break;
            }
        }
        BigDecimal novaQtd = (existente == null ? BigDecimal.ZERO : existente.getQuantidade()).add(qtd);
        if (novaQtd.compareTo(p.getEstoqueAtual()) > 0) {
            aviso("Estoque insuficiente para \"" + p.getNome() + "\".\nDisponível: "
                    + qtdTexto(p.getEstoqueAtual()) + " " + p.getUnidade()
                    + (existente == null ? "" : "  (já no carrinho: " + qtdTexto(existente.getQuantidade()) + ")"));
            return;
        }

        int linha;
        if (existente == null) {
            carrinho.add(new ItemVenda(p, qtd));
            linha = carrinho.size() - 1;
        } else {
            existente.setQuantidade(novaQtd);
            existente.setProduto(p);
            linha = carrinho.indexOf(existente);
        }
        atualizarTabela();
        tabela.setRowSelectionInterval(linha, linha);
        tabela.scrollRectToVisible(tabela.getCellRect(linha, 0, true));

        lblUltimo.setText("<html><div style='width:300px'>Último item:<br><b>" + escapar(p.getNome())
                + "</b><br>" + Numeros.moeda(p.getPrecoVenda()) + " / " + p.getUnidade() + "</div></html>");
    }

    private void alterarQuantidade() {
        int row = tabela.getSelectedRow();
        if (row < 0 || row >= carrinho.size()) {
            aviso("Selecione um item do carrinho.");
            return;
        }
        ItemVenda item = carrinho.get(row);
        Object resp = JOptionPane.showInputDialog(this,
                "Nova quantidade para \"" + item.getProduto().getNome() + "\":",
                qtdTexto(item.getQuantidade()));
        if (resp == null) return;

        BigDecimal nova;
        try {
            nova = Numeros.parse(resp.toString());
        } catch (NumberFormatException e) {
            aviso("Quantidade inválida.");
            return;
        }
        if (nova.signum() <= 0) {
            aviso("A quantidade deve ser maior que zero. Para tirar o item, use Remover.");
            return;
        }
        if (nova.compareTo(item.getProduto().getEstoqueAtual()) > 0) {
            aviso("Estoque insuficiente. Disponível: " + qtdTexto(item.getProduto().getEstoqueAtual())
                    + " " + item.getProduto().getUnidade());
            return;
        }
        item.setQuantidade(nova);
        atualizarTabela();
        tabela.setRowSelectionInterval(row, row);
        txtBusca.requestFocusInWindow();
    }

    private void removerItem() {
        int row = tabela.getSelectedRow();
        if (row < 0 || row >= carrinho.size()) return;
        carrinho.remove(row);
        atualizarTabela();
        if (!carrinho.isEmpty()) {
            int sel = Math.min(row, carrinho.size() - 1);
            tabela.setRowSelectionInterval(sel, sel);
        }
        txtBusca.requestFocusInWindow();
    }

    private void limparCarrinho(boolean confirmar) {
        if (carrinho.isEmpty() && txtDesconto.getText().isEmpty()) return;
        if (confirmar) {
            int r = JOptionPane.showConfirmDialog(this, "Descartar todos os itens desta venda?",
                    "Cancelar venda", JOptionPane.YES_NO_OPTION);
            if (r != JOptionPane.YES_OPTION) return;
        }
        carrinho.clear();
        txtDesconto.setText("");
        atualizarTabela();
        lblUltimo.setText(" ");
        txtBusca.requestFocusInWindow();
    }

    private void atualizarTabela() {
        modelo.setRowCount(0);
        int n = 1;
        for (ItemVenda i : carrinho) {
            modelo.addRow(new Object[]{
                    n++,
                    i.getProduto().getNome(),
                    qtdTexto(i.getQuantidade()) + " " + i.getProduto().getUnidade(),
                    Numeros.moeda(i.getPrecoUnitario()),
                    Numeros.moeda(i.getTotal())});
        }
        recalcular();
    }

    // ------------------------------------------------------------ Totais

    private BigDecimal subtotal() {
        BigDecimal s = BigDecimal.ZERO;
        for (ItemVenda i : carrinho) {
            s = s.add(i.getTotal());
        }
        return s;
    }

    /** Retorna o desconto em R$, ou null se o texto digitado for inválido. Aceita "10", "10,50" ou "5%". */
    private BigDecimal calcularDesconto(BigDecimal subtotal) {
        String t = txtDesconto.getText().trim();
        if (t.isEmpty()) return BigDecimal.ZERO;
        try {
            BigDecimal d;
            if (t.endsWith("%")) {
                BigDecimal pct = Numeros.parse(t.substring(0, t.length() - 1));
                if (pct.signum() < 0 || pct.compareTo(BigDecimal.valueOf(100)) > 0) return null;
                d = subtotal.multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            } else {
                d = Numeros.parse(t).setScale(2, RoundingMode.HALF_UP);
            }
            if (d.signum() < 0 || d.compareTo(subtotal) > 0) return null;
            return d;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void recalcular() {
        BigDecimal subtotal = subtotal();
        BigDecimal desconto = calcularDesconto(subtotal);
        boolean valido = desconto != null;

        txtDesconto.putClientProperty("JComponent.outline", valido ? null : "error");
        txtDesconto.repaint();

        lblItens.setText(String.valueOf(carrinho.size()));
        lblSubtotal.setText(Numeros.moeda(subtotal));
        lblTotal.setText(Numeros.moeda(valido ? subtotal.subtract(desconto) : subtotal));

        btnFinalizar.setEnabled(!carrinho.isEmpty() && valido);
        btnLimpar.setEnabled(!carrinho.isEmpty());
    }

    // ---------------------------------------------------------- Fechamento

    private void finalizar() {
        if (carrinho.isEmpty() || !btnFinalizar.isEnabled()) return;

        BigDecimal subtotal = subtotal();
        BigDecimal desconto = calcularDesconto(subtotal);
        if (desconto == null) {
            aviso("Desconto inválido. Use um valor (10 ou 10,50) ou uma porcentagem (5%), sem passar do subtotal.");
            return;
        }
        BigDecimal total = subtotal.subtract(desconto);

        // 1) cria a venda aguardando pagamento (o estoque ainda não muda)
        long vendaId;
        try {
            vendaId = vendaDAO.criarVenda(new ArrayList<>(carrinho), desconto, Sessao.usuarioId);
        } catch (VendaException e) {
            aviso(e.getMessage());
            return;
        } catch (SQLException e) {
            erro("Erro ao criar a venda", e);
            return;
        }

        // 2) pagamento
        PagamentoDialog dlg = new PagamentoDialog(SwingUtilities.getWindowAncestor(this), vendaId, total);
        dlg.setVisible(true);
        if (!dlg.isConfirmado()) {
            cancelarSilencioso(vendaId);
            txtBusca.requestFocusInWindow();
            return;
        }

        // 3) só conclui quando o pagamento é confirmado
        MetodoPagamento metodo = dlg.getMetodo();
        try {
            long pagamentoId = vendaDAO.registrarPagamentoPendente(vendaId, metodo, total, dlg.getParcelas());
            String motivo = metodo == MetodoPagamento.DINHEIRO
                    ? "Pagamento em dinheiro"
                    : "Confirmado manualmente pelo operador";
            String ref = dlg.getReferencia();
            vendaDAO.confirmarPagamento(pagamentoId, OrigemConfirmacao.MANUAL, Sessao.usuarioId, motivo,
                    metodo == MetodoPagamento.PIX ? ref : null,
                    metodo == MetodoPagamento.CARTAO_DEBITO || metodo == MetodoPagamento.CARTAO_CREDITO ? ref : null,
                    null);
        } catch (VendaException e) {
            cancelarSilencioso(vendaId);
            aviso("Não foi possível concluir a venda:\n" + e.getMessage()
                    + "\n\nA venda foi cancelada. Ajuste o carrinho e tente novamente.");
            return;
        } catch (SQLException e) {
            cancelarSilencioso(vendaId);
            erro("Erro ao registrar o pagamento", e);
            return;
        }

        String msg = "Venda #" + vendaId + " concluída!\n\nTotal: " + Numeros.moeda(total)
                + "\nForma de pagamento: " + metodo.getRotulo();
        if (metodo == MetodoPagamento.DINHEIRO && dlg.getTroco().signum() > 0) {
            msg += "\nTROCO: " + Numeros.moeda(dlg.getTroco());
        }
        JOptionPane.showMessageDialog(this, msg, "Venda concluída", JOptionPane.INFORMATION_MESSAGE);

        carrinho.clear();
        txtDesconto.setText("");
        atualizarTabela();
        lblUltimo.setText("<html>Venda #" + vendaId + " concluída: <b>" + Numeros.moeda(total) + "</b></html>");
        txtBusca.requestFocusInWindow();
    }

    private void cancelarSilencioso(long vendaId) {
        try {
            vendaDAO.cancelarVenda(vendaId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------------------------------- Utilidades

    private static String qtdTexto(BigDecimal q) {
        return q.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private static String escapar(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void aviso(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Atenção", JOptionPane.WARNING_MESSAGE);
    }

    private void erro(String titulo, Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, titulo + ":\n" + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
    }
}
