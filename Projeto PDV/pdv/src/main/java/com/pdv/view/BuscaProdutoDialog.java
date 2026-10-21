package com.pdv.view;

import com.pdv.model.Produto;
import com.pdv.util.Numeros;

import javax.swing.JComponent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/** Janela para escolher um produto quando a busca por nome retorna vários resultados. */
public final class BuscaProdutoDialog {

    private BuscaProdutoDialog() { }

    /** Retorna o produto escolhido, ou null se o usuário cancelar. */
    public static Produto escolher(Component pai, List<Produto> todos) {
        final List<Produto> lista = todos.subList(0, Math.min(100, todos.size()));

        final JDialog d = new JDialog(SwingUtilities.getWindowAncestor(pai),
                "Vários produtos encontrados - escolha um", Dialog.ModalityType.APPLICATION_MODAL);

        DefaultTableModel modelo = new DefaultTableModel(new String[]{"Produto", "Preço", "Estoque", "Código"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Produto p : lista) {
            modelo.addRow(new Object[]{
                    p.getNome(),
                    Numeros.moeda(p.getPrecoVenda()),
                    Numeros.decimal(p.getEstoqueAtual(), 3) + " " + p.getUnidade(),
                    p.getCodigoBarras() == null ? "" : p.getCodigoBarras()});
        }

        final JTable tabela = new JTable(modelo);
        tabela.setRowHeight(30);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(320);
        tabela.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foco, row, col);
                setHorizontalAlignment(col == 1 || col == 2 ? SwingConstants.RIGHT : SwingConstants.LEFT);
                return c;
            }
        });
        tabela.setRowSelectionInterval(0, 0);

        final Produto[] escolhido = new Produto[1];
        final Runnable confirmar = () -> {
            int r = tabela.getSelectedRow();
            if (r >= 0) {
                escolhido[0] = lista.get(r);
                d.dispose();
            }
        };

        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) confirmar.run();
            }
        });
        tabela.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "escolher");
        tabela.getActionMap().put("escolher", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { confirmar.run(); }
        });
        d.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "fechar");
        d.getRootPane().getActionMap().put("fechar", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { d.dispose(); }
        });

        JPanel raiz = new JPanel(new BorderLayout(0, 10));
        raiz.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        JLabel dica = new JLabel("Use as setas e Enter (ou clique duas vezes). Esc cancela.");
        dica.setForeground(Estilo.MUTED);
        raiz.add(dica, BorderLayout.NORTH);
        raiz.add(new JScrollPane(tabela), BorderLayout.CENTER);
        d.setContentPane(raiz);

        d.setSize(760, 420);
        d.setLocationRelativeTo(pai);
        tabela.requestFocusInWindow();
        d.setVisible(true);
        return escolhido[0];
    }
}
