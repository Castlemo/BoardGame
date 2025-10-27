package com.marblegame.ui;

import com.marblegame.model.*;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * 게임 보드를 그래픽으로 렌더링하는 패널
 * 28칸을 정사각형 형태로 배치
 */
public class BoardPanel extends JPanel {
    private static final int TILE_SIZE = 90;
    private static final int BOARD_SIZE = 8; // 한 변에 8칸
    private static final Color[] PLAYER_COLORS = {
        new Color(231, 76, 60),   // Red
        new Color(52, 152, 219),  // Blue
        new Color(46, 204, 113),  // Green
        new Color(230, 126, 34)   // Orange
    };

    private Board board;
    private List<Player> players;

    public BoardPanel(Board board, List<Player> players) {
        this.board = board;
        this.players = players;
        setPreferredSize(new Dimension(TILE_SIZE * BOARD_SIZE, TILE_SIZE * BOARD_SIZE));
        setBackground(new Color(44, 62, 80)); // 다크 네이비
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawBoard(g2d);
        drawPlayers(g2d);
    }

    private void drawBoard(Graphics2D g) {
        // 상단 (0-7)
        for (int i = 0; i < 8; i++) {
            drawTile(g, i, i * TILE_SIZE, 0);
        }

        // 우측 (8-13)
        for (int i = 8; i <= 13; i++) {
            drawTile(g, i, 7 * TILE_SIZE, (i - 7) * TILE_SIZE);
        }

        // 하단 (14-20)
        for (int i = 14; i <= 20; i++) {
            int x = (7 - (i - 14)) * TILE_SIZE;
            drawTile(g, i, x, 7 * TILE_SIZE);
        }

        // 좌측 (21-27)
        for (int i = 21; i <= 27; i++) {
            int y = (7 - (i - 21)) * TILE_SIZE;
            drawTile(g, i, 0, y);
        }

        // 중앙 로고
        drawCenterLogo(g);
    }

