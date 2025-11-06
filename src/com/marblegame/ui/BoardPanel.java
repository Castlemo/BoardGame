package com.marblegame.ui;

import com.marblegame.model.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.function.Consumer;

/**
 * 게임 보드를 그래픽으로 렌더링하는 패널
 * 32칸을 9x9 정사각형 형태로 배치
 */
public class BoardPanel extends JPanel {
    private static final int BASE_TILE_SIZE = 80;
    private static final int BOARD_SIZE = 9; // 한 변에 9칸
    private static final Color[] PLAYER_COLORS = {
        new Color(231, 76, 60),   // Red
        new Color(52, 152, 219),  // Blue
        new Color(46, 204, 113),  // Green
        new Color(230, 126, 34)   // Orange
    };
    private static final int BASE_BOARD_SIZE = BASE_TILE_SIZE * BOARD_SIZE;

    private Board board;
    private List<Player> players;
    private Point2D.Double[] playerAnimationPositions;

    // 타일 클릭 리스너
    private Consumer<Integer> tileClickListener = null;
    private boolean tileClickEnabled = false;

    // 호버 효과
    private int hoveredTileIndex = -1;

    private double scaleFactor = 1.0;
    private int translateX = 0;
    private int translateY = 0;

    public BoardPanel(Board board, List<Player> players) {
        this.board = board;
        this.players = players;
        this.playerAnimationPositions = new Point2D.Double[players.size()];
        setPreferredSize(new Dimension(BASE_BOARD_SIZE, BASE_BOARD_SIZE));
        setBackground(new Color(44, 62, 80)); // 다크 네이비

        // 마우스 클릭 리스너 추가
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (tileClickEnabled && tileClickListener != null) {
                    int tileIndex = getTileIndexAt(e.getX(), e.getY());
                    if (tileIndex >= 0) {
                        tileClickListener.accept(tileIndex);
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // 마우스가 패널을 벗어나면 호버 효과 제거
                if (hoveredTileIndex != -1) {
                    hoveredTileIndex = -1;
                    repaint();
                }
            }
        });

        // 마우스 모션 리스너 추가 (호버 효과)
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (tileClickEnabled) {
                    int tileIndex = getTileIndexAt(e.getX(), e.getY());
                    if (tileIndex != hoveredTileIndex) {
                        hoveredTileIndex = tileIndex;
                        repaint();
                    }
                } else if (hoveredTileIndex != -1) {
                    hoveredTileIndex = -1;
                    repaint();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        updateTransform();

        g2d.translate(translateX, translateY);
        g2d.scale(scaleFactor, scaleFactor);

        drawBoard(g2d);
        drawPlayers(g2d);

        g2d.dispose();
    }

    private void updateTransform() {
        double boardPixels = BASE_BOARD_SIZE;
        if (boardPixels <= 0) {
            scaleFactor = 1.0;
            translateX = 0;
            translateY = 0;
            return;
        }

        double availableWidth = getWidth();
        double availableHeight = getHeight();

        if (availableWidth <= 0 || availableHeight <= 0) {
            scaleFactor = 1.0;
            translateX = 0;
            translateY = 0;
            return;
        }

        scaleFactor = Math.min(availableWidth / boardPixels, availableHeight / boardPixels);
        if (scaleFactor <= 0) {
            scaleFactor = 1.0;
        }

        double scaledWidth = boardPixels * scaleFactor;
        double scaledHeight = boardPixels * scaleFactor;

        translateX = (int) Math.round((availableWidth - scaledWidth) / 2.0);
        translateY = (int) Math.round((availableHeight - scaledHeight) / 2.0);
    }

