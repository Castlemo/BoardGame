package com.marblegame.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 더블 억제 알림 다이얼로그
 * 연속 더블 또는 합계 2/12로 인해 더블 효과가 무효화된 경우 표시
 */
public class DoubleSuppressedDialog extends JDialog {
    // 다크 테마 색상
    private static final Color BACKGROUND_DARK = new Color(32, 33, 36);
    private static final Color PANEL_DARK = new Color(44, 47, 51);
    private static final Color TEXT_PRIMARY = new Color(232, 234, 237);
    private static final Color TEXT_SECONDARY = new Color(189, 195, 199);
    private static final Color BUTTON_CONFIRM = new Color(52, 152, 219);  // 파란색
    private static final Color HIGHLIGHT_COLOR = new Color(231, 76, 60); // 빨강

    public DoubleSuppressedDialog(JFrame parent, int diceValue, int consecutiveCount) {
        super(parent, "더블 억제", true);

        initComponents(diceValue, consecutiveCount);
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initComponents(int diceValue, int consecutiveCount) {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(BACKGROUND_DARK);

        // 헤더 패널
        JPanel headerPanel = createHeaderPanel(diceValue);
        add(headerPanel, BorderLayout.NORTH);

        // 메시지 패널
        JPanel messagePanel = createMessagePanel(consecutiveCount);
        add(messagePanel, BorderLayout.CENTER);

        // 버튼 패널
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel(int diceValue) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(PANEL_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));

        JLabel titleLabel = new JLabel("🚫 더블 억제");
        titleLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 26));
        titleLabel.setForeground(HIGHLIGHT_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel diceLabel = new JLabel("주사위: (" + diceValue + ", " + diceValue + ")");
        diceLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 16));
        diceLabel.setForeground(TEXT_SECONDARY);
        diceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(diceLabel);

        return panel;
    }

    private JPanel createMessagePanel(int consecutiveCount) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel messageLabel = new JLabel("더블 효과가 적용되지 않습니다");
        messageLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 18));
        messageLabel.setForeground(TEXT_PRIMARY);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(messageLabel);
        panel.add(Box.createVerticalStrut(15));

        // 억제 이유 표시
        String reasonText = getSuppressionReasonText(consecutiveCount);
        JLabel reasonLabel = new JLabel(reasonText);
        reasonLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        reasonLabel.setForeground(TEXT_SECONDARY);
        reasonLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(reasonLabel);

        return panel;
    }

    private String getSuppressionReasonText(int consecutiveCount) {
        if (consecutiveCount >= 2) {
            return "연속 " + consecutiveCount + "회 더블로 인해 억제되었습니다";
        } else if (consecutiveCount == 1) {
            return "연속 더블 확률 감소로 억제되었습니다";
        } else {
            return "더블 확률 조정으로 억제되었습니다";
        }
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));

        JButton confirmButton = createButton("확인", BUTTON_CONFIRM);
        confirmButton.addActionListener(e -> dispose());

        panel.add(confirmButton);

        return panel;
    }

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
                button.setBackground(hoverColor);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }
}
