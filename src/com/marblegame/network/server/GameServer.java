package com.marblegame.network.server;

import com.marblegame.network.NetConstants;
import com.marblegame.network.protocol.Message;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 게임 서버
 * 클라이언트 연결을 수락하고 관리
 */
public class GameServer {
    private final int port;
    private final RoomManager roomManager;
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private Thread acceptThread;
    private volatile boolean running;
    private final List<ClientHandler> handlers;
    private GameServerListener listener;

    /**
     * GameServer 생성자
     * @param port 포트 번호
     * @param maxPlayers 최대 플레이어 수
     */
    public GameServer(int port, int maxPlayers) {
        this.port = port;
        this.roomManager = new RoomManager(maxPlayers);
        this.handlers = new ArrayList<>();
        this.running = false;
    }

    /**
     * 서버 시작
     * @throws IOException 서버 소켓 생성 실패 시
     */
    public void start() throws IOException {
        if (running) {
            System.out.println("서버가 이미 실행 중입니다.");
            return;
        }

        // 서버 소켓 생성
        serverSocket = new ServerSocket(port);
        executorService = Executors.newCachedThreadPool();
        running = true;

        // 로컬 IP 주소 출력
        String localIP = InetAddress.getLocalHost().getHostAddress();
        System.out.println("=================================");
        System.out.println("게임 서버 시작");
        System.out.println("포트: " + port);
        System.out.println("로컬 IP: " + localIP);
        System.out.println("최대 플레이어: " + roomManager.getMaxPlayers());
        System.out.println("=================================");

        // 클라이언트 수락 스레드 시작
        acceptThread = new Thread(this::acceptClients, "ServerAcceptThread");
        acceptThread.start();

        if (listener != null) {
            listener.onServerStarted(localIP, port);
        }
    }

    /**
     * 클라이언트 연결 수락
     */
    private void acceptClients() {
        System.out.println("클라이언트 연결 대기 중...");

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();

                // 방이 가득 찬 경우
                if (roomManager.isFull()) {
                    System.out.println("방이 가득 참. 연결 거부: " +
                        clientSocket.getInetAddress().getHostAddress());
                    clientSocket.close();
                    continue;
                }

                // ClientHandler 생성 및 실행
                ClientHandler handler = new ClientHandler(clientSocket, this, roomManager);
                handlers.add(handler);
                executorService.execute(handler);

            } catch (SocketException e) {
                if (running) {
                    System.err.println("소켓 에러: " + e.getMessage());
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("클라이언트 수락 중 오류: " + e.getMessage());
                }
            }
        }

        System.out.println("클라이언트 수락 종료");
    }

    /**
     * 클라이언트 메시지 처리
     * @param handler 클라이언트 핸들러
     * @param message 메시지
     */
    public void handleClientMessage(ClientHandler handler, Message message) {
        // 기본 처리: 모든 클라이언트에게 브로드캐스트
        // Phase 3에서 게임 로직과 통합 시 구체적으로 구현
        System.out.println("클라이언트 메시지 처리: " + message.getType() +
            " from " + handler.getPlayerId());

        // 리스너에 전달
        if (listener != null) {
            listener.onMessageReceived(handler.getPlayerId(), message);
        }
    }

    /**
     * 게임 시작 콜백
     */
    public void onGameStart() {
        System.out.println("게임 시작됨!");
        if (listener != null) {
            listener.onGameStarted();
        }
    }

    /**
     * 서버 종료
     */
    public void stop() {
        if (!running) {
            return;
        }

        System.out.println("서버 종료 중...");
        running = false;

        // 모든 클라이언트 연결 종료
        roomManager.disconnectAll();

        // 핸들러 정리
        for (ClientHandler handler : handlers) {
            if (handler.isRunning()) {
                handler.disconnect();
            }
        }
        handlers.clear();

        // ExecutorService 종료
        if (executorService != null) {
            executorService.shutdown();
        }

        // 서버 소켓 닫기
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("서버 소켓 종료 중 오류: " + e.getMessage());
        }

        // 수락 스레드 종료 대기
        if (acceptThread != null && acceptThread.isAlive()) {
            try {
                acceptThread.join(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (listener != null) {
            listener.onServerStopped();
        }

        System.out.println("서버 종료 완료");
    }

    /**
     * 모든 클라이언트에게 메시지 브로드캐스트
     * @param message 메시지
     */
    public void broadcast(Message message) {
        roomManager.broadcast(message);
    }

    /**
     * 특정 플레이어에게 메시지 전송
     * @param playerId 플레이어 ID
     * @param message 메시지
     */
    public void sendToPlayer(String playerId, Message message) {
        roomManager.sendToPlayer(playerId, message);
    }

    // Getters
    public int getPort() {
        return port;
    }

    public boolean isRunning() {
        return running;
    }

    public RoomManager getRoomManager() {
        return roomManager;
    }

    public void setListener(GameServerListener listener) {
        this.listener = listener;
    }

    /**
     * 서버 이벤트 리스너 인터페이스
     */
    public interface GameServerListener {
        void onServerStarted(String ipAddress, int port);
        void onServerStopped();
        void onGameStarted();
        void onMessageReceived(String playerId, Message message);
    }
}
