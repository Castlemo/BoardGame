package com.marblegame.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 세금 납부 확인 다이얼로그
 */
public class TaxPaymentDialog extends JDialog {
    private final int playerCash;
    private final int taxAmount;

    public TaxPaymentDialog(JFrame parent, int playerCash, int taxAmount) {
        super(parent, "세금 납부", true); // modal dialog
        this.playerCash = playerCash;
        this.taxAmount = taxAmount;

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

        // 제목
        JLabel titleLabel = new JLabel("🏛️ 국세청");
        titleLabel.setFont(UIConstants.FONT_SUBTITLE);
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 안내 메시지
        JLabel messageLabel = new JLabel("세금을 납부하시겠습니까?");
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

        // 보유 자금
        JPanel cashPanel = createInfoRow("💵 보유 자금", String.format("%,d원", playerCash));
        panel.add(cashPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 세율
        JPanel ratePanel = createInfoRow("📊 세율", "10%");
        panel.add(ratePanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 납부 세금
        JPanel taxPanel = createInfoRow("💸 납부 세금", String.format("%,d원", taxAmount));
        panel.add(taxPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 구분선
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(149, 165, 166, 100));
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panel.add(separator);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 납부 후 잔액
        int remainingCash = playerCash - taxAmount;
        JPanel remainingPanel = createInfoRow("💳 납부 후 잔액",
            String.format("%,d원", remainingCash), remainingCash < 0);
        panel.add(remainingPanel);

        // 파산 경고
        if (remainingCash < 0) {
            panel.add(Box.createRigidArea(new Dimension(0, 15)));
            JLabel warningLabel = new JLabel("⚠️ 잔액이 부족합니다! 파산 처리됩니다.");
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
        panel.setMaximumSize(new Dimension(350, 30));

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

        // 납부하기 버튼
        JButton confirmButton = UIConstants.createStyledButton("납부하기", UIConstants.BUTTON_CONFIRM);
        confirmButton.addActionListener(e -> dispose());

        panel.add(confirmButton);

        return panel;
    }
}
