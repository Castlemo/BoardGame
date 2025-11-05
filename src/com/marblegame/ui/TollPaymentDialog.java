package com.marblegame.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 통행료 지불 확인 다이얼로그
 */
public class TollPaymentDialog extends JDialog {
    private final String cityName;
    private final String ownerName;
    private final int level;
    private final int toll;
    private final boolean hasOlympicBoost;
    private final int playerCash;

    // 다크 테마 색상
    private static final Color BACKGROUND_DARK = new Color(32, 33, 36);
    private static final Color PANEL_DARK = new Color(44, 47, 51);
    private static final Color TEXT_PRIMARY = new Color(232, 234, 237);
    private static final Color TEXT_SECONDARY = new Color(189, 195, 199);
    private static final Color BUTTON_CONFIRM = new Color(39, 174, 96);  // 녹색
    private static final Color TEXT_WARNING = new Color(244, 67, 54);    // 빨간색 (음수 잔액)

    public TollPaymentDialog(JFrame parent, String cityName, String ownerName, int level,
                            int toll, boolean hasOlympicBoost, int playerCash) {
        super(parent, "통행료 지불", true); // modal dialog
        this.cityName = cityName;
        this.ownerName = ownerName;
        this.level = level;
        this.toll = toll;
        this.hasOlympicBoost = hasOlympicBoost;
        this.playerCash = playerCash;

        initComponents();
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); // ESC로 닫기 불가
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(BACKGROUND_DARK);

        // 헤더 패널
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // 정보 패널
        JPanel infoPanel = createInfoPanel();
        add(infoPanel, BorderLayout.CENTER);

        // 버튼 패널
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * 헤더 패널 생성
     */
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(PANEL_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));

        // 도시 이름
        JLabel titleLabel = new JLabel(cityName);
        titleLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 20));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 안내 메시지
        JLabel messageLabel = new JLabel("통행료를 지불하시겠습니까?");
        messageLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        messageLabel.setForeground(TEXT_SECONDARY);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(messageLabel);

        return panel;
    }

    /**
     * 정보 패널 생성
     */
    private JPanel createInfoPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        // 소유자
        JPanel ownerPanel = createInfoRow("🏠 소유자", ownerName);
        panel.add(ownerPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 레벨
        String levelText = "레벨 " + level + " " + getLevelEmoji(level);
        JPanel levelPanel = createInfoRow("📊 레벨", levelText);
        panel.add(levelPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 통행료
        JPanel tollPanel = createInfoRow("💸 통행료", String.format("%,d원", toll));
        panel.add(tollPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 올림픽 효과
        if (hasOlympicBoost) {
            JPanel olympicPanel = createInfoRow("⚡ 올림픽 효과", "통행료 2배!");
            panel.add(olympicPanel);
            panel.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        // 구분선
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(149, 165, 166, 100));
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panel.add(separator);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 보유 자금
        JPanel cashPanel = createInfoRow("💵 보유 자금", String.format("%,d원", playerCash));
        panel.add(cashPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 지불 후 잔액
        int remainingCash = playerCash - toll;
        JPanel remainingPanel = createInfoRow("💳 지불 후 잔액",
            String.format("%,d원", remainingCash), remainingCash < 0);
        panel.add(remainingPanel);

        // 파산 경고
        if (remainingCash < 0) {
            panel.add(Box.createRigidArea(new Dimension(0, 15)));
            JLabel warningLabel = new JLabel("⚠️ 잔액이 부족합니다! 파산 처리됩니다.");
            warningLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 12));
            warningLabel.setForeground(TEXT_WARNING);
            warningLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(warningLabel);
        }

        return panel;
    }

    /**
     * 정보 행 생성
     */
    private JPanel createInfoRow(String label, String value) {
        return createInfoRow(label, value, false);
    }

    private JPanel createInfoRow(String label, String value, boolean isWarning) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(400, 30));

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        labelComp.setForeground(TEXT_SECONDARY);

        JLabel valueComp = new JLabel(value);
        valueComp.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        valueComp.setForeground(isWarning ? TEXT_WARNING : TEXT_PRIMARY);
        valueComp.setHorizontalAlignment(SwingConstants.RIGHT);

        panel.add(labelComp, BorderLayout.WEST);
        panel.add(valueComp, BorderLayout.EAST);

        return panel;
    }

    /**
     * 버튼 패널 생성
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        // 지불하기 버튼
        JButton confirmButton = createButton("지불하기", BUTTON_CONFIRM);
        confirmButton.addActionListener(e -> dispose());

        panel.add(confirmButton);

        return panel;
    }

    /**
     * 버튼 생성
     */
    private JButton createButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        button.setPreferredSize(new Dimension(120, 40));
        button.setBackground(bgColor);
        button.setForeground(TEXT_PRIMARY);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 호버 효과
        Color hoverColor = bgColor.brighter();
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(hoverColor);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    /**
     * 레벨 이모지 반환
     */
    private String getLevelEmoji(int level) {
        switch (level) {
            case 1: return "🏠";
            case 2: return "🏢";
            case 3: return "🏬";
            case 4: return "🏛️";
            default: return "";
        }
    }
}
