package com.marblegame.ui;

import com.marblegame.model.DiceGauge;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * 보드 위에 겹쳐지는 중앙 오버레이 패널
 * 턴 표시, 주사위, 게이지, 행동 버튼을 중앙에 배치
 *
 * 배치 순서 (상단 → 하단):
 * 1. 턴 라벨 (Turn #N)
 * 2. 주사위 애니메이션 패널
 * 3. 게이지 패널
 * 4. 행동 버튼 패널
 */
public class OverlayPanel extends JPanel {
    private static final int COMPONENT_SPACING = 24; // 컴포넌트 간 간격

    private JLabel turnLabel;
    private DiceAnimationPanel dicePanel;
    private GaugePanel gaugePanel;
    private DiceGauge diceGauge; // 추가됨: 게이지 모델
    private JPanel actionButtonPanel;

    // 추가됨: 행동 버튼들
    private JButton rollDiceButton;
    private JButton purchaseButton;
    private JButton upgradeButton;
    private JButton takeoverButton;
    private JButton skipButton;
    private JButton escapeButton;

    // 다크 테마 색상
    private static final Color BACKGROUND_DARK = new Color(32, 33, 36);
    private static final Color TEXT_PRIMARY = new Color(232, 234, 237);
    private static final Color ACCENT_COLOR = new Color(138, 180, 248);

    // 버튼 색상
    private static final Color BUTTON_ROLL = new Color(41, 128, 185);
    private static final Color BUTTON_PURCHASE = new Color(39, 174, 96);
    private static final Color BUTTON_UPGRADE = new Color(243, 156, 18);
    private static final Color BUTTON_TAKEOVER = new Color(142, 68, 173);
    private static final Color BUTTON_SKIP = new Color(127, 140, 141);
    private static final Color BUTTON_ESCAPE = new Color(192, 57, 43);

    public OverlayPanel() {
        setLayout(null); // 절대 위치 사용
        setOpaque(false); // 투명 배경으로 보드가 보이도록

        initComponents();

        // 리사이즈 시 컴포넌트 재배치
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                repositionComponents();
            }
        });
    }

    private void initComponents() {
        // 1. 턴 라벨
        turnLabel = new JLabel("Turn #1", SwingConstants.CENTER);
        turnLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 24));
        turnLabel.setForeground(ACCENT_COLOR);
        turnLabel.setOpaque(true);
        turnLabel.setBackground(new Color(BACKGROUND_DARK.getRed(), BACKGROUND_DARK.getGreen(),
                                          BACKGROUND_DARK.getBlue(), 220)); // 반투명
        turnLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_COLOR, 2),
            BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        add(turnLabel);

        // 2. 주사위 패널
        dicePanel = new DiceAnimationPanel();
        add(dicePanel);

        // 3. 게이지 패널 (DiceGauge 모델과 함께 생성)
        diceGauge = new DiceGauge();
        gaugePanel = new GaugePanel(diceGauge);
        add(gaugePanel);

        // 4. 행동 버튼 패널
        actionButtonPanel = new JPanel();
        actionButtonPanel.setLayout(new BoxLayout(actionButtonPanel, BoxLayout.Y_AXIS));
        actionButtonPanel.setOpaque(false);
        add(actionButtonPanel);

        // 5. 버튼 생성 (초기에는 숨김)
        rollDiceButton = createStyledButton("🎲 주사위 굴리기", BUTTON_ROLL);
        purchaseButton = createStyledButton("🏠 매입하기", BUTTON_PURCHASE);
        upgradeButton = createStyledButton("⭐ 업그레이드", BUTTON_UPGRADE);
        takeoverButton = createStyledButton("💰 인수하기", BUTTON_TAKEOVER);
        skipButton = createStyledButton("⏭ 패스", BUTTON_SKIP);
        escapeButton = createStyledButton("🔓 탈출하기", BUTTON_ESCAPE);

        // 모든 버튼을 패널에 추가 (초기 상태는 숨김)
        rollDiceButton.setVisible(false);
        purchaseButton.setVisible(false);
        upgradeButton.setVisible(false);
        takeoverButton.setVisible(false);
        skipButton.setVisible(false);
        escapeButton.setVisible(false);

        actionButtonPanel.add(rollDiceButton);
        actionButtonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        actionButtonPanel.add(purchaseButton);
        actionButtonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        actionButtonPanel.add(upgradeButton);
        actionButtonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        actionButtonPanel.add(takeoverButton);
        actionButtonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        actionButtonPanel.add(skipButton);
        actionButtonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        actionButtonPanel.add(escapeButton);
    }

    /**
     * 스타일이 적용된 버튼 생성
     */
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        button.setForeground(TEXT_PRIMARY);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(260, 35));
        button.setPreferredSize(new Dimension(260, 35));

        // 호버 효과
        Color hoverColor = bgColor.brighter();
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(hoverColor);
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    /**
     * 창 크기 변경 시 모든 컴포넌트를 중앙에 재배치
     */
    private void repositionComponents() {
        int width = getWidth();
        int height = getHeight();

        if (width == 0 || height == 0) return;

        int cx = width / 2;  // 중심 X 좌표
        int cy = height / 2; // 중심 Y 좌표

        // 컴포넌트 크기
        final int TURN_LABEL_WIDTH = 200;
        final int TURN_LABEL_HEIGHT = 50;

        final int DICE_PANEL_WIDTH = 180;
        final int DICE_PANEL_HEIGHT = 100;

        final int GAUGE_PANEL_WIDTH = 320;
        final int GAUGE_PANEL_HEIGHT = 60;

        final int BUTTON_PANEL_WIDTH = 280;
        final int BUTTON_PANEL_HEIGHT = 80;

        // 전체 높이 계산
        int totalHeight = TURN_LABEL_HEIGHT + COMPONENT_SPACING +
                         DICE_PANEL_HEIGHT + 10 + // 주사위와 게이지 간격은 좁게
                         GAUGE_PANEL_HEIGHT + COMPONENT_SPACING +
                         BUTTON_PANEL_HEIGHT;

        // 시작 Y 좌표 (중앙 정렬)
        int startY = cy - (totalHeight / 2);
        int currentY = startY;

        // 1. 턴 라벨 배치
        turnLabel.setBounds(cx - TURN_LABEL_WIDTH / 2, currentY,
                           TURN_LABEL_WIDTH, TURN_LABEL_HEIGHT);
        currentY += TURN_LABEL_HEIGHT + COMPONENT_SPACING;

        // 2. 주사위 패널 배치
        dicePanel.setBounds(cx - DICE_PANEL_WIDTH / 2, currentY,
                           DICE_PANEL_WIDTH, DICE_PANEL_HEIGHT);
        currentY += DICE_PANEL_HEIGHT + 10;

        // 3. 게이지 패널 배치
        gaugePanel.setBounds(cx - GAUGE_PANEL_WIDTH / 2, currentY,
                            GAUGE_PANEL_WIDTH, GAUGE_PANEL_HEIGHT);
        currentY += GAUGE_PANEL_HEIGHT + COMPONENT_SPACING;

        // 4. 행동 버튼 패널 배치
        actionButtonPanel.setBounds(cx - BUTTON_PANEL_WIDTH / 2, currentY,
                                   BUTTON_PANEL_WIDTH, BUTTON_PANEL_HEIGHT);
    }

    /**
     * 턴 번호 업데이트
     */
    public void setTurnNumber(int turnNumber) {
        turnLabel.setText("Turn #" + turnNumber);
    }

    /**
     * 주사위 패널 반환 (외부에서 제어용)
     */
    public DiceAnimationPanel getDicePanel() {
        return dicePanel;
    }

    /**
     * 게이지 패널 반환 (외부에서 제어용)
     */
    public GaugePanel getGaugePanel() {
        return gaugePanel;
    }

    /**
     * 행동 버튼 추가
     */
    public void addActionButton(JButton button) {
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(260, 35));
        actionButtonPanel.add(button);
        actionButtonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        actionButtonPanel.revalidate();
        actionButtonPanel.repaint();
        repositionComponents();
    }

    /**
     * 모든 행동 버튼 제거
     */
    public void clearActionButtons() {
        actionButtonPanel.removeAll();
        actionButtonPanel.revalidate();
        actionButtonPanel.repaint();
        repositionComponents();
    }

    /**
     * 특정 버튼 표시/숨김
     */
    public void setButtonVisible(JButton button, boolean visible) {
        button.setVisible(visible);
        actionButtonPanel.revalidate();
        actionButtonPanel.repaint();
    }

    @Override
    public void doLayout() {
        super.doLayout();
        repositionComponents();
    }

    // ========== 추가됨: 버튼 관련 메서드 ==========

    /**
     * 주사위 굴리기 버튼 반환
     */
    public JButton getRollDiceButton() {
        return rollDiceButton;
    }

    /**
     * 버튼 활성화/비활성화 설정
     * @param roll 주사위 굴리기
     * @param purchase 매입하기
     * @param upgrade 업그레이드
     * @param takeover 인수하기
     * @param skip 패스
     * @param escape 탈출하기
     */
    public void setButtonsEnabled(boolean roll, boolean purchase, boolean upgrade,
                                  boolean takeover, boolean skip, boolean escape) {
        rollDiceButton.setVisible(roll);
        rollDiceButton.setEnabled(roll);

        purchaseButton.setVisible(purchase);
        purchaseButton.setEnabled(purchase);

        upgradeButton.setVisible(upgrade);
        upgradeButton.setEnabled(upgrade);

        takeoverButton.setVisible(takeover);
        takeoverButton.setEnabled(takeover);

        skipButton.setVisible(skip);
        skipButton.setEnabled(skip);

        escapeButton.setVisible(escape);
        escapeButton.setEnabled(escape);

        actionButtonPanel.revalidate();
        actionButtonPanel.repaint();
    }

    /**
     * 이벤트 리스너 설정
     */
    public void setPurchaseListener(java.awt.event.ActionListener listener) {
        purchaseButton.addActionListener(listener);
    }

    public void setUpgradeListener(java.awt.event.ActionListener listener) {
        upgradeButton.addActionListener(listener);
    }

    public void setTakeoverListener(java.awt.event.ActionListener listener) {
        takeoverButton.addActionListener(listener);
    }

    public void setSkipListener(java.awt.event.ActionListener listener) {
        skipButton.addActionListener(listener);
    }

    public void setEscapeListener(java.awt.event.ActionListener listener) {
        escapeButton.addActionListener(listener);
    }

    /**
     * 게이지 반환 (하위 호환성)
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
     * 게이지 애니메이션 종료
     */
    public void stopGaugeAnimation() {
        gaugePanel.stopAnimation();
    }

    /**
     * 주사위 애니메이션 패널 반환
     */
    public DiceAnimationPanel getDiceAnimationPanel() {
        return dicePanel;
    }
}
