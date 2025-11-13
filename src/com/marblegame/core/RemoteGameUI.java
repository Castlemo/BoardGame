package com.marblegame.core;

import com.marblegame.core.input.LocalPlayerInputRouter;
import com.marblegame.core.input.PlayerInputEvent;
import com.marblegame.core.input.PlayerInputSink;
import com.marblegame.model.Board;
import com.marblegame.model.City;
import com.marblegame.model.Player;
import com.marblegame.model.Tile;
import com.marblegame.model.TouristSpot;
import com.marblegame.network.ClientNetworkService;
import com.marblegame.network.message.MessageType;
import com.marblegame.network.message.NetworkMessage;
import com.marblegame.network.message.RemoteActionCodec;
import com.marblegame.network.snapshot.GameSnapshot;
import com.marblegame.network.snapshot.GameSnapshotSerializer;
import com.marblegame.ui.GameFrame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * 원격 클라이언트에서 호스트의 GameUI 상태를 미러링하는 경량 컨트롤러.
 */
public class RemoteGameUI {
    private final ClientNetworkService networkService;
    private final Runnable onDisconnect;

    private GameFrame frame;
    private Board board;
    private Player[] players;
    private boolean initialized = false;
    private int lastDiceSequence = -1;
    private final List<String> pendingLogs = new ArrayList<>();
    private volatile boolean localDisconnectRequested = false;

    public RemoteGameUI(ClientNetworkService networkService, Runnable onDisconnect) {
        this.networkService = networkService;
        this.onDisconnect = onDisconnect;
        this.networkService.setMessageListener(this::handleServerMessage);
        this.networkService.setDisconnectListener(this::handleNetworkDisconnect);
    }

    private void handleServerMessage(NetworkMessage message) {
        if (message.getType() == MessageType.HEARTBEAT) {
            return;
        }
        if (message.getType() == MessageType.STATE_SNAPSHOT) {
            try {
                GameSnapshot snapshot = GameSnapshotSerializer.deserialize(message.getPayload());
                if (snapshot != null) {
                    SwingUtilities.invokeLater(() -> applySnapshot(snapshot));
                }
            } catch (IllegalArgumentException ex) {
                System.err.println("[Client] 스냅샷 파싱 실패: " + ex.getMessage());
            }
        } else if (message.getType() == MessageType.LOG_ENTRY) {
            String payload = message.getPayload();
            if (payload != null && !payload.isEmpty()) {
                SwingUtilities.invokeLater(() -> appendLog(payload));
            }
        }
    }

    private void applySnapshot(GameSnapshot snapshot) {
        if (snapshot.players.isEmpty()) {
            return;
        }

        if (!initialized) {
            initializeFrame(snapshot);
        }

        if (frame == null) {
            return;
        }

        syncPlayers(snapshot);
        syncBoard(snapshot);
        syncButtons(snapshot);
        syncDice(snapshot);

        frame.updateDisplay(snapshot.turnNumber);
        frame.getBoardPanel().setTileClickEnabled(snapshot.tileSelectionEnabled);
    }

