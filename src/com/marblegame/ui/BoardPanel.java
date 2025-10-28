package com.marblegame.ui;

import com.marblegame.model.*;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * 게임 보드를 그래픽으로 렌더링하는 패널
 * 44칸을 정사각형 형태로 배치
 */
public class BoardPanel extends JPanel {
    private static final int TILE_SIZE = 60;
    private static final int BOARD_SIZE = 12; // 한 변에 12칸
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
        // 44칸 보드: 12x12 그리드
        // 반시계 방향: 출발(우하) → 무인도(좌하) → 복지기금(좌상) → 전국철도(우상) → 출발

        // 하단 (우→좌): 0(출발), 1-10, 11(무인도) = 12칸
        for (int i = 0; i <= 11; i++) {
            int x = (11 - i) * TILE_SIZE;
            drawTile(g, i, x, 11 * TILE_SIZE);
        }

        // 좌측 (하→상): 12-21, 22(복지기금) = 11칸 (11번 무인도는 이미 그려짐)
        for (int i = 12; i <= 22; i++) {
            int y = (11 - (i - 11)) * TILE_SIZE;
            drawTile(g, i, 0, y);
        }

        // 상단 (좌→우): 23-32, 33(전국철도) = 11칸 (22번 복지기금은 이미 그려짐)
        for (int i = 23; i <= 33; i++) {
            int x = (i - 22) * TILE_SIZE;
            drawTile(g, i, x, 0);
        }

        // 우측 (상→하): 34-43 = 10칸 (33번 전국철도는 이미 그려짐, 0번 출발로 순환)
        for (int i = 34; i <= 43; i++) {
            int y = (i - 33) * TILE_SIZE;
            drawTile(g, i, 11 * TILE_SIZE, y);
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
                g.setStroke(new BasicStroke(6));
                g.drawLine(x + 12, y + 12, x + TILE_SIZE - 12, y + TILE_SIZE - 12);
                g.drawLine(x + 12, y + TILE_SIZE - 12, x + TILE_SIZE - 12, y + 12);

                // 삭제됨 텍스트
                g.setColor(new Color(236, 240, 241));
                g.setFont(new Font("맑은 고딕", Font.BOLD, 9));
                String deletedText = "삭제됨";
                FontMetrics fm = g.getFontMetrics();
                int textWidth = fm.stringWidth(deletedText);
                g.drawString(deletedText, x + (TILE_SIZE - textWidth) / 2, y + TILE_SIZE / 2 + 15);
                return; // 더 이상 그리지 않음
            }

            // 컬러 바 (상단)
            g.setColor(getColorGroupColor(city.colorGroup));
            g.fillRoundRect(x + 4, y + 4, TILE_SIZE - 8, 10, 5, 5);

