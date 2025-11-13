package com.marblegame.core;

import com.marblegame.core.input.LocalPlayerInputRouter;
import com.marblegame.core.input.PlayerInputEvent;
import com.marblegame.core.input.PlayerInputSink;
import com.marblegame.model.*;
import com.marblegame.ui.*;
import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * UI 버전 게임 컨트롤러
 */
import com.marblegame.network.HostNetworkService;
import com.marblegame.network.lobby.LobbyState;
import com.marblegame.network.lobby.LobbyStateCodec;
import com.marblegame.network.lobby.LobbyStateView;
import com.marblegame.network.message.DialogSyncCodec;
import com.marblegame.network.message.DialogSyncPayload;
import com.marblegame.network.message.DialogType;
import com.marblegame.network.message.MessageType;
import com.marblegame.network.message.NetworkMessage;
import com.marblegame.network.message.ReadyStatusPayload;
import com.marblegame.network.message.RemoteActionCodec;
import com.marblegame.network.message.SlotAssignmentPayload;
import com.marblegame.network.message.SlotRequestPayload;
import com.marblegame.network.snapshot.GameSnapshot;
import com.marblegame.network.snapshot.GameSnapshotSerializer;

public class GameUI implements PlayerInputSink {
    private final Board board;
    private final RuleEngine ruleEngine;
    private final Player[] players;
    private final Dice dice;
    private final GameFrame frame;

    private int currentPlayerIndex = 0;
    private int turnCount = 1;
    private GameState state = GameState.WAITING_FOR_ROLL;

    private enum GameState {
        WAITING_FOR_ROLL,
        WAITING_FOR_ACTION,
        WAITING_FOR_JAIL_CHOICE,
        WAITING_FOR_RAILROAD_SELECTION,
        WAITING_FOR_LANDMARK_SELECTION,
        WAITING_FOR_DOUBLE_ROLL,  // 더블 발생 후 추가 주사위 대기
        WAITING_FOR_READY,        // 네트워크 플레이어 준비 대기
        ANIMATING_MOVEMENT,
        GAME_OVER
    }

    // 홀수/짝수 주사위 모드
    private enum DiceMode {
        NORMAL,  // 일반 모드
        ODD,     // 홀수만 (1, 3, 5)
        EVEN     // 짝수만 (2, 4, 6)
    }
    private DiceMode diceMode = DiceMode.NORMAL;
    private static final int[][][] SUM_TO_DICE_COMBINATIONS = createSumToDiceCombinations();

    // 더블 시스템
    private int consecutiveDoubles = 0;  // 현재 턴에서 연속 더블 횟수
    private int lastD1 = 0;  // 마지막 주사위 1
    private int lastD2 = 0;  // 마지막 주사위 2

    private Tile currentTile;
    private City selectedLandmarkCity = null;
    private static final int MOVEMENT_ANIMATION_INTERVAL = 16;
    private static final int MOVEMENT_SUB_STEPS = 12;
    private static final int MOVEMENT_HOLD_STEPS = 6;
    private static final double MOVEMENT_HOP_HEIGHT = 16.0;
    private Timer movementTimer;
    private Player movementPlayer;
    private int movementPlayerIndex;
    private int movementStepsRemaining;
    private int movementCurrentTile;
    private int movementNextTile;
    private int movementSubStep;
    private Point2D.Double movementStartPoint;
    private Point2D.Double movementEndPoint;

    private final HostNetworkService hostNetworkService;
    private HostLobbyFrame hostLobbyFrame;
    private LobbyState lobbyState;
    private Timer snapshotTimer;
    private int diceRollSequence = 0;
    private boolean tileSelectionEnabled = false;
    private boolean rollButtonActive;
    private boolean purchaseButtonActive;
    private boolean upgradeButtonActive;
    private boolean takeoverButtonActive;
    private boolean skipButtonActive;
    private boolean escapeButtonActive;
    private boolean waitingForReadyGate = false;

    public GameUI(int numPlayers, int initialCash) {
        this(numPlayers, initialCash, null);
    }

