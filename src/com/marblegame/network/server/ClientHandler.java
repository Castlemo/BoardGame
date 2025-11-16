package com.marblegame.network.server;

import com.marblegame.network.NetConstants;
import com.marblegame.network.protocol.Message;
import com.marblegame.network.protocol.MessageSerializer;
import com.marblegame.network.protocol.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 클라이언트 핸들러
 * 각 클라이언트의 소켓 통신을 처리하는 스레드
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final GameServer server;
    private final RoomManager roomManager;
    private BufferedReader in;
    private PrintWriter out;
    private String playerId;
    private volatile boolean running;
    private volatile long lastHeartbeat;
    private Timer heartbeatTimer;
    private volatile boolean awaitingPong;
    private volatile long lastPingSent;

    /**
     * ClientHandler 생성자
     * @param socket 클라이언트 소켓
     * @param server 게임 서버
     * @param roomManager 방 관리자
     */
    public ClientHandler(Socket socket, GameServer server, RoomManager roomManager) {
        this.socket = socket;
        this.server = server;
        this.roomManager = roomManager;
        this.running = true;
    }

    @Override
    public void run() {
        try {
            // 소켓 타임아웃 설정
            socket.setSoTimeout(NetConstants.SOCKET_TIMEOUT);

            // 입출력 스트림 초기화
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), NetConstants.CHARSET));
            out = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("클라이언트 연결됨: " + socket.getInetAddress().getHostAddress());
            lastHeartbeat = System.currentTimeMillis();
            startHeartbeatMonitor();

            // 메시지 수신 루프
            String line;
            while (running) {
                try {
                    line = in.readLine();
                    if (line == null) {
                        break;
                    }
                    handleMessage(line);
                } catch (SocketTimeoutException ste) {
                    if (System.currentTimeMillis() - lastHeartbeat > NetConstants.HEARTBEAT_TIMEOUT) {
                        System.out.println("Heartbeat timeout: " + playerId);
                        break;
                    }
                } catch (Exception e) {
                    System.err.println("메시지 처리 중 오류: " + e.getMessage());
                    e.printStackTrace();
                }
            }

        } catch (SocketException e) {
            if (running) {
                System.out.println("클라이언트 연결 종료: " + socket.getInetAddress().getHostAddress());
            }
        } catch (IOException e) {
            System.err.println("클라이언트 통신 오류: " + e.getMessage());
        } finally {
            stopHeartbeatMonitor();
            disconnect();
        }
    }

    /**
     * 메시지 처리
     * @param line 수신된 메시지 (JSON 문자열)
     */
    private void handleMessage(String line) {
        try {
            Message message = MessageSerializer.deserialize(line);
            System.out.println("메시지 수신: " + message.getType() + " from " + message.getPlayerId());
            refreshHeartbeat();

            // 메시지 타입별 처리
            switch (message.getType()) {
                case CONNECT:
                    handleConnect(message);
                    break;

                case PLAYER_JOIN:
                    handlePlayerJoin(message);
                    break;

                case DISCONNECT:
                    handleDisconnect(message);
                    break;

                case GAME_START:
                    handleGameStart(message);
                    break;

                case PING:
                    handlePing(message);
                    break;
                case PONG:
                    handlePong(message);
                    break;

                default:
                    // 다른 메시지는 서버에 전달
                    server.handleClientMessage(this, message);
                    break;
            }

        } catch (IllegalArgumentException e) {
            System.err.println("잘못된 메시지 형식: " + e.getMessage());
            sendError("Invalid message format");
        }
    }

    /**
     * 연결 요청 처리
     */
    private void handleConnect(Message message) {
        // 연결 승인 메시지 전송
        Message ack = new Message(MessageType.CONNECT_ACK);
        ack.addData("status", "connected");
        ack.addData("maxPlayers", roomManager.getMaxPlayers());
        ack.addData("currentPlayers", roomManager.getPlayerCount());
        sendMessage(ack);
    }

    /**
     * 플레이어 참가 처리
     */
    private void handlePlayerJoin(Message message) {
        String playerId = message.getPlayerId();
        String playerName = message.getString("name");

        if (playerId == null || playerName == null) {
            sendError("Player ID and name are required");
            return;
        }

        // 플레이어 추가
        boolean success = roomManager.addPlayer(playerId, playerName, this);

        if (success) {
            this.playerId = playerId;

            // 참가 성공 메시지 전송
            Message response = new Message(MessageType.PLAYER_JOIN, playerId);
            response.addData("status", "joined");
            response.addData("playerCount", roomManager.getPlayerCount());
            sendMessage(response);

            // 다른 플레이어들에게 알림
            Message notification = new Message(MessageType.PLAYER_JOIN, playerId);
            notification.addData("name", playerName);
            notification.addData("playerCount", roomManager.getPlayerCount());
            roomManager.broadcastExcept(notification, playerId);

        } else {
            sendError("Failed to join: room is full or game already started");
        }
    }

    /**
     * 연결 종료 처리
     */
    private void handleDisconnect(Message message) {
        disconnect();
    }

    /**
     * 게임 시작 처리
     */
    private void handleGameStart(Message message) {
        if (roomManager.canStartGame()) {
            roomManager.startGame();

            // 모든 플레이어에게 게임 시작 알림
            Message startMessage = new Message(MessageType.GAME_START);
            startMessage.addData("playerCount", roomManager.getPlayerCount());
            startMessage.addData("initialCash", NetConstants.DEFAULT_INITIAL_CASH);

            List<RoomManager.PlayerInfo> currentPlayers = roomManager.getPlayers();
            List<Map<String, Object>> playerData = new ArrayList<>();
            for (int i = 0; i < currentPlayers.size(); i++) {
                RoomManager.PlayerInfo info = currentPlayers.get(i);
                Map<String, Object> entry = new HashMap<>();
                entry.put("index", i);
                entry.put("playerId", info.getPlayerId());
                entry.put("name", info.getPlayerName());
                playerData.add(entry);
            }
            startMessage.addData("players", playerData);
            if (!currentPlayers.isEmpty()) {
                startMessage.addData("hostId", currentPlayers.get(0).getPlayerId());
            }
            roomManager.broadcast(startMessage);

            // 서버에 게임 시작 통보
            server.onGameStart();
        } else {
            sendError("Cannot start game: not enough players or game already started");
        }
    }

    /**
     * 핑 처리
     */
    private void handlePing(Message message) {
        refreshHeartbeat();
        Message pong = new Message(MessageType.PONG);
        pong.addData("timestamp", System.currentTimeMillis());
        sendMessage(pong);
    }

    private void handlePong(Message message) {
        refreshHeartbeat();
    }

    private void refreshHeartbeat() {
        lastHeartbeat = System.currentTimeMillis();
        awaitingPong = false;
    }

    private void startHeartbeatMonitor() {
        stopHeartbeatMonitor();
        heartbeatTimer = new Timer("ServerHeartbeat-" + socket.getPort(), true);
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkHeartbeat();
            }
        }, NetConstants.HEARTBEAT_INTERVAL, NetConstants.HEARTBEAT_INTERVAL);
    }

    private void stopHeartbeatMonitor() {
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
            heartbeatTimer = null;
        }
    }

    private void checkHeartbeat() {
        if (!running) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!awaitingPong && now - lastHeartbeat >= NetConstants.HEARTBEAT_INTERVAL) {
            sendHeartbeatPing();
        } else if (awaitingPong && now - lastPingSent >= NetConstants.HEARTBEAT_TIMEOUT) {
            System.out.println("하트비트 시간 초과: " +
                (playerId != null ? playerId : socket.getInetAddress().getHostAddress()));
            disconnect();
        }
    }

    private void sendHeartbeatPing() {
        Message ping = new Message(MessageType.PING);
        sendMessage(ping);
        awaitingPong = true;
        lastPingSent = System.currentTimeMillis();
    }

    /**
     * 메시지 전송
     * @param message 메시지
     */
    public void sendMessage(Message message) {
        if (out != null && !socket.isClosed()) {
            try {
                String json = MessageSerializer.serialize(message);
                out.println(json);
                System.out.println("메시지 전송: " + message.getType() + " to " +
                    (playerId != null ? playerId : socket.getInetAddress().getHostAddress()));
            } catch (Exception e) {
                System.err.println("메시지 전송 실패: " + e.getMessage());
            }
        }
    }

    /**
     * 에러 메시지 전송
     * @param errorMessage 에러 메시지
     */
    public void sendError(String errorMessage) {
        Message error = new Message(MessageType.ERROR);
        error.addData("message", errorMessage);
        sendMessage(error);
    }

    /**
     * 연결 종료
     */
    public void disconnect() {
        stopHeartbeatMonitor();
        if (!running) {
            return;
        }

        running = false;

        String disconnectedPlayerId = playerId;

        // 방에서 플레이어 제거
        if (playerId != null) {
            roomManager.removePlayer(playerId);

            // 다른 플레이어들에게 퇴장 알림
            Message leaveMessage = new Message(MessageType.PLAYER_LEAVE, playerId);
            leaveMessage.addData("playerCount", roomManager.getPlayerCount());
            roomManager.broadcast(leaveMessage);

            // 게임 중 연결 끊김 처리 (서버에 알림)
            server.onPlayerDisconnected(disconnectedPlayerId);
        }

        // 소켓 닫기
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("클라이언트 연결 종료: " +
                (playerId != null ? playerId : "unknown"));
        } catch (IOException e) {
            System.err.println("소켓 종료 중 오류: " + e.getMessage());
        }
    }

    // Getters
    public String getPlayerId() {
        return playerId;
    }

    public boolean isRunning() {
        return running;
    }
}
