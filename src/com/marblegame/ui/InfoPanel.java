package com.marblegame.ui;

import com.marblegame.model.Player;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 플레이어 정보를 비율 유지하며 렌더링하는 패널
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

    private static final int TURN_BASE_WIDTH = 250;
    private static final int TURN_BASE_HEIGHT = 85;
    private static final int CARD_BASE_WIDTH = 250;
    private static final int CARD_BASE_HEIGHT = 120;

    private final List<Player> players;
    private final TurnPanel turnPanel;
    private final List<PlayerInfoPanel> playerPanels;

    public InfoPanel(List<Player> players) {
        this.players = players;
        setLayout(new GridBagLayout());
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        turnPanel = new TurnPanel();
        playerPanels = new ArrayList<>();

        double totalBaseHeight = TURN_BASE_HEIGHT + players.size() * CARD_BASE_HEIGHT;
        if (totalBaseHeight <= 0) {
            totalBaseHeight = 1.0;
        }
        double accumulated = 0.0;

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 12, 0);
        gbc.weightx = 1.0;

        gbc.gridy = 0;
        gbc.weighty = TURN_BASE_HEIGHT / totalBaseHeight;
        add(wrapWithAspect(turnPanel, TURN_BASE_WIDTH, TURN_BASE_HEIGHT), gbc);
        accumulated += gbc.weighty;

        for (int i = 0; i < players.size(); i++) {
            PlayerInfoPanel panel = new PlayerInfoPanel(players.get(i), i);
            playerPanels.add(panel);

            gbc.gridy = i + 1;
            gbc.insets = new Insets(i == 0 ? 12 : 10, 0, 0, 0);
            double weight = CARD_BASE_HEIGHT / totalBaseHeight;
            gbc.weighty = weight;
            accumulated += weight;
            add(wrapWithAspect(panel, CARD_BASE_WIDTH, CARD_BASE_HEIGHT), gbc);
        }

        if (accumulated < 1.0) {
            gbc.gridy = players.size() + 1;
            gbc.weighty = Math.max(0.0, 1.0 - accumulated);
            gbc.insets = new Insets(10, 0, 0, 0);
            add(Box.createVerticalGlue(), gbc);
        }
    }

    private AspectRatioWrapper wrapWithAspect(JComponent component, int baseWidth, int baseHeight) {
        return new AspectRatioWrapper(component, baseWidth, baseHeight);
    }

    public void updateInfo(int currentTurn) {
        turnPanel.setTurn(currentTurn);

        for (int i = 0; i < playerPanels.size(); i++) {
            playerPanels.get(i).setPlayer(players.get(i));
        }

        repaint();
    }

    private static class AspectRatioWrapper extends JPanel {
        private final JComponent content;
        private final int baseWidth;
        private final int baseHeight;

        AspectRatioWrapper(JComponent content, int baseWidth, int baseHeight) {
            super(null);
            this.content = content;
            this.baseWidth = baseWidth;
            this.baseHeight = baseHeight;
            setOpaque(false);
            add(content);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(Math.max(120, baseWidth / 2), Math.max(80, baseHeight / 2));
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(baseWidth, baseHeight);
        }

        @Override
        public void doLayout() {
            int width = getWidth();
            int height = getHeight();

            double scale = Math.min(width / (double) baseWidth, height / (double) baseHeight);
            int scaledWidth = (int) Math.round(baseWidth * scale);
            int scaledHeight = (int) Math.round(baseHeight * scale);

            int x = (width - scaledWidth) / 2;
            int y = (height - scaledHeight) / 2;

            content.setBounds(x, y, scaledWidth, scaledHeight);
        }
    }

    private class TurnPanel extends JPanel {
        private static final int BASE_WIDTH = TURN_BASE_WIDTH;
        private static final int BASE_HEIGHT = TURN_BASE_HEIGHT;

        private int currentTurn = 1;

        TurnPanel() {
            setOpaque(false);
        }

        void setTurn(int turn) {
            this.currentTurn = turn;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            double scale = Math.min(getWidth() / (double) BASE_WIDTH, getHeight() / (double) BASE_HEIGHT);
            int offsetX = (int) ((getWidth() - BASE_WIDTH * scale) / 2);
            int offsetY = (int) ((getHeight() - BASE_HEIGHT * scale) / 2);

            g2.translate(offsetX, offsetY);
            g2.scale(scale, scale);

            // 배경
            g2.setColor(CARD_BACKGROUND);
            g2.fillRoundRect(0, 0, BASE_WIDTH, BASE_HEIGHT, 18, 18);

            // 테두리
            g2.setStroke(new BasicStroke(3f));
            g2.setColor(BORDER_COLOR);
            g2.drawRoundRect(0, 0, BASE_WIDTH, BASE_HEIGHT, 18, 18);

            // 텍스트
            g2.setColor(TEXT_SECONDARY);
            Font subtitle = new Font("맑은 고딕", Font.PLAIN, 14);
            g2.setFont(subtitle);
            String title = "현재 턴";
            FontMetrics fm = g2.getFontMetrics();
            int titleX = (BASE_WIDTH - fm.stringWidth(title)) / 2;
            g2.drawString(title, titleX, 28);

            g2.setColor(new Color(52, 152, 219));
            Font turnFont = new Font("Arial", Font.BOLD, 46);
            g2.setFont(turnFont);
            String turnText = String.valueOf(currentTurn);
            FontMetrics turnFm = g2.getFontMetrics();
            g2.drawString(turnText, (BASE_WIDTH - turnFm.stringWidth(turnText)) / 2, 65);

            g2.dispose();
        }
    }

    private class PlayerInfoPanel extends JPanel {
        private static final int BASE_WIDTH = CARD_BASE_WIDTH;
        private static final int BASE_HEIGHT = CARD_BASE_HEIGHT;

        private Player player;
        private final int playerIndex;

        PlayerInfoPanel(Player player, int index) {
            this.player = player;
            this.playerIndex = index;
            setOpaque(false);
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

            double scale = Math.min(getWidth() / (double) BASE_WIDTH, getHeight() / (double) BASE_HEIGHT);
            int offsetX = (int) ((getWidth() - BASE_WIDTH * scale) / 2);
            int offsetY = (int) ((getHeight() - BASE_HEIGHT * scale) / 2);

            g2.translate(offsetX, offsetY);
            g2.scale(scale, scale);

            // 카드 배경
            g2.setColor(CARD_BACKGROUND);
            g2.fillRoundRect(0, 0, BASE_WIDTH, BASE_HEIGHT, 18, 18);

            // 테두리
            Color accent = PLAYER_COLORS[playerIndex % PLAYER_COLORS.length];
            g2.setColor(accent);
            g2.setStroke(new BasicStroke(3f));
            g2.drawRoundRect(0, 0, BASE_WIDTH, BASE_HEIGHT, 18, 18);

            g2.setColor(TEXT_PRIMARY);
            Font nameFont = fitFont(g2, new Font("맑은 고딕", Font.BOLD, 12), player.name, BASE_WIDTH - 80, 8);
            g2.setFont(nameFont);
            FontMetrics nameFm = g2.getFontMetrics();
            int nameY = 38;
            g2.drawString(player.name, 20, nameY);

            // 정보 텍스트
            Font infoFont = new Font("맑은 고딕", Font.PLAIN, 10);
            infoFont = fitFont(g2, infoFont, "💰 000,000원", BASE_WIDTH - 40, 6);
            g2.setFont(infoFont);
            g2.setColor(TEXT_PRIMARY);
            int infoY = 56;
            int lineHeight = g2.getFontMetrics().getHeight() + 2;

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

        private Font fitFont(Graphics2D g2, Font baseFont, String text, int maxWidth, int minSize) {
            Font font = baseFont;
            FontMetrics fm = g2.getFontMetrics(font);
            while (fm.stringWidth(text) > maxWidth && font.getSize() > minSize) {
                font = font.deriveFont(font.getSize2D() - 1f);
                fm = g2.getFontMetrics(font);
            }
            return font;
        }
    }
}