    private void drawBoard(Graphics2D g) {
        // 32칸 보드: 9x9 그리드
        // 반시계 방향: Start(우하) → 무인도(좌하) → 올림픽(좌상) → 세계여행(우상) → Start

        // 하단 (우→좌): 0(Start), 1-7, 8(무인도) = 9칸
        for (int i = 0; i <= 8; i++) {
            int x = (8 - i) * BASE_TILE_SIZE;
            drawTile(g, i, x, 8 * BASE_TILE_SIZE);
        }

        // 좌측 (하→상): 9-15, 16(올림픽) = 8칸 (8번 무인도는 이미 그려짐)
        for (int i = 9; i <= 16; i++) {
            int y = (8 - (i - 8)) * BASE_TILE_SIZE;
            drawTile(g, i, 0, y);
        }

        // 상단 (좌→우): 17-23, 24(세계여행) = 8칸 (16번 올림픽은 이미 그려짐)
        for (int i = 17; i <= 24; i++) {
            int x = (i - 16) * BASE_TILE_SIZE;
            drawTile(g, i, x, 0);
        }

        // 우측 (상→하): 25-31 = 7칸 (24번 세계여행은 이미 그려짐, 0번 Start로 순환)
        for (int i = 25; i <= 31; i++) {
            int y = (i - 24) * BASE_TILE_SIZE;
            drawTile(g, i, 8 * BASE_TILE_SIZE, y);
        }
    }

