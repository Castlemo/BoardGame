package com.marblegame.model;

/**
 * 도시(매입 가능한 땅) 클래스
 * Tile을 상속받아 가격, 통행료, 소유자, 레벨 관리
 */
public class City extends Tile {
    public final int price;
    public final int baseToll;
    public int level = 0;
    public Integer owner = null; // 플레이어 인덱스 (null이면 미소유)
    public boolean isDeleted = false; // 페이즈 딜리트로 삭제된 칸

    public City(int id, String name, String colorGroup, int price, int baseToll) {
        super(id, name, Type.CITY, colorGroup);
        this.price = price;
        this.baseToll = baseToll;
    }

    public boolean isOwned() {
        return owner != null;
    }

    public int getUpgradeCost() {
        return (int)(price * 0.3); // 매입가의 30%
    }

    public boolean canUpgrade() {
        return level < 3;
    }

    public void upgrade() {
        if (canUpgrade()) {
            level++;
        }
    }

    @Override
    public String toString() {
        String ownerInfo = isOwned() ? String.format(" (소유자: P%d, Lv%d)", owner, level) : " (미소유)";
        return String.format("[%d] %s%s", id, name, ownerInfo);
    }
}
