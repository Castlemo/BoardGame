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
    private final double[] tollMultiplierByLevel = {0.0, 1.0, 2.0, 4.0, 8.0}; // 레벨 0~4
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
     * 삭제된 칸은 독점 판정에서 제외
     *
     * 독점 조건:
     * 1. 해당 컬러 그룹의 남은 활성 칸(삭제되지 않은 칸) 중
     * 2. 모든 칸이 동일한 플레이어 소유여야 함
     * 3. 최소 1칸 이상 소유해야 함 (칸 수와 무관)
     */
    public boolean hasColorMonopoly(int playerIndex, String colorGroup) {
        List<City> cities = board.getCitiesByColor(colorGroup);
        int activeCities = 0;  // 삭제되지 않은 칸 수
        int ownedByPlayer = 0; // 플레이어가 소유한 칸 수

        for (City city : cities) {
            // 삭제된 칸은 독점 판정에서 제외
            if (city.isDeleted) {
                continue;
            }

            activeCities++;

            // 해당 플레이어가 소유한 칸 카운트
            if (city.isOwned() && city.owner == playerIndex) {
                ownedByPlayer++;
            }
        }

        // 활성 칸이 없으면 독점 불가
        if (activeCities == 0) {
            return false;
        }

        // 모든 활성 칸을 소유해야 독점 성립
        return ownedByPlayer == activeCities;
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
     * 관광지(TouristSpot) 통행료 계산 (고정 금액)
     */
    public int calculateTouristSpotToll(TouristSpot touristSpot) {
        return touristSpot.toll;
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
     */
    public void payTax(Player player) {
        int tax = calculateTax(player);
        player.pay(tax);
        // 세금은 소멸 (어느 플레이어에게도 지급되지 않음)
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
     * 페이즈 딜리트: 비어있는 도시 칸 중 하나를 랜덤으로 삭제
     * @return 삭제된 도시 이름 (삭제 실패 시 null)
     */
    public String performPhaseDelete() {
        List<Tile> tiles = board.getAllTiles();
        java.util.List<City> unownedCities = new java.util.ArrayList<>();

        // 비어있는 도시 칸 찾기 (START 제외, 도시만)
        for (Tile tile : tiles) {
            if (tile.type == Tile.Type.CITY) {
                City city = (City) tile;
                if (!city.isOwned() && !city.isDeleted) {
                    unownedCities.add(city);
                }
            }
        }

        // 비어있는 도시가 없으면 null 반환
        if (unownedCities.isEmpty()) {
            return null;
        }

        // 랜덤으로 하나 선택하여 삭제
        int randomIndex = (int)(Math.random() * unownedCities.size());
        City cityToDelete = unownedCities.get(randomIndex);
        cityToDelete.isDeleted = true;
        return cityToDelete.name;
    }
}
