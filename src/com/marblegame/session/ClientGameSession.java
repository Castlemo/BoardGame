package com.marblegame.session;

import com.marblegame.network.ClientNetworkService;
import com.marblegame.network.message.MessageType;
import com.marblegame.network.message.NetworkMessage;
import com.marblegame.core.RemoteGameUI;
import java.io.IOException;
import javax.swing.JOptionPane;

/**
 * 클라이언트 모드 세션. 호스트에 접속하고 대기 화면을 띄운다.
 * 실제 게임 플레이 UI는 추후 추가된다.
 */
public class ClientGameSession implements GameSession {
    private final String host;
    private final int port;

    private ClientNetworkService clientService;
    private RemoteGameUI remoteUI;

    public ClientGameSession(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void start() {
        clientService = new ClientNetworkService(host, port);
        try {
            clientService.connect();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                null,
                "호스트에 연결할 수 없습니다:\n" + ex.getMessage(),
                "연결 실패",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        if (!clientService.send(new NetworkMessage(MessageType.LOG_ENTRY, "클라이언트가 접속했습니다."))) {
            JOptionPane.showMessageDialog(
                null,
                "호스트에 초기 메시지를 전송하지 못했습니다. 연결이 끊어졌을 수 있습니다.",
                "전송 실패",
                JOptionPane.ERROR_MESSAGE
            );
            clientService.disconnect();
            return;
        }

        remoteUI = new RemoteGameUI(clientService, () -> clientService.disconnect());
    }

    @Override
    public void stop() {
        if (remoteUI != null) {
            remoteUI.dispose();
            remoteUI = null;
        }
        if (clientService != null) {
            clientService.disconnect();
            clientService = null;
        }
    }
}
