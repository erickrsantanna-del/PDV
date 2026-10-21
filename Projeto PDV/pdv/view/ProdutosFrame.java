package com.pdv.view;

import com.pdv.config.Sessao;
import com.pdv.dao.CategoriaDAO;
import com.pdv.dao.ProdutoDAO;
import com.pdv.model.Categoria;
import com.pdv.model.Produto;
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Tela de cadastro de produtos: busca, lista e formulário. */
public class ProdutosFrame extends JFrame {

    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();

    private List<Produto> produtos = new ArrayList<>();
    private Produto selecionado; // null = cadastrando novo

    // Busca
    private final JTextField txtBusca = new JTextField(25);
    private final JCheckBox chkInativos = new JCheckBox("Mostrar inativos");

    // Tabela
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"ID", "Código de barras", "Nome", "Categoria", "Custo", "Venda", "Estoque", "Mínimo", "Un", "Ativo"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable tabela = new JTable(modelo);

    // Formulário
    private final JLabel lblTitulo = new JLabel("Novo produto");
    private final JTextField txtCodigo = new JTextField();
    private final JTextField txtNome = new JTextField();
    private final JComboBox<String> cmbCategoria = new JComboBox<>();
    private final JTextField txtCusto = new JTextField();
    private final JTextField txtVenda = new JTextField();
    private final JLabel lblEstoque = new JLabel("Estoque inicial");
    private final JTextField txtEstoque = new JTextField();
    private final JTextField txtMinimo = new JTextField();
    private final JTextField txtUnidade = new JTextField("UN");
    private final JLabel lblMargem = new JLabel(" ");
    private final JButton btnSalvar = new JButton("Salvar");
    private final JButton btnNovo = new JButton("Novo");
    private final JButton btnAtivar = new JButton("Inativar");

    public ProdutosFrame() {
        super("PDV - Cadastro de Produtos");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 650);
        setLocationRelativeTo(null);

        add(montarTopo(), BorderLayout.NORTH);
        add(montarTabela(), BorderLayout.CENTER);
        add(montarFormulario(), BorderLayout.EAST);

