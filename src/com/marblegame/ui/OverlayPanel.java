package com.marblegame.ui;

import com.marblegame.model.DiceGauge;
import com.marblegame.model.Player;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

    // 홀수/짝수 선택 패널
    private JPanel oddEvenPanel;
    private JButton oddButton;
    private JButton evenButton;

    // 추가됨: 채팅 패널
    private ChatPanel chatPanel;
    private static final int CHAT_PANEL_WIDTH = 180;
    private static final int CHAT_PANEL_HEIGHT = 250;

    // 네트워크 채팅 콜백
    private java.util.function.BiConsumer<String, String> networkChatCallback; // type, content

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

        // 3. 홀수/짝수 선택 패널
        oddEvenPanel = new JPanel();
        oddEvenPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));
        oddEvenPanel.setOpaque(false);

        oddButton = createCircularToggleButton("홀수");
        evenButton = createCircularToggleButton("짝수");

        oddEvenPanel.add(oddButton);
        oddEvenPanel.add(evenButton);
        add(oddEvenPanel);

        // 4. 게이지 패널 (DiceGauge 모델과 함께 생성)
        diceGauge = new DiceGauge();
        gaugePanel = new GaugePanel(diceGauge);
        add(gaugePanel);

        // 5. 행동 버튼 패널
        actionButtonPanel = new JPanel();
        actionButtonPanel.setLayout(new BoxLayout(actionButtonPanel, BoxLayout.Y_AXIS));
        actionButtonPanel.setOpaque(false);
        add(actionButtonPanel);

        // 6. 버튼 생성 (초기에는 숨김)
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

        // 7. 채팅 패널 생성 및 추가
        chatPanel = new ChatPanel();
        chatPanel.setMessageSendCallback(message -> {
            // 네트워크 콜백이 설정되어 있으면 네트워크로 전송
            if (networkChatCallback != null) {
                networkChatCallback.accept("message", message);
            } else {
                // 로컬 모드: 바로 표시
                if (players.size() > 0) {
                    int currentPlayerIndex = getCurrentPlayerIndex();
                    String playerName = players.get(currentPlayerIndex).name;
                    chatPanel.addPlayerMessage(currentPlayerIndex, playerName, message);
                }
            }
        });
        chatPanel.setEmojiSendCallback(emoji -> {
            // 네트워크 콜백이 설정되어 있으면 네트워크로 전송
            if (networkChatCallback != null) {
                networkChatCallback.accept("emoji", emoji);
            } else {
                // 로컬 모드: 바로 표시
                if (players.size() > 0) {
                    int currentPlayerIndex = getCurrentPlayerIndex();
                    String playerName = players.get(currentPlayerIndex).name;
                    chatPanel.addEmojiMessage(currentPlayerIndex, playerName, emoji);
                }
            }
        });
        add(chatPanel);
    }

    /**
     * 네트워크 채팅 콜백 설정
     * @param callback BiConsumer<type, content> - type: "message" or "emoji", content: 메시지 내용
     */
    public void setNetworkChatCallback(java.util.function.BiConsumer<String, String> callback) {
        this.networkChatCallback = callback;
    }

    // 현재 플레이어 인덱스 (턴 라벨에서 추출)
    private int currentPlayerIndex = 0;

    public void setCurrentPlayerIndex(int index) {
        this.currentPlayerIndex = index;
    }

    private int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    /**
     * 스타일이 적용된 게임형 버튼 생성 (둥근 모서리, 그라데이션, 그림자, 애니메이션)
     */
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text) {
            private boolean hovered = false;
            private boolean pressed = false;
            private float animationProgress = 0f;
            private Timer animationTimer;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();
                int arc = 20; // 둥근 모서리 반경

                // 버튼이 비활성화되었을 때
                if (!isEnabled()) {
                    // 그림자 (비활성화)
                    g2.setColor(new Color(0, 0, 0, 30));
                    g2.fillRoundRect(2, 2, width - 4, height - 4, arc, arc);

                    // 배경 (회색)
                    g2.setColor(new Color(60, 63, 65));
                    g2.fillRoundRect(0, 0, width - 4, height - 6, arc, arc);

                    // 텍스트
                    g2.setColor(new Color(150, 150, 150));
                    g2.setFont(getFont());
                    FontMetrics fm = g2.getFontMetrics();
                    int textX = (width - fm.stringWidth(text)) / 2;
                    int textY = (height + fm.getAscent() - fm.getDescent()) / 2 - 2;
                    g2.drawString(text, textX, textY);

                    g2.dispose();
                    return;
                }

                // 그림자 효과
                int shadowOffset = pressed ? 1 : 3;
                int shadowSize = pressed ? 2 : 4;
                g2.setColor(new Color(0, 0, 0, 60));
                g2.fillRoundRect(shadowOffset, shadowOffset, width - shadowSize, height - shadowSize, arc, arc);

                // 버튼 위치 조정 (눌렸을 때 아래로 이동)
                int offsetY = pressed ? 2 : 0;

                // 그라데이션 배경
                Color color1 = hovered ? bgColor.brighter() : bgColor;
                Color color2 = hovered ? bgColor : bgColor.darker();

                GradientPaint gradient = new GradientPaint(
                    0, offsetY, color1,
                    0, height - 4 + offsetY, color2
                );
                g2.setPaint(gradient);
                g2.fillRoundRect(0, offsetY, width - 4, height - 6, arc, arc);

                // 하이라이트 (상단 광택 효과)
                GradientPaint highlight = new GradientPaint(
                    0, offsetY, new Color(255, 255, 255, 40),
                    0, height / 3 + offsetY, new Color(255, 255, 255, 0)
                );
                g2.setPaint(highlight);
                g2.fillRoundRect(0, offsetY, width - 4, height / 2 - 3, arc, arc);

                // 테두리 (미세한 외곽선)
                g2.setColor(new Color(255, 255, 255, 50));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, offsetY, width - 5, height - 7, arc, arc);

                // 호버 시 빛나는 효과
                if (hovered && animationProgress > 0) {
                    int alpha = (int)(100 * animationProgress);
                    g2.setColor(new Color(255, 255, 255, alpha));
                    g2.fillRoundRect(2, 2 + offsetY, width - 8, height - 10, arc, arc);
                }

                // 텍스트 (그림자 포함)
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int textX = (width - fm.stringWidth(text)) / 2;
                int textY = (height + fm.getAscent() - fm.getDescent()) / 2 - 2 + offsetY;

                // 텍스트 그림자
                g2.setColor(new Color(0, 0, 0, 100));
                g2.drawString(text, textX + 1, textY + 1);

                // 텍스트
                g2.setColor(TEXT_PRIMARY);
                g2.drawString(text, textX, textY);

                g2.dispose();
            }

            @Override
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                repaint();
            }
        };

        int fontSize = Math.max(10, (int)(14 * scaleFactor));
        button.setFont(new Font("Malgun Gothic", Font.BOLD, fontSize));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);

        int buttonWidth = (int)(260 * scaleFactor);
        int buttonHeight = (int)(40 * scaleFactor); // 약간 더 높게
        button.setMaximumSize(new Dimension(buttonWidth, buttonHeight));
        button.setPreferredSize(new Dimension(buttonWidth, buttonHeight));

        // 마우스 이벤트로 호버 및 클릭 애니메이션
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            private Timer hoverTimer;

            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (!button.isEnabled()) return;

                try {
                    java.lang.reflect.Field hoveredField = button.getClass().getDeclaredField("hovered");
                    hoveredField.setAccessible(true);
                    hoveredField.set(button, true);
                } catch (Exception ignored) {}

                // 호버 애니메이션 시작
                if (hoverTimer != null) hoverTimer.stop();
                hoverTimer = new Timer(30, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            java.lang.reflect.Field progressField = button.getClass().getDeclaredField("animationProgress");
                            progressField.setAccessible(true);
                            float progress = progressField.getFloat(button);
                            progress = Math.min(1f, progress + 0.1f);
                            progressField.set(button, progress);
                            button.repaint();
                            if (progress >= 1f) {
                                hoverTimer.stop();
                            }
                        } catch (Exception ignored) {}
                    }
                });
                hoverTimer.start();
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                try {
                    java.lang.reflect.Field hoveredField = button.getClass().getDeclaredField("hovered");
                    hoveredField.setAccessible(true);
                    hoveredField.set(button, false);
                } catch (Exception ignored) {}

                // 호버 애니메이션 종료
                if (hoverTimer != null) hoverTimer.stop();
                hoverTimer = new Timer(30, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            java.lang.reflect.Field progressField = button.getClass().getDeclaredField("animationProgress");
                            progressField.setAccessible(true);
                            float progress = progressField.getFloat(button);
                            progress = Math.max(0f, progress - 0.15f);
                            progressField.set(button, progress);
                            button.repaint();
                            if (progress <= 0f) {
                                hoverTimer.stop();
                            }
                        } catch (Exception ignored) {}
                    }
                });
                hoverTimer.start();
            }

            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (!button.isEnabled()) return;
                try {
                    java.lang.reflect.Field pressedField = button.getClass().getDeclaredField("pressed");
                    pressedField.setAccessible(true);
                    pressedField.set(button, true);
                    button.repaint();
                } catch (Exception ignored) {}
            }

            public void mouseReleased(java.awt.event.MouseEvent evt) {
                try {
                    java.lang.reflect.Field pressedField = button.getClass().getDeclaredField("pressed");
                    pressedField.setAccessible(true);
                    pressedField.set(button, false);
                    button.repaint();
                } catch (Exception ignored) {}
            }
        });

        return button;
    }

    /**
     * 원형 토글 버튼 생성 (홀수/짝수 선택용)
     */
    private JButton createCircularToggleButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int size = Math.min(getWidth(), getHeight());
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                // 선택 상태 확인
                Boolean selected = (Boolean) getClientProperty("selected");
                if (selected == null) selected = false;

                // 배경 원
                if (selected) {
                    g2.setColor(new Color(52, 152, 219)); // 파란색 (선택)
                } else {
                    g2.setColor(new Color(127, 140, 141)); // 회색 (미선택)
                }
                g2.fillOval(x, y, size, size);

                // 테두리
                g2.setColor(new Color(236, 240, 241));
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(x, y, size, size);

                // 텍스트
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int textX = (getWidth() - fm.stringWidth(text)) / 2;
                int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(text, textX, textY);

                g2.dispose();
            }
        };

        button.setPreferredSize(new Dimension(60, 60));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.putClientProperty("selected", false);

        return button;
    }

    /**
     * 창 크기 변경 시 모든 컴포넌트를 재배치
     * 플레이어 카드: 보드 내부(타일 안쪽) 좌측 상단/하단
     * 중앙 컴포넌트: 턴/주사위/홀짝/게이지/버튼
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

        // 컴포넌트 크기 (스케일 적용, 30% 축소 = 0.7배)
        final int TURN_LABEL_WIDTH = (int)(140 * scaleFactor);   // 200 * 0.7
        final int TURN_LABEL_HEIGHT = (int)(35 * scaleFactor);   // 50 * 0.7

        final int DICE_PANEL_WIDTH = (int)(126 * scaleFactor);   // 180 * 0.7
        final int DICE_PANEL_HEIGHT = (int)(70 * scaleFactor);   // 100 * 0.7

        final int ODDEVEN_PANEL_WIDTH = (int)(140 * scaleFactor); // 가로 배치를 위해 너비 증가
        final int ODDEVEN_PANEL_HEIGHT = (int)(49 * scaleFactor);// 70 * 0.7

        final int GAUGE_PANEL_WIDTH = (int)(224 * scaleFactor);  // 320 * 0.7
        final int GAUGE_PANEL_HEIGHT = (int)(42 * scaleFactor);  // 60 * 0.7

        final int BUTTON_PANEL_WIDTH = (int)(216 * scaleFactor); // 280 * 0.7 * 1.1 (10% 증가)
        final int BUTTON_PANEL_BASE_HEIGHT = (int)(62 * scaleFactor); // 80 * 0.7 * 1.1 (10% 증가)

        int buttonPanelHeight = BUTTON_PANEL_BASE_HEIGHT;
        Dimension actionPref = actionButtonPanel.getPreferredSize();
        if (actionPref != null) {
            buttonPanelHeight = Math.max(buttonPanelHeight, actionPref.height);
        }

        final int scaledSpacing = (int)(COMPONENT_SPACING * scaleFactor);

        // 전체 높이 계산 (홀짝 패널 추가)
        int totalHeight = TURN_LABEL_HEIGHT + scaledSpacing +
                         DICE_PANEL_HEIGHT + (int)(10 * scaleFactor) +
                         ODDEVEN_PANEL_HEIGHT + (int)(10 * scaleFactor) + // 홀짝 패널 추가
                         GAUGE_PANEL_HEIGHT + scaledSpacing +
                         buttonPanelHeight;

        // 시작 Y 좌표 (중앙 정렬)
        int startY = cy - (totalHeight / 2);
        int currentY = startY;

        // 폰트 크기도 스케일 적용 (30% 축소)
        turnLabel.setFont(new Font("Malgun Gothic", Font.BOLD, (int)(17 * scaleFactor))); // 24 * 0.7

        // 1. 턴 라벨 배치
        turnLabel.setBounds(cx - TURN_LABEL_WIDTH / 2, currentY,
                           TURN_LABEL_WIDTH, TURN_LABEL_HEIGHT);
        currentY += TURN_LABEL_HEIGHT + scaledSpacing;

        // 2. 주사위 패널 배치
        dicePanel.setBounds(cx - DICE_PANEL_WIDTH / 2, currentY,
                           DICE_PANEL_WIDTH, DICE_PANEL_HEIGHT);
        currentY += DICE_PANEL_HEIGHT + (int)(10 * scaleFactor);

        // 3. 홀수/짝수 선택 패널 배치
        oddEvenPanel.setBounds(cx - ODDEVEN_PANEL_WIDTH / 2, currentY,
                              ODDEVEN_PANEL_WIDTH, ODDEVEN_PANEL_HEIGHT);
        // 버튼 크기도 스케일 적용 (30% 축소)
        int buttonSize = (int)(42 * scaleFactor); // 60 * 0.7
        oddButton.setPreferredSize(new Dimension(buttonSize, buttonSize));
        evenButton.setPreferredSize(new Dimension(buttonSize, buttonSize));
        currentY += ODDEVEN_PANEL_HEIGHT + (int)(10 * scaleFactor);

        // 4. 게이지 패널 배치
        gaugePanel.setBounds(cx - GAUGE_PANEL_WIDTH / 2, currentY,
                            GAUGE_PANEL_WIDTH, GAUGE_PANEL_HEIGHT);
        currentY += GAUGE_PANEL_HEIGHT + scaledSpacing;

        // 5. 행동 버튼 패널 배치
        actionButtonPanel.setBounds(cx - BUTTON_PANEL_WIDTH / 2, currentY,
                                   BUTTON_PANEL_WIDTH, buttonPanelHeight);

        // 6. 채팅 패널 배치 (보드 내부 우측)
        int scaledChatWidth = (int)(CHAT_PANEL_WIDTH * scaleFactor);
        int scaledChatHeight = (int)(CHAT_PANEL_HEIGHT * scaleFactor);
        chatPanel.setBounds(
            innerRight - scaledChatWidth - scaledCardMargin,
            innerTop + scaledCardMargin,
            scaledChatWidth,
            scaledChatHeight
        );
        chatPanel.updateFontSize(scaleFactor);
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
     * 채팅 패널 반환 (외부에서 제어용)
     */
    public ChatPanel getChatPanel() {
        return chatPanel;
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
        setButtonsEnabled(false, false, false, false, false, false);
        clearPriceLabels();
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
        int buttonHeight = (int)(40 * scaleFactor); // 게임형 UI로 약간 더 높게
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
     * 홀수 버튼 반환
     */
    public JButton getOddButton() {
        return oddButton;
    }

    /**
     * 짝수 버튼 반환
     */
    public JButton getEvenButton() {
        return evenButton;
    }

    /**
     * 게이지 반환 (하위 호환성)
     */
    public DiceGauge getDiceGauge() {
        return diceGauge;
    }

    /**
     * 가격 라벨 초기화 (더 이상 사용되지 않지만 호환성 유지)
     */
    public void clearPriceLabels() {
        // taxInfoLabel 제거됨 - 메서드는 호환성을 위해 유지
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

    /**
     * 특정 플레이어의 자산 변동 표시
     * @param playerIndex 플레이어 인덱스
     * @param change 변동 금액 (양수: 수입, 음수: 지출)
     */
    public void showMoneyChange(int playerIndex, int change) {
        if (playerIndex >= 0 && playerIndex < playerCards.size()) {
            playerCards.get(playerIndex).showMoneyChange(change);
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
        private int moneyChange = 0;
        private long moneyChangeStartTime = 0;
        private static final long MONEY_CHANGE_DURATION = 2000; // 2초

        CompactPlayerCard(Player player, int playerIndex) {
            this.player = player;
            this.playerIndex = playerIndex;
            setOpaque(false);
            setPreferredSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));
        }

        /**
         * 자산 변동 표시
         * @param change 변동 금액 (양수: 수입, 음수: 지출)
         */
        void showMoneyChange(int change) {
            if (change == 0) return;
            this.moneyChange = change;
            this.moneyChangeStartTime = System.currentTimeMillis();

            // 2초 동안 표시
            Timer timer = new Timer(50, null);
            timer.addActionListener(e -> {
                long elapsed = System.currentTimeMillis() - moneyChangeStartTime;
                if (elapsed >= MONEY_CHANGE_DURATION) {
                    moneyChange = 0;
                    timer.stop();
                }
                repaint();
            });
            timer.start();
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

            // 자산 변동 표시 (보유금액 옆)
            if (moneyChange != 0 && System.currentTimeMillis() - moneyChangeStartTime < MONEY_CHANGE_DURATION) {
                String changeText;
                Color changeColor;
                if (moneyChange > 0) {
                    // 수입: 초록색
                    changeText = String.format("+%,d", moneyChange);
                    changeColor = new Color(46, 204, 113);
                } else {
                    // 지출: 빨간색
                    changeText = String.format("%,d", moneyChange);
                    changeColor = new Color(231, 76, 60);
                }

                // 페이드 아웃 효과
                long elapsed = System.currentTimeMillis() - moneyChangeStartTime;
                float alpha = 1.0f - ((float) elapsed / MONEY_CHANGE_DURATION);
                alpha = Math.max(0, Math.min(1, alpha));

                g2.setColor(new Color(changeColor.getRed(), changeColor.getGreen(), changeColor.getBlue(),
                    (int)(alpha * 255)));
                int changeFontSize = Math.max(7, (int)(12 * scaleFactor));
                Font changeFont = new Font("Malgun Gothic", Font.BOLD, changeFontSize);
                g2.setFont(changeFont);

                // 보유금액 텍스트 오른쪽에 표시
                FontMetrics fm = g2.getFontMetrics();
                String cashText = String.format("💰 %,d원", player.cash);
                int cashTextWidth = fm.stringWidth(cashText);
                g2.drawString(changeText, nameX + cashTextWidth + (int)(5 * scaleFactor), infoY);

                g2.setFont(infoFont); // 원래 폰트로 복구
                g2.setColor(TEXT_PRIMARY); // 원래 색상으로 복구
            }

            infoY += lineHeight;

            // 조건부 표시: 무인도에 있을 때만 남은 턴 수 표시
            if (player.isInJail()) {
                g2.drawString(String.format("🏝 %d턴", player.jailTurns), nameX, infoY);
            }

            g2.dispose();
        }
    }
}
