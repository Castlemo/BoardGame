package com.marblegame.ui;

import com.marblegame.model.DiceGauge;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 주사위 애니메이션 + 게이지를 통합한 행동 패널
 * 플레이어 정보 패널 하단에 고정 배치
 */
public class ActionPanel extends JPanel {
    private static final int BASE_WIDTH = 300;
    private static final int BASE_HEIGHT = 280;
    private static final int DICE_PANEL_BASE_WIDTH = 150;
    private static final int DICE_PANEL_BASE_HEIGHT = 90;
    private static final int GAUGE_PANEL_BASE_WIDTH = 300;
    private static final int GAUGE_PANEL_BASE_HEIGHT = 60;

    private DiceGauge diceGauge;
    private GaugePanel gaugePanel;
    private DiceAnimationPanel diceAnimationPanel;
    private JPanel container;
    private JLabel titleLabel;
    private JSeparator separator;
    private final java.util.List<JComponent> spacers = new java.util.ArrayList<>();

    private Font baseTitleFont;
    private double scaleFactor = 1.0;

    public ActionPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(new Color(44, 62, 80));
        setBorder(new EmptyBorder(8, 8, 8, 8));

        // 게이지 초기화
        diceGauge = new DiceGauge();

        initComponents();

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                applyScale();
            }
        });

        applyScale();
    }

    private void initComponents() {
        // 메인 컨테이너 - 그라데이션 배경과 라운드 모서리
        container = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 그라데이션 배경
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(52, 73, 94),
                    0, getHeight(), new Color(44, 62, 80)
                );
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

                // 외곽선
                g2d.setColor(new Color(149, 165, 166));
                float stroke = (float) Math.max(1f, 2f * (float) scaleFactor);
                g2d.setStroke(new BasicStroke(stroke));
                g2d.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 15, 15);
            }
        };
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        container.setBorder(new EmptyBorder(12, 12, 12, 12));

        // 제목 - 더 큰 폰트와 아이콘
        titleLabel = new JLabel("🎲 주사위 컨트롤");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        titleLabel.setForeground(new Color(236, 240, 241));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        container.add(titleLabel);

        // 구분선
        separator = new JSeparator();
        separator.setForeground(new Color(149, 165, 166, 100));
        separator.setMaximumSize(new Dimension(260, 1));
        addSpacer(container, 8);
        container.add(separator);
        addSpacer(container, 12);

        // 주사위 애니메이션 패널
        diceAnimationPanel = new DiceAnimationPanel();
        JPanel diceContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        diceContainer.setOpaque(false);
        diceContainer.add(diceAnimationPanel);
        container.add(diceContainer);

        addSpacer(container, 15);

        // 게이지 패널
        gaugePanel = new GaugePanel(diceGauge);
        JPanel gaugeContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        gaugeContainer.setOpaque(false);
        gaugeContainer.add(gaugePanel);
        container.add(gaugeContainer);

        addSpacer(container, 10);

        add(container, BorderLayout.CENTER);

        baseTitleFont = titleLabel.getFont();
    }

    private void addSpacer(JPanel parent, int baseHeight) {
        JComponent spacer = (JComponent) Box.createVerticalStrut(baseHeight);
        spacer.putClientProperty("baseSpacing", baseHeight);
        spacers.add(spacer);
        parent.add(spacer);
    }

    /**
     * 주사위 게이지 접근자
     */
    public DiceGauge getDiceGauge() {
        return diceGauge;
    }

    /**
     * 게이지 애니메이션 시작
     */
    public void startGaugeAnimation() {
        gaugePanel.startAnimation();
    }

    /**
     * 게이지 애니메이션 정지
     */
    public void stopGaugeAnimation() {
        gaugePanel.stopAnimation();
    }

    /**
     * 주사위 애니메이션 패널 접근자
     */
    public DiceAnimationPanel getDiceAnimationPanel() {
        return diceAnimationPanel;
    }

    private void applyScale() {
        double width = Math.max(1, getWidth());
        double height = Math.max(1, getHeight());
        double scaleX = width / BASE_WIDTH;
        double scaleY = height / BASE_HEIGHT;
        scaleFactor = Math.max(0.6, Math.min(scaleX, scaleY));

        setBorder(new EmptyBorder(
            Math.max(6, (int) Math.round(8 * scaleFactor)),
            Math.max(6, (int) Math.round(8 * scaleFactor)),
            Math.max(6, (int) Math.round(8 * scaleFactor)),
            Math.max(6, (int) Math.round(8 * scaleFactor))
        ));

        if (baseTitleFont != null) {
            float titleSize = (float) Math.max(12f, baseTitleFont.getSize2D() * scaleFactor);
            titleLabel.setFont(baseTitleFont.deriveFont(titleSize));
        }

        separator.setMaximumSize(new Dimension(
            Math.max(120, (int) Math.round(260 * scaleFactor)),
            Math.max(1, (int) Math.round(scaleFactor))
        ));

        container.setBorder(new EmptyBorder(
            Math.max(8, (int) Math.round(12 * scaleFactor)),
            Math.max(8, (int) Math.round(12 * scaleFactor)),
            Math.max(8, (int) Math.round(12 * scaleFactor)),
            Math.max(8, (int) Math.round(12 * scaleFactor))
        ));

        for (JComponent spacer : spacers) {
            Object base = spacer.getClientProperty("baseSpacing");
            if (base instanceof Integer) {
                int scaled = Math.max(4, (int) Math.round((Integer) base * scaleFactor));
                spacer.setPreferredSize(new Dimension(0, scaled));
                spacer.setMinimumSize(new Dimension(0, scaled));
            }
        }

        int diceWidth = Math.max(120, (int) Math.round(DICE_PANEL_BASE_WIDTH * scaleFactor));
        int diceHeight = Math.max(70, (int) Math.round(DICE_PANEL_BASE_HEIGHT * scaleFactor));
        diceAnimationPanel.setPreferredSize(new Dimension(diceWidth, diceHeight));
        diceAnimationPanel.setMinimumSize(new Dimension(80, 60));

        int gaugeWidth = Math.max(160, (int) Math.round(GAUGE_PANEL_BASE_WIDTH * scaleFactor));
        int gaugeHeight = Math.max(40, (int) Math.round(GAUGE_PANEL_BASE_HEIGHT * scaleFactor));
        gaugePanel.setPreferredSize(new Dimension(gaugeWidth, gaugeHeight));
        gaugePanel.setMinimumSize(new Dimension(120, 40));

        revalidate();
        repaint();
    }
}
