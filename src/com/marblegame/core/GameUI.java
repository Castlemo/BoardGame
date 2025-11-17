package com.marblegame.core;

import com.marblegame.model.*;
import com.marblegame.network.protocol.MessageType;
import com.marblegame.network.sync.GameStateMapper;
import com.marblegame.network.sync.GameStateSnapshot;
import com.marblegame.ui.*;
import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UI 버전 게임 컨트롤러
 */
public class GameUI {
    private final Board board;
    private final RuleEngine ruleEngine;
    private final Player[] players;
    private final Dice dice;
    private final GameFrame frame;

    private int currentPlayerIndex = 0;
    private int turnCount = 1;
    private GameState state = GameState.WAITING_FOR_ROLL;

    public interface GameStateSyncListener {
        void onStateChanged(GameStateSnapshot snapshot);
    }

    public static class PlayerSetup {
        private final int index;
        private final String playerId;
        private final String name;

        public PlayerSetup(int index, String playerId, String name) {
            this.index = index;
            this.playerId = playerId;
            this.name = name;
        }

        public int getIndex() {
            return index;
        }

        public String getPlayerId() {
            return playerId;
        }

        public String getName() {
            return name;
        }
    }

    public static class NetworkSettings {
        private final boolean host;
        private final String localPlayerId;
        private final List<PlayerSetup> playerSetups;
        private final GameStateSyncListener syncListener;
        private final NetworkActionSender actionSender;

        public NetworkSettings(boolean host, String localPlayerId, List<PlayerSetup> playerSetups,
                               GameStateSyncListener syncListener, NetworkActionSender actionSender) {
            this.host = host;
            this.localPlayerId = localPlayerId;
            this.playerSetups = playerSetups != null
                ? new ArrayList<>(playerSetups)
                : Collections.emptyList();
            this.syncListener = syncListener;
            this.actionSender = actionSender;
        }

        public boolean isHost() {
            return host;
        }

        public String getLocalPlayerId() {
            return localPlayerId;
        }

        public List<PlayerSetup> getPlayerSetups() {
            return Collections.unmodifiableList(playerSetups);
        }

        public GameStateSyncListener getSyncListener() {
            return syncListener;
        }

        public NetworkActionSender getActionSender() {
            return actionSender;
        }
    }

    public interface NetworkActionSender {
        void sendAction(MessageType type, Map<String, Object> payload);
    }

    private enum GameState {
        WAITING_FOR_ROLL,
        WAITING_FOR_ACTION,
        WAITING_FOR_JAIL_CHOICE,
        WAITING_FOR_RAILROAD_SELECTION,
        WAITING_FOR_LANDMARK_SELECTION,
        WAITING_FOR_DOUBLE_ROLL,  // 더블 발생 후 추가 주사위 대기
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
    private final Map<String, Integer> playerIndexById = new HashMap<>();
    private final boolean networkMode;
    private final boolean isHost;
    private final String localPlayerId;
    private final GameStateSyncListener gameStateSyncListener;
    private final NetworkActionSender networkActionSender;
    private final List<String> currentAvailableActions = new ArrayList<>();
    private int localPlayerIndex = -1;
    private boolean awaitingNetworkResolution = false;
    private int lastEventSequence = 0;
    private GameStateSnapshot.EventState lastEventState;
    private int lastHandledEventId = 0;
    private final boolean[] bankruptcyAnnounced;
    private static final String ACTION_ROLL = "ROLL";
    private static final String ACTION_PURCHASE = "PURCHASE";
    private static final String ACTION_UPGRADE = "UPGRADE";
    private static final String ACTION_TAKEOVER = "TAKEOVER";
    private static final String ACTION_SKIP = "SKIP";
    private static final String ACTION_ESCAPE = "ESCAPE";

    public GameUI(int numPlayers, int initialCash) {
        this(numPlayers, initialCash, null);
    }

    public GameUI(int numPlayers, int initialCash, NetworkSettings networkSettings) {
        this.board = new Board();
        this.ruleEngine = new RuleEngine(board);
        this.networkMode = networkSettings != null;
        this.isHost = networkMode && networkSettings.isHost();
        this.localPlayerId = networkMode ? networkSettings.getLocalPlayerId() : null;
        this.gameStateSyncListener = networkMode ? networkSettings.getSyncListener() : null;
        this.networkActionSender = networkMode ? networkSettings.getActionSender() : null;

        List<PlayerSetup> setups = networkMode ? networkSettings.getPlayerSetups() : Collections.emptyList();
        int resolvedPlayerCount = networkMode && !setups.isEmpty() ? setups.size() : numPlayers;
        if (resolvedPlayerCount <= 0) {
            resolvedPlayerCount = 2;
        }

        this.players = new Player[resolvedPlayerCount];
        this.bankruptcyAnnounced = new boolean[resolvedPlayerCount];
        this.dice = new Dice();

        if (networkMode && !setups.isEmpty()) {
            for (PlayerSetup setup : setups) {
                int index = clampPlayerIndex(setup.getIndex(), resolvedPlayerCount);
                Player player = new Player(setup.getPlayerId(), setup.getName(), initialCash);
                players[index] = player;
                if (setup.getPlayerId() != null) {
                    playerIndexById.put(setup.getPlayerId(), index);
                }
            }

            for (int i = 0; i < players.length; i++) {
                if (players[i] == null) {
                    players[i] = new Player("Player" + (char)('A' + i), initialCash);
                }
            }

            if (localPlayerId != null && playerIndexById.containsKey(localPlayerId)) {
                localPlayerIndex = playerIndexById.get(localPlayerId);
            }
        } else {
            for (int i = 0; i < resolvedPlayerCount; i++) {
                players[i] = new Player("Player" + (char)('A' + i), initialCash);
            }
        }

        // UI 초기화
        frame = new GameFrame(board, java.util.Arrays.asList(players), networkMode);
        setupEventHandlers();

        frame.setVisible(true);
        frame.getControlPanel().addLog("=== 모두의 마블 게임 시작 ===");
        frame.getControlPanel().addLog("플레이어 수: " + players.length);
        frame.getControlPanel().addLog("초기 자금: " + String.format("%,d", initialCash) + "원\n");

        if (!networkMode || isHost) {
            startTurn();
        } else {
            enterPassiveNetworkMode();
        }
    }

    private void setupEventHandlers() {
        // 주사위 굴리기 - press-and-hold 이벤트
        setupDiceButtonPressAndHold();

        // 매입
        frame.getActionPanel().setPurchaseListener(e -> purchaseCity());

        // 업그레이드
        frame.getActionPanel().setUpgradeListener(e -> upgradeCity());

        // 인수
        frame.getActionPanel().setTakeoverListener(e -> {
            if (currentTile instanceof City) {
                takeoverCity();
            } else if (currentTile instanceof TouristSpot) {
                takeoverTouristSpot();
            }
        });

        // 패스
        frame.getActionPanel().setSkipListener(e -> skip());

        // 보석금 탈출
        frame.getActionPanel().setEscapeListener(e -> escapeWithBail());

        // 홀수/짝수 선택
        frame.getOverlayPanel().getOddButton().addActionListener(e -> {
            if (diceMode == DiceMode.ODD) {
                // 이미 선택된 경우 해제
                diceMode = DiceMode.NORMAL;
                log("일반 주사위 모드");
            } else {
                diceMode = DiceMode.ODD;
                log("🔢 홀수 주사위 모드 선택 (1, 3, 5만 나옴)");
            }
            updateOddEvenButtons();
        });

        frame.getOverlayPanel().getEvenButton().addActionListener(e -> {
            if (diceMode == DiceMode.EVEN) {
                // 이미 선택된 경우 해제
                diceMode = DiceMode.NORMAL;
                log("일반 주사위 모드");
            } else {
                diceMode = DiceMode.EVEN;
                log("🔢 짝수 주사위 모드 선택 (2, 4, 6만 나옴)");
            }
            updateOddEvenButtons();
        });

        // 보드 타일 클릭 (전국철도 선택용)
        frame.getBoardPanel().setTileClickListener(tileIndex -> onTileSelected(tileIndex));

        // 네트워크 채팅 콜백 설정
        if (networkMode) {
            frame.getOverlayPanel().setNetworkChatCallback((type, content) -> {
                sendChatMessage(type, content);
            });
        }
    }

    /**
     * 채팅 메시지 전송 (네트워크)
     */
    private void sendChatMessage(String type, String content) {
        if (!networkMode || networkActionSender == null) {
            return;
        }

        // 로컬 플레이어의 인덱스와 이름을 사용 (자신의 턴이 아니어도 채팅 가능)
        int senderIndex = localPlayerIndex >= 0 ? localPlayerIndex : currentPlayerIndex;
        String senderName = players[senderIndex].name;

        Map<String, Object> payload = new HashMap<>();
        payload.put("playerIndex", senderIndex);
        payload.put("playerName", senderName);
        payload.put("content", content);

        if ("emoji".equals(type)) {
            networkActionSender.sendAction(MessageType.CHAT_EMOJI, payload);
        } else {
            networkActionSender.sendAction(MessageType.CHAT_MESSAGE, payload);
        }
    }

    private void enterPassiveNetworkMode() {
        setActionButtons(false, false, false, false, false, false);
        frame.getBoardPanel().setTileClickEnabled(false);
        log("네트워크 게임 동기화를 기다리는 중입니다...");
        updateDisplay();
    }

    private boolean isNetworkClient() {
        return networkMode && !isHost;
    }

