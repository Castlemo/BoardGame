package com.marblegame.core;

import com.marblegame.model.*;
import java.util.List;

/**
 * 게임 규칙 엔진
 * 통행료 계산, 매입, 업그레이드, 파산, 무인도 처리
 */
public class RuleEngine {
    // rules.json 값들
    private final int salary = 200000;
    private final int bail = 200000;
    private final int islandMaxTurns = 2; // 2턴 대기
    private final double[] tollMultiplierByLevel = {0.0, 1.5, 2.2, 3.0, 4.0}; // 레벨 0~4
    private final double colorMonopolyMultiplier = 1.5;
    private final int chanceReward = 100000;
    private final double taxRate = 0.1; // 국세청 세율 10%

    private final Board board;

    public RuleEngine(Board board) {
        this.board = board;
    }

    /**
     * 통행료 계산
     * level과 컬러 독점 여부, 올림픽 효과 반영
     */
    public int calculateToll(City city, int ownerIndex) {
        int toll = (int)(city.baseToll * tollMultiplierByLevel[city.level]);

        // 컬러 독점 체크
        if (hasColorMonopoly(ownerIndex, city.colorGroup)) {
            toll = (int)(toll * colorMonopolyMultiplier);
        }

        // 올림픽 효과로 통행료 2배
        if (city.hasOlympicBoost) {
            toll *= 2;
        }

        return toll;
    }

    /**
     * 특정 플레이어가 해당 컬러 그룹을 독점했는지 확인
     *
     * 독점 조건:
     * 해당 컬러 그룹의 모든 칸이 동일한 플레이어 소유여야 함
     */
    public boolean hasColorMonopoly(int playerIndex, String colorGroup) {
        List<City> cities = board.getCitiesByColor(colorGroup);

        if (cities.isEmpty()) {
            return false;
        }

        for (City city : cities) {
            if (!city.isOwned() || city.owner != playerIndex) {
                return false;
            }
        }

        return true;
    }

    /**
     * 도시 매입 처리
     */
    public boolean purchaseCity(Player player, City city, int playerIndex) {
        if (city.isOwned()) {
            return false;
        }

        if (!player.canAfford(city.price)) {
            return false;
        }

        player.pay(city.price);
        city.owner = playerIndex;
        return true;
    }

    /**
     * 선택한 레벨로 도시 구매
     * @param player 플레이어
     * @param city 도시 타일
     * @param level 선택한 레벨 (1, 2, 3)
     * @param playerIndex 플레이어 인덱스
     * @return 구매 성공 여부
     */
    public boolean purchaseCityWithLevel(Player player, City city, int level, int playerIndex) {
        if (city.isOwned()) {
            return false;
        }

        if (level < 1 || level > 3) {
            return false; // 잘못된 레벨
        }

        // 누적 비용 계산
        int totalCost = calculateLevelCost(city.price, level);

        if (!player.canAfford(totalCost)) {
            return false;
        }

        player.pay(totalCost);
        city.owner = playerIndex;
        city.level = level; // 선택한 레벨로 설정
        return true;
    }

    /**
     * 레벨별 누적 건설 비용 계산
     * @param basePrice 기본 가격
     * @param level 목표 레벨 (1, 2, 3)
     * @return 누적 건설 비용
     */
    public int calculateLevelCost(int basePrice, int level) {
        switch (level) {
            case 1: return basePrice;                    // 레벨 1: 기본가
            case 2: return (int)(basePrice * 1.3);       // 레벨 2: 기본가 + 30%
            case 3: return (int)(basePrice * 1.6);       // 레벨 3: 기본가 + 60%
            default: return basePrice;
        }
    }

    /**
     * 관광지(TouristSpot) 매입 처리
     */
    public boolean purchaseTouristSpot(Player player, TouristSpot touristSpot, int playerIndex) {
        if (touristSpot.isOwned()) {
            return false;
        }

        if (!player.canAfford(touristSpot.price)) {
            return false;
        }

        player.pay(touristSpot.price);
        touristSpot.owner = playerIndex;
        return true;
    }

