package com.marblegame.ui;

import com.marblegame.model.Player;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * 플레이어 정보를 표시하는 패널
 */
public class InfoPanel extends JPanel {
    private static final Color[] PLAYER_COLORS = {
        new Color(231, 76, 60),   // Red
        new Color(52, 152, 219),  // Blue
        new Color(46, 204, 113),  // Green
        new Color(230, 126, 34)   // Orange
    };

    private List<Player> players;
    private JLabel[] playerLabels;
    private JLabel turnLabel;

    public InfoPanel(List<Player> players) {
        this.players = players;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(280, 720));
        setBackground(new Color(44, 62, 80));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        initComponents();
    }

    private void initComponents() {
        // 턴 정보 패널
        JPanel turnPanel = createTurnPanel();
        add(turnPanel);
        add(Box.createVerticalStrut(20));

        // 플레이어 정보
        playerLabels = new JLabel[players.size()];

        for (int i = 0; i < players.size(); i++) {
            JPanel playerPanel = createPlayerPanel(i);
            add(playerPanel);
            add(Box.createVerticalStrut(12));
        }
    }

    private JPanel createTurnPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setMaximumSize(new Dimension(250, 85));
        panel.setBackground(new Color(52, 73, 94));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(41, 128, 185), 2),
            new EmptyBorder(10, 15, 10, 15)
        ));

        JLabel titleLabel = new JLabel("현재 턴");
        titleLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        titleLabel.setForeground(new Color(189, 195, 199));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        turnLabel = new JLabel("1");
        turnLabel.setFont(new Font("Arial", Font.BOLD, 36));
        turnLabel.setForeground(new Color(52, 152, 219));
        turnLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(turnLabel);

        return panel;
    }

    private JPanel createPlayerPanel(int index) {
        Player player = players.get(index);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(10, 5));
        panel.setMaximumSize(new Dimension(250, 120));
        panel.setBackground(new Color(52, 73, 94));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PLAYER_COLORS[index], 3),
            new EmptyBorder(10, 10, 10, 10)
        ));

        // 플레이어 아이콘 + 이름
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        headerPanel.setBackground(new Color(52, 73, 94));

        JLabel iconLabel = new JLabel();
        iconLabel.setPreferredSize(new Dimension(30, 30));
        iconLabel.setOpaque(true);
        iconLabel.setBackground(PLAYER_COLORS[index]);
        iconLabel.setForeground(Color.WHITE);
        iconLabel.setFont(new Font("Arial", Font.BOLD, 16));
        iconLabel.setText(String.valueOf((char)('A' + index)));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));

        JLabel nameLabel = new JLabel(player.name);
        nameLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        nameLabel.setForeground(Color.WHITE);

        headerPanel.add(iconLabel);
        headerPanel.add(nameLabel);

        // 플레이어 정보
        playerLabels[index] = new JLabel();
        updatePlayerLabel(index);
        playerLabels[index].setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        playerLabels[index].setForeground(new Color(236, 240, 241));

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(playerLabels[index], BorderLayout.CENTER);

        return panel;
    }

    private void updatePlayerLabel(int index) {
        Player player = players.get(index);
        String status = player.bankrupt ? "💀 파산" : "✓ 플레이 중";
        String statusColor = player.bankrupt ? "#e74c3c" : "#2ecc71";
        String jailInfo = player.jailTurns > 0 ? "<br>🏝 무인도 " + player.jailTurns + "턴 남음" : "";

        String html = String.format(
            "<html>" +
            "<div style='padding: 5px;'>" +
            "💰 현금: <b style='color: #f39c12;'>%,d</b>원<br>" +
            "📍 위치: <b>%d</b>번 칸<br>" +
            "<span style='color: %s;'>%s</span>%s" +
            "</div>" +
            "</html>",
            player.cash,
            player.pos,
            statusColor,
            status,
            jailInfo
        );

        playerLabels[index].setText(html);
    }

    public void updateInfo(int currentTurn) {
        turnLabel.setText(String.valueOf(currentTurn));
        for (int i = 0; i < players.size(); i++) {
            updatePlayerLabel(i);
        }
        repaint();
    }
}
