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
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(44, 62, 80));
        setPreferredSize(new Dimension(300, 200));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // 게이지 초기화
        diceGauge = new DiceGauge();

        initComponents();
    }

    private void initComponents() {
        // 전체를 세로로 배치
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(new Color(44, 62, 80));

        // 제목
        JLabel titleLabel = new JLabel("🎯 주사위 컨트롤");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        titleLabel.setForeground(new Color(236, 240, 241));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(10));

        // 주사위 애니메이션 패널
        diceAnimationPanel = new DiceAnimationPanel();
        JPanel diceContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        diceContainer.setBackground(new Color(44, 62, 80));
        diceContainer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(231, 76, 60), 2),
            new EmptyBorder(5, 5, 5, 5)
        ));
        diceContainer.add(diceAnimationPanel);
        diceContainer.setMaximumSize(new Dimension(280, 100));
        mainPanel.add(diceContainer);

        mainPanel.add(Box.createVerticalStrut(10));

        // 게이지 패널
        gaugePanel = new GaugePanel(diceGauge);
        JPanel gaugeContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        gaugeContainer.setBackground(new Color(44, 62, 80));
        gaugeContainer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(52, 152, 219), 2),
            new EmptyBorder(5, 5, 5, 5)
        ));
        gaugeContainer.add(gaugePanel);
        gaugeContainer.setMaximumSize(new Dimension(280, 80));
        mainPanel.add(gaugeContainer);

        add(mainPanel, BorderLayout.CENTER);
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
