package com.marblegame.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 세계여행 칸 도착 다이얼로그
 */
public class WorldTourDialog extends JDialog {

    public WorldTourDialog(JFrame parent) {
        super(parent, "세계여행", true);

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

        JLabel titleLabel = new JLabel("세계여행");
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

        JLabel messageLabel1 = new JLabel("세계여행 칸에 도착했습니다!");
        messageLabel1.setFont(new Font(UIConstants.FONT_NAME, Font.BOLD, 16));
        messageLabel1.setForeground(UIConstants.TEXT_PRIMARY);
        messageLabel1.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel messageLabel2 = new JLabel("다음 턴에 원하는 칸을 선택하세요.");
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

        JButton confirmButton = UIConstants.createStyledButton("확인", UIConstants.STATUS_INFO);
        confirmButton.addActionListener(e -> dispose());

        panel.add(confirmButton);

        return panel;
    }
}
