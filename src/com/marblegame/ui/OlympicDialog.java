package com.marblegame.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 올림픽 칸 도착 다이얼로그
 */
public class OlympicDialog extends JDialog {
    // 올림픽 특별 색상 (빨간색)
    private static final Color HIGHLIGHT_COLOR = new Color(231, 76, 60);

    public OlympicDialog(JFrame parent) {
        super(parent, "올림픽", true);

        initComponents();
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(UIConstants.BACKGROUND_DARK);

        // 헤더 패널
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // 메시지 패널
        JPanel messagePanel = createMessagePanel();
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

        JLabel titleLabel = new JLabel("올림픽");
        titleLabel.setFont(UIConstants.FONT_TITLE);
        titleLabel.setForeground(HIGHLIGHT_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);

        return panel;
    }

    private JPanel createMessagePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIConstants.BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel messageLabel1 = new JLabel("올림픽 칸에 도착했습니다!");
        messageLabel1.setFont(new Font(UIConstants.FONT_NAME, Font.BOLD, 16));
        messageLabel1.setForeground(UIConstants.TEXT_PRIMARY);
        messageLabel1.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel messageLabel2 = new JLabel("소유한 곳 중 한 곳의 통행료가 2배가 됩니다!");
        messageLabel2.setFont(UIConstants.FONT_BODY);
        messageLabel2.setForeground(UIConstants.TEXT_SECONDARY);
        messageLabel2.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(messageLabel1);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(messageLabel2);

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
