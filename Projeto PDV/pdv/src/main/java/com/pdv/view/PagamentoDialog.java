package com.pdv.view;

import com.pdv.model.MetodoPagamento;
import com.pdv.util.Numeros;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;

/**
 * Janela de pagamento. Nesta etapa a confirmação é manual (operador).
 * Na Etapa 6 os cartões e o PIX passam a ser confirmados pela maquininha/provedor.
 */
public class PagamentoDialog extends JDialog {

    private final BigDecimal total;

    private MetodoPagamento metodo = MetodoPagamento.DINHEIRO;
    private boolean confirmado;
    private BigDecimal troco = BigDecimal.ZERO;
    private int parcelas = 1;
    private String referencia = "";

    private final CardLayout cartoes = new CardLayout();
    private final JPanel painelCartoes = new JPanel(cartoes);

    private final JTextField txtRecebido = new JTextField();
    private final JLabel lblTroco = new JLabel(" ");
    private final JSpinner spnParcelas = new JSpinner(new SpinnerNumberModel(1, 1, 12, 1));
    private final JTextField txtRefCartao = new JTextField();
    private final JTextField txtRefPix = new JTextField();
    private final JCheckBox chkConfirma = new JCheckBox("Confirmo que o pagamento foi aprovado");
    private final JButton btnConfirmar = new JButton("Confirmar pagamento");

