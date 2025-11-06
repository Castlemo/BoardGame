package com.marblegame.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 무인도 칸 도착 다이얼로그
 */
public class IslandDialog extends JDialog {
    // 다크 테마 색상
    private static final Color BACKGROUND_DARK = new Color(32, 33, 36);
    private static final Color PANEL_DARK = new Color(44, 47, 51);
    private static final Color TEXT_PRIMARY = new Color(232, 234, 237);
    private static final Color TEXT_SECONDARY = new Color(189, 195, 199);
    private static final Color BUTTON_CONFIRM = new Color(127, 140, 141);  // 회색
    private static final Color HIGHLIGHT_COLOR = new Color(149, 165, 166); // 회색

    public IslandDialog(JFrame parent, int jailTurns) {
        super(parent, "무인도", true);

        initComponents(jailTurns);
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initComponents(int jailTurns) {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(BACKGROUND_DARK);

        // 헤더 패널
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // 메시지 패널
        JPanel messagePanel = createMessagePanel(jailTurns);
        add(messagePanel, BorderLayout.CENTER);

        // 버튼 패널
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(PANEL_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));

        JLabel titleLabel = new JLabel("🏝️ 무인도");
        titleLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 24));
        titleLabel.setForeground(HIGHLIGHT_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);

        return panel;
    }

    private JPanel createMessagePanel(int jailTurns) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel messageLabel1 = new JLabel("무인도에 갇혔습니다!");
        messageLabel1.setFont(new Font("Malgun Gothic", Font.BOLD, 18));
        messageLabel1.setForeground(TEXT_PRIMARY);
        messageLabel1.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel messageLabel2 = new JLabel(String.format("%d턴 동안 갇혀있습니다.", jailTurns));
        messageLabel2.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        messageLabel2.setForeground(TEXT_SECONDARY);
        messageLabel2.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hintLabel = new JLabel("보석금 200,000원으로 즉시 탈출 가능");
        hintLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        hintLabel.setForeground(new Color(241, 196, 15)); // 금색
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(messageLabel1);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(messageLabel2);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(hintLabel);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));

        JButton confirmButton = createButton("확인", BUTTON_CONFIRM);
        confirmButton.addActionListener(e -> dispose());

        panel.add(confirmButton);

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
}
