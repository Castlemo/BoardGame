package com.marblegame.ui;

import com.marblegame.model.DiceGauge;
import com.marblegame.model.Player;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 보드 위에 겹쳐지는 중앙 오버레이 패널
 * 턴 표시, 주사위, 게이지, 행동 버튼을 중앙에 배치
 * 수정됨: 플레이어 정보 카드도 좌측 상단/하단에 표시
 *
 * 배치 순서:
 * - 좌측 상단: Player 1 카드
 * - 좌측 하단: Player 2 카드
 * - 중앙: 턴/주사위/게이지/버튼
 */
public class OverlayPanel extends JPanel {
    private static final int COMPONENT_SPACING = 24; // 컴포넌트 간 간격
    private static final int CARD_WIDTH = 160;  // 플레이어 카드 너비 (축소: 200→160)
    private static final int CARD_HEIGHT = 70;  // 플레이어 카드 높이 (축소: 120→70)
    private static final int CARD_MARGIN = 20;  // 카드와 테두리 간격

    private JLabel turnLabel;
    private DiceAnimationPanel dicePanel;
    private GaugePanel gaugePanel;
    private DiceGauge diceGauge; // 추가됨: 게이지 모델
    private JPanel actionButtonPanel;

    // 추가됨: 플레이어 카드
    private List<CompactPlayerCard> playerCards;
    private List<Player> players;

    // 스케일 팩터 (보드와 동일한 비율로 스케일링)
    private double scaleFactor = 1.0;

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

    public OverlayPanel(List<Player> players) {
        this.players = players;
        this.playerCards = new ArrayList<>();

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

        // 6. 플레이어 카드 생성 및 추가
        for (int i = 0; i < players.size(); i++) {
            CompactPlayerCard card = new CompactPlayerCard(players.get(i), i);
            playerCards.add(card);
            add(card);
        }
    }