        carregarCategorias();
        carregarTabela();
        novoProduto();
    }

    // ---------------------------------------------------------------- UI

    private JPanel montarTopo() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        p.add(new JLabel("Buscar (nome ou código):"));
        p.add(txtBusca);
        p.add(chkInativos);
        JButton btnBuscar = new JButton("Buscar");
        p.add(btnBuscar);

        btnBuscar.addActionListener(e -> carregarTabela());
        txtBusca.addActionListener(e -> carregarTabela());
        chkInativos.addActionListener(e -> carregarTabela());
        return p;
    }

    private JScrollPane montarTabela() {
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setRowHeight(26);
        tabela.getTableHeader().setReorderingAllowed(false);

        int[] larguras = {50, 140, 300, 130, 90, 90, 80, 80, 40, 50};
        for (int i = 0; i < larguras.length; i++) {
            tabela.getColumnModel().getColumn(i).setPreferredWidth(larguras[i]);
        }

        // Linhas com estoque baixo em vermelho; inativos em cinza
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foco, row, col);
                if (!sel && row < produtos.size()) {
                    Produto p = produtos.get(row);
                    if (!p.isAtivo()) {
                        c.setForeground(Color.GRAY);
                    } else if (p.isEstoqueBaixo()) {
                        c.setForeground(new Color(200, 30, 30));
                    } else {
                        c.setForeground(t.getForeground());
                    }
                }
                if (col >= 4 && col <= 7) {
                    setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }
                return c;
            }
        };
        tabela.setDefaultRenderer(Object.class, renderer);

        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tabela.getSelectedRow();
                if (row >= 0 && row < produtos.size()) {
                    editarProduto(produtos.get(row));
                }
            }
        });
        return new JScrollPane(tabela);
    }

    private JPanel montarFormulario() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        p.setPreferredSize(new Dimension(340, 0));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        g.insets = new Insets(2, 0, 2, 0);

        lblTitulo.setFont(lblTitulo.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        cmbCategoria.setEditable(true);

        int y = 0;
        g.gridy = y++; p.add(lblTitulo, g);
        g.gridy = y++; p.add(new JLabel("Código de barras"), g);
        g.gridy = y++; p.add(txtCodigo, g);
        g.gridy = y++; p.add(new JLabel("Nome *"), g);
        g.gridy = y++; p.add(txtNome, g);
        g.gridy = y++; p.add(new JLabel("Categoria (digite para criar nova)"), g);
        g.gridy = y++; p.add(cmbCategoria, g);
        g.gridy = y++; p.add(new JLabel("Preço de custo"), g);
        g.gridy = y++; p.add(txtCusto, g);
        g.gridy = y++; p.add(new JLabel("Preço de venda *"), g);
        g.gridy = y++; p.add(txtVenda, g);
        g.gridy = y++; p.add(lblMargem, g);
        g.gridy = y++; p.add(lblEstoque, g);
        g.gridy = y++; p.add(txtEstoque, g);
        g.gridy = y++; p.add(new JLabel("Estoque mínimo (alerta)"), g);
        g.gridy = y++; p.add(txtMinimo, g);
        g.gridy = y++; p.add(new JLabel("Unidade (UN, KG, L, CX...)"), g);
        g.gridy = y++; p.add(txtUnidade, g);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botoes.add(btnSalvar);
        botoes.add(btnNovo);
        botoes.add(btnAtivar);
        g.gridy = y++;
        g.insets = new Insets(14, 0, 2, 0);
        p.add(botoes, g);

        // empurra tudo para cima
        g.gridy = y;
        g.weighty = 1;
        g.fill = GridBagConstraints.BOTH;
        p.add(new JPanel(), g);

        btnSalvar.addActionListener(e -> salvar());
        btnNovo.addActionListener(e -> novoProduto());
        btnAtivar.addActionListener(e -> alternarAtivo());

        javax.swing.event.DocumentListener calc = new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { atualizarMargem(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { atualizarMargem(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { atualizarMargem(); }
        };
        txtCusto.getDocument().addDocumentListener(calc);
        txtVenda.getDocument().addDocumentListener(calc);
        return p;
    }

    // ------------------------------------------------------------ Dados

    private void carregarCategorias() {
        try {
            cmbCategoria.removeAllItems();
            cmbCategoria.addItem("");
            for (Categoria c : categoriaDAO.listar()) {
                cmbCategoria.addItem(c.getNome());
            }
        } catch (SQLException e) {
            erro("Erro ao carregar categorias", e);
        }
    }

    private void carregarTabela() {
        try {
            produtos = produtoDAO.listar(txtBusca.getText(), chkInativos.isSelected());
        } catch (SQLException e) {
            erro("Erro ao carregar produtos", e);
            return;
        }
        modelo.setRowCount(0);
        for (Produto p : produtos) {
            modelo.addRow(new Object[]{
                    p.getId(),
                    p.getCodigoBarras() == null ? "" : p.getCodigoBarras(),
                    p.getNome(),
                    p.getCategoriaNome() == null ? "" : p.getCategoriaNome(),
                    Numeros.moeda(p.getPrecoCusto()),
                    Numeros.moeda(p.getPrecoVenda()),
                    Numeros.decimal(p.getEstoqueAtual(), 3),
                    Numeros.decimal(p.getEstoqueMinimo(), 3),
                    p.getUnidade(),
                    p.isAtivo() ? "Sim" : "Não"
            });
        }
        setTitle("PDV - Cadastro de Produtos (" + produtos.size() + " itens)");
    }

    // ------------------------------------------------------- Formulário

    private void novoProduto() {
        selecionado = null;
        tabela.clearSelection();
        lblTitulo.setText("Novo produto");
        txtCodigo.setText("");
        txtNome.setText("");
        cmbCategoria.setSelectedItem("");
        txtCusto.setText("");
        txtVenda.setText("");
        lblEstoque.setText("Estoque inicial");
        txtEstoque.setText("0");
        txtEstoque.setEnabled(true);
        txtMinimo.setText("0");
        txtUnidade.setText("UN");
        btnAtivar.setEnabled(false);
        btnAtivar.setText("Inativar");
        txtCodigo.requestFocusInWindow();
    }

    private void editarProduto(Produto p) {
        selecionado = p;
        lblTitulo.setText("Editando #" + p.getId());
        txtCodigo.setText(p.getCodigoBarras() == null ? "" : p.getCodigoBarras());
        txtNome.setText(p.getNome());
        cmbCategoria.setSelectedItem(p.getCategoriaNome() == null ? "" : p.getCategoriaNome());
        txtCusto.setText(Numeros.decimal(p.getPrecoCusto(), 2));
        txtVenda.setText(Numeros.decimal(p.getPrecoVenda(), 2));
        lblEstoque.setText("Estoque atual (alterar na tela de Estoque)");
        txtEstoque.setText(Numeros.decimal(p.getEstoqueAtual(), 3));
        txtEstoque.setEnabled(false);
        txtMinimo.setText(Numeros.decimal(p.getEstoqueMinimo(), 3));
        txtUnidade.setText(p.getUnidade());
        btnAtivar.setEnabled(true);
        btnAtivar.setText(p.isAtivo() ? "Inativar" : "Reativar");
    }

    private void atualizarMargem() {
        try {
            BigDecimal custo = Numeros.parse(txtCusto.getText());
            BigDecimal venda = Numeros.parse(txtVenda.getText());
            if (venda.signum() > 0 && custo.signum() > 0) {
                BigDecimal lucro = venda.subtract(custo);
                BigDecimal margem = lucro.multiply(BigDecimal.valueOf(100))
                        .divide(venda, 1, java.math.RoundingMode.HALF_UP);
                lblMargem.setText("Lucro: " + Numeros.moeda(lucro) + "  |  Margem: " + margem + "%");
                return;
            }
        } catch (NumberFormatException ignorado) {
            // enquanto o usuário digita, o valor pode estar incompleto
        }
        lblMargem.setText(" ");
    }

    private void salvar() {
        String nome = txtNome.getText().trim();
        if (nome.isEmpty()) {
            aviso("Informe o nome do produto.");
            txtNome.requestFocusInWindow();
            return;
        }

        BigDecimal custo, venda, estoque, minimo;
        try {
            custo = Numeros.parse(txtCusto.getText());
            venda = Numeros.parse(txtVenda.getText());
            estoque = Numeros.parse(txtEstoque.getText());
            minimo = Numeros.parse(txtMinimo.getText());
        } catch (NumberFormatException e) {
            aviso("Há um valor numérico inválido. Use apenas números, por exemplo 12,50.");
            return;
        }
        if (venda.signum() <= 0) {
            aviso("O preço de venda deve ser maior que zero.");
            txtVenda.requestFocusInWindow();
            return;
        }
        if (custo.signum() < 0 || estoque.signum() < 0 || minimo.signum() < 0) {
            aviso("Valores não podem ser negativos.");
            return;
        }

        String codigo = txtCodigo.getText().trim();
        String unidade = txtUnidade.getText().trim().toUpperCase();
        if (unidade.isEmpty()) unidade = "UN";
        if (unidade.length() > 5) {
            aviso("A unidade deve ter no máximo 5 caracteres.");
            return;
        }

        try {
            if (!codigo.isEmpty()
                    && produtoDAO.existeCodigoBarras(codigo, selecionado == null ? null : selecionado.getId())) {
                aviso("Já existe outro produto com esse código de barras.");
                return;
            }

            Produto p = (selecionado == null) ? new Produto() : selecionado;
            p.setCodigoBarras(codigo.isEmpty() ? null : codigo);
            p.setNome(nome);
            Object cat = cmbCategoria.getEditor().getItem();
            String catNome = cat == null ? "" : cat.toString().trim();
            p.setCategoriaId(catNome.isEmpty() ? null : categoriaDAO.buscarOuCriar(catNome));
            p.setPrecoCusto(custo);
            p.setPrecoVenda(venda);
            p.setEstoqueMinimo(minimo);
            p.setUnidade(unidade);

            if (selecionado == null) {
                produtoDAO.inserir(p, estoque, Sessao.usuarioId);
            } else {
                produtoDAO.atualizar(p);
            }
        } catch (SQLException e) {
            erro("Erro ao salvar o produto", e);
            return;
        }

        carregarCategorias();
        carregarTabela();
        novoProduto();
    }

    private void alternarAtivo() {
        if (selecionado == null) return;
        boolean novoEstado = !selecionado.isAtivo();
        String acao = novoEstado ? "reativar" : "inativar";
        int r = JOptionPane.showConfirmDialog(this,
                "Deseja " + acao + " o produto \"" + selecionado.getNome() + "\"?\n"
                        + (novoEstado ? "" : "Ele deixa de aparecer no PDV, mas o histórico de vendas é mantido."),
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;
        try {
            produtoDAO.definirAtivo(selecionado.getId(), novoEstado);
        } catch (SQLException e) {
            erro("Erro ao atualizar o produto", e);
            return;
        }
        carregarTabela();
        novoProduto();
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
