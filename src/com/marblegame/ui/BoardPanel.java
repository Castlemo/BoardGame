package com.marblegame.ui;

import com.marblegame.model.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

    // 알림 시스템
    private String notificationMessage = null;
    private String notificationSubtext = null;
    private Color notificationColor = Color.WHITE;
    private float notificationAlpha = 0.0f;
    private Timer fadeTimer = null;

    // 타일 클릭 리스너
    private Consumer<Integer> tileClickListener = null;
    private boolean tileClickEnabled = false;

    private double scaleFactor = 1.0;
    private int translateX = 0;
    private int translateY = 0;

    public BoardPanel(Board board, List<Player> players) {
        this.board = board;
        this.players = players;
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

        // 중앙 로고
        drawCenterLogo(g);
    }

    private void drawTile(Graphics2D g, int tileIndex, int x, int y) {
        Tile tile = board.getTile(tileIndex);

        // 타일 배경
        Color bgColor = getTileColor(tile);
        g.setColor(bgColor);
        g.fillRoundRect(x + 2, y + 2, BASE_TILE_SIZE - 4, BASE_TILE_SIZE - 4, 10, 10);

        // 타일 테두리 (랜드마크는 금색)
        boolean isLandmark = (tile instanceof City) && ((City) tile).isLandmark();
        if (isLandmark) {
            g.setColor(new Color(255, 215, 0)); // 금색 테두리
            g.setStroke(new BasicStroke(4)); // 더 두꺼운 테두리
        } else {
            g.setColor(new Color(236, 240, 241));
            g.setStroke(new BasicStroke(2));
        }
        g.drawRoundRect(x + 2, y + 2, BASE_TILE_SIZE - 4, BASE_TILE_SIZE - 4, 10, 10);

        // 도시인 경우 소유자 및 레벨 표시
        if (tile instanceof City) {
            City city = (City) tile;

            // 삭제된 칸인 경우 X 표시
            if (city.isDeleted) {
                // 반투명한 배경
                g.setColor(new Color(44, 62, 80, 200));
                g.fillRoundRect(x + 2, y + 2, BASE_TILE_SIZE - 4, BASE_TILE_SIZE - 4, 10, 10);

                // 큰 빨간 X 표시
                g.setColor(new Color(192, 57, 43));
                g.setStroke(new BasicStroke(6));
                g.drawLine(x + 12, y + 12, x + BASE_TILE_SIZE - 12, y + BASE_TILE_SIZE - 12);
                g.drawLine(x + 12, y + BASE_TILE_SIZE - 12, x + BASE_TILE_SIZE - 12, y + 12);

                // 삭제됨 텍스트
                g.setColor(new Color(236, 240, 241));
                g.setFont(new Font("맑은 고딕", Font.BOLD, 9));
                String deletedText = "삭제됨";
                FontMetrics fm = g.getFontMetrics();
                int textWidth = fm.stringWidth(deletedText);
                g.drawString(deletedText, x + (BASE_TILE_SIZE - textWidth) / 2, y + BASE_TILE_SIZE / 2 + 15);
                return; // 더 이상 그리지 않음
            }

            if (city.isOwned()) {
                // 소유자 표시 (좌측 상단 원)
                g.setColor(PLAYER_COLORS[city.owner]);
                g.fillOval(x + 6, y + 16, 16, 16);

                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 10));
                g.drawString(String.valueOf((char)('A' + city.owner)), x + 11, y + 27);

                // 건물 이모지 표시 (도시 이름 위에)
                if (city.level > 0) {
                    String buildingEmoji = city.getBuildingEmoji();
                    g.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
                    FontMetrics fm = g.getFontMetrics();
                    int emojiWidth = fm.stringWidth(buildingEmoji);
                    g.drawString(buildingEmoji, x + (BASE_TILE_SIZE - emojiWidth) / 2, y + BASE_TILE_SIZE / 2);
                }

                // 올림픽 효과 표시 (2배 아이콘)
                if (city.hasOlympicBoost) {
                    g.setFont(new Font("Arial", Font.BOLD, 14));
                    g.setColor(new Color(231, 76, 60)); // 빨간색
                    g.drawString("×2", x + BASE_TILE_SIZE - 28, y + 16);
                }
            }
        } else if (tile instanceof TouristSpot) {
            // 관광지인 경우 소유자 표시
            TouristSpot touristSpot = (TouristSpot) tile;

            // 삭제된 칸인 경우 X 표시
            if (touristSpot.isDeleted) {
                g.setColor(new Color(44, 62, 80, 200));
                g.fillRoundRect(x + 2, y + 2, BASE_TILE_SIZE - 4, BASE_TILE_SIZE - 4, 10, 10);

                g.setColor(new Color(192, 57, 43));
                g.setStroke(new BasicStroke(6));
                g.drawLine(x + 12, y + 12, x + BASE_TILE_SIZE - 12, y + BASE_TILE_SIZE - 12);
                g.drawLine(x + 12, y + BASE_TILE_SIZE - 12, x + BASE_TILE_SIZE - 12, y + 12);

                g.setColor(new Color(236, 240, 241));
                g.setFont(new Font("맑은 고딕", Font.BOLD, 9));
                String deletedText = "삭제됨";
                FontMetrics fm = g.getFontMetrics();
                int textWidth = fm.stringWidth(deletedText);
                g.drawString(deletedText, x + (BASE_TILE_SIZE - textWidth) / 2, y + BASE_TILE_SIZE / 2 + 15);
                return;
            }

            if (touristSpot.isOwned()) {
                // 소유자 표시 (좌측 상단 원)
                g.setColor(PLAYER_COLORS[touristSpot.owner]);
                g.fillOval(x + 6, y + 16, 16, 16);

                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 10));
                g.drawString(String.valueOf((char)('A' + touristSpot.owner)), x + 11, y + 27);
            }
        }

        // 특수 타일 아이콘
        drawTileIcon(g, tile, x, y);

        // 타일 이름
        Color textColor = Color.WHITE;
        if (tile.type == Tile.Type.ISLAND || tile.type == Tile.Type.OLYMPIC ||
            tile.type == Tile.Type.WORLD_TOUR || tile.type == Tile.Type.CHANCE ||
            tile.type == Tile.Type.TAX) {
            textColor = Color.BLACK; // 특수 타일은 검은색 텍스트
        }
        g.setColor(textColor);
        g.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        String name = tile.name;
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(name);

        // 특수 타일은 이모지 아래에 텍스트 배치
        if (tile.type == Tile.Type.ISLAND || tile.type == Tile.Type.OLYMPIC ||
            tile.type == Tile.Type.WORLD_TOUR || tile.type == Tile.Type.CHANCE ||
            tile.type == Tile.Type.TAX) {
            g.drawString(name, x + (BASE_TILE_SIZE - textWidth) / 2, y + BASE_TILE_SIZE / 2 + 28);
        } else {
            g.drawString(name, x + (BASE_TILE_SIZE - textWidth) / 2, y + BASE_TILE_SIZE - 20);
        }

        // 타일 번호
        g.setColor(new Color(189, 195, 199));
        g.setFont(new Font("Arial", Font.PLAIN, 9));
        g.drawString(String.valueOf(tileIndex), x + 8, y + BASE_TILE_SIZE - 8);
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
                icon = "🎁";
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

    private void drawCenterLogo(Graphics2D g) {
        int centerX = BASE_TILE_SIZE * 1;
        int centerY = BASE_TILE_SIZE * 1;
        int centerW = BASE_TILE_SIZE * 7;
        int centerH = BASE_TILE_SIZE * 7;

        // 배경
        g.setColor(new Color(236, 240, 241));
        g.fillRoundRect(centerX, centerY, centerW, centerH, 20, 20);

        // 알림이 있으면 알림 표시
        if (notificationMessage != null && notificationAlpha > 0) {
            drawNotification(g, centerX, centerY, centerW, centerH);
        } else {
            // 기본 로고 표시
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
            String title1 = "한성대";
            FontMetrics fm1 = g.getFontMetrics();
            int x1 = centerX + (centerW - fm1.stringWidth(title1)) / 2;
            g.drawString(title1, x1, centerY + centerH / 2 - 20);

            g.setFont(new Font("맑은 고딕", Font.BOLD, 40));
            String title2 = "객지2";
            FontMetrics fm2 = g.getFontMetrics();
            int x2 = centerX + (centerW - fm2.stringWidth(title2)) / 2;
            g.drawString(title2, x2, centerY + centerH / 2 + 40);

            // 버전
            g.setFont(new Font("Arial", Font.PLAIN, 14));
            g.setColor(new Color(236, 240, 241));
            g.drawString("v2.0", centerX + centerW - 50, centerY + centerH - 20);
        }
    }

    private void drawNotification(Graphics2D g, int centerX, int centerY, int centerW, int centerH) {
        // 투명도가 적용된 그라데이션 배경
        Composite originalComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, notificationAlpha));

        GradientPaint gradient = new GradientPaint(
            centerX, centerY, new Color(44, 62, 80),
            centerX, centerY + centerH, new Color(52, 73, 94)
        );
        g.setPaint(gradient);
        g.fillRoundRect(centerX + 15, centerY + 15, centerW - 30, centerH - 30, 15, 15);

        // 메인 메시지
        g.setColor(notificationColor);
        g.setFont(new Font("맑은 고딕", Font.BOLD, 56));
        FontMetrics fm1 = g.getFontMetrics();
        int msgWidth = fm1.stringWidth(notificationMessage);
        int msgX = centerX + (centerW - msgWidth) / 2;
        int msgY = centerY + centerH / 2;

        // 서브텍스트가 있으면 메인 메시지를 위로 올림
        if (notificationSubtext != null) {
            msgY -= 30;
        }

        g.drawString(notificationMessage, msgX, msgY);

        // 서브텍스트
        if (notificationSubtext != null) {
            g.setFont(new Font("맑은 고딕", Font.BOLD, 40));
            FontMetrics fm2 = g.getFontMetrics();
            int subWidth = fm2.stringWidth(notificationSubtext);
            int subX = centerX + (centerW - subWidth) / 2;
            int subY = msgY + 60;
            g.drawString(notificationSubtext, subX, subY);
        }

        g.setComposite(originalComposite);
    }

    public void updateBoard() {
        repaint();
    }

    /**
     * 중앙 영역에 알림 표시 (페이드 인/아웃 효과)
     * @param message 메인 메시지
     * @param subtext 서브텍스트 (null 가능)
     * @param color 메시지 색상
     */
    public void showNotification(String message, String subtext, Color color) {
        // 기존 타이머가 있으면 중지
        if (fadeTimer != null && fadeTimer.isRunning()) {
            fadeTimer.stop();
        }

        this.notificationMessage = message;
        this.notificationSubtext = subtext;
        this.notificationColor = color;
        this.notificationAlpha = 0.0f;

        // 페이드 효과 타이머
        final int FADE_IN_DURATION = 200;  // 0.2초
        final int DISPLAY_DURATION = 800; // 0.8초 유지
        final int FADE_OUT_DURATION = 200; // 0.2초
        final int TOTAL_DURATION = FADE_IN_DURATION + DISPLAY_DURATION + FADE_OUT_DURATION;

        final long startTime = System.currentTimeMillis();

        fadeTimer = new Timer(16, new ActionListener() { // ~60 FPS
            @Override
            public void actionPerformed(ActionEvent e) {
                long elapsed = System.currentTimeMillis() - startTime;

                if (elapsed < FADE_IN_DURATION) {
                    // 페이드 인
                    notificationAlpha = (float) elapsed / FADE_IN_DURATION;
                } else if (elapsed < FADE_IN_DURATION + DISPLAY_DURATION) {
                    // 유지
                    notificationAlpha = 1.0f;
                } else if (elapsed < TOTAL_DURATION) {
                    // 페이드 아웃
                    float fadeOutProgress = (float) (elapsed - FADE_IN_DURATION - DISPLAY_DURATION) / FADE_OUT_DURATION;
                    notificationAlpha = 1.0f - fadeOutProgress;
                } else {
                    // 종료
                    notificationAlpha = 0.0f;
                    notificationMessage = null;
                    notificationSubtext = null;
                    fadeTimer.stop();
                }

                repaint();
            }
        });
        fadeTimer.start();
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
