package com.marblegame.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 찬스 칸 도착 다이얼로그
 */
public class ChanceDialog extends JDialog {
    // 특수 색상 (UIConstants에 없는 고유 색상)
    private static final Color HIGHLIGHT_COLOR = new Color(241, 196, 15); // 금색

    public ChanceDialog(JFrame parent, int amount) {
        super(parent, "찬스!", true);

        initComponents(amount);
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initComponents(int amount) {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(UIConstants.BACKGROUND_DARK);

        // 헤더 패널
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // 메시지 패널
        JPanel messagePanel = createMessagePanel(amount);
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

        JLabel titleLabel = new JLabel("찬스!");
        titleLabel.setFont(UIConstants.FONT_TITLE);
        titleLabel.setForeground(HIGHLIGHT_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);

        return panel;
    }

    private JPanel createMessagePanel(int amount) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIConstants.BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel messageLabel = new JLabel(String.format("%,d원을 획득하셨습니다!", amount));
        messageLabel.setFont(UIConstants.FONT_HEADER);
        messageLabel.setForeground(UIConstants.TEXT_PRIMARY);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(messageLabel);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(UIConstants.BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));

        JButton confirmButton = UIConstants.createStyledButton("확인", UIConstants.BUTTON_CONFIRM);
        confirmButton.addActionListener(e -> dispose());

        panel.add(confirmButton);

        return panel;
    }
}
