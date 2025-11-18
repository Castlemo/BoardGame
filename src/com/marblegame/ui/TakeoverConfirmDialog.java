package com.marblegame.ui;

import com.marblegame.util.ImageLoader;
import javax.swing.*;
import java.awt.*;

/**
 * 도시 인수 확인 다이얼로그
 */
public class TakeoverConfirmDialog extends JDialog {
    private boolean confirmed = false;

    public TakeoverConfirmDialog(JFrame parent, String cityName, String currentOwner,
                                 int level, int takeoverCost, int playerCash) {
        super(parent, "도시 인수 확인", true);

        initComponents(cityName, currentOwner, level, takeoverCost, playerCash);
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initComponents(String cityName, String currentOwner, int level,
                               int takeoverCost, int playerCash) {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(UIConstants.BACKGROUND_DARK);

        // 헤더 패널
        JPanel headerPanel = createHeaderPanel(cityName);
        add(headerPanel, BorderLayout.NORTH);

        // 정보 패널
        JPanel infoPanel = createInfoPanel(currentOwner, level, takeoverCost, playerCash);
        add(infoPanel, BorderLayout.CENTER);

        // 버튼 패널
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel(String cityName) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIConstants.PANEL_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));

        JLabel titleLabel = new JLabel("▶ 도시 인수");
        titleLabel.setFont(UIConstants.FONT_SUBTITLE);
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel cityLabel = new JLabel(cityName);
        cityLabel.setFont(new Font(UIConstants.FONT_NAME, Font.BOLD, 16));
        cityLabel.setForeground(UIConstants.STATUS_INFO); // 파란색
        cityLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(cityLabel);

        return panel;
    }

    private JPanel createInfoPanel(String currentOwner, int level, int takeoverCost, int playerCash) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIConstants.BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // 현재 소유자
        panel.add(createInfoRow("현재 소유자:", currentOwner));
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        // 건물 레벨 (아이콘 포함)
        JPanel levelPanel = createInfoRowWithIcon("건물 레벨:", "레벨 " + level, level);
        panel.add(levelPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        // 인수 비용
        panel.add(createInfoRow("인수 비용:", String.format("%,d원", takeoverCost)));
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        // 구분선
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(70, 73, 77));
        separator.setMaximumSize(new Dimension(400, 1));
        panel.add(separator);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        // 현재 보유 자금
        panel.add(createInfoRow("보유 자금:", String.format("%,d원", playerCash)));
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        // 인수 후 잔액
        int remainingBalance = playerCash - takeoverCost;
        JPanel balanceRow = createInfoRow("인수 후 잔액:", String.format("%,d원", remainingBalance));

        // 잔액이 음수면 경고 표시
        if (remainingBalance < 0) {
            JLabel warningLabel = new JLabel("! 잔액 부족!");
            warningLabel.setFont(UIConstants.FONT_SMALL_BOLD);
            warningLabel.setForeground(UIConstants.STATUS_ERROR); // 빨간색
            warningLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(Box.createRigidArea(new Dimension(0, 10)));
            panel.add(warningLabel);
        }

        panel.add(balanceRow);

        return panel;
    }

    private JPanel createInfoRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(UIConstants.PANEL_DARK);
        row.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        row.setMaximumSize(new Dimension(400, 35));

        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(new Font(UIConstants.FONT_NAME, Font.PLAIN, 13));
        labelComponent.setForeground(UIConstants.TEXT_SECONDARY);

        JLabel valueComponent = new JLabel(value);
        valueComponent.setFont(new Font(UIConstants.FONT_NAME, Font.BOLD, 13));
        valueComponent.setForeground(UIConstants.TEXT_PRIMARY);

        row.add(labelComponent, BorderLayout.WEST);
        row.add(valueComponent, BorderLayout.EAST);

        return row;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 0));
        panel.setBackground(UIConstants.BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));

        // 예 버튼
        JButton confirmButton = UIConstants.createStyledButton("예", UIConstants.BUTTON_CONFIRM);
        confirmButton.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        // 아니오 버튼
        JButton cancelButton = UIConstants.createStyledButton("아니오", UIConstants.BUTTON_WARNING);
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        panel.add(confirmButton);
        panel.add(cancelButton);

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
        labelComp.setFont(new Font(UIConstants.FONT_NAME, Font.PLAIN, 13));
        labelComp.setForeground(UIConstants.TEXT_SECONDARY);

        // 값과 아이콘을 담을 패널
        JPanel valuePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        valuePanel.setOpaque(false);

        JLabel valueComp = new JLabel(value);
        valueComp.setFont(new Font(UIConstants.FONT_NAME, Font.BOLD, 13));
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

    public boolean isConfirmed() {
        return confirmed;
    }
}
