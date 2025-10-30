package com.marblegame.core;

import com.marblegame.model.*;
import com.marblegame.ui.*;
import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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
        // 주사위 굴리기 - press-and-hold 이벤트
        setupDiceButtonPressAndHold();

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

        // 보드 타일 클릭 (전국철도 선택용)
        frame.getBoardPanel().setTileClickListener(tileIndex -> onTileSelected(tileIndex));
    }

    /**
     * 주사위 버튼에 press-and-hold 이벤트 설정
     */
    private void setupDiceButtonPressAndHold() {
        JButton diceButton = frame.getControlPanel().getRollDiceButton();

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
            frame.getBoardPanel().setTileClickEnabled(false);
            log("무인도에 갇혀있습니다. (남은 턴: " + player.jailTurns + ")");
            log("💰 보석금 200,000원으로 즉시 탈출하거나, ⏭ 패스하여 대기하세요.");
        } else if (player.hasRailroadTicket) {
            state = GameState.WAITING_FOR_RAILROAD_SELECTION;
            frame.getControlPanel().setButtonsEnabled(false, false, false, false, false, false);
            frame.getBoardPanel().setTileClickEnabled(true);
            log("🚆 전국철도 티켓이 있습니다!");
            log("보드에서 원하는 칸을 클릭하세요.");
            frame.getBoardPanel().showNotification("칸 선택", "클릭하세요!", new java.awt.Color(22, 160, 133));
        } else {
            state = GameState.WAITING_FOR_ROLL;
            frame.getControlPanel().setButtonsEnabled(true, false, false, false, false, false);
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
                updateDisplay();
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
                frame.getBoardPanel().showNotification("출발", "통과!", new java.awt.Color(46, 204, 113));
                endTurn();
                break;

            case CITY:
                frame.getBoardPanel().showNotification(currentTile.name, "도착!", java.awt.Color.WHITE);
                handleCityTile((City) currentTile);
                break;

            case PALACE:
                frame.getBoardPanel().showNotification(currentTile.name, "도착!", new java.awt.Color(155, 89, 182));
                handlePalaceTile((Palace) currentTile);
                break;

            case ISLAND:
                frame.getBoardPanel().showNotification("무인도", "갇힘!", new java.awt.Color(127, 140, 141));
                log("무인도에 도착했습니다!");
                player.jailTurns = 2; // 2턴 갇힘
                log("무인도에 " + player.jailTurns + "턴 동안 갇힙니다.");
                endTurn();
                break;

            case CHANCE:
                ruleEngine.processChance(player);
                frame.getBoardPanel().showNotification("찬스", "보너스!", new java.awt.Color(241, 196, 15));
                log("찬스 카드! " + String.format("%,d", ruleEngine.getChanceReward()) + "원을 받았습니다!");
                endTurn();
                break;

            case WELFARE:
                frame.getBoardPanel().showNotification("사회복지기금", "도착!", new java.awt.Color(52, 152, 219));
                log("사회복지기금에 도착했습니다! (기능 미구현)");
                endTurn();
                break;

            case RAILROAD:
                frame.getBoardPanel().showNotification("전국철도", "도착!", new java.awt.Color(22, 160, 133));
                log("전국철도에 도착했습니다!");
                log("다음 턴에 원하는 칸을 선택할 수 있습니다!");
                player.hasRailroadTicket = true;
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

            // 통행료 지불 알림
            frame.getBoardPanel().showNotification("통행료", String.format("%,d원 지불", toll), new java.awt.Color(231, 76, 60));

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

    private void handlePalaceTile(Palace palace) {
        Player player = players[currentPlayerIndex];

        if (!palace.isOwned()) {
            // 미소유 궁
            log(palace.name + "은(는) 미소유 관광지입니다. (가격: " + String.format("%,d", palace.price) + "원)");
            log("(관광지는 업그레이드가 불가능합니다)");
            state = GameState.WAITING_FOR_ACTION;
            frame.getControlPanel().setButtonsEnabled(false, true, false, false, true, false);
        } else if (palace.owner == currentPlayerIndex) {
            // 본인 소유 궁
            log(palace.name + "은(는) 본인 소유 관광지입니다.");
            log("(관광지는 업그레이드가 불가능합니다)");
            endTurn();
        } else {
            // 타인 소유 궁
            Player owner = players[palace.owner];
            int toll = ruleEngine.calculatePalaceToll(palace);

            log(palace.name + "은(는) " + owner.name + "의 소유 관광지입니다.");
            log("💸 통행료 " + String.format("%,d", toll) + "원을 지불합니다.");

            // 통행료 지불 알림
            frame.getBoardPanel().showNotification("통행료", String.format("%,d원 지불", toll), new java.awt.Color(231, 76, 60));

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
            if (ruleEngine.purchaseCity(player, city, currentPlayerIndex)) {
                log(player.name + "이(가) " + city.name + "을(를) " + String.format("%,d", city.price) + "원에 매입했습니다!");
                frame.getBoardPanel().showNotification(city.name, "구매 완료!", new java.awt.Color(46, 204, 113));
            } else {
                log("자금이 부족하여 매입할 수 없습니다.");
            }
        } else if (currentTile instanceof Palace) {
            Palace palace = (Palace) currentTile;
            if (ruleEngine.purchasePalace(player, palace, currentPlayerIndex)) {
                log(player.name + "이(가) " + palace.name + "을(를) " + String.format("%,d", palace.price) + "원에 매입했습니다!");
                frame.getBoardPanel().showNotification(palace.name, "구매 완료!", new java.awt.Color(155, 89, 182));
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
            log(city.name + "을(를) 레벨 " + city.level + "로 업그레이드했습니다!");
            frame.getBoardPanel().showNotification(city.name, "레벨 " + city.level + " 업그레이드!", new java.awt.Color(52, 152, 219));
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
            frame.getBoardPanel().showNotification(city.name, "인수 완료!", new java.awt.Color(230, 126, 34));
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

    /**
     * 타일 선택 이벤트 (전국철도 티켓 사용)
     */
    private void onTileSelected(int tileIndex) {
        // 전국철도 선택 상태가 아니면 무시
        if (state != GameState.WAITING_FOR_RAILROAD_SELECTION) {
            return;
        }

        Player player = players[currentPlayerIndex];
        Tile selectedTile = board.getTile(tileIndex);

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
