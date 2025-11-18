package com.marblegame.ui;

import com.marblegame.model.Player;
import com.marblegame.util.ImageLoader;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * 플레이어 정보만 표시하는 패널 (좌측 배치용)
 * 수정됨: TurnPanel 제거 (중앙 오버레이로 이동), 단순 BoxLayout 사용
 */
public class InfoPanel extends JPanel {
    // InfoPanel 전용 색상 (다른 UI와 다른 테마 사용)
    private static final Color BACKGROUND = new Color(44, 62, 80);
    private static final Color CARD_BACKGROUND = new Color(52, 73, 94);
    private static final Color BORDER_COLOR = new Color(41, 128, 185);

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

    /**
     * 특정 플레이어의 자산 변동 표시
     * @param playerIndex 플레이어 인덱스
     * @param change 변동 금액 (양수: 수입, 음수: 지출)
     */
    public void showMoneyChange(int playerIndex, int change) {
        if (playerIndex >= 0 && playerIndex < playerPanels.size()) {
            playerPanels.get(playerIndex).showMoneyChange(change);
        }
    }

    private class PlayerInfoPanel extends JPanel {
        private Player player;
        private final int playerIndex;
        private int moneyChange = 0;
        private long moneyChangeStartTime = 0;
        private static final long MONEY_CHANGE_DURATION = 2000; // 2초

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

        /**
         * 자산 변동 표시
         * @param change 변동 금액 (양수: 수입, 음수: 지출)
         */
        void showMoneyChange(int change) {
            if (change == 0) return;
            this.moneyChange = change;
            this.moneyChangeStartTime = System.currentTimeMillis();

            // 2초 동안 표시
            Timer timer = new Timer(50, null);
            timer.addActionListener(e -> {
                long elapsed = System.currentTimeMillis() - moneyChangeStartTime;
                if (elapsed >= MONEY_CHANGE_DURATION) {
                    moneyChange = 0;
                    timer.stop();
                }
                repaint();
            });
            timer.start();
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
            g2.setColor(UIConstants.TEXT_PRIMARY);
            g2.setFont(UIConstants.FONT_HEADER);
            g2.drawString(player.name, 20, 35);

            // 정보 텍스트
            g2.setFont(UIConstants.FONT_BODY);
            g2.setColor(UIConstants.TEXT_PRIMARY);
            int infoY = 60;
            int lineHeight = 22;

            // 돈 아이콘과 금액 표시
            BufferedImage moneyIcon = ImageLoader.getTileImage("MONEY");
            int iconX = 20;
            if (moneyIcon != null) {
                BufferedImage scaledMoney = ImageLoader.scaleImage(moneyIcon, 18, 18);
                g2.drawImage(scaledMoney, iconX, infoY - 14, null);
                iconX += 22;
            }
            g2.drawString(String.format("%,d원", player.cash), iconX, infoY);

            // 자산 변동 표시 (보유금액 바로 아래)
            if (moneyChange != 0 && System.currentTimeMillis() - moneyChangeStartTime < MONEY_CHANGE_DURATION) {
                String changeText;
                Color changeColor;
                if (moneyChange > 0) {
                    // 수입: 초록색
                    changeText = String.format("+%,d원", moneyChange);
                    changeColor = UIConstants.STATUS_SUCCESS;
                } else {
                    // 지출: 빨간색
                    changeText = String.format("%,d원", moneyChange);
                    changeColor = UIConstants.STATUS_ERROR;
                }

                // 페이드 아웃 효과
                long elapsed = System.currentTimeMillis() - moneyChangeStartTime;
                float alpha = 1.0f - ((float) elapsed / MONEY_CHANGE_DURATION);
                alpha = Math.max(0, Math.min(1, alpha));

                g2.setColor(new Color(changeColor.getRed(), changeColor.getGreen(), changeColor.getBlue(),
                    (int)(alpha * 255)));
                g2.setFont(UIConstants.FONT_BODY_BOLD);
                g2.drawString(changeText, 160, infoY);

                g2.setFont(UIConstants.FONT_BODY); // 원래 폰트로 복구
                g2.setColor(UIConstants.TEXT_PRIMARY); // 원래 색상으로 복구
            }

            infoY += lineHeight;

            g2.drawString(String.format("> %d번 칸", player.pos), 20, infoY);
            infoY += lineHeight;

            String status = player.bankrupt ? "X 파산" : "O 플레이 중";
            g2.drawString(status, 20, infoY);
            infoY += lineHeight;

            String jailInfo = player.jailTurns > 0 ? String.format("~ %d턴 남음", player.jailTurns) : "~ 없음";
            g2.setColor(UIConstants.TEXT_SECONDARY);
            g2.drawString(jailInfo, 20, infoY);

            g2.dispose();
        }
    }
}
