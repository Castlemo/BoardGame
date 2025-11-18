package com.marblegame.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 더블 알림 다이얼로그
 */
public class DoubleDialog extends JDialog {
    // 더블 전용 색상 (금색 테마)
    private static final Color GOLD_COLOR = new Color(241, 196, 15);
    private static final Color HIGHLIGHT_GOLD = new Color(255, 215, 0);

    public DoubleDialog(JFrame parent, int diceValue, int consecutiveCount) {
        super(parent, "더블!", true);

        initComponents(diceValue, consecutiveCount);
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initComponents(int diceValue, int consecutiveCount) {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(UIConstants.BACKGROUND_DARK);

        // 헤더 패널
        JPanel headerPanel = createHeaderPanel(diceValue);
        add(headerPanel, BorderLayout.NORTH);

        // 메시지 패널
        JPanel messagePanel = createMessagePanel(consecutiveCount);
        add(messagePanel, BorderLayout.CENTER);

        // 버튼 패널
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel(int diceValue) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIConstants.PANEL_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));

        JLabel titleLabel = new JLabel("* 더블! (" + diceValue + ", " + diceValue + ")");
        titleLabel.setFont(new Font(UIConstants.FONT_NAME, Font.BOLD, 26));
        titleLabel.setForeground(HIGHLIGHT_GOLD);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);

        return panel;
    }

    private JPanel createMessagePanel(int consecutiveCount) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIConstants.BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel messageLabel = new JLabel("한 번 더 굴릴 수 있습니다!");
        messageLabel.setFont(UIConstants.FONT_HEADER);
        messageLabel.setForeground(UIConstants.TEXT_PRIMARY);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 연속 더블 횟수 표시
        if (consecutiveCount > 0) {
            JLabel countLabel = new JLabel("연속 더블: " + consecutiveCount + "회");
            countLabel.setFont(new Font(UIConstants.FONT_NAME, Font.BOLD, 16));
            countLabel.setForeground(HIGHLIGHT_GOLD);
            countLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(countLabel);
            panel.add(Box.createVerticalStrut(10));
        }

        panel.add(messageLabel);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(UIConstants.BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));

        JButton confirmButton = createGoldButton("확인");
        confirmButton.addActionListener(e -> dispose());

        panel.add(confirmButton);

        return panel;
    }

    private JButton createGoldButton(String text) {
        JButton button = new JButton(text);
        button.setFont(UIConstants.FONT_BODY_BOLD);
        button.setPreferredSize(UIConstants.BUTTON_SIZE_DEFAULT);
        button.setBackground(GOLD_COLOR);
        button.setForeground(Color.BLACK); // 금색 배경에는 검은 글씨
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 호버 효과
        Color hoverColor = GOLD_COLOR.brighter();
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(hoverColor);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(GOLD_COLOR);
            }
        });

        return button;
    }
}