    /**
     * 관광지(TouristSpot) 통행료 계산 (기본가 × 2.0)
     */
    public int calculateTouristSpotToll(TouristSpot touristSpot) {
        return (int)(touristSpot.price * 2.0);
    }

    /**
     * 관광지 잠금 처리
     */
    public void lockTouristSpot(TouristSpot touristSpot, int playerIndex) {
        touristSpot.setLocked(true);
        touristSpot.lockedBy = playerIndex;
    }

    /**
     * 관광지 잠금 해제 처리
     */
    public void unlockTouristSpot(TouristSpot touristSpot) {
        touristSpot.setLocked(false);
        touristSpot.lockedBy = null;
    }

    /**
     * 특정 플레이어가 잠근 모든 관광지의 잠금 해제
     */
    public void unlockPlayerTouristSpots(int playerIndex) {
        for (int i = 0; i < board.getSize(); i++) {
            Tile tile = board.getTile(i);
            if (tile instanceof TouristSpot) {
                TouristSpot spot = (TouristSpot) tile;
                // 해당 플레이어가 잠근 관광지만 해제
                if (spot.isLocked() && spot.lockedBy != null && spot.lockedBy.intValue() == playerIndex) {
                    unlockTouristSpot(spot);
                }
            }
        }
    }

    /**
     * 관광지 인수 처리
     * 잠금된 관광지는 인수 불가능
     */
    public boolean takeoverTouristSpot(Player buyer, Player seller, TouristSpot spot, int buyerIndex) {
        if (!spot.isOwned()) {
            return false; // 미소유 관광지는 인수 불가 (구매만 가능)
        }

        if (spot.isLocked()) {
            return false; // 잠금된 관광지는 인수 불가
        }

        int takeoverCost = spot.price; // 관광지 인수 가격 = 원래 가격
        if (!buyer.canAfford(takeoverCost)) {
            return false;
        }

        buyer.pay(takeoverCost);
        seller.earn(takeoverCost);
        spot.owner = buyerIndex;

        return true;
    }

    /**
     * 도시 업그레이드 처리
     */
    public boolean upgradeCity(Player player, City city) {
        if (!city.canUpgrade()) {
            return false;
        }

        int upgradeCost = city.getUpgradeCost();
        if (!player.canAfford(upgradeCost)) {
            return false;
        }

        player.pay(upgradeCost);
        city.upgrade();
        return true;
    }

    /**
     * 도시 인수 처리
     * 랜드마크는 인수 불가능
     * 기존 소유자에게 인수 비용(원래 가격 + 모든 업그레이드 비용)을 지불하고 소유권 이전
     */
    public boolean takeoverCity(Player buyer, Player seller, City city, int buyerIndex) {
        if (!city.isOwned()) {
            return false; // 미소유 도시는 인수 불가 (구매만 가능)
        }

        if (city.isLandmark()) {
            return false; // 랜드마크는 인수 불가능
        }

        int takeoverCost = city.getTakeoverPrice();
        if (!buyer.canAfford(takeoverCost)) {
            return false;
        }

        buyer.pay(takeoverCost);
        seller.earn(takeoverCost);
        city.owner = buyerIndex;

        return true;
    }

    /**
     * 올림픽 효과 적용 - 선택한 도시의 통행료를 2배로 만듦
     * (다음 통행료 지불 시 자동으로 해제됨)
     */
    public void applyOlympicBoost(City city) {
        city.hasOlympicBoost = true;
    }

    /**
     * 올림픽 효과 해제
     */
    public void removeOlympicBoost(City city) {
        city.hasOlympicBoost = false;
    }

    /**
     * 국세청 세금 계산 (보유 금액의 10%)
     */
    public int calculateTax(Player player) {
        return (int)(player.cash * taxRate);
    }

    /**
     * 국세청 세금 납부 처리
     * 파산 체크 포함
     */
    public void payTax(Player player) {
        int tax = calculateTax(player);
        player.pay(tax);
        // 세금은 소멸 (어느 플레이어에게도 지급되지 않음)

        if (player.cash < 0) {
            player.bankrupt = true;
        }
    }

