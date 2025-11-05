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

    // 다크 테마 색상
    private static final Color BACKGROUND_DARK = new Color(32, 33, 36);
    private static final Color PANEL_DARK = new Color(44, 47, 51);
    private static final Color TEXT_PRIMARY = new Color(232, 234, 237);
    private static final Color TEXT_SECONDARY = new Color(189, 195, 199);
    private static final Color BUTTON_CONFIRM = new Color(39, 174, 96);  // 녹색
    private static final Color BUTTON_CANCEL = new Color(127, 140, 141); // 회색
    private static final Color BUTTON_DISABLED = new Color(60, 63, 65);  // 어두운 회색

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
        getContentPane().setBackground(BACKGROUND_DARK);

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
        panel.setBackground(PANEL_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));

        // 관광지 이름
        JLabel titleLabel = new JLabel(spotName);
        titleLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 20));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 안내 메시지
        JLabel messageLabel = new JLabel("이 관광지를 매입하시겠습니까?");
        messageLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        messageLabel.setForeground(TEXT_SECONDARY);
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
        panel.setBackground(BACKGROUND_DARK);
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
        noteLabel.setFont(new Font("Malgun Gothic", Font.ITALIC, 11));
        noteLabel.setForeground(new Color(189, 195, 199));
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
        labelComp.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        labelComp.setForeground(TEXT_SECONDARY);

        JLabel valueComp = new JLabel(value);
        valueComp.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        valueComp.setForeground(TEXT_PRIMARY);
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
        panel.setBackground(BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        // 매입하기 버튼
        JButton confirmButton = createButton("매입하기", BUTTON_CONFIRM);
        boolean canAfford = playerCash >= price;
        confirmButton.setEnabled(canAfford);

        if (!canAfford) {
            confirmButton.setBackground(BUTTON_DISABLED);
            confirmButton.setForeground(new Color(150, 150, 150));
        }

        confirmButton.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        // 취소 버튼
        JButton cancelButton = createButton("취소", BUTTON_CANCEL);
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        panel.add(confirmButton);
        panel.add(cancelButton);

        return panel;
    }

    /**
     * 버튼 생성
     */
    private JButton createButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        button.setPreferredSize(new Dimension(120, 40));
        button.setBackground(bgColor);
        button.setForeground(TEXT_PRIMARY);
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
     * 매입 확인 여부 반환
     */
    public boolean isConfirmed() {
        return confirmed;
    }
}
