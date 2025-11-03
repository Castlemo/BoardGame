package com.marblegame.ui;

import com.marblegame.model.DiceGauge;
import javax.swing.*;
import java.awt.*;

/**
 * 주사위 게이지 시각화 패널
 * 3구간을 색상으로 구분하고 현재 위치를 표시
 */
public class GaugePanel extends JPanel {
    private DiceGauge gauge;
    private Timer animationTimer;

    // 구간 색상
    private static final Color SECTION1_COLOR = new Color(46, 204, 113); // 초록 (2~3)
    private static final Color SECTION2_COLOR = new Color(52, 152, 219); // 파랑 (3~5)
    private static final Color SECTION3_COLOR = new Color(241, 196, 15); // 노랑 (5~6)
    private static final Color INDICATOR_COLOR = new Color(231, 76, 60); // 빨강 (인디케이터)

    public GaugePanel(DiceGauge gauge) {
        this.gauge = gauge;
        setPreferredSize(new Dimension(300, 60));
        setMinimumSize(new Dimension(120, 40));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        setBackground(new Color(44, 62, 80));

        // 30fps 애니메이션 타이머
        animationTimer = new Timer(33, e -> repaint());
    }

    /**
     * 게이지 애니메이션 시작
     */
    public void startAnimation() {
        animationTimer.start();
    }

    /**
     * 게이지 애니메이션 정지
     */
    public void stopAnimation() {
        animationTimer.stop();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int gaugeHeight = Math.max(18, (int) Math.round(height * 0.5));
        int gaugeY = (height - gaugeHeight) / 2;

        // 3구간 배경 그리기
        drawSections(g2d, 0, gaugeY, width, gaugeHeight);

        // 구간 경계선
        drawBoundaries(g2d, 0, gaugeY, width, gaugeHeight);

        // 현재 위치 인디케이터
        drawIndicator(g2d, 0, gaugeY, width, gaugeHeight);

        // 구간 라벨
        drawLabels(g2d, 0, gaugeY, width, gaugeHeight);
    }

    /**
     * 3구간 배경 그리기
     */
    private void drawSections(Graphics2D g, int x, int y, int width, int height) {
        int section1Width = (int)(width * 0.333);
        int section2Width = (int)(width * 0.333);
        int section3Width = width - section1Width - section2Width;

        // S1 (초록)
        int corner = Math.max(8, height / 2);

        g.setColor(SECTION1_COLOR);
        g.fillRoundRect(x, y, section1Width, height, corner, corner);

        // S2 (파랑)
        g.setColor(SECTION2_COLOR);
        g.fillRect(x + section1Width, y, section2Width, height);

        // S3 (노랑)
        g.setColor(SECTION3_COLOR);
        g.fillRoundRect(x + section1Width + section2Width, y, section3Width, height, corner, corner);
    }

    /**
     * 구간 경계선 그리기
     */
    private void drawBoundaries(Graphics2D g, int x, int y, int width, int height) {
        g.setColor(new Color(236, 240, 241));
        float boundaryStroke = Math.max(1f, height / 20f);
        g.setStroke(new BasicStroke(boundaryStroke));

        int boundary1X = (int)(width * 0.333);
        int boundary2X = (int)(width * 0.666);

        g.drawLine(x + boundary1X, y, x + boundary1X, y + height);
        g.drawLine(x + boundary2X, y, x + boundary2X, y + height);

        // 외곽선
        int corner = Math.max(8, height / 2);
        g.drawRoundRect(x, y, width, height, corner, corner);
    }

    /**
     * 현재 위치 인디케이터 그리기
     */
    private void drawIndicator(Graphics2D g, int x, int y, int width, int height) {
        double position = gauge.getCurrentPosition();
        int indicatorX = x + (int)(position * width);

        g.setColor(INDICATOR_COLOR);
        float indicatorStroke = Math.max(2f, height / 12f);
        g.setStroke(new BasicStroke(indicatorStroke));

        int padding = Math.max(4, (int) Math.round(height * 0.2));
        g.drawLine(indicatorX, y - padding, indicatorX, y + height + padding);

        int triangleWidth = Math.max(6, (int) Math.round(height * 0.3));
        int triangleHeight = Math.max(6, (int) Math.round(height * 0.3));

        int[] triangleX = {indicatorX - triangleWidth, indicatorX + triangleWidth, indicatorX};
        int[] triangleY = {y - padding, y - padding, y - padding - triangleHeight};
        g.fillPolygon(triangleX, triangleY, 3);

        int[] triangleX2 = {indicatorX - triangleWidth, indicatorX + triangleWidth, indicatorX};
        int[] triangleY2 = {y + height + padding, y + height + padding, y + height + padding + triangleHeight};
        g.fillPolygon(triangleX2, triangleY2, 3);
    }

    /**
     * 구간 라벨 그리기
     */
    private void drawLabels(Graphics2D g, int x, int y, int width, int height) {
        g.setColor(Color.WHITE);
        double scale = height / 60.0;
        int baseFontSize = 12;
        g.setFont(new Font("맑은 고딕", Font.BOLD, Math.max(10, (int) Math.round(baseFontSize * scale))));

        int section1Center = (int)(width * 0.166);
        int section2Center = (int)(width * 0.5);
        int section3Center = (int)(width * 0.833);

        FontMetrics fm = g.getFontMetrics();

        // S1 라벨
        String label1 = "2~5";
        int label1Width = fm.stringWidth(label1);
        g.drawString(label1, x + section1Center - label1Width / 2, y + height / 2 + 5);

        // S2 라벨
        String label2 = "6~9";
        int label2Width = fm.stringWidth(label2);
        g.drawString(label2, x + section2Center - label2Width / 2, y + height / 2 + 5);

        // S3 라벨
        String label3 = "10~12";
        int label3Width = fm.stringWidth(label3);
        g.drawString(label3, x + section3Center - label3Width / 2, y + height / 2 + 5);
    }
}
