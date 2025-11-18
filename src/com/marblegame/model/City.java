package com.marblegame.model;

/**
 * 도시(매입 가능한 땅) 클래스
 * Tile을 상속받아 가격, 통행료, 소유자, 레벨 관리
 * 레벨: 0=미소유, 1=집(L1), 2=아파트(L2), 3=건물(L3), 4=랜드마크(L4)(금테두리)
 */
public class City extends Tile {
    public final int price;
    public final int baseToll;
    public int level = 0; // 0: 미소유, 1: 집, 2: 아파트, 3: 건물, 4: 랜드마크
    public Integer owner = null; // 플레이어 인덱스 (null이면 미소유)
    public boolean hasOlympicBoost = false; // 올림픽 효과로 통행료 2배
    public boolean isDeleted = false; // 페이즈 딜리트로 삭제된 도시

    public City(int id, String name, String colorGroup, int price, int baseToll) {
        super(id, name, Type.CITY, colorGroup);
        this.price = price;
        this.baseToll = baseToll;
    }

    public boolean isOwned() {
        return owner != null;
    }

    /**
     * 업그레이드 비용 계산
     * 레벨 1->2: 매입가의 30%
     * 레벨 2->3: 매입가의 30%
     * 레벨 3->4: 매입가의 40%
     */
    public int getUpgradeCost() {
        if (level == 0) return price; // 미소유시 구매 가격
        if (level == 3) return (int)(price * 0.4); // 랜드마크 업그레이드
        return (int)(price * 0.3); // 일반 업그레이드
    }

    /**
     * 업그레이드 가능 여부
     */
    public boolean canUpgrade() {
        return level < 4;
    }

    /**
     * 업그레이드 실행
     */
    public void upgrade() {
        if (canUpgrade()) {
            level++;
        }
    }

    /**
     * 인수 가격 계산
     * 공식: 기본가 + (레벨 단계 누적 × 기본가의 50%)
     * L1: 기본가 × 1.5
     * L2: 기본가 × 2.0 (1+1 단계)
     * L3: 기본가 × 2.5 (1+1+1 단계)
     * L4: 기본가 × 3.0 (1+1+1+1 단계)
     */
    public int getTakeoverPrice() {
        int levelSteps = level; // L1=1, L2=2, L3=3, L4=4
        return (int)(price * (1.0 + levelSteps * 0.5));
    }

    /**
     * 랜드마크 여부 확인
     */
    public boolean isLandmark() {
        return level == 4;
    }

    /**
     * 건물 레벨 아이콘 반환
     */
    public String getBuildingEmoji() {
        switch (level) {
            case 1: return "L1"; // 집
            case 2: return "L2"; // 아파트
            case 3: return "L3"; // 건물
            case 4: return "L4"; // 랜드마크
            default: return "";
        }
    }

    @Override
    public String toString() {
        String ownerInfo = isOwned() ? String.format(" (소유자: P%d, Lv%d)", owner, level) : " (미소유)";
        return String.format("[%d] %s%s", id, name, ownerInfo);
    }
}
