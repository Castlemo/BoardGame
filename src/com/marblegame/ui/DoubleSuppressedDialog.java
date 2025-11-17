package com.marblegame.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 더블 억제 알림 다이얼로그
 * 연속 더블 또는 합계 2/12로 인해 더블 효과가 무효화된 경우 표시
 */
public class DoubleSuppressedDialog extends JDialog {

    public DoubleSuppressedDialog(JFrame parent, int diceValue, int consecutiveCount) {
        super(parent, "더블 억제", true);

        initComponents(diceValue, consecutiveCount);
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initComponents(int diceValue, int consecutiveCount) {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(UIConstants.BACKGROUND_DARK);

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
        panel.setBackground(UIConstants.PANEL_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));

        JLabel titleLabel = new JLabel("🚫 더블 억제");
        titleLabel.setFont(new Font(UIConstants.FONT_NAME, Font.BOLD, 26));
        titleLabel.setForeground(UIConstants.STATUS_ERROR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel diceLabel = new JLabel("주사위: (" + diceValue + ", " + diceValue + ")");
        diceLabel.setFont(new Font(UIConstants.FONT_NAME, Font.PLAIN, 16));
        diceLabel.setForeground(UIConstants.TEXT_SECONDARY);
        diceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(diceLabel);

        return panel;
    }

    private JPanel createMessagePanel(int consecutiveCount) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIConstants.BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel messageLabel = new JLabel("더블 효과가 적용되지 않습니다");
        messageLabel.setFont(UIConstants.FONT_HEADER);
        messageLabel.setForeground(UIConstants.TEXT_PRIMARY);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(messageLabel);
        panel.add(Box.createVerticalStrut(15));

        // 억제 이유 표시
        String reasonText = getSuppressionReasonText(consecutiveCount);
        JLabel reasonLabel = new JLabel(reasonText);
        reasonLabel.setFont(UIConstants.FONT_BODY);
        reasonLabel.setForeground(UIConstants.TEXT_SECONDARY);
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
        panel.setBackground(UIConstants.BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));

        JButton confirmButton = UIConstants.createStyledButton("확인", UIConstants.STATUS_INFO);
        confirmButton.addActionListener(e -> dispose());

        panel.add(confirmButton);

        return panel;
    }
}
