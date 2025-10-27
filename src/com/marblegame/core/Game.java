package com.marblegame.core;

import com.marblegame.cli.BoardRenderer;
import com.marblegame.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * 게임 루프 및 전체 흐름 관리
 */
public class Game {
    private final Board board;
    private final RuleEngine ruleEngine;
    private final List<Player> players;
    private final Dice dice;
    private final Scanner scanner;
    private final BoardRenderer renderer;
    private int turnCount = 0;

    public Game(int numPlayers, int initialCash) {
        this.board = new Board();
        this.ruleEngine = new RuleEngine(board);
        this.players = new ArrayList<>();
        this.dice = new Dice();
        this.scanner = new Scanner(System.in);
        this.renderer = new BoardRenderer(board);

        // 플레이어 초기화
        for (int i = 0; i < numPlayers; i++) {
            players.add(new Player("Player" + (char)('A' + i), initialCash));
        }
    }

    /**
     * 게임 시작
     */
    public void start() {
        System.out.println("=== 모두의 마블 게임 시작 ===");
        System.out.println("플레이어 수: " + players.size());
        System.out.println("초기 자금: 1,000,000원\n");

        while (!isGameOver()) {
            turnCount++;
            System.out.println("\n========== 턴 " + turnCount + " ==========");

            for (int i = 0; i < players.size(); i++) {
                Player player = players.get(i);

                if (player.bankrupt) {
                    continue;
                }

                playTurn(player, i);

                // 턴 종료 후 보드 렌더링
                renderer.render(players);

                if (isGameOver()) {
                    break;
                }
            }
        }

        announceWinner();
        scanner.close();
    }

    /**
     * 한 플레이어의 턴 처리
     */
    private void playTurn(Player player, int playerIndex) {
        System.out.println("\n--- " + player.name + "의 차례 ---");
        System.out.println(player);

        // 무인도 체크
        if (player.isInJail()) {
            handleJailTurn(player);
            return;
        }

        // 주사위 굴리기
        dice.roll();
        System.out.println("주사위: " + dice + " = " + dice.sum());

        // 이동 전 위치 저장 (출발지 통과 체크용)
        int oldPos = player.pos;

        // 이동
        player.move(dice.sum(), board.getSize(), board);
        Tile currentTile = board.getTile(player.pos);
        System.out.println(player.name + "이(가) " + currentTile.name + "에 도착했습니다.");

        // 출발지 통과 체크
        if (oldPos > player.pos || (oldPos != 0 && player.pos == 0)) {
            ruleEngine.paySalary(player);
            System.out.println("출발지를 통과하여 월급 " + String.format("%,d", ruleEngine.getSalary()) + "원을 받았습니다!");
        }

        // 도착한 칸 처리
        handleTileLanding(player, playerIndex, currentTile);
    }

    /**
     * 무인도 턴 처리
     */
    private void handleJailTurn(Player player) {
        System.out.println("무인도에 갇혀있습니다. (남은 턴: " + player.jailTurns + ")");
        System.out.println("1. 턴 패스");
        System.out.println("2. 보석금 200,000원 내고 탈출");
        System.out.print("선택: ");

        String choice = scanner.nextLine().trim();

        if (choice.equals("2")) {
            if (ruleEngine.escapeIslandWithBail(player)) {
                System.out.println("보석금을 내고 무인도에서 탈출했습니다!");
            } else {
                System.out.println("보석금이 부족합니다.");
                ruleEngine.decreaseJailTurns(player);
            }
        } else {
            // 턴 패스
            ruleEngine.decreaseJailTurns(player);
            System.out.println("턴을 패스했습니다. (남은 무인도 턴: " + player.jailTurns + ")");

            if (player.jailTurns == 0) {
                System.out.println("무인도 3턴이 지나 자동으로 탈출합니다!");
            }
        }
    }

    /**
     * 도착한 칸 처리
     */
    private void handleTileLanding(Player player, int playerIndex, Tile tile) {
        switch (tile.type) {
            case START:
                // 출발지는 통과 시에만 처리
                break;

            case CITY:
                handleCityTile(player, playerIndex, (City) tile);
                break;

            case ISLAND:
                System.out.println("무인도에 도착했습니다!");
                player.jailTurns = 2; // 2턴 갇힘
                System.out.println("무인도에 " + player.jailTurns + "턴 동안 갇힙니다.");
                break;

            case CHANCE:
                ruleEngine.processChance(player);
                System.out.println("찬스 카드! " + String.format("%,d", ruleEngine.getChanceReward()) + "원을 받았습니다!");
                break;
        }
    }

    /**
     * 도시 칸 처리
     */
    private void handleCityTile(Player player, int playerIndex, City city) {
        if (!city.isOwned()) {
            // 미소유 땅
            System.out.println(city.name + "은(는) 미소유 땅입니다. (가격: " + String.format("%,d", city.price) + "원)");
            System.out.print("매입하시겠습니까? (Y/N): ");

            String answer = scanner.nextLine().trim().toUpperCase();
            if (answer.equals("Y")) {
                if (ruleEngine.purchaseCity(player, city, playerIndex)) {
                    System.out.println(player.name + "이(가) " + city.name + "을(를) " + String.format("%,d", city.price) + "원에 매입했습니다!");
                } else {
                    System.out.println("자금이 부족하여 매입할 수 없습니다.");
                }
            }
        } else if (city.owner == playerIndex) {
            // 본인 소유 땅
            System.out.println(city.name + "은(는) 본인 소유입니다. (레벨: " + city.level + ")");

            if (city.canUpgrade()) {
                int upgradeCost = city.getUpgradeCost();
                System.out.print("업그레이드 하시겠습니까? (비용: " + String.format("%,d", upgradeCost) + "원) (Y/N): ");

                String answer = scanner.nextLine().trim().toUpperCase();
                if (answer.equals("Y")) {
                    if (ruleEngine.upgradeCity(player, city)) {
                        System.out.println(city.name + "을(를) 레벨 " + city.level + "로 업그레이드했습니다!");
                    } else {
                        System.out.println("자금이 부족하여 업그레이드할 수 없습니다.");
                    }
                }
            } else {
                System.out.println("최대 레벨입니다. 더 이상 업그레이드할 수 없습니다.");
            }
        } else {
            // 타인 소유 땅
            Player owner = players.get(city.owner);
            int toll = ruleEngine.calculateToll(city, city.owner);

            System.out.println(city.name + "은(는) " + owner.name + "의 소유입니다.");
            System.out.println("통행료 " + String.format("%,d", toll) + "원을 지불합니다.");

            ruleEngine.payToll(player, owner, toll);

            if (player.bankrupt) {
                System.out.println(player.name + "이(가) 파산했습니다!");
            }
        }
    }

    /**
     * 게임 종료 조건 체크
     */
    private boolean isGameOver() {
        int alive = 0;
        for (Player player : players) {
            if (!player.bankrupt) {
                alive++;
            }
        }
        return alive <= 1;
    }

    /**
     * 승자 발표
     */
    private void announceWinner() {
        System.out.println("\n\n=== 게임 종료 ===");
        for (Player player : players) {
            if (!player.bankrupt) {
                System.out.println("🎉 승자: " + player.name + " 🎉");
                System.out.println("최종 자산: " + String.format("%,d", player.cash) + "원");
                return;
            }
        }
    }

    /**
     * 플레이어 인덱스 조회
     */
    private int getPlayerIndex(Player player) {
        return players.indexOf(player);
    }

    public Board getBoard() {
        return board;
    }

    public List<Player> getPlayers() {
        return players;
    }
}
