package br.edu.ifsuldeminas.sd.chat.client;

import br.edu.ifsuldeminas.sd.chat.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Interface gráfica moderna para o Chat UDP/TCP.
 *
 * Tema: "Cosmic Dark"
 *  - Fundo profundo azul-marinho
 *  - Mensagens enviadas: bolhas índigo (direita)
 *  - Mensagens recebidas: bolhas escuras com borda (esquerda)
 *  - Toggle animado UDP ↔ TCP
 *  - Exibe automaticamente o IP local para facilitar configuração em rede
 *
 * Implementa MessageContainer — chamado pela thread do Receiver.
 */
public class SwingChatView extends JFrame implements MessageContainer {

    // ─── Paleta ──────────────────────────────────────────────────────────────
    static final Color BG_DEEP     = new Color(  7,   7,  17);
    static final Color BG_SURFACE  = new Color( 13,  13,  26);
    static final Color BG_ELEVATED = new Color( 20,  20,  38);
    static final Color BG_INPUT    = new Color( 26,  26,  46);
    static final Color INDIGO      = new Color( 79,  70, 229);
    static final Color INDIGO_HVR  = new Color( 99,  88, 243);
    static final Color INDIGO_LT   = new Color(129, 140, 248);
    static final Color EMERALD     = new Color( 16, 185, 129);
    static final Color ROSE        = new Color(244,  63,  94);
    static final Color AMBER       = new Color(251, 191,  36);
    static final Color TEXT_PRI    = new Color(241, 245, 249);
    static final Color TEXT_SEC    = new Color(148, 163, 184);
    static final Color TEXT_MUT    = new Color( 71,  85, 105);
    static final Color BORDER      = new Color( 37,  37,  64);
    static final Color SENT_BG     = new Color( 67,  56, 202);
    static final Color RECV_BG     = new Color( 22,  22,  45);

    // ─── Fontes ──────────────────────────────────────────────────────────────
    static final Font F_TITLE  = font("Segoe UI", Font.BOLD,  17);
    static final Font F_LABEL  = font("Segoe UI", Font.PLAIN, 11);
    static final Font F_FIELD  = font("Segoe UI", Font.PLAIN, 13);
    static final Font F_MSG    = font("Segoe UI", Font.PLAIN, 14);
    static final Font F_META   = font("Segoe UI", Font.PLAIN, 11);
    static final Font F_BTN    = font("Segoe UI", Font.BOLD,  13);

    private static Font font(String name, int style, int size) {
        Font f = new Font(name, style, size);
        // Fallback se Segoe UI não estiver disponível
        return f.getFamily().equals(name) ? f : new Font(Font.SANS_SERIF, style, size);
    }

    // ─── Campos de conexão ───────────────────────────────────────────────────
    private JTextField    localPortField;
    private JTextField    remoteIPField;
    private JTextField    remotePortField;
    private JTextField    usernameField;
    private ProtocolToggle protoToggle;
    private JButton       connectButton;
    private JLabel        myIPLabel;

    // ─── Área de mensagens ───────────────────────────────────────────────────
    private JPanel        messagesPanel;
    private JScrollPane   messagesScroll;

    // ─── Barra de entrada ────────────────────────────────────────────────────
    private JTextField    messageField;
    private JButton       sendButton;

    // ─── Status ──────────────────────────────────────────────────────────────
    private JLabel        dotLabel;
    private JLabel        statusLabel;

    // ─── Estado ──────────────────────────────────────────────────────────────
    private Sender  sender;
    private String  username  = "Usuário";
    private boolean connected = false;

    // ═════════════════════════════════════════════════════════════════════════

    public SwingChatView() {
        configFrame();
        buildUI();
        detectLocalIP();
        setVisible(true);
    }