    private void drawTile(Graphics2D g, int tileIndex, int x, int y) {
        Tile tile = board.getTile(tileIndex);
        int arc = 15; // 둥근 모서리 반경
        int padding = 2;
        int tileWidth = BASE_TILE_SIZE - 4;
        int tileHeight = BASE_TILE_SIZE - 4;

        // 1. 외부 그림자 (drop shadow)
        g.setColor(new Color(0, 0, 0, 50));
        g.fillRoundRect(x + padding + 2, y + padding + 2, tileWidth, tileHeight, arc, arc);

        // 2. 그라데이션 배경
        Color bgColor = getTileColor(tile);
        boolean isHovered = tileClickEnabled && tileIndex == hoveredTileIndex;
        Color color1 = isHovered ? bgColor.brighter() : bgColor;
        Color color2 = isHovered ? bgColor : bgColor.darker();

        GradientPaint gradient = new GradientPaint(
            x + padding, y + padding, color1,
            x + padding, y + padding + tileHeight, color2
        );
        g.setPaint(gradient);
        g.fillRoundRect(x + padding, y + padding, tileWidth, tileHeight, arc, arc);

        // 3. 내부 그림자 (상단, 깊이감)
        GradientPaint insetShadow = new GradientPaint(
            x + padding, y + padding, new Color(0, 0, 0, 40),
            x + padding, y + padding + 8, new Color(0, 0, 0, 0)
        );
        g.setPaint(insetShadow);
        g.fillRoundRect(x + padding, y + padding, tileWidth, 8, arc, arc);

        // 4. 광택 효과 (상단 30%)
        int glossHeight = tileHeight / 3;
        GradientPaint gloss = new GradientPaint(
            x + padding, y + padding, new Color(255, 255, 255, 40),
            x + padding, y + padding + glossHeight, new Color(255, 255, 255, 0)
        );
        g.setPaint(gloss);
        g.fillRoundRect(x + padding, y + padding, tileWidth, glossHeight, arc, arc);

        // 5. 호버 효과 강화
        if (isHovered) {
            g.setColor(new Color(255, 255, 255, 60));
            g.fillRoundRect(x + padding, y + padding, tileWidth, tileHeight, arc, arc);
        }

        // 6. 테두리
        boolean isLandmark = (tile instanceof City) && ((City) tile).isLandmark();
        if (isLandmark) {
            // 랜드마크 금색 빛나는 테두리
            drawLandmarkBorder(g, x + padding, y + padding, tileWidth, tileHeight, arc);
        } else {
            // 일반 테두리 + 하이라이트
            g.setStroke(new BasicStroke(2.5f));
            g.setColor(new Color(0, 0, 0, 150));
            g.drawRoundRect(x + padding, y + padding, tileWidth - 1, tileHeight - 1, arc, arc);

            // 내부 하이라이트
            g.setColor(new Color(255, 255, 255, 30));
            g.setStroke(new BasicStroke(1.0f));
            g.drawRoundRect(x + padding + 1, y + padding + 1, tileWidth - 3, tileHeight - 3, arc - 2, arc - 2);
        }

        // 도시인 경우 소유자 및 레벨 표시
        if (tile instanceof City) {
            City city = (City) tile;

            if (city.isOwned()) {
                // 개선된 소유자 배지 (둥근 사각형 + 그림자 + 광택)
                drawOwnerBadge(g, x + 6, y + 10, city.owner);

                // 건물 이모지 배경 + 이모지
                if (city.level > 0) {
                    int centerX = x + BASE_TILE_SIZE / 2;
                    int centerY = y + BASE_TILE_SIZE / 2;

                    // 반투명 원형 배경
                    g.setColor(new Color(255, 255, 255, 80));
                    g.fillOval(centerX - 20, centerY - 20, 40, 40);

                    // 배경 그라데이션
                    GradientPaint bgGradient = new GradientPaint(
                        centerX, centerY - 20, new Color(255, 255, 255, 100),
                        centerX, centerY + 20, new Color(255, 255, 255, 20)
                    );
                    g.setPaint(bgGradient);
                    g.fillOval(centerX - 18, centerY - 18, 36, 36);

                    // 건물 이모지
                    String buildingEmoji = city.getBuildingEmoji();
                    g.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
                    FontMetrics fm = g.getFontMetrics();
                    int emojiWidth = fm.stringWidth(buildingEmoji);

                    // 이모지 그림자
                    g.setColor(new Color(0, 0, 0, 60));
                    g.drawString(buildingEmoji, centerX - emojiWidth / 2 + 1, centerY + 8 + 1);

                    // 이모지
                    g.setColor(Color.BLACK);
                    g.drawString(buildingEmoji, centerX - emojiWidth / 2, centerY + 8);
                }

                // 올림픽 효과 표시 (개선된 디자인)
                if (city.hasOlympicBoost) {
                    // 배경 원
                    g.setColor(new Color(231, 76, 60, 200));
                    g.fillOval(x + BASE_TILE_SIZE - 32, y + 8, 24, 24);

                    // 테두리
                    g.setColor(new Color(192, 57, 43));
                    g.setStroke(new BasicStroke(2f));
                    g.drawOval(x + BASE_TILE_SIZE - 32, y + 8, 24, 24);

                    // ×2 텍스트
                    g.setFont(new Font("Arial", Font.BOLD, 12));
                    g.setColor(Color.WHITE);
                    g.drawString("×2", x + BASE_TILE_SIZE - 28, y + 23);
                }
            }
        } else if (tile instanceof TouristSpot) {
            // 관광지인 경우 소유자 표시
            TouristSpot touristSpot = (TouristSpot) tile;

            if (touristSpot.isOwned()) {
                // 개선된 소유자 배지
                drawOwnerBadge(g, x + 6, y + 10, touristSpot.owner);
            }
        }

        // 특수 타일 아이콘
        drawTileIcon(g, tile, x, y);

        // 타일 이름 (그림자 추가)
        Color textColor = Color.WHITE;
        Color shadowColor = new Color(0, 0, 0, 100);
        boolean isSpecialTile = tile.type == Tile.Type.ISLAND || tile.type == Tile.Type.OLYMPIC ||
                                tile.type == Tile.Type.WORLD_TOUR || tile.type == Tile.Type.CHANCE ||
                                tile.type == Tile.Type.TAX;

        g.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        String name = tile.name;
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(name);

        int textX = x + (BASE_TILE_SIZE - textWidth) / 2;
        int textY = isSpecialTile ? y + BASE_TILE_SIZE / 2 + 28 : y + BASE_TILE_SIZE - 20;

        // 텍스트 그림자
        g.setColor(shadowColor);
        g.drawString(name, textX + 1, textY + 1);

        // 텍스트
        g.setColor(textColor);
        g.drawString(name, textX, textY);

        // 타일 번호 (그림자 추가)
        g.setFont(new Font("Arial", Font.PLAIN, 9));
        g.setColor(new Color(0, 0, 0, 80));
        g.drawString(String.valueOf(tileIndex), x + 9, y + BASE_TILE_SIZE - 7);
        g.setColor(new Color(189, 195, 199));
        g.drawString(String.valueOf(tileIndex), x + 8, y + BASE_TILE_SIZE - 8);
    }

