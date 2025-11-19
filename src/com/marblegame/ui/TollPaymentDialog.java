package com.marblegame.ui;

import com.marblegame.util.ImageLoader;
import javax.swing.*;
import java.awt.*;

/**
 * 통행료 지불 확인 다이얼로그
 */
public class TollPaymentDialog extends JDialog {
    private final String cityName;
    private final String ownerName;
    private final int level;
    private final int toll;
    private final boolean hasOlympicBoost;
    private final int playerCash;

    public TollPaymentDialog(JFrame parent, String cityName, String ownerName, int level,
                            int toll, boolean hasOlympicBoost, int playerCash) {
        super(parent, "통행료 지불", true); // modal dialog
        this.cityName = cityName;
        this.ownerName = ownerName;
        this.level = level;
        this.toll = toll;
        this.hasOlympicBoost = hasOlympicBoost;
        this.playerCash = playerCash;

        initComponents();
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); // ESC로 닫기 불가
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

        // 도시 이름
        JLabel titleLabel = new JLabel(cityName);
        titleLabel.setFont(UIConstants.FONT_SUBTITLE);
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 안내 메시지
        JLabel messageLabel = new JLabel("통행료를 지불하시겠습니까?");
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

        // 소유자
        JPanel ownerPanel = createInfoRow("소유자", ownerName);
        panel.add(ownerPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 레벨 (아이콘 포함)
        JPanel levelPanel = createInfoRowWithIcon("레벨", "레벨 " + level, level);
        panel.add(levelPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 통행료
        JPanel tollPanel = createInfoRowWithMoneyIcon("통행료", String.format("%,d원", toll));
        panel.add(tollPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 올림픽 효과
        if (hasOlympicBoost) {
            JPanel olympicPanel = createInfoRow("올림픽 효과", "통행료 2배!");
            panel.add(olympicPanel);
            panel.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        // 구분선
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(149, 165, 166, 100));
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panel.add(separator);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 보유 자금
        JPanel cashPanel = createInfoRowWithMoneyIcon("보유 자금", String.format("%,d원", playerCash));
        panel.add(cashPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 지불 후 잔액
        int remainingCash = playerCash - toll;
        JPanel remainingPanel = createInfoRowWithMoneyIcon("지불 후 잔액",
            String.format("%,d원", remainingCash), remainingCash < 0);
        panel.add(remainingPanel);

        // 파산 경고
        if (remainingCash < 0) {
            panel.add(Box.createRigidArea(new Dimension(0, 15)));
            JLabel warningLabel = new JLabel("⚠ 잔액이 부족합니다! 파산 처리됩니다.");
            warningLabel.setFont(UIConstants.FONT_SMALL_BOLD);
            warningLabel.setForeground(UIConstants.STATUS_ERROR);
            warningLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(warningLabel);
        }

        return panel;
    }

    /**
     * 정보 행 생성
     */
    private JPanel createInfoRow(String label, String value) {
        return createInfoRow(label, value, false);
    }

    private JPanel createInfoRow(String label, String value, boolean isWarning) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(400, 30));

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(UIConstants.FONT_BODY);
        labelComp.setForeground(UIConstants.TEXT_SECONDARY);

        JLabel valueComp = new JLabel(value);
        valueComp.setFont(UIConstants.FONT_BODY_BOLD);
        valueComp.setForeground(isWarning ? UIConstants.STATUS_ERROR : UIConstants.TEXT_PRIMARY);
        valueComp.setHorizontalAlignment(SwingConstants.RIGHT);

        panel.add(labelComp, BorderLayout.WEST);
        panel.add(valueComp, BorderLayout.EAST);

        return panel;
    }

    /**
     * 버튼 패널 생성
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(UIConstants.BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        // 지불하기 버튼
        JButton confirmButton = UIConstants.createStyledButton("지불하기", UIConstants.BUTTON_CONFIRM);
        confirmButton.addActionListener(e -> dispose());

        panel.add(confirmButton);

        return panel;
    }

    /**
     * 아이콘 포함 정보 행 생성
     */
    private JPanel createInfoRowWithIcon(String label, String value, int buildingLevel) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(400, 30));

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(UIConstants.FONT_BODY);
        labelComp.setForeground(UIConstants.TEXT_SECONDARY);

        // 값과 아이콘을 담을 패널
        JPanel valuePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        valuePanel.setOpaque(false);

        JLabel valueComp = new JLabel(value);
        valueComp.setFont(UIConstants.FONT_BODY_BOLD);
        valueComp.setForeground(UIConstants.TEXT_PRIMARY);

        // 건물 아이콘 추가
        ImageIcon icon = ImageLoader.loadIcon(getBuildingIconName(buildingLevel), 20, 20);
        if (icon != null) {
            JLabel iconLabel = new JLabel(icon);
            valuePanel.add(valueComp);
            valuePanel.add(iconLabel);
        } else {
            valuePanel.add(valueComp);
        }

        panel.add(labelComp, BorderLayout.WEST);
        panel.add(valuePanel, BorderLayout.EAST);

        return panel;
    }

    /**
     * 건물 아이콘 파일명 반환
     */
    private String getBuildingIconName(int level) {
        switch (level) {
            case 1: return "house.png";
            case 2: return "building.png";
            case 3: return "tower.png";
            case 4: return "landmark.png";
            default: return null;
        }
    }

    /**
     * 돈 아이콘 포함 정보 행 생성
     */
    private JPanel createInfoRowWithMoneyIcon(String label, String value) {
        return createInfoRowWithMoneyIcon(label, value, false);
    }

    private JPanel createInfoRowWithMoneyIcon(String label, String value, boolean isWarning) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(400, 30));

        // 라벨과 돈 아이콘을 담을 패널
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        labelPanel.setOpaque(false);

        // 돈 아이콘 추가
        ImageIcon moneyIcon = ImageLoader.loadIcon("money.png", 16, 16);
        if (moneyIcon != null) {
            JLabel iconLabel = new JLabel(moneyIcon);
            labelPanel.add(iconLabel);
        }

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(UIConstants.FONT_BODY);
        labelComp.setForeground(UIConstants.TEXT_SECONDARY);
        labelPanel.add(labelComp);

        JLabel valueComp = new JLabel(value);
        valueComp.setFont(UIConstants.FONT_BODY_BOLD);
        valueComp.setForeground(isWarning ? UIConstants.STATUS_ERROR : UIConstants.TEXT_PRIMARY);
        valueComp.setHorizontalAlignment(SwingConstants.RIGHT);

        panel.add(labelPanel, BorderLayout.WEST);
        panel.add(valueComp, BorderLayout.EAST);

        return panel;
    }
}