    public GameUI(int numPlayers, int initialCash, HostNetworkService hostNetworkService) {
        this.board = new Board();
        this.ruleEngine = new RuleEngine(board);
        this.players = new Player[numPlayers];
        this.dice = new Dice();
        this.hostNetworkService = hostNetworkService;

        // 플레이어 초기화
        for (int i = 0; i < numPlayers; i++) {
            players[i] = new Player("Player" + (char)('A' + i), initialCash);
        }

        List<String> slotLabels = new ArrayList<>();
        for (Player player : players) {
            slotLabels.add(player.name);
        }
        lobbyState = new LobbyState(slotLabels);

        // UI 초기화
        frame = new GameFrame(board, java.util.Arrays.asList(players));
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (snapshotTimer != null) {
                    snapshotTimer.stop();
                }
                if (hostLobbyFrame != null) {
                    hostLobbyFrame.dispose();
                    hostLobbyFrame = null;
                }
            }
        });
        new LocalPlayerInputRouter(frame, this);
        if (hostNetworkService != null) {
            hostNetworkService.setMessageListener(this::handleClientMessage);
            hostNetworkService.setClientLifecycleListener(new HostNetworkService.ClientLifecycleListener() {
                @Override
                public void onClientConnected(String clientId) {
                    SwingUtilities.invokeLater(() -> {
                        log("[네트워크] 클라이언트 연결: " + clientId);
                        handleLobbyConnection(clientId);
                    });
                }

                @Override
                public void onClientDisconnected(String clientId, String reason) {
                    SwingUtilities.invokeLater(() -> {
                        log("[네트워크] 클라이언트 연결 종료(" + clientId + "): " + reason);
                        handleLobbyDisconnection(clientId);
                    });
                }
            });
            hostLobbyFrame = new HostLobbyFrame(slotIndex ->
                SwingUtilities.invokeLater(() -> releaseSlotFromHost(slotIndex))
            );
            hostLobbyFrame.setLocationRelativeTo(frame);
            hostLobbyFrame.setVisible(true);
            startSnapshotTimer();
            pushLobbyState();
        }

        frame.setVisible(true);
        frame.getControlPanel().addLog("=== 모두의 마블 게임 시작 ===");
        frame.getControlPanel().addLog("플레이어 수: " + numPlayers);
        frame.getControlPanel().addLog("초기 자금: " + String.format("%,d", initialCash) + "원\n");

        startTurn();
    }

    private void startSnapshotTimer() {
        if (snapshotTimer != null) {
            snapshotTimer.stop();
        }
        snapshotTimer = new Timer(300, e -> broadcastSnapshot());
        snapshotTimer.setRepeats(true);
        snapshotTimer.start();
    }

    private void handleLobbyConnection(String clientId) {
        if (hostNetworkService == null || lobbyState == null) {
            return;
        }
        lobbyState.onClientConnected(clientId);
        pushLobbyState();
    }

    private void handleLobbyDisconnection(String clientId) {
        if (hostNetworkService == null || lobbyState == null) {
            return;
        }
        Integer slotIndex = lobbyState.getSlotIndex(clientId);
        lobbyState.onClientDisconnected(clientId);
        if (slotIndex != null) {
            players[slotIndex].name = lobbyState.getEffectivePlayerName(slotIndex);
            frame.getOverlayPanel().updatePlayerInfo();
        }
        pushLobbyState();
    }

    private void pushLobbyState() {
        if (hostNetworkService == null || lobbyState == null) {
            return;
        }
        LobbyStateView view = lobbyState.toView(state != GameState.GAME_OVER);
        NetworkMessage message = new NetworkMessage(
            MessageType.LOBBY_STATE,
            LobbyStateCodec.encode(view)
        );
        hostNetworkService.broadcast(message);
        if (hostLobbyFrame != null) {
            hostLobbyFrame.update(view);
        }
        resumeIfReadyGateCleared();
    }

    private void notifySlotAssignment(String clientId, int slotIndex, String playerName,
                                      SlotAssignmentPayload.Status status, String note) {
        if (hostNetworkService == null) {
            return;
        }
        SlotAssignmentPayload payload = new SlotAssignmentPayload(slotIndex, playerName, status, note);
        hostNetworkService.sendTo(
            clientId,
            new NetworkMessage(MessageType.SLOT_ASSIGNMENT, SlotAssignmentPayload.encode(payload))
        );
    }

    private String sanitizePlayerName(String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = raw.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return "";
        }
        return normalized.length() > 16 ? normalized.substring(0, 16) : normalized;
    }

    private boolean shouldWaitForReadyGate() {
        if (hostNetworkService == null || lobbyState == null) {
            waitingForReadyGate = false;
            return false;
        }
        if (lobbyState.areAllAssignedReady()) {
            waitingForReadyGate = false;
            return false;
        }
        if (!waitingForReadyGate) {
            log("[네트워크] 모든 플레이어가 준비 완료될 때까지 대기합니다.");
            broadcastLog("[네트워크] 준비 신호 대기 중입니다.");
        }
        waitingForReadyGate = true;
        state = GameState.WAITING_FOR_READY;
        setActionButtons(false, false, false, false, false, false);
        setTileSelectionEnabled(false);
        if (frame != null) {
            frame.getOverlayPanel().showWaitingMessage("모든 플레이어 준비 대기 중...");
        }
        return true;
    }

    private void resumeIfReadyGateCleared() {
        if (!waitingForReadyGate) {
            return;
        }
        if (lobbyState != null && lobbyState.areAllAssignedReady()) {
            waitingForReadyGate = false;
            if (frame != null) {
                frame.getOverlayPanel().hideWaitingMessage();
            }
            SwingUtilities.invokeLater(this::startTurn);
        }
    }

    private void setActionButtons(boolean roll, boolean purchase, boolean upgrade,
                                  boolean takeover, boolean skip, boolean escape) {
        rollButtonActive = roll;
        purchaseButtonActive = purchase;
        upgradeButtonActive = upgrade;
        takeoverButtonActive = takeover;
        skipButtonActive = skip;
        escapeButtonActive = escape;
        frame.getActionPanel().setButtonsEnabled(roll, purchase, upgrade, takeover, skip, escape);
    }

    private void setTileSelectionEnabled(boolean enabled) {
        tileSelectionEnabled = enabled;
        frame.getBoardPanel().setTileClickEnabled(enabled);
    }

    @Override
    public void handlePlayerInput(PlayerInputEvent event) {
        switch (event.getType()) {
            case GAUGE_PRESS:
                onGaugePressed();
                break;
            case GAUGE_RELEASE:
                onGaugeReleased();
                break;
            case PURCHASE_CITY:
                purchaseCity();
                break;
            case UPGRADE_CITY:
                upgradeCity();
                break;
            case TAKEOVER:
                handleTakeover();
                break;
            case SKIP_TURN:
                skip();
                break;
            case PAY_BAIL:
                escapeWithBail();
                break;
            case TOGGLE_ODD_MODE:
                toggleOddMode();
                break;
            case TOGGLE_EVEN_MODE:
                toggleEvenMode();
                break;
            case TILE_SELECTED:
                onTileSelected(event.requireIntValue());
                break;
        }
    }

    private void onGaugePressed() {
        JButton diceButton = frame.getActionPanel().getRollDiceButton();
        if (diceButton.isEnabled()) {
            frame.getActionPanel().getDiceGauge().start();
            frame.getActionPanel().startGaugeAnimation();
            log("🎯 게이지 타이밍을 잡으세요!");
        }
    }

    private void onGaugeReleased() {
        JButton diceButton = frame.getActionPanel().getRollDiceButton();
        if (diceButton.isEnabled() && frame.getActionPanel().getDiceGauge().isRunning()) {
            rollDiceWithGauge();
        }
    }

    private void handleTakeover() {
        if (currentTile instanceof City) {
            takeoverCity();
        } else if (currentTile instanceof TouristSpot) {
            takeoverTouristSpot();
        }
    }

    private void toggleOddMode() {
        if (diceMode == DiceMode.ODD) {
            diceMode = DiceMode.NORMAL;
            log("일반 주사위 모드");
        } else {
            diceMode = DiceMode.ODD;
            log("🔢 홀수 주사위 모드 선택 (1, 3, 5만 나옴)");
        }
        updateOddEvenButtons();
    }

    private void toggleEvenMode() {
        if (diceMode == DiceMode.EVEN) {
            diceMode = DiceMode.NORMAL;
            log("일반 주사위 모드");
        } else {
            diceMode = DiceMode.EVEN;
            log("🔢 짝수 주사위 모드 선택 (2, 4, 6만 나옴)");
        }
        updateOddEvenButtons();
    }

    private void startTurn() {
        if (isGameOver()) {
            endGame();
            return;
        }

        if (shouldWaitForReadyGate()) {
            return;
        }
        if (frame != null) {
            frame.getOverlayPanel().hideWaitingMessage();
        }

        Player player = players[currentPlayerIndex];
        frame.getActionPanel().clearPriceLabels();

        if (player.bankrupt) {
            nextPlayer();
            return;
        }

        // 페이즈 딜리트: 3의 배수 턴마다 발동
        if (turnCount % 3 == 0 && currentPlayerIndex == 0) {
            executePhaseDelete();
        }

        // 관광지 잠금 해제: 다음 내 턴 시작 시 자동 해제
        ruleEngine.unlockPlayerTouristSpots(currentPlayerIndex);

        log("\n--- " + player.name + "의 차례 ---");
        log(String.format("%s (현금: %,d원, 위치: %d)", player.name, player.cash, player.pos));

        if (player.isInJail()) {
            state = GameState.WAITING_FOR_JAIL_CHOICE;
            setActionButtons(false, false, false, false, true, true);
            setTileSelectionEnabled(false);
            log("무인도에 갇혀있습니다. (남은 턴: " + player.jailTurns + ")");
            log("💰 보석금 200,000원으로 즉시 탈출하거나, ⏭ 패스하여 대기하세요.");
        } else if (player.hasRailroadTicket) {
            state = GameState.WAITING_FOR_RAILROAD_SELECTION;
            setActionButtons(false, false, false, false, false, false);
            setTileSelectionEnabled(true);
            log("🚆 전국철도/세계여행 티켓이 있습니다!");
            log("보드에서 원하는 칸을 클릭하세요.");

            // 도시 선택 안내 다이얼로그 표시
            CitySelectionDialog selectionDialog = new CitySelectionDialog(frame);
            broadcastDialog(DialogSyncPayload.builder(DialogType.CITY_SELECTION).build());
            selectionDialog.setVisible(true);
        } else {
            state = GameState.WAITING_FOR_ROLL;
            setActionButtons(true, false, false, false, false, false);
            setTileSelectionEnabled(false);
            log("주사위를 굴려주세요.");
        }

        updateDisplay();
    }

    /**
     * 게이지 기반 주사위 굴리기
     */
    private void rollDiceWithGauge() {
        Player player = players[currentPlayerIndex];

        if (state == GameState.WAITING_FOR_ROLL || state == GameState.WAITING_FOR_DOUBLE_ROLL) {
            // 게이지 정지 및 결과 생성
            int result = frame.getActionPanel().getDiceGauge().stop();
            frame.getActionPanel().stopGaugeAnimation();

            int section = frame.getActionPanel().getDiceGauge().getCurrentSection();
            String sectionName = getSectionName(section);

            log("🎯 구간: " + sectionName);

            int originalResult = result;

            // 홀수/짝수 필터 적용 (결과값 자체를 홀수/짝수로 조정)
            if (diceMode == DiceMode.ODD && result % 2 == 0) {
                // 짝수 결과를 홀수로 변경 (±1)
                if (result > 2) result -= 1;  // 4→3, 6→5, 8→7, 10→9, 12→11
                else result += 1;  // 2→3
            } else if (diceMode == DiceMode.EVEN && result % 2 == 1) {
                // 홀수 결과를 짝수로 변경 (±1)
                if (result < 12) result += 1;  // 3→4, 5→6, 7→8, 9→10, 11→12
                else result -= 1;  // 극히 드문 경우
            }

            int[] dicePair = getRandomDicePairForSum(result);
            int tempD1 = dicePair[0];
            int tempD2 = dicePair[1];
            boolean isDouble = (tempD1 == tempD2);

            // 더블 확률 억제 시스템 (연속 더블 횟수에 따라)
            // 첫 번째 주사위: 60%, 두 번째: 20%, 세 번째: 0%
            boolean showSuppressionDialog = false;
            if (isDouble) {
                double suppressProbability = getDoubleSuppressProbability(consecutiveDoubles);
                if (Math.random() < suppressProbability) {
                    // 합계 2(1,1) 또는 12(6,6)는 더블만 가능
                    // 이 경우 주사위 값을 조정하여 더블 무효화
                    if (result == 2 || result == 12) {
                        // 주사위 값을 비더블로 조정
                        if (result == 2) {
                            if (diceMode == DiceMode.EVEN) {
                                // 짝수 모드에서는 합계가 짝수로 유지되도록 (1,3)으로 조정
                                tempD1 = 1;
                                tempD2 = 3;
                            } else {
                                // 기본 동작: (1,2)로 조정 (합계 3)
                                tempD1 = 1;
                                tempD2 = 2;
                            }
                        } else {
                            if (diceMode == DiceMode.EVEN) {
                                // 짝수 모드에서는 합계가 짝수로 유지되도록 (6,4)로 조정
                                tempD1 = 6;
                                tempD2 = 4;
                            } else {
                                // 기본 동작: (6,5)로 조정 (합계 11)
                                tempD1 = 6;
                                tempD2 = 5;
                            }
                        }
                        isDouble = false;

                        // 연속 더블 2번 이후에만 억제 다이얼로그 표시
                        if (consecutiveDoubles >= 2) {
                            showSuppressionDialog = true;
                        }

                        logDoubleSuppression(originalResult, tempD1 + tempD2);
                    } else {
                        // 강제로 비더블로 변환 (±1 조정)
                        if (tempD1 > 1) {
                            tempD1 -= 1;
                            tempD2 += 1;
                        } else {
                            tempD1 += 1;
                            tempD2 -= 1;
                        }
                        isDouble = false;
                        logDoubleSuppression(originalResult, tempD1 + tempD2);
                    }
                }
            }

            // final 변수로 복사 (람다 사용을 위해)
            final int finalD1 = tempD1;
            final int finalD2 = tempD2;
            final int finalResult = finalD1 + finalD2;
            final boolean finalIsDouble = isDouble;
            final boolean finalShowSuppressionDialog = showSuppressionDialog;
            final int finalConsecutiveDoubles = consecutiveDoubles;

            // 주사위 값 저장 (나중에 더블 체크용)
            lastD1 = finalD1;
            lastD2 = finalD2;
            diceRollSequence++;

            // 주사위 애니메이션 시작
            frame.getActionPanel().getDiceAnimationPanel().startAnimation(finalD1, finalD2, () -> {
                if (finalIsDouble) {
                    log("🎲 주사위: [" + finalD1 + ", " + finalD2 + "] = " + finalResult + " - 더블!");
                } else {
                    log("주사위: [" + finalD1 + ", " + finalD2 + "] = " + finalResult);
                }

                // 연속 더블 2번 후 합계 2 또는 12인 경우 억제 다이얼로그 표시
                if (finalShowSuppressionDialog) {
                    DoubleSuppressedDialog suppressedDialog = new DoubleSuppressedDialog(
                        frame, finalD1, finalConsecutiveDoubles);
                    broadcastDialog(
                        DialogSyncPayload.builder(DialogType.DOUBLE_SUPPRESSED)
                            .putInt("diceValue", finalD1)
                            .putInt("consecutive", finalConsecutiveDoubles)
                            .build()
                    );
                    suppressedDialog.setVisible(true);
                }

                movePlayer(finalResult);
            });
        }
    }

    /**
     * 구간 이름 반환
     */
    private String getSectionName(int section) {
        switch (section) {
            case 1: return "S1 (2~4 우대)";
            case 2: return "S2 (4~6 우대)";
            case 3: return "S3 (7~10 우대)";
            case 4: return "S4 (9~12 우대)";
            default: return "Unknown";
        }
    }

    private static int[][][] createSumToDiceCombinations() {
        int[][][] combos = new int[13][][];
        combos[2] = new int[][]{{1, 1}};
        combos[3] = new int[][]{{1, 2}, {2, 1}};
        combos[4] = new int[][]{{1, 3}, {2, 2}, {3, 1}};
        combos[5] = new int[][]{{1, 4}, {2, 3}, {3, 2}, {4, 1}};
        combos[6] = new int[][]{{1, 5}, {2, 4}, {3, 3}, {4, 2}, {5, 1}};
        combos[7] = new int[][]{{1, 6}, {2, 5}, {3, 4}, {4, 3}, {5, 2}, {6, 1}};
        combos[8] = new int[][]{{2, 6}, {3, 5}, {4, 4}, {5, 3}, {6, 2}};
        combos[9] = new int[][]{{3, 6}, {4, 5}, {5, 4}, {6, 3}};
        combos[10] = new int[][]{{4, 6}, {5, 5}, {6, 4}};
        combos[11] = new int[][]{{5, 6}, {6, 5}};
        combos[12] = new int[][]{{6, 6}};
        return combos;
    }

    private int[] getRandomDicePairForSum(int sum) {
        if (sum < 2 || sum > 12) {
            return new int[]{1, 1};
        }
        int[][] combos = SUM_TO_DICE_COMBINATIONS[sum];
        if (combos == null || combos.length == 0) {
            return new int[]{1, 1};
        }
        int idx = (int)(Math.random() * combos.length);
        return combos[idx];
    }

    /**
     * 더블 억제 확률 계산
     * @param consecutiveCount 연속 더블 횟수
     * @return 억제 확률 (0.0 ~ 1.0)
     */
    private double getDoubleSuppressProbability(int consecutiveCount) {
        switch (consecutiveCount) {
            case 0: return 0.4;    // 1차 더블: 40% 억제 (60% 더블 가능)
            case 1: return 0.8;    // 2차 더블: 80% 억제 (20% 더블 가능)
            default: return 1.0;   // 3차 이상: 100% 억제 (0% 더블 가능)
        }
    }

    private void logDoubleSuppression(int originalSum, int newSum) {
        String probabilityText;
        switch (consecutiveDoubles) {
            case 0:
                probabilityText = "60% 확률";
                break;
            case 1:
                probabilityText = "20% 확률";
                break;
            default:
                probabilityText = "0% 확률";
                break;
        }

        if (originalSum > 0 && newSum > 0) {
            log("🎲 더블 억제 발동! (" + probabilityText + ") - 합계 " + originalSum + " → " + newSum);
        } else {
            log("🎲 더블 억제 발동! (" + probabilityText + ")");
        }
    }

    private void clearDoubleState(String messageIfDouble) {
        boolean wasDouble = (lastD1 == lastD2 && lastD1 > 0);
        lastD1 = 0;
        lastD2 = 0;
        consecutiveDoubles = 0;

        if (wasDouble && messageIfDouble != null && !messageIfDouble.isEmpty()) {
            log(messageIfDouble);
        }
    }

    /**
     * 더블 체크 및 처리
     */
    private boolean checkAndHandleDouble() {
        boolean isDouble = (lastD1 == lastD2 && lastD1 > 0);

        if (isDouble) {
            consecutiveDoubles++;
            return true;
        } else {
            return false;
        }
    }

    private void movePlayer(int steps) {
        Player player = players[currentPlayerIndex];
        if (steps <= 0) {
            currentTile = board.getTile(player.pos);
            log(player.name + "이(가) " + currentTile.name + "에 도착했습니다.");
            handleTileLanding();
            return;
        }

        startMovementAnimation(player, steps);
    }

    private void handleTileLanding() {
        Player player = players[currentPlayerIndex];
        frame.getActionPanel().clearPriceLabels();

        switch (currentTile.type) {
            case START:
                clearDoubleState("🎲 더블이었지만 START 칸에서 무효가 되었습니다.");
                handleStartTile();
                break;

            case CITY:
                handleCityTile((City) currentTile);
                break;

            case TOURIST_SPOT:
                handleTouristSpotTile((TouristSpot) currentTile);
                break;

            case ISLAND:
                player.jailTurns = 2; // 2턴 갇힘
                // 무인도 다이얼로그 표시
                IslandDialog islandDialog = new IslandDialog(frame, player.jailTurns);
                broadcastDialog(
                    DialogSyncPayload.builder(DialogType.ISLAND_STATUS)
                        .putInt("jailTurns", player.jailTurns)
                        .build()
                );
                islandDialog.setVisible(true);

                log("무인도에 도착했습니다!");
                clearDoubleState("🎲 더블이었지만 무인도에 갇혀 무효가 되었습니다.");
                log("무인도에 " + player.jailTurns + "턴 동안 갇힙니다.");
                endTurn();
                break;

            case CHANCE:
                int chanceReward = ruleEngine.getChanceReward();
                ruleEngine.processChance(player);

                // 자산 변동 표시
                frame.getOverlayPanel().showMoneyChange(currentPlayerIndex, chanceReward);

                // 찬스 다이얼로그 표시
                ChanceDialog chanceDialog = new ChanceDialog(frame, chanceReward);
                broadcastDialog(
                    DialogSyncPayload.builder(DialogType.CHANCE_REWARD)
                        .putInt("amount", chanceReward)
                        .build()
                );
                chanceDialog.setVisible(true);

                log("찬스 카드! " + String.format("%,d", chanceReward) + "원을 받았습니다!");
                endTurn();
                break;

            case WELFARE:
                log("사회복지기금에 도착했습니다! (기능 미구현)");
                endTurn();
                break;

            case RAILROAD:
                log("전국철도에 도착했습니다!");
                log("다음 턴에 원하는 칸을 선택할 수 있습니다!");
                player.hasRailroadTicket = true;
                endTurn();
                break;

            case TAX:
                handleTaxTile();
                break;

            case OLYMPIC:
                clearDoubleState("🎲 더블이었지만 올림픽 칸에서 무효가 되었습니다.");
                handleOlympicTile();
                break;

            case WORLD_TOUR:
                // 세계여행 다이얼로그 표시
                WorldTourDialog worldTourDialog = new WorldTourDialog(frame);
                broadcastDialog(DialogSyncPayload.builder(DialogType.WORLD_TOUR).build());
                worldTourDialog.setVisible(true);

                log("세계여행에 도착했습니다!");
                clearDoubleState("🎲 더블이었지만 세계여행 칸에서 무효가 되었습니다.");
                log("다음 턴에 원하는 칸을 선택할 수 있습니다!");
                player.hasRailroadTicket = true; // 전국철도와 동일한 효과
                endTurn();
                break;
        }

        updateDisplay();
    }

    private void handleCityTile(City city) {
        Player player = players[currentPlayerIndex];

        if (!city.isOwned()) {
            // 미소유 땅
            log(city.name + "은(는) 미소유 땅입니다. (가격: " + String.format("%,d", city.price) + "원)");
            state = GameState.WAITING_FOR_ACTION;
            setActionButtons(false, true, false, false, true, false);
        } else if (city.owner == currentPlayerIndex) {
            // 본인 소유 땅
            log(city.name + "은(는) 본인 소유입니다. (레벨: " + city.level + ")");

            // 본인 랜드마크 도착 시 듀얼 마그네틱 코어 발동
            if (city.isLandmark()) {
                int landmarkPos = city.id;
                int pulledCount = ruleEngine.applyDualMagneticCore(landmarkPos, players, currentPlayerIndex);

                // 다이얼로그 표시
                DualMagneticDialog magneticDialog = new DualMagneticDialog(frame, city.name, pulledCount);
                broadcastDialog(
                    DialogSyncPayload.builder(DialogType.DUAL_MAGNETIC)
                        .put("cityName", city.name)
                        .putInt("pulledCount", pulledCount)
                        .build()
                );
                magneticDialog.setVisible(true);

                if (pulledCount > 0) {
                    log("🧲 듀얼 마그네틱 코어 발동! " + pulledCount + "명의 플레이어를 끌어당깁니다!");

                    // 끌려온 플레이어들에게 통행료 징수
                    handleMagneticTollCollection(city);
                } else {
                    log("🧲 듀얼 마그네틱 코어 발동! 범위 내 플레이어가 없습니다.");
                }

                endTurn();
                return;
            }

            if (city.canUpgrade()) {
                int upgradeCost = city.getUpgradeCost();
                log("업그레이드 비용: " + String.format("%,d", upgradeCost) + "원");
                state = GameState.WAITING_FOR_ACTION;
                setActionButtons(false, false, true, false, true, false);
            } else {
                log("최대 레벨입니다. 더 이상 업그레이드할 수 없습니다.");
                endTurn();
            }
        } else {
            // 타인 소유 땅
            Player owner = players[city.owner];
            int toll = ruleEngine.calculateToll(city, city.owner);

            log(city.name + "은(는) " + owner.name + "의 소유입니다. (레벨: " + city.level + ")");

            // 올림픽 효과 표시
            if (city.hasOlympicBoost) {
                log("⚡ 올림픽 효과로 통행료 2배!");
            }

            // 통행료 지불 확인 다이얼로그
            TollPaymentDialog tollDialog = new TollPaymentDialog(
                frame,
                city.name,
                owner.name,
                city.level,
                toll,
                city.hasOlympicBoost,
                player.cash
            );
            broadcastDialog(
                DialogSyncPayload.builder(DialogType.TOLL_PAYMENT)
                    .put("cityName", city.name)
                    .put("ownerName", owner.name)
                    .putInt("level", city.level)
                    .putInt("toll", toll)
                    .putBoolean("olympic", city.hasOlympicBoost)
                    .putInt("playerCash", player.cash)
                    .build()
            );
            tollDialog.setVisible(true);

            log("💸 통행료 " + String.format("%,d", toll) + "원을 지불합니다.");
            ruleEngine.payToll(player, owner, toll);

            // 자산 변동 표시
            frame.getOverlayPanel().showMoneyChange(currentPlayerIndex, -toll);
            frame.getOverlayPanel().showMoneyChange(city.owner, toll);

            // 올림픽 효과 해제 (한 번 통행료 지불 후)
            if (city.hasOlympicBoost) {
                ruleEngine.removeOlympicBoost(city);
                log("올림픽 효과가 해제되었습니다.");
            }

            if (player.bankrupt) {
                log(player.name + "이(가) 파산했습니다!");
                endTurn();
            } else {
                // 랜드마크는 인수 불가
                if (city.isLandmark()) {
                    log("🏛️ 랜드마크는 인수할 수 없습니다.");
                    endTurn();
                } else {
                    // 통행료 지불 후 인수 선택지 제공
                    int takeoverCost = city.getTakeoverPrice();
                    log("💰 인수 비용: " + String.format("%,d", takeoverCost) + "원");
                    log("이 땅을 인수하거나 패스하세요.");
                    state = GameState.WAITING_FOR_ACTION;
                    setActionButtons(false, false, false, true, true, false);
                }
            }
        }
    }

    private void handleTouristSpotTile(TouristSpot touristSpot) {
        Player player = players[currentPlayerIndex];

        if (!touristSpot.isOwned()) {
            // 미소유 관광지 → 매입 다이얼로그 → 선택지 다이얼로그
            log(touristSpot.name + "은(는) 미소유 관광지입니다. (가격: " + String.format("%,d", touristSpot.price) + "원)");

            // 매입 다이얼로그 표시
            TouristSpotPurchaseDialog purchaseDialog = new TouristSpotPurchaseDialog(
                frame,
                touristSpot.name,
                touristSpot.price,
                player.cash
            );
            broadcastDialog(
                DialogSyncPayload.builder(DialogType.TOURIST_PURCHASE)
                    .put("spotName", touristSpot.name)
                    .putInt("price", touristSpot.price)
                    .putInt("playerCash", player.cash)
                    .build()
            );
            purchaseDialog.setVisible(true);

            // 매입 처리
            if (purchaseDialog.isConfirmed()) {
                if (ruleEngine.purchaseTouristSpot(player, touristSpot, currentPlayerIndex)) {
                    log("✅ " + touristSpot.name + "을(를) 매입했습니다!");
                    frame.getOverlayPanel().showMoneyChange(currentPlayerIndex, -touristSpot.price);
                } else {
                    log("❌ 매입 실패!");
                }
            } else {
                log("매입을 취소했습니다.");
            }

            // 매입 성공 여부와 관계없이 선택지 다이얼로그 표시
            showTouristSpotChoiceDialog(touristSpot, player);

        } else if (touristSpot.owner == currentPlayerIndex) {
            // 본인 소유 관광지 → 선택지 다이얼로그만 표시
            log(touristSpot.name + "은(는) 본인 소유 관광지입니다.");
            showTouristSpotChoiceDialog(touristSpot, player);

        } else {
            // 타인 소유 관광지
            Player owner = players[touristSpot.owner];
            int toll = ruleEngine.calculateTouristSpotToll(touristSpot);

            log(touristSpot.name + "은(는) " + owner.name + "의 소유 관광지입니다.");

            // 잠금 여부 체크
            if (touristSpot.isLocked()) {
                log("🔒 이 관광지는 잠금 상태입니다! (인수 불가)");
            }

            // 통행료 지불 확인 다이얼로그 (관광지는 레벨 1로 표시)
            TollPaymentDialog tollDialog = new TollPaymentDialog(
                frame,
                touristSpot.name,
                owner.name,
                1,  // 관광지는 레벨 개념 없음
                toll,
                false,  // 관광지는 올림픽 효과 없음
                player.cash
            );
            broadcastDialog(
                DialogSyncPayload.builder(DialogType.TOLL_PAYMENT)
                    .put("cityName", touristSpot.name)
                    .put("ownerName", owner.name)
                    .putInt("level", 1)
                    .putInt("toll", toll)
                    .putBoolean("olympic", false)
                    .putInt("playerCash", player.cash)
                    .build()
            );
            tollDialog.setVisible(true);

            log("💸 통행료 " + String.format("%,d", toll) + "원을 지불합니다.");
            ruleEngine.payToll(player, owner, toll);

            // 자산 변동 표시
            frame.getOverlayPanel().showMoneyChange(currentPlayerIndex, -toll);
            frame.getOverlayPanel().showMoneyChange(touristSpot.owner, toll);

            // 잠금된 관광지는 통행료 지불 후 잠금 해제
            if (touristSpot.isLocked()) {
                ruleEngine.unlockTouristSpot(touristSpot);
                log("🔓 관광지 잠금이 해제되었습니다.");

                if (player.bankrupt) {
                    log(player.name + "이(가) 파산했습니다!");
                }
                endTurn();
            } else {
                // 잠금되지 않은 경우 인수 선택지 제공
                if (player.bankrupt) {
                    log(player.name + "이(가) 파산했습니다!");
                    endTurn();
                } else {
                    // 통행료 지불 후 인수 선택지 제공
                    int takeoverCost = touristSpot.price;
                    log("💰 인수 비용: " + String.format("%,d", takeoverCost) + "원");
                    log("이 관광지를 인수하거나 패스하세요.");
                    state = GameState.WAITING_FOR_ACTION;
                    setActionButtons(false, false, false, true, true, false);
                }
            }
        }
    }

    /**
     * 관광지 선택지 다이얼로그 표시 (잠금 / 주사위 한 번 더)
     */
    private void showTouristSpotChoiceDialog(TouristSpot touristSpot, Player player) {
        log("행동을 선택하세요.");

        TouristSpotChoiceDialog choiceDialog = new TouristSpotChoiceDialog(
            frame,
            touristSpot.name
        );
        broadcastDialog(
            DialogSyncPayload.builder(DialogType.TOURIST_CHOICE)
                .put("spotName", touristSpot.name)
                .build()
        );
        choiceDialog.setVisible(true);

        TouristSpotChoiceDialog.Choice choice = choiceDialog.getSelectedChoice();

        switch (choice) {
            case LOCK:
                // 잠금
                ruleEngine.lockTouristSpot(touristSpot, currentPlayerIndex);
                log("🔒 " + touristSpot.name + "을(를) 잠금 설정했습니다! (다음 내 턴까지 인수 불가)");
                endTurn();
                break;

            case EXTRA_ROLL:
                // 주사위 한 번 더
                player.hasExtraChance = true;
                log("🎲 추가 주사위 기회를 획득했습니다!");
                endTurn();
                break;
        }
    }

    private void purchaseCity() {
        Player player = players[currentPlayerIndex];

        if (currentTile instanceof City) {
            City city = (City) currentTile;

            // 레벨 선택 다이얼로그 표시
            LevelSelectionDialog dialog = new LevelSelectionDialog(
                frame,
                city.name,
                city.price,
                player.cash
            );
            broadcastDialog(
                DialogSyncPayload.builder(DialogType.LEVEL_SELECTION)
                    .put("cityName", city.name)
                    .putInt("price", city.price)
                    .putInt("playerCash", player.cash)
                    .build()
            );
            dialog.setVisible(true);

            int selectedLevel = dialog.getSelectedLevel();

            if (selectedLevel == 0) {
                // 취소 선택
                log("구매를 취소했습니다.");
                endTurn();
                return;
            }

            // 선택한 레벨로 구매 시도
            if (ruleEngine.purchaseCityWithLevel(player, city, selectedLevel, currentPlayerIndex)) {
                int totalCost = ruleEngine.calculateLevelCost(city.price, selectedLevel);
                String levelName = getLevelName(selectedLevel);
                String emoji = city.getBuildingEmoji();

                // 자산 변동 표시
                frame.getOverlayPanel().showMoneyChange(currentPlayerIndex, -totalCost);

                log(player.name + "이(가) " + city.name + "을(를) " +
                    String.format("%,d", totalCost) + "원에 매입했습니다!");
                log(emoji + " " + levelName + "이(가) 건설되었습니다! (레벨 " + selectedLevel + ")");
            } else {
                log("자금이 부족하여 매입할 수 없습니다.");
            }
        } else if (currentTile instanceof TouristSpot) {
            TouristSpot touristSpot = (TouristSpot) currentTile;

            // 관광지 매입 확인 다이얼로그 표시
            TouristSpotPurchaseDialog dialog = new TouristSpotPurchaseDialog(
                frame,
                touristSpot.name,
                touristSpot.price,
                player.cash
            );
            broadcastDialog(
                DialogSyncPayload.builder(DialogType.TOURIST_PURCHASE)
                    .put("spotName", touristSpot.name)
                    .putInt("price", touristSpot.price)
                    .putInt("playerCash", player.cash)
                    .build()
            );
            dialog.setVisible(true);

            if (!dialog.isConfirmed()) {
                // 취소 선택
                log("구매를 취소했습니다.");
                endTurn();
                return;
            }

            // 매입 시도
            if (ruleEngine.purchaseTouristSpot(player, touristSpot, currentPlayerIndex)) {
                // 자산 변동 표시
                frame.getOverlayPanel().showMoneyChange(currentPlayerIndex, -touristSpot.price);

                log(player.name + "이(가) " + touristSpot.name + "을(를) " +
                    String.format("%,d", touristSpot.price) + "원에 매입했습니다!");
            } else {
                log("자금이 부족하여 매입할 수 없습니다.");
            }
        }

        endTurn();
    }

    private void upgradeCity() {
        Player player = players[currentPlayerIndex];
        City city = (City) currentTile;

        int upgradeCost = city.getUpgradeCost();
        if (ruleEngine.upgradeCity(player, city)) {
            // 자산 변동 표시
            frame.getOverlayPanel().showMoneyChange(currentPlayerIndex, -upgradeCost);

            String levelEmoji = city.getBuildingEmoji();
            String levelName = "";
            switch (city.level) {
                case 2: levelName = "아파트"; break;
                case 3: levelName = "건물"; break;
                case 4: levelName = "랜드마크"; break;
            }
            log(city.name + "을(를) 레벨 " + city.level + "(" + levelName + " " + levelEmoji + ")로 업그레이드했습니다!");

            // 랜드마크 건설 시 듀얼 마그네틱 코어 발동
            if (city.isLandmark()) {
                log("🏛️ 랜드마크가 건설되었습니다! 다른 플레이어는 이 땅을 인수할 수 없습니다.");

                int landmarkPos = city.id;
                int pulledCount = ruleEngine.applyDualMagneticCore(landmarkPos, players, currentPlayerIndex);

                // 다이얼로그 표시
                DualMagneticDialog magneticDialog = new DualMagneticDialog(frame, city.name, pulledCount);
                broadcastDialog(
                    DialogSyncPayload.builder(DialogType.DUAL_MAGNETIC)
                        .put("cityName", city.name)
                        .putInt("pulledCount", pulledCount)
                        .build()
                );
                magneticDialog.setVisible(true);

                if (pulledCount > 0) {
                    log("🧲 듀얼 마그네틱 코어 발동! " + pulledCount + "명의 플레이어를 끌어당깁니다!");

                    // 끌려온 플레이어들에게 통행료 징수
                    handleMagneticTollCollection(city);
                } else {
                    log("🧲 듀얼 마그네틱 코어 발동! 범위 내 플레이어가 없습니다.");
                }
            }
        } else {
            log("자금이 부족하여 업그레이드할 수 없습니다.");
        }

        endTurn();
    }

    private void takeoverCity() {
        Player buyer = players[currentPlayerIndex];
        City city = (City) currentTile;
        Player seller = players[city.owner];

        int takeoverCost = city.getTakeoverPrice();

        // 인수 확인 다이얼로그
        TakeoverConfirmDialog dialog = new TakeoverConfirmDialog(
            frame,
            city.name,
            seller.name,
            city.level,
            takeoverCost,
            buyer.cash
        );
        broadcastDialog(
            DialogSyncPayload.builder(DialogType.TAKEOVER_CONFIRM)
                .put("cityName", city.name)
                .put("ownerName", seller.name)
                .putInt("level", city.level)
                .putInt("cost", takeoverCost)
                .putInt("playerCash", buyer.cash)
                .build()
        );
        dialog.setVisible(true);

        if (!dialog.isConfirmed()) {
            log("도시 인수를 취소했습니다.");
            endTurn();
            return;
        }

        // 인수 진행
        if (ruleEngine.takeoverCity(buyer, seller, city, currentPlayerIndex)) {
            log(buyer.name + "이(가) " + seller.name + "으로부터 " + city.name + "을(를) " +
                String.format("%,d", takeoverCost) + "원에 인수했습니다!");
            log(seller.name + "이(가) " + String.format("%,d", takeoverCost) + "원을 받았습니다.");
        } else if (city.isLandmark()) {
            log("🏛️ 랜드마크는 인수할 수 없습니다.");
        } else {
            log("자금이 부족하여 인수할 수 없습니다.");
        }

        endTurn();
    }

    private void takeoverTouristSpot() {
        Player buyer = players[currentPlayerIndex];
        TouristSpot spot = (TouristSpot) currentTile;
        Player seller = players[spot.owner];

        int takeoverCost = spot.price;

        // 인수 확인 다이얼로그 (관광지는 레벨 1로 표시)
        TakeoverConfirmDialog dialog = new TakeoverConfirmDialog(
            frame,
            spot.name,
            seller.name,
            1,  // 관광지는 레벨 개념 없음
            takeoverCost,
            buyer.cash
        );
        broadcastDialog(
            DialogSyncPayload.builder(DialogType.TAKEOVER_CONFIRM)
                .put("cityName", spot.name)
                .put("ownerName", seller.name)
                .putInt("level", 1)
                .putInt("cost", takeoverCost)
                .putInt("playerCash", buyer.cash)
                .build()
        );
        dialog.setVisible(true);

        if (!dialog.isConfirmed()) {
            log("관광지 인수를 취소했습니다.");
            endTurn();
            return;
        }

        // 인수 진행
        if (ruleEngine.takeoverTouristSpot(buyer, seller, spot, currentPlayerIndex)) {
            log(buyer.name + "이(가) " + seller.name + "으로부터 " + spot.name + "을(를) " +
                String.format("%,d", takeoverCost) + "원에 인수했습니다!");
            log(seller.name + "이(가) " + String.format("%,d", takeoverCost) + "원을 받았습니다.");

            // 자산 변동 표시
            frame.getOverlayPanel().showMoneyChange(currentPlayerIndex, -takeoverCost);
            frame.getOverlayPanel().showMoneyChange(spot.owner, takeoverCost);

            // 인수 후 선택지 다이얼로그 표시
            showTouristSpotChoiceDialog(spot, buyer);
            return; // endTurn()은 showTouristSpotChoiceDialog 내에서 호출됨
        } else if (spot.isLocked()) {
            log("🔒 잠금된 관광지는 인수할 수 없습니다.");
        } else {
            log("자금이 부족하여 인수할 수 없습니다.");
        }

        endTurn();
    }

    private void skip() {
        Player player = players[currentPlayerIndex];

        if (player.isInJail()) {
            // 무인도에서 패스하면 턴 감소
            ruleEngine.decreaseJailTurns(player);
            log(player.name + "이(가) 턴을 패스했습니다. (남은 무인도 턴: " + player.jailTurns + ")");

            if (player.jailTurns == 0) {
                log("무인도 3턴이 지나 자동으로 탈출합니다!");
            }
        } else {
            log(player.name + "이(가) 패스했습니다.");
        }

        endTurn();
    }

    private void escapeWithBail() {
        Player player = players[currentPlayerIndex];

        if (ruleEngine.escapeIslandWithBail(player)) {
            log("보석금 200,000원을 내고 무인도에서 탈출했습니다!");
            state = GameState.WAITING_FOR_ROLL;
            setActionButtons(true, false, false, false, false, false);
            updateDisplay();
        } else {
            log("보석금이 부족합니다.");
        }
    }

    /**
     * 타일 선택 이벤트 (전국철도 티켓 사용)
     */
    private void onTileSelected(int tileIndex) {
        Player player = players[currentPlayerIndex];
        Tile selectedTile = board.getTile(tileIndex);

        // 전국철도 선택 처리
        if (state == GameState.WAITING_FOR_RAILROAD_SELECTION) {
            log(player.name + "이(가) " + selectedTile.name + " (칸 " + tileIndex + ")을(를) 선택했습니다!");

            // 선택한 칸으로 이동
            player.pos = tileIndex;
            player.hasRailroadTicket = false; // 티켓 사용
            currentTile = selectedTile;

            // 타일 클릭 비활성화
            setTileSelectionEnabled(false);

            // 선택한 타일 처리
            log("선택한 칸에서 이벤트를 처리합니다.");
            handleTileLanding();
            return;
        }

        // 랜드마크 건설 확정 처리
        if (state == GameState.WAITING_FOR_LANDMARK_SELECTION) {
            // 클릭한 타일이 도시인지 확인
            if (!(selectedTile instanceof City)) {
                log("도시가 아닌 칸을 선택했습니다.");
                showErrorDialog("선택 오류", "도시가 아닌 칸을 선택했습니다.");
                return; // 재선택 가능하도록 상태 유지
            }

            City city = (City) selectedTile;

            // 본인 소유 도시인지 확인
            if (!city.isOwned() || city.owner != currentPlayerIndex) {
                log("본인 소유 도시가 아닙니다.");
                showErrorDialog("선택 오류", "본인 소유 도시가 아닙니다.");
                return; // 재선택 가능
            }

            // 레벨 1~3인지 확인 (업그레이드 가능한지)
            if (city.level < 1 || city.level >= 4) {
                log("업그레이드할 수 없는 도시입니다. (레벨 1~3만 가능)");
                showErrorDialog("선택 오류", "업그레이드할 수 없는 도시입니다.");
                return; // 재선택 가능
            }

            // 조건을 만족하면 업그레이드 진행
            log(player.name + "이(가) " + selectedTile.name + " 업그레이드를 확정했습니다!");
            selectedLandmarkCity = city;  // 선택된 도시 저장
            handleLandmarkConstruction();
            return;
        }
    }

    private void handleTaxTile() {
        Player player = players[currentPlayerIndex];
        int tax = ruleEngine.calculateTax(player);

        log("국세청에 도착했습니다!");

        // 세금 납부 확인 다이얼로그
        TaxPaymentDialog taxDialog = new TaxPaymentDialog(
            frame,
            player.cash,
            tax
        );
        broadcastDialog(
            DialogSyncPayload.builder(DialogType.TAX_PAYMENT)
                .putInt("playerCash", player.cash)
                .putInt("taxAmount", tax)
                .build()
        );
        taxDialog.setVisible(true);

        log("💸 보유 금액의 10%를 세금으로 납부합니다: " + String.format("%,d", tax) + "원");
        ruleEngine.payTax(player);

        // 자산 변동 표시
        frame.getOverlayPanel().showMoneyChange(currentPlayerIndex, -tax);

        if (player.bankrupt) {
            log(player.name + "이(가) 파산했습니다!");
        }

        // 세금 납부 후 즉시 턴 종료
        endTurn();
    }

    private void handleStartTile() {
        Player player = players[currentPlayerIndex];
        log("START 지점에 도착했습니다!");

        // 업그레이드 가능한 도시가 있는지 확인 (레벨 1~3인 본인 소유 도시)
        boolean hasUpgradeableCity = false;
        for (Tile tile : board.getAllTiles()) {
            if (tile instanceof City) {
                City city = (City) tile;
                if (city.isOwned() && city.owner == currentPlayerIndex && city.level >= 1 && city.level < 4) {
                    hasUpgradeableCity = true;
                    break;
                }
            }
        }

        if (!hasUpgradeableCity) {
            log("업그레이드할 수 있는 도시가 없습니다. (레벨 1~3 도시 필요)");
            endTurn();
            return;
        }

        // 간단한 안내 메시지 다이얼로그 표시
        log("⬆️ 본인 소유 도시를 1단계 업그레이드할 수 있습니다!");

        String upgradeGuideMessage = "원하는 도시를 선택해주세요!\n\n보드에서 본인 소유 도시(레벨 1~3)를 클릭하면 1단계 업그레이드됩니다.";
        broadcastDialog(
            DialogSyncPayload.builder(DialogType.UPGRADE_GUIDE)
                .put("title", "도시 업그레이드")
                .put("message", upgradeGuideMessage)
                .build()
        );
        JOptionPane.showMessageDialog(
            frame,
            upgradeGuideMessage,
            "도시 업그레이드",
            JOptionPane.INFORMATION_MESSAGE
        );

        // 보드 클릭 대기 상태로 전환
        state = GameState.WAITING_FOR_LANDMARK_SELECTION;
        setTileSelectionEnabled(true);
        log("📍 업그레이드할 도시를 클릭하세요. (레벨 1→2, 2→3, 3→4)");
    }

    private void handleLandmarkConstruction() {
        Player player = players[currentPlayerIndex];

        if (selectedLandmarkCity == null) {
            log("오류: 선택된 도시가 없습니다.");
            endTurn();
            return;
        }

        // 업그레이드 비용 계산 (City.getUpgradeCost() 사용)
        int upgradeCost = selectedLandmarkCity.getUpgradeCost();

        if (!player.canAfford(upgradeCost)) {
            log("잔액이 부족하여 업그레이드할 수 없습니다.");
            showErrorDialog("잔액 부족", "업그레이드 비용이 부족합니다.");
            selectedLandmarkCity = null;
            setTileSelectionEnabled(false);
            endTurn();
            return;
        }

        // 현재 레벨 저장
        int previousLevel = selectedLandmarkCity.level;

        // 업그레이드 실행
        player.pay(upgradeCost);
        selectedLandmarkCity.upgrade();

        // 자산 변동 표시
        frame.getOverlayPanel().showMoneyChange(currentPlayerIndex, -upgradeCost);

        // 업그레이드 메시지
        String[] levelNames = {"", "🏠 집", "🏢 아파트", "🏬 건물", "🏛️ 랜드마크"};
        log("⬆️ " + selectedLandmarkCity.name + "을(를) 업그레이드했습니다!");
        log(levelNames[previousLevel] + " → " + levelNames[selectedLandmarkCity.level]);
        log("업그레이드 비용: " + String.format("%,d", upgradeCost) + "원");
        log("남은 잔액: " + String.format("%,d", player.cash) + "원");

        // 랜드마크 건설 시 듀얼 마그네틱 코어 발동
        if (selectedLandmarkCity.level == 4) {
            int landmarkPos = selectedLandmarkCity.id;
            int pulledCount = ruleEngine.applyDualMagneticCore(landmarkPos, players, currentPlayerIndex);

            // 다이얼로그 표시
            DualMagneticDialog magneticDialog = new DualMagneticDialog(frame, selectedLandmarkCity.name, pulledCount);
            broadcastDialog(
                DialogSyncPayload.builder(DialogType.DUAL_MAGNETIC)
                    .put("cityName", selectedLandmarkCity.name)
                    .putInt("pulledCount", pulledCount)
                    .build()
            );
            magneticDialog.setVisible(true);

            if (pulledCount > 0) {
                log("🧲 듀얼 마그네틱 코어 발동! " + pulledCount + "명의 플레이어를 끌어당깁니다!");

                // 끌려온 플레이어들에게 통행료 징수
                handleMagneticTollCollection(selectedLandmarkCity);
            } else {
                log("🧲 듀얼 마그네틱 코어 발동! 범위 내 플레이어가 없습니다.");
            }
        }

        // 상태 초기화
        selectedLandmarkCity = null;
        state = GameState.WAITING_FOR_ROLL;
        setTileSelectionEnabled(false);

        endTurn();
    }

    private void handleOlympicTile() {
        Player player = players[currentPlayerIndex];

        // 올림픽 다이얼로그 표시
        OlympicDialog olympicDialog = new OlympicDialog(frame);
        broadcastDialog(DialogSyncPayload.builder(DialogType.OLYMPIC).build());
        olympicDialog.setVisible(true);

        log("올림픽에 도착했습니다!");

        // 플레이어가 소유한 도시 찾기
        List<City> ownedCities = new java.util.ArrayList<>();
        for (Tile tile : board.getAllTiles()) {
            if (tile instanceof City) {
                City city = (City) tile;
                if (city.isOwned() && city.owner == currentPlayerIndex) {
                    ownedCities.add(city);
                }
            }
        }

        if (ownedCities.isEmpty()) {
            log("소유한 도시가 없어 올림픽 효과를 사용할 수 없습니다.");
            endTurn();
            return;
        }

        log("소유한 도시 중 하나를 선택하여 통행료를 2배로 만들 수 있습니다.");
        log("(다음 통행료 지불 시 자동으로 해제됩니다)");

        // 소유한 도시 목록 표시
        for (int i = 0; i < ownedCities.size(); i++) {
            City city = ownedCities.get(i);
            log((i + 1) + ". " + city.name + " (레벨 " + city.level + ")");
        }

        // TODO: UI에서 도시 선택 기능 추가 필요
        // 임시로 첫 번째 도시에 적용
        if (!ownedCities.isEmpty()) {
            City selectedCity = ownedCities.get(0);
            ruleEngine.applyOlympicBoost(selectedCity);
            log("⚡ " + selectedCity.name + "에 올림픽 효과가 적용되었습니다! (통행료 2배)");
        }

        endTurn();
    }

    private void handleMagneticTollCollection(City landmark) {
        // 랜드마크에 끌려온 플레이어들에게 통행료 징수
        Player owner = players[currentPlayerIndex];
        int toll = ruleEngine.calculateToll(landmark, currentPlayerIndex);

        for (int i = 0; i < players.length; i++) {
            // 본인은 제외
            if (i == currentPlayerIndex) {
                continue;
            }

            Player player = players[i];

            // 랜드마크 위치에 있는 플레이어만 통행료 징수
            if (player.pos == landmark.id && !player.bankrupt) {
                log("💸 " + player.name + "이(가) " + landmark.name + "에 끌려와 통행료 " + String.format("%,d", toll) + "원을 지불합니다.");
                ruleEngine.payToll(player, owner, toll);

                // 자산 변동 표시
                frame.getOverlayPanel().showMoneyChange(i, -toll);
                frame.getOverlayPanel().showMoneyChange(currentPlayerIndex, toll);

                if (player.bankrupt) {
                    log(player.name + "이(가) 파산했습니다!");
                }
            }
        }

        // 보드 업데이트 (플레이어 위치 변경 반영)
        frame.getBoardPanel().updateBoard();
        frame.getOverlayPanel().updatePlayerInfo();
    }

    private void executePhaseDelete() {
        // 빈 도시(미소유 도시) 필터링
        List<City> emptyCities = new java.util.ArrayList<>();
        for (Tile tile : board.getAllTiles()) {
            if (tile instanceof City) {
                City city = (City) tile;
                if (!city.isOwned() && !city.isDeleted) {
                    emptyCities.add(city);
                }
            }
        }

        // 빈 도시가 없으면 발동 안 함
        if (emptyCities.isEmpty()) {
            log("⚠️ 페이즈 딜리트: 삭제할 수 있는 빈 도시가 없습니다.");
            return;
        }

        // 무작위로 1개 선택
        int randomIndex = (int)(Math.random() * emptyCities.size());
        City deletedCity = emptyCities.get(randomIndex);
        deletedCity.isDeleted = true;

        log("⚠️ 페이즈 딜리트 발동! " + deletedCity.name + "가 삭제됩니다!");

        // 삭제 다이얼로그 표시
        PhaseDeleteDialog deleteDialog = new PhaseDeleteDialog(frame, deletedCity.name);
        broadcastDialog(
            DialogSyncPayload.builder(DialogType.PHASE_DELETE)
                .put("cityName", deletedCity.name)
                .build()
        );
        deleteDialog.setVisible(true);

        // 보드 업데이트
        frame.getBoardPanel().repaint();
    }

    private void endTurn() {
        Player player = players[currentPlayerIndex];

        // 파산 시 더블 및 Extra Chance 무효화
        if (player.bankrupt) {
            log("💀 파산으로 인해 더블과 Extra Chance가 무효가 되었습니다.");
            consecutiveDoubles = 0;
            lastD1 = 0;
            lastD2 = 0;
            player.hasExtraChance = false;
            // 파산이면 더블 및 Extra Chance 체크 생략하고 바로 턴 종료
        } else {
            // Extra Chance 체크 (더블보다 우선)
            if (player.hasExtraChance) {
                log("🎲 Extra Chance! 추가 주사위를 굴릴 수 있습니다!");
                player.hasExtraChance = false; // Extra Chance 소진

                // 정규 주사위 상태로 전환
                state = GameState.WAITING_FOR_ROLL;
                setActionButtons(true, false, false, false, false, false);
                setTileSelectionEnabled(false);

                updateDisplay();
                return; // 턴 종료하지 않음
            }

            // 더블 체크: 행동 완료 후 더블이면 추가 주사위 기회
            if (checkAndHandleDouble()) {
                log("🎲 더블! 한 번 더 굴릴 수 있습니다!");

                // 더블 다이얼로그 표시
                DoubleDialog doubleDialog = new DoubleDialog(frame, lastD1, consecutiveDoubles);
                broadcastDialog(
                    DialogSyncPayload.builder(DialogType.DOUBLE_ROLL)
                        .putInt("diceValue", lastD1)
                        .putInt("consecutive", consecutiveDoubles)
                        .build()
                );
                doubleDialog.setVisible(true);

                // 더블 상태로 전환 (다시 주사위 굴리기 가능)
                state = GameState.WAITING_FOR_DOUBLE_ROLL;
                setActionButtons(true, false, false, false, false, false);
                setTileSelectionEnabled(false);

                updateDisplay();
                return; // 턴 종료하지 않음
            }
        }

        // 더블이 아니면 턴 종료 및 연속 더블 카운터 리셋
        consecutiveDoubles = 0;
        lastD1 = 0;
        lastD2 = 0;

        // 승리 조건 체크 (턴 종료 시)
        if (ruleEngine.checkVictory(players, currentPlayerIndex)) {
            endGame();
            return;
        }

        nextPlayer();
    }

    private void nextPlayer() {
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.length;
        } while (players[currentPlayerIndex].bankrupt && !isGameOver());

        if (currentPlayerIndex == 0) {
            turnCount++;
        }

        startTurn();
    }

    private boolean isGameOver() {
        // 기존 파산 체크도 유지
        int alive = 0;
        for (Player player : players) {
            if (!player.bankrupt) {
                alive++;
            }
        }
        if (alive <= 1) {
            return true;
        }

        // 승리 조건 체크
        for (int i = 0; i < players.length; i++) {
            if (ruleEngine.checkVictory(players, i)) {
                return true;
            }
        }

        return false;
    }

    private void endGame() {
        state = GameState.GAME_OVER;
        setActionButtons(false, false, false, false, false, false);
        setTileSelectionEnabled(false);
        frame.getActionPanel().clearPriceLabels();

        log("\n\n=== 게임 종료 ===");

        // 승리자 찾기
        Player winner = null;
        int winnerIndex = -1;
        for (int i = 0; i < players.length; i++) {
            if (!players[i].bankrupt) {
                winner = players[i];
                winnerIndex = i;
                break;
            }
        }

        // 승리 조건도 체크
        for (int i = 0; i < players.length; i++) {
            if (ruleEngine.checkVictory(players, i)) {
                winner = players[i];
                winnerIndex = i;
                break;
            }
        }

        if (winner != null && winnerIndex >= 0) {
            String victoryType = ruleEngine.getVictoryType(players, winnerIndex);
            log("🎉 승자: " + winner.name + " 🎉");
            log("승리 조건: " + victoryType);
            log("최종 자산: " + String.format("%,d", winner.cash) + "원");

            // 재시작 옵션이 포함된 다이얼로그
            broadcastDialog(
                DialogSyncPayload.builder(DialogType.GAME_OVER)
                    .put("winner", winner.name)
                    .put("victoryType", victoryType)
                    .putInt("cash", winner.cash)
                    .build()
            );
            int choice = JOptionPane.showOptionDialog(
                frame,
                winner.name + " 승리!\n승리 조건: " + victoryType + "\n최종 자산: " + String.format("%,d", winner.cash) + "원",
                "게임 종료",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                new Object[]{"새 게임", "종료"},
                "새 게임"
            );

            if (choice == 0) {
                // 새 게임 시작
                restartGame();
            } else {
                // 게임 종료
                System.exit(0);
            }
        }
    }

    private void restartGame() {
        // 현재 프레임 닫기
        frame.dispose();

        // 새 게임 시작
        SwingUtilities.invokeLater(() -> {
            new GameUI(players.length, 1000000, hostNetworkService);
        });
    }

    private void startMovementAnimation(Player player, int steps) {
        if (movementTimer != null && movementTimer.isRunning()) {
            movementTimer.stop();
        }

        movementPlayer = player;
        movementPlayerIndex = currentPlayerIndex;
        movementStepsRemaining = steps;
        movementCurrentTile = player.pos;
        movementSubStep = 0;
        movementStartPoint = null;
        movementEndPoint = null;

        state = GameState.ANIMATING_MOVEMENT;
        setActionButtons(false, false, false, false, false, false);
        setTileSelectionEnabled(false);
        frame.getActionPanel().clearPriceLabels();

        prepareNextMovementStep();

        movementTimer = new Timer(MOVEMENT_ANIMATION_INTERVAL, e -> updateMovementAnimation());
        movementTimer.start();
    }

    private void prepareNextMovementStep() {
        if (movementStepsRemaining <= 0) {
            finishMovementAnimation();
            return;
        }

        movementStartPoint = frame.getBoardPanel().getPlayerAnchorForTile(movementCurrentTile, movementPlayerIndex);

        // 다음 타일 계산 (삭제된 도시는 건너뜀)
        movementNextTile = (movementCurrentTile + 1) % board.getSize();
        Tile nextTile = board.getTile(movementNextTile);

        // 삭제된 도시면 추가로 건너뜀 (카운트하지 않음)
        while (nextTile instanceof City && ((City) nextTile).isDeleted) {
            movementNextTile = (movementNextTile + 1) % board.getSize();
            nextTile = board.getTile(movementNextTile);
        }

        movementEndPoint = frame.getBoardPanel().getPlayerAnchorForTile(movementNextTile, movementPlayerIndex);
        movementSubStep = 0;
    }

    private void updateMovementAnimation() {
        if (movementPlayer == null || movementStartPoint == null || movementEndPoint == null) {
            finishMovementAnimation();
            return;
        }

        movementSubStep++;
        double progress = Math.min(1.0, (double) movementSubStep / MOVEMENT_SUB_STEPS);
        double easedProgress = Math.sin((Math.PI / 2.0) * progress); // ease-out for hop motion
        double x = movementStartPoint.x + (movementEndPoint.x - movementStartPoint.x) * easedProgress;
        double y = movementStartPoint.y + (movementEndPoint.y - movementStartPoint.y) * easedProgress;
        double hopOffset = Math.sin(Math.PI * progress) * MOVEMENT_HOP_HEIGHT;
        y -= hopOffset;

        frame.getBoardPanel().setPlayerAnimationPosition(movementPlayerIndex, x, y);

        if (movementSubStep >= MOVEMENT_SUB_STEPS + MOVEMENT_HOLD_STEPS) {
            frame.getBoardPanel().clearPlayerAnimation(movementPlayerIndex);
            movementCurrentTile = movementNextTile;
            movementPlayer.pos = movementCurrentTile;
            movementStepsRemaining--;

            if (movementCurrentTile == 0) {
                ruleEngine.paySalary(movementPlayer);
                log("출발지를 통과하여 월급 " + String.format("%,d", ruleEngine.getSalary()) + "원을 받았습니다!");
                frame.getOverlayPanel().showMoneyChange(movementPlayerIndex, ruleEngine.getSalary());
            }

            frame.getBoardPanel().updateBoard();
            frame.getOverlayPanel().updatePlayerInfo();

            if (movementStepsRemaining <= 0) {
                finishMovementAnimation();
            } else {
                prepareNextMovementStep();
            }
        }
    }

    private void finishMovementAnimation() {
        if (movementTimer != null) {
            movementTimer.stop();
            movementTimer = null;
        }

        if (movementPlayer == null) {
            state = GameState.WAITING_FOR_ACTION;
            return;
        }

        frame.getBoardPanel().clearPlayerAnimation(movementPlayerIndex);
        frame.getBoardPanel().updateBoard();
        frame.getOverlayPanel().updatePlayerInfo();

        currentTile = board.getTile(movementPlayer.pos);
        log(movementPlayer.name + "이(가) " + currentTile.name + "에 도착했습니다.");

        movementStartPoint = null;
        movementEndPoint = null;
        movementPlayer = null;

        state = GameState.WAITING_FOR_ACTION;
        handleTileLanding();
    }

    private void log(String message) {
        frame.getControlPanel().addLog(message);
        broadcastLog(message);
    }

    private void showErrorDialog(String title, String message) {
        ErrorDialog errorDialog = new ErrorDialog(frame, title, message);
        broadcastDialog(
            DialogSyncPayload.builder(DialogType.ERROR)
                .put("title", title)
                .put("message", message)
                .build()
        );
        errorDialog.setVisible(true);
    }

    private void broadcastLog(String message) {
        if (hostNetworkService != null) {
            hostNetworkService.broadcast(new NetworkMessage(MessageType.LOG_ENTRY, message));
        }
    }

    private void broadcastDialog(DialogSyncPayload payload) {
        if (hostNetworkService == null || payload == null) {
            return;
        }
        try {
            String serialized = DialogSyncCodec.encode(payload);
            hostNetworkService.broadcast(new NetworkMessage(MessageType.DIALOG_SYNC, serialized));
        } catch (Exception ex) {
            System.err.println("[Host] 다이얼로그 동기화 실패: " + ex.getMessage());
        }
    }

    private void broadcastSnapshot() {
        if (hostNetworkService == null) {
            return;
        }
        try {
            GameSnapshot snapshot = createSnapshot();
            String payload = GameSnapshotSerializer.serialize(snapshot);
            hostNetworkService.broadcast(new NetworkMessage(MessageType.STATE_SNAPSHOT, payload));
        } catch (Exception ex) {
            System.err.println("[Host] 스냅샷 전송 실패: " + ex.getMessage());
        }
    }

    private GameSnapshot createSnapshot() {
        GameSnapshot snapshot = new GameSnapshot();
        snapshot.turnNumber = turnCount;
        snapshot.currentPlayerIndex = currentPlayerIndex;
        snapshot.diceRollSequence = diceRollSequence;
        snapshot.dice1 = lastD1;
        snapshot.dice2 = lastD2;
        snapshot.oddModeSelected = diceMode == DiceMode.ODD;
        snapshot.evenModeSelected = diceMode == DiceMode.EVEN;
        snapshot.tileSelectionEnabled = tileSelectionEnabled;

        snapshot.buttons.roll = rollButtonActive;
        snapshot.buttons.purchase = purchaseButtonActive;
        snapshot.buttons.upgrade = upgradeButtonActive;
        snapshot.buttons.takeover = takeoverButtonActive;
        snapshot.buttons.skip = skipButtonActive;
        snapshot.buttons.escape = escapeButtonActive;

        for (Player player : players) {
            GameSnapshot.PlayerState ps = new GameSnapshot.PlayerState();
            ps.name = player.name;
            ps.cash = player.cash;
            ps.position = player.pos;
            ps.jailTurns = player.jailTurns;
            ps.bankrupt = player.bankrupt;
            ps.hasRailroadTicket = player.hasRailroadTicket;
            ps.hasExtraChance = player.hasExtraChance;
            snapshot.players.add(ps);
        }

        for (int i = 0; i < board.getSize(); i++) {
            Tile tile = board.getTile(i);
            if (tile instanceof City) {
                City city = (City) tile;
                GameSnapshot.CityState cs = new GameSnapshot.CityState();
                cs.tileId = city.id;
                cs.owner = city.owner;
                cs.level = city.level;
                cs.hasOlympicBoost = city.hasOlympicBoost;
                cs.deleted = city.isDeleted;
                snapshot.cities.add(cs);
            } else if (tile instanceof TouristSpot) {
                TouristSpot spot = (TouristSpot) tile;
                GameSnapshot.TouristSpotState ts = new GameSnapshot.TouristSpotState();
                ts.tileId = spot.id;
                ts.owner = spot.owner;
                ts.locked = spot.locked;
                ts.lockedBy = spot.lockedBy;
                snapshot.touristSpots.add(ts);
            }
        }
        return snapshot;
    }

    private void updateDisplay() {
        frame.updateDisplay(turnCount);
    }

    /**
     * 홀수/짝수 버튼 상태 업데이트
     */
    private void updateOddEvenButtons() {
        frame.getOverlayPanel().getOddButton().putClientProperty("selected", diceMode == DiceMode.ODD);
        frame.getOverlayPanel().getEvenButton().putClientProperty("selected", diceMode == DiceMode.EVEN);
        frame.getOverlayPanel().getOddButton().repaint();
        frame.getOverlayPanel().getEvenButton().repaint();
    }

    /**
     * 레벨 번호에서 건물 이름으로 변환
     */
    private String getLevelName(int level) {
        switch (level) {
            case 1: return "집";
            case 2: return "아파트";
            case 3: return "건물";
            case 4: return "랜드마크";
            default: return "";
        }
    }

    private void handleClientMessage(String clientId, NetworkMessage message) {
        MessageType type = message.getType();
        switch (type) {
            case PLAYER_ACTION:
                handleRemoteAction(clientId, message);
                break;
            case LOG_ENTRY:
                String payload = message.getPayload();
                if (payload != null && !payload.isEmpty()) {
                    log("[원격] " + payload);
                }
                break;
            case SLOT_REQUEST:
                handleSlotRequestMessage(clientId, message.getPayload());
                break;
            case READY_STATUS:
                handleReadyStatusMessage(clientId, message.getPayload());
                break;
            default:
                break;
        }
    }

    private void handleRemoteAction(String clientId, NetworkMessage message) {
        if (!isClientTurn(clientId)) {
            System.out.println("[Host] " + clientId + " 원격 입력 무시: 현재 차례가 아님");
            return;
        }
        try {
            PlayerInputEvent remoteEvent = RemoteActionCodec.decode(message);
            SwingUtilities.invokeLater(() -> handlePlayerInput(remoteEvent));
        } catch (IllegalArgumentException ex) {
            System.err.println("[Host] 잘못된 원격 입력(" + clientId + "): " + message.getPayload());
        }
    }

    private boolean isClientTurn(String clientId) {
        if (lobbyState == null) {
            return false;
        }
        Integer slotIndex = lobbyState.getSlotIndex(clientId);
        if (slotIndex == null) {
            System.out.println("[Host] 슬롯 미할당 클라이언트 입력 무시: " + clientId);
            return false;
        }
        return slotIndex == currentPlayerIndex;
    }

    private void handleSlotRequestMessage(String clientId, String payload) {
        if (lobbyState == null) {
            return;
        }
        try {
            SlotRequestPayload request = SlotRequestPayload.decode(payload);
            SwingUtilities.invokeLater(() -> processSlotRequest(clientId, request));
        } catch (IllegalArgumentException ex) {
            System.err.println("[Host] 잘못된 슬롯 요청: " + ex.getMessage());
            notifySlotAssignment(clientId, -1, "", SlotAssignmentPayload.Status.DENIED, "요청 형식 오류");
        }
    }

    private void processSlotRequest(String clientId, SlotRequestPayload request) {
        if (hostNetworkService == null || lobbyState == null) {
            return;
        }
        if (request.getSlotIndex() < 0) {
            Integer prevSlot = lobbyState.getSlotIndex(clientId);
            lobbyState.releaseSlot(clientId);
            if (prevSlot != null) {
                players[prevSlot].name = lobbyState.getEffectivePlayerName(prevSlot);
                frame.getOverlayPanel().updatePlayerInfo();
            }
            notifySlotAssignment(clientId, -1, "", SlotAssignmentPayload.Status.RELEASED, "슬롯을 비웠습니다.");
            pushLobbyState();
            return;
        }

        String sanitized = sanitizePlayerName(request.getPlayerName());
        Integer previousSlot = lobbyState.getSlotIndex(clientId);
        boolean assigned = lobbyState.assignSlot(clientId, request.getSlotIndex(), sanitized);
        if (!assigned) {
            notifySlotAssignment(
                clientId,
                request.getSlotIndex(),
                "",
                SlotAssignmentPayload.Status.DENIED,
                "이미 점유된 슬롯입니다."
            );
            pushLobbyState();
            return;
        }

        if (previousSlot != null && previousSlot != request.getSlotIndex()) {
            players[previousSlot].name = lobbyState.getEffectivePlayerName(previousSlot);
        }
        players[request.getSlotIndex()].name = lobbyState.getEffectivePlayerName(request.getSlotIndex());
        frame.getOverlayPanel().updatePlayerInfo();
        notifySlotAssignment(
            clientId,
            request.getSlotIndex(),
            players[request.getSlotIndex()].name,
            SlotAssignmentPayload.Status.ASSIGNED,
            "슬롯 #" + (request.getSlotIndex() + 1) + " 배정 완료"
        );
        pushLobbyState();
    }

    private void releaseSlotFromHost(int slotIndex) {
        if (lobbyState == null) {
            return;
        }
        if (slotIndex < 0 || slotIndex >= players.length) {
            return;
        }
        String clientId = lobbyState.getClientIdForSlot(slotIndex);
        boolean released = lobbyState.releaseSlot(slotIndex);
        if (!released) {
            return;
        }
        players[slotIndex].name = lobbyState.getEffectivePlayerName(slotIndex);
        frame.getOverlayPanel().updatePlayerInfo();
        if (clientId != null) {
            notifySlotAssignment(
                clientId,
                slotIndex,
                "",
                SlotAssignmentPayload.Status.RELEASED,
                "호스트가 슬롯을 해제했습니다."
            );
        }
        log("[네트워크] 슬롯 #" + (slotIndex + 1) + " 을(를) 해제했습니다.");
        pushLobbyState();
    }

    private void handleReadyStatusMessage(String clientId, String payload) {
        if (lobbyState == null) {
            return;
        }
        try {
            ReadyStatusPayload readyPayload = ReadyStatusPayload.decode(payload);
            SwingUtilities.invokeLater(() -> {
                if (lobbyState.updateReady(clientId, readyPayload.isReady())) {
                    pushLobbyState();
                }
            });
        } catch (IllegalArgumentException ex) {
            System.err.println("[Host] 준비 상태 파싱 실패: " + ex.getMessage());
        }
    }
}
