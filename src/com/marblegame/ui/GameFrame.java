package com.marblegame.ui;

import com.marblegame.model.Board;
import com.marblegame.model.Player;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * 메인 게임 윈도우
 * 리팩토링: 좌측 플레이어 정보 + 중앙 보드(오버레이) + 하단 로그
 */
public class GameFrame extends JFrame {
    private BoardPanel boardPanel;
    private InfoPanel infoPanel;           // 수정됨: 플레이어 정보만 (좌측)
    private ControlPanel controlPanel;
    private OverlayPanel overlayPanel;     // 추가됨: 중앙 오버레이 (턴/주사위/버튼)
    private SocialPanel socialPanel;       // 추가됨: 우측 소셜 패널 (플레이어 요약 + 채팅)
    private boolean networkMode;           // 네트워크 모드 여부

    public GameFrame(Board board, List<Player> players) {
        this(board, players, false); // 기본값: 로컬 모드
    }

    public GameFrame(Board board, List<Player> players, boolean networkMode) {
        this.networkMode = networkMode;
        setTitle("모두의 마블 2.0 - Board Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true); // 크기 조절 가능

        // 다크 테마 배경
        getContentPane().setBackground(new Color(44, 62, 80));

        initComponents(board, players);

        pack();

        // 화면 크기 가져오기
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        // macOS Dock 높이 고려 (일반적으로 70~100px)
        int dockHeight = 80;
        int safeHeight = screenSize.height - dockHeight - 50; // 상단 메뉴바도 고려

        // 윈도우 크기 설정 (화면보다 약간 작게)
        int windowWidth = Math.min(getPreferredSize().width, screenSize.width - 100);
        int windowHeight = Math.min(getPreferredSize().height, safeHeight);

        setSize(windowWidth, windowHeight);

        // 화면 중앙에 배치 (Dock를 고려하여 약간 위로)
        setLocation(
            (screenSize.width - windowWidth) / 2,
            Math.max(0, (screenSize.height - windowHeight - dockHeight) / 2)
        );
    }

    private void initComponents(Board board, List<Player> players) {
        setLayout(new BorderLayout(10, 10));

        // 수정됨: 좌측 InfoPanel 제거 - 플레이어 정보는 오버레이에 표시
        infoPanel = new InfoPanel(players); // 하위 호환성을 위해 생성은 유지

        // 수정됨: 중앙 - JLayeredPane (보드 + 오버레이)
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(900, 900)); // 좌측 공간 확보로 더 넓게

        // 보드 패널 (DEFAULT_LAYER)
        boardPanel = new BoardPanel(board, players);
        boardPanel.setBounds(0, 0, 900, 900);
        layeredPane.add(boardPanel, JLayeredPane.DEFAULT_LAYER);

        // 오버레이 패널 (PALETTE_LAYER) - 보드 위에 겹침, 플레이어 정보 포함
        overlayPanel = new OverlayPanel(players, networkMode); // 네트워크 모드 전달
        overlayPanel.setBounds(0, 0, 900, 900);
        layeredPane.add(overlayPanel, JLayeredPane.PALETTE_LAYER);

        // LayeredPane 리사이즈 처리
        layeredPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int w = layeredPane.getWidth();
                int h = layeredPane.getHeight();
                boardPanel.setBounds(0, 0, w, h);
                overlayPanel.setBounds(0, 0, w, h);

                // BoardPanel이 repaint된 후 scaleFactor를 OverlayPanel에 전달
                SwingUtilities.invokeLater(() -> {
                    overlayPanel.setScaleFactor(boardPanel.getScaleFactor());
                });
            }
        });

        // 우측 소셜 패널 (플레이어 요약 + 채팅)
        socialPanel = new SocialPanel(players);

        // 수정됨: ControlPanel 제거 - 로그 UI 없음
        controlPanel = new ControlPanel(); // 하위 호환성을 위해 생성은 유지

        // 레이아웃 배치 (WEST, SOUTH 제거)
        add(layeredPane, BorderLayout.CENTER);        // 중앙: 보드 + 오버레이만 (플레이어 카드 포함)
        add(socialPanel, BorderLayout.EAST);          // 우측: 소셜 패널

        // 여백 추가
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    public BoardPanel getBoardPanel() {
        return boardPanel;
    }

    public InfoPanel getInfoPanel() {
        return infoPanel;
    }

    public ControlPanel getControlPanel() {
        return controlPanel;
    }

    // 추가됨: 오버레이 패널 getter
    public OverlayPanel getOverlayPanel() {
        return overlayPanel;
    }

    public SocialPanel getSocialPanel() {
        return socialPanel;
    }

    // 수정됨: ActionPanel은 제거되었으므로 deprecated
    @Deprecated
    public OverlayPanel getActionPanel() {
        return overlayPanel; // 하위 호환성을 위해 overlayPanel 반환
    }

    public void updateDisplay(int currentTurn, int currentPlayerIndex) {
        boardPanel.updateBoard();
        overlayPanel.updatePlayerInfo();             // 수정됨: 플레이어 정보는 오버레이에서 업데이트
        overlayPanel.setTurnNumber(currentTurn);     // 추가됨: 오버레이 턴 업데이트
        if (socialPanel != null) {
            socialPanel.updatePlayerInfo();
            socialPanel.setCurrentPlayerIndex(currentPlayerIndex);
        }
    }
}