    private void configFrame() {
        setTitle("NEXUS CHAT");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(940, 700);
        setMinimumSize(new Dimension(720, 520));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DEEP);
        setLayout(new BorderLayout());
    }

    private void buildUI() {
        add(buildHeader(),   BorderLayout.NORTH);
        add(buildCenter(),   BorderLayout.CENTER);
        add(buildInputBar(), BorderLayout.SOUTH);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HEADER
    // ══════════════════════════════════════════════════════════════════════════

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_SURFACE);
        panel.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));

        // ── Linha 1: logo + toggle ───────────────────────────────────────────
        JPanel row1 = new JPanel(new BorderLayout());
        row1.setBackground(BG_SURFACE);
        row1.setBorder(new EmptyBorder(14, 20, 8, 20));

        JLabel logo = new JLabel("◈  NEXUS CHAT");
        logo.setFont(F_TITLE);
        logo.setForeground(INDIGO_LT);
        row1.add(logo, BorderLayout.WEST);

        JPanel toggleArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        toggleArea.setBackground(BG_SURFACE);
        JLabel protoLbl = new JLabel("protocolo  ");
        protoLbl.setFont(F_LABEL);
        protoLbl.setForeground(TEXT_MUT);
        protoToggle = new ProtocolToggle();
        toggleArea.add(protoLbl);
        toggleArea.add(protoToggle);
        row1.add(toggleArea, BorderLayout.EAST);

        // ── Linha 2: campos de conexão ───────────────────────────────────────
        JPanel row2 = new JPanel(new GridBagLayout());
        row2.setBackground(BG_SURFACE);
        row2.setBorder(new EmptyBorder(0, 20, 14, 20));

        GridBagConstraints g = new GridBagConstraints();
        g.insets  = new Insets(3, 5, 3, 5);
        g.fill    = GridBagConstraints.HORIZONTAL;
        g.weighty = 0;

        // Linha 2a: Porta local | IP remoto | Porta remota
        g.gridy = 0;
        addLabelField(row2, g, 0, "PORTA LOCAL",  localPortField  = field("5000"),       0.15);
        addLabelField(row2, g, 2, "IP REMOTO",    remoteIPField   = field("192.168.x.x"), 1.0);
        addLabelField(row2, g, 4, "PORTA REMOTA", remotePortField = field("5001"),        0.15);

        // Linha 2b: Usuário | IP local (info) | Botão
        g.gridy = 1;
        g.gridx = 0; g.weightx = 0;
        row2.add(lbl("USUÁRIO"), g);

        usernameField = field("Seu nome");
        g.gridx = 1; g.weightx = 1.0; g.gridwidth = 3;
        row2.add(usernameField, g);
        g.gridwidth = 1;

        myIPLabel = new JLabel("Seu IP: detectando...");
        myIPLabel.setFont(F_LABEL);
        myIPLabel.setForeground(TEXT_MUT);
        g.gridx = 4; g.weightx = 0;
        row2.add(myIPLabel, g);

        connectButton = buildButton("Conectar", INDIGO, Color.WHITE);
        connectButton.addActionListener(e -> handleConnect());
        g.gridx = 5; g.weightx = 0;
        row2.add(connectButton, g);

        panel.add(row1, BorderLayout.NORTH);
        panel.add(row2, BorderLayout.CENTER);
        return panel;
    }

    private void addLabelField(JPanel p, GridBagConstraints g,
            int col, String lblText, JTextField field, double weight) {
        g.gridx = col;     g.weightx = 0;      p.add(lbl(lblText), g);
        g.gridx = col + 1; g.weightx = weight; p.add(field, g);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CENTRO — STATUS + MENSAGENS
    // ══════════════════════════════════════════════════════════════════════════

    private JPanel buildCenter() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG_DEEP);

        // Status bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(BG_ELEVATED);
        statusBar.setBorder(new EmptyBorder(6, 20, 6, 20));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setBackground(BG_ELEVATED);
        dotLabel = new JLabel("●");
        dotLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        dotLabel.setForeground(TEXT_MUT);
        statusLabel = new JLabel("Desconectado — configure os campos e clique em Conectar");
        statusLabel.setFont(F_LABEL);
        statusLabel.setForeground(TEXT_MUT);
        left.add(dotLabel);
        left.add(statusLabel);
        statusBar.add(left, BorderLayout.WEST);

        outer.add(statusBar, BorderLayout.NORTH);

        // Painel de mensagens (BoxLayout vertical)
        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(BG_DEEP);
        messagesPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        messagesScroll = new JScrollPane(messagesPanel);
        messagesScroll.setBorder(BorderFactory.createEmptyBorder());
        messagesScroll.getViewport().setBackground(BG_DEEP);
        messagesScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        styleScrollbar(messagesScroll.getVerticalScrollBar());

        outer.add(messagesScroll, BorderLayout.CENTER);

        // Mensagem inicial
        appendSys("Sistema inicializado. Preencha os campos acima e clique em Conectar.");
        return outer;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BARRA DE ENTRADA
    // ══════════════════════════════════════════════════════════════════════════

    private JPanel buildInputBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBackground(BG_SURFACE);
        bar.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER),
            new EmptyBorder(12, 20, 14, 20)
        ));

        messageField = new JTextField();
        messageField.setBackground(BG_INPUT);
        messageField.setForeground(TEXT_PRI);
        messageField.setCaretColor(INDIGO_LT);
        messageField.setFont(F_FIELD);
        messageField.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER, 1),
            new EmptyBorder(11, 14, 11, 14)
        ));
        messageField.setEnabled(false);

        // Placeholder
        setPlaceholder(messageField, "Escreva uma mensagem...");

        messageField.addActionListener(e -> handleSend());

        sendButton = buildButton("Enviar  ➤", INDIGO, Color.WHITE);
        sendButton.setPreferredSize(new Dimension(120, 44));
        sendButton.setEnabled(false);
        sendButton.addActionListener(e -> handleSend());

        bar.add(messageField, BorderLayout.CENTER);
        bar.add(sendButton,   BorderLayout.EAST);
        return bar;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LÓGICA — CONECTAR
    // ══════════════════════════════════════════════════════════════════════════

    private void handleConnect() {
        if (connected) return;

        String localPortStr  = localPortField.getText().trim();
        String remotePortStr = remotePortField.getText().trim();
        String ip            = remoteIPField.getText().trim();
        String nick          = usernameField.getText().trim();

        if (localPortStr.isEmpty() || remotePortStr.isEmpty() || ip.isEmpty()) {
            appendErr("Preencha todos os campos antes de conectar.");
            return;
        }

        int lp, rp;
        try {
            lp = Integer.parseInt(localPortStr);
            rp = Integer.parseInt(remotePortStr);
        } catch (NumberFormatException e) {
            appendErr("As portas devem ser números inteiros válidos.");
            return;
        }

        username = (nick.isEmpty() || nick.equals("Seu nome")) ? "Usuário" : nick;
        boolean tcp = protoToggle.isTCP();

        try {
            sender    = tcp ? TCPChatFactory.build(ip, rp, lp, this)
                            : ChatFactory.build(ip, rp, lp, this);
            connected = true;
            String proto = tcp ? "TCP" : "UDP";

            connectButton.setText("Conectado  ✓");
            connectButton.setBackground(EMERALD);
            connectButton.setEnabled(false);
            localPortField.setEnabled(false);
            remotePortField.setEnabled(false);
            remoteIPField.setEnabled(false);
            usernameField.setEnabled(false);
            protoToggle.setEnabled(false);
            messageField.setEnabled(true);
            sendButton.setEnabled(true);
            messageField.requestFocus();

            dotLabel.setForeground(EMERALD);
            statusLabel.setForeground(EMERALD);
            statusLabel.setText("[" + username + "]  •  " + proto
                + "  •  local:" + lp + "  →  " + ip + ":" + rp);

            appendSys("Conectado como [" + username + "] via " + proto
                + "  |  local:" + lp + "  →  " + ip + ":" + rp);

            if (tcp) {
                appendSys("TCP: ambos os lados devem estar conectados antes de enviar a primeira mensagem.");
            }

        } catch (ChatException ex) {
            appendErr("Erro ao conectar: " + ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LÓGICA — ENVIAR
    // ══════════════════════════════════════════════════════════════════════════

    private void handleSend() {
        if (!connected || sender == null) return;
        String text = messageField.getText().trim();
        if (text.isEmpty() || text.equals("Escreva uma mensagem...")) return;

        String payload = text + MessageContainer.FROM + username;
        try {
            sender.send(payload);
            appendSent(text, username);
            messageField.setText("");
        } catch (ChatException ex) {
            appendErr("Falha ao enviar: " + ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  MessageContainer — chamado pela thread do Receiver
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void newMessage(String message) {
        if (message == null || message.trim().isEmpty()) return;
        String[] parts = message.trim().split(MessageContainer.FROM, 2);
        if (parts.length < 2) return;
        String text = parts[0].trim();
        String from = parts[1].trim();
        if (text.isEmpty()) return;
        SwingUtilities.invokeLater(() -> appendReceived(text, from));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BOLHAS DE MENSAGEM
    // ══════════════════════════════════════════════════════════════════════════

    /** Mensagem enviada — bolha índigo, alinhada à direita */
    private void appendSent(String text, String from) {
        SwingUtilities.invokeLater(() -> {
            JPanel row = msgRow();

            Bubble bubble = new Bubble(SENT_BG, true);
            bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
            bubble.setBorder(new EmptyBorder(10, 14, 10, 14));

            JLabel txt = htmlLabel(text, Color.WHITE, 300);
            txt.setAlignmentX(Component.RIGHT_ALIGNMENT);
            JLabel meta = metaLabel(ts(), new Color(200, 196, 255));
            meta.setAlignmentX(Component.RIGHT_ALIGNMENT);

            bubble.add(txt);
            bubble.add(Box.createVerticalStrut(4));
            bubble.add(meta);

            // Nome acima da bolha (à direita)
            JLabel nameLbl = new JLabel("você  •  " + from);
            nameLbl.setFont(F_META);
            nameLbl.setForeground(TEXT_MUT);

            JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 1));
            nameRow.setBackground(BG_DEEP);
            nameRow.add(nameLbl);

            JPanel bubbleRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
            bubbleRow.setBackground(BG_DEEP);
            bubbleRow.add(bubble);

            row.add(nameRow);
            row.add(bubbleRow);
            addRow(row);
        });
    }

    /** Mensagem recebida — bolha escura, alinhada à esquerda */
    private void appendReceived(String text, String from) {
        JPanel row = msgRow();

        JLabel nameLbl = new JLabel(from);
        nameLbl.setFont(F_META);
        nameLbl.setForeground(INDIGO_LT);

        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 1));
        nameRow.setBackground(BG_DEEP);
        nameRow.add(nameLbl);

        Bubble bubble = new Bubble(RECV_BG, false);
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel txt  = htmlLabel(text, TEXT_PRI, 300);
        txt.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel meta = metaLabel(ts(), TEXT_MUT);
        meta.setAlignmentX(Component.LEFT_ALIGNMENT);

        bubble.add(txt);
        bubble.add(Box.createVerticalStrut(4));
        bubble.add(meta);

        JPanel bubbleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        bubbleRow.setBackground(BG_DEEP);
        bubbleRow.add(bubble);

        row.add(nameRow);
        row.add(bubbleRow);
        addRow(row);
    }

    /** Mensagem do sistema — centralizada, texto discreto */
    private void appendSys(String text) {
        SwingUtilities.invokeLater(() -> {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
            row.setBackground(BG_DEEP);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            JLabel lbl = new JLabel(text);
            lbl.setFont(F_META);
            lbl.setForeground(TEXT_MUT);
            row.add(lbl);
            addRow(row);
        });
    }

    /** Mensagem de erro — centralizada, vermelho */
    private void appendErr(String text) {
        SwingUtilities.invokeLater(() -> {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
            row.setBackground(BG_DEEP);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            JLabel lbl = new JLabel("⚠  " + text);
            lbl.setFont(F_META);
            lbl.setForeground(ROSE);
            row.add(lbl);
            addRow(row);
        });
    }

    private JPanel msgRow() {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setBackground(BG_DEEP);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return row;
    }

    private void addRow(JPanel row) {
        messagesPanel.add(row);
        messagesPanel.add(Box.createVerticalStrut(6));
        messagesPanel.revalidate();
        // Duplo invokeLater garante scroll após o layout ser calculado
        SwingUtilities.invokeLater(() ->
            SwingUtilities.invokeLater(() -> {
                JScrollBar bar = messagesScroll.getVerticalScrollBar();
                bar.setValue(bar.getMaximum());
            })
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UTILS
    // ══════════════════════════════════════════════════════════════════════════

    private JLabel htmlLabel(String text, Color color, int maxWidth) {
        JLabel l = new JLabel("<html><body style='width:" + maxWidth + "px'>"
            + escHtml(text) + "</body></html>");
        l.setFont(F_MSG);
        l.setForeground(color);
        return l;
    }

    private JLabel metaLabel(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(F_META);
        l.setForeground(color);
        return l;
    }

    private JLabel lbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(F_LABEL);
        l.setForeground(TEXT_MUT);
        return l;
    }

    private JTextField field(String placeholder) {
        JTextField f = new JTextField(placeholder);
        f.setBackground(BG_INPUT);
        f.setForeground(TEXT_PRI);
        f.setCaretColor(INDIGO_LT);
        f.setFont(F_FIELD);
        f.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER, 1),
            new EmptyBorder(6, 10, 6, 10)
        ));
        return f;
    }

    private JButton buildButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(F_BTN);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(8, 18, 8, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            final Color base = bg;
            @Override public void mouseEntered(MouseEvent e) {
                if (b.isEnabled()) b.setBackground(base.brighter());
            }
            @Override public void mouseExited(MouseEvent e) { b.setBackground(base); }
        });
        return b;
    }

    private void setPlaceholder(JTextField f, String placeholder) {
        f.setText(placeholder);
        f.setForeground(TEXT_MUT);
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (f.getText().equals(placeholder)) {
                    f.setText("");
                    f.setForeground(TEXT_PRI);
                }
                f.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(INDIGO, 1), new EmptyBorder(11, 14, 11, 14)));
            }
            @Override public void focusLost(FocusEvent e) {
                if (f.getText().isEmpty()) {
                    f.setText(placeholder);
                    f.setForeground(TEXT_MUT);
                }
                f.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(BORDER, 1), new EmptyBorder(11, 14, 11, 14)));
            }
        });
    }

    private void styleScrollbar(JScrollBar bar) {
        bar.setBackground(BG_DEEP);
        bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = BG_ELEVATED;
                trackColor = BG_DEEP;
                thumbHighlightColor = INDIGO;
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroBtn(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroBtn(); }
            private JButton zeroBtn() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });
    }

    private void detectLocalIP() {
        new Thread(() -> {
            try {
                String ip = InetAddress.getLocalHost().getHostAddress();
                SwingUtilities.invokeLater(() -> {
                    myIPLabel.setText("Seu IP: " + ip);
                    myIPLabel.setForeground(EMERALD);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> myIPLabel.setText("IP: indisponível"));
            }
        }).start();
    }

    private String ts() {
        return new SimpleDateFormat("HH:mm").format(new Date());
    }

    private String escHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  INNER CLASS — Bolha com cantos arredondados (Graphics2D)
    // ══════════════════════════════════════════════════════════════════════════

    static class Bubble extends JPanel {
        private final Color  color;
        private final boolean sent;
        private static final int R = 18; // raio dos cantos

        Bubble(Color color, boolean sent) {
            this.color = color;
            this.sent  = sent;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            // Canto pontiagudo no lado da "cauda" da bolha
            int w = getWidth(), h = getHeight();
            if (sent) {
                // Canto inferior direito mais fechado
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, R, R));
                g2.fillRect(w - R, h - R, R, R); // corta canto inf-dir
            } else {
                // Canto inferior esquerdo mais fechado
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, R, R));
                g2.fillRect(0, h - R, R, R); // corta canto inf-esq
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  INNER CLASS — Toggle animado UDP / TCP
    // ══════════════════════════════════════════════════════════════════════════

    static class ProtocolToggle extends JPanel {
        private boolean tcp = false;
        private float   anim = 0f; // 0 = UDP, 1 = TCP (para transição suave)
        private Timer   timer;

        ProtocolToggle() {
            setPreferredSize(new Dimension(150, 34));
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            timer = new Timer(12, e -> {
                float target = tcp ? 1f : 0f;
                anim += (target - anim) * 0.25f;
                if (Math.abs(anim - target) < 0.01f) { anim = target; timer.stop(); }
                repaint();
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!isEnabled()) return;
                    tcp = !tcp;
                    timer.start();
                }
            });
        }

        public boolean isTCP() { return tcp; }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight(), half = w / 2;

            // Fundo pill
            g2.setColor(BG_ELEVATED);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));

            // Indicador deslizante
            float slideX = 2 + anim * (half - 2);
            g2.setColor(INDIGO);
            g2.fill(new RoundRectangle2D.Float(slideX, 2, half - 2, h - 4, h - 4, h - 4));

            // Textos
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();
            int textY = (h + fm.getAscent() - fm.getDescent()) / 2;

            // UDP
            Color udpColor = blend(Color.WHITE, TEXT_MUT, anim);
            g2.setColor(udpColor);
            String u = "UDP";
            g2.drawString(u, (half - fm.stringWidth(u)) / 2, textY);

            // TCP
            Color tcpColor = blend(TEXT_MUT, Color.WHITE, anim);
            g2.setColor(tcpColor);
            String t = "TCP";
            g2.drawString(t, half + (half - fm.stringWidth(t)) / 2, textY);

            g2.dispose();
        }

        private Color blend(Color a, Color b, float t) {
            return new Color(
                (int)(a.getRed()   + (b.getRed()   - a.getRed())   * t),
                (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int)(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t)
            );
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  MAIN
    // ══════════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                UIManager.put("Panel.background",     BG_DEEP);
                UIManager.put("ScrollPane.background", BG_DEEP);
                UIManager.put("Viewport.background",   BG_DEEP);
                UIManager.put("ToolTip.background",    BG_ELEVATED);
                UIManager.put("ToolTip.foreground",    TEXT_PRI);
            } catch (Exception ignored) {}
            new SwingChatView();
        });
    }
}