    public PagamentoDialog(Window dono, long vendaId, BigDecimal total) {
        super(dono, "Pagamento - Venda #" + vendaId, Dialog.ModalityType.APPLICATION_MODAL);
        this.total = total;

        JPanel raiz = new JPanel(new BorderLayout(0, 14));
        raiz.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        setContentPane(raiz);

        // ---- topo: total
        JPanel topo = new JPanel(new GridLayout(2, 1));
        JLabel l1 = new JLabel("Total a pagar");
        l1.setForeground(Estilo.MUTED);
        JLabel lblTotal = new JLabel(Numeros.moeda(total));
        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD, 34f));
        lblTotal.setForeground(Estilo.TEXTO);
        topo.add(l1);
        topo.add(lblTotal);
        raiz.add(topo, BorderLayout.NORTH);

        // ---- centro: formas de pagamento + campos
        JPanel centro = new JPanel(new BorderLayout(0, 14));

        JPanel formas = new JPanel(new GridLayout(1, 4, 8, 0));
        Chip cDinheiro = new Chip("Dinheiro");
        Chip cDebito = new Chip("Débito");
        Chip cCredito = new Chip("Crédito");
        Chip cPix = new Chip("PIX");
        ButtonGroup grupo = new ButtonGroup();
        for (Chip c : new Chip[]{cDinheiro, cDebito, cCredito, cPix}) {
            grupo.add(c);
            formas.add(c);
        }
        cDinheiro.setSelected(true);
        cDinheiro.addActionListener(e -> selecionar(MetodoPagamento.DINHEIRO));
        cDebito.addActionListener(e -> selecionar(MetodoPagamento.CARTAO_DEBITO));
        cCredito.addActionListener(e -> selecionar(MetodoPagamento.CARTAO_CREDITO));
        cPix.addActionListener(e -> selecionar(MetodoPagamento.PIX));
        centro.add(formas, BorderLayout.NORTH);

        painelCartoes.add(montarDinheiro(), "DINHEIRO");
        painelCartoes.add(montarCartao(), "CARTAO");
        painelCartoes.add(montarPix(), "PIX");
        centro.add(painelCartoes, BorderLayout.CENTER);
        raiz.add(centro, BorderLayout.CENTER);

        // ---- rodapé
        JPanel rodape = new JPanel(new BorderLayout(0, 10));
        rodape.add(chkConfirma, BorderLayout.NORTH);

        JPanel botoes = new JPanel(new GridLayout(1, 2, 10, 0));
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setPreferredSize(new Dimension(0, 46));
        btnConfirmar.setPreferredSize(new Dimension(0, 46));
        btnConfirmar.setBackground(Estilo.VERDE);
        btnConfirmar.setForeground(Color.WHITE);
        btnConfirmar.setFont(btnConfirmar.getFont().deriveFont(Font.BOLD));
        botoes.add(btnCancelar);
        botoes.add(btnConfirmar);
        rodape.add(botoes, BorderLayout.SOUTH);
        raiz.add(rodape, BorderLayout.SOUTH);

        btnCancelar.addActionListener(e -> dispose());
        btnConfirmar.addActionListener(e -> confirmar());
        chkConfirma.addActionListener(e -> atualizar());
        aoMudar(txtRecebido, this::atualizar);
        getRootPane().setDefaultButton(btnConfirmar);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "fechar");
        getRootPane().getActionMap().put("fechar", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { dispose(); }
        });

        selecionar(MetodoPagamento.DINHEIRO);
        setSize(520, 500);
        setLocationRelativeTo(dono);
    }

    // ------------------------------------------------------------ painéis

    private JPanel montarDinheiro() {
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 6));
        p.add(new JLabel("Valor recebido"));
        txtRecebido.setFont(txtRecebido.getFont().deriveFont(Font.BOLD, 22f));
        txtRecebido.setPreferredSize(new Dimension(100, 48));
        p.add(txtRecebido);
        lblTroco.setFont(lblTroco.getFont().deriveFont(Font.BOLD, 18f));
        p.add(lblTroco);
        JButton exato = new JButton("Valor exato");
        exato.addActionListener(e -> {
            txtRecebido.setText(Numeros.decimal(total, 2));
            txtRecebido.requestFocusInWindow();
        });
        p.add(exato);

        JPanel envolver = new JPanel(new BorderLayout());
        envolver.add(p, BorderLayout.NORTH);
        return envolver;
    }

    private JPanel montarCartao() {
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 6));
        p.add(new JLabel("Parcelas (somente crédito)"));
        p.add(spnParcelas);
        p.add(new JLabel("NSU / comprovante (opcional)"));
        p.add(txtRefCartao);
        p.add(nota());

        JPanel envolver = new JPanel(new BorderLayout());
        envolver.add(p, BorderLayout.NORTH);
        return envolver;
    }

    private JPanel montarPix() {
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 6));
        p.add(new JLabel("ID da transação PIX (opcional)"));
        p.add(txtRefPix);
        p.add(nota());

        JPanel envolver = new JPanel(new BorderLayout());
        envolver.add(p, BorderLayout.NORTH);
        return envolver;
    }

    private JLabel nota() {
        JLabel l = new JLabel("<html>A confirmação automática pela maquininha/PIX chega na próxima etapa. "
                + "Por enquanto, confirme manualmente depois de ver a aprovação.</html>");
        l.setForeground(Estilo.MUTED);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 12f));
        return l;
    }

    // ------------------------------------------------------------ lógica

    private void selecionar(MetodoPagamento m) {
        this.metodo = m;
        if (m == MetodoPagamento.DINHEIRO) {
            cartoes.show(painelCartoes, "DINHEIRO");
        } else if (m == MetodoPagamento.PIX) {
            cartoes.show(painelCartoes, "PIX");
        } else {
            cartoes.show(painelCartoes, "CARTAO");
        }
        spnParcelas.setEnabled(m == MetodoPagamento.CARTAO_CREDITO);
        if (m != MetodoPagamento.CARTAO_CREDITO) {
            spnParcelas.setValue(1);
        }
        chkConfirma.setVisible(m != MetodoPagamento.DINHEIRO);
        chkConfirma.setSelected(false);
        atualizar();
        if (m == MetodoPagamento.DINHEIRO) {
            txtRecebido.requestFocusInWindow();
        }
    }

    private BigDecimal lerRecebido() {
        try {
            return Numeros.parse(txtRecebido.getText());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private void atualizar() {
        if (metodo == MetodoPagamento.DINHEIRO) {
            BigDecimal recebido = lerRecebido();
            BigDecimal diferenca = recebido.subtract(total);
            if (recebido.signum() == 0) {
                lblTroco.setText(" ");
                lblTroco.setForeground(Estilo.TEXTO);
            } else if (diferenca.signum() >= 0) {
                lblTroco.setText("Troco: " + Numeros.moeda(diferenca));
                lblTroco.setForeground(Estilo.VERDE);
            } else {
                lblTroco.setText("Faltam " + Numeros.moeda(diferenca.negate()));
                lblTroco.setForeground(Estilo.VERMELHO);
            }
            btnConfirmar.setEnabled(recebido.compareTo(total) >= 0);
        } else {
            btnConfirmar.setEnabled(chkConfirma.isSelected());
        }
    }

    private void confirmar() {
        if (metodo == MetodoPagamento.DINHEIRO) {
            troco = lerRecebido().subtract(total);
            referencia = "";
            parcelas = 1;
        } else if (metodo == MetodoPagamento.PIX) {
            troco = BigDecimal.ZERO;
            referencia = txtRefPix.getText().trim();
            parcelas = 1;
        } else {
            troco = BigDecimal.ZERO;
            referencia = txtRefCartao.getText().trim();
            parcelas = metodo == MetodoPagamento.CARTAO_CREDITO ? (Integer) spnParcelas.getValue() : 1;
        }
        confirmado = true;
        dispose();
    }

    private static void aoMudar(JTextComponent campo, Runnable acao) {
        campo.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { acao.run(); }
            @Override public void removeUpdate(DocumentEvent e) { acao.run(); }
            @Override public void changedUpdate(DocumentEvent e) { acao.run(); }
        });
    }

    public boolean isConfirmado() { return confirmado; }
    public MetodoPagamento getMetodo() { return metodo; }
    public BigDecimal getTroco() { return troco; }
    public int getParcelas() { return parcelas; }
    public String getReferencia() { return referencia; }

    /** Botão de seleção arredondado: azul quando escolhido. */
    private static class Chip extends JToggleButton {
        Chip(String texto) {
            super(texto);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(getFont().deriveFont(Font.BOLD, 14f));
            setForeground(Estilo.TEXTO);
            setPreferredSize(new Dimension(0, 48));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addItemListener(e -> setForeground(isSelected() ? Color.WHITE : Estilo.TEXTO));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (isSelected()) {
                g2.setColor(Estilo.ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            } else {
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(Estilo.BORDA);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
