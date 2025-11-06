package com.marblegame.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 도시 인수 확인 다이얼로그
 */
public class TakeoverConfirmDialog extends JDialog {
    private boolean confirmed = false;

    // 다크 테마 색상
    private static final Color BACKGROUND_DARK = new Color(32, 33, 36);
    private static final Color PANEL_DARK = new Color(44, 47, 51);
    private static final Color TEXT_PRIMARY = new Color(232, 234, 237);
    private static final Color TEXT_SECONDARY = new Color(189, 195, 199);
    private static final Color BUTTON_CONFIRM = new Color(39, 174, 96);  // 녹색
    private static final Color BUTTON_CANCEL = new Color(231, 76, 60);   // 빨간색
    private static final Color INFO_ROW_BG = new Color(44, 47, 51);

    public TakeoverConfirmDialog(JFrame parent, String cityName, String currentOwner,
                                 int level, int takeoverCost, int playerCash) {
        super(parent, "도시 인수 확인", true);

        initComponents(cityName, currentOwner, level, takeoverCost, playerCash);
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initComponents(String cityName, String currentOwner, int level,
                               int takeoverCost, int playerCash) {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(BACKGROUND_DARK);

        // 헤더 패널
        JPanel headerPanel = createHeaderPanel(cityName);
        add(headerPanel, BorderLayout.NORTH);

        // 정보 패널
        JPanel infoPanel = createInfoPanel(currentOwner, level, takeoverCost, playerCash);
        add(infoPanel, BorderLayout.CENTER);

        // 버튼 패널
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel(String cityName) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(PANEL_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));

        JLabel titleLabel = new JLabel("🏢 도시 인수");
        titleLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 20));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel cityLabel = new JLabel(cityName);
        cityLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 16));
        cityLabel.setForeground(new Color(52, 152, 219)); // 파란색
        cityLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(cityLabel);

        return panel;
    }

    private JPanel createInfoPanel(String currentOwner, int level, int takeoverCost, int playerCash) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // 현재 소유자
        panel.add(createInfoRow("현재 소유자:", currentOwner));
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        // 건물 레벨
        String levelEmoji = getLevelEmoji(level);
        panel.add(createInfoRow("건물 레벨:", levelEmoji + " 레벨 " + level));
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        // 인수 비용
        panel.add(createInfoRow("인수 비용:", String.format("%,d원", takeoverCost)));
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        // 구분선
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(70, 73, 77));
        separator.setMaximumSize(new Dimension(400, 1));
        panel.add(separator);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        // 현재 보유 자금
        panel.add(createInfoRow("보유 자금:", String.format("%,d원", playerCash)));
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        // 인수 후 잔액
        int remainingBalance = playerCash - takeoverCost;
        JPanel balanceRow = createInfoRow("인수 후 잔액:", String.format("%,d원", remainingBalance));

        // 잔액이 음수면 경고 표시
        if (remainingBalance < 0) {
            JLabel warningLabel = new JLabel("⚠ 잔액 부족!");
            warningLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 12));
            warningLabel.setForeground(new Color(231, 76, 60)); // 빨간색
            warningLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(Box.createRigidArea(new Dimension(0, 10)));
            panel.add(warningLabel);
        }

        panel.add(balanceRow);

        return panel;
    }

    private JPanel createInfoRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(INFO_ROW_BG);
        row.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        row.setMaximumSize(new Dimension(400, 35));

        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        labelComponent.setForeground(TEXT_SECONDARY);

        JLabel valueComponent = new JLabel(value);
        valueComponent.setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        valueComponent.setForeground(TEXT_PRIMARY);

        row.add(labelComponent, BorderLayout.WEST);
        row.add(valueComponent, BorderLayout.EAST);

        return row;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 0));
        panel.setBackground(BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));

        // 예 버튼
        JButton confirmButton = createButton("예", BUTTON_CONFIRM);
        confirmButton.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        // 아니오 버튼
        JButton cancelButton = createButton("아니오", BUTTON_CANCEL);
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        panel.add(confirmButton);
        panel.add(cancelButton);

        return panel;
    }

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

    private String getLevelEmoji(int level) {
        switch (level) {
            case 1: return "🏠";
            case 2: return "🏢";
            case 3: return "🏬";
            case 4: return "🏛️";
            default: return "";
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