            if (city.isOwned()) {
                // 소유자 표시 (좌측 상단 원)
                g.setColor(PLAYER_COLORS[city.owner]);
                g.fillOval(x + 6, y + 16, 16, 16);

                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 10));
                g.drawString(String.valueOf((char)('A' + city.owner)), x + 11, y + 27);

                // 레벨 표시 (별)
                if (city.level > 0) {
                    g.setColor(new Color(241, 196, 15)); // 금색
                    g.setFont(new Font("Arial", Font.BOLD, 9));
                    for (int i = 0; i < city.level; i++) {
                        g.drawString("★", x + 24 + i * 10, y + 27);
                    }
                }
            }
        } else if (tile instanceof Palace) {
            // 궁(관광지)인 경우 소유자 표시
            Palace palace = (Palace) tile;

            // 삭제된 칸인 경우 X 표시
            if (palace.isDeleted) {
                g.setColor(new Color(44, 62, 80, 200));
                g.fillRoundRect(x + 2, y + 2, TILE_SIZE - 4, TILE_SIZE - 4, 10, 10);

                g.setColor(new Color(192, 57, 43));
                g.setStroke(new BasicStroke(6));
                g.drawLine(x + 12, y + 12, x + TILE_SIZE - 12, y + TILE_SIZE - 12);
                g.drawLine(x + 12, y + TILE_SIZE - 12, x + TILE_SIZE - 12, y + 12);

                g.setColor(new Color(236, 240, 241));
                g.setFont(new Font("맑은 고딕", Font.BOLD, 9));
                String deletedText = "삭제됨";
                FontMetrics fm = g.getFontMetrics();
                int textWidth = fm.stringWidth(deletedText);
                g.drawString(deletedText, x + (TILE_SIZE - textWidth) / 2, y + TILE_SIZE / 2 + 15);
                return;
            }

            if (palace.isOwned()) {
                // 소유자 표시 (좌측 상단 원)
                g.setColor(PLAYER_COLORS[palace.owner]);
                g.fillOval(x + 6, y + 16, 16, 16);

                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 10));
                g.drawString(String.valueOf((char)('A' + palace.owner)), x + 11, y + 27);
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
        g.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
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
            case PALACE:
                icon = "🏛";
                break;
            case WELFARE:
                icon = "💰";
                break;
            case RAILROAD:
                icon = "🚆";
                break;
        }

        if (!icon.isEmpty()) {
            g.drawString(icon, x + TILE_SIZE/2 - 10, y + TILE_SIZE/2 + 7);
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
            case PALACE:
                return new Color(155, 89, 182); // 자주색 (궁)
            case WELFARE:
                return new Color(52, 152, 219); // 밝은 파랑 (복지기금)
            case RAILROAD:
                return new Color(22, 160, 133); // 청록색 (철도)
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

            // 플레이어 위치 조정 (타일 크기 60px에 맞게)
            int offsetX = (i % 2) * 20 + 5;
            int offsetY = (i / 2) * 25 + 35;

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
        // 44칸 보드 위치 계산
        // 하단 (우→좌): 0-11
        if (tileIndex <= 11) {
            int x = (11 - tileIndex) * TILE_SIZE;
            return new Point(x, 11 * TILE_SIZE);
        }
        // 좌측 (하→상): 12-22
        else if (tileIndex <= 22) {
            int y = (11 - (tileIndex - 11)) * TILE_SIZE;
            return new Point(0, y);
        }
        // 상단 (좌→우): 23-33
        else if (tileIndex <= 33) {
            int x = (tileIndex - 22) * TILE_SIZE;
            return new Point(x, 0);
        }
        // 우측 (상→하): 34-43
        else {
            int y = (tileIndex - 33) * TILE_SIZE;
            return new Point(11 * TILE_SIZE, y);
        }
    }

    private void drawCenterLogo(Graphics2D g) {
        int centerX = TILE_SIZE * 2;
        int centerY = TILE_SIZE * 2;
        int centerW = TILE_SIZE * 8;
        int centerH = TILE_SIZE * 8;

        // 배경
        g.setColor(new Color(236, 240, 241));
        g.fillRoundRect(centerX, centerY, centerW, centerH, 20, 20);

        // 그라데이션 효과
        GradientPaint gradient = new GradientPaint(
            centerX, centerY, new Color(52, 152, 219),
            centerX, centerY + centerH, new Color(41, 128, 185)
        );
        g.setPaint(gradient);
        g.fillRoundRect(centerX + 15, centerY + 15, centerW - 30, centerH - 30, 15, 15);

        // 타이틀
        g.setColor(Color.WHITE);
        g.setFont(new Font("맑은 고딕", Font.BOLD, 48));
        String title1 = "모두의";
        FontMetrics fm1 = g.getFontMetrics();
        int x1 = centerX + (centerW - fm1.stringWidth(title1)) / 2;
        g.drawString(title1, x1, centerY + centerH / 2 - 20);

        g.setFont(new Font("맑은 고딕", Font.BOLD, 56));
        String title2 = "마블";
        FontMetrics fm2 = g.getFontMetrics();
        int x2 = centerX + (centerW - fm2.stringWidth(title2)) / 2;
        g.drawString(title2, x2, centerY + centerH / 2 + 40);

        // 버전
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.setColor(new Color(236, 240, 241));
        g.drawString("v2.0", centerX + centerW - 50, centerY + centerH - 20);
    }

    public void updateBoard() {
        repaint();
    }
}
