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
    private DiceGauge diceGauge;
    private GaugePanel gaugePanel;
    private DiceAnimationPanel diceAnimationPanel;

    public ActionPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(new Color(44, 62, 80));
        setPreferredSize(new Dimension(300, 280));
        setBorder(new EmptyBorder(8, 8, 8, 8));

        // 게이지 초기화
        diceGauge = new DiceGauge();

        initComponents();
    }

    private void initComponents() {
        // 메인 컨테이너 - 그라데이션 배경과 라운드 모서리
        JPanel container = new JPanel() {
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
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 15, 15);
            }
        };
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        container.setBorder(new EmptyBorder(12, 12, 12, 12));

        // 제목 - 더 큰 폰트와 아이콘
        JLabel titleLabel = new JLabel("🎲 주사위 컨트롤");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        titleLabel.setForeground(new Color(236, 240, 241));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        container.add(titleLabel);

        // 구분선
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(149, 165, 166, 100));
        separator.setMaximumSize(new Dimension(260, 1));
        container.add(Box.createVerticalStrut(8));
        container.add(separator);
        container.add(Box.createVerticalStrut(12));

        // 주사위 애니메이션 패널
        diceAnimationPanel = new DiceAnimationPanel();
        JPanel diceContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        diceContainer.setOpaque(false);
        diceContainer.add(diceAnimationPanel);
        container.add(diceContainer);

        container.add(Box.createVerticalStrut(15));

        // 게이지 패널
        gaugePanel = new GaugePanel(diceGauge);
        JPanel gaugeContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        gaugeContainer.setOpaque(false);
        gaugeContainer.add(gaugePanel);
        container.add(gaugeContainer);

        container.add(Box.createVerticalStrut(10));

        add(container, BorderLayout.CENTER);
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
}
