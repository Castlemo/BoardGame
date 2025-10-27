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

    public GameFrame(Board board, List<Player> players) {
        setTitle("모두의 마블 2.0 - Board Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // 다크 테마 배경
        getContentPane().setBackground(new Color(44, 62, 80));

        initComponents(board, players);

        pack();
        setLocationRelativeTo(null);
    }

    private void initComponents(Board board, List<Player> players) {
        setLayout(new BorderLayout(10, 10));

        // 보드 패널 (중앙)
        boardPanel = new BoardPanel(board, players);

        // 정보 패널 (우측)
        infoPanel = new InfoPanel(players);

        // 컨트롤 패널 (하단)
        controlPanel = new ControlPanel();

        // 레이아웃 배치
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(boardPanel, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
        add(infoPanel, BorderLayout.EAST);
        add(controlPanel, BorderLayout.SOUTH);

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

    public void updateDisplay(int currentTurn) {
        boardPanel.updateBoard();
        infoPanel.updateInfo(currentTurn);
    }
}
