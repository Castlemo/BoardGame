package com.marblegame.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 관광지 구매 확인 다이얼로그
 */
public class TouristSpotPurchaseDialog extends JDialog {
    private boolean confirmed = false;
    private final String spotName;
    private final int price;
    private final int playerCash;

    public TouristSpotPurchaseDialog(JFrame parent, String spotName, int price, int playerCash) {
        super(parent, "관광지 매입 확인", true); // modal dialog
        this.spotName = spotName;
        this.price = price;
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
        JLabel messageLabel = new JLabel("이 관광지를 매입하시겠습니까?");
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

        // 매입 가격
        JPanel pricePanel = createInfoRow("💰 매입 가격", String.format("%,d원", price));
        panel.add(pricePanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 보유 자금
        JPanel cashPanel = createInfoRow("💵 보유 자금", String.format("%,d원", playerCash));
        panel.add(cashPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 매입 후 잔액
        int remainingCash = playerCash - price;
        JPanel remainingPanel = createInfoRow("💳 매입 후 잔액", String.format("%,d원", remainingCash));
        panel.add(remainingPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));

        // 안내 문구
        JLabel noteLabel = new JLabel("※ 관광지는 업그레이드가 불가능합니다");
        noteLabel.setFont(UIConstants.FONT_HINT);
        noteLabel.setForeground(UIConstants.TEXT_SECONDARY);
        noteLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(noteLabel);

        return panel;
    }

    /**
     * 정보 행 생성
     */
    private JPanel createInfoRow(String label, String value) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(350, 30));

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(UIConstants.FONT_BODY);
        labelComp.setForeground(UIConstants.TEXT_SECONDARY);

        JLabel valueComp = new JLabel(value);
        valueComp.setFont(UIConstants.FONT_BODY_BOLD);
        valueComp.setForeground(UIConstants.TEXT_PRIMARY);
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
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 0));
        panel.setBackground(UIConstants.BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        // 매입하기 버튼
        boolean canAfford = playerCash >= price;
        JButton confirmButton = UIConstants.createStyledButton("매입하기", UIConstants.BUTTON_CONFIRM, canAfford);

        confirmButton.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        // 취소 버튼
        JButton cancelButton = UIConstants.createStyledButton("취소", UIConstants.BUTTON_SKIP);
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        panel.add(confirmButton);
        panel.add(cancelButton);

        return panel;
    }

    /**
     * 매입 확인 여부 반환
     */
    public boolean isConfirmed() {
        return confirmed;
    }
}
