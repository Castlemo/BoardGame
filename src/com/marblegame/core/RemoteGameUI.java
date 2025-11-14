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
import com.marblegame.network.listener.ServerMessageListener;
import com.marblegame.network.message.DialogCommandPayload;
import com.marblegame.network.message.DialogResponsePayload;
import com.marblegame.network.message.DialogSyncCodec;
import com.marblegame.network.message.DialogSyncPayload;
import com.marblegame.network.message.DialogType;
import com.marblegame.network.message.MessageType;
import com.marblegame.network.message.NetworkMessage;
import com.marblegame.network.message.RemoteActionCodec;
import com.marblegame.network.message.SlotAssignmentPayload;
import com.marblegame.network.snapshot.GameSnapshot;
import com.marblegame.network.snapshot.GameSnapshotSerializer;
import com.marblegame.ui.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final List<DialogSyncPayload> pendingDialogs = new ArrayList<>();
    private final List<DialogCommandPayload> pendingDialogCommands = new ArrayList<>();
    private volatile boolean localDisconnectRequested = false;
    private final ServerMessageListener messageListener;
    private volatile int assignedPlayerIndex = -1;
    private volatile String assignedPlayerName = "";

    public RemoteGameUI(ClientNetworkService networkService, Runnable onDisconnect) {
        this.networkService = networkService;
        this.onDisconnect = onDisconnect;
        this.messageListener = this::handleServerMessage;
        this.networkService.addMessageListener(messageListener);
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
        } else if (message.getType() == MessageType.DIALOG_SYNC) {
            handleDialogSync(message.getPayload());
        } else if (message.getType() == MessageType.SLOT_ASSIGNMENT) {
            handleSlotAssignment(message.getPayload());
        } else if (message.getType() == MessageType.DIALOG_COMMAND) {
            handleDialogCommand(message.getPayload());
        }
    }

    private void handleDialogSync(String payload) {
        if (payload == null || payload.isEmpty()) {
            return;
        }
        try {
            DialogSyncPayload dialogPayload = DialogSyncCodec.decode(payload);
            if (!shouldDisplayDialog(dialogPayload)) {
                return;
            }
            SwingUtilities.invokeLater(() -> enqueueOrShowDialog(dialogPayload));
        } catch (IllegalArgumentException ex) {
            System.err.println("[Client] 다이얼로그 파싱 실패: " + ex.getMessage());
        }
    }

    private void handleSlotAssignment(String payload) {
        if (payload == null || payload.isEmpty()) {
            return;
        }
        try {
            SlotAssignmentPayload assignment = SlotAssignmentPayload.decode(payload);
            SwingUtilities.invokeLater(() -> applySlotAssignment(assignment));
        } catch (IllegalArgumentException ex) {
            System.err.println("[Client] 슬롯 배정 파싱 실패: " + ex.getMessage());
        }
    }

    private void handleDialogCommand(String payload) {
        if (payload == null || payload.isEmpty()) {
            return;
        }
        try {
            DialogCommandPayload command = DialogCommandPayload.decode(payload);
            if (assignedPlayerIndex < 0 || command.getPlayerIndex() != assignedPlayerIndex) {
                return;
            }
            SwingUtilities.invokeLater(() -> enqueueOrHandleDialogCommand(command));
        } catch (IllegalArgumentException ex) {
            System.err.println("[Client] 다이얼로그 명령 파싱 실패: " + ex.getMessage());
        }
    }

    private void applySlotAssignment(SlotAssignmentPayload assignment) {
        if (assignment.getStatus() == SlotAssignmentPayload.Status.ASSIGNED) {
            assignedPlayerIndex = assignment.getSlotIndex();
            assignedPlayerName = assignment.getPlayerName();
        } else {
            assignedPlayerIndex = -1;
            assignedPlayerName = "";
        }
        refreshLocalHighlight();
        if (frame != null && assignedPlayerIndex < 0) {
            frame.getOverlayPanel().hideWaitingMessage();
        }
        if (assignment.getNote() != null && !assignment.getNote().isEmpty()) {
            appendLog("[로비] " + assignment.getNote());
        }
    }

    private void enqueueOrShowDialog(DialogSyncPayload payload) {
        if (frame == null) {
            pendingDialogs.add(payload);
            return;
        }
        showSynchronizedDialog(payload);
    }

    private boolean shouldDisplayDialog(DialogSyncPayload payload) {
        if (payload == null) {
            return false;
        }
        int targetIndex = payload.getInt(DialogSyncPayload.ATTR_TARGET_PLAYER_INDEX, -1);
        if (targetIndex < 0) {
            return true;
        }
        return assignedPlayerIndex >= 0 && assignedPlayerIndex == targetIndex;
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
        updateWaitingIndicator(snapshot);
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
        flushPendingDialogs();
        flushPendingDialogCommands();
        refreshLocalHighlight();
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

    private void updateWaitingIndicator(GameSnapshot snapshot) {
        if (frame == null) {
            return;
        }
        if (assignedPlayerIndex < 0) {
            frame.getOverlayPanel().hideWaitingMessage();
            frame.getOverlayPanel().setHighlightedPlayerIndex(-1);
            return;
        }
        if (snapshot.currentPlayerIndex == assignedPlayerIndex) {
            frame.getOverlayPanel().hideWaitingMessage();
            frame.getOverlayPanel().setHighlightedPlayerIndex(-1);
            return;
        }
        String activeName = resolvePlayerName(snapshot.currentPlayerIndex);
        String message = "다른 플레이어가 진행 중입니다. 잠시만 기다려주세요.";
        if (activeName != null && !activeName.isEmpty()) {
            message = activeName + " 차례입니다. 잠시만 기다려주세요.";
        }
        frame.getOverlayPanel().showWaitingMessage(message);
        frame.getOverlayPanel().setHighlightedPlayerIndex(assignedPlayerIndex);
    }

    private String resolvePlayerName(int index) {
        if (players == null || index < 0 || index >= players.length) {
            return "";
        }
        String name = players[index].name;
        return name == null ? "" : name;
    }

    private void showSynchronizedDialog(DialogSyncPayload payload) {
        if (frame == null || payload == null) {
            return;
        }
        DialogType type = payload.getType();
        switch (type) {
            case CITY_SELECTION:
                new CitySelectionDialog(frame).setVisible(true);
                break;
            case DOUBLE_SUPPRESSED:
                new DoubleSuppressedDialog(
                    frame,
                    payload.getInt("diceValue", 0),
                    payload.getInt("consecutive", 0)
                ).setVisible(true);
                break;
            case ISLAND_STATUS:
                new IslandDialog(frame, payload.getInt("jailTurns", 0)).setVisible(true);
                break;
            case CHANCE_REWARD:
                new ChanceDialog(frame, payload.getInt("amount", 0)).setVisible(true);
                break;
            case WORLD_TOUR:
                new WorldTourDialog(frame).setVisible(true);
                break;
            case DUAL_MAGNETIC:
                new DualMagneticDialog(
                    frame,
                    safeString(payload.get("cityName"), "도시"),
                    payload.getInt("pulledCount", 0)
                ).setVisible(true);
                break;
            case TOLL_PAYMENT:
                new TollPaymentDialog(
                    frame,
                    safeString(payload.get("cityName"), "도시"),
                    safeString(payload.get("ownerName"), "???"),
                    payload.getInt("level", 1),
                    payload.getInt("toll", 0),
                    payload.getBoolean("olympic", false),
                    payload.getInt("playerCash", 0)
                ).setVisible(true);
                break;
            case TOURIST_PURCHASE:
                new TouristSpotPurchaseDialog(
                    frame,
                    safeString(payload.get("spotName"), "관광지"),
                    payload.getInt("price", 0),
                    payload.getInt("playerCash", 0)
                ).setVisible(true);
                break;
            case TOURIST_CHOICE:
                new TouristSpotChoiceDialog(
                    frame,
                    safeString(payload.get("spotName"), "관광지")
                ).setVisible(true);
                break;
            case LEVEL_SELECTION:
                new LevelSelectionDialog(
                    frame,
                    safeString(payload.get("cityName"), "도시"),
                    payload.getInt("price", 0),
                    payload.getInt("playerCash", 0)
                ).setVisible(true);
                break;
            case TAKEOVER_CONFIRM:
                new TakeoverConfirmDialog(
                    frame,
                    safeString(payload.get("cityName"), "도시"),
                    safeString(payload.get("ownerName"), "???"),
                    payload.getInt("level", 1),
                    payload.getInt("cost", 0),
                    payload.getInt("playerCash", 0)
                ).setVisible(true);
                break;
            case ERROR:
                new ErrorDialog(
                    frame,
                    safeString(payload.get("title"), "오류"),
                    safeString(payload.get("message"), "")
                ).setVisible(true);
                break;
            case TAX_PAYMENT:
                new TaxPaymentDialog(
                    frame,
                    payload.getInt("playerCash", 0),
                    payload.getInt("taxAmount", 0)
                ).setVisible(true);
                break;
            case OLYMPIC:
                new OlympicDialog(frame).setVisible(true);
                break;
            case PHASE_DELETE:
                new PhaseDeleteDialog(
                    frame,
                    safeString(payload.get("cityName"), "도시")
                ).setVisible(true);
                break;
            case DOUBLE_ROLL:
                new DoubleDialog(
                    frame,
                    payload.getInt("diceValue", 0),
                    payload.getInt("consecutive", 0)
                ).setVisible(true);
                break;
            case UPGRADE_GUIDE:
                JOptionPane.showMessageDialog(
                    frame,
                    safeString(payload.get("message"), ""),
                    safeString(payload.get("title"), "안내"),
                    JOptionPane.INFORMATION_MESSAGE
                );
                break;
            case GAME_OVER:
                String winner = safeString(payload.get("winner"), "플레이어");
                String victoryType = safeString(payload.get("victoryType"), "조건");
                int cash = payload.getInt("cash", 0);
                JOptionPane.showOptionDialog(
                    frame,
                    winner + " 승리!\n승리 조건: " + victoryType + "\n최종 자산: " + String.format("%,d", cash) + "원",
                    "게임 종료",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    new Object[]{"새 게임", "종료"},
                    "새 게임"
                );
                break;
            default:
                break;
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

    private void flushPendingDialogs() {
        if (frame == null || pendingDialogs.isEmpty()) {
            return;
        }
        for (DialogSyncPayload payload : pendingDialogs) {
            showSynchronizedDialog(payload);
        }
        pendingDialogs.clear();
    }

    private void flushPendingDialogCommands() {
        if (frame == null || pendingDialogCommands.isEmpty()) {
            return;
        }
        List<DialogCommandPayload> commands = new ArrayList<>(pendingDialogCommands);
        pendingDialogCommands.clear();
        for (DialogCommandPayload command : commands) {
            handleDialogCommandInternal(command);
        }
    }

    private void enqueueOrHandleDialogCommand(DialogCommandPayload command) {
        if (frame == null) {
            pendingDialogCommands.add(command);
            return;
        }
        handleDialogCommandInternal(command);
    }

    private void handleDialogCommandInternal(DialogCommandPayload command) {
        if (frame == null) {
            pendingDialogCommands.add(command);
            return;
        }
        Map<String, String> attrs = command.getAttributes();
        switch (command.getDialogType()) {
            case LEVEL_SELECTION:
                handleLevelSelectionCommand(command, attrs);
                break;
            case TOURIST_PURCHASE:
                handleTouristPurchaseCommand(command, attrs);
                break;
            case TAKEOVER_CONFIRM:
                handleTakeoverConfirmCommand(command, attrs);
                break;
            case TOURIST_CHOICE:
                handleTouristChoiceCommand(command, attrs);
                break;
            default:
                sendDialogResponse(command, "ACK", newAttributeMap());
                break;
        }
    }

    private void handleLevelSelectionCommand(DialogCommandPayload command, Map<String, String> attrs) {
        String cityName = getAttr(attrs, "cityName", "도시");
        int price = getIntAttr(attrs, "price", 0);
        int cash = getIntAttr(attrs, "playerCash", 0);
        LevelSelectionDialog dialog = new LevelSelectionDialog(frame, cityName, price, cash);
        dialog.setVisible(true);
        int selectedLevel = dialog.getSelectedLevel();
        Map<String, String> responseAttrs = newAttributeMap();
        String result;
        if (selectedLevel > 0) {
            result = "LEVEL_SELECTED";
            responseAttrs.put("selectedLevel", Integer.toString(selectedLevel));
        } else {
            result = "CANCEL";
        }
        sendDialogResponse(command, result, responseAttrs);
    }

    private void handleTouristPurchaseCommand(DialogCommandPayload command, Map<String, String> attrs) {
        String spotName = getAttr(attrs, "spotName", "관광지");
        int price = getIntAttr(attrs, "price", 0);
        int cash = getIntAttr(attrs, "playerCash", 0);
        TouristSpotPurchaseDialog dialog = new TouristSpotPurchaseDialog(frame, spotName, price, cash);
        dialog.setVisible(true);
        String result = dialog.isConfirmed() ? "CONFIRM" : "CANCEL";
        sendDialogResponse(command, result, newAttributeMap());
    }

    private void handleTakeoverConfirmCommand(DialogCommandPayload command, Map<String, String> attrs) {
        String cityName = getAttr(attrs, "cityName", "도시");
        String ownerName = getAttr(attrs, "ownerName", "???");
        int level = getIntAttr(attrs, "level", 1);
        int cost = getIntAttr(attrs, "cost", 0);
        int cash = getIntAttr(attrs, "playerCash", 0);
        TakeoverConfirmDialog dialog = new TakeoverConfirmDialog(frame, cityName, ownerName, level, cost, cash);
        dialog.setVisible(true);
        String result = dialog.isConfirmed() ? "CONFIRM" : "CANCEL";
        sendDialogResponse(command, result, newAttributeMap());
    }

    private void handleTouristChoiceCommand(DialogCommandPayload command, Map<String, String> attrs) {
        String spotName = getAttr(attrs, "spotName", "관광지");
        TouristSpotChoiceDialog dialog = new TouristSpotChoiceDialog(frame, spotName);
        dialog.setVisible(true);
        TouristSpotChoiceDialog.Choice choice = dialog.getSelectedChoice();
        if (choice == null) {
            choice = TouristSpotChoiceDialog.Choice.LOCK;
        }
        String result = (choice == TouristSpotChoiceDialog.Choice.EXTRA_ROLL) ? "EXTRA_ROLL" : "LOCK";
        sendDialogResponse(command, result, newAttributeMap());
    }

    private void sendDialogResponse(DialogCommandPayload command, String result, Map<String, String> attributes) {
        Map<String, String> attrsCopy = attributes == null ? newAttributeMap() : new LinkedHashMap<>(attributes);
        DialogResponsePayload response = new DialogResponsePayload(
            command.getRequestId(),
            command.getDialogType(),
            command.getPlayerIndex(),
            result == null ? "" : result,
            attrsCopy
        );
        NetworkMessage message = new NetworkMessage(
            MessageType.DIALOG_RESPONSE,
            DialogResponsePayload.encode(response)
        );
        if (!networkService.send(message)) {
            System.err.println("[Client] 다이얼로그 응답 전송 실패: " + command.getDialogType());
        }
    }

    private Map<String, String> newAttributeMap() {
        return new LinkedHashMap<>();
    }

    private String getAttr(Map<String, String> attrs, String key, String fallback) {
        if (attrs == null) {
            return fallback;
        }
        String value = attrs.get(key);
        return (value == null || value.isEmpty()) ? fallback : value;
    }

    private int getIntAttr(Map<String, String> attrs, String key, int fallback) {
        if (attrs == null) {
            return fallback;
        }
        String raw = attrs.get(key);
        if (raw == null || raw.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private void refreshLocalHighlight() {
        if (frame == null) {
            return;
        }
        if (assignedPlayerIndex < 0) {
            frame.getOverlayPanel().setHighlightedPlayerIndex(-1);
        }
    }

    private String safeString(String value, String fallback) {
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        return value;
    }

    public void dispose() {
        networkService.removeMessageListener(messageListener);
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
