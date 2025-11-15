package com.marblegame.network.client;

import com.marblegame.network.NetConstants;
import com.marblegame.network.protocol.Message;
import com.marblegame.network.protocol.MessageSerializer;
import com.marblegame.network.protocol.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;

/**
 * 게임 클라이언트
 * 서버에 연결하고 메시지를 송수신
 */
public class GameClient {
    private final String serverIP;
    private final int serverPort;
    private final String playerId;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ServerListener serverListener;
    private Thread listenerThread;
    private volatile boolean connected;
    private GameClientListener listener;

    /**
     * GameClient 생성자
     * @param serverIP 서버 IP 주소
     * @param serverPort 서버 포트
     * @param playerId 플레이어 ID
     */
    public GameClient(String serverIP, int serverPort, String playerId) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.playerId = playerId;
        this.connected = false;
    }

    /**
     * 서버 연결
     * @return 연결 성공 여부
     */
    public boolean connect() {
        try {
            System.out.println("서버 연결 시도: " + serverIP + ":" + serverPort);

            // 소켓 생성 및 연결 (타임아웃 설정)
            socket = new Socket();
            socket.connect(
                new java.net.InetSocketAddress(serverIP, serverPort),
                NetConstants.CONNECTION_TIMEOUT
            );
            socket.setSoTimeout(NetConstants.SOCKET_TIMEOUT);

            // 입출력 스트림 초기화
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), NetConstants.CHARSET));
            out = new PrintWriter(socket.getOutputStream(), true);

            connected = true;

            System.out.println("서버 연결 성공!");

            // 연결 메시지 전송
            Message connectMsg = new Message(MessageType.CONNECT, playerId);
            sendMessage(connectMsg);

            // 서버 리스너 시작
            serverListener = new ServerListener(in, this);
            listenerThread = new Thread(serverListener, "ServerListenerThread");
            listenerThread.start();

            if (listener != null) {
                listener.onConnected();
            }

            return true;

        } catch (SocketTimeoutException e) {
            System.err.println("서버 연결 타임아웃: " + e.getMessage());
            if (listener != null) {
                listener.onConnectionFailed("연결 타임아웃");
            }
            return false;

        } catch (IOException e) {
            System.err.println("서버 연결 실패: " + e.getMessage());
            if (listener != null) {
                listener.onConnectionFailed(e.getMessage());
            }
            return false;
        }
    }

    /**
     * 방 참가 요청
     * @param playerName 플레이어 이름
     */
    public void joinRoom(String playerName) {
        Message joinMsg = new Message(MessageType.PLAYER_JOIN, playerId);
        joinMsg.addData("name", playerName);
        sendMessage(joinMsg);
    }

    /**
     * 게임 시작 요청 (호스트만)
     */
    public void requestGameStart() {
        Message startMsg = new Message(MessageType.GAME_START, playerId);
        sendMessage(startMsg);
    }

    /**
     * 메시지 전송
     * @param message 메시지
     */
    public void sendMessage(Message message) {
        if (!connected || out == null) {
            System.err.println("서버에 연결되지 않음");
            return;
        }

        try {
            String json = MessageSerializer.serialize(message);
            out.println(json);
            System.out.println("메시지 전송: " + message.getType());
        } catch (Exception e) {
            System.err.println("메시지 전송 실패: " + e.getMessage());
        }
    }

    public void sendAction(MessageType type, Map<String, Object> payload) {
        Message message = new Message(type, playerId);
        if (payload != null) {
            for (Map.Entry<String, Object> entry : payload.entrySet()) {
                message.addData(entry.getKey(), entry.getValue());
            }
        }
        sendMessage(message);
    }

    /**
     * 서버로부터 메시지 수신 (ServerListener가 호출)
     * @param message 메시지
     */
    void onMessageReceived(Message message) {
        System.out.println("메시지 수신: " + message.getType());

        // 리스너에 전달
        if (listener != null) {
            listener.onMessageReceived(message);
        }
    }

    /**
     * 연결 종료 콜백 (ServerListener가 호출)
     */
    void onDisconnected() {
        if (connected) {
            connected = false;
            System.out.println("서버 연결 종료됨");

            if (listener != null) {
                listener.onDisconnected();
            }
        }
    }

    /**
     * 연결 종료
     */
    public void disconnect() {
        if (!connected) {
            return;
        }

        System.out.println("서버 연결 종료 중...");

        // 종료 메시지 전송
        Message disconnectMsg = new Message(MessageType.DISCONNECT, playerId);
        sendMessage(disconnectMsg);

        connected = false;

        // 리스너 종료
        if (serverListener != null) {
            serverListener.stop();
        }

        // 리스너 스레드 종료 대기
        if (listenerThread != null && listenerThread.isAlive()) {
            try {
                listenerThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 소켓 닫기
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("소켓 종료 중 오류: " + e.getMessage());
        }

        System.out.println("서버 연결 종료 완료");
    }

    // Getters
    public boolean isConnected() {
        return connected;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setListener(GameClientListener listener) {
        this.listener = listener;
    }

    /**
     * 클라이언트 이벤트 리스너 인터페이스
     */
    public interface GameClientListener {
        void onConnected();
        void onConnectionFailed(String reason);
        void onDisconnected();
        void onMessageReceived(Message message);
    }
}