    /**
     * 스타일이 적용된 버튼 생성 (스케일 적용)
     */
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        int fontSize = Math.max(10, (int)(14 * scaleFactor));
        button.setFont(new Font("Malgun Gothic", Font.BOLD, fontSize));
        button.setForeground(TEXT_PRIMARY);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        int buttonWidth = (int)(260 * scaleFactor);
        int buttonHeight = (int)(35 * scaleFactor);
        button.setMaximumSize(new Dimension(buttonWidth, buttonHeight));
        button.setPreferredSize(new Dimension(buttonWidth, buttonHeight));

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
     * 창 크기 변경 시 모든 컴포넌트를 재배치
     * 플레이어 카드: 보드 내부(타일 안쪽) 좌측 상단/하단
     * 중앙 컴포넌트: 턴/주사위/게이지/버튼
     */
    private void repositionComponents() {
        int width = getWidth();
        int height = getHeight();

        if (width == 0 || height == 0) return;

        // 보드 내부 영역 계산 (타일 안쪽)
        // 보드는 9x9 타일로 구성되며, 각 타일은 80px
        // 내부 영역: 첫 번째 타일 이후부터 마지막 타일 전까지
        int tileSize = 80;
        int boardSize = 9 * tileSize; // 720px

        // 패널 크기에 맞춰 스케일 계산
        float scale = Math.min((float)width / boardSize, (float)height / boardSize);
        int scaledTileSize = (int)(tileSize * scale);
        int scaledBoardSize = (int)(boardSize * scale);

        // 보드가 중앙에 위치하도록 오프셋 계산
        int offsetX = (width - scaledBoardSize) / 2;
        int offsetY = (height - scaledBoardSize) / 2;

        // 내부 영역 경계 (첫 번째 타일 다음부터)
        int innerLeft = offsetX + scaledTileSize;
        int innerTop = offsetY + scaledTileSize;
        int innerRight = offsetX + scaledBoardSize - scaledTileSize;
        int innerBottom = offsetY + scaledBoardSize - scaledTileSize;

        // 스케일된 크기 계산
        int scaledCardWidth = (int)(CARD_WIDTH * scaleFactor);
        int scaledCardHeight = (int)(CARD_HEIGHT * scaleFactor);
        int scaledCardMargin = (int)(CARD_MARGIN * scaleFactor);

        // === 플레이어 카드 배치 (보드 내부 좌측) ===
        if (playerCards.size() >= 1) {
            // Player 1: 내부 영역 좌측 상단
            playerCards.get(0).setBounds(
                innerLeft + scaledCardMargin,
                innerTop + scaledCardMargin,
                scaledCardWidth,
                scaledCardHeight
            );
        }

        if (playerCards.size() >= 2) {
            // Player 2: 내부 영역 좌측 하단
            playerCards.get(1).setBounds(
                innerLeft + scaledCardMargin,
                innerBottom - scaledCardHeight - scaledCardMargin,
                scaledCardWidth,
                scaledCardHeight
            );
        }

        // === 중앙 컴포넌트 배치 ===
        int cx = width / 2;  // 중심 X 좌표
        int cy = height / 2; // 중심 Y 좌표

        // 컴포넌트 크기 (스케일 적용)
        final int TURN_LABEL_WIDTH = (int)(200 * scaleFactor);
        final int TURN_LABEL_HEIGHT = (int)(50 * scaleFactor);

        final int DICE_PANEL_WIDTH = (int)(180 * scaleFactor);
        final int DICE_PANEL_HEIGHT = (int)(100 * scaleFactor);

        final int GAUGE_PANEL_WIDTH = (int)(320 * scaleFactor);
        final int GAUGE_PANEL_HEIGHT = (int)(60 * scaleFactor);

        final int BUTTON_PANEL_WIDTH = (int)(280 * scaleFactor);
        final int BUTTON_PANEL_HEIGHT = (int)(80 * scaleFactor);

        final int scaledSpacing = (int)(COMPONENT_SPACING * scaleFactor);

        // 전체 높이 계산
        int totalHeight = TURN_LABEL_HEIGHT + scaledSpacing +
                         DICE_PANEL_HEIGHT + (int)(10 * scaleFactor) + // 주사위와 게이지 간격은 좁게
                         GAUGE_PANEL_HEIGHT + scaledSpacing +
                         BUTTON_PANEL_HEIGHT;

        // 시작 Y 좌표 (중앙 정렬)
        int startY = cy - (totalHeight / 2);
        int currentY = startY;

        // 폰트 크기도 스케일 적용
        turnLabel.setFont(new Font("Malgun Gothic", Font.BOLD, (int)(24 * scaleFactor)));

        // 1. 턴 라벨 배치
        turnLabel.setBounds(cx - TURN_LABEL_WIDTH / 2, currentY,
                           TURN_LABEL_WIDTH, TURN_LABEL_HEIGHT);
        currentY += TURN_LABEL_HEIGHT + scaledSpacing;

        // 2. 주사위 패널 배치
        dicePanel.setBounds(cx - DICE_PANEL_WIDTH / 2, currentY,
                           DICE_PANEL_WIDTH, DICE_PANEL_HEIGHT);
        currentY += DICE_PANEL_HEIGHT + (int)(10 * scaleFactor);

        // 3. 게이지 패널 배치
        gaugePanel.setBounds(cx - GAUGE_PANEL_WIDTH / 2, currentY,
                            GAUGE_PANEL_WIDTH, GAUGE_PANEL_HEIGHT);
        currentY += GAUGE_PANEL_HEIGHT + scaledSpacing;

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
     * 스케일 팩터 설정 (보드와 동일한 비율로 스케일링)
     */
    public void setScaleFactor(double scaleFactor) {
        this.scaleFactor = scaleFactor;
        updateButtonSizes();
        repositionComponents();
    }

    /**
     * 버튼 크기 및 폰트 업데이트
     */
    private void updateButtonSizes() {
        int fontSize = Math.max(10, (int)(14 * scaleFactor));
        int buttonWidth = (int)(260 * scaleFactor);
        int buttonHeight = (int)(35 * scaleFactor);
        Font buttonFont = new Font("Malgun Gothic", Font.BOLD, fontSize);
        Dimension buttonSize = new Dimension(buttonWidth, buttonHeight);

        JButton[] buttons = {rollDiceButton, purchaseButton, upgradeButton, takeoverButton, skipButton, escapeButton};
        for (JButton button : buttons) {
            if (button != null) {
                button.setFont(buttonFont);
                button.setMaximumSize(buttonSize);
                button.setPreferredSize(buttonSize);
            }
        }
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

    /**
     * 플레이어 정보 업데이트
     */
    public void updatePlayerInfo() {
        for (CompactPlayerCard card : playerCards) {
            card.repaint();
        }
    }

    // ========== 내부 클래스: CompactPlayerCard ==========

    /**
     * 소형 플레이어 정보 카드 (200x120)
     */
    private class CompactPlayerCard extends JPanel {
        private static final Color CARD_BACKGROUND = new Color(52, 73, 94);
        private static final Color TEXT_PRIMARY = new Color(236, 240, 241);
        private static final Color TEXT_SECONDARY = new Color(189, 195, 199);

        private static final Color[] PLAYER_COLORS = {
            new Color(231, 76, 60),   // Red
            new Color(52, 152, 219),  // Blue
            new Color(46, 204, 113),  // Green
            new Color(230, 126, 34)   // Orange
        };

        private final Player player;
        private final int playerIndex;

        CompactPlayerCard(Player player, int playerIndex) {
            this.player = player;
            this.playerIndex = playerIndex;
            setOpaque(false);
            setPreferredSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            // 스케일에 맞춘 라운드 크기와 테두리 두께
            int roundSize = Math.max(6, (int)(12 * scaleFactor));
            float strokeWidth = Math.max(1.5f, (float)(3 * scaleFactor));

            // 카드 배경
            g2.setColor(CARD_BACKGROUND);
            g2.fillRoundRect(0, 0, width, height, roundSize, roundSize);

            // 테두리
            Color accent = PLAYER_COLORS[playerIndex % PLAYER_COLORS.length];
            g2.setColor(accent);
            g2.setStroke(new BasicStroke(strokeWidth));
            g2.drawRoundRect(0, 0, width, height, roundSize, roundSize);

            // 플레이어 이름 (스케일 적용 폰트)
            g2.setColor(TEXT_PRIMARY);
            int nameFontSize = Math.max(8, (int)(12 * scaleFactor));
            Font nameFont = new Font("Malgun Gothic", Font.BOLD, nameFontSize);
            g2.setFont(nameFont);
            int nameX = (int)(10 * scaleFactor);
            int nameY = (int)(20 * scaleFactor);
            g2.drawString(player.name, nameX, nameY);

            // 정보 텍스트 (스케일 적용 폰트)
            int infoFontSize = Math.max(7, (int)(11 * scaleFactor));
            Font infoFont = new Font("Malgun Gothic", Font.PLAIN, infoFontSize);
            g2.setFont(infoFont);
            g2.setColor(TEXT_PRIMARY);
            int infoY = (int)(38 * scaleFactor);
            int lineHeight = (int)(16 * scaleFactor);

            // 항상 표시: 보유금액
            g2.drawString(String.format("💰 %,d원", player.cash), nameX, infoY);
            infoY += lineHeight;

            // 조건부 표시: 무인도에 있을 때만 남은 턴 수 표시
            if (player.isInJail()) {
                g2.drawString(String.format("🏝 %d턴", player.jailTurns), nameX, infoY);
            }

            g2.dispose();
        }
    }
}
