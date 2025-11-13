package com.marblegame.session;

import com.marblegame.core.GameUI;
import com.marblegame.network.HostNetworkService;
import java.io.IOException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * 호스트 모드 세션. 네트워크 서버를 띄우고 GameUI를 구동한다.
 */
public class HostGameSession implements GameSession {
    private final int numPlayers;
    private final int initialCash;
    private final int port;

    private HostNetworkService networkService;
    private GameUI gameUI;

    public HostGameSession(int numPlayers, int initialCash, int port) {
        this.numPlayers = numPlayers;
        this.initialCash = initialCash;
        this.port = port;
    }

    @Override
    public void start() {
        try {
            networkService = new HostNetworkService(port);
            networkService.start();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                null,
                "호스트 서버를 시작할 수 없습니다:\n" + ex.getMessage(),
                "네트워크 오류",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        SwingUtilities.invokeLater(() -> gameUI = new GameUI(numPlayers, initialCash, networkService));
    }

    @Override
    public void stop() {
        if (gameUI != null) {
            // GameUI 자체는 닫기 버튼을 누르면 dispose 되므로 별도 처리는 추후 확장
        }
        if (networkService != null) {
            networkService.stop();
            networkService = null;
        }
    }
}
