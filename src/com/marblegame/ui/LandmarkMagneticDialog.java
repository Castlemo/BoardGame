package com.marblegame.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 랜드마크 마그네틱 효과 다이얼로그
 */
public class LandmarkMagneticDialog extends JDialog {
    // 다크 테마 색상
    private static final Color BACKGROUND_DARK = new Color(32, 33, 36);
    private static final Color PANEL_DARK = new Color(44, 47, 51);
    private static final Color TEXT_PRIMARY = new Color(232, 234, 237);
    private static final Color TEXT_SECONDARY = new Color(189, 195, 199);
    private static final Color BUTTON_CONFIRM = new Color(142, 68, 173);  // 보라색 (자기장)
    private static final Color HIGHLIGHT_COLOR = new Color(155, 89, 182); // 밝은 보라색

    public LandmarkMagneticDialog(JFrame parent, String landmarkName, int pulledCount) {
        super(parent, "랜드마크 마그네틱", true);

        initComponents(landmarkName, pulledCount);
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initComponents(String landmarkName, int pulledCount) {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(BACKGROUND_DARK);

        // 헤더 패널
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // 메시지 패널
        JPanel messagePanel = createMessagePanel(landmarkName, pulledCount);
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

        JLabel titleLabel = new JLabel("🧲 랜드마크 마그네틱");
        titleLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 24));
        titleLabel.setForeground(HIGHLIGHT_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);

        return panel;
    }

    private JPanel createMessagePanel(String landmarkName, int pulledCount) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel messageLabel = new JLabel(landmarkName + "의 자기장이 발동!");
        messageLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 18));
        messageLabel.setForeground(TEXT_PRIMARY);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String countText = pulledCount > 0
            ? pulledCount + "명의 플레이어를 끌어당깁니다!"
            : "범위 내 플레이어가 없습니다.";
        JLabel countLabel = new JLabel(countText);
        countLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 16));
        countLabel.setForeground(pulledCount > 0 ? HIGHLIGHT_COLOR : TEXT_SECONDARY);
        countLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hintLabel = new JLabel("양옆 4칸 이내의 플레이어에게 영향");
        hintLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        hintLabel.setForeground(TEXT_SECONDARY);
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(messageLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(countLabel);
        panel.add(Box.createVerticalStrut(10));
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
