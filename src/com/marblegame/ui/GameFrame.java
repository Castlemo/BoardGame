package com.marblegame.ui;

import com.marblegame.model.Board;
import com.marblegame.model.Player;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * 메인 게임 윈도우
 */
public class GameFrame extends JFrame {
    private BoardPanel boardPanel;
    private InfoPanel infoPanel;
    private ControlPanel controlPanel;
    private ActionPanel actionPanel;

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

        // 보드 패널 (좌측)
        boardPanel = new BoardPanel(board, players);
        JPanel boardContainer = new JPanel(new GridBagLayout());
        boardContainer.setBackground(new Color(44, 62, 80));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        boardContainer.add(boardPanel, gbc);

        // 우측 패널 (정보 + 액션)
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBackground(new Color(44, 62, 80));
        rightPanel.setMinimumSize(new Dimension(320, 400));

        // 정보 패널 (우측 상단)
        infoPanel = new InfoPanel(players);

        // 액션 패널 (우측 하단) - 고정
        actionPanel = new ActionPanel();

        GridBagConstraints rightGbc = new GridBagConstraints();
        rightGbc.gridx = 0;
        rightGbc.gridy = 0;
        rightGbc.weightx = 1.0;
        rightGbc.weighty = 0.65;
        rightGbc.fill = GridBagConstraints.BOTH;
        rightPanel.add(infoPanel, rightGbc);

        rightGbc.gridy = 1;
        rightGbc.weighty = 0.35;
        rightGbc.insets = new Insets(10, 0, 0, 0);
        rightPanel.add(actionPanel, rightGbc);

        // 컨트롤 패널 (하단) - 스크롤 가능
        controlPanel = new ControlPanel();
        JScrollPane controlScrollPane = new JScrollPane(controlPanel);
        controlScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        controlScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        controlScrollPane.setBorder(null);
        controlScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // 레이아웃 배치
        add(boardContainer, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
        add(controlScrollPane, BorderLayout.SOUTH);

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

    public ActionPanel getActionPanel() {
        return actionPanel;
    }

    public void updateDisplay(int currentTurn) {
        boardPanel.updateBoard();
        infoPanel.updateInfo(currentTurn);
    }
}
