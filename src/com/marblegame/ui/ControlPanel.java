package com.marblegame.ui;

import com.marblegame.model.DiceGauge;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 게임 컨트롤 및 로그를 표시하는 패널
 * 더 인터랙티브하고 눈에 띄는 디자인
 */
public class ControlPanel extends JPanel {
    private JTextArea logArea;
    private JButton rollDiceButton;
    private JButton purchaseButton;
    private JButton upgradeButton;
    private JButton takeoverButton;
    private JButton skipButton;
    private JButton escapeButton;

    // 버튼 원래 색상 저장
    private Color rollDiceColor = new Color(41, 128, 185);
    private Color purchaseColor = new Color(39, 174, 96);
    private Color upgradeColor = new Color(243, 156, 18);
    private Color takeoverColor = new Color(142, 68, 173);
    private Color skipColor = new Color(127, 140, 141);
    private Color escapeColor = new Color(192, 57, 43);

    // 동적 버튼 컨테이너
    private JPanel buttonContainer;
    private JPanel logPanel;

    // 주사위 게이지
    private DiceGauge diceGauge;
    private GaugePanel gaugePanel;

    public ControlPanel() {
        setLayout(new BorderLayout(15, 15));
        setPreferredSize(new Dimension(1000, 310)); // 게이지 공간 추가
        setBackground(new Color(44, 62, 80));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // 게이지 초기화
        diceGauge = new DiceGauge();

        initComponents();
    }

