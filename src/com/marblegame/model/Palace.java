package com.marblegame.model;

/**
 * 관광지(궁) 클래스
 * 구매 가능하지만 업그레이드 불가능
 */
public class Palace extends Tile {
    public final int price;
    public final int toll;  // 고정 통행료 (업그레이드 없음)
    public Integer owner = null; // 플레이어 인덱스 (null이면 미소유)
    public boolean isDeleted = false; // 페이즈 딜리트로 삭제된 칸

    public Palace(int id, String name, int price, int toll) {
        super(id, name, Type.PALACE, "PALACE");
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
