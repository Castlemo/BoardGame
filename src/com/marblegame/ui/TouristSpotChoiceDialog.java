package com.marblegame.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 관광지 선택지 다이얼로그
 * 관광지 소유 시 2가지 선택지 제공 (잠금 / 주사위 한 번 더)
 */
public class TouristSpotChoiceDialog extends JDialog {
    public enum Choice {
        LOCK,        // 잠금
        EXTRA_ROLL   // 주사위 한 번 더
    }

    private Choice selectedChoice = null;
    private final String spotName;

    // 관광지 선택 버튼 색상
    private static final Color BUTTON_LOCK = new Color(231, 76, 60);      // 빨간색
    private static final Color BUTTON_EXTRA = new Color(52, 152, 219);    // 파란색

    public TouristSpotChoiceDialog(JFrame parent, String spotName) {
        super(parent, "관광지 선택", true); // modal dialog
        this.spotName = spotName;

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

        // 정보 패널
        JPanel infoPanel = createInfoPanel();
        add(infoPanel, BorderLayout.CENTER);

        // 버튼 패널
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * 헤더 패널 생성
     */
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIConstants.PANEL_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));

        // 관광지 이름
        JLabel titleLabel = new JLabel(spotName);
        titleLabel.setFont(UIConstants.FONT_SUBTITLE);
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 안내 메시지
        JLabel messageLabel = new JLabel("행동을 선택하세요");
        messageLabel.setFont(UIConstants.FONT_BODY);
        messageLabel.setForeground(UIConstants.TEXT_SECONDARY);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(messageLabel);

        return panel;
    }

    /**
     * 정보 패널 생성
     */
    private JPanel createInfoPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIConstants.BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        // 선택지 설명
        JPanel descPanel = new JPanel();
        descPanel.setLayout(new BoxLayout(descPanel, BoxLayout.Y_AXIS));
        descPanel.setOpaque(false);

        addDescriptionLine(descPanel, "🔒 잠금", "다음 내 턴까지 인수 불가");
        addDescriptionLine(descPanel, "🎲 주사위 한 번 더", "추가 주사위 기회 획득");

        panel.add(descPanel);

        return panel;
    }

    /**
     * 설명 라인 추가
     */
    private void addDescriptionLine(JPanel parent, String title, String desc) {
        JPanel linePanel = new JPanel();
        linePanel.setLayout(new BoxLayout(linePanel, BoxLayout.Y_AXIS));
        linePanel.setOpaque(false);
        linePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UIConstants.FONT_SMALL_BOLD);
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel descLabel = new JLabel("  → " + desc);
        descLabel.setFont(UIConstants.FONT_CAPTION);
        descLabel.setForeground(UIConstants.TEXT_SECONDARY);

        linePanel.add(titleLabel);
        linePanel.add(descLabel);
        linePanel.add(Box.createRigidArea(new Dimension(0, 5)));

        parent.add(linePanel);
    }

    /**
     * 버튼 패널 생성
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIConstants.BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));

        // 잠금, 주사위 한 번 더
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        row.setOpaque(false);

        JButton lockButton = createButton("🔒 잠금", BUTTON_LOCK, 120);
        lockButton.addActionListener(e -> {
            selectedChoice = Choice.LOCK;
            dispose();
        });

        JButton extraButton = createButton("🎲 주사위 한 번 더", BUTTON_EXTRA, 120);
        extraButton.addActionListener(e -> {
            selectedChoice = Choice.EXTRA_ROLL;
            dispose();
        });

        row.add(lockButton);
        row.add(extraButton);
        panel.add(row);

        return panel;
    }

    /**
     * 버튼 생성
     */
    private JButton createButton(String text, Color bgColor, int width) {
        JButton button = new JButton(text);
        button.setFont(new Font(UIConstants.FONT_NAME, Font.BOLD, 13));
        button.setPreferredSize(new Dimension(width, 40));
        button.setBackground(bgColor);
        button.setForeground(UIConstants.TEXT_PRIMARY);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 호버 효과
        Color hoverColor = bgColor.brighter();
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(hoverColor);
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(bgColor);
                }
            }
        });

        return button;
    }

    /**
     * 선택된 옵션 반환
     */
    public Choice getSelectedChoice() {
        return selectedChoice;
    }
}