    private void drawTile(Graphics2D g, int tileIndex, int x, int y) {
        Tile tile = board.getTile(tileIndex);

        // 타일 배경
        Color bgColor = getTileColor(tile);
        g.setColor(bgColor);
        g.fillRoundRect(x + 2, y + 2, TILE_SIZE - 4, TILE_SIZE - 4, 10, 10);

        // 타일 테두리
        g.setColor(new Color(236, 240, 241));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(x + 2, y + 2, TILE_SIZE - 4, TILE_SIZE - 4, 10, 10);

        // 도시인 경우 소유자 및 레벨 표시
        if (tile instanceof City) {
            City city = (City) tile;

            // 삭제된 칸인 경우 X 표시
            if (city.isDeleted) {
                // 반투명한 배경
                g.setColor(new Color(44, 62, 80, 200));
                g.fillRoundRect(x + 2, y + 2, TILE_SIZE - 4, TILE_SIZE - 4, 10, 10);

                // 큰 빨간 X 표시
                g.setColor(new Color(192, 57, 43));
                g.setStroke(new BasicStroke(8));
                g.drawLine(x + 15, y + 15, x + TILE_SIZE - 15, y + TILE_SIZE - 15);
                g.drawLine(x + 15, y + TILE_SIZE - 15, x + TILE_SIZE - 15, y + 15);

                // 삭제됨 텍스트
                g.setColor(new Color(236, 240, 241));
                g.setFont(new Font("맑은 고딕", Font.BOLD, 10));
                String deletedText = "삭제됨";
                FontMetrics fm = g.getFontMetrics();
                int textWidth = fm.stringWidth(deletedText);
                g.drawString(deletedText, x + (TILE_SIZE - textWidth) / 2, y + TILE_SIZE / 2 + 20);
                return; // 더 이상 그리지 않음
            }

            // 컬러 바 (상단)
            g.setColor(getColorGroupColor(city.colorGroup));
            g.fillRoundRect(x + 4, y + 4, TILE_SIZE - 8, 12, 5, 5);

            if (city.isOwned()) {
                // 소유자 표시 (좌측 상단 원)
                g.setColor(PLAYER_COLORS[city.owner]);
                g.fillOval(x + 8, y + 20, 20, 20);

                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 12));
                g.drawString(String.valueOf((char)('A' + city.owner)), x + 14, y + 34);

                // 레벨 표시 (별)
                if (city.level > 0) {
                    g.setColor(new Color(241, 196, 15)); // 금색
                    g.setFont(new Font("Arial", Font.BOLD, 10));
                    for (int i = 0; i < city.level; i++) {
                        g.drawString("★", x + 32 + i * 12, y + 34);
                    }
                }
            }
        }

        // 타일 이름
        g.setColor(Color.WHITE);
        g.setFont(new Font("맑은 고딕", Font.BOLD, 11));
        String name = tile.name;
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(name);
        g.drawString(name, x + (TILE_SIZE - textWidth) / 2, y + TILE_SIZE - 25);

        // 타일 번호
        g.setColor(new Color(189, 195, 199));
        g.setFont(new Font("Arial", Font.PLAIN, 9));
        g.drawString(String.valueOf(tileIndex), x + 8, y + TILE_SIZE - 8);

        // 특수 타일 아이콘
        drawTileIcon(g, tile, x, y);
    }

    private void drawTileIcon(Graphics2D g, Tile tile, int x, int y) {
        g.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        String icon = "";

        switch (tile.type) {
            case START:
                icon = "🏁";
                break;
            case ISLAND:
                icon = "🏝";
                break;
            case CHANCE:
                icon = "🎁";
                break;
        }

        if (!icon.isEmpty()) {
            g.drawString(icon, x + TILE_SIZE/2 - 12, y + TILE_SIZE/2 + 8);
        }
    }

    private Color getTileColor(Tile tile) {
        switch (tile.type) {
            case START:
                return new Color(26, 188, 156); // 청록색
            case ISLAND:
                return new Color(52, 73, 94); // 어두운 파랑
            case CHANCE:
                return new Color(142, 68, 173); // 보라색
            case CITY:
                return new Color(52, 73, 94); // 기본 어두운 파랑
        }
        return new Color(52, 73, 94);
    }

    private Color getColorGroupColor(String colorGroup) {
        if (colorGroup == null) return Color.GRAY;

        switch (colorGroup) {
            case "RED": return new Color(231, 76, 60);
            case "BLUE": return new Color(52, 152, 219);
            case "GREEN": return new Color(46, 204, 113);
            case "YELLOW": return new Color(241, 196, 15);
            default: return Color.GRAY;
        }
    }

    private void drawPlayers(Graphics2D g) {
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (player.bankrupt) continue;

            Point pos = getTilePosition(player.pos);

            // 플레이어 위치 조정
            int offsetX = (i % 2) * 25 + 5;
            int offsetY = (i / 2) * 30 + 40;

            drawPlayerIcon(g, pos.x + offsetX, pos.y + offsetY, PLAYER_COLORS[i], (char)('A' + i));
        }
    }

    /**
     * 사람 모양 플레이어 아이콘 그리기
     */
    private void drawPlayerIcon(Graphics2D g, int x, int y, Color color, char label) {
        // 그림자
        g.setColor(new Color(0, 0, 0, 100));
        g.fillOval(x + 4, y + 25, 16, 4); // 발 아래 그림자

        // 머리
        g.setColor(color);
        g.fillOval(x + 6, y, 12, 12);

        // 머리 테두리
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval(x + 6, y, 12, 12);

        // 몸통 (사다리꼴)
        g.setColor(color);
        int[] bodyX = {x + 8, x + 16, x + 18, x + 6};
        int[] bodyY = {y + 12, y + 12, y + 22, y + 22};
        g.fillPolygon(bodyX, bodyY, 4);

        // 몸통 테두리
        g.setColor(Color.WHITE);
        g.drawPolygon(bodyX, bodyY, 4);

        // 팔 (좌)
        g.setColor(color);
        g.setStroke(new BasicStroke(2.5f));
        g.drawLine(x + 8, y + 14, x + 4, y + 18);

        // 팔 (우)
        g.drawLine(x + 16, y + 14, x + 20, y + 18);

        // 다리 (좌)
        g.drawLine(x + 9, y + 22, x + 7, y + 28);

        // 다리 (우)
        g.drawLine(x + 15, y + 22, x + 17, y + 28);

        // 플레이어 라벨 (머리에 표시)
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 8));
        String labelStr = String.valueOf(label);
        FontMetrics fm = g.getFontMetrics();
        int labelWidth = fm.stringWidth(labelStr);
        g.drawString(labelStr, x + 12 - labelWidth/2, y + 8);
    }

    private Point getTilePosition(int tileIndex) {
        // 상단 (0-7)
        if (tileIndex < 8) {
            return new Point(tileIndex * TILE_SIZE, 0);
        }
        // 우측 (8-13)
        else if (tileIndex <= 13) {
            return new Point(7 * TILE_SIZE, (tileIndex - 7) * TILE_SIZE);
        }
        // 하단 (14-20)
        else if (tileIndex <= 20) {
            return new Point((7 - (tileIndex - 14)) * TILE_SIZE, 7 * TILE_SIZE);
        }
        // 좌측 (21-27)
        else {
            return new Point(0, (7 - (tileIndex - 21)) * TILE_SIZE);
        }
    }

    private void drawCenterLogo(Graphics2D g) {
        int centerX = TILE_SIZE * 2;
        int centerY = TILE_SIZE * 2;
        int centerW = TILE_SIZE * 4;
        int centerH = TILE_SIZE * 4;

        // 배경
        g.setColor(new Color(236, 240, 241));
        g.fillRoundRect(centerX, centerY, centerW, centerH, 20, 20);

        // 그라데이션 효과
        GradientPaint gradient = new GradientPaint(
            centerX, centerY, new Color(52, 152, 219),
            centerX, centerY + centerH, new Color(41, 128, 185)
        );
        g.setPaint(gradient);
        g.fillRoundRect(centerX + 20, centerY + 20, centerW - 40, centerH - 40, 15, 15);

        // 타이틀
        g.setColor(Color.WHITE);
        g.setFont(new Font("맑은 고딕", Font.BOLD, 32));
        g.drawString("모두의", centerX + 85, centerY + 130);
        g.setFont(new Font("맑은 고딕", Font.BOLD, 40));
        g.drawString("마블", centerX + 95, centerY + 180);

        // 버전
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.setColor(new Color(236, 240, 241));
        g.drawString("v2.0", centerX + centerW - 60, centerY + centerH - 30);
    }

    public void updateBoard() {
        repaint();
    }
}
