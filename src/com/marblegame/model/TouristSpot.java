package com.marblegame.model;

/**
 * 관광지 클래스
 * 구매 가능하지만 업그레이드 불가능
 */
public class TouristSpot extends Tile {
    public final int price;
    public final int toll;  // 고정 통행료 (업그레이드 없음)
    public Integer owner = null; // 플레이어 인덱스 (null이면 미소유)

    public TouristSpot(int id, String name, int price, int toll) {
        super(id, name, Type.TOURIST_SPOT, "TOURIST_SPOT");
        this.price = price;
        this.toll = toll;
    }

    public boolean isOwned() {
        return owner != null;
    }

    @Override
    public String toString() {
        String ownerInfo = isOwned() ? String.format(" (소유자: P%d)", owner) : " (미소유)";
        return String.format("[%d] %s%s", id, name, ownerInfo);
    }
}