    /**
     * 통행료 지불 처리
     * 파산 체크 포함
     */
    public void payToll(Player payer, Player receiver, int toll) {
        payer.pay(toll);
        receiver.earn(toll);

        if (payer.cash < 0) {
            payer.bankrupt = true;
        }
    }

    /**
     * 무인도 처리
     */
    public void sendToIsland(Player player) {
        // 무인도 위치 찾기 (id 18)
        for (int i = 0; i < board.getSize(); i++) {
            if (board.getTile(i).type == Tile.Type.ISLAND) {
                player.pos = i;
                player.jailTurns = islandMaxTurns;
                break;
            }
        }
    }

    /**
     * 무인도 턴 감소
     */
    public void decreaseJailTurns(Player player) {
        if (player.jailTurns > 0) {
            player.jailTurns--;
        }
    }

    /**
     * 더블로 무인도 탈출
     */
    public void escapeIslandWithDouble(Player player) {
        player.jailTurns = 0;
    }

    /**
     * 보석금으로 무인도 탈출
     */
    public boolean escapeIslandWithBail(Player player) {
        if (!player.canAfford(bail)) {
            return false;
        }

        player.pay(bail);
        player.jailTurns = 0;
        return true;
    }

    /**
     * 출발지 통과 시 월급 지급
     */
    public void paySalary(Player player) {
        player.earn(salary);
    }

    /**
     * 찬스 카드 처리
     */
    public void processChance(Player player) {
        player.earn(chanceReward);
    }

    public int getSalary() {
        return salary;
    }

    public int getChanceReward() {
        return chanceReward;
    }

    /**
     * 승리조건 1: 파산 승리
     * 다른 모든 플레이어의 보유금액이 0원 이하인지 확인
     */
    public boolean checkBankruptcyVictory(Player[] players, int playerIndex) {
        for (int i = 0; i < players.length; i++) {
            if (i == playerIndex) continue; // 자신은 제외
            if (players[i].cash > 0 && !players[i].bankrupt) {
                return false; // 한 명이라도 돈이 있으면 승리 아님
            }
        }
        return true;
    }

