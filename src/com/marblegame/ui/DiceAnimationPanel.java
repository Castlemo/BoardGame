package com.marblegame.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 주사위 애니메이션 패널
 * 회전 애니메이션과 함께 최종 결과를 시각적으로 표시
 */
public class DiceAnimationPanel extends JPanel {
    private int dice1 = 1;
    private int dice2 = 1;
    private boolean animating = false;
    private Timer animationTimer;
    private long animationStartTime;
    private int finalDice1;
    private int finalDice2;

    // 애니메이션 파라미터
    private static final long ANIMATION_DURATION = 1400; // 1.4초
    private static final int FRAME_INTERVAL = 60; // 60ms

    // 주사위 색상
    private static final Color DICE_BG = new Color(236, 240, 241);
    private static final Color DICE_DOT = new Color(44, 62, 80);
    private static final Color DICE_BORDER = new Color(189, 195, 199);

    public DiceAnimationPanel() {
        setPreferredSize(new Dimension(150, 90));
        setBackground(new Color(44, 62, 80));
        setOpaque(false);
    }

    /**
     * 주사위 애니메이션 시작
     */
    public void startAnimation(int finalValue1, int finalValue2, Runnable onComplete) {
        this.finalDice1 = finalValue1;
        this.finalDice2 = finalValue2;
        this.animating = true;
        this.animationStartTime = System.currentTimeMillis();

        if (animationTimer != null) {
            animationTimer.stop();
        }

        animationTimer = new Timer(FRAME_INTERVAL, e -> {
            long elapsed = System.currentTimeMillis() - animationStartTime;
            double progress = Math.min(1.0, (double)elapsed / ANIMATION_DURATION);

            if (progress >= 1.0) {
                // 애니메이션 종료
                dice1 = finalDice1;
                dice2 = finalDice2;
                animating = false;
                animationTimer.stop();
                repaint();

                // 완료 콜백
                if (onComplete != null) {
                    SwingUtilities.invokeLater(onComplete);
                }
            } else {
                // 랜덤 주사위 값 (cubic-out easing)
                double easedProgress = cubicOut(progress);

                // 진행도가 높을수록 프레임 변경 빈도 감소
                if (Math.random() > easedProgress * 0.7) {
                    dice1 = 1 + (int)(Math.random() * 6);
                    dice2 = 1 + (int)(Math.random() * 6);
                }

                repaint();
            }
        });

        animationTimer.start();
    }

    /**
     * Cubic-out easing
     */
    private double cubicOut(double t) {
        double f = t - 1;
        return f * f * f + 1;
    }

    /**
     * 현재 애니메이션 중인지 확인
     */
    public boolean isAnimating() {
        return animating;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // 주사위 크기 및 위치
        int diceSize = 60;
        int spacing = 10;
        int totalWidth = diceSize * 2 + spacing;
        int startX = (width - totalWidth) / 2;
        int y = (height - diceSize) / 2;

        // 첫 번째 주사위
        drawDice(g2d, startX, y, diceSize, dice1);

        // 두 번째 주사위
        drawDice(g2d, startX + diceSize + spacing, y, diceSize, dice2);

        // 애니메이션 중이면 회전 효과 표시
        if (animating) {
            g2d.setColor(new Color(52, 152, 219, 100));
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRoundRect(startX - 5, y - 5, diceSize + 10, diceSize + 10, 10, 10);
            g2d.drawRoundRect(startX + diceSize + spacing - 5, y - 5, diceSize + 10, diceSize + 10, 10, 10);
        }
    }

    /**
     * 주사위 그리기
     */
    private void drawDice(Graphics2D g, int x, int y, int size, int value) {
        // 배경
        g.setColor(DICE_BG);
        g.fillRoundRect(x, y, size, size, 10, 10);

        // 테두리
        g.setColor(DICE_BORDER);
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(x, y, size, size, 10, 10);

        // 점 그리기
        g.setColor(DICE_DOT);
        int dotSize = 8;
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        int offset = size / 4;

        switch (value) {
            case 1:
                fillDot(g, centerX, centerY, dotSize);
                break;
            case 2:
                fillDot(g, centerX - offset, centerY - offset, dotSize);
                fillDot(g, centerX + offset, centerY + offset, dotSize);
                break;
            case 3:
                fillDot(g, centerX - offset, centerY - offset, dotSize);
                fillDot(g, centerX, centerY, dotSize);
                fillDot(g, centerX + offset, centerY + offset, dotSize);
                break;
            case 4:
                fillDot(g, centerX - offset, centerY - offset, dotSize);
                fillDot(g, centerX + offset, centerY - offset, dotSize);
                fillDot(g, centerX - offset, centerY + offset, dotSize);
                fillDot(g, centerX + offset, centerY + offset, dotSize);
                break;
            case 5:
                fillDot(g, centerX - offset, centerY - offset, dotSize);
                fillDot(g, centerX + offset, centerY - offset, dotSize);
                fillDot(g, centerX, centerY, dotSize);
                fillDot(g, centerX - offset, centerY + offset, dotSize);
                fillDot(g, centerX + offset, centerY + offset, dotSize);
                break;
            case 6:
                fillDot(g, centerX - offset, centerY - offset, dotSize);
                fillDot(g, centerX + offset, centerY - offset, dotSize);
                fillDot(g, centerX - offset, centerY, dotSize);
                fillDot(g, centerX + offset, centerY, dotSize);
                fillDot(g, centerX - offset, centerY + offset, dotSize);
                fillDot(g, centerX + offset, centerY + offset, dotSize);
                break;
        }
    }

    /**
     * 점 그리기 (중심 좌표 기준)
     */
    private void fillDot(Graphics2D g, int cx, int cy, int size) {
        g.fillOval(cx - size / 2, cy - size / 2, size, size);
    }

    /**
     * 주사위 값 리셋
     */
    public void reset() {
        dice1 = 1;
        dice2 = 1;
        animating = false;
        if (animationTimer != null) {
            animationTimer.stop();
        }
        repaint();
    }
}
