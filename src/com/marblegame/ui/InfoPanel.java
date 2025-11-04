package com.marblegame.ui;

import com.marblegame.model.Player;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 플레이어 정보만 표시하는 패널 (좌측 배치용)
 * 수정됨: TurnPanel 제거 (중앙 오버레이로 이동), 단순 BoxLayout 사용
 */
public class InfoPanel extends JPanel {
    private static final Color BACKGROUND = new Color(44, 62, 80);
    private static final Color CARD_BACKGROUND = new Color(52, 73, 94);
    private static final Color BORDER_COLOR = new Color(41, 128, 185);
    private static final Color TEXT_PRIMARY = new Color(236, 240, 241);
    private static final Color TEXT_SECONDARY = new Color(189, 195, 199);

    private static final Color[] PLAYER_COLORS = {
        new Color(231, 76, 60),   // Red
        new Color(52, 152, 219),  // Blue
        new Color(46, 204, 113),  // Green
        new Color(230, 126, 34)   // Orange
    };

    private static final int CARD_WIDTH = 260;
    private static final int CARD_HEIGHT = 160;

    private final List<Player> players;
    private final List<PlayerInfoPanel> playerPanels;

    public InfoPanel(List<Player> players) {
        this.players = players;

        // 수정됨: BoxLayout으로 단순화
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        playerPanels = new ArrayList<>();

        // 플레이어 패널만 추가
        for (int i = 0; i < players.size(); i++) {
            if (i > 0) {
                add(Box.createRigidArea(new Dimension(0, 12))); // 간격
            }

            PlayerInfoPanel panel = new PlayerInfoPanel(players.get(i), i);
            panel.setMaximumSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));
            panel.setPreferredSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));
            panel.setAlignmentX(Component.CENTER_ALIGNMENT);

            playerPanels.add(panel);
            add(panel);
        }

        // 여백 추가
        add(Box.createVerticalGlue());
    }

    // 수정됨: currentTurn 파라미터 제거 (오버레이에서 표시)
    public void updateInfo() {
        for (int i = 0; i < playerPanels.size(); i++) {
            playerPanels.get(i).setPlayer(players.get(i));
        }
        repaint();
    }

    private class PlayerInfoPanel extends JPanel {
        private Player player;
        private final int playerIndex;

        PlayerInfoPanel(Player player, int index) {
            this.player = player;
            this.playerIndex = index;
            setOpaque(false);
            setPreferredSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));
        }

        void setPlayer(Player player) {
            this.player = player;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            // 카드 배경
            g2.setColor(CARD_BACKGROUND);
            g2.fillRoundRect(0, 0, width, height, 18, 18);

            // 테두리
            Color accent = PLAYER_COLORS[playerIndex % PLAYER_COLORS.length];
            g2.setColor(accent);
            g2.setStroke(new BasicStroke(3f));
            g2.drawRoundRect(0, 0, width, height, 18, 18);

            // 플레이어 이름
            g2.setColor(TEXT_PRIMARY);
            Font nameFont = new Font("Malgun Gothic", Font.BOLD, 18);
            g2.setFont(nameFont);
            g2.drawString(player.name, 20, 35);

            // 정보 텍스트
            Font infoFont = new Font("Malgun Gothic", Font.PLAIN, 14);
            g2.setFont(infoFont);
            g2.setColor(TEXT_PRIMARY);
            int infoY = 60;
            int lineHeight = 22;

            g2.drawString(String.format("💰 %,d원", player.cash), 20, infoY);
            infoY += lineHeight;

            g2.drawString(String.format("📍 %d번 칸", player.pos), 20, infoY);
            infoY += lineHeight;

            String status = player.bankrupt ? "💀 파산" : "✅ 플레이 중";
            g2.drawString(status, 20, infoY);
            infoY += lineHeight;

            String jailInfo = player.jailTurns > 0 ? String.format("🏝 %d턴 남음", player.jailTurns) : "🏝 없음";
            g2.setColor(TEXT_SECONDARY);
            g2.drawString(jailInfo, 20, infoY);

            g2.dispose();
        }
    }
}
