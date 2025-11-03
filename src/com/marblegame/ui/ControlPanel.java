package com.marblegame.ui;

import com.marblegame.model.DiceGauge;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 게임 컨트롤 및 로그를 표시하는 패널
 * 더 인터랙티브하고 눈에 띄는 디자인
 */
public class ControlPanel extends JPanel {
    private static final int BASE_WIDTH = 1000;
    private static final int BASE_HEIGHT = 250;
    private static final int BASE_BUTTON_WIDTH = 300;
    private static final int BASE_BUTTON_HEIGHT = 55;

    private JTextArea logArea;
    private JButton rollDiceButton;
    private JButton purchaseButton;
    private JButton upgradeButton;
    private JButton takeoverButton;
    private JButton skipButton;
    private JButton escapeButton;

    private JLabel logTitleLabel;
    private JLabel controlTitleLabel;

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

    private final List<ActionButtonCard> buttonCards = new ArrayList<>();
    private final List<JComponent> spacers = new ArrayList<>();

    private Font baseLogTitleFont;
    private Font baseLogFont;
    private Font baseControlTitleFont;
    private Font baseButtonFont;
    private double scaleFactor = 1.0;

    public ControlPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(44, 62, 80));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        initComponents();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                applyScale();
            }
        });

        applyScale();
    }

    private void initComponents() {
        // 로그 영역 (좌측)
        logPanel = createLogPanel();

        // 컨트롤 버튼 패널 (우측) - 동적으로 변경 가능하게 생성
        JPanel controlPanel = createControlPanelContainer();

        add(logPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.EAST);

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
            BorderFactory.createLineBorder(new Color(52, 152, 219), 2),
            new EmptyBorder(6, 6, 6, 6)
        ));

        // 제목
        logTitleLabel = new JLabel("📋 게임 로그");
        logTitleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        logTitleLabel.setForeground(new Color(236, 240, 241));
        logTitleLabel.setBorder(new EmptyBorder(0, 0, 6, 0));

        logArea = new JTextArea(6, 40);
        logArea.setEditable(false);
        logArea.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        logArea.setBackground(new Color(52, 73, 94));
        logArea.setForeground(new Color(236, 240, 241));
        logArea.setCaretColor(Color.WHITE);
        logArea.setBorder(new EmptyBorder(6, 6, 6, 6));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(null);

        panel.add(logTitleLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        baseLogTitleFont = logTitleLabel.getFont();
        baseLogFont = logArea.getFont();

        return panel;
    }

    private JPanel createControlPanelContainer() {
        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.setBackground(new Color(44, 62, 80));
        outerPanel.setPreferredSize(new Dimension(220, 180));

        // 제목
        controlTitleLabel = new JLabel("🎮 행동 선택");
        controlTitleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        controlTitleLabel.setForeground(new Color(236, 240, 241));
        controlTitleLabel.setBorder(new EmptyBorder(0, 0, 8, 0));

        // 동적 버튼 컨테이너
        buttonContainer = new JPanel();
        buttonContainer.setLayout(new BoxLayout(buttonContainer, BoxLayout.Y_AXIS));
        buttonContainer.setBackground(new Color(44, 62, 80));

        outerPanel.add(controlTitleLabel, BorderLayout.NORTH);
        outerPanel.add(buttonContainer, BorderLayout.CENTER);

        baseControlTitleFont = controlTitleLabel.getFont();

        return outerPanel;
    }

    private ActionButtonCard createButtonCard(JButton button, Color accentColor) {
        ActionButtonCard card = new ActionButtonCard(button, accentColor);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonCards.add(card);
        if (baseButtonFont == null) {
            baseButtonFont = button.getFont();
        }
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
        spacers.clear();
        buttonCards.clear();

        // 활성화된 버튼만 추가
        if (roll) addButtonToContainer(rollDiceButton, rollDiceColor);
        if (purchase) addButtonToContainer(purchaseButton, purchaseColor);
        if (upgrade) addButtonToContainer(upgradeButton, upgradeColor);
        if (takeover) addButtonToContainer(takeoverButton, takeoverColor);
        if (skip) addButtonToContainer(skipButton, skipColor);
        if (escape) addButtonToContainer(escapeButton, escapeColor);

        if (!spacers.isEmpty()) {
            JComponent lastSpacer = spacers.remove(spacers.size() - 1);
            buttonContainer.remove(lastSpacer);
        }

        // UI 갱신
        buttonContainer.revalidate();
        buttonContainer.repaint();

        applyScale();
    }

    /**
     * 버튼을 컨테이너에 추가
     */
    private void addButtonToContainer(JButton button, Color accentColor) {
        ActionButtonCard card = createButtonCard(button, accentColor);
        buttonContainer.add(card);

        JComponent spacer = (JComponent) Box.createVerticalStrut(8);
        spacer.putClientProperty("baseSpacing", 8);
        spacers.add(spacer);
        buttonContainer.add(spacer);
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
     * 주사위 버튼 접근자 (press-and-hold 이벤트용)
     */
    public JButton getRollDiceButton() {
        return rollDiceButton;
    }

    private void applyScale() {
        if (baseLogTitleFont == null || baseLogFont == null || baseControlTitleFont == null) {
            return;
        }

        double width = Math.max(1, getWidth());
        double height = Math.max(1, getHeight());
        double scaleX = width / BASE_WIDTH;
        double scaleY = height / BASE_HEIGHT;
        scaleFactor = Math.max(0.5, Math.min(scaleX, scaleY)) * (2.0 / 3.0);

        float titleSize = (float) Math.max(10f, baseLogTitleFont.getSize2D() * scaleFactor);
        logTitleLabel.setFont(baseLogTitleFont.deriveFont(titleSize));
        controlTitleLabel.setFont(baseControlTitleFont.deriveFont((float) Math.max(11f, baseControlTitleFont.getSize2D() * scaleFactor)));

        logArea.setFont(baseLogFont.deriveFont((float) Math.max(10f, baseLogFont.getSize2D() * scaleFactor)));
        int padding = Math.max(6, (int) Math.round(10 * scaleFactor));
        logArea.setBorder(new EmptyBorder(padding, padding, padding, padding));

        int borderThickness = Math.max(1, (int) Math.round(3 * scaleFactor));
        logPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(52, 152, 219), borderThickness),
            new EmptyBorder(padding, padding, padding, padding)
        ));

        for (ActionButtonCard card : buttonCards) {
            card.applyScale(scaleFactor, baseButtonFont, BASE_BUTTON_WIDTH, BASE_BUTTON_HEIGHT);
        }

        for (JComponent spacer : spacers) {
            Object base = spacer.getClientProperty("baseSpacing");
            if (base instanceof Integer) {
                int scaled = Math.max(4, (int) Math.round((Integer) base * scaleFactor));
                spacer.setPreferredSize(new Dimension(0, scaled));
                spacer.setMinimumSize(new Dimension(0, scaled));
            }
        }

        revalidate();
        repaint();
    }

    private static class ActionButtonCard extends JPanel {
        private final JButton button;
        private final JPanel accentBar;
        private final Color accentColor;

        ActionButtonCard(JButton button, Color accentColor) {
            super(new BorderLayout());
            this.button = button;
            this.accentColor = accentColor;
            setBackground(new Color(52, 73, 94));

            accentBar = new JPanel();
            accentBar.setBackground(accentColor);

            button.setEnabled(true);
            button.setBackground(new Color(52, 73, 94));
            button.setForeground(Color.WHITE);
            button.setBorder(new EmptyBorder(5, 10, 5, 10));

            add(accentBar, BorderLayout.WEST);
            add(button, BorderLayout.CENTER);
        }

        void applyScale(double scale, Font baseFont, int baseWidth, int baseHeight) {
            int accentWidth = Math.max(2, (int) Math.round(5 * scale));
            int height = Math.max(30, (int) Math.round(baseHeight * scale));

            accentBar.setPreferredSize(new Dimension(accentWidth, height));
            accentBar.setMinimumSize(new Dimension(accentWidth, height));
            accentBar.setBackground(accentColor);

            int borderThickness = Math.max(1, (int) Math.round(3 * scale));
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accentColor, borderThickness),
                new EmptyBorder(0, 0, 0, 0)
            ));

            Font base = baseFont != null ? baseFont : button.getFont();
            float fontSize = (float) Math.max(10f, base.getSize2D() * (float) scale);
            button.setFont(base.deriveFont(fontSize));

            int paddingV = Math.max(4, (int) Math.round(6 * scale));
            int paddingH = Math.max(8, (int) Math.round(12 * scale));
            button.setBorder(new EmptyBorder(paddingV, paddingH, paddingV, paddingH));

            int preferredWidth = Math.max(120, (int) Math.round(baseWidth * scale));
            button.setPreferredSize(new Dimension(preferredWidth, height));
            button.setMinimumSize(new Dimension(preferredWidth, height));

            setPreferredSize(new Dimension(preferredWidth + accentWidth, height));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        }
    }
}
