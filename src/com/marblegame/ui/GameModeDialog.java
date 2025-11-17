package com.marblegame.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 게임 모드 선택 다이얼로그
 * 로컬 게임 또는 네트워크 멀티플레이 선택
 */
public class GameModeDialog extends JDialog {
    private int choice = 0; // 0 = 취소, 1 = 로컬 게임, 2 = 네트워크 멀티플레이

    public GameModeDialog(JFrame parent) {
        super(parent, "모두의 마블 2.0", true);

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

        // 버튼 패널
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.CENTER);

        // 푸터 패널
        JPanel footerPanel = createFooterPanel();
        add(footerPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIConstants.PANEL_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 20, 25, 20));

        JLabel titleLabel = new JLabel("🎮 모두의 마블 2.0");
        titleLabel.setFont(new Font(UIConstants.FONT_NAME, Font.BOLD, 32));
        titleLabel.setForeground(UIConstants.STATUS_SUCCESS);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("게임 모드를 선택하세요");
        subtitleLabel.setFont(UIConstants.FONT_BODY);
        subtitleLabel.setForeground(UIConstants.TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(subtitleLabel);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIConstants.BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // 로컬 게임 버튼
        JButton localButton = createModeButton(
            "🏠 로컬 게임",
            "한 컴퓨터에서 2-4명이 번갈아 플레이",
            UIConstants.STATUS_INFO,
            1
        );

        // 네트워크 멀티플레이 버튼
        JButton networkButton = createModeButton(
            "🌐 네트워크 멀티플레이",
            "LAN 환경에서 친구들과 함께 플레이",
            UIConstants.HIGHLIGHT_PURPLE,
            2
        );

        panel.add(localButton);
        panel.add(Box.createVerticalStrut(15));
        panel.add(networkButton);

        return panel;
    }

    private JButton createModeButton(String title, String description, Color bgColor, int choiceValue) {
        JButton button = new JButton();
        button.setLayout(new BorderLayout(10, 5));
        button.setPreferredSize(new Dimension(450, 80));
        button.setMaximumSize(new Dimension(450, 80));
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 버튼 내용 패널
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        // 타이틀
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UIConstants.FONT_SUBTITLE);
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 설명
        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font(UIConstants.FONT_NAME, Font.PLAIN, 13));
        descLabel.setForeground(new Color(245, 245, 245));
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(5));
        contentPanel.add(descLabel);

        button.add(contentPanel, BorderLayout.CENTER);

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

        // 버튼 클릭 이벤트
        button.addActionListener(e -> {
            choice = choiceValue;
            dispose();
        });

        return button;
    }

    private JPanel createFooterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(UIConstants.BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 25, 20));

        JLabel versionLabel = new JLabel("v2.0 Network Edition");
        versionLabel.setFont(UIConstants.FONT_CAPTION);
        versionLabel.setForeground(UIConstants.TEXT_SECONDARY);

        panel.add(versionLabel);

        return panel;
    }

    /**
     * 사용자 선택 반환
     * @return 0 (취소), 1 (로컬 게임), 2 (네트워크 멀티플레이)
     */
    public int getChoice() {
        return choice;
    }
}
