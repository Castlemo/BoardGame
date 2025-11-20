package com.marblegame;

import com.marblegame.core.GameUI;
import com.marblegame.network.client.GameClient;
import com.marblegame.network.protocol.Message;
import com.marblegame.network.protocol.MessageType;
import com.marblegame.network.server.GameServer;
import com.marblegame.network.server.RoomManager;
import com.marblegame.network.ui.CreateRoomDialog;
import com.marblegame.network.ui.JoinRoomDialog;
import com.marblegame.network.ui.LobbyPanel;
import com.marblegame.network.ui.NetworkMenuDialog;
import com.marblegame.network.NetConstants;
import com.marblegame.network.sync.GameStateMapper;
import com.marblegame.network.sync.GameStateSnapshot;
import com.marblegame.ui.ChatPanel;
import com.marblegame.ui.GameModeDialog;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 게임 진입점
 * 로컬 게임 또는 네트워크 멀티플레이 선택
 */
public class Main {
    private static GameServer server;
    private static GameClient client;
    private static LobbyPanel lobbyPanel;
    private static GameUI gameUI;
    private static boolean networkHost;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            showGameModeSelection();
        });
    }

    /**
     * 게임 모드 선택
     */
    private static void showGameModeSelection() {
        GameModeDialog modeDialog = new GameModeDialog(null);
        modeDialog.setVisible(true);

        int choice = modeDialog.getChoice();

        if (choice == 1) {
            // 로컬 게임
            startLocalGame();
        } else if (choice == 2) {
            // 네트워크 멀티플레이
            showNetworkMenu();
        } else {
            // 취소
            System.exit(0);
        }
    }

    /**
     * 로컬 게임 시작
     */
    private static void startLocalGame() {
        // 기존 로컬 게임 로직
        int numPlayers = 2;
        int initialCash = 3000000;

        SwingUtilities.invokeLater(() -> {
            new GameUI(numPlayers, initialCash);
        });
    }

    /**
     * 네트워크 메뉴 표시
     */
    private static void showNetworkMenu() {
        NetworkMenuDialog menuDialog = new NetworkMenuDialog(null);
        menuDialog.setVisible(true);

        int choice = menuDialog.getChoice();

        if (choice == 1) {
            // 방 만들기
            createRoom();
        } else if (choice == 2) {
            // 방 참가
            joinRoom();
        } else {
            // 취소 - 모드 선택으로 돌아가기
            showGameModeSelection();
        }
    }

    /**
     * 방 만들기 (호스트)
     */
    private static void createRoom() {
        CreateRoomDialog createDialog = new CreateRoomDialog(null);
        createDialog.setVisible(true);

        if (!createDialog.isConfirmed()) {
            showNetworkMenu();
            return;
        }

        String playerName = createDialog.getPlayerName();
        int port = createDialog.getPort();
        int maxPlayers = createDialog.getMaxPlayers();

        // 서버 시작
        try {
            server = new GameServer(port, maxPlayers);

            // 서버 이벤트 리스너 설정
            server.setListener(new GameServer.GameServerListener() {
                @Override
                public void onServerStarted(String ipAddress, int port) {
                    System.out.println("서버 시작됨: " + ipAddress + ":" + port);
                }

                @Override
                public void onServerStopped() {
                    System.out.println("서버 종료됨");
                }

                @Override
                public void onGameStarted() {
                    System.out.println("게임 시작!");
                    // TODO: Phase 3에서 GameUI 연동
                }

                @Override
                public void onMessageReceived(String playerId, Message message) {
                    System.out.println("메시지 수신: " + message.getType());
                    handleServerSideAction(playerId, message);
                }

                @Override
                public void onPlayerDisconnected(String playerId) {
                    System.out.println("플레이어 연결 끊김: " + playerId);
                    handlePlayerDisconnect(playerId);
                }
            });

            server.start();

            // 호스트도 클라이언트로 자신의 서버에 연결
            String playerId = UUID.randomUUID().toString();
            client = new GameClient("localhost", port, playerId);

            // 클라이언트 이벤트 리스너 설정
            client.setListener(new GameClient.GameClientListener() {
                @Override
                public void onConnected() {
                    System.out.println("서버 연결 성공");
                    // 방 참가
                    client.joinRoom(playerName);
                }

                @Override
                public void onConnectionFailed(String reason) {
                    System.err.println("서버 연결 실패: " + reason);
                    JOptionPane.showMessageDialog(null,
                        "서버 연결 실패: " + reason,
                        "오류",
                        JOptionPane.ERROR_MESSAGE);
                    if (server != null) {
                        server.stop();
                    }
                    showNetworkMenu();
                }

                @Override
                public void onDisconnected() {
                    System.out.println("서버 연결 종료");
                    if (gameUI != null) {
                        gameUI.dispose();
                        gameUI = null;
                    }
                    networkHost = false;
                }

                @Override
                public void onMessageReceived(Message message) {
                    handleMessage(message);
                }
            });

            if (client.connect()) {
                // 대기실 표시
                showLobby(true, maxPlayers);
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                "서버 시작 실패: " + e.getMessage(),
                "오류",
                JOptionPane.ERROR_MESSAGE);
            showNetworkMenu();
        }
    }

    /**
     * 방 참가 (클라이언트)
     */
    private static void joinRoom() {
        JoinRoomDialog joinDialog = new JoinRoomDialog(null);
        joinDialog.setVisible(true);

        if (!joinDialog.isConfirmed()) {
            showNetworkMenu();
            return;
        }

        String playerName = joinDialog.getPlayerName();
        String serverIP = joinDialog.getServerIP();
        int port = joinDialog.getPort();

        // 클라이언트 연결
        String playerId = UUID.randomUUID().toString();
        client = new GameClient(serverIP, port, playerId);

        // 클라이언트 이벤트 리스너 설정
        client.setListener(new GameClient.GameClientListener() {
            @Override
            public void onConnected() {
                System.out.println("서버 연결 성공");
                // 방 참가
                client.joinRoom(playerName);
            }

            @Override
            public void onConnectionFailed(String reason) {
                System.err.println("서버 연결 실패: " + reason);
                JOptionPane.showMessageDialog(null,
                    "서버 연결 실패: " + reason,
                    "오류",
                    JOptionPane.ERROR_MESSAGE);
                showNetworkMenu();
            }

            @Override
            public void onDisconnected() {
                System.out.println("서버 연결 종료");
                if (gameUI != null) {
                    gameUI.dispose();
                    gameUI = null;
                }
                networkHost = false;
                if (lobbyPanel != null) {
                    lobbyPanel.dispose();
                }
                JOptionPane.showMessageDialog(null,
                    "서버 연결이 끊어졌습니다.",
                    "알림",
                    JOptionPane.INFORMATION_MESSAGE);
                showGameModeSelection();
            }

            @Override
            public void onMessageReceived(Message message) {
                handleMessage(message);
            }
        });

        if (client.connect()) {
            // 연결 성공 시 대기실 표시는 PLAYER_JOIN 응답 후에 처리
        }
    }

    /**
     * 대기실 표시
     */
    private static void showLobby(boolean isHost, int maxPlayers) {
        SwingUtilities.invokeLater(() -> {
            lobbyPanel = new LobbyPanel(isHost, maxPlayers);

            lobbyPanel.setLobbyListener(new LobbyPanel.LobbyListener() {
                @Override
                public void onGameStart() {
                    // 게임 시작 요청
                    client.requestGameStart();
                }

                @Override
                public void onLeave() {
                    // 연결 종료
                    if (client != null) {
                        client.disconnect();
                    }
                    if (server != null) {
                        server.stop();
                    }
                    if (gameUI != null) {
                        gameUI.dispose();
                        gameUI = null;
                    }
                    networkHost = false;
                    showGameModeSelection();
                }
            });

            lobbyPanel.setVisible(true);

            // 초기 플레이어 목록 업데이트 (서버가 있는 경우)
            if (server != null) {
                updatePlayerList();
            }
        });
    }

    /**
     * 메시지 처리
     */
    private static void handleMessage(Message message) {
        System.out.println("메시지 처리: " + message.getType());

        switch (message.getType()) {
            case PLAYER_JOIN:
                // 플레이어 참가 알림
                if (server == null && message.getString("status") != null && message.getString("status").equals("joined")) {
                    // 자신이 참가한 경우
                    Integer playerCount = message.getInt("playerCount");
                    if (lobbyPanel == null && playerCount != null) {
                        // 클라이언트로 참가한 경우 대기실 표시
                        // 최대 플레이어 수는 서버에서 받아야 하지만, 임시로 4명으로 설정
                        showLobby(false, 4);
                    }
                }
                updatePlayerList();
                updateLobbyPlayerCount(message.getInt("playerCount"));
                break;

            case PLAYER_LEAVE:
                // 플레이어 퇴장 알림
                updatePlayerList();
                updateLobbyPlayerCount(message.getInt("playerCount"));
                break;

            case GAME_START:
                handleGameStartMessage(message);
                break;

            case GAME_STATE_UPDATE:
                handleGameStateUpdate(message);
                break;

            case ERROR:
                // 에러 메시지
                String errorMsg = message.getString("message");
                JOptionPane.showMessageDialog(null,
                    errorMsg,
                    "오류",
                    JOptionPane.ERROR_MESSAGE);
                break;

            case CHAT_MESSAGE:
                handleChatMessage(message);
                break;

            case CHAT_EMOJI:
                handleChatEmoji(message);
                break;

            default:
                break;
        }
    }

    /**
     * 채팅 메시지 처리
     */
    private static void handleChatMessage(Message message) {
        if (gameUI == null) {
            return;
        }

        Integer playerIndex = message.getInt("playerIndex");
        String playerName = message.getString("playerName");
        String content = message.getString("content");

        if (playerIndex != null && playerName != null && content != null) {
            SwingUtilities.invokeLater(() -> {
                ChatPanel chatPanel = gameUI.getFrame().getOverlayPanel().getChatPanel();
                if (chatPanel != null) {
                    chatPanel.addPlayerMessage(playerIndex, playerName, content);
                }
            });
        }
    }

    /**
     * 이모지 메시지 처리
     */
    private static void handleChatEmoji(Message message) {
        if (gameUI == null) {
            return;
        }

        Integer playerIndex = message.getInt("playerIndex");
        String playerName = message.getString("playerName");
        String emoji = message.getString("content");

        if (playerIndex != null && playerName != null && emoji != null) {
            SwingUtilities.invokeLater(() -> {
                ChatPanel chatPanel = gameUI.getFrame().getOverlayPanel().getChatPanel();
                if (chatPanel != null) {
                    chatPanel.addEmojiMessage(playerIndex, playerName, emoji);
                }
            });
        }
    }

    private static void handleGameStartMessage(Message message) {
        Object playersObj = message.get("players");
        List<GameUI.PlayerSetup> setups = parsePlayerSetups(playersObj);
        Integer initialCashValue = message.getInt("initialCash");
        int initialCash = initialCashValue != null ? initialCashValue : NetConstants.DEFAULT_INITIAL_CASH;
        String hostId = message.getString("hostId");
        boolean isHostGame = client != null && client.getPlayerId() != null && hostId != null
            && hostId.equals(client.getPlayerId());
        startNetworkGame(isHostGame, setups, initialCash);
    }

    private static void handleGameStateUpdate(Message message) {
        if (networkHost || gameUI == null) {
            return;
        }

        Map<String, Object> data = message.getData();
        if (data == null) {
            return;
        }

        GameStateSnapshot snapshot = GameStateMapper.fromMap(data);
        if (snapshot != null) {
            gameUI.applyNetworkSnapshot(snapshot);
        }
    }

    /**
     * 플레이어 목록 업데이트
     */
    private static void updatePlayerList() {
        if (server != null && lobbyPanel != null) {
            RoomManager roomManager = server.getRoomManager();
            SwingUtilities.invokeLater(() -> {
                lobbyPanel.updatePlayerList(roomManager.getPlayers());
                lobbyPanel.updatePlayerCount(roomManager.getPlayerCount());
            });
        }
    }

    private static void updateLobbyPlayerCount(Integer playerCount) {
        if (lobbyPanel != null && playerCount != null) {
            lobbyPanel.updatePlayerCount(playerCount);
        }
    }

    private static void startNetworkGame(boolean isHostGame, List<GameUI.PlayerSetup> setups, int initialCash) {
        if (setups == null || setups.isEmpty()) {
            setups = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                setups.add(new GameUI.PlayerSetup(i, null, "Player" + (char)('A' + i)));
            }
        }

        final List<GameUI.PlayerSetup> finalSetups = setups;
        networkHost = isHostGame;

        SwingUtilities.invokeLater(() -> {
            if (lobbyPanel != null) {
                lobbyPanel.dispose();
                lobbyPanel = null;
            }

            if (gameUI != null) {
                gameUI.dispose();
            }

            GameUI.GameStateSyncListener syncListener = isHostGame ? Main::broadcastGameState : null;
            // 호스트와 클라이언트 모두 actionSender를 가짐 (채팅 등을 위해)
            GameUI.NetworkActionSender actionSender = client != null
                ? Main::sendClientAction
                : null;
            GameUI.NetworkSettings settings = new GameUI.NetworkSettings(
                isHostGame,
                client != null ? client.getPlayerId() : null,
                finalSetups,
                syncListener,
                actionSender
            );

            gameUI = new GameUI(finalSetups.size(), initialCash, settings);
        });
    }

    private static void broadcastGameState(GameStateSnapshot snapshot) {
        if (server == null || snapshot == null) {
            return;
        }

        Message stateMessage = new Message(MessageType.GAME_STATE_UPDATE);
        stateMessage.setData(GameStateMapper.toMap(snapshot));
        server.broadcast(stateMessage);
    }

    private static void sendClientAction(MessageType type, Map<String, Object> payload) {
        if (client == null) {
            return;
        }
        client.sendAction(type, payload);
    }

    private static void handleServerSideAction(String playerId, Message message) {
        if (!networkHost || gameUI == null || message == null) {
            return;
        }

        switch (message.getType()) {
            case ROLL_DICE:
                Integer section = message.getInt("section");
                gameUI.handleRemoteRoll(playerId, section != null ? section : 1, message.getString("diceMode"));
                break;
            case BUY_CITY:
                gameUI.handleRemotePurchase(playerId,
                    message.getString("target"),
                    message.getInt("level"),
                    message.getInt("tileId"));
                break;
            case UPGRADE:
                gameUI.handleRemoteUpgrade(playerId, message.getInt("tileId"));
                break;
            case PASS:
                gameUI.handleRemoteSkip(playerId);
                break;
            case JAIL_CHOICE:
                gameUI.handleRemoteEscape(playerId, message.getString("choice"));
                break;
            case TAKEOVER:
                gameUI.handleRemoteTakeover(playerId,
                    message.getString("target"),
                    message.getInt("tileId"),
                    message.getString("choice"));
                break;
            case CITY_SELECTION:
                gameUI.handleRemoteTileSelection(playerId,
                    message.getInt("tileId"),
                    message.getString("context"));
                break;
            case TOURIST_SPOT_CHOICE:
                gameUI.handleRemoteTouristSpotChoice(playerId,
                    message.getInt("tileId"),
                    message.getString("choice"),
                    message.getBoolean("purchased"));
                break;
            case CHAT_MESSAGE:
            case CHAT_EMOJI:
                // 채팅 메시지는 모든 클라이언트에게 브로드캐스트
                if (server != null) {
                    server.broadcast(message);
                }
                break;
            default:
                break;
        }
    }

    private static void handlePlayerDisconnect(String playerId) {
        if (!networkHost || gameUI == null || playerId == null) {
            return;
        }
        gameUI.handlePlayerDisconnect(playerId);
    }

    @SuppressWarnings("unchecked")
    private static List<GameUI.PlayerSetup> parsePlayerSetups(Object playersObj) {
        List<GameUI.PlayerSetup> setups = new ArrayList<>();
        if (playersObj instanceof List) {
            for (Object entryObj : (List<?>) playersObj) {
                if (entryObj instanceof Map) {
                    Map<String, Object> entry = (Map<String, Object>) entryObj;
                    int index = safeInt(entry.get("index"), setups.size());
                    String playerId = safeString(entry.get("playerId"));
                    String name = safeString(entry.get("name"));
                    if (name == null) {
                        name = "Player" + (index + 1);
                    }
                    setups.add(new GameUI.PlayerSetup(index, playerId, name));
                }
            }
            setups.sort(Comparator.comparingInt(GameUI.PlayerSetup::getIndex));
        }
        return setups;
    }

    private static int safeInt(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return value != null ? Integer.parseInt(value.toString()) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String safeString(Object value) {
        return value != null ? value.toString() : null;
    }
}
