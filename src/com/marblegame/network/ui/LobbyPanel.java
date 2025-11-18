package com.marblegame.network.ui;

import com.marblegame.network.NetConstants;
import com.marblegame.network.server.RoomManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * 게임 대기실 패널
 * 플레이어 목록 표시 및 게임 시작 버튼
 */
public class LobbyPanel extends JFrame {
    private JPanel playerListPanel;
    private JButton startButton;
    private JButton leaveButton;
    private final boolean isHost;
    private final int maxPlayers;
    private LobbyListener listener;
    private int currentPlayerCount = 0;

    // 다크 테마 색상
    private static final Color BACKGROUND_DARK = new Color(32, 33, 36);
    private static final Color PANEL_DARK = new Color(44, 47, 51);
    private static final Color TEXT_PRIMARY = new Color(232, 234, 237);
    private static final Color TEXT_SECONDARY = new Color(189, 195, 199);
    private static final Color BUTTON_START = new Color(46, 204, 113);      // 녹색
    private static final Color BUTTON_LEAVE = new Color(231, 76, 60);       // 빨간색
    private static final Color BUTTON_DISABLED = new Color(60, 63, 65);     // 어두운 회색
    private static final Color PLAYER_CARD_BG = new Color(55, 58, 64);
    private static final Color HIGHLIGHT_COLOR = new Color(155, 89, 182);   // 보라색

    public LobbyPanel(boolean isHost, int maxPlayers) {
        super("게임 대기실");
        this.isHost = isHost;
        this.maxPlayers = maxPlayers;

        initComponents();
        setSize(500, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // 창 닫기 이벤트
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                handleLeave();
            }
        });
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(BACKGROUND_DARK);

        // 헤더 패널
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // 플레이어 목록 스크롤 패널
        JScrollPane scrollPane = createPlayerListScrollPane();
        add(scrollPane, BorderLayout.CENTER);

        // 버튼 패널
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(PANEL_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(25, 20, 20, 20));

        JLabel titleLabel = new JLabel("⏳ 게임 대기실");
        titleLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 28));
        titleLabel.setForeground(HIGHLIGHT_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel roleLabel = new JLabel(isHost ? "호스트" : "참가자");
        roleLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        roleLabel.setForeground(isHost ? new Color(52, 152, 219) : new Color(46, 204, 113));
        roleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("플레이어들이 모이기를 기다리는 중...");
        subtitleLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        subtitleLabel.setForeground(TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(roleLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(subtitleLabel);

        return panel;
    }

    private JScrollPane createPlayerListScrollPane() {
        playerListPanel = new JPanel();
        playerListPanel.setLayout(new BoxLayout(playerListPanel, BoxLayout.Y_AXIS));
        playerListPanel.setBackground(BACKGROUND_DARK);
        playerListPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JScrollPane scrollPane = new JScrollPane(playerListPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BACKGROUND_DARK);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        return scrollPane;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));
        panel.setBackground(BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 25, 20));

        // 게임 시작 버튼 (호스트만)
        if (isHost) {
            startButton = createButton("게임 시작", BUTTON_START);
            startButton.setEnabled(false); // 초기에는 비활성화
            startButton.addActionListener(e -> handleStart());
            panel.add(startButton);
        }

        // 나가기 버튼
        leaveButton = createButton("나가기", BUTTON_LEAVE);
        leaveButton.addActionListener(e -> handleLeave());
        panel.add(leaveButton);

        return panel;
    }

    private JButton createButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        button.setPreferredSize(new Dimension(140, 45));
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
                } else {
                    button.setBackground(BUTTON_DISABLED);
                }
            }
        });

        return button;
    }

    /**
     * 플레이어 목록 업데이트
     * @param players 플레이어 정보 목록
     */
    public void updatePlayerList(List<RoomManager.PlayerInfo> players) {
        SwingUtilities.invokeLater(() -> {
            playerListPanel.removeAll();

            // 플레이어 카드 추가
            for (int i = 0; i < players.size(); i++) {
                RoomManager.PlayerInfo player = players.get(i);
                JPanel playerCard = createPlayerCard(player.getPlayerName(), i == 0);
                playerListPanel.add(playerCard);
                if (i < players.size() - 1) {
                    playerListPanel.add(Box.createVerticalStrut(10));
                }
            }

            // 빈 슬롯 추가
            for (int i = players.size(); i < maxPlayers; i++) {
                JPanel emptyCard = createEmptyCard();
                playerListPanel.add(Box.createVerticalStrut(10));
                playerListPanel.add(emptyCard);
            }

            playerListPanel.revalidate();
            playerListPanel.repaint();

            updatePlayerCountInternal(players.size());
        });
    }

    public void updatePlayerCount(int playerCount) {
        SwingUtilities.invokeLater(() -> updatePlayerCountInternal(playerCount));
    }

    private void updatePlayerCountInternal(int playerCount) {
        this.currentPlayerCount = playerCount;
        if (isHost && startButton != null) {
            boolean canStart = currentPlayerCount >= NetConstants.MIN_PLAYERS;
            startButton.setEnabled(canStart);
            startButton.setBackground(canStart ? BUTTON_START : BUTTON_DISABLED);
        }
    }

    private JPanel createPlayerCard(String playerName, boolean isHostPlayer) {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(PLAYER_CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 73, 79), 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        // 플레이어 아이콘
        JLabel iconLabel = new JLabel(isHostPlayer ? "★" : "○");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));

        // 플레이어 이름
        JLabel nameLabel = new JLabel(playerName);
        nameLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 16));
        nameLabel.setForeground(TEXT_PRIMARY);

        // 역할 라벨
        JLabel roleLabel = new JLabel(isHostPlayer ? "호스트" : "플레이어");
        roleLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        roleLabel.setForeground(isHostPlayer ? new Color(255, 193, 7) : TEXT_SECONDARY);

        // 이름과 역할을 담을 패널
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.add(nameLabel);
        infoPanel.add(Box.createVerticalStrut(3));
        infoPanel.add(roleLabel);

        card.add(iconLabel, BorderLayout.WEST);
        card.add(infoPanel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createEmptyCard() {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(new Color(40, 42, 46));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createDashedBorder(new Color(80, 83, 89), 2, 5, 5, false),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        // 빈 슬롯 아이콘
        JLabel iconLabel = new JLabel("⏺");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        iconLabel.setForeground(new Color(100, 103, 109));

        // 빈 슬롯 텍스트
        JLabel emptyLabel = new JLabel("대기 중...");
        emptyLabel.setFont(new Font("Malgun Gothic", Font.ITALIC, 14));
        emptyLabel.setForeground(new Color(120, 123, 129));

        card.add(iconLabel, BorderLayout.WEST);
        card.add(emptyLabel, BorderLayout.CENTER);

        return card;
    }

    private void handleStart() {
        if (listener != null) {
            listener.onGameStart();
        }
    }

    private void handleLeave() {
        int result = JOptionPane.showConfirmDialog(
            this,
            "대기실을 나가시겠습니까?",
            "확인",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            if (listener != null) {
                listener.onLeave();
            }
            dispose();
        }
    }

    public void setLobbyListener(LobbyListener listener) {
        this.listener = listener;
    }

    /**
     * 로비 이벤트 리스너 인터페이스
     */
    public interface LobbyListener {
        void onGameStart();
        void onLeave();
    }
}
