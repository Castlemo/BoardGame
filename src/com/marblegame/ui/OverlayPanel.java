package com.marblegame.ui;

import com.marblegame.model.DiceGauge;
import com.marblegame.model.Player;
import com.marblegame.util.ImageLoader;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
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

    // 네트워크 모드 여부
    private boolean networkMode;

    // 네트워크 채팅 콜백
    private java.util.function.BiConsumer<String, String> networkChatCallback; // type, content

    // 네트워크 턴 차단용 오버레이
    private TurnBlockLayer turnBlockLayer;
    private Rectangle turnBlockVisualArea;

    // UIConstants에서 가져온 다크 테마 색상 (로컬 별칭)
    private static final Color BACKGROUND_DARK = UIConstants.BACKGROUND_DARK;
    private static final Color TEXT_PRIMARY = UIConstants.TEXT_PRIMARY;
    private static final Color ACCENT_COLOR = UIConstants.ACCENT_COLOR;

    // UIConstants에서 가져온 버튼 색상 (로컬 별칭)
    private static final Color BUTTON_ROLL = UIConstants.BUTTON_ROLL;
    private static final Color BUTTON_PURCHASE = UIConstants.BUTTON_PURCHASE;
    private static final Color BUTTON_UPGRADE = UIConstants.BUTTON_UPGRADE;
    private static final Color BUTTON_TAKEOVER = UIConstants.BUTTON_TAKEOVER;
    private static final Color BUTTON_SKIP = UIConstants.BUTTON_SKIP;
    private static final Color BUTTON_ESCAPE = UIConstants.BUTTON_ESCAPE;

    public OverlayPanel(List<Player> players) {
        this(players, false); // 기본값: 로컬 모드
    }

    public OverlayPanel(List<Player> players, boolean networkMode) {
        this.players = players;
        this.playerCards = new ArrayList<>();
        this.networkMode = networkMode;

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
        turnLabel.setFont(new Font(UIConstants.FONT_NAME, Font.BOLD, 24));
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

        oddButton = createCircularToggleButton("홀수", new Color(52, 152, 219)); // 파란색
        evenButton = createCircularToggleButton("짝수", new Color(231, 76, 60)); // 빨간색

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
        rollDiceButton = createStyledButton("주사위 굴리기", BUTTON_ROLL);
        purchaseButton = createStyledButton("매입하기", BUTTON_PURCHASE);
        upgradeButton = createStyledButton("업그레이드", BUTTON_UPGRADE);
        takeoverButton = createStyledButton("인수하기", BUTTON_TAKEOVER);
        skipButton = createStyledButton("패스", BUTTON_SKIP);
        escapeButton = createStyledButton("탈출하기", BUTTON_ESCAPE);

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

        // 7. 채팅 패널 생성 및 추가 (네트워크 모드에서만)
        if (networkMode) {
            chatPanel = new ChatPanel();
            chatPanel.setMessageSendCallback(message -> {
                // 네트워크 콜백이 설정되어 있으면 네트워크로 전송
                if (networkChatCallback != null) {
                    networkChatCallback.accept("message", message);
                }
            });
            chatPanel.setEmojiSendCallback(emoji -> {
                // 네트워크 콜백이 설정되어 있으면 네트워크로 전송
                if (networkChatCallback != null) {
                    networkChatCallback.accept("emoji", emoji);
                }
            });
            add(chatPanel);
        }

        // 8. 턴 차단 오버레이 (마지막에 추가하여 최상단에 위치)
        turnBlockLayer = new TurnBlockLayer();
        turnBlockLayer.setVisible(false);
        add(turnBlockLayer);
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
                    g2.setColor(UIConstants.BUTTON_DISABLED);
                    g2.fillRoundRect(0, 0, width - 4, height - 6, arc, arc);

                    // 텍스트
                    g2.setColor(UIConstants.TEXT_DISABLED);
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
        button.setFont(new Font(UIConstants.FONT_NAME, Font.BOLD, fontSize));
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
     * 원형 토글 버튼 생성 (홀수/짝수 선택용) - 개선된 인터랙티브 UI
     * @param text 버튼 텍스트
     * @param buttonColor 버튼 기본 색상
     */
    private JButton createCircularToggleButton(String text, Color buttonColor) {
        JButton button = new JButton(text) {
            private boolean hovered = false;
            private boolean pressed = false;
            private float glowIntensity = 0f;
            private float scaleAnimation = 1.0f;
            private Timer animationTimer;
            private final int BASE_SIZE = 54; // 기본 렌더링 크기 (60에서 54로 축소하여 여유 공간 확보)

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // 실제 렌더링 크기 (스케일 적용)
                int renderSize = (int)(BASE_SIZE * scaleAnimation);
                int x = (getWidth() - renderSize) / 2;
                int y = (getHeight() - renderSize) / 2;

                // 선택 상태 확인
                Boolean selected = (Boolean) getClientProperty("selected");
                if (selected == null) selected = false;

                // 외부 발광 효과 (선택되었을 때)
                if (selected && glowIntensity > 0) {
                    int glowSize = renderSize + (int)(10 * glowIntensity);
                    int glowX = (getWidth() - glowSize) / 2;
                    int glowY = (getHeight() - glowSize) / 2;

                    for (int i = 4; i >= 0; i--) {
                        int alpha = (int)(30 * glowIntensity * (1 - i / 5.0));
                        // 발광 색상도 버튼 색상에 맞춤
                        Color glowColor = new Color(
                            buttonColor.getRed(),
                            buttonColor.getGreen(),
                            buttonColor.getBlue(),
                            alpha
                        );
                        g2.setColor(glowColor);
                        int offset = i * 2;
                        g2.fillOval(glowX - offset, glowY - offset,
                                   glowSize + offset * 2, glowSize + offset * 2);
                    }
                }

                // 그림자 (눌렸을 때 감소)
                int shadowOffset = pressed ? 1 : 3;
                int shadowSize = pressed ? 2 : 5;
                g2.setColor(new Color(0, 0, 0, 80));
                g2.fillOval(x + shadowOffset, y + shadowOffset,
                           renderSize - shadowSize, renderSize - shadowSize);

                // 배경 그라데이션
                Color baseColor;
                if (selected) {
                    baseColor = buttonColor; // 전달받은 색상 사용
                } else {
                    baseColor = new Color(127, 140, 141); // 회색 (미선택)
                }

                Color color1 = hovered ? baseColor.brighter() : baseColor;
                Color color2 = hovered ? baseColor : baseColor.darker();

                GradientPaint gradient = new GradientPaint(
                    x, y, color1,
                    x, y + renderSize, color2
                );
                g2.setPaint(gradient);
                g2.fillOval(x, y, renderSize, renderSize);

                // 상단 하이라이트 (광택 효과)
                int highlightSize = (int)(renderSize * 0.6);
                GradientPaint highlight = new GradientPaint(
                    x + renderSize / 4, y + renderSize / 6, new Color(255, 255, 255, 80),
                    x + renderSize / 4, y + renderSize / 2, new Color(255, 255, 255, 0)
                );
                g2.setPaint(highlight);
                g2.fillOval(x + renderSize / 6, y + renderSize / 8,
                           highlightSize, highlightSize / 2);

                // 테두리
                if (selected) {
                    // 선택됨: 금색 테두리
                    g2.setColor(new Color(241, 196, 15));
                    g2.setStroke(new BasicStroke(3f));
                } else {
                    // 미선택: 흰색 테두리
                    g2.setColor(new Color(236, 240, 241));
                    g2.setStroke(new BasicStroke(2f));
                }
                g2.drawOval(x + 1, y + 1, renderSize - 2, renderSize - 2);

                // 내부 테두리 (깊이감)
                g2.setColor(new Color(0, 0, 0, 30));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(x + 3, y + 3, renderSize - 6, renderSize - 6);

                // 호버 효과 (반짝임)
                if (hovered) {
                    int pulseAlpha = (int)(50 + 30 * Math.sin(System.currentTimeMillis() / 200.0));
                    g2.setColor(new Color(255, 255, 255, pulseAlpha));
                    g2.fillOval(x + 2, y + 2, renderSize - 4, renderSize - 4);
                }

                // 텍스트 (그림자 포함)
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int textX = (getWidth() - fm.stringWidth(text)) / 2;
                int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;

                // 텍스트 그림자
                g2.setColor(new Color(0, 0, 0, 120));
                g2.drawString(text, textX + 1, textY + 1);

                // 텍스트
                g2.setColor(Color.WHITE);
                g2.drawString(text, textX, textY);

                g2.dispose();
            }
        };

        // 버튼 크기를 70x70으로 확대하여 1.1배 스케일 시 여유 공간 확보
        button.setPreferredSize(new Dimension(70, 70));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.putClientProperty("selected", false);

        // 마우스 이벤트로 인터랙티브 효과
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            private Timer hoverTimer;
            private Timer clickAnimationTimer;

            public void mouseEntered(java.awt.event.MouseEvent evt) {
                try {
                    java.lang.reflect.Field hoveredField = button.getClass().getDeclaredField("hovered");
                    hoveredField.setAccessible(true);
                    hoveredField.set(button, true);
                } catch (Exception ignored) {}

                // 호버 애니메이션 (발광 효과)
                if (hoverTimer != null) hoverTimer.stop();
                hoverTimer = new Timer(20, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            java.lang.reflect.Field glowField = button.getClass().getDeclaredField("glowIntensity");
                            glowField.setAccessible(true);
                            float intensity = glowField.getFloat(button);
                            intensity = Math.min(1f, intensity + 0.1f);
                            glowField.set(button, intensity);

                            java.lang.reflect.Field scaleField = button.getClass().getDeclaredField("scaleAnimation");
                            scaleField.setAccessible(true);
                            float scale = scaleField.getFloat(button);
                            scale = Math.min(1.1f, scale + 0.02f);
                            scaleField.set(button, scale);

                            button.repaint();
                            if (intensity >= 1f && scale >= 1.1f) {
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
                hoverTimer = new Timer(20, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            java.lang.reflect.Field glowField = button.getClass().getDeclaredField("glowIntensity");
                            glowField.setAccessible(true);
                            float intensity = glowField.getFloat(button);
                            intensity = Math.max(0f, intensity - 0.15f);
                            glowField.set(button, intensity);

                            java.lang.reflect.Field scaleField = button.getClass().getDeclaredField("scaleAnimation");
                            scaleField.setAccessible(true);
                            float scale = scaleField.getFloat(button);
                            scale = Math.max(1.0f, scale - 0.03f);
                            scaleField.set(button, scale);

                            button.repaint();
                            if (intensity <= 0f && scale <= 1.0f) {
                                hoverTimer.stop();
                            }
                        } catch (Exception ignored) {}
                    }
                });
                hoverTimer.start();
            }

            public void mousePressed(java.awt.event.MouseEvent evt) {
                try {
                    java.lang.reflect.Field pressedField = button.getClass().getDeclaredField("pressed");
                    pressedField.setAccessible(true);
                    pressedField.set(button, true);

                    // 클릭 시 스케일 감소
                    java.lang.reflect.Field scaleField = button.getClass().getDeclaredField("scaleAnimation");
                    scaleField.setAccessible(true);
                    scaleField.set(button, 0.95f);

                    button.repaint();
                } catch (Exception ignored) {}
            }

            public void mouseReleased(java.awt.event.MouseEvent evt) {
                try {
                    java.lang.reflect.Field pressedField = button.getClass().getDeclaredField("pressed");
                    pressedField.setAccessible(true);
                    pressedField.set(button, false);

                    // 릴리즈 시 바운스 애니메이션
                    if (clickAnimationTimer != null) clickAnimationTimer.stop();
                    clickAnimationTimer = new Timer(15, new ActionListener() {
                        private int step = 0;
                        public void actionPerformed(ActionEvent e) {
                            try {
                                java.lang.reflect.Field scaleField = button.getClass().getDeclaredField("scaleAnimation");
                                scaleField.setAccessible(true);

                                float targetScale = 1.1f;
                                float currentScale = scaleField.getFloat(button);

                                if (step < 10) {
                                    // 바운스 업
                                    currentScale = Math.min(targetScale, currentScale + 0.03f);
                                } else {
                                    // 원래 크기로
                                    currentScale = Math.max(targetScale, currentScale - 0.02f);
                                }

                                scaleField.set(button, currentScale);
                                button.repaint();

                                step++;
                                if (step > 15 || (step > 10 && Math.abs(currentScale - targetScale) < 0.01f)) {
                                    clickAnimationTimer.stop();
                                }
                            } catch (Exception ignored) {}
                        }
                    });
                    clickAnimationTimer.start();

                    button.repaint();
                } catch (Exception ignored) {}
            }
        });

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
        turnLabel.setFont(new Font(UIConstants.FONT_NAME, Font.BOLD, (int)(17 * scaleFactor))); // 24 * 0.7

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
        // 버튼 크기도 스케일 적용 (기본 70px, 스케일 적용 시 49px = 70 * 0.7)
        int buttonSize = (int)(49 * scaleFactor); // 70 * 0.7
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

        // 6. 채팅 패널 배치 (보드 내부 우측) - 네트워크 모드에서만
        if (chatPanel != null) {
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

        // 7. 턴 차단 오버레이는 패널 전체를 덮도록 설정
        if (turnBlockLayer != null) {
            turnBlockLayer.setBounds(0, 0, width, height);
            // 채팅 영역은 통과하도록 영역을 전달 (채팅 허용)
            Rectangle chatBounds = chatPanel != null ? chatPanel.getBounds() : null;
            turnBlockLayer.setChatPassThroughArea(chatBounds);

            // 시각적으로 표현할 영역: 주사위 굴리기 버튼 아래쪽(나머지 액션 영역)을 감싸도록 설정
            int overlayWidth = Math.max(Math.max(DICE_PANEL_WIDTH, GAUGE_PANEL_WIDTH), BUTTON_PANEL_WIDTH)
                + (int)(30 * scaleFactor);
            int overlayX = cx - overlayWidth / 2;
            int rollBtnY = rollDiceButton != null ? rollDiceButton.getY() : 0;
            int rollBtnH = rollDiceButton != null
                ? (rollDiceButton.getHeight() > 0 ? rollDiceButton.getHeight() : rollDiceButton.getPreferredSize().height)
                : 0;
            int overlayY = actionButtonPanel.getY() + rollBtnY + rollBtnH + (int)(6 * scaleFactor);
            int buttonBottom = actionButtonPanel.getY() + actionButtonPanel.getHeight();
            int overlayHeight = buttonBottom - overlayY + (int)(12 * scaleFactor);
            if (overlayHeight < 0) {
                overlayHeight = (int)(200 * scaleFactor);
            }
            turnBlockVisualArea = new Rectangle(overlayX, overlayY, overlayWidth, overlayHeight);
            turnBlockLayer.setVisualArea(turnBlockVisualArea);
        }
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
        Font buttonFont = new Font(UIConstants.FONT_NAME, Font.BOLD, fontSize);
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
     * 네트워크에서 다른 플레이어 턴일 때 화면을 덮는 오버레이 토글
     * @param blocked true면 차단 오버레이 표시
     */
    public void setTurnBlocked(boolean blocked) {
        if (turnBlockLayer != null) {
            turnBlockLayer.setVisible(blocked);
            turnBlockLayer.repaint();
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
        // UIConstants에서 가져온 색상 (로컬 별칭)
        private static final Color CARD_BACKGROUND = UIConstants.BORDER_DEFAULT; // 52, 73, 94
        private static final Color TEXT_PRIMARY = UIConstants.TEXT_PRIMARY;
        private static final Color TEXT_SECONDARY = UIConstants.TEXT_SECONDARY;

        // 플레이어 색상 (카드 전용)
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
            Font nameFont = new Font(UIConstants.FONT_NAME, Font.BOLD, nameFontSize);
            g2.setFont(nameFont);
            int nameX = (int)(10 * scaleFactor);
            int nameY = (int)(20 * scaleFactor);
            g2.drawString(player.name, nameX, nameY);

            // 정보 텍스트 (스케일 적용 폰트)
            int infoFontSize = Math.max(7, (int)(11 * scaleFactor));
            Font infoFont = new Font(UIConstants.FONT_NAME, Font.PLAIN, infoFontSize);
            g2.setFont(infoFont);
            g2.setColor(TEXT_PRIMARY);
            int infoY = (int)(38 * scaleFactor);
            int lineHeight = (int)(16 * scaleFactor);

            // 항상 표시: 보유금액 (아이콘 포함)
            BufferedImage moneyIcon = ImageLoader.getTileImage("MONEY");
            int iconX = nameX;
            if (moneyIcon != null) {
                int iconSize = (int)(14 * scaleFactor);
                BufferedImage scaledMoney = ImageLoader.scaleImage(moneyIcon, iconSize, iconSize);
                g2.drawImage(scaledMoney, iconX, infoY - iconSize + 2, null);
                iconX += iconSize + (int)(3 * scaleFactor);
            }
            g2.drawString(String.format("%,d원", player.cash), iconX, infoY);

            // 자산 변동 표시 (보유금액 옆)
            if (moneyChange != 0 && System.currentTimeMillis() - moneyChangeStartTime < MONEY_CHANGE_DURATION) {
                String changeText;
                Color changeColor;
                if (moneyChange > 0) {
                    // 수입: 초록색
                    changeText = String.format("+%,d", moneyChange);
                    changeColor = UIConstants.STATUS_SUCCESS;
                } else {
                    // 지출: 빨간색
                    changeText = String.format("%,d", moneyChange);
                    changeColor = UIConstants.STATUS_ERROR;
                }

                // 페이드 아웃 효과
                long elapsed = System.currentTimeMillis() - moneyChangeStartTime;
                float alpha = 1.0f - ((float) elapsed / MONEY_CHANGE_DURATION);
                alpha = Math.max(0, Math.min(1, alpha));

                g2.setColor(new Color(changeColor.getRed(), changeColor.getGreen(), changeColor.getBlue(),
                    (int)(alpha * 255)));
                int changeFontSize = Math.max(7, (int)(12 * scaleFactor));
                Font changeFont = new Font(UIConstants.FONT_NAME, Font.BOLD, changeFontSize);
                g2.setFont(changeFont);

                // 보유금액 텍스트 오른쪽에 표시
                FontMetrics fm = g2.getFontMetrics();
                String cashText = String.format("%,d원", player.cash);
                int iconOffset = moneyIcon != null ? (int)(17 * scaleFactor) : 0;
                int cashTextWidth = fm.stringWidth(cashText);
                g2.drawString(changeText, nameX + iconOffset + cashTextWidth + (int)(5 * scaleFactor), infoY);

                g2.setFont(infoFont); // 원래 폰트로 복구
                g2.setColor(TEXT_PRIMARY); // 원래 색상으로 복구
            }

            infoY += lineHeight;

            // 조건부 표시: 무인도에 있을 때만 남은 턴 수 표시
            if (player.isInJail()) {
                g2.drawString(String.format("~ %d턴", player.jailTurns), nameX, infoY);
            }

            g2.dispose();
        }
    }

    /**
     * 다른 플레이어 턴일 때 표시되는 반투명 차단 레이어
     * - 검은 반투명 배경
     * - 중앙 텍스트 "다른 플레이어가 플레이중"
     * - 채팅 영역은 투명하게 뚫어 입력 가능
     */
    private class TurnBlockLayer extends JComponent {
        private Rectangle chatPassThroughArea;
        private Rectangle visualArea;

        private TurnBlockLayer() {
            setOpaque(false);
            // 입력 차단용 기본 리스너 (이 레이어가 상단에서 이벤트를 받아 아래로 내려가지 않도록 함)
            addMouseListener(new java.awt.event.MouseAdapter() { });
            addMouseMotionListener(new java.awt.event.MouseMotionAdapter() { });
            addMouseWheelListener(e -> e.consume());
        }

        public void setChatPassThroughArea(Rectangle rect) {
            this.chatPassThroughArea = rect != null ? new Rectangle(rect) : null;
        }

        public void setVisualArea(Rectangle area) {
            this.visualArea = area != null ? new Rectangle(area) : null;
        }

        @Override
        public boolean contains(int x, int y) {
            // 채팅 영역은 오버레이가 없는 것처럼 취급하여 입력이 통과하도록 함
            if (chatPassThroughArea != null && chatPassThroughArea.contains(x, y)) {
                return false;
            }
            return super.contains(x, y);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (!isVisible()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 중앙 텍스트
            String message = "상대방 플레이어가 플레이 중..";
            int fontSize = Math.max(14, (int)(22 * scaleFactor));
            Font font = new Font(UIConstants.FONT_NAME, Font.BOLD, fontSize);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(message);
            int textHeight = fm.getHeight();

            // 시각적으로 오버레이를 표시할 영역 (없으면 전체)
            Rectangle paintArea = visualArea != null ? new Rectangle(visualArea) : new Rectangle(0, 0, getWidth(), getHeight());

            // 텍스트 크기에 맞춰 영역을 보정 (너무 작거나 크지 않게 중앙 기준으로 맞춤)
            int minWidth = textWidth + (int)(60 * scaleFactor);  // 패딩 소폭 축소
            int minHeight = textHeight + (int)(24 * scaleFactor);
            int centerX = paintArea.x + paintArea.width / 2;
            int centerY = paintArea.y + paintArea.height / 2;
            int newWidth = Math.max(paintArea.width, minWidth);
            int newHeight = Math.max(paintArea.height, minHeight);
            paintArea.x = centerX - newWidth / 2;
            paintArea.y = centerY - newHeight / 2;
            paintArea.width = newWidth;
            paintArea.height = newHeight;
            // 컴포넌트 경계 내로 보정
            if (paintArea.x < 0) {
                paintArea.width = paintArea.width + paintArea.x;
                paintArea.x = 0;
            }
            if (paintArea.y < 0) {
                paintArea.height = paintArea.height + paintArea.y;
                paintArea.y = 0;
            }
            paintArea.width = Math.min(paintArea.width, getWidth() - paintArea.x);
            paintArea.height = Math.min(paintArea.height, getHeight() - paintArea.y);

            // 오버레이 영역에서 채팅 영역을 제외
            float arc = (float)(18 * scaleFactor);
            Area overlayArea = new Area(new RoundRectangle2D.Float(
                paintArea.x, paintArea.y,
                paintArea.width, paintArea.height,
                arc, arc
            ));
            if (chatPassThroughArea != null) {
                overlayArea.subtract(new Area(chatPassThroughArea));
            }

            // 반투명 검은 배경
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fill(overlayArea);

            int textX = paintArea.x + (paintArea.width - textWidth) / 2;
            int textY = paintArea.y + (paintArea.height + fm.getAscent()) / 2;

            // 텍스트 외곽선 효과
            g2.setColor(new Color(0, 0, 0, 200));
            g2.drawString(message, textX + 2, textY + 2);
            g2.setColor(Color.WHITE);
            g2.drawString(message, textX, textY);

            g2.dispose();
        }
    }
}
