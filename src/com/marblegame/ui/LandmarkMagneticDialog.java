package com.marblegame.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 랜드마크 마그네틱 효과 다이얼로그
 */
public class LandmarkMagneticDialog extends JDialog {

    public LandmarkMagneticDialog(JFrame parent, String landmarkName, int pulledCount) {
        super(parent, "랜드마크 마그네틱", true);

        initComponents(landmarkName, pulledCount);
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initComponents(String landmarkName, int pulledCount) {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(UIConstants.BACKGROUND_DARK);

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
        panel.setBackground(UIConstants.PANEL_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));

        JLabel titleLabel = new JLabel("🧲 랜드마크 마그네틱");
        titleLabel.setFont(UIConstants.FONT_TITLE);
        titleLabel.setForeground(UIConstants.HIGHLIGHT_PURPLE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);

        return panel;
    }

    private JPanel createMessagePanel(String landmarkName, int pulledCount) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIConstants.BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel messageLabel = new JLabel(landmarkName + "의 자기장이 발동!");
        messageLabel.setFont(UIConstants.FONT_HEADER);
        messageLabel.setForeground(UIConstants.TEXT_PRIMARY);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String countText = pulledCount > 0
            ? pulledCount + "명의 플레이어를 끌어당깁니다!"
            : "범위 내 플레이어가 없습니다.";
        JLabel countLabel = new JLabel(countText);
        countLabel.setFont(new Font(UIConstants.FONT_NAME, Font.BOLD, 16));
        countLabel.setForeground(pulledCount > 0 ? UIConstants.HIGHLIGHT_PURPLE : UIConstants.TEXT_SECONDARY);
        countLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hintLabel = new JLabel("양옆 4칸 이내의 플레이어에게 영향");
        hintLabel.setFont(new Font(UIConstants.FONT_NAME, Font.PLAIN, 13));
        hintLabel.setForeground(UIConstants.TEXT_SECONDARY);
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
        panel.setBackground(UIConstants.BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));

        JButton confirmButton = UIConstants.createStyledButton("확인", UIConstants.BUTTON_TAKEOVER);
        confirmButton.addActionListener(e -> dispose());

        panel.add(confirmButton);

        return panel;
    }
}
