package com.marblegame;

import com.marblegame.ui.LauncherFrame;
import javax.swing.SwingUtilities;

/**
 * 애플리케이션 진입점.
 * 실행 즉시 호스트/클라이언트 모드를 고르는 런처를 띄운다.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LauncherFrame launcher = new LauncherFrame();
            launcher.setVisible(true);
        });
    }
}