    private void drawLandmarkBorder(Graphics2D g, int x, int y, int width, int height, int arc) {
        // 랜드마크 타일에 대한 애니메이션 금색 테두리
        // 3겹의 레이어로 빛나는 효과 생성
        int padding = 2;

        // 외부 빛나는 효과 (가장 밝은 금색)
        g.setColor(new Color(241, 196, 15, 100));
        g.setStroke(new BasicStroke(3.0f));
        g.drawRoundRect(x + padding - 1, y + padding - 1, width + 2, height + 2, arc, arc);

        // 중간 레이어 (진한 금색)
        g.setColor(new Color(243, 156, 18, 180));
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(x + padding, y + padding, width, height, arc, arc);

        // 내부 하이라이트 (밝은 금색)
        g.setColor(new Color(255, 215, 0, 220));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(x + padding + 1, y + padding + 1, width - 2, height - 2, arc, arc);
    }

    private void drawOwnerBadge(Graphics2D g, int x, int y, int ownerIndex) {
        // 소유자 배지: 둥근 사각형 배지 + 그림자 + 광택
        int badgeWidth = 24;
        int badgeHeight = 20;
        int arc = 8;

        // 배지 그림자
        g.setColor(new Color(0, 0, 0, 80));
        g.fillRoundRect(x + 2, y + 2, badgeWidth, badgeHeight, arc, arc);

        // 배지 그라데이션 배경
        Color[] ownerColors = {
            new Color(231, 76, 60),   // 플레이어 1: 빨강
            new Color(52, 152, 219),  // 플레이어 2: 파랑
            new Color(46, 204, 113),  // 플레이어 3: 초록
            new Color(241, 196, 15)   // 플레이어 4: 금색
        };

        Color badgeColor = ownerColors[ownerIndex % ownerColors.length];
        GradientPaint badgeGradient = new GradientPaint(
            x, y, badgeColor.brighter(),
            x, y + badgeHeight, badgeColor.darker()
        );
        g.setPaint(badgeGradient);
        g.fillRoundRect(x, y, badgeWidth, badgeHeight, arc, arc);

        // 배지 광택 효과 (상단 50%)
        int glossHeight = badgeHeight / 2;
        GradientPaint gloss = new GradientPaint(
            x, y, new Color(255, 255, 255, 100),
            x, y + glossHeight, new Color(255, 255, 255, 0)
        );
        g.setPaint(gloss);
        g.fillRoundRect(x, y, badgeWidth, glossHeight, arc, arc);

        // 배지 테두리
        g.setColor(new Color(0, 0, 0, 150));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(x, y, badgeWidth, badgeHeight, arc, arc);

        // 플레이어 번호 텍스트
        g.setFont(new Font("Malgun Gothic", Font.BOLD, 12));
        String playerText = "P" + (ownerIndex + 1);
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(playerText);
        int textX = x + (badgeWidth - textWidth) / 2;
        int textY = y + badgeHeight / 2 + fm.getAscent() / 2 - 1;

        // 텍스트 그림자
        g.setColor(new Color(0, 0, 0, 150));
        g.drawString(playerText, textX + 1, textY + 1);

        // 텍스트
        g.setColor(Color.WHITE);
        g.drawString(playerText, textX, textY);
    }