    private void initComponents() {
        // 전체 레이아웃
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(new Color(44, 62, 80));

        // 로그 영역 (좌측)
        logPanel = createLogPanel();

        // 컨트롤 버튼 패널 (우측) - 동적으로 변경 가능하게 생성
        JPanel controlPanel = createControlPanelContainer();

        mainPanel.add(logPanel, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.EAST);

        // 게이지 패널 (하단)
        gaugePanel = new GaugePanel(diceGauge);
        JPanel gaugeContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        gaugeContainer.setBackground(new Color(44, 62, 80));
        gaugeContainer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(52, 152, 219), 2),
            new EmptyBorder(5, 5, 5, 5)
        ));
        gaugeContainer.add(gaugePanel);

        add(mainPanel, BorderLayout.CENTER);
        add(gaugeContainer, BorderLayout.SOUTH);

        // 버튼 생성 (초기화만, 화면에는 추가하지 않음)
        rollDiceButton = createInteractiveButton("🎲 주사위 굴리기", rollDiceColor);
        purchaseButton = createInteractiveButton("🏠 매입하기", purchaseColor);
        upgradeButton = createInteractiveButton("⭐ 업그레이드", upgradeColor);
        takeoverButton = createInteractiveButton("💰 인수하기", takeoverColor);
        skipButton = createInteractiveButton("⏭ 패스", skipColor);
        escapeButton = createInteractiveButton("🔓 탈출하기", escapeColor);
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(44, 62, 80));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(52, 152, 219), 3),
            new EmptyBorder(10, 10, 10, 10)
        ));

        // 제목
        JLabel titleLabel = new JLabel("📋 게임 로그");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        titleLabel.setForeground(new Color(236, 240, 241));
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));

        logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        logArea.setBackground(new Color(52, 73, 94));
        logArea.setForeground(new Color(236, 240, 241));
        logArea.setCaretColor(Color.WHITE);
        logArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(null);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createControlPanelContainer() {
        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.setBackground(new Color(44, 62, 80));
        outerPanel.setPreferredSize(new Dimension(320, 250));

        // 제목
        JLabel titleLabel = new JLabel("🎮 행동 선택");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        titleLabel.setForeground(new Color(236, 240, 241));
        titleLabel.setBorder(new EmptyBorder(0, 0, 15, 0));

        // 동적 버튼 컨테이너
        buttonContainer = new JPanel();
        buttonContainer.setLayout(new BoxLayout(buttonContainer, BoxLayout.Y_AXIS));
        buttonContainer.setBackground(new Color(44, 62, 80));

        outerPanel.add(titleLabel, BorderLayout.NORTH);
        outerPanel.add(buttonContainer, BorderLayout.CENTER);

        return outerPanel;
    }

    private JPanel createButtonCard(JButton button, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(new Color(52, 73, 94));
        card.setMaximumSize(new Dimension(300, 55));
        card.setPreferredSize(new Dimension(300, 55));

        // 활성화된 버튼 스타일: 두꺼운 색상 테두리
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 3),
            new EmptyBorder(0, 0, 0, 0)
        ));

        // 좌측 색상 액센트 바
        JPanel accentBar = new JPanel();
        accentBar.setBackground(accentColor);
        accentBar.setPreferredSize(new Dimension(5, 55));

        // 버튼 활성화 상태로 설정
        button.setEnabled(true);
        button.setBackground(new Color(52, 73, 94));
        button.setForeground(Color.WHITE);
        button.setPreferredSize(new Dimension(290, 50));
        button.setBorder(new EmptyBorder(5, 10, 5, 10));

        card.add(accentBar, BorderLayout.WEST);
        card.add(button, BorderLayout.CENTER);

        return card;
    }

    private JButton createInteractiveButton(String text, Color baseColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(52, 73, 94));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 인터랙티브 효과
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    // 호버 시 색상 변경
                    button.setBackground(baseColor.darker());
                    button.setForeground(Color.WHITE);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(new Color(52, 73, 94));
                    button.setForeground(Color.WHITE);
                } else {
                    button.setBackground(new Color(52, 73, 94));
                    button.setForeground(new Color(100, 100, 100));
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(baseColor);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(baseColor.darker());
                }
            }
        });

        return button;
    }

    public void addLog(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public void clearLog() {
        logArea.setText("");
    }

    /**
     * 활성화된 버튼만 동적으로 표시
     * 비활성 버튼은 UI에서 제거하여 간결하게 유지
     */
    public void setButtonsEnabled(boolean roll, boolean purchase, boolean upgrade, boolean takeover, boolean skip, boolean escape) {
        // 기존 버튼 모두 제거
        buttonContainer.removeAll();

        // 활성화된 버튼만 추가
        if (roll) addButtonToContainer(rollDiceButton, rollDiceColor);
        if (purchase) addButtonToContainer(purchaseButton, purchaseColor);
        if (upgrade) addButtonToContainer(upgradeButton, upgradeColor);
        if (takeover) addButtonToContainer(takeoverButton, takeoverColor);
        if (skip) addButtonToContainer(skipButton, skipColor);
        if (escape) addButtonToContainer(escapeButton, escapeColor);

        // UI 갱신
        buttonContainer.revalidate();
        buttonContainer.repaint();
    }

    /**
     * 버튼을 컨테이너에 추가
     */
    private void addButtonToContainer(JButton button, Color accentColor) {
        // 버튼 카드 생성 및 추가
        buttonContainer.add(createButtonCard(button, accentColor));
        buttonContainer.add(Box.createVerticalStrut(8));
    }

    // 버튼 리스너 설정
    public void setRollDiceListener(ActionListener listener) {
        rollDiceButton.addActionListener(listener);
    }

    public void setPurchaseListener(ActionListener listener) {
        purchaseButton.addActionListener(listener);
    }

    public void setUpgradeListener(ActionListener listener) {
        upgradeButton.addActionListener(listener);
    }

    public void setTakeoverListener(ActionListener listener) {
        takeoverButton.addActionListener(listener);
    }

    public void setSkipListener(ActionListener listener) {
        skipButton.addActionListener(listener);
    }

    public void setEscapeListener(ActionListener listener) {
        escapeButton.addActionListener(listener);
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
     * 주사위 버튼 접근자 (press-and-hold 이벤트용)
     */
    public JButton getRollDiceButton() {
        return rollDiceButton;
    }
}
