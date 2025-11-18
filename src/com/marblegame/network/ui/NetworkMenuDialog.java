package com.marblegame.network.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 네트워크 메뉴 다이얼로그
 * 방 만들기 또는 방 참가 선택
 */
public class NetworkMenuDialog extends JDialog {
    private int choice = 0; // 0 = 취소, 1 = 방 만들기, 2 = 방 참가

    // 다크 테마 색상
    private static final Color BACKGROUND_DARK = new Color(32, 33, 36);
    private static final Color PANEL_DARK = new Color(44, 47, 51);
    private static final Color TEXT_PRIMARY = new Color(232, 234, 237);
    private static final Color TEXT_SECONDARY = new Color(189, 195, 199);
    private static final Color BUTTON_CREATE = new Color(52, 152, 219);  // 파란색
    private static final Color BUTTON_JOIN = new Color(46, 204, 113);    // 녹색
    private static final Color BUTTON_CANCEL = new Color(127, 140, 141); // 회색
    private static final Color HIGHLIGHT_COLOR = new Color(155, 89, 182); // 보라색

    public NetworkMenuDialog(JFrame parent) {
        super(parent, "네트워크 멀티플레이", true);

        initComponents();
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(BACKGROUND_DARK);

        // 헤더 패널
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // 버튼 패널
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.CENTER);

        // 취소 버튼 패널
        JPanel cancelPanel = createCancelPanel();
        add(cancelPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(PANEL_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(25, 20, 20, 20));

        JLabel titleLabel = new JLabel("네트워크 멀티플레이");
        titleLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 24));
        titleLabel.setForeground(HIGHLIGHT_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("LAN 환경에서 친구들과 함께 플레이하세요");
        subtitleLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        subtitleLabel.setForeground(TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(subtitleLabel);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // 방 만들기 버튼
        JButton createButton = createMenuButton(
            "> 방 만들기",
            "새로운 게임 방을 만들고 호스트가 됩니다",
            BUTTON_CREATE,
            1
        );

        // 방 참가하기 버튼
        JButton joinButton = createMenuButton(
            "> 방 참가하기",
            "호스트의 IP 주소로 게임 방에 참가합니다",
            BUTTON_JOIN,
            2
        );

        panel.add(createButton);
        panel.add(Box.createVerticalStrut(15));
        panel.add(joinButton);

        return panel;
    }

    private JButton createMenuButton(String title, String description, Color bgColor, int choiceValue) {
        JButton button = new JButton();
        button.setLayout(new BorderLayout(10, 5));
        button.setPreferredSize(new Dimension(400, 70));
        button.setMaximumSize(new Dimension(400, 70));
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 버튼 내용 패널
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // 타이틀
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 18));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 설명
        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        descLabel.setForeground(new Color(240, 240, 240));
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(3));
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

    private JPanel createCancelPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        JButton cancelButton = new JButton("취소");
        cancelButton.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        cancelButton.setPreferredSize(new Dimension(120, 40));
        cancelButton.setBackground(BUTTON_CANCEL);
        cancelButton.setForeground(TEXT_PRIMARY);
        cancelButton.setFocusPainted(false);
        cancelButton.setBorderPainted(false);
        cancelButton.setOpaque(true);
        cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 호버 효과
        Color hoverColor = BUTTON_CANCEL.brighter();
        cancelButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                cancelButton.setBackground(hoverColor);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                cancelButton.setBackground(BUTTON_CANCEL);
            }
        });

        cancelButton.addActionListener(e -> {
            choice = 0; // 취소
            dispose();
        });

        panel.add(cancelButton);

        return panel;
    }

    /**
     * 사용자 선택 반환
     * @return 0 (취소), 1 (방 만들기), 2 (방 참가)
     */
    public int getChoice() {
        return choice;
    }
}