    private void drawTileIcon(Graphics2D g, Tile tile, int x, int y) {
        g.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        String icon = "";

        switch (tile.type) {
            case START:
                icon = "🏁";
                break;
            case ISLAND:
                icon = "🏝";
                break;
            case CHANCE:
                icon = "🃏";
                break;
            case OLYMPIC:
                icon = "🏆";
                break;
            case WORLD_TOUR:
                icon = "✈️";
                break;
            case TAX:
                icon = "💵";
                break;
        }

        if (!icon.isEmpty()) {
            FontMetrics fm = g.getFontMetrics();
            int iconWidth = fm.stringWidth(icon);
            g.drawString(icon, x + (BASE_TILE_SIZE - iconWidth) / 2, y + BASE_TILE_SIZE/2 - 5);
        }
    }

    private Color getTileColor(Tile tile) {
        switch (tile.type) {
            case START:
                return new Color(26, 188, 156); // 청록색
            case ISLAND:
                return new Color(135, 206, 235); // 하늘색 배경
            case CHANCE:
                return new Color(128, 128, 128); // 회색 배경
            case OLYMPIC:
                return new Color(135, 206, 235); // 하늘색 배경
            case WORLD_TOUR:
                return new Color(135, 206, 235); // 하늘색 배경
            case TAX:
                return new Color(128, 128, 128); // 회색 배경
            case TOURIST_SPOT:
                // 관광지는 핑크-보라 그라데이션
                return new Color(255, 182, 193); // 연한 핑크
            case CITY:
                // 도시는 컬러 그룹 색상 사용
                if (tile instanceof City) {
                    City city = (City) tile;
                    return getColorGroupColor(city.colorGroup);
                }
                return new Color(52, 73, 94); // 기본 어두운 파랑
        }
        return new Color(52, 73, 94);
    }

    private Color getColorGroupColor(String colorGroup) {
        if (colorGroup == null) return Color.GRAY;

        switch (colorGroup) {
            case "LIME": return new Color(144, 238, 144); // 연두색
            case "GREEN": return new Color(34, 139, 34); // 초록색
            case "CYAN": return new Color(135, 206, 235); // 하늘색
            case "SKY_GRADIENT": return new Color(135, 206, 250); // 하늘색 그라데이션
            case "BLUE": return new Color(30, 144, 255); // 파란색
            case "LIGHT_PURPLE": return new Color(186, 152, 204); // 연보라색
            case "PURPLE": return new Color(138, 43, 226); // 보라색
            case "BROWN": return new Color(139, 69, 19); // 갈색
            case "RED": return new Color(220, 20, 60); // 빨간색
            case "PINK_GRADIENT": return new Color(255, 192, 203); // 핑크색 그라데이션
            default: return Color.GRAY;
        }
    }

