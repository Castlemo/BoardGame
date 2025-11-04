package com.marblegame;

import com.marblegame.core.GameUI;
import javax.swing.SwingUtilities;

/**
 * 게임 진입점
 */
public class Main {
    public static void main(String[] args) {
        // 게임 설정
        int numPlayers = 2;
        int initialCash = 1500000;

        // Swing UI는 EDT(Event Dispatch Thread)에서 실행
        SwingUtilities.invokeLater(() -> {
            new GameUI(numPlayers, initialCash);
        });
    }
}
