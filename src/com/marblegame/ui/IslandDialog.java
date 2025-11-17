package com.marblegame.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 무인도 칸 도착 다이얼로그
 */
public class IslandDialog extends JDialog {
    // 특수 색상 (UIConstants에 없는 고유 색상)
    private static final Color HIGHLIGHT_COLOR = new Color(149, 165, 166); // 회색
    private static final Color GOLD_COLOR = new Color(241, 196, 15); // 금색

    public IslandDialog(JFrame parent, int jailTurns) {
        super(parent, "무인도", true);

        initComponents(jailTurns);
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initComponents(int jailTurns) {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(UIConstants.BACKGROUND_DARK);

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
        panel.setBackground(UIConstants.PANEL_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));

        JLabel titleLabel = new JLabel("🏝️ 무인도");
        titleLabel.setFont(UIConstants.FONT_TITLE);
        titleLabel.setForeground(HIGHLIGHT_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);

        return panel;
    }

    private JPanel createMessagePanel(int jailTurns) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIConstants.BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel messageLabel1 = new JLabel("무인도에 갇혔습니다!");
        messageLabel1.setFont(UIConstants.FONT_HEADER);
        messageLabel1.setForeground(UIConstants.TEXT_PRIMARY);
        messageLabel1.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel messageLabel2 = new JLabel(String.format("%d턴 동안 갇혀있습니다.", jailTurns));
        messageLabel2.setFont(UIConstants.FONT_BODY);
        messageLabel2.setForeground(UIConstants.TEXT_SECONDARY);
        messageLabel2.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hintLabel = new JLabel("보석금 200,000원으로 즉시 탈출 가능");
        hintLabel.setFont(UIConstants.FONT_SMALL);
        hintLabel.setForeground(GOLD_COLOR);
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
        panel.setBackground(UIConstants.BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));

        JButton confirmButton = UIConstants.createStyledButton("확인", UIConstants.BUTTON_CANCEL);
        confirmButton.addActionListener(e -> dispose());

        panel.add(confirmButton);

        return panel;
    }
}
