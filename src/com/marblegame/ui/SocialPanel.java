package com.marblegame.ui;

import com.marblegame.model.Player;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 우측 소셜 패널: 플레이어 요약 + 채팅
 */
public class SocialPanel extends JPanel {
    private static final Color PANEL_BG = UIConstants.BACKGROUND_DARK;
    private static final Color BORDER = UIConstants.BORDER_DEFAULT;

    private final List<Player> players;
    private final PlayerSummaryPanel summaryPanel;
    private final ChatPanel chatPanel;

    public SocialPanel(List<Player> players) {
        this.players = players;
        setLayout(new BorderLayout(0, 12));
        setBackground(PANEL_BG);
        setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, BORDER));
        setPreferredSize(new Dimension(260, 0)); // 화면의 약 20~25% 폭

        // 플레이어 요약
        summaryPanel = new PlayerSummaryPanel(players);
        add(summaryPanel, BorderLayout.NORTH);

        // 채팅 영역
        chatPanel = new ChatPanel();
        add(chatPanel, BorderLayout.CENTER);
    }

    public ChatPanel getChatPanel() {
        return chatPanel;
    }

    public void updatePlayerInfo() {
        summaryPanel.refresh();
    }

    private static class PlayerSummaryPanel extends JPanel {
        private static final Color[] PLAYER_COLORS = {
            new Color(231, 76, 60),   // Red
            new Color(52, 152, 219),  // Blue
            new Color(46, 204, 113),  // Green
            new Color(230, 126, 34)   // Orange
        };

        private final List<Player> players;
        private final List<Card> cards = new ArrayList<>();

        PlayerSummaryPanel(List<Player> players) {
            this.players = players;
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            setBorder(new EmptyBorder(12, 12, 0, 12));

            JLabel title = new JLabel("Players");
            title.setFont(UIConstants.FONT_HEADER);
            title.setForeground(UIConstants.TEXT_PRIMARY);
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(title);
            add(Box.createRigidArea(new Dimension(0, 8)));

            for (int i = 0; i < players.size(); i++) {
                Card card = new Card(players.get(i), i);
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                cards.add(card);
                add(card);
                add(Box.createRigidArea(new Dimension(0, 8)));
            }
        }

        void refresh() {
            for (int i = 0; i < cards.size(); i++) {
                cards.get(i).setPlayer(players.get(i));
            }
            repaint();
        }

        private class Card extends JPanel {
            private Player player;
            private final int index;

            Card(Player player, int index) {
                this.player = player;
                this.index = index;
                setPreferredSize(new Dimension(236, 86));
                setMaximumSize(new Dimension(Short.MAX_VALUE, 86));
                setOpaque(false);
            }

            void setPlayer(Player player) {
                this.player = player;
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int arc = 14;

                // 배경
                g2.setColor(UIConstants.PANEL_DARK);
                g2.fillRoundRect(0, 0, w, h, arc, arc);

                // 테두리
                Color accent = PLAYER_COLORS[index % PLAYER_COLORS.length];
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

                // 이름 + 상태
                g2.setFont(UIConstants.FONT_BODY_BOLD);
                g2.setColor(UIConstants.TEXT_PRIMARY);
                g2.drawString(player.name, 14, 24);

                String status = player.bankrupt ? "파산" : "플레이 중";
                g2.setFont(UIConstants.FONT_SMALL);
                g2.setColor(UIConstants.TEXT_SECONDARY);
                g2.drawString(status, 14, 42);

                // 현금
                g2.setFont(UIConstants.FONT_BODY);
                g2.setColor(UIConstants.TEXT_PRIMARY);
                String cash = String.format("%,d원", player.cash);
                g2.drawString(cash, 14, 60);

                // 위치
                g2.setFont(UIConstants.FONT_SMALL);
                g2.setColor(UIConstants.TEXT_SECONDARY);
                g2.drawString("위치: " + player.pos, 14, 74);

                g2.dispose();
            }
        }
    }
}
