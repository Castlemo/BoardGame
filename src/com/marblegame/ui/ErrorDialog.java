package com.marblegame.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 오류 다이얼로그
 */
public class ErrorDialog extends JDialog {

    public ErrorDialog(JFrame parent, String title, String message) {
        super(parent, title, true);

        initComponents(message);
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initComponents(String message) {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(UIConstants.BACKGROUND_DARK);

        // 헤더 패널
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // 메시지 패널
        JPanel messagePanel = createMessagePanel(message);
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

        JLabel titleLabel = new JLabel("⚠ 오류");
        titleLabel.setFont(UIConstants.FONT_TITLE);
        titleLabel.setForeground(UIConstants.STATUS_ERROR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);

        return panel;
    }

    private JPanel createMessagePanel(String message) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIConstants.BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(new Font(UIConstants.FONT_NAME, Font.BOLD, 16));
        messageLabel.setForeground(UIConstants.TEXT_PRIMARY);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hintLabel = new JLabel("다시 선택해주세요.");
        hintLabel.setFont(new Font(UIConstants.FONT_NAME, Font.PLAIN, 13));
        hintLabel.setForeground(UIConstants.TEXT_SECONDARY);
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(messageLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(hintLabel);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(UIConstants.BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));

        JButton confirmButton = UIConstants.createStyledButton("확인", UIConstants.BUTTON_WARNING);
        confirmButton.addActionListener(e -> dispose());

        panel.add(confirmButton);

        return panel;
    }
}
