package com.marblegame.model;

/**
 * 플레이어 클래스
 * 이름, 현금, 위치, 무인도 턴, 파산 여부 관리
 */
public class Player {
    public final String name;
    public int cash;
    public int pos = 0;
    public int jailTurns = 0;
    public boolean bankrupt = false;

    public Player(String name, int cash) {
        this.name = name;
        this.cash = cash;
    }

    public void move(int steps, int boardSize, Board board) {
        int stepsRemaining = steps;

        while (stepsRemaining > 0) {
            pos = (pos + 1) % boardSize;

            // 삭제된 칸은 건너뛰기 (카운트하지 않음)
            Tile tile = board.getTile(pos);
            if (tile.type == Tile.Type.CITY) {
                City city = (City) tile;
                if (city.isDeleted) {
                    continue; // 삭제된 칸은 스텝 카운트 안 함
                }
            }

            stepsRemaining--;
        }
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
