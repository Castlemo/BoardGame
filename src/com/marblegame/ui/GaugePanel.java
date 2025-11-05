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

    // 연료 게이지 색상
    private static final Color YELLOW_COLOR = new Color(255, 235, 59);   // 노란색 (0~33%)
    private static final Color ORANGE_COLOR = new Color(255, 152, 0);    // 주황색 (33~67%)
    private static final Color RED_COLOR = new Color(244, 67, 54);       // 빨간색 (67~100%)
    private static final Color EMPTY_GAUGE_COLOR = new Color(60, 60, 60); // 빈 부분 배경

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

        // 연료 게이지 스타일로 그리기
        drawFuelGauge(g2d, 0, gaugeY, width, gaugeHeight);
    }

    /**
     * 연료 게이지 스타일로 채워지는 바 그리기
     */
    private void drawFuelGauge(Graphics2D g, int x, int y, int width, int height) {
        double position = gauge.getCurrentPosition();
        int filledWidth = (int)(position * width);
        int corner = Math.max(8, height / 2);

        // 빈 부분 먼저 그리기 (전체 배경)
        g.setColor(EMPTY_GAUGE_COLOR);
        g.fillRoundRect(x, y, width, height, corner, corner);

        // 채워진 부분 그리기 (위치에 따라 색상 변경)
        if (filledWidth > 0) {
            Color fillColor;
            if (position < 0.333) {
                fillColor = YELLOW_COLOR;  // 0~33%: 노란색
            } else if (position < 0.666) {
                fillColor = ORANGE_COLOR;  // 33~67%: 주황색
            } else {
                fillColor = RED_COLOR;     // 67~100%: 빨간색
            }

            g.setColor(fillColor);
            g.fillRoundRect(x, y, filledWidth, height, corner, corner);
        }

        // 외곽 테두리
        g.setColor(new Color(200, 200, 200));
        float borderStroke = Math.max(1.5f, height / 15f);
        g.setStroke(new BasicStroke(borderStroke));
        g.drawRoundRect(x, y, width, height, corner, corner);
    }

}
