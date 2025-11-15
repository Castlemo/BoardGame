package com.marblegame.model;

/**
 * 플레이어 클래스
 * 이름, 현금, 위치, 무인도 턴, 파산 여부 관리
 */
public class Player {
    public final String playerId;
    public String name;
    public int cash;
    public int pos = 0;
    public int jailTurns = 0;
    public boolean bankrupt = false;
    public boolean hasRailroadTicket = false; // 전국철도 티켓 (다음 턴에 원하는 칸 선택 가능)
    public boolean hasExtraChance = false; // 관광지 선택지로 획득한 추가 주사위 기회

    public Player(String name, int cash) {
        this(null, name, cash);
    }

    public Player(String playerId, String name, int cash) {
        this.playerId = playerId;
        this.name = name;
        this.cash = cash;
    }

    public void move(int steps, int boardSize, Board board) {
        pos = (pos + steps) % boardSize;
    }

    public boolean canAfford(int amount) {
        return cash >= amount;
    }

    public void pay(int amount) {
        cash -= amount;
    }

    public void earn(int amount) {
        cash += amount;
    }

    public boolean isInJail() {
        return jailTurns > 0;
    }

    @Override
    public String toString() {
        return String.format("%s (현금: %,d원, 위치: %d)", name, cash, pos);
    }
}
