package com.marblegame.core;

import com.marblegame.model.*;
import com.marblegame.ui.*;
import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.List;

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

    private enum GameState {
        WAITING_FOR_ROLL,
        WAITING_FOR_ACTION,
        WAITING_FOR_JAIL_CHOICE,
        WAITING_FOR_RAILROAD_SELECTION,
        WAITING_FOR_LANDMARK_SELECTION,
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

    public GameUI(int numPlayers, int initialCash) {
        this.board = new Board();
        this.ruleEngine = new RuleEngine(board);
        this.players = new Player[numPlayers];
        this.dice = new Dice();

        // 플레이어 초기화
        for (int i = 0; i < numPlayers; i++) {
            players[i] = new Player("Player" + (char)('A' + i), initialCash);
        }

        // UI 초기화
        frame = new GameFrame(board, java.util.Arrays.asList(players));
        setupEventHandlers();

        frame.setVisible(true);
        frame.getControlPanel().addLog("=== 모두의 마블 게임 시작 ===");
        frame.getControlPanel().addLog("플레이어 수: " + numPlayers);
        frame.getControlPanel().addLog("초기 자금: " + String.format("%,d", initialCash) + "원\n");

        startTurn();
    }

    private void setupEventHandlers() {
        // 주사위 굴리기 - press-and-hold 이벤트
        setupDiceButtonPressAndHold();

        // 매입
        frame.getActionPanel().setPurchaseListener(e -> purchaseCity());

        // 업그레이드
        frame.getActionPanel().setUpgradeListener(e -> upgradeCity());

        // 인수
        frame.getActionPanel().setTakeoverListener(e -> takeoverCity());

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
                    // 게이지 정지 및 주사위 굴리기
                    rollDiceWithGauge();
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

        log("\n--- " + player.name + "의 차례 ---");
        log(String.format("%s (현금: %,d원, 위치: %d)", player.name, player.cash, player.pos));

        if (player.isInJail()) {
            state = GameState.WAITING_FOR_JAIL_CHOICE;
            frame.getActionPanel().setButtonsEnabled(false, false, false, false, true, true);
            frame.getBoardPanel().setTileClickEnabled(false);
            log("무인도에 갇혀있습니다. (남은 턴: " + player.jailTurns + ")");
            log("💰 보석금 200,000원으로 즉시 탈출하거나, ⏭ 패스하여 대기하세요.");
        } else if (player.hasRailroadTicket) {
            state = GameState.WAITING_FOR_RAILROAD_SELECTION;
            frame.getActionPanel().setButtonsEnabled(false, false, false, false, false, false);
            frame.getBoardPanel().setTileClickEnabled(true);
            log("🚆 전국철도/세계여행 티켓이 있습니다!");
            log("보드에서 원하는 칸을 클릭하세요.");

            // 도시 선택 안내 다이얼로그 표시
            CitySelectionDialog selectionDialog = new CitySelectionDialog(frame);
            selectionDialog.setVisible(true);
        } else {
            state = GameState.WAITING_FOR_ROLL;
            frame.getActionPanel().setButtonsEnabled(true, false, false, false, false, false);
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

        if (state == GameState.WAITING_FOR_ROLL) {
            // 게이지 정지 및 결과 생성
            int result = frame.getActionPanel().getDiceGauge().stop();
            frame.getActionPanel().stopGaugeAnimation();

            int section = frame.getActionPanel().getDiceGauge().getCurrentSection();
            String sectionName = getSectionName(section);

            log("🎯 구간: " + sectionName);

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

            // 주사위 2개로 분할 (2~12 범위를 2D6로 변환)
            int tempD1, tempD2;
            if (result <= 7) {
                // 2~7: d1 = 1~6, d2 = result - d1
                tempD1 = 1 + (int)(Math.random() * Math.min(6, result - 1));
                tempD2 = result - tempD1;
                if (tempD2 > 6) {
                    tempD1 = result - 6;
                    tempD2 = 6;
                }
            } else {
                // 8~12: d1 = result - 6 ~ 6
                tempD1 = Math.max(result - 6, 1 + (int)(Math.random() * 6));
                tempD2 = result - tempD1;
                if (tempD1 > 6) tempD1 = 6;
                if (tempD2 > 6) tempD2 = 6;
            }

            // final 변수로 복사 (람다 사용을 위해)
            final int finalD1 = tempD1;
            final int finalD2 = tempD2;
            final int finalResult = result;

            // 주사위 애니메이션 시작
            frame.getActionPanel().getDiceAnimationPanel().startAnimation(finalD1, finalD2, () -> {
                log("주사위: [" + finalD1 + ", " + finalD2 + "] = " + finalResult);
                movePlayer(finalResult);
            });
        }
    }

    /**
     * 구간 이름 반환
     */
    private String getSectionName(int section) {
        switch (section) {
            case 1: return "S1 (2~5 우대)";
            case 2: return "S2 (6~9 우대)";
            case 3: return "S3 (10~12 우대)";
            default: return "Unknown";
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
                islandDialog.setVisible(true);

                log("무인도에 도착했습니다!");
                log("무인도에 " + player.jailTurns + "턴 동안 갇힙니다.");
                endTurn();
                break;

            case CHANCE:
                int chanceReward = ruleEngine.getChanceReward();
                ruleEngine.processChance(player);

                // 찬스 다이얼로그 표시
                ChanceDialog chanceDialog = new ChanceDialog(frame, chanceReward);
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
                handleOlympicTile();
                break;

            case WORLD_TOUR:
                // 세계여행 다이얼로그 표시
                WorldTourDialog worldTourDialog = new WorldTourDialog(frame);
                worldTourDialog.setVisible(true);

                log("세계여행에 도착했습니다!");
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
            frame.getActionPanel().setButtonsEnabled(false, true, false, false, true, false);
        } else if (city.owner == currentPlayerIndex) {
            // 본인 소유 땅
            log(city.name + "은(는) 본인 소유입니다. (레벨: " + city.level + ")");

            if (city.canUpgrade()) {
                int upgradeCost = city.getUpgradeCost();
                log("업그레이드 비용: " + String.format("%,d", upgradeCost) + "원");
                state = GameState.WAITING_FOR_ACTION;
                frame.getActionPanel().setButtonsEnabled(false, false, true, false, true, false);
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
            tollDialog.setVisible(true);

            log("💸 통행료 " + String.format("%,d", toll) + "원을 지불합니다.");
            ruleEngine.payToll(player, owner, toll);

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
                    frame.getActionPanel().setButtonsEnabled(false, false, false, true, true, false);
                }
            }
        }
    }

    private void handleTouristSpotTile(TouristSpot touristSpot) {
        Player player = players[currentPlayerIndex];

        if (!touristSpot.isOwned()) {
            // 미소유 관광지
            log(touristSpot.name + "은(는) 미소유 관광지입니다. (가격: " + String.format("%,d", touristSpot.price) + "원)");
            log("(관광지는 업그레이드가 불가능합니다)");
            state = GameState.WAITING_FOR_ACTION;
            frame.getActionPanel().setButtonsEnabled(false, true, false, false, true, false);
        } else if (touristSpot.owner == currentPlayerIndex) {
            // 본인 소유 관광지
            log(touristSpot.name + "은(는) 본인 소유 관광지입니다.");
            log("(관광지는 업그레이드가 불가능합니다)");
            endTurn();
        } else {
            // 타인 소유 관광지
            Player owner = players[touristSpot.owner];
            int toll = ruleEngine.calculateTouristSpotToll(touristSpot);

            log(touristSpot.name + "은(는) " + owner.name + "의 소유 관광지입니다.");

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
            tollDialog.setVisible(true);

            log("💸 통행료 " + String.format("%,d", toll) + "원을 지불합니다.");
            ruleEngine.payToll(player, owner, toll);

            if (player.bankrupt) {
                log(player.name + "이(가) 파산했습니다!");
            }

            endTurn();
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
            dialog.setVisible(true);

            if (!dialog.isConfirmed()) {
                // 취소 선택
                log("구매를 취소했습니다.");
                endTurn();
                return;
            }

            // 매입 시도
            if (ruleEngine.purchaseTouristSpot(player, touristSpot, currentPlayerIndex)) {
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

        if (ruleEngine.upgradeCity(player, city)) {
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
            frame.getActionPanel().setButtonsEnabled(true, false, false, false, false, false);
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
            frame.getBoardPanel().setTileClickEnabled(false);

            // 선택한 타일 처리
            log("선택한 칸에서 이벤트를 처리합니다.");
            handleTileLanding();
            return;
        }

        // 랜드마크 건설 확정 처리
        if (state == GameState.WAITING_FOR_LANDMARK_SELECTION) {
            // 클릭한 타일이 도시인지 확인
            if (!(selectedTile instanceof City)) {
                log("도시가 아닌 칸을 선택했습니다. 랜드마크 건설을 취소합니다.");
                frame.getBoardPanel().setTileClickEnabled(false);
                endTurn();
                return;
            }

            City city = (City) selectedTile;

            // 레벨 3 본인 소유 도시인지 확인
            if (!city.isOwned() || city.owner != currentPlayerIndex) {
                log("본인 소유 도시가 아닙니다. 랜드마크 건설을 취소합니다.");
                frame.getBoardPanel().setTileClickEnabled(false);
                endTurn();
                return;
            }

            if (city.level != 3) {
                log("레벨 3 도시가 아닙니다. 랜드마크 건설을 취소합니다.");
                frame.getBoardPanel().setTileClickEnabled(false);
                endTurn();
                return;
            }

            if (city.isLandmark()) {
                log("이미 랜드마크인 도시입니다. 랜드마크 건설을 취소합니다.");
                frame.getBoardPanel().setTileClickEnabled(false);
                endTurn();
                return;
            }

            // 조건을 만족하면 랜드마크 건설
            log(player.name + "이(가) " + selectedTile.name + "에 랜드마크 건설을 확정했습니다!");
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
        taxDialog.setVisible(true);

        log("💸 보유 금액의 10%를 세금으로 납부합니다: " + String.format("%,d", tax) + "원");
        ruleEngine.payTax(player);

        if (player.bankrupt) {
            log(player.name + "이(가) 파산했습니다!");
        }

        // 세금 납부 후 즉시 턴 종료
        endTurn();
    }

    private void handleStartTile() {
        Player player = players[currentPlayerIndex];
        log("START 지점에 도착했습니다!");

        // 레벨 3 도시가 있는지 확인
        boolean hasLevel3City = false;
        for (Tile tile : board.getAllTiles()) {
            if (tile instanceof City) {
                City city = (City) tile;
                if (city.isOwned() && city.owner == currentPlayerIndex && city.level == 3 && !city.isLandmark()) {
                    hasLevel3City = true;
                    break;
                }
            }
        }

        if (!hasLevel3City) {
            log("랜드마크를 건설할 수 있는 도시가 없습니다. (레벨 3 도시 필요)");
            endTurn();
            return;
        }

        // 간단한 안내 메시지 다이얼로그 표시
        log("🏛️ 레벨 3 도시를 랜드마크로 업그레이드할 수 있습니다!");

        JOptionPane.showMessageDialog(
            frame,
            "원하는 도시를 선택해주세요!\n\n보드에서 레벨 3 도시를 클릭하면 랜드마크가 건설됩니다.",
            "랜드마크 건설",
            JOptionPane.INFORMATION_MESSAGE
        );

        // 보드 클릭 대기 상태로 전환
        state = GameState.WAITING_FOR_LANDMARK_SELECTION;
        frame.getBoardPanel().setTileClickEnabled(true);
        log("📍 레벨 3 도시를 클릭하여 랜드마크를 건설하세요.");
    }

    private void handleLandmarkConstruction() {
        Player player = players[currentPlayerIndex];

        if (selectedLandmarkCity == null) {
            log("오류: 선택된 도시가 없습니다.");
            endTurn();
            return;
        }

        int constructionCost = (int)(selectedLandmarkCity.price * 0.4);

        if (!player.canAfford(constructionCost)) {
            log("잔액이 부족하여 랜드마크를 건설할 수 없습니다.");
            selectedLandmarkCity = null;
            endTurn();
            return;
        }

        // 랜드마크 건설
        player.pay(constructionCost);
        selectedLandmarkCity.level = 4;
        log("🏛️ " + selectedLandmarkCity.name + "에 랜드마크를 건설했습니다!");
        log("건설 비용: " + String.format("%,d", constructionCost) + "원");
        log("남은 잔액: " + String.format("%,d", player.cash) + "원");

        // 상태 초기화
        selectedLandmarkCity = null;
        state = GameState.WAITING_FOR_ROLL;
        frame.getBoardPanel().setTileClickEnabled(false);

        endTurn();
    }

    private void handleOlympicTile() {
        Player player = players[currentPlayerIndex];

        // 올림픽 다이얼로그 표시
        OlympicDialog olympicDialog = new OlympicDialog(frame);
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

        // 보드 업데이트
        frame.getBoardPanel().repaint();
    }

    private void endTurn() {
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
        frame.getActionPanel().setButtonsEnabled(false, false, false, false, false, false);
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
            new GameUI(players.length, 1000000);
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
        frame.getActionPanel().setButtonsEnabled(false, false, false, false, false, false);
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
}
