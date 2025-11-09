package com.marblegame.model;

/**
 * 관광지 클래스
 * 구매 가능하지만 업그레이드 불가능
 */
public class TouristSpot extends Tile {
    public final int price;
    public final int toll;  // 고정 통행료 (업그레이드 없음)
    public Integer owner = null; // 플레이어 인덱스 (null이면 미소유)
    public boolean locked = false; // 잠금 상태 (true면 다음 내 턴까지 인수 불가)
    public Integer lockedBy = null; // 잠금한 플레이어 인덱스 (null이면 잠금 안됨)

    public TouristSpot(int id, String name, int price, int toll) {
        super(id, name, Type.TOURIST_SPOT, "TOURIST_SPOT");
        this.price = price;
        this.toll = toll;
    }

    public boolean isOwned() {
        return owner != null;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @Override
    public String toString() {
        String ownerInfo = isOwned() ? String.format(" (소유자: P%d)", owner) : " (미소유)";
        return String.format("[%d] %s%s", id, name, ownerInfo);
    }
}
