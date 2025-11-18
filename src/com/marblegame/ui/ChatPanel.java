package com.marblegame.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

/**
 * 게임 내 채팅 패널
 * 보드 우측에 위치하며, 메시지 표시와 입력 기능 제공
 */
public class ChatPanel extends JPanel {
    // UIConstants에서 가져온 색상 (로컬 별칭)
    private static final Color BACKGROUND = UIConstants.BORDER_DEFAULT; // 52, 73, 94
    private static final Color TEXT_PRIMARY = UIConstants.TEXT_PRIMARY;
    private static final Color TEXT_SECONDARY = UIConstants.TEXT_SECONDARY;
    private static final Color BUTTON_COLOR = UIConstants.BUTTON_ROLL;
    private static final Color MESSAGE_BG = UIConstants.PANEL_DARK;
    private static final Color INPUT_BG = UIConstants.INPUT_BG;

    private JTextPane chatArea;
    private JButton chatButton;
    private JScrollPane scrollPane;
    private JPanel emojiPanel;

    // 메시지 전송 콜백
    private Consumer<String> messageSendCallback;
    private Consumer<String> emojiSendCallback;

    // 플레이어 색상 (채팅 전용)
    private static final Color[] PLAYER_COLORS = {
        new Color(231, 76, 60),   // Red
        new Color(52, 152, 219),  // Blue
        new Color(46, 204, 113),  // Green
        new Color(230, 126, 34)   // Orange
    };

    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    public ChatPanel() {
        setLayout(new BorderLayout(0, 8));
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(30, 39, 46), 2),
            new EmptyBorder(10, 10, 10, 10)
        ));

        initComponents();
    }

    private void initComponents() {
        // 헤더
        JLabel headerLabel = new JLabel("> 채팅", SwingConstants.CENTER);
        headerLabel.setFont(new Font(UIConstants.FONT_NAME, Font.BOLD, 14));
        headerLabel.setForeground(TEXT_PRIMARY);
        headerLabel.setBorder(new EmptyBorder(0, 0, 5, 0));
        add(headerLabel, BorderLayout.NORTH);

        // 채팅 영역
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setBackground(MESSAGE_BG);
        chatArea.setForeground(TEXT_PRIMARY);
        chatArea.setFont(new Font(UIConstants.FONT_NAME, Font.PLAIN, 11));
        chatArea.setContentType("text/html");
        chatArea.setText(getInitialHTML());

        scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_DARK));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // 스크롤바 스타일링
        customizeScrollBar(scrollPane.getVerticalScrollBar());

        add(scrollPane, BorderLayout.CENTER);

        // 하단 패널 (이모지 + 채팅 버튼)
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 5));
        bottomPanel.setOpaque(false);

        // 이모지 버튼 패널
        emojiPanel = createEmojiPanel();
        bottomPanel.add(emojiPanel, BorderLayout.NORTH);

        // 채팅 버튼
        chatButton = createChatButton();
        bottomPanel.add(chatButton, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);

        // 시스템 메시지 추가
        addSystemMessage("채팅이 시작되었습니다.");
    }

    private JPanel createEmojiPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 5, 3, 0));
        panel.setOpaque(false);

        String[] emojis = {"[OK]", ":)", ":D", "[!]", "[*]"};

        for (String emoji : emojis) {
            JButton emojiButton = createEmojiButton(emoji);
            panel.add(emojiButton);
        }

        return panel;
    }

    private JButton createEmojiButton(String emoji) {
        JButton button = new JButton(emoji);
        button.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        button.setBackground(new Color(60, 84, 110));
        button.setForeground(TEXT_PRIMARY);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(30, 25));

        // 호버 효과
        Color hoverColor = new Color(75, 100, 130);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(60, 84, 110));
            }
        });

        // 클릭 시 이모지 전송
        button.addActionListener(e -> {
            if (emojiSendCallback != null) {
                emojiSendCallback.accept(emoji);
            }
        });

        return button;
    }

    private String getInitialHTML() {
        return "<html><head><style>" +
               "body { font-family: 'Malgun Gothic'; font-size: 11px; color: #ecf0f1; " +
               "margin: 5px; background-color: #2c3e50; }" +
               ".msg { margin: 3px 0; padding: 4px 6px; border-radius: 4px; }" +
               ".system { color: #95a5a6; font-style: italic; text-align: center; }" +
               ".player { background-color: #34495e; }" +
               ".name { font-weight: bold; }" +
               ".time { color: #7f8c8d; font-size: 9px; }" +
               "</style></head><body></body></html>";
    }

    private JButton createChatButton() {
        JButton button = new JButton("> 채팅하기");
        button.setFont(new Font(UIConstants.FONT_NAME, Font.BOLD, 12));
        button.setBackground(BUTTON_COLOR);
        button.setForeground(TEXT_PRIMARY);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(100, 30));

        // 호버 효과
        Color hoverColor = BUTTON_COLOR.brighter();
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(BUTTON_COLOR);
            }
        });

        // 클릭 이벤트
        button.addActionListener(e -> showChatInputDialog());

        return button;
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

    /**
     * 채팅 입력 다이얼로그 표시
     */
    private void showChatInputDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "메시지 입력", true);
        dialog.setSize(350, 180);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(UIConstants.BACKGROUND_DARK);
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // 제목
        JLabel titleLabel = new JLabel("> 메시지 입력");
        titleLabel.setFont(new Font(UIConstants.FONT_NAME, Font.BOLD, 16));
        titleLabel.setForeground(TEXT_PRIMARY);
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // 입력 필드
        JTextField inputField = new JTextField();
        inputField.setFont(new Font(UIConstants.FONT_NAME, Font.PLAIN, 14));
        inputField.setBackground(INPUT_BG);
        inputField.setForeground(TEXT_PRIMARY);
        inputField.setCaretColor(TEXT_PRIMARY);
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT),
            new EmptyBorder(8, 10, 8, 10)
        ));
        mainPanel.add(inputField, BorderLayout.CENTER);

        // 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        JButton sendButton = new JButton("전송");
        sendButton.setFont(new Font(UIConstants.FONT_NAME, Font.BOLD, 12));
        sendButton.setBackground(BUTTON_COLOR);
        sendButton.setForeground(TEXT_PRIMARY);
        sendButton.setFocusPainted(false);
        sendButton.setBorderPainted(false);
        sendButton.setPreferredSize(new Dimension(80, 32));

        JButton cancelButton = new JButton("취소");
        cancelButton.setFont(new Font(UIConstants.FONT_NAME, Font.BOLD, 12));
        cancelButton.setBackground(UIConstants.BUTTON_CANCEL);
        cancelButton.setForeground(TEXT_PRIMARY);
        cancelButton.setFocusPainted(false);
        cancelButton.setBorderPainted(false);
        cancelButton.setPreferredSize(new Dimension(80, 32));

        // 버튼 액션
        ActionListener sendAction = e -> {
            String message = inputField.getText().trim();
            if (!message.isEmpty()) {
                if (messageSendCallback != null) {
                    messageSendCallback.accept(message);
                }
                dialog.dispose();
            }
        };

        sendButton.addActionListener(sendAction);
        inputField.addActionListener(sendAction); // Enter 키로 전송
        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(cancelButton);
        buttonPanel.add(sendButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);

        // ESC 키로 닫기
        dialog.getRootPane().registerKeyboardAction(
            e -> dialog.dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        dialog.setVisible(true);
    }

    /**
     * 플레이어 메시지 추가
     */
    public void addPlayerMessage(int playerIndex, String playerName, String message) {
        String time = timeFormat.format(new Date());
        Color playerColor = PLAYER_COLORS[playerIndex % PLAYER_COLORS.length];
        String colorHex = String.format("#%02x%02x%02x",
            playerColor.getRed(), playerColor.getGreen(), playerColor.getBlue());

        String htmlMessage = String.format(
            "<div class='msg player'>" +
            "<span class='name' style='color:%s'>%s</span> " +
            "<span class='time'>[%s]</span><br>" +
            "%s</div>",
            colorHex, escapeHTML(playerName), time, escapeHTML(message)
        );

        appendToChat(htmlMessage);
    }

    /**
     * 시스템 메시지 추가
     */
    public void addSystemMessage(String message) {
        String htmlMessage = String.format(
            "<div class='msg system'>--- %s ---</div>",
            escapeHTML(message)
        );
        appendToChat(htmlMessage);
    }

    /**
     * 이모지 메시지 추가 (빠른 반응용)
     */
    public void addEmojiMessage(int playerIndex, String playerName, String emoji) {
        Color playerColor = PLAYER_COLORS[playerIndex % PLAYER_COLORS.length];
        String colorHex = String.format("#%02x%02x%02x",
            playerColor.getRed(), playerColor.getGreen(), playerColor.getBlue());

        String htmlMessage = String.format(
            "<div class='msg' style='text-align:center;font-size:20px'>" +
            "<span style='color:%s'>%s</span>: %s</div>",
            colorHex, escapeHTML(playerName), emoji
        );

        appendToChat(htmlMessage);
    }

    private void appendToChat(String htmlContent) {
        SwingUtilities.invokeLater(() -> {
            try {
                javax.swing.text.Document doc = chatArea.getDocument();
                javax.swing.text.html.HTMLDocument htmlDoc = (javax.swing.text.html.HTMLDocument) doc;
                javax.swing.text.html.HTMLEditorKit kit = (javax.swing.text.html.HTMLEditorKit) chatArea.getEditorKit();

                // body 태그 끝 이전에 삽입
                kit.insertHTML(htmlDoc, htmlDoc.getLength(), htmlContent, 0, 0, null);

                // 스크롤을 맨 아래로
                chatArea.setCaretPosition(htmlDoc.getLength());
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
     * 이모지 전송 콜백 설정
     */
    public void setEmojiSendCallback(Consumer<String> callback) {
        this.emojiSendCallback = callback;
    }

    /**
     * 채팅 초기화
     */
    public void clearChat() {
        chatArea.setText(getInitialHTML());
        addSystemMessage("채팅이 초기화되었습니다.");
    }

    /**
     * 폰트 크기 업데이트 (스케일링용)
     */
    public void updateFontSize(double scaleFactor) {
        int baseFontSize = (int)(11 * scaleFactor);
        int headerFontSize = (int)(14 * scaleFactor);
        int buttonFontSize = (int)(12 * scaleFactor);

        chatArea.setFont(new Font(UIConstants.FONT_NAME, Font.PLAIN, baseFontSize));
        chatButton.setFont(new Font(UIConstants.FONT_NAME, Font.BOLD, buttonFontSize));

        // HTML 스타일 업데이트는 복잡하므로 기본 폰트만 변경
    }
}
