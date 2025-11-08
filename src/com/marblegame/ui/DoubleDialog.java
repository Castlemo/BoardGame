package com.marblegame.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 더블 알림 다이얼로그
 */
public class DoubleDialog extends JDialog {
    // 다크 테마 색상
    private static final Color BACKGROUND_DARK = new Color(32, 33, 36);
    private static final Color PANEL_DARK = new Color(44, 47, 51);
    private static final Color TEXT_PRIMARY = new Color(232, 234, 237);
    private static final Color TEXT_SECONDARY = new Color(189, 195, 199);
    private static final Color BUTTON_CONFIRM = new Color(241, 196, 15);  // 금색
    private static final Color HIGHLIGHT_COLOR = new Color(255, 215, 0); // 밝은 금색

    public DoubleDialog(JFrame parent, int diceValue, int consecutiveCount) {
        super(parent, "더블!", true);

        initComponents(diceValue, consecutiveCount);
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initComponents(int diceValue, int consecutiveCount) {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(BACKGROUND_DARK);

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
        panel.setBackground(PANEL_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));

        JLabel titleLabel = new JLabel("🎲 더블! (" + diceValue + ", " + diceValue + ")");
        titleLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 26));
        titleLabel.setForeground(HIGHLIGHT_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);

        return panel;
    }

    private JPanel createMessagePanel(int consecutiveCount) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel messageLabel = new JLabel("한 번 더 굴릴 수 있습니다!");
        messageLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 18));
        messageLabel.setForeground(TEXT_PRIMARY);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 연속 더블 횟수 표시
        if (consecutiveCount > 0) {
            JLabel countLabel = new JLabel("연속 더블: " + consecutiveCount + "회");
            countLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 16));
            countLabel.setForeground(HIGHLIGHT_COLOR);
            countLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(countLabel);
            panel.add(Box.createVerticalStrut(10));
        }

        panel.add(messageLabel);
        panel.add(Box.createVerticalStrut(10));

        // 다음 더블 확률 표시
        if (consecutiveCount < 3) {
            String probabilityText = getNextDoubleProbabilityText(consecutiveCount);
            JLabel probLabel = new JLabel(probabilityText);
            probLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
            probLabel.setForeground(TEXT_SECONDARY);
            probLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(probLabel);
        } else {
            JLabel limitLabel = new JLabel("다음 주사위는 더블이 나오지 않습니다");
            limitLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
            limitLabel.setForeground(new Color(231, 76, 60)); // 빨강
            limitLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(limitLabel);
        }

        return panel;
    }

    private String getNextDoubleProbabilityText(int consecutiveCount) {
        switch (consecutiveCount) {
            case 0: return "다음 더블 확률: 100%";
            case 1: return "다음 더블 확률: 70%";
            case 2: return "다음 더블 확률: 20%";
            default: return "다음 더블 확률: 0%";
        }
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
        button.setForeground(Color.BLACK); // 금색 배경에는 검은 글씨
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
