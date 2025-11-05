package com.marblegame.ui;

import com.marblegame.model.City;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * 랜드마크 건설 도시 선택 다이얼로그
 */
public class LandmarkSelectionDialog extends JDialog {
    private City selectedCity = null;
    private final List<City> availableCities;
    private final int playerCash;

    // 다크 테마 색상
    private static final Color BACKGROUND_DARK = new Color(32, 33, 36);
    private static final Color PANEL_DARK = new Color(44, 47, 51);
    private static final Color TEXT_PRIMARY = new Color(232, 234, 237);
    private static final Color TEXT_SECONDARY = new Color(189, 195, 199);
    private static final Color BUTTON_CONFIRM = new Color(39, 174, 96);  // 녹색
    private static final Color BUTTON_CANCEL = new Color(127, 140, 141); // 회색
    private static final Color BUTTON_DISABLED = new Color(60, 63, 65);  // 어두운 회색
    private static final Color CARD_BACKGROUND = new Color(44, 47, 51);
    private static final Color CARD_BORDER = new Color(149, 165, 166, 50);

    public LandmarkSelectionDialog(JFrame parent, List<City> availableCities, int playerCash) {
        super(parent, "랜드마크 건설", true); // modal dialog
        this.availableCities = availableCities;
        this.playerCash = playerCash;

        initComponents();
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(BACKGROUND_DARK);

        // 헤더 패널
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // 도시 목록 패널
        JPanel citiesPanel = createCitiesPanel();
        add(citiesPanel, BorderLayout.CENTER);

        // 버튼 패널
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * 헤더 패널 생성
     */
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(PANEL_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));

        // 제목
        JLabel titleLabel = new JLabel("🏛️ 랜드마크 건설");
        titleLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 20));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 안내 메시지
        JLabel messageLabel = new JLabel("랜드마크를 건설할 도시를 선택하세요");
        messageLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        messageLabel.setForeground(TEXT_SECONDARY);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 보유 자금
        JLabel cashLabel = new JLabel(String.format("💵 보유 자금: %,d원", playerCash));
        cashLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        cashLabel.setForeground(TEXT_SECONDARY);
        cashLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(messageLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(cashLabel);

        return panel;
    }

    /**
     * 도시 목록 패널 생성
     */
    private JPanel createCitiesPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        if (availableCities.isEmpty()) {
            // 건설 가능한 도시가 없는 경우
            JLabel noCitiesLabel = new JLabel("건설 가능한 도시가 없습니다 (레벨 3 필요)");
            noCitiesLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
            noCitiesLabel.setForeground(TEXT_SECONDARY);
            noCitiesLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(noCitiesLabel);
        } else {
            // 도시 카드들을 추가
            for (City city : availableCities) {
                JPanel cityCard = createCityCard(city);
                panel.add(cityCard);
                panel.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        }

        return panel;
    }

    /**
     * 도시 카드 생성
     */
    private JPanel createCityCard(City city) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout(15, 0));
        card.setBackground(CARD_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BORDER, 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        card.setMaximumSize(new Dimension(450, 80));

        // 왼쪽: 도시 정보
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(city.name);
        nameLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 16));
        nameLabel.setForeground(TEXT_PRIMARY);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        int constructionCost = (int)(city.price * 0.4);
        JLabel costLabel = new JLabel(String.format("건설 비용: %,d원", constructionCost));
        costLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        costLabel.setForeground(TEXT_SECONDARY);
        costLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel levelLabel = new JLabel("🏬 레벨 3 → 🏛️ 레벨 4");
        levelLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        levelLabel.setForeground(TEXT_SECONDARY);
        levelLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoPanel.add(nameLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        infoPanel.add(costLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 3)));
        infoPanel.add(levelLabel);

        // 오른쪽: 선택 버튼
        JButton selectButton = createButton("선택", BUTTON_CONFIRM);
        selectButton.setPreferredSize(new Dimension(100, 40));

        boolean canAfford = playerCash >= constructionCost;
        selectButton.setEnabled(canAfford);

        if (!canAfford) {
            selectButton.setBackground(BUTTON_DISABLED);
            selectButton.setForeground(new Color(150, 150, 150));
        }

        selectButton.addActionListener(e -> {
            selectedCity = city;
            dispose();
        });

        card.add(infoPanel, BorderLayout.CENTER);
        card.add(selectButton, BorderLayout.EAST);

        return card;
    }

    /**
     * 버튼 패널 생성
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        // 취소 버튼
        JButton cancelButton = createButton("취소", BUTTON_CANCEL);
        cancelButton.addActionListener(e -> {
            selectedCity = null;
            dispose();
        });

        panel.add(cancelButton);

        return panel;
    }

    /**
     * 버튼 생성
     */
    private JButton createButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        button.setPreferredSize(new Dimension(120, 40));
        button.setBackground(bgColor);
        button.setForeground(TEXT_PRIMARY);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 호버 효과
        Color hoverColor = bgColor.brighter();
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(hoverColor);
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(bgColor);
                }
            }
        });

        return button;
    }

    /**
     * 선택된 도시 반환
     */
    public City getSelectedCity() {
        return selectedCity;
    }
}