    private void initializeFrame(GameSnapshot snapshot) {
        board = new Board();
        players = new Player[snapshot.players.size()];

        for (int i = 0; i < snapshot.players.size(); i++) {
            GameSnapshot.PlayerState ps = snapshot.players.get(i);
            Player player = new Player(ps.name, ps.cash);
            player.pos = ps.position;
            player.jailTurns = ps.jailTurns;
            player.bankrupt = ps.bankrupt;
            player.hasRailroadTicket = ps.hasRailroadTicket;
            player.hasExtraChance = ps.hasExtraChance;
            players[i] = player;
        }

        frame = new GameFrame(board, Arrays.asList(players));
        frame.setTitle("모두의 마블 2.0 - 클라이언트");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                localDisconnectRequested = true;
                if (onDisconnect != null) {
                    onDisconnect.run();
                }
            }
        });

        new LocalPlayerInputRouter(
            frame,
            new NetworkPlayerInputSink(networkService, frame)
        );

        frame.setVisible(true);
        initialized = true;
        flushPendingLogs();
    }

    private void syncPlayers(GameSnapshot snapshot) {
        if (players == null || players.length != snapshot.players.size()) {
            return;
        }
        for (int i = 0; i < players.length; i++) {
            GameSnapshot.PlayerState ps = snapshot.players.get(i);
            Player player = players[i];
            player.cash = ps.cash;
            player.pos = ps.position;
            player.jailTurns = ps.jailTurns;
            player.bankrupt = ps.bankrupt;
            player.hasRailroadTicket = ps.hasRailroadTicket;
            player.hasExtraChance = ps.hasExtraChance;
        }
        frame.getOverlayPanel().updatePlayerInfo();
    }

    private void syncBoard(GameSnapshot snapshot) {
        if (board == null) {
            board = new Board();
        }
        for (GameSnapshot.CityState cs : snapshot.cities) {
            Tile tile = board.getTile(cs.tileId);
            if (tile instanceof City) {
                City city = (City) tile;
                city.owner = cs.owner;
                city.level = cs.level;
                city.hasOlympicBoost = cs.hasOlympicBoost;
                city.isDeleted = cs.deleted;
            }
        }
        for (GameSnapshot.TouristSpotState ts : snapshot.touristSpots) {
            Tile tile = board.getTile(ts.tileId);
            if (tile instanceof TouristSpot) {
                TouristSpot spot = (TouristSpot) tile;
                spot.owner = ts.owner;
                spot.locked = ts.locked;
                spot.lockedBy = ts.lockedBy;
            }
        }
        frame.getBoardPanel().updateBoard();
    }

    private void syncButtons(GameSnapshot snapshot) {
        if (snapshot.buttons != null) {
            frame.getActionPanel().setButtonsEnabled(
                snapshot.buttons.roll,
                snapshot.buttons.purchase,
                snapshot.buttons.upgrade,
                snapshot.buttons.takeover,
                snapshot.buttons.skip,
                snapshot.buttons.escape
            );
        }

        frame.getOverlayPanel().getOddButton().putClientProperty("selected", snapshot.oddModeSelected);
        frame.getOverlayPanel().getEvenButton().putClientProperty("selected", snapshot.evenModeSelected);
        frame.getOverlayPanel().getOddButton().repaint();
        frame.getOverlayPanel().getEvenButton().repaint();
    }

    private void syncDice(GameSnapshot snapshot) {
        int sequence = snapshot.diceRollSequence;
        if (sequence <= 0) {
            frame.getActionPanel().getDiceAnimationPanel().showFaces(snapshot.dice1, snapshot.dice2);
            lastDiceSequence = sequence;
            return;
        }

        if (lastDiceSequence < 0) {
            lastDiceSequence = sequence;
            frame.getActionPanel().getDiceAnimationPanel().showFaces(snapshot.dice1, snapshot.dice2);
            return;
        }

        if (sequence != lastDiceSequence) {
            lastDiceSequence = sequence;
            frame.getActionPanel().getDiceAnimationPanel().startAnimation(snapshot.dice1, snapshot.dice2, null);
        }
    }

    private void appendLog(String message) {
        if (frame == null) {
            pendingLogs.add(message);
        } else {
            frame.getControlPanel().addLog(message);
        }
    }

    private void flushPendingLogs() {
        if (frame == null || pendingLogs.isEmpty()) {
            return;
        }
        for (String log : pendingLogs) {
            frame.getControlPanel().addLog(log);
        }
        pendingLogs.clear();
    }

    public void dispose() {
        networkService.setMessageListener(null);
        networkService.setDisconnectListener(null);
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    /**
     * 로컬 입력을 호스트로 전달하는 PlayerInputSink.
     */
    private static class NetworkPlayerInputSink implements PlayerInputSink {
        private final ClientNetworkService networkService;
        private final GameFrame frame;

        NetworkPlayerInputSink(ClientNetworkService networkService, GameFrame frame) {
            this.networkService = networkService;
            this.frame = frame;
        }

        @Override
        public void handlePlayerInput(PlayerInputEvent event) {
            switch (event.getType()) {
                case GAUGE_PRESS:
                    frame.getActionPanel().getDiceGauge().start();
                    frame.getActionPanel().startGaugeAnimation();
                    break;
                case GAUGE_RELEASE:
                    frame.getActionPanel().getDiceGauge().stop();
                    frame.getActionPanel().stopGaugeAnimation();
                    break;
                default:
                    break;
            }
            if (!networkService.send(RemoteActionCodec.encode(event))) {
                System.err.println("[Client] 원격 입력 전송 실패");
            }
        }
    }

    private void handleNetworkDisconnect() {
        if (localDisconnectRequested) {
            SwingUtilities.invokeLater(this::dispose);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            String reason = networkService.getDisconnectReason();
            if (reason == null || reason.isEmpty()) {
                reason = "호스트와의 연결이 종료되었습니다.";
            }
            appendLog("[시스템] " + reason);
            if (frame != null) {
                JOptionPane.showMessageDialog(
                    frame,
                    reason,
                    "연결 끊김",
                    JOptionPane.WARNING_MESSAGE
                );
            }
            if (onDisconnect != null) {
                onDisconnect.run();
            }
            dispose();
        });
    }
}