    private boolean isLocalPlayersTurn() {
        return isNetworkClient() && localPlayerIndex >= 0 && localPlayerIndex == currentPlayerIndex;
    }

    private boolean canSendNetworkAction() {
        return isLocalPlayersTurn() && !awaitingNetworkResolution && networkActionSender != null;
    }

    private boolean isCurrentNetworkPlayer(String playerId) {
        if (playerId == null) {
            return false;
        }
        Integer index = playerIndexById.get(playerId);
        return index != null && index == currentPlayerIndex;
    }

    private boolean shouldShowLocalDialog() {
        if (!networkMode) {
            return true;
        }
        if (isHost) {
            return localPlayerIndex >= 0 && localPlayerIndex == currentPlayerIndex;
        }
        return false;
    }

    private boolean tileMatches(Integer tileId) {
        if (tileId == null || currentTile == null) {
            return true;
        }
        return currentTile.id == tileId;
    }

    private DiceMode parseDiceMode(String value) {
        if (value == null) {
            return DiceMode.NORMAL;
        }
        try {
            return DiceMode.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return DiceMode.NORMAL;
        }
    }

    private int rollBiasedResultForSection(int section) {
        return DiceGauge.rollBiasedForSection(section);
    }

    /**
     * 주사위 버튼에 press-and-hold 이벤트 설정
     */
    private void setupDiceButtonPressAndHold() {
        JButton diceButton = frame.getActionPanel().getRollDiceButton();

        diceButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (diceButton.isEnabled()) {
                    // 게이지 시작
                    frame.getActionPanel().getDiceGauge().start();
                    frame.getActionPanel().startGaugeAnimation();
                    log("🎯 게이지 타이밍을 잡으세요!");
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (diceButton.isEnabled() && frame.getActionPanel().getDiceGauge().isRunning()) {
                    if (isNetworkClient()) {
                        handleNetworkDiceRelease();
                    } else {
                        // 게이지 정지 및 주사위 굴리기
                        rollDiceWithGauge();
                    }
                }
            }
        });
    }

    private void startTurn() {
        if (isGameOver()) {
            endGame();
            return;
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
            frame.getBoardPanel().setTileClickEnabled(false);
            log("무인도에 갇혀있습니다. (남은 턴: " + player.jailTurns + ")");
            log("💰 보석금 200,000원으로 즉시 탈출하거나, ⏭ 패스하여 대기하세요.");
        } else if (player.hasRailroadTicket) {
            state = GameState.WAITING_FOR_RAILROAD_SELECTION;
            setActionButtons(false, false, false, false, false, false);
            frame.getBoardPanel().setTileClickEnabled(true);
            log("🚆 전국철도/세계여행 티켓이 있습니다!");
            log("보드에서 원하는 칸을 클릭하세요.");

            // 도시 선택 안내 다이얼로그 표시
            CitySelectionDialog selectionDialog = new CitySelectionDialog(frame);
            selectionDialog.setVisible(true);
        } else {
            state = GameState.WAITING_FOR_ROLL;
            setActionButtons(true, false, false, false, false, false);
            frame.getBoardPanel().setTileClickEnabled(false);
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
            int result = frame.getActionPanel().getDiceGauge().stop();
            frame.getActionPanel().stopGaugeAnimation();
            int section = frame.getActionPanel().getDiceGauge().getCurrentSection();
            resolveDiceRoll(result, section, diceMode, true);
        }
    }

    private void resolveDiceRoll(int result, int section, DiceMode overrideMode, boolean logSection) {
        DiceMode activeMode = overrideMode != null ? overrideMode : diceMode;
        String sectionName = getSectionName(section);
        if (logSection) {
            log("🎯 구간: " + sectionName);
        }

        int originalResult = result;

        if (activeMode == DiceMode.ODD && result % 2 == 0) {
            if (result > 2) result -= 1;
            else result += 1;
        } else if (activeMode == DiceMode.EVEN && result % 2 == 1) {
            if (result < 12) result += 1;
            else result -= 1;
        }

        int[] dicePair = getRandomDicePairForSum(result);
        int tempD1 = dicePair[0];
        int tempD2 = dicePair[1];
        boolean isDouble = (tempD1 == tempD2);

        boolean showSuppressionDialog = false;
        if (isDouble) {
            double suppressProbability = getDoubleSuppressProbability(consecutiveDoubles);
            if (Math.random() < suppressProbability) {
                if (result == 2 || result == 12) {
                    if (result == 2) {
                        if (activeMode == DiceMode.EVEN) {
                            tempD1 = 1;
                            tempD2 = 3;
                        } else {
                            tempD1 = 1;
                            tempD2 = 2;
                        }
                    } else {
                        if (activeMode == DiceMode.EVEN) {
                            tempD1 = 6;
                            tempD2 = 4;
                        } else {
                            tempD1 = 6;
                            tempD2 = 5;
                        }
                    }
                    isDouble = false;

                    if (consecutiveDoubles >= 2) {
                        showSuppressionDialog = true;
                    }

                    logDoubleSuppression(originalResult, tempD1 + tempD2);
                } else {
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

        final int finalD1 = tempD1;
        final int finalD2 = tempD2;
        final int finalResult = finalD1 + finalD2;
        final boolean finalIsDouble = isDouble;
        final boolean finalShowSuppressionDialog = showSuppressionDialog;
        final int finalConsecutiveDoubles = consecutiveDoubles;

        lastD1 = finalD1;
        lastD2 = finalD2;

        if (networkMode && isHost) {
            pushNetworkEvent(MessageType.ROLL_DICE, createDiceEventData(finalResult, finalD1, finalD2,
                finalIsDouble, finalShowSuppressionDialog, finalConsecutiveDoubles));
        }

        frame.getActionPanel().getDiceAnimationPanel().startAnimation(finalD1, finalD2, () -> {
            if (finalIsDouble) {
                log("🎲 주사위: [" + finalD1 + ", " + finalD2 + "] = " + finalResult + " - 더블!");
            } else {
                log("주사위: [" + finalD1 + ", " + finalD2 + "] = " + finalResult);
            }

            if (finalShowSuppressionDialog && shouldShowLocalDialog()) {
                DoubleSuppressedDialog suppressedDialog = new DoubleSuppressedDialog(
                    frame, finalD1, finalConsecutiveDoubles);
                suppressedDialog.setVisible(true);
            }

            movePlayer(finalResult);
        });
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
                // 무인도 다이얼로그 표시 (자신의 턴일 때만)
                if (shouldShowLocalDialog()) {
                    IslandDialog islandDialog = new IslandDialog(frame, player.jailTurns);
                    islandDialog.setVisible(true);
                }

                log("무인도에 도착했습니다!");
                clearDoubleState("🎲 더블이었지만 무인도에 갇혀 무효가 되었습니다.");
                log("무인도에 " + player.jailTurns + "턴 동안 갇힙니다.");
                notifyIslandEvent(player.name, player.jailTurns);
                endTurn();
                break;

            case CHANCE:
                int chanceReward = ruleEngine.getChanceReward();
                ruleEngine.processChance(player);

                // 자산 변동 표시
                frame.getOverlayPanel().showMoneyChange(currentPlayerIndex, chanceReward);

                // 찬스 다이얼로그 표시 (자신의 턴일 때만)
                if (shouldShowLocalDialog()) {
                    ChanceDialog chanceDialog = new ChanceDialog(frame, chanceReward);
                    chanceDialog.setVisible(true);
                }

                log("찬스 카드! " + String.format("%,d", chanceReward) + "원을 받았습니다!");
                notifyChanceEvent(player.name, chanceReward);
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
                // 세계여행 다이얼로그 표시 (자신의 턴일 때만)
                if (shouldShowLocalDialog()) {
                    WorldTourDialog worldTourDialog = new WorldTourDialog(frame);
                    worldTourDialog.setVisible(true);
                }

                log("세계여행에 도착했습니다!");
                clearDoubleState("🎲 더블이었지만 세계여행 칸에서 무효가 되었습니다.");
                log("다음 턴에 원하는 칸을 선택할 수 있습니다!");
                player.hasRailroadTicket = true; // 전국철도와 동일한 효과
                notifyWorldTourEvent(player.name);
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
                magneticDialog.setVisible(true);

                if (pulledCount > 0) {
                    log("🧲 듀얼 마그네틱 코어 발동! " + pulledCount + "명의 플레이어를 끌어당깁니다!");

                    // 끌려온 플레이어들에게 통행료 징수
                    handleMagneticTollCollection(city);
                } else {
                    log("🧲 듀얼 마그네틱 코어 발동! 범위 내 플레이어가 없습니다.");
                }

                notifyMagneticEvent(city.name, pulledCount);

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

            // 통행료 지불 확인 다이얼로그 (자신의 턴일 때만)
            int playerCashBefore = player.cash;
            if (shouldShowLocalDialog()) {
                TollPaymentDialog tollDialog = new TollPaymentDialog(
                    frame,
                    city.name,
                    owner.name,
                    city.level,
                    toll,
                    city.hasOlympicBoost,
                    playerCashBefore
                );
                tollDialog.setVisible(true);
            }

            log("💸 통행료 " + String.format("%,d", toll) + "원을 지불합니다.");
            ruleEngine.payToll(player, owner, toll);

            // 자산 변동 표시
            frame.getOverlayPanel().showMoneyChange(currentPlayerIndex, -toll);
            frame.getOverlayPanel().showMoneyChange(city.owner, toll);

            notifyTollEvent(player.name, owner.name, city.name, city.level, toll, city.hasOlympicBoost, false);

            // 올림픽 효과 해제 (한 번 통행료 지불 후)
            if (city.hasOlympicBoost) {
                ruleEngine.removeOlympicBoost(city);
                log("올림픽 효과가 해제되었습니다.");
            }

            if (player.bankrupt) {
                announceBankruptcy(currentPlayerIndex);
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

            // 네트워크 클라이언트는 별도 처리
            if (isNetworkClient()) {
                handleClientUnownedTouristSpot(touristSpot, player);
                return;
            }

            // 호스트가 아닌 다른 플레이어의 턴이면 클라이언트에게 알림
            if (!shouldShowLocalDialog()) {
                log("클라이언트의 관광지 매입 결정을 기다립니다...");
                notifyTouristLandingEvent(touristSpot);
                return;
            }

            // 매입 다이얼로그 표시
            TouristSpotPurchaseDialog purchaseDialog = new TouristSpotPurchaseDialog(
                frame,
                touristSpot.name,
                touristSpot.price,
                player.cash
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

            // 네트워크 클라이언트는 별도 처리
            if (isNetworkClient()) {
                handleClientOwnedTouristSpot(touristSpot, player);
                return;
            }

            // 호스트가 아닌 다른 플레이어의 턴이면 클라이언트에게 알림
            if (!shouldShowLocalDialog()) {
                log("클라이언트의 관광지 선택을 기다립니다...");
                notifyTouristLandingEvent(touristSpot);
                return;
            }

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

            // 통행료 지불 확인 다이얼로그 (자신의 턴일 때만, 관광지는 레벨 1로 표시)
            int playerCashBefore = player.cash;
            if (shouldShowLocalDialog()) {
                TollPaymentDialog tollDialog = new TollPaymentDialog(
                    frame,
                    touristSpot.name,
                    owner.name,
                    1,  // 관광지는 레벨 개념 없음
                    toll,
                    false,  // 관광지는 올림픽 효과 없음
                    playerCashBefore
                );
                tollDialog.setVisible(true);
            }

            log("💸 통행료 " + String.format("%,d", toll) + "원을 지불합니다.");
            ruleEngine.payToll(player, owner, toll);

            // 자산 변동 표시
            frame.getOverlayPanel().showMoneyChange(currentPlayerIndex, -toll);
            frame.getOverlayPanel().showMoneyChange(touristSpot.owner, toll);

            notifyTollEvent(player.name, owner.name, touristSpot.name, 1, toll, false, true);

            // 잠금된 관광지는 통행료 지불 후 잠금 해제
            if (touristSpot.isLocked()) {
                ruleEngine.unlockTouristSpot(touristSpot);
                log("🔓 관광지 잠금이 해제되었습니다.");

                if (player.bankrupt) {
                    announceBankruptcy(currentPlayerIndex);
                }
                endTurn();
            } else {
                // 잠금되지 않은 경우 인수 선택지 제공
                if (player.bankrupt) {
                    announceBankruptcy(currentPlayerIndex);
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
        choiceDialog.setVisible(true);

        TouristSpotChoiceDialog.Choice choice = choiceDialog.getSelectedChoice();
        if (applyTouristSpotChoice(touristSpot, player, choice)) {
            endTurn();
        } else {
            log("선택이 취소되어 기본 동작 없이 턴을 종료합니다.");
            endTurn();
        }
    }

    private boolean finalizeCityPurchase(Player player, City city, int selectedLevel) {
        if (ruleEngine.purchaseCityWithLevel(player, city, selectedLevel, currentPlayerIndex)) {
            int totalCost = ruleEngine.calculateLevelCost(city.price, selectedLevel);
            String levelName = getLevelName(selectedLevel);
            String emoji = city.getBuildingEmoji();

            frame.getOverlayPanel().showMoneyChange(currentPlayerIndex, -totalCost);

            log(player.name + "이(가) " + city.name + "을(를) " +
                String.format("%,d", totalCost) + "원에 매입했습니다!");
            log(emoji + " " + levelName + "이(가) 건설되었습니다! (레벨 " + selectedLevel + ")");
            return true;
        }

        log("자금이 부족하여 매입할 수 없습니다.");
        return false;
    }

    private boolean finalizeTouristPurchase(Player player, TouristSpot touristSpot) {
        if (ruleEngine.purchaseTouristSpot(player, touristSpot, currentPlayerIndex)) {
            frame.getOverlayPanel().showMoneyChange(currentPlayerIndex, -touristSpot.price);

            log(player.name + "이(가) " + touristSpot.name + "을(를) " +
                String.format("%,d", touristSpot.price) + "원에 매입했습니다!");
            return true;
        }

        log("자금이 부족하여 매입할 수 없습니다.");
        return false;
    }

    private boolean finalizeCityTakeover(City city) {
        if (city == null || city.owner == null) {
            log("인수할 도시 정보가 올바르지 않습니다.");
            return false;
        }

        int sellerIndex = city.owner;
        if (sellerIndex < 0 || sellerIndex >= players.length) {
            log("인수할 도시 소유자 정보가 잘못되었습니다.");
            return false;
        }

        Player buyer = players[currentPlayerIndex];
        Player seller = players[sellerIndex];
        int takeoverCost = city.getTakeoverPrice();

        if (ruleEngine.takeoverCity(buyer, seller, city, currentPlayerIndex)) {
            frame.getOverlayPanel().showMoneyChange(currentPlayerIndex, -takeoverCost);
            frame.getOverlayPanel().showMoneyChange(sellerIndex, takeoverCost);
            log(buyer.name + "이(가) " + seller.name + "으로부터 " + city.name + "을(를) " +
                String.format("%,d", takeoverCost) + "원에 인수했습니다!");
            log(seller.name + "이(가) " + String.format("%,d", takeoverCost) + "원을 받았습니다.");
            return true;
        } else if (city.isLandmark()) {
            log("🏛️ 랜드마크는 인수할 수 없습니다.");
        } else {
            log("자금이 부족하여 인수할 수 없습니다.");
        }
        return false;
    }

    private boolean finalizeTouristTakeover(TouristSpot spot) {
        if (spot == null || spot.owner == null) {
            log("인수할 관광지 정보가 올바르지 않습니다.");
            return false;
        }

        int sellerIndex = spot.owner;
        if (sellerIndex < 0 || sellerIndex >= players.length) {
            log("인수할 관광지 소유자 정보가 잘못되었습니다.");
            return false;
        }

        Player buyer = players[currentPlayerIndex];
        Player seller = players[sellerIndex];
        int takeoverCost = spot.price;

        if (ruleEngine.takeoverTouristSpot(buyer, seller, spot, currentPlayerIndex)) {
            frame.getOverlayPanel().showMoneyChange(currentPlayerIndex, -takeoverCost);
            frame.getOverlayPanel().showMoneyChange(sellerIndex, takeoverCost);
            log(buyer.name + "이(가) " + seller.name + "으로부터 " + spot.name + "을(를) " +
                String.format("%,d", takeoverCost) + "원에 인수했습니다!");
            log(seller.name + "이(가) " + String.format("%,d", takeoverCost) + "원을 받았습니다.");
            return true;
        } else if (spot.isLocked()) {
            log("🔒 잠금된 관광지는 인수할 수 없습니다.");
        } else {
            log("자금이 부족하여 인수할 수 없습니다.");
        }
        return false;
    }

    private boolean applyTouristSpotChoice(TouristSpot touristSpot, Player player,
                                           TouristSpotChoiceDialog.Choice choice) {
        if (choice == null) {
            return false;
        }

        switch (choice) {
            case LOCK:
                ruleEngine.lockTouristSpot(touristSpot, currentPlayerIndex);
                log("🔒 " + touristSpot.name + "을(를) 잠금 설정했습니다! (다음 내 턴까지 인수 불가)");
                notifyTouristChoiceEvent(touristSpot.name, choice);
                return true;

            case EXTRA_ROLL:
                player.hasExtraChance = true;
                log("🎲 추가 주사위 기회를 획득했습니다!");
                notifyTouristChoiceEvent(touristSpot.name, choice);
                return true;

            default:
                return false;
        }
    }

    private TouristSpotChoiceDialog.Choice parseTouristChoice(String value) {
        if (value == null) {
            return TouristSpotChoiceDialog.Choice.LOCK;
        }
        try {
            return TouristSpotChoiceDialog.Choice.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return TouristSpotChoiceDialog.Choice.LOCK;
        }
    }

    private void purchaseCity() {
        Player player = players[currentPlayerIndex];

        if (currentTile instanceof City) {
            City city = (City) currentTile;

            if (isNetworkClient()) {
                handleClientCityPurchase(city, player);
                return;
            }

            // 레벨 선택 다이얼로그 표시
            LevelSelectionDialog dialog = new LevelSelectionDialog(
                frame,
                city.name,
                city.price,
                player.cash
            );
            dialog.setVisible(true);

            int selectedLevel = dialog.getSelectedLevel();

            if (selectedLevel == 0) {
                // 취소 선택
                log("구매를 취소했습니다.");
                endTurn();
                return;
            }

            finalizeCityPurchase(player, city, selectedLevel);
        } else if (currentTile instanceof TouristSpot) {
            TouristSpot touristSpot = (TouristSpot) currentTile;

            if (isNetworkClient()) {
                handleClientTouristPurchase(touristSpot, player);
                return;
            }

            // 관광지 매입 확인 다이얼로그 표시
            TouristSpotPurchaseDialog dialog = new TouristSpotPurchaseDialog(
                frame,
                touristSpot.name,
                touristSpot.price,
                player.cash
            );
            dialog.setVisible(true);

            if (!dialog.isConfirmed()) {
                // 취소 선택
                log("구매를 취소했습니다.");
                endTurn();
                return;
            }

            finalizeTouristPurchase(player, touristSpot);
        }

        endTurn();
    }

    private void upgradeCity() {
        if (isNetworkClient()) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("target", "CITY");
            payload.put("tileId", currentTile != null ? currentTile.id : -1);
            sendNetworkActionAndAwait(MessageType.UPGRADE, payload, "업그레이드 결과를 기다리는 중입니다...");
            return;
        }

        performCityUpgrade();
    }

    private void performCityUpgrade() {
        Player player = players[currentPlayerIndex];
        if (!(currentTile instanceof City)) {
            log("업그레이드할 도시 정보가 없습니다.");
            return;
        }
        City city = (City) currentTile;

        int upgradeCost = city.getUpgradeCost();
        if (ruleEngine.upgradeCity(player, city)) {
            frame.getOverlayPanel().showMoneyChange(currentPlayerIndex, -upgradeCost);

            String levelEmoji = city.getBuildingEmoji();
            String levelName = "";
            switch (city.level) {
                case 2: levelName = "아파트"; break;
                case 3: levelName = "건물"; break;
                case 4: levelName = "랜드마크"; break;
            }
            log(city.name + "을(를) 레벨 " + city.level + "(" + levelName + " " + levelEmoji + ")로 업그레이드했습니다!");

            if (city.isLandmark()) {
                log("🏛️ 랜드마크가 건설되었습니다! 다른 플레이어는 이 땅을 인수할 수 없습니다.");

                int landmarkPos = city.id;
                int pulledCount = ruleEngine.applyDualMagneticCore(landmarkPos, players, currentPlayerIndex);

                DualMagneticDialog magneticDialog = new DualMagneticDialog(frame, city.name, pulledCount);
                magneticDialog.setVisible(true);

                if (pulledCount > 0) {
                    log("🧲 듀얼 마그네틱 코어 발동! " + pulledCount + "명의 플레이어를 끌어당깁니다!");
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

        TakeoverConfirmDialog dialog = new TakeoverConfirmDialog(
            frame,
            city.name,
            seller.name,
            city.level,
            takeoverCost,
            buyer.cash
        );
        dialog.setVisible(true);

        if (!dialog.isConfirmed()) {
            log("도시 인수를 취소했습니다.");
            if (isNetworkClient()) {
                sendNetworkActionAndAwait(MessageType.PASS, null, "행동 결과를 기다리는 중입니다...");
                return;
            }
            endTurn();
            return;
        }

        if (isNetworkClient()) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("target", "CITY");
            payload.put("tileId", city.id);
            sendNetworkActionAndAwait(MessageType.TAKEOVER, payload, "인수 결과를 기다리는 중입니다...");
            return;
        }

        finalizeCityTakeover(city);
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
        dialog.setVisible(true);

        if (!dialog.isConfirmed()) {
            log("관광지 인수를 취소했습니다.");
            if (isNetworkClient()) {
                sendNetworkActionAndAwait(MessageType.PASS, null, "행동 결과를 기다리는 중입니다...");
                return;
            }
            endTurn();
            return;
        }

        if (isNetworkClient()) {
            TouristSpotChoiceDialog choiceDialog = new TouristSpotChoiceDialog(
                frame,
                spot.name
            );
            choiceDialog.setVisible(true);
            TouristSpotChoiceDialog.Choice choice = choiceDialog.getSelectedChoice();

            if (choice == null) {
                log("관광지 인수를 취소했습니다.");
                sendNetworkActionAndAwait(MessageType.PASS, null, "행동 결과를 기다리는 중입니다...");
                return;
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("target", "TOURIST");
            payload.put("tileId", spot.id);
            payload.put("choice", choice.name());
            sendNetworkActionAndAwait(MessageType.TAKEOVER, payload, "인수 결과를 기다리는 중입니다...");
            return;
        }

        if (finalizeTouristTakeover(spot)) {
            showTouristSpotChoiceDialog(spot, buyer);
            return;
        }

        endTurn();
    }

    private void skip() {
        Player player = players[currentPlayerIndex];

        if (isNetworkClient()) {
            sendNetworkActionAndAwait(MessageType.PASS, null, "행동 결과를 기다리는 중입니다...");
            return;
        }

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

        if (isNetworkClient()) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("choice", "BAIL");
            sendNetworkActionAndAwait(MessageType.JAIL_CHOICE, payload, "보석금 처리 결과를 기다리는 중입니다...");
            return;
        }

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

        if (isNetworkClient() && (state == GameState.WAITING_FOR_RAILROAD_SELECTION
            || state == GameState.WAITING_FOR_LANDMARK_SELECTION)) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("tileId", tileIndex);
            payload.put("context", state == GameState.WAITING_FOR_RAILROAD_SELECTION ? "RAILROAD" : "LANDMARK");
            sendNetworkActionAndAwait(MessageType.CITY_SELECTION, payload, "선택 결과를 기다리는 중입니다...");
            return;
        }

        // 전국철도 선택 처리
        if (state == GameState.WAITING_FOR_RAILROAD_SELECTION) {
            handleRailroadSelection(tileIndex, selectedTile);
            return;
        }

        // 랜드마크 건설 확정 처리
        if (state == GameState.WAITING_FOR_LANDMARK_SELECTION) {
            handleLandmarkSelection(selectedTile);
            return;
        }
    }

    private void handleRailroadSelection(int tileIndex, Tile selectedTile) {
        Player player = players[currentPlayerIndex];
        log(player.name + "이(가) " + selectedTile.name + " (칸 " + tileIndex + ")을(를) 선택했습니다!");

        player.pos = tileIndex;
        player.hasRailroadTicket = false;
        currentTile = selectedTile;
        frame.getBoardPanel().setTileClickEnabled(false);
        log("선택한 칸에서 이벤트를 처리합니다.");
        notifyRailroadSelectionEvent(player.name, selectedTile.name);
        handleTileLanding();
    }

    private void handleLandmarkSelection(Tile selectedTile) {
        if (!(selectedTile instanceof City)) {
            log("도시가 아닌 칸을 선택했습니다.");
            ErrorDialog errorDialog = new ErrorDialog(frame, "선택 오류", "도시가 아닌 칸을 선택했습니다.");
            errorDialog.setVisible(true);
            return;
        }

        City city = (City) selectedTile;

        if (!city.isOwned() || city.owner != currentPlayerIndex) {
            log("본인 소유 도시가 아닙니다.");
            ErrorDialog errorDialog = new ErrorDialog(frame, "선택 오류", "본인 소유 도시가 아닙니다.");
            errorDialog.setVisible(true);
            return;
        }

        if (city.level < 1 || city.level >= 4) {
            log("업그레이드할 수 없는 도시입니다. (레벨 1~3만 가능)");
            ErrorDialog errorDialog = new ErrorDialog(frame, "선택 오류", "업그레이드할 수 없는 도시입니다.");
            errorDialog.setVisible(true);
            return;
        }

        log(players[currentPlayerIndex].name + "이(가) " + city.name + " 업그레이드를 확정했습니다!");
        selectedLandmarkCity = city;
        handleLandmarkConstruction();
    }

    private void handleTaxTile() {
        Player player = players[currentPlayerIndex];
        int tax = ruleEngine.calculateTax(player);

        log("국세청에 도착했습니다!");

        // 세금 납부 확인 다이얼로그 (자신의 턴일 때만)
        if (shouldShowLocalDialog()) {
            TaxPaymentDialog taxDialog = new TaxPaymentDialog(
                frame,
                player.cash,
                tax
            );
            taxDialog.setVisible(true);
        }

        log("💸 보유 금액의 10%를 세금으로 납부합니다: " + String.format("%,d", tax) + "원");
        ruleEngine.payTax(player);

        // 자산 변동 표시
        frame.getOverlayPanel().showMoneyChange(currentPlayerIndex, -tax);

        if (player.bankrupt) {
            announceBankruptcy(currentPlayerIndex);
        }

        notifyTaxEvent(player.name, tax);

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

        JOptionPane.showMessageDialog(
            frame,
            "원하는 도시를 선택해주세요!\n\n보드에서 본인 소유 도시(레벨 1~3)를 클릭하면 1단계 업그레이드됩니다.",
            "도시 업그레이드",
            JOptionPane.INFORMATION_MESSAGE
        );

        // 보드 클릭 대기 상태로 전환
        state = GameState.WAITING_FOR_LANDMARK_SELECTION;
        frame.getBoardPanel().setTileClickEnabled(true);
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
            ErrorDialog errorDialog = new ErrorDialog(frame, "잔액 부족", "업그레이드 비용이 부족합니다.");
            errorDialog.setVisible(true);
            selectedLandmarkCity = null;
            frame.getBoardPanel().setTileClickEnabled(false);
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
        frame.getBoardPanel().setTileClickEnabled(false);

        endTurn();
    }

    private void handleOlympicTile() {
        Player player = players[currentPlayerIndex];

        // 올림픽 다이얼로그 표시 (자신의 턴일 때만)
        if (shouldShowLocalDialog()) {
            OlympicDialog olympicDialog = new OlympicDialog(frame);
            olympicDialog.setVisible(true);
        }

        log("올림픽에 도착했습니다!");
        notifyOlympicEvent(player.name);

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
                    announceBankruptcy(i);
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
        deleteDialog.setVisible(true);

        notifyPhaseDeleteEvent(deletedCity.name);

        // 보드 업데이트
        frame.getBoardPanel().repaint();
    }

    private void endTurn() {
        Player player = players[currentPlayerIndex];

        // 파산 시 더블 및 Extra Chance 무효화
        if (player.bankrupt) {
            announceBankruptcy(currentPlayerIndex);
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
                frame.getBoardPanel().setTileClickEnabled(false);

                updateDisplay();
                return; // 턴 종료하지 않음
            }

            // 더블 체크: 행동 완료 후 더블이면 추가 주사위 기회
            if (checkAndHandleDouble()) {
                log("🎲 더블! 한 번 더 굴릴 수 있습니다!");

                // 더블 다이얼로그 표시 (자신의 턴일 때만)
                if (shouldShowLocalDialog()) {
                    DoubleDialog doubleDialog = new DoubleDialog(frame, lastD1, consecutiveDoubles);
                    doubleDialog.setVisible(true);
                }

                // 네트워크 이벤트 알림
                notifyDoubleEvent(lastD1, consecutiveDoubles);

                // 더블 상태로 전환 (다시 주사위 굴리기 가능)
                state = GameState.WAITING_FOR_DOUBLE_ROLL;
                setActionButtons(true, false, false, false, false, false);
                frame.getBoardPanel().setTileClickEnabled(false);

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

        // 채팅 패널에 현재 플레이어 인덱스 업데이트
        frame.getOverlayPanel().setCurrentPlayerIndex(currentPlayerIndex);

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
            notifyGameOverEvent(winner, victoryType);

            // 재시작 옵션이 포함된 다이얼로그
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
        if (networkMode && isHost) {
            // 네트워크 모드: 게임 상태 리셋 및 클라이언트에 알림
            resetGameState();
            notifyGameRestart();
        } else if (!networkMode) {
            // 로컬 모드: 새 GameUI 생성
            frame.dispose();
            SwingUtilities.invokeLater(() -> {
                new GameUI(players.length, 1000000);
            });
        }
        // 클라이언트는 서버로부터 재시작 명령을 받아 처리
    }

    private void resetGameState() {
        // 게임 상태 초기화
        state = GameState.WAITING_FOR_ROLL;
        turnCount = 0;
        currentPlayerIndex = 0;
        consecutiveDoubles = 0;
        lastD1 = 0;
        lastD2 = 0;
        diceMode = DiceMode.NORMAL;

        // 플레이어 상태 초기화
        for (int i = 0; i < players.length; i++) {
            players[i].cash = 1000000;
            players[i].pos = 0;
            players[i].jailTurns = 0;
            players[i].bankrupt = false;
            players[i].hasRailroadTicket = false;
            players[i].hasExtraChance = false;
            bankruptcyAnnounced[i] = false;
        }

        // 보드 상태 초기화
        board.resetBoard();

        // UI 업데이트
        updateDisplay();
        log("=== 새 게임 시작 ===");
        log(players[0].name + "의 턴입니다.");
        setActionButtons(true, false, false, false, false, false);

        // 네트워크 상태 동기화
        if (networkMode && isHost) {
            notifyStateSync();
        }
    }

    private void notifyGameRestart() {
        if (!networkMode || !isHost) {
            return;
        }
        pushNetworkEvent(MessageType.GAME_RESTART, new HashMap<>());
    }

    public void handleRemoteGameRestart() {
        if (networkMode && !isHost) {
            SwingUtilities.invokeLater(() -> {
                log("=== 호스트가 새 게임을 시작했습니다 ===");
                // 클라이언트는 호스트로부터 상태 동기화를 받아 자동 업데이트됨
            });
        }
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
        frame.getBoardPanel().setTileClickEnabled(false);
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

    public void applyNetworkSnapshot(GameStateSnapshot snapshot) {
        if (!networkMode || snapshot == null) {
            return;
        }

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> applyNetworkSnapshot(snapshot));
            return;
        }

        GameStateMapper.apply(snapshot, board, players);
        this.turnCount = snapshot.getTurnCount();
        int incomingIndex = snapshot.getCurrentPlayerIndex();
        if (incomingIndex >= 0 && incomingIndex < players.length) {
            this.currentPlayerIndex = incomingIndex;
        }

        String phaseName = snapshot.getPhase();
        if (phaseName != null) {
            try {
                this.state = GameState.valueOf(phaseName);
            } catch (IllegalArgumentException e) {
                this.state = GameState.WAITING_FOR_ROLL;
            }
        }

        GameStateSnapshot.DiceState diceState = snapshot.getDiceState();
        if (diceState != null) {
            this.lastD1 = diceState.getDice1();
            this.lastD2 = diceState.getDice2();
        }

        Player focusedPlayer = players[currentPlayerIndex];
        if (focusedPlayer != null) {
            currentTile = board.getTile(focusedPlayer.pos);
        }

        frame.getBoardPanel().updateBoard();
        frame.getOverlayPanel().updatePlayerInfo();
        frame.updateDisplay(turnCount);

        if (isNetworkClient()) {
            applyClientAvailableActions(snapshot.getAvailableActions());
            int snapshotSequence = snapshot.getEventSequence();
            if (snapshot.getEventState() == null && snapshotSequence > lastHandledEventId) {
                lastHandledEventId = snapshotSequence; // late joiners skip history
            }
            handleNetworkEvent(snapshot.getEventState());
        }
    }

    public void handleRemoteRoll(String playerId, int section, String diceModeValue) {
        if (!networkMode || !isHost || !isCurrentNetworkPlayer(playerId)) {
            return;
        }

        DiceMode mode = parseDiceMode(diceModeValue);
        int clampedSection = Math.max(1, Math.min(4, section));

        SwingUtilities.invokeLater(() -> {
            log(players[currentPlayerIndex].name + "의 주사위 요청을 처리합니다.");
            int result = rollBiasedResultForSection(clampedSection);
            resolveDiceRoll(result, clampedSection, mode, true);
        });
    }

    public void handleRemotePurchase(String playerId, String target, Integer level, Integer tileId) {
        if (!networkMode || !isHost || !isCurrentNetworkPlayer(playerId)) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            if (!tileMatches(tileId)) {
                log("요청된 타일 정보가 서버 상태와 일치하지 않아 무시합니다.");
                return;
            }

            if ("TOURIST".equalsIgnoreCase(target) && currentTile instanceof TouristSpot) {
                finalizeTouristPurchase(players[currentPlayerIndex], (TouristSpot) currentTile);
                endTurn();
                return;
            }

            if (currentTile instanceof City) {
                int desiredLevel = level != null ? level : 1;
                desiredLevel = Math.max(1, Math.min(3, desiredLevel));
                finalizeCityPurchase(players[currentPlayerIndex], (City) currentTile, desiredLevel);
                endTurn();
            }
        });
    }

    public void handleRemoteUpgrade(String playerId, Integer tileId) {
        if (!networkMode || !isHost || !isCurrentNetworkPlayer(playerId)) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            if (!tileMatches(tileId)) {
                log("요청된 업그레이드 타일 정보가 서버 상태와 일치하지 않아 무시합니다.");
                return;
            }
            performCityUpgrade();
        });
    }

    public void handleRemoteSkip(String playerId) {
        if (!networkMode || !isHost || !isCurrentNetworkPlayer(playerId)) {
            return;
        }
        SwingUtilities.invokeLater(this::skip);
    }

    public void handleRemoteEscape(String playerId, String choice) {
        if (!networkMode || !isHost || !isCurrentNetworkPlayer(playerId)) {
            return;
        }

        if (!"BAIL".equalsIgnoreCase(choice)) {
            return;
        }

        SwingUtilities.invokeLater(this::escapeWithBail);
    }

    public void handleRemoteTakeover(String playerId, String target, Integer tileId, String choiceValue) {
        if (!networkMode || !isHost || !isCurrentNetworkPlayer(playerId)) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            if (!tileMatches(tileId)) {
                log("요청된 인수 타일 정보가 서버 상태와 일치하지 않아 무시합니다.");
                return;
            }

            if ("TOURIST".equalsIgnoreCase(target) && currentTile instanceof TouristSpot) {
                if (finalizeTouristTakeover((TouristSpot) currentTile)) {
                    TouristSpotChoiceDialog.Choice choice = parseTouristChoice(choiceValue);
                    applyTouristSpotChoice((TouristSpot) currentTile, players[currentPlayerIndex], choice);
                }
                endTurn();
                return;
            }

            if (currentTile instanceof City) {
                finalizeCityTakeover((City) currentTile);
                endTurn();
            }
        });
    }

    public void handleRemoteTileSelection(String playerId, Integer tileId, String context) {
        if (!networkMode || !isHost || !isCurrentNetworkPlayer(playerId) || tileId == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            Tile selectedTile = board.getTile(Math.max(0, Math.min(board.getSize() - 1, tileId)));

            if (state == GameState.WAITING_FOR_RAILROAD_SELECTION) {
                handleRailroadSelection(selectedTile.id, selectedTile);
                return;
            }

            if (state == GameState.WAITING_FOR_LANDMARK_SELECTION) {
                handleLandmarkSelection(selectedTile);
            }
        });
    }

    public void handleRemoteTouristSpotChoice(String playerId, Integer tileId, String choiceValue, Boolean purchased) {
        if (!networkMode || !isHost || !isCurrentNetworkPlayer(playerId)) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            if (tileId == null || !(currentTile instanceof TouristSpot)) {
                log("유효하지 않은 관광지 선택입니다.");
                endTurn();
                return;
            }

            TouristSpot touristSpot = (TouristSpot) currentTile;
            Player player = players[currentPlayerIndex];

            // 매입 처리 (매입 요청이 있었다면)
            if (purchased != null && purchased && !touristSpot.isOwned()) {
                if (ruleEngine.purchaseTouristSpot(player, touristSpot, currentPlayerIndex)) {
                    log("✅ " + touristSpot.name + "을(를) 매입했습니다!");
                    frame.getOverlayPanel().showMoneyChange(currentPlayerIndex, -touristSpot.price);
                } else {
                    log("❌ 매입 실패!");
                }
            }

            // 선택지 처리
            TouristSpotChoiceDialog.Choice choice = parseTouristChoice(choiceValue);
            if (applyTouristSpotChoice(touristSpot, player, choice)) {
                notifyTouristChoiceEvent(touristSpot.name, choice);
            }
            endTurn();
        });
    }

    public void handlePlayerDisconnect(String playerId) {
        if (!networkMode || !isHost || playerId == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            // 플레이어 찾기
            int disconnectedIndex = -1;
            for (int i = 0; i < players.length; i++) {
                if (playerId.equals(players[i].playerId)) {
                    disconnectedIndex = i;
                    break;
                }
            }

            if (disconnectedIndex < 0) {
                return;
            }

            Player disconnectedPlayer = players[disconnectedIndex];

            // 이미 파산한 경우 무시
            if (disconnectedPlayer.bankrupt) {
                return;
            }

            // 연결 끊김으로 인한 파산 처리
            log("⚠️ " + disconnectedPlayer.name + " 연결 끊김! 자동 파산 처리됩니다.");
            disconnectedPlayer.bankrupt = true;
            announceBankruptcy(disconnectedIndex);

            // 현재 턴이 연결 끊긴 플레이어의 턴이면 다음 턴으로
            if (currentPlayerIndex == disconnectedIndex) {
                log("연결 끊긴 플레이어의 턴을 건너뜁니다.");
                consecutiveDoubles = 0;
                endTurn();
            }

            // 게임 종료 체크
            if (isGameOver()) {
                endGame();
            }
        });
    }

    public void dispose() {
        if (frame != null) {
            SwingUtilities.invokeLater(() -> frame.dispose());
        }
    }

    /**
     * GameFrame 반환
     */
    public GameFrame getFrame() {
        return frame;
    }

    private void log(String message) {
        frame.getControlPanel().addLog(message);
    }

    private void updateDisplay() {
        frame.updateDisplay(turnCount);
        if (networkMode && isHost) {
            notifyStateSync();
        }
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

    private void setActionButtons(boolean roll, boolean purchase, boolean upgrade,
                                  boolean takeover, boolean skip, boolean escape) {
        frame.getActionPanel().setButtonsEnabled(roll, purchase, upgrade, takeover, skip, escape);
        updateAvailableActions(roll, purchase, upgrade, takeover, skip, escape);

        if (networkMode && isHost) {
            notifyStateSync();
        }
    }

    private void updateAvailableActions(boolean roll, boolean purchase, boolean upgrade,
                                        boolean takeover, boolean skip, boolean escape) {
        currentAvailableActions.clear();
        if (roll) currentAvailableActions.add(ACTION_ROLL);
        if (purchase) currentAvailableActions.add(ACTION_PURCHASE);
        if (upgrade) currentAvailableActions.add(ACTION_UPGRADE);
        if (takeover) currentAvailableActions.add(ACTION_TAKEOVER);
        if (skip) currentAvailableActions.add(ACTION_SKIP);
        if (escape) currentAvailableActions.add(ACTION_ESCAPE);
    }

    private void applyClientAvailableActions(List<String> actions) {
        if (!isNetworkClient()) {
            return;
        }

        if (!isLocalPlayersTurn()) {
            frame.getActionPanel().setButtonsEnabled(false, false, false, false, false, false);
            frame.getBoardPanel().setTileClickEnabled(false);
            return;
        }

        boolean roll = actions != null && actions.contains(ACTION_ROLL);
        boolean purchase = actions != null && actions.contains(ACTION_PURCHASE);
        boolean upgrade = actions != null && actions.contains(ACTION_UPGRADE);
        boolean takeover = actions != null && actions.contains(ACTION_TAKEOVER);
        boolean skip = actions != null && actions.contains(ACTION_SKIP);
        boolean escape = actions != null && actions.contains(ACTION_ESCAPE);

        frame.getActionPanel().setButtonsEnabled(roll, purchase, upgrade, takeover, skip, escape);
        frame.getBoardPanel().setTileClickEnabled(false);
        awaitingNetworkResolution = false;
    }

    private void handleNetworkEvent(GameStateSnapshot.EventState eventState) {
        if (!isNetworkClient() || eventState == null) {
            return;
        }

        if (eventState.getId() <= lastHandledEventId) {
            return;
        }
        lastHandledEventId = eventState.getId();

        String type = eventState.getType();
        if (type == null) {
            return;
        }

        switch (type) {
            case "CHANCE_EVENT":
                handleRemoteChanceEvent(eventState.getData());
                break;
            case "PHASE_DELETE":
                handleRemotePhaseDelete(eventState.getData());
                break;
            case "TOURIST_SPOT_CHOICE":
                handleRemoteTouristChoiceEvent(eventState.getData());
                break;
            case "CITY_SELECTION":
                handleRemoteRailroadSelectionEvent(eventState.getData());
                break;
            case "ROLL_DICE":
                handleRemoteDiceAnimation(eventState.getData());
                break;
            case "ISLAND_EVENT":
                handleRemoteIslandEvent(eventState.getData());
                break;
            case "WORLD_TOUR_EVENT":
                handleRemoteWorldTourEvent(eventState.getData());
                break;
            case "TAX_EVENT":
                handleRemoteTaxEvent(eventState.getData());
                break;
            case "TOLL_EVENT":
                handleRemoteTollEvent(eventState.getData());
                break;
            case "MAGNETIC_EVENT":
                handleRemoteMagneticEvent(eventState.getData());
                break;
            case "DOUBLE_EVENT":
                handleRemoteDoubleEvent(eventState.getData());
                break;
            case "OLYMPIC_EVENT":
                handleRemoteOlympicEvent(eventState.getData());
                break;
            case "TOURIST_LANDING_EVENT":
                handleRemoteTouristLandingEvent(eventState.getData());
                break;
            case "PLAYER_BANKRUPT":
                handleRemoteBankruptEvent(eventState.getData());
                break;
            case "GAME_OVER":
                handleRemoteGameOverEvent(eventState.getData());
                break;
            case "GAME_RESTART":
                handleRemoteGameRestart();
                break;
            default:
                break;
        }
    }

    private void sendNetworkAction(MessageType type, Map<String, Object> payload) {
        if (!isNetworkClient() || networkActionSender == null) {
            return;
        }

        Map<String, Object> data = payload != null ? new HashMap<>(payload) : new HashMap<>();
        networkActionSender.sendAction(type, data);
    }

    private void awaitNetworkResponse(String logMessage) {
        frame.getActionPanel().setButtonsEnabled(false, false, false, false, false, false);
        frame.getBoardPanel().setTileClickEnabled(false);
        if (logMessage != null) {
            log(logMessage);
        }
    }

    private void sendNetworkActionAndAwait(MessageType type, Map<String, Object> payload, String waitingMessage) {
        if (!canSendNetworkAction()) {
            log("현재 행동을 보낼 수 없습니다. 서버 응답을 기다려 주세요.");
            return;
        }
        sendNetworkAction(type, payload);
        awaitingNetworkResolution = true;
        awaitNetworkResponse(waitingMessage);
    }

    private void pushNetworkEvent(MessageType type, Map<String, Object> data) {
        if (!networkMode || !isHost) {
            return;
        }
        lastEventSequence++;
        GameStateSnapshot.EventState eventState = new GameStateSnapshot.EventState();
        eventState.setId(lastEventSequence);
        eventState.setType(type.name());
        eventState.setData(data);
        this.lastEventState = eventState;
        notifyStateSync();
    }

    private void handleNetworkDiceRelease() {
        DiceGauge gauge = frame.getActionPanel().getDiceGauge();
        gauge.stop();
        frame.getActionPanel().stopGaugeAnimation();

        if (!canSendNetworkAction()) {
            log("지금은 주사위를 굴릴 수 없습니다.");
            return;
        }

        int section = gauge.getCurrentSection();
        double position = gauge.getCurrentPosition();
        String sectionName = getSectionName(section);
        log("🎯 구간: " + sectionName);

        Map<String, Object> payload = new HashMap<>();
        payload.put("section", section);
        payload.put("position", position);
        payload.put("diceMode", diceMode.name());
        sendNetworkActionAndAwait(MessageType.ROLL_DICE, payload, "주사위 결과를 기다리는 중입니다...");
    }

    private void handleClientCityPurchase(City city, Player player) {
        LevelSelectionDialog dialog = new LevelSelectionDialog(
            frame,
            city.name,
            city.price,
            player.cash
        );
        dialog.setVisible(true);

        int selectedLevel = dialog.getSelectedLevel();
        if (selectedLevel == 0) {
            log("구매를 취소했습니다.");
            sendNetworkActionAndAwait(MessageType.PASS, null, "행동 결과를 기다리는 중입니다...");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("target", "CITY");
        payload.put("level", selectedLevel);
        payload.put("tileId", city.id);
        sendNetworkActionAndAwait(MessageType.BUY_CITY, payload, "구매 결과를 기다리는 중입니다...");
    }

    private void handleRemoteChanceEvent(Map<String, Object> data) {
        if (!isLocalPlayersTurn()) {
            return;
        }
        int reward = safeMapInt(data, "reward", ruleEngine.getChanceReward());
        ChanceDialog chanceDialog = new ChanceDialog(frame, reward);
        chanceDialog.setVisible(true);
        log("찬스 카드 이벤트로 " + String.format("%,d", reward) + "원을 획득했습니다.");
    }

    private void handleRemotePhaseDelete(Map<String, Object> data) {
        String cityName = data != null && data.get("city") != null
            ? data.get("city").toString()
            : "??";
        PhaseDeleteDialog deleteDialog = new PhaseDeleteDialog(frame, cityName);
        deleteDialog.setVisible(true);
        log("⚠️ 페이즈 딜리트: " + cityName + "이(가) 삭제되었습니다.");
    }

    private void handleRemoteDiceAnimation(Map<String, Object> data) {
        int dice1 = safeMapInt(data, "d1", 1);
        int dice2 = safeMapInt(data, "d2", 1);
        boolean isDouble = safeMapBoolean(data, "double", false);
        boolean suppressed = safeMapBoolean(data, "suppressed", false);
        int doubleCount = safeMapInt(data, "doubleCount", consecutiveDoubles);
        lastD1 = dice1;
        lastD2 = dice2;

        frame.getActionPanel().getDiceAnimationPanel().startAnimation(dice1, dice2, () -> {
            int sum = dice1 + dice2;
            if (isDouble) {
                log("🎲 주사위: [" + dice1 + ", " + dice2 + "] = " + sum + " - 더블!");
            } else {
                log("주사위: [" + dice1 + ", " + dice2 + "] = " + sum);
            }

            if (suppressed) {
                DoubleSuppressedDialog suppressedDialog = new DoubleSuppressedDialog(frame, dice1, doubleCount);
                suppressedDialog.setVisible(true);
            }
        });
    }

    private void handleRemoteTouristChoiceEvent(Map<String, Object> data) {
        String spot = safeMapString(data, "spot", "관광지");
        String choiceValue = safeMapString(data, "choice", TouristSpotChoiceDialog.Choice.LOCK.name());
        TouristSpotChoiceDialog.Choice choice = parseTouristChoice(choiceValue);
        String message;
        if (choice == TouristSpotChoiceDialog.Choice.LOCK) {
            message = "🔒 " + spot + " 잠금 선택";
        } else {
            message = "🎲 " + spot + "에서 추가 주사위 선택";
        }
        log(message);
    }

    private void handleRemoteRailroadSelectionEvent(Map<String, Object> data) {
        String playerName = safeMapString(data, "player", "플레이어");
        String tileName = safeMapString(data, "tile", "알 수 없는 칸");
        showInfoDialog("특수 이동", playerName + " → " + tileName + " 선택");
    }

    private void handleRemoteBankruptEvent(Map<String, Object> data) {
        String playerName = safeMapString(data, "player", "플레이어");
        log(playerName + "이(가) 파산했습니다!");
        showInfoDialog("파산", playerName + "이(가) 파산했습니다!");
    }

    private void handleRemoteGameOverEvent(Map<String, Object> data) {
        String winnerName = safeMapString(data, "winner", "플레이어");
        String victoryType = safeMapString(data, "victoryType", "승리");
        int cash = safeMapInt(data, "cash", 0);
        log("🎉 승자: " + winnerName + " 🎉");
        log("승리 조건: " + victoryType);
        showInfoDialog(
            "게임 종료",
            winnerName + " 승리!\n승리 조건: " + victoryType + "\n최종 자산: " +
                String.format("%,d", cash) + "원\n\n호스트가 \"새 게임\"을 선택하면 자동으로 재시작됩니다."
        );
    }

    private void handleRemoteIslandEvent(Map<String, Object> data) {
        if (!isLocalPlayersTurn()) {
            return;
        }
        int turns = safeMapInt(data, "turns", 2);
        IslandDialog dialog = new IslandDialog(frame, turns);
        dialog.setVisible(true);
    }

    private void handleRemoteWorldTourEvent(Map<String, Object> data) {
        if (!isLocalPlayersTurn()) {
            return;
        }
        WorldTourDialog dialog = new WorldTourDialog(frame);
        dialog.setVisible(true);
    }

    private void handleRemoteTaxEvent(Map<String, Object> data) {
        String playerName = safeMapString(data, "player", "플레이어");
        String playerId = safeMapString(data, "playerId", null);
        int amount = safeMapInt(data, "amount", 0);

        if (!isLocalPlayer(playerId)) {
            log(playerName + "이(가) 세금 " + String.format("%,d", amount) + "원을 납부했습니다.");
            return;
        }

        Player local = getLocalPlayer();
        int currentCash = local != null ? local.cash : 0;
        int balanceBefore = currentCash + amount;
        TaxPaymentDialog dialog = new TaxPaymentDialog(frame, balanceBefore, amount);
        dialog.setVisible(true);
    }

    private void handleRemoteTollEvent(Map<String, Object> data) {
        String playerId = safeMapString(data, "playerId", null);
        String playerName = safeMapString(data, "player", "플레이어");
        String tile = safeMapString(data, "tile", "타일");
        String owner = safeMapString(data, "owner", "소유자");
        int level = safeMapInt(data, "level", 1);
        int toll = safeMapInt(data, "toll", 0);
        boolean olympic = safeMapBoolean(data, "olympic", false);
        boolean tourist = safeMapBoolean(data, "tourist", false);

        if (!isLocalPlayer(playerId)) {
            String context = tourist ? "관광지" : "도시";
            log(playerName + "이(가) " + tile + " " + context + " 통행료 " +
                String.format("%,d", toll) + "원을 지불했습니다.");
            return;
        }

        Player local = getLocalPlayer();
        int currentCash = local != null ? local.cash : 0;
        int balanceBefore = currentCash + toll; // derive from local state to avoid stale host data
        TollPaymentDialog dialog = new TollPaymentDialog(
            frame,
            tile,
            owner,
            level,
            toll,
            olympic,
            balanceBefore
        );
        dialog.setVisible(true);
    }

    private void handleRemoteMagneticEvent(Map<String, Object> data) {
        String city = safeMapString(data, "city", "도시");
        int pulled = safeMapInt(data, "pulled", 0);
        DualMagneticDialog dialog = new DualMagneticDialog(frame, city, pulled);
        dialog.setVisible(true);
    }

    private void handleRemoteDoubleEvent(Map<String, Object> data) {
        if (!isLocalPlayersTurn()) {
            return;
        }
        int diceValue = safeMapInt(data, "diceValue", lastD1);
        int doubleCount = safeMapInt(data, "doubleCount", consecutiveDoubles);
        DoubleDialog doubleDialog = new DoubleDialog(frame, diceValue, doubleCount);
        doubleDialog.setVisible(true);
        log("🎲 더블! 한 번 더 굴릴 수 있습니다!");
    }

    private void handleRemoteOlympicEvent(Map<String, Object> data) {
        if (!isLocalPlayersTurn()) {
            return;
        }
        OlympicDialog olympicDialog = new OlympicDialog(frame);
        olympicDialog.setVisible(true);
        log("올림픽에 도착했습니다!");
    }

    private void handleRemoteTouristLandingEvent(Map<String, Object> data) {
        if (!isLocalPlayersTurn()) {
            return;
        }

        Integer tileId = (Integer) data.get("tileId");
        Boolean isOwned = (Boolean) data.get("isOwned");
        Integer ownerIndex = (Integer) data.get("ownerIndex");

        if (tileId == null) {
            return;
        }

        // 타일 ID로 관광지 찾기
        TouristSpot touristSpot = null;
        for (Tile tile : board.getAllTiles()) {
            if (tile instanceof TouristSpot && tile.id == tileId) {
                touristSpot = (TouristSpot) tile;
                break;
            }
        }

        if (touristSpot == null) {
            return;
        }

        Player player = players[currentPlayerIndex];

        if (isOwned != null && !isOwned) {
            // 미소유 관광지
            handleClientUnownedTouristSpot(touristSpot, player);
        } else if (ownerIndex != null && ownerIndex == currentPlayerIndex) {
            // 본인 소유 관광지
            handleClientOwnedTouristSpot(touristSpot, player);
        }
        // 타인 소유 관광지는 통행료 지불이므로 다이얼로그 필요 없음
    }

    private void handleClientTouristPurchase(TouristSpot touristSpot, Player player) {
        TouristSpotPurchaseDialog dialog = new TouristSpotPurchaseDialog(
            frame,
            touristSpot.name,
            touristSpot.price,
            player.cash
        );
        dialog.setVisible(true);

        if (!dialog.isConfirmed()) {
            log("구매를 취소했습니다.");
            sendNetworkActionAndAwait(MessageType.PASS, null, "행동 결과를 기다리는 중입니다...");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("target", "TOURIST");
        payload.put("tileId", touristSpot.id);
        sendNetworkActionAndAwait(MessageType.BUY_CITY, payload, "구매 결과를 기다리는 중입니다...");
    }

    private void handleClientUnownedTouristSpot(TouristSpot touristSpot, Player player) {
        // 미소유 관광지 매입 다이얼로그
        TouristSpotPurchaseDialog purchaseDialog = new TouristSpotPurchaseDialog(
            frame,
            touristSpot.name,
            touristSpot.price,
            player.cash
        );
        purchaseDialog.setVisible(true);

        boolean purchased = false;
        if (purchaseDialog.isConfirmed()) {
            // 매입 요청 전송
            Map<String, Object> payload = new HashMap<>();
            payload.put("target", "TOURIST");
            payload.put("tileId", touristSpot.id);
            sendNetworkAction(MessageType.BUY_CITY, payload);
            log("관광지 매입 요청을 전송했습니다.");
            purchased = true;
        } else {
            log("매입을 취소했습니다.");
        }

        // 선택지 다이얼로그 표시 (잠금/주사위)
        TouristSpotChoiceDialog choiceDialog = new TouristSpotChoiceDialog(
            frame,
            touristSpot.name
        );
        choiceDialog.setVisible(true);

        TouristSpotChoiceDialog.Choice choice = choiceDialog.getSelectedChoice();
        Map<String, Object> choicePayload = new HashMap<>();
        choicePayload.put("target", "TOURIST_CHOICE");
        choicePayload.put("tileId", touristSpot.id);
        choicePayload.put("choice", choice.name());
        choicePayload.put("purchased", purchased);
        sendNetworkActionAndAwait(MessageType.TOURIST_SPOT_CHOICE, choicePayload, "관광지 선택 결과를 기다리는 중입니다...");
    }

    private void handleClientOwnedTouristSpot(TouristSpot touristSpot, Player player) {
        // 본인 소유 관광지 선택지 다이얼로그
        TouristSpotChoiceDialog choiceDialog = new TouristSpotChoiceDialog(
            frame,
            touristSpot.name
        );
        choiceDialog.setVisible(true);

        TouristSpotChoiceDialog.Choice choice = choiceDialog.getSelectedChoice();
        Map<String, Object> payload = new HashMap<>();
        payload.put("target", "TOURIST_CHOICE");
        payload.put("tileId", touristSpot.id);
        payload.put("choice", choice.name());
        sendNetworkActionAndAwait(MessageType.TOURIST_SPOT_CHOICE, payload, "관광지 선택 결과를 기다리는 중입니다...");
    }

    private void notifyChanceEvent(String playerName, int reward) {
        if (!networkMode || !isHost) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("player", playerName);
        data.put("reward", reward);
        pushNetworkEvent(MessageType.CHANCE_EVENT, data);
    }

    private void notifyPhaseDeleteEvent(String cityName) {
        if (!networkMode || !isHost) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("city", cityName);
        pushNetworkEvent(MessageType.PHASE_DELETE, data);
    }

    private void notifyTouristChoiceEvent(String spotName, TouristSpotChoiceDialog.Choice choice) {
        if (!networkMode || !isHost) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("spot", spotName);
        data.put("choice", choice != null ? choice.name() : null);
        pushNetworkEvent(MessageType.TOURIST_SPOT_CHOICE, data);
    }

    private void notifyRailroadSelectionEvent(String playerName, String tileName) {
        if (!networkMode || !isHost) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("player", playerName);
        data.put("tile", tileName);
        pushNetworkEvent(MessageType.CITY_SELECTION, data);
    }

    private void notifyDoubleEvent(int diceValue, int doubleCount) {
        if (!networkMode || !isHost) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("diceValue", diceValue);
        data.put("doubleCount", doubleCount);
        pushNetworkEvent(MessageType.DOUBLE_EVENT, data);
    }

    private void notifyOlympicEvent(String playerName) {
        if (!networkMode || !isHost) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("player", playerName);
        pushNetworkEvent(MessageType.OLYMPIC_EVENT, data);
    }

    private void notifyTouristLandingEvent(TouristSpot touristSpot) {
        if (!networkMode || !isHost) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("tileId", touristSpot.id);
        data.put("isOwned", touristSpot.isOwned());
        data.put("ownerIndex", touristSpot.owner);
        pushNetworkEvent(MessageType.TOURIST_LANDING_EVENT, data);
    }

    private void notifyIslandEvent(String playerName, int turns) {
        if (!networkMode || !isHost) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("player", playerName);
        data.put("playerId", players[currentPlayerIndex].playerId);
        data.put("turns", turns);
        pushNetworkEvent(MessageType.ISLAND_EVENT, data);
    }

    private void notifyWorldTourEvent(String playerName) {
        if (!networkMode || !isHost) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("player", playerName);
        data.put("playerId", players[currentPlayerIndex].playerId);
        pushNetworkEvent(MessageType.WORLD_TOUR_EVENT, data);
    }

    private void notifyTaxEvent(String playerName, int amount) {
        if (!networkMode || !isHost) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("player", playerName);
        data.put("playerId", players[currentPlayerIndex].playerId);
        data.put("amount", amount);
        pushNetworkEvent(MessageType.TAX_EVENT, data);
    }

    private void notifyTollEvent(String playerName, String ownerName, String tileName, int level,
                                 int toll, boolean olympic, boolean tourist) {
        if (!networkMode || !isHost) {
            return;
        }
        Player payer = players[currentPlayerIndex];
        Map<String, Object> data = new HashMap<>();
        data.put("player", playerName);
        data.put("playerId", payer.playerId);
        data.put("owner", ownerName);
        data.put("tile", tileName);
        data.put("level", level);
        data.put("toll", toll);
        data.put("olympic", olympic);
        data.put("tourist", tourist);
        pushNetworkEvent(MessageType.TOLL_EVENT, data);
    }

    private void notifyMagneticEvent(String cityName, int pulled) {
        if (!networkMode || !isHost) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("city", cityName);
        data.put("pulled", pulled);
        pushNetworkEvent(MessageType.MAGNETIC_EVENT, data);
    }

    private void notifyBankruptcyEvent(Player player) {
        if (!networkMode || !isHost || player == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("player", player.name);
        data.put("playerId", player.playerId);
        data.put("cash", player.cash);
        pushNetworkEvent(MessageType.PLAYER_BANKRUPT, data);
    }

    private void notifyGameOverEvent(Player winner, String victoryType) {
        if (!networkMode || !isHost || winner == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("winner", winner.name);
        data.put("winnerId", winner.playerId);
        data.put("victoryType", victoryType);
        data.put("cash", winner.cash);
        pushNetworkEvent(MessageType.GAME_OVER, data);
    }

    private Map<String, Object> createDiceEventData(int sum, int dice1, int dice2,
                                                    boolean isDouble, boolean suppressed, int doubleCount) {
        Map<String, Object> data = new HashMap<>();
        data.put("sum", sum);
        data.put("d1", dice1);
        data.put("d2", dice2);
        data.put("double", isDouble);
        data.put("suppressed", suppressed);
        data.put("doubleCount", doubleCount);
        return data;
    }

    private void notifyStateSync() {
        if (!networkMode || !isHost || gameStateSyncListener == null) {
            return;
        }

        GameStateSnapshot.EventState pendingEvent = lastEventState;
        GameStateSnapshot snapshot = GameStateMapper.capture(
            board,
            players,
            currentPlayerIndex,
            turnCount,
            state.name(),
            lastD1,
            lastD2,
            lastD1 != 0 && lastD1 == lastD2,
            new ArrayList<>(currentAvailableActions),
            pendingEvent
        );
        snapshot.setEventSequence(lastEventSequence);
        gameStateSyncListener.onStateChanged(snapshot);
        if (pendingEvent != null && lastEventState != null &&
            pendingEvent.getId() == lastEventState.getId()) {
            lastEventState = null;
        }
    }

    private void announceBankruptcy(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= players.length) {
            return;
        }
        if (bankruptcyAnnounced[playerIndex]) {
            return;
        }
        bankruptcyAnnounced[playerIndex] = true;
        Player bankruptPlayer = players[playerIndex];
        log(bankruptPlayer.name + "이(가) 파산했습니다!");
        notifyBankruptcyEvent(bankruptPlayer);
    }

    private int clampPlayerIndex(int index, int length) {
        if (length <= 0) {
            return 0;
        }
        if (index < 0) {
            return 0;
        }
        if (index >= length) {
            return length - 1;
        }
        return index;
    }

    private int safeMapInt(Map<String, Object> data, String key, int defaultValue) {
        if (data == null) {
            return defaultValue;
        }
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return value != null ? Integer.parseInt(value.toString()) : defaultValue;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private String safeMapString(Map<String, Object> data, String key, String defaultValue) {
        if (data == null) {
            return defaultValue;
        }
        Object value = data.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private boolean safeMapBoolean(Map<String, Object> data, String key, boolean defaultValue) {
        if (data == null) {
            return defaultValue;
        }
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    private boolean isLocalPlayer(String playerId) {
        if (playerId == null || localPlayerIndex < 0 || localPlayerIndex >= players.length) {
            return false;
        }
        if (localPlayerId != null) {
            return playerId.equals(localPlayerId);
        }
        Player local = players[localPlayerIndex];
        return local.playerId != null && local.playerId.equals(playerId);
    }

    private Player getLocalPlayer() {
        if (localPlayerIndex >= 0 && localPlayerIndex < players.length) {
            return players[localPlayerIndex];
        }
        return null;
    }

    private void showInfoDialog(String title, String message) {
        JOptionPane.showMessageDialog(
            frame,
            message,
            title,
            JOptionPane.INFORMATION_MESSAGE
        );
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
}
