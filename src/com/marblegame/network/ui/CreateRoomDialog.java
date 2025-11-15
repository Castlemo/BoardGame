package com.marblegame.network.ui;

import com.marblegame.network.NetConstants;

import javax.swing.*;
import java.awt.*;
import java.net.InetAddress;

/**
 * 방 만들기 다이얼로그
 */
public class CreateRoomDialog extends JDialog {
    private JTextField nameField;
    private JTextField portField;
    private JComboBox<String> maxPlayersCombo;
    private boolean confirmed = false;

    // 다크 테마 색상
    private static final Color BACKGROUND_DARK = new Color(32, 33, 36);
    private static final Color PANEL_DARK = new Color(44, 47, 51);
    private static final Color TEXT_PRIMARY = new Color(232, 234, 237);
    private static final Color TEXT_SECONDARY = new Color(189, 195, 199);
    private static final Color BUTTON_CONFIRM = new Color(46, 204, 113);   // 녹색
    private static final Color BUTTON_CANCEL = new Color(127, 140, 141);   // 회색
    private static final Color INPUT_BG = new Color(55, 58, 64);
    private static final Color HIGHLIGHT_COLOR = new Color(52, 152, 219);  // 파란색

    public CreateRoomDialog(JFrame parent) {
        super(parent, "방 만들기", true);

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

        // 입력 폼 패널
        JPanel formPanel = createFormPanel();
        add(formPanel, BorderLayout.CENTER);

        // 버튼 패널
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(PANEL_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));

        JLabel titleLabel = new JLabel("🏠 방 만들기");
        titleLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 24));
        titleLabel.setForeground(HIGHLIGHT_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("게임 방을 만들고 친구를 초대하세요");
        subtitleLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        subtitleLabel.setForeground(TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(subtitleLabel);

        return panel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // 로컬 IP 주소 표시
        JPanel ipPanel = createIPPanel();
        panel.add(ipPanel);
        panel.add(Box.createVerticalStrut(20));

        // 플레이어 이름
        JPanel namePanel = createInputPanel(
            "플레이어 이름",
            "이름을 입력하세요",
            nameField = new JTextField(15)
        );
        panel.add(namePanel);
        panel.add(Box.createVerticalStrut(15));

        // 포트 번호
        JPanel portPanel = createInputPanel(
            "포트 번호",
            String.valueOf(NetConstants.DEFAULT_PORT),
            portField = new JTextField(String.valueOf(NetConstants.DEFAULT_PORT), 10)
        );
        panel.add(portPanel);
        panel.add(Box.createVerticalStrut(15));

        // 최대 플레이어 수
        JPanel playersPanel = createComboPanel();
        panel.add(playersPanel);

        return panel;
    }

    private JPanel createIPPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BACKGROUND_DARK);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel("로컬 IP 주소 (친구에게 알려주세요)");
        label.setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        label.setForeground(TEXT_SECONDARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        String localIP = getLocalIPAddress();
        JLabel ipLabel = new JLabel(localIP);
        ipLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 18));
        ipLabel.setForeground(new Color(46, 204, 113)); // 녹색
        ipLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(label);
        panel.add(Box.createVerticalStrut(5));
        panel.add(ipLabel);

        return panel;
    }

    private String getLocalIPAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "IP 주소를 가져올 수 없음";
        }
    }

    private JPanel createInputPanel(String labelText, String placeholder, JTextField textField) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BACKGROUND_DARK);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        label.setForeground(TEXT_SECONDARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        textField.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        textField.setForeground(TEXT_PRIMARY);
        textField.setBackground(INPUT_BG);
        textField.setCaretColor(TEXT_PRIMARY);
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 73, 79), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        textField.setMaximumSize(new Dimension(400, 40));
        textField.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(label);
        panel.add(Box.createVerticalStrut(5));
        panel.add(textField);

        return panel;
    }

    private JPanel createComboPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BACKGROUND_DARK);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel("최대 플레이어 수");
        label.setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        label.setForeground(TEXT_SECONDARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        String[] options = {"2명", "3명", "4명"};
        maxPlayersCombo = new JComboBox<>(options);
        maxPlayersCombo.setSelectedIndex(1); // 기본값: 3명
        maxPlayersCombo.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        maxPlayersCombo.setForeground(TEXT_PRIMARY);
        maxPlayersCombo.setBackground(INPUT_BG);
        maxPlayersCombo.setMaximumSize(new Dimension(400, 40));
        maxPlayersCombo.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(label);
        panel.add(Box.createVerticalStrut(5));
        panel.add(maxPlayersCombo);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));
        panel.setBackground(BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));

        JButton confirmButton = createButton("생성", BUTTON_CONFIRM);
        confirmButton.addActionListener(e -> {
            if (validateInput()) {
                confirmed = true;
                dispose();
            }
        });

        JButton cancelButton = createButton("취소", BUTTON_CANCEL);
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        panel.add(confirmButton);
        panel.add(cancelButton);

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

    private boolean validateInput() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "플레이어 이름을 입력해주세요.",
                "입력 오류",
                JOptionPane.ERROR_MESSAGE);
            return false;
        }

        String portStr = portField.getText().trim();
        try {
            int port = Integer.parseInt(portStr);
            if (port < 1024 || port > 65535) {
                JOptionPane.showMessageDialog(this,
                    "포트 번호는 1024-65535 사이여야 합니다.",
                    "입력 오류",
                    JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "유효한 포트 번호를 입력해주세요.",
                "입력 오류",
                JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    // Getters
    public boolean isConfirmed() {
        return confirmed;
    }

    public String getPlayerName() {
        return nameField.getText().trim();
    }

    public int getPort() {
        return Integer.parseInt(portField.getText().trim());
    }

    public int getMaxPlayers() {
        return maxPlayersCombo.getSelectedIndex() + 2; // 0->2, 1->3, 2->4
    }
}
