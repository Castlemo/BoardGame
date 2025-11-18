package com.marblegame.ui;

import com.marblegame.util.ImageLoader;
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

        // 선택지 설명 (카드 스타일)
        JPanel lockCard = createChoiceCard(
            "lock.png",
            "잠금 모드",
            "다음 내 턴까지 다른 플레이어가",
            "이 관광지를 인수할 수 없습니다",
            BUTTON_LOCK
        );

        JPanel diceCard = createChoiceCard(
            "dice.png",
            "추가 주사위",
            "즉시 주사위를 한 번 더 굴려",
            "추가로 이동할 수 있습니다",
            BUTTON_EXTRA
        );

        panel.add(lockCard);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        panel.add(diceCard);

        return panel;
    }

    /**
     * 선택지 카드 생성
     */
    private JPanel createChoiceCard(String iconFile, String title, String desc1, String desc2, Color accentColor) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout(15, 0));
        card.setBackground(UIConstants.PANEL_DARK);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor.darker(), 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        card.setMaximumSize(new Dimension(400, 80));

        // 아이콘
        ImageIcon icon = ImageLoader.loadIcon(iconFile, 40, 40);
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setPreferredSize(new Dimension(40, 40));

        // 텍스트 영역
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UIConstants.FONT_BODY_BOLD);
        titleLabel.setForeground(accentColor.brighter());

        JLabel desc1Label = new JLabel(desc1);
        desc1Label.setFont(UIConstants.FONT_CAPTION);
        desc1Label.setForeground(UIConstants.TEXT_SECONDARY);

        JLabel desc2Label = new JLabel(desc2);
        desc2Label.setFont(UIConstants.FONT_CAPTION);
        desc2Label.setForeground(UIConstants.TEXT_SECONDARY);

        textPanel.add(titleLabel);
        textPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        textPanel.add(desc1Label);
        textPanel.add(desc2Label);

        card.add(iconLabel, BorderLayout.WEST);
        card.add(textPanel, BorderLayout.CENTER);

        return card;
    }

    /**
     * 버튼 패널 생성
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIConstants.BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));

        // 잠금, 주사위 한 번 더 (아이콘 버튼)
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        row.setOpaque(false);

        JButton lockButton = createIconButton("lock.png", "잠금", BUTTON_LOCK);
        lockButton.addActionListener(e -> {
            selectedChoice = Choice.LOCK;
            dispose();
        });

        JButton extraButton = createIconButton("dice.png", "주사위", BUTTON_EXTRA);
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
     * 아이콘 버튼 생성
     */
    private JButton createIconButton(String iconFile, String tooltip, Color bgColor) {
        JButton button = new JButton();

        // 아이콘 설정
        ImageIcon icon = ImageLoader.loadIcon(iconFile, 48, 48);
        if (icon != null) {
            button.setIcon(icon);
        }

        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(100, 80));
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