    /**
     * 승리조건 2: 라인 독점 승리
     * 보드판 4면 중 한 라인에 있는 모든 타일(도시 + 관광지)을 구매한 경우 승리
     * 라인 구성: 하단(0-8), 좌측(9-16), 상단(17-24), 우측(25-31)
     */
    public boolean checkLineMonopolyVictory(int playerIndex) {
        // 4개 라인 정의 (모서리 타일 포함)
        int[][] lines = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8},      // 하단 (Start ~ 무인도)
            {8, 9, 10, 11, 12, 13, 14, 15, 16}, // 좌측 (무인도 ~ 올림픽)
            {16, 17, 18, 19, 20, 21, 22, 23, 24}, // 상단 (올림픽 ~ 세계여행)
            {24, 25, 26, 27, 28, 29, 30, 31, 0}  // 우측 (세계여행 ~ Start)
        };

        for (int[] line : lines) {
            boolean hasMonopoly = true;
            for (int tileId : line) {
                Tile tile = board.getTile(tileId);

                // 도시나 관광지만 소유 확인 (특수 타일은 제외)
                if (tile instanceof City) {
                    City city = (City) tile;
                    if (!city.isOwned() || city.owner != playerIndex) {
                        hasMonopoly = false;
                        break;
                    }
                } else if (tile instanceof TouristSpot) {
                    TouristSpot spot = (TouristSpot) tile;
                    if (!spot.isOwned() || spot.owner != playerIndex) {
                        hasMonopoly = false;
                        break;
                    }
                }
                // 특수 타일(START, ISLAND 등)은 무시
            }

            if (hasMonopoly) {
                return true; // 한 라인이라도 독점하면 승리
            }
        }

        return false;
    }

    /**
     * 승리조건 3: 트리플 독점 승리
     * 보드판의 도시 색상 중 3가지의 컬러를 모두 소유한 경우 승리
     * 가능한 컬러: LIME, GREEN, CYAN, BLUE, LIGHT_PURPLE, PURPLE, BROWN, RED,
     *            SKY_GRADIENT, PINK_GRADIENT (총 10가지)
     */
    public boolean checkTripleColorMonopolyVictory(int playerIndex) {
        String[] allColors = {
            "LIME", "GREEN", "CYAN", "BLUE", "LIGHT_PURPLE",
            "PURPLE", "BROWN", "RED", "SKY_GRADIENT", "PINK_GRADIENT"
        };

        int monopolyCount = 0;

        for (String color : allColors) {
            if (hasColorMonopoly(playerIndex, color)) {
                monopolyCount++;
            }
        }

        return monopolyCount >= 3;
    }

    /**
     * 통합 승리 조건 체크
     * 3가지 조건 중 하나라도 만족하면 승리
     */
    public boolean checkVictory(Player[] players, int playerIndex) {
        // 1. 파산 승리
        if (checkBankruptcyVictory(players, playerIndex)) {
            return true;
        }

        // 2. 라인 독점 승리
        if (checkLineMonopolyVictory(playerIndex)) {
            return true;
        }

        // 3. 트리플 독점 승리
        if (checkTripleColorMonopolyVictory(playerIndex)) {
            return true;
        }

        return false;
    }

    /**
     * 승리 타입 확인 (디버깅/로그용)
     */
    public String getVictoryType(Player[] players, int playerIndex) {
        if (checkBankruptcyVictory(players, playerIndex)) {
            return "파산 승리";
        }
        if (checkLineMonopolyVictory(playerIndex)) {
            return "라인 독점 승리";
        }
        if (checkTripleColorMonopolyVictory(playerIndex)) {
            return "트리플 독점 승리";
        }
        return "승리 조건 미달성";
    }

    /**
     * 듀얼 마그네틱 코어: 양옆 4칸 이내의 거리 계산
     * 삭제된 땅은 카운트에서 제외
     *
     * @param fromPos 시작 위치
     * @param toPos 목표 위치
     * @param clockwise 시계방향 여부
     * @return 실제 거리 (삭제된 도시 제외)
     */
    private int calculateDistanceExcludingDeleted(int fromPos, int toPos, boolean clockwise) {
        int boardSize = board.getSize();
        int distance = 0;
        int currentPos = fromPos;

        while (currentPos != toPos && distance < boardSize) {
            // 다음 위치로 이동
            if (clockwise) {
                currentPos = (currentPos + 1) % boardSize;
            } else {
                currentPos = (currentPos - 1 + boardSize) % boardSize;
            }

            // 삭제되지 않은 타일만 카운트
            Tile tile = board.getTile(currentPos);
            if (!(tile instanceof City) || !((City) tile).isDeleted) {
                distance++;
            }

            // 목표 위치 도달
            if (currentPos == toPos) {
                break;
            }
        }

        return distance;
    }

    /**
     * 듀얼 마그네틱 코어: 양옆 4칸 이내의 플레이어를 끌어당김
     *
     * @param landmarkPos 랜드마크 위치
     * @param players 모든 플레이어 배열
     * @param ownerIndex 랜드마크 소유자 인덱스
     * @return 끌려온 플레이어 수
     */
    public int applyDualMagneticCore(int landmarkPos, Player[] players, int ownerIndex) {
        int pulledCount = 0;
        final int MAGNETIC_RANGE = 4;

        for (int i = 0; i < players.length; i++) {
            // 본인은 제외
            if (i == ownerIndex) {
                continue;
            }

            Player player = players[i];

            // 무인도에 갇힌 플레이어는 면역
            if (player.isInJail()) {
                continue;
            }

            // 파산한 플레이어 제외
            if (player.bankrupt) {
                continue;
            }

            int playerPos = player.pos;

            // 양옆 거리 계산 (삭제된 도시 제외)
            int clockwiseDistance = calculateDistanceExcludingDeleted(landmarkPos, playerPos, true);
            int counterClockwiseDistance = calculateDistanceExcludingDeleted(landmarkPos, playerPos, false);

            // 더 짧은 거리 선택
            int minDistance = Math.min(clockwiseDistance, counterClockwiseDistance);

            // 4칸 이내면 끌어당김
            if (minDistance <= MAGNETIC_RANGE && minDistance > 0) {
                player.pos = landmarkPos;
                pulledCount++;
            }
        }

        return pulledCount;
    }

}
