package com.marblegame.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 도시 선택 안내 다이얼로그 (세계여행/전국철도용)
 */
public class CitySelectionDialog extends JDialog {

    public CitySelectionDialog(JFrame parent) {
        super(parent, "도시 선택", true);

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

        JLabel titleLabel = new JLabel("> 도시 선택");
        titleLabel.setFont(UIConstants.FONT_TITLE);
        titleLabel.setForeground(UIConstants.STATUS_INFO);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);

        return panel;
    }

    private JPanel createMessagePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIConstants.BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel messageLabel = new JLabel("원하는 칸을 선택하세요!");
        messageLabel.setFont(UIConstants.FONT_HEADER);
        messageLabel.setForeground(UIConstants.TEXT_PRIMARY);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hintLabel = new JLabel("보드의 원하는 칸을 클릭하세요.");
        hintLabel.setFont(UIConstants.FONT_BODY);
        hintLabel.setForeground(UIConstants.TEXT_SECONDARY);
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(messageLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(hintLabel);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(UIConstants.BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));

        JButton confirmButton = UIConstants.createStyledButton("확인", UIConstants.STATUS_INFO);
        confirmButton.addActionListener(e -> dispose());

        panel.add(confirmButton);

        return panel;
    }
}
