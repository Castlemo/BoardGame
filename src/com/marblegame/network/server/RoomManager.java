package com.marblegame.network.server;

import com.marblegame.network.NetConstants;
import com.marblegame.network.protocol.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 게임 방 관리 클래스
 * 플레이어 목록 관리 및 브로드캐스트 기능 제공
 */
public class RoomManager {
    private final Map<String, ClientHandler> clients;
    private final List<PlayerInfo> players;
    private final int maxPlayers;
    private boolean gameStarted;

    /**
     * RoomManager 생성자
     * @param maxPlayers 최대 플레이어 수
     */
    public RoomManager(int maxPlayers) {
        this.clients = new ConcurrentHashMap<>();
        this.players = new ArrayList<>();
        this.maxPlayers = Math.min(maxPlayers, NetConstants.MAX_PLAYERS);
        this.gameStarted = false;
    }

    /**
     * 플레이어 추가
     * @param playerId 플레이어 ID
     * @param playerName 플레이어 이름
     * @param handler 클라이언트 핸들러
     * @return 성공 여부
     */
    public synchronized boolean addPlayer(String playerId, String playerName, ClientHandler handler) {
        if (gameStarted) {
            System.out.println("게임이 이미 시작되어 플레이어를 추가할 수 없습니다.");
            return false;
        }

        if (clients.size() >= maxPlayers) {
            System.out.println("방이 가득 찼습니다. (최대 " + maxPlayers + "명)");
            return false;
        }

        if (clients.containsKey(playerId)) {
            System.out.println("이미 존재하는 플레이어 ID입니다: " + playerId);
            return false;
        }

        clients.put(playerId, handler);
        players.add(new PlayerInfo(playerId, playerName));
        System.out.println("플레이어 추가: " + playerName + " (ID: " + playerId + ")");
        System.out.println("현재 플레이어 수: " + clients.size() + "/" + maxPlayers);

        return true;
    }

    /**
     * 플레이어 제거
     * @param playerId 플레이어 ID
     */
    public synchronized void removePlayer(String playerId) {
        ClientHandler handler = clients.remove(playerId);
        if (handler != null) {
            players.removeIf(p -> p.getPlayerId().equals(playerId));
            System.out.println("플레이어 제거: " + playerId);
            System.out.println("현재 플레이어 수: " + clients.size() + "/" + maxPlayers);
        }
    }

    /**
     * 모든 클라이언트에게 메시지 브로드캐스트
     * @param message 메시지
     */
    public void broadcast(Message message) {
        for (ClientHandler handler : clients.values()) {
            handler.sendMessage(message);
        }
    }

    /**
     * 특정 플레이어를 제외한 모든 클라이언트에게 메시지 브로드캐스트
     * @param message 메시지
     * @param excludePlayerId 제외할 플레이어 ID
     */
    public void broadcastExcept(Message message, String excludePlayerId) {
        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            if (!entry.getKey().equals(excludePlayerId)) {
                entry.getValue().sendMessage(message);
            }
        }
    }

    /**
     * 특정 플레이어에게 메시지 전송
     * @param playerId 플레이어 ID
     * @param message 메시지
     */
    public void sendToPlayer(String playerId, Message message) {
        ClientHandler handler = clients.get(playerId);
        if (handler != null) {
            handler.sendMessage(message);
        }
    }

    /**
     * 게임 시작 가능 여부 확인
     * @return 시작 가능 여부
     */
    public synchronized boolean canStartGame() {
        return clients.size() >= NetConstants.MIN_PLAYERS && !gameStarted;
    }

    /**
     * 게임 시작
     */
    public synchronized void startGame() {
        if (canStartGame()) {
            gameStarted = true;
            System.out.println("게임 시작! 플레이어 수: " + clients.size());
        }
    }

    /**
     * 게임 종료
     */
    public synchronized void endGame() {
        gameStarted = false;
        System.out.println("게임 종료");
    }

    /**
     * 모든 클라이언트 연결 종료
     */
    public synchronized void disconnectAll() {
        System.out.println("모든 클라이언트 연결 종료 중...");
        for (ClientHandler handler : clients.values()) {
            handler.disconnect();
        }
        clients.clear();
        players.clear();
    }

    // Getters
    public int getPlayerCount() {
        return clients.size();
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public List<PlayerInfo> getPlayers() {
        return new ArrayList<>(players);
    }

    public boolean isFull() {
        return clients.size() >= maxPlayers;
    }

    /**
     * 플레이어 정보 클래스
     */
    public static class PlayerInfo {
        private final String playerId;
        private final String playerName;

        public PlayerInfo(String playerId, String playerName) {
            this.playerId = playerId;
            this.playerName = playerName;
        }

        public String getPlayerId() {
            return playerId;
        }

        public String getPlayerName() {
            return playerName;
        }

        @Override
        public String toString() {
            return playerName + " (ID: " + playerId + ")";
        }
    }
}
