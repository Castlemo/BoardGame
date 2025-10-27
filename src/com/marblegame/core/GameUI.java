package com.marblegame.core;

import com.marblegame.model.*;
import com.marblegame.ui.*;
import javax.swing.*;

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
        GAME_OVER
    }

    private Tile currentTile;

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
        // 주사위 굴리기
        frame.getControlPanel().setRollDiceListener(e -> rollDice());

        // 매입
        frame.getControlPanel().setPurchaseListener(e -> purchaseCity());

        // 업그레이드
        frame.getControlPanel().setUpgradeListener(e -> upgradeCity());

        // 인수
        frame.getControlPanel().setTakeoverListener(e -> takeoverCity());

        // 패스
        frame.getControlPanel().setSkipListener(e -> skip());

        // 보석금 탈출
        frame.getControlPanel().setEscapeListener(e -> escapeWithBail());
    }

    private void startTurn() {
        if (isGameOver()) {
            endGame();
            return;
        }

        Player player = players[currentPlayerIndex];

        if (player.bankrupt) {
            nextPlayer();
            return;
        }

        // 5턴 이후부터 페이즈 딜리트 실행 (매 턴 시작 시 한 번)
        if (currentPlayerIndex == 0 && turnCount >= 5) {
            String deletedCity = ruleEngine.performPhaseDelete();
            if (deletedCity != null) {
                log("\n⚠️ [페이즈 딜리트] " + deletedCity + " 칸이 삭제되었습니다! ⚠️");
            }
        }

        log("\n--- " + player.name + "의 차례 ---");
        log(String.format("%s (현금: %,d원, 위치: %d)", player.name, player.cash, player.pos));

        if (player.isInJail()) {
            state = GameState.WAITING_FOR_JAIL_CHOICE;
            frame.getControlPanel().setButtonsEnabled(false, false, false, false, true, true);
            log("무인도에 갇혀있습니다. (남은 턴: " + player.jailTurns + ")");
            log("💰 보석금 200,000원으로 즉시 탈출하거나, ⏭ 패스하여 대기하세요.");
        } else {
            state = GameState.WAITING_FOR_ROLL;
            frame.getControlPanel().setButtonsEnabled(true, false, false, false, false, false);
            log("주사위를 굴려주세요.");
        }

        updateDisplay();
    }

    private void rollDice() {
        Player player = players[currentPlayerIndex];

        if (state == GameState.WAITING_FOR_ROLL) {
            // 일반 주사위
            dice.roll();
            log("주사위: [" + dice.getD1() + ", " + dice.getD2() + "] = " + dice.sum());

            movePlayer(dice.sum());
        }

        updateDisplay();
    }

    private void movePlayer(int steps) {
        Player player = players[currentPlayerIndex];
        int oldPos = player.pos;

        player.move(steps, board.getSize(), board);
        currentTile = board.getTile(player.pos);

        log(player.name + "이(가) " + currentTile.name + "에 도착했습니다.");

        // 출발지 통과 체크
        if (oldPos > player.pos || (oldPos != 0 && player.pos == 0)) {
            ruleEngine.paySalary(player);
            log("출발지를 통과하여 월급 " + String.format("%,d", ruleEngine.getSalary()) + "원을 받았습니다!");
        }

        handleTileLanding();
    }

    private void handleTileLanding() {
        Player player = players[currentPlayerIndex];

        switch (currentTile.type) {
            case START:
                endTurn();
                break;

            case CITY:
                handleCityTile((City) currentTile);
                break;

            case ISLAND:
                log("무인도에 도착했습니다!");
                player.jailTurns = 2; // 2턴 갇힘
                log("무인도에 " + player.jailTurns + "턴 동안 갇힙니다.");
                endTurn();
                break;

            case CHANCE:
                ruleEngine.processChance(player);
                log("찬스 카드! " + String.format("%,d", ruleEngine.getChanceReward()) + "원을 받았습니다!");
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
            frame.getControlPanel().setButtonsEnabled(false, true, false, false, true, false);
        } else if (city.owner == currentPlayerIndex) {
            // 본인 소유 땅
            log(city.name + "은(는) 본인 소유입니다. (레벨: " + city.level + ")");

            if (city.canUpgrade()) {
                int upgradeCost = city.getUpgradeCost();
                log("업그레이드 비용: " + String.format("%,d", upgradeCost) + "원");
                state = GameState.WAITING_FOR_ACTION;
                frame.getControlPanel().setButtonsEnabled(false, false, true, false, true, false);
            } else {
                log("최대 레벨입니다. 더 이상 업그레이드할 수 없습니다.");
                endTurn();
            }
        } else {
            // 타인 소유 땅
            Player owner = players[city.owner];
            int toll = ruleEngine.calculateToll(city, city.owner);

            log(city.name + "은(는) " + owner.name + "의 소유입니다. (레벨: " + city.level + ")");
            log("💸 통행료 " + String.format("%,d", toll) + "원을 지불합니다.");

            ruleEngine.payToll(player, owner, toll);

            if (player.bankrupt) {
                log(player.name + "이(가) 파산했습니다!");
                endTurn();
            } else {
                // 통행료 지불 후 인수 선택지 제공
                int takeoverCost = ruleEngine.calculateTakeoverCost(city);
                log("💰 인수 비용: " + String.format("%,d", takeoverCost) + "원");
                log("이 땅을 인수하거나 패스하세요.");
                state = GameState.WAITING_FOR_ACTION;
                frame.getControlPanel().setButtonsEnabled(false, false, false, true, true, false);
            }
        }
    }

    private void purchaseCity() {
        Player player = players[currentPlayerIndex];
        City city = (City) currentTile;

        if (ruleEngine.purchaseCity(player, city, currentPlayerIndex)) {
            log(player.name + "이(가) " + city.name + "을(를) " + String.format("%,d", city.price) + "원에 매입했습니다!");
        } else {
            log("자금이 부족하여 매입할 수 없습니다.");
        }

        endTurn();
    }

    private void upgradeCity() {
        Player player = players[currentPlayerIndex];
        City city = (City) currentTile;

        if (ruleEngine.upgradeCity(player, city)) {
            log(city.name + "을(를) 레벨 " + city.level + "로 업그레이드했습니다!");
        } else {
            log("자금이 부족하여 업그레이드할 수 없습니다.");
        }

        endTurn();
    }

    private void takeoverCity() {
        Player buyer = players[currentPlayerIndex];
        City city = (City) currentTile;
        Player seller = players[city.owner];

        int takeoverCost = ruleEngine.calculateTakeoverCost(city);

        if (ruleEngine.takeoverCity(buyer, seller, city, currentPlayerIndex)) {
            log(buyer.name + "이(가) " + seller.name + "으로부터 " + city.name + "을(를) " +
                String.format("%,d", takeoverCost) + "원에 인수했습니다!");
            log(seller.name + "이(가) " + String.format("%,d", takeoverCost) + "원을 받았습니다.");
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
            frame.getControlPanel().setButtonsEnabled(true, false, false, false, false, false);
            updateDisplay();
        } else {
            log("보석금이 부족합니다.");
        }
    }

    private void endTurn() {
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
        int alive = 0;
        for (Player player : players) {
            if (!player.bankrupt) {
                alive++;
            }
        }
        return alive <= 1;
    }

    private void endGame() {
        state = GameState.GAME_OVER;
        frame.getControlPanel().setButtonsEnabled(false, false, false, false, false, false);

        log("\n\n=== 게임 종료 ===");
        for (Player player : players) {
            if (!player.bankrupt) {
                log("🎉 승자: " + player.name + " 🎉");
                log("최종 자산: " + String.format("%,d", player.cash) + "원");

                // 재시작 옵션이 포함된 다이얼로그
                int choice = JOptionPane.showOptionDialog(
                    frame,
                    player.name + " 승리!\n최종 자산: " + String.format("%,d", player.cash) + "원",
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
                return;
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

    private void log(String message) {
        frame.getControlPanel().addLog(message);
    }

    private void updateDisplay() {
        frame.updateDisplay(turnCount);
    }
}
