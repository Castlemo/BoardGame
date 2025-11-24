package com.marblegame.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

/**
 * 게임 채팅 패널 (오른쪽 소셜 패널 하단)
 * - 상단바(제목 + 최소화)
 * - 메시지 영역(스크롤)
 * - 입력 영역([텍스트][보내기] + 이모지 버튼)
 */
public class ChatPanel extends JPanel {
    private static final Color BG = UIConstants.BACKGROUND_DARK;
    private static final Color PANEL_BG = UIConstants.PANEL_DARK;
    private static final Color BORDER = UIConstants.BORDER_DEFAULT;
    private static final Color INPUT_BG = UIConstants.INPUT_BG;
    private static final Color TEXT_PRIMARY = UIConstants.TEXT_PRIMARY;
    private static final Color TEXT_SECONDARY = UIConstants.TEXT_SECONDARY;
    private static final Color SEND_COLOR = UIConstants.BUTTON_ROLL;

    // 플레이어 색상 (채팅 말풍선)
    private static final Color[] PLAYER_COLORS = {
        new Color(231, 76, 60),   // Red
        new Color(52, 152, 219),  // Blue
        new Color(46, 204, 113),  // Green
        new Color(230, 126, 34)   // Orange
    };

    private final JTextPane messagePane;
    private final JScrollPane scrollPane;
    private final JTextField inputField;
    private final JButton sendButton;
    private final JButton toggleButton;
    private final JPanel bodyPanel;

    private Consumer<String> messageSendCallback;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    public ChatPanel() {
        setLayout(new BorderLayout(0, 10));
        setOpaque(true);
        setBackground(BG);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 2),
            new EmptyBorder(12, 12, 12, 12)
        ));

        // 헤더
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Chat");
        title.setFont(UIConstants.FONT_HEADER);
        title.setForeground(TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);

        toggleButton = new JButton("\u2212"); // minus sign
        toggleButton.setFocusable(false);
        toggleButton.setBorderPainted(false);
        toggleButton.setContentAreaFilled(false);
        toggleButton.setForeground(TEXT_SECONDARY);
        toggleButton.setFont(new Font(UIConstants.FONT_NAME, Font.BOLD, 16));
        toggleButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggleButton.addActionListener(e -> toggleBody());
        header.add(toggleButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // 본문(메시지 + 입력)
        bodyPanel = new JPanel(new BorderLayout(0, 8));
        bodyPanel.setOpaque(false);

        messagePane = new JTextPane();
        messagePane.setEditable(false);
        messagePane.setContentType("text/html");
        messagePane.setText(getInitialHTML());
        messagePane.setOpaque(true);
        messagePane.setBackground(PANEL_BG);
        messagePane.setBorder(new EmptyBorder(10, 12, 10, 12));

        scrollPane = new JScrollPane(messagePane);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        customizeScrollBar(scrollPane.getVerticalScrollBar());
        bodyPanel.add(scrollPane, BorderLayout.CENTER);

        // 입력 영역
        JPanel inputWrapper = new JPanel();
        inputWrapper.setLayout(new BoxLayout(inputWrapper, BoxLayout.Y_AXIS));
        inputWrapper.setOpaque(false);

        JPanel sendRow = new JPanel(new BorderLayout(6, 0));
        sendRow.setOpaque(false);
        inputField = new JTextField();
        inputField.setFont(UIConstants.FONT_BODY);
        inputField.setBackground(INPUT_BG);
        inputField.setForeground(TEXT_PRIMARY);
        inputField.setCaretColor(TEXT_PRIMARY);
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(8, 10, 8, 10)
        ));
        inputField.addActionListener(e -> sendMessage());
        sendRow.add(inputField, BorderLayout.CENTER);

        sendButton = new JButton("보내기");
        sendButton.setFont(UIConstants.FONT_BODY_BOLD);
        sendButton.setBackground(SEND_COLOR);
        sendButton.setForeground(TEXT_PRIMARY);
        sendButton.setFocusPainted(false);
        sendButton.setBorderPainted(false);
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendButton.setPreferredSize(new Dimension(86, 34));
        sendButton.addActionListener(e -> sendMessage());
        sendRow.add(sendButton, BorderLayout.EAST);

        inputWrapper.add(sendRow);
        bodyPanel.add(inputWrapper, BorderLayout.SOUTH);

        add(bodyPanel, BorderLayout.CENTER);

        // 초기 시스템 메시지
        addSystemMessage("채팅이 시작 되었습니다.");
    }

    private void toggleBody() {
        boolean nowVisible = !bodyPanel.isVisible();
        bodyPanel.setVisible(nowVisible);
        toggleButton.setText(nowVisible ? "\u2212" : "\u25be"); // minus / down arrow
        revalidate();
    }

    private String getInitialHTML() {
        return "<html><head><style>" +
            "body { font-family:'" + UIConstants.FONT_NAME + "'; font-size:12px; margin:0; background:#2c3e50; color:#ecf0f1; }" +
            ".msg { margin:6px 0; line-height:1.4; }" +
            ".time { color:#ecf0f1; font-size:11px; }" +
            ".name { font-weight:bold; }" +
            ".text { color:#ecf0f1; }" +
            ".system { text-align:center; color:#95a5a6; font-style:italic; margin:8px 0; white-space:nowrap; }" +
            "</style></head><body></body></html>";
    }

    private void customizeScrollBar(JScrollBar scrollBar) {
        scrollBar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(127, 140, 141);
                this.trackColor = new Color(44, 62, 80);
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                return button;
            }
        });
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        if (messageSendCallback != null) {
            messageSendCallback.accept(text);
        }
        inputField.setText("");
    }

    /**
     * 플레이어 메시지 추가 (색상 말풍선)
     */
    public void addPlayerMessage(int playerIndex, String playerName, String message) {
        String time = timeFormat.format(new Date());
        Color playerColor = PLAYER_COLORS[playerIndex % PLAYER_COLORS.length];
        String colorHex = String.format("#%02x%02x%02x", playerColor.getRed(), playerColor.getGreen(), playerColor.getBlue());

        String htmlMessage = String.format(
            "<div class='msg'>" +
            "<span class='time'>%s</span> " +
            "<span class='name' style='color:%s'>%s</span> " +
            "<span class='text'>%s</span>" +
            "</div>",
            time,
            colorHex,
            escapeHTML(playerName),
            escapeHTML(message)
        );

        appendToChat(htmlMessage);
    }

    /**
     * 시스템 메시지 추가
     */
    public void addSystemMessage(String message) {
        String htmlMessage = String.format(
            "<div class='system'>-- %s --</div>",
            escapeHTML(message)
        );
        appendToChat(htmlMessage);
    }

    private void appendToChat(String htmlContent) {
        SwingUtilities.invokeLater(() -> {
            try {
                javax.swing.text.html.HTMLDocument htmlDoc = (javax.swing.text.html.HTMLDocument) messagePane.getDocument();
                javax.swing.text.html.HTMLEditorKit kit = (javax.swing.text.html.HTMLEditorKit) messagePane.getEditorKit();
                kit.insertHTML(htmlDoc, htmlDoc.getLength(), htmlContent, 0, 0, null);
                messagePane.setCaretPosition(htmlDoc.getLength());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private String escapeHTML(String text) {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    /**
     * 메시지 전송 콜백 설정
     */
    public void setMessageSendCallback(Consumer<String> callback) {
        this.messageSendCallback = callback;
    }

    /**
     * 채팅 초기화
     */
    public void clearChat() {
        messagePane.setText(getInitialHTML());
        addSystemMessage("채팅이 초기화되었습니다.");
    }
}