    private void drawPlayers(Graphics2D g) {
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (player.bankrupt) continue;

            Point2D.Double animPos = (playerAnimationPositions != null && i < playerAnimationPositions.length)
                ? playerAnimationPositions[i]
                : null;

            double drawX;
            double drawY;

            if (animPos != null) {
                drawX = animPos.x;
                drawY = animPos.y;
            } else {
                Point2D.Double anchor = getPlayerAnchorForTile(player.pos, i);
                drawX = anchor.x;
                drawY = anchor.y;
            }

            drawPlayerIcon(g, (int)Math.round(drawX), (int)Math.round(drawY), PLAYER_COLORS[i], (char)('A' + i));
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

    private Point getPlayerOffset(int playerIndex) {
        int offsetX = (playerIndex % 2) * 20 + 5;
        int offsetY = (playerIndex / 2) * 25 + 35;
        return new Point(offsetX, offsetY);
    }

    private Point getTilePosition(int tileIndex) {
        // 32칸 보드 위치 계산 (9x9 그리드)
        // 하단 (우→좌): 0-8
        if (tileIndex <= 8) {
            int x = (8 - tileIndex) * BASE_TILE_SIZE;
            return new Point(x, 8 * BASE_TILE_SIZE);
        }
        // 좌측 (하→상): 9-16
        else if (tileIndex <= 16) {
            int y = (8 - (tileIndex - 8)) * BASE_TILE_SIZE;
            return new Point(0, y);
        }
        // 상단 (좌→우): 17-24
        else if (tileIndex <= 24) {
            int x = (tileIndex - 16) * BASE_TILE_SIZE;
            return new Point(x, 0);
        }
        // 우측 (상→하): 25-31
        else {
            int y = (tileIndex - 24) * BASE_TILE_SIZE;
            return new Point(8 * BASE_TILE_SIZE, y);
        }
    }

    public void updateBoard() {
        repaint();
    }

    /**
     * 특정 플레이어의 애니메이션 위치 설정
     */
    public void setPlayerAnimationPosition(int playerIndex, double x, double y) {
        if (playerIndex < 0 || playerIndex >= playerAnimationPositions.length) return;
        playerAnimationPositions[playerIndex] = new Point2D.Double(x, y);
        repaint();
    }

    /**
     * 특정 플레이어의 애니메이션 해제
     */
    public void clearPlayerAnimation(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= playerAnimationPositions.length) return;
        playerAnimationPositions[playerIndex] = null;
        repaint();
    }

    /**
     * 타일 기준 플레이어 아이콘 기준 좌표 반환
     */
    public Point2D.Double getPlayerAnchorForTile(int tileIndex, int playerIndex) {
        Point tilePos = getTilePosition(tileIndex);
        Point offset = getPlayerOffset(playerIndex);
        return new Point2D.Double(tilePos.x + offset.x, tilePos.y + offset.y);
    }

    /**
     * 현재 스케일 팩터 반환
     */
    public double getScaleFactor() {
        return scaleFactor;
    }

    /**
     * 타일 클릭 리스너 설정
     */
    public void setTileClickListener(Consumer<Integer> listener) {
        this.tileClickListener = listener;
    }

    /**
     * 타일 클릭 활성화/비활성화
     */
    public void setTileClickEnabled(boolean enabled) {
        this.tileClickEnabled = enabled;
        if (enabled) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
            // 타일 클릭 비활성화 시 호버 효과 제거
            hoveredTileIndex = -1;
        }
        repaint();
    }

    /**
     * 마우스 좌표로부터 타일 인덱스 계산
     * @return 타일 인덱스 (0-31), 타일이 아닌 영역을 클릭하면 -1 반환
     */
    private int getTileIndexAt(int mouseX, int mouseY) {
        // 중앙 영역 클릭은 무시
        int centerX = BASE_TILE_SIZE * 1;
        int centerY = BASE_TILE_SIZE * 1;
        int centerW = BASE_TILE_SIZE * 7;
        int centerH = BASE_TILE_SIZE * 7;
        updateTransform();

        double boardX = (mouseX - translateX) / scaleFactor;
        double boardY = (mouseY - translateY) / scaleFactor;

        if (boardX < 0 || boardY < 0 ||
            boardX >= BASE_BOARD_SIZE || boardY >= BASE_BOARD_SIZE) {
            return -1;
        }

        if (boardX >= centerX && boardX < centerX + centerW &&
            boardY >= centerY && boardY < centerY + centerH) {
            return -1;
        }

        // 44개 타일의 위치를 확인
        for (int i = 0; i < board.getSize(); i++) {
            Point pos = getTilePosition(i);
            if (boardX >= pos.x && boardX < pos.x + BASE_TILE_SIZE &&
                boardY >= pos.y && boardY < pos.y + BASE_TILE_SIZE) {
                return i;
            }
        }

        return -1;
    }
}
