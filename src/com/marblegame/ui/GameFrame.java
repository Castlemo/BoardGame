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

    public GameFrame(Board board, List<Player> players) {
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

        // 수정됨: 좌측 - 플레이어 정보 패널
        infoPanel = new InfoPanel(players);
        JScrollPane playerScrollPane = new JScrollPane(infoPanel);
        playerScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        playerScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        playerScrollPane.setBorder(null);
        playerScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        playerScrollPane.setPreferredSize(new Dimension(300, 600));
        playerScrollPane.setMinimumSize(new Dimension(280, 400));

        // 추가됨: 중앙 - JLayeredPane (보드 + 오버레이)
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(800, 800));

        // 보드 패널 (DEFAULT_LAYER)
        boardPanel = new BoardPanel(board, players);
        boardPanel.setBounds(0, 0, 800, 800);
        layeredPane.add(boardPanel, JLayeredPane.DEFAULT_LAYER);

        // 오버레이 패널 (PALETTE_LAYER) - 보드 위에 겹침
        overlayPanel = new OverlayPanel();
        overlayPanel.setBounds(0, 0, 800, 800);
        layeredPane.add(overlayPanel, JLayeredPane.PALETTE_LAYER);

        // LayeredPane 리사이즈 처리
        layeredPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int w = layeredPane.getWidth();
                int h = layeredPane.getHeight();
                boardPanel.setBounds(0, 0, w, h);
                overlayPanel.setBounds(0, 0, w, h);
            }
        });

        // 수정됨: 하단 - 로그 패널만 (버튼 제거됨)
        controlPanel = new ControlPanel();
        JScrollPane controlScrollPane = new JScrollPane(controlPanel);
        controlScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        controlScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        controlScrollPane.setBorder(null);
        controlScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        controlScrollPane.setPreferredSize(new Dimension(1200, 180));

        // 레이아웃 배치
        add(playerScrollPane, BorderLayout.WEST);     // 좌측: 플레이어 정보
        add(layeredPane, BorderLayout.CENTER);        // 중앙: 보드 + 오버레이
        add(controlScrollPane, BorderLayout.SOUTH);   // 하단: 로그

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

    // 수정됨: ActionPanel은 제거되었으므로 deprecated
    @Deprecated
    public OverlayPanel getActionPanel() {
        return overlayPanel; // 하위 호환성을 위해 overlayPanel 반환
    }

    public void updateDisplay(int currentTurn) {
        boardPanel.updateBoard();
        infoPanel.updateInfo();                      // 수정됨: 턴은 오버레이에서 표시
        overlayPanel.setTurnNumber(currentTurn);     // 추가됨: 오버레이 턴 업데이트
    }
}
