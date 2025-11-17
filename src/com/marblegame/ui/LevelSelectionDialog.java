package com.marblegame.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 도시 구매 시 건설 레벨(1-3)을 선택하는 다이얼로그
 */
public class LevelSelectionDialog extends JDialog {
    private int selectedLevel = 0; // 0 = 취소, 1-3 = 선택된 레벨
    private final String cityName;
    private final int basePrice;
    private final int playerCash;

    // 레벨별 특별 색상
    private static final Color BUTTON_LEVEL1 = new Color(39, 174, 96);   // 녹색
    private static final Color BUTTON_LEVEL2 = new Color(41, 128, 185);  // 파란색
    private static final Color BUTTON_LEVEL3 = new Color(142, 68, 173);  // 보라색
    private static final Color CASH_DISPLAY_COLOR = new Color(255, 193, 7); // 노란색

    public LevelSelectionDialog(JFrame parent, String cityName, int basePrice, int playerCash) {
        super(parent, "도시 건설 레벨 선택", true); // modal dialog
        this.cityName = cityName;
        this.basePrice = basePrice;
        this.playerCash = playerCash;

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

        // 레벨 선택 버튼 패널
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.CENTER);

        // 취소 버튼 패널
        JPanel cancelPanel = createCancelPanel();
        add(cancelPanel, BorderLayout.SOUTH);
    }

    /**
     * 헤더 패널 생성 (도시 이름 및 안내 메시지)
     */
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIConstants.PANEL_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));

        // 도시 이름
        JLabel titleLabel = new JLabel(cityName);
        titleLabel.setFont(UIConstants.FONT_SUBTITLE);
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 안내 메시지
        JLabel messageLabel = new JLabel("건설할 레벨을 선택하세요");
        messageLabel.setFont(UIConstants.FONT_BODY);
        messageLabel.setForeground(UIConstants.TEXT_SECONDARY);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 보유 자금 표시
        JLabel cashLabel = new JLabel("보유 자금: " + String.format("%,d", playerCash) + "원");
        cashLabel.setFont(new Font(UIConstants.FONT_NAME, Font.BOLD, 13));
        cashLabel.setForeground(CASH_DISPLAY_COLOR);
        cashLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(messageLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(cashLabel);

        return panel;
    }

    /**
     * 레벨 선택 버튼 패널 생성
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIConstants.BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // 레벨별 비용 계산
        int cost1 = basePrice;
        int cost2 = (int)(basePrice * 1.3);
        int cost3 = (int)(basePrice * 1.6);

        // 레벨 1 버튼
        JButton level1Button = createLevelButton(
            "🏠 레벨 1 (집)",
            cost1,
            BUTTON_LEVEL1,
            1
        );
        level1Button.setEnabled(playerCash >= cost1);

        // 레벨 2 버튼
        JButton level2Button = createLevelButton(
            "🏢 레벨 2 (아파트)",
            cost2,
            BUTTON_LEVEL2,
            2
        );
        level2Button.setEnabled(playerCash >= cost2);

        // 레벨 3 버튼
        JButton level3Button = createLevelButton(
            "🏬 레벨 3 (건물)",
            cost3,
            BUTTON_LEVEL3,
            3
        );
        level3Button.setEnabled(playerCash >= cost3);

        panel.add(level1Button);
        panel.add(Box.createRigidArea(new Dimension(0, 12)));
        panel.add(level2Button);
        panel.add(Box.createRigidArea(new Dimension(0, 12)));
        panel.add(level3Button);

        return panel;
    }

    /**
     * 레벨 버튼 생성
     */
    private JButton createLevelButton(String levelText, int cost, Color bgColor, int level) {
        JButton button = new JButton();
        button.setLayout(new BorderLayout(10, 0));
        button.setPreferredSize(new Dimension(350, 60));
        button.setMaximumSize(new Dimension(350, 60));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 레벨 텍스트 (왼쪽)
        JLabel levelLabel = new JLabel(levelText);
        levelLabel.setFont(new Font(UIConstants.FONT_NAME, Font.BOLD, 16));
        levelLabel.setForeground(UIConstants.TEXT_PRIMARY);
        levelLabel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));

        // 비용 텍스트 (오른쪽)
        JLabel costLabel = new JLabel(String.format("%,d", cost) + "원");
        costLabel.setFont(new Font(UIConstants.FONT_NAME, Font.BOLD, 16));
        costLabel.setForeground(UIConstants.TEXT_PRIMARY);
        costLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15));

        button.add(levelLabel, BorderLayout.WEST);
        button.add(costLabel, BorderLayout.EAST);

        // 색상 설정
        button.setBackground(bgColor);

        // 호버 효과
        Color hoverColor = bgColor.brighter();
        Color disabledColor = UIConstants.BUTTON_DISABLED;

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(hoverColor);
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(bgColor);
                } else {
                    button.setBackground(disabledColor);
                }
            }
        });

        // 버튼 클릭 이벤트
        button.addActionListener(e -> {
            selectedLevel = level;
            dispose();
        });

        // 비활성화 시 색상 변경
        if (playerCash < cost) {
            button.setBackground(disabledColor);
            levelLabel.setForeground(UIConstants.TEXT_DISABLED);
            costLabel.setForeground(UIConstants.TEXT_DISABLED);
        }

        return button;
    }

    /**
     * 취소 버튼 패널 생성
     */
    private JPanel createCancelPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(UIConstants.BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        JButton cancelButton = UIConstants.createStyledButton("취소", UIConstants.BUTTON_SKIP);
        cancelButton.addActionListener(e -> {
            selectedLevel = 0; // 취소
            dispose();
        });

        panel.add(cancelButton);

        return panel;
    }

    /**
     * 선택된 레벨 반환
     * @return 0 (취소), 1-3 (선택된 레벨)
     */
    public int getSelectedLevel() {
        return selectedLevel;
    }
}
