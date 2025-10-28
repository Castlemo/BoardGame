package com.marblegame.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 게임 보드 클래스
 * 44칸 순환형 보드 관리
 */
public class Board {
    private final List<Tile> tiles;

    public Board() {
        tiles = new ArrayList<>();
        initializeBoard();
    }

    private void initializeBoard() {
        // 44칸 보드: 4개 모서리 + 각 면 10칸
        // 반시계 방향: 출발(우하) → 무인도(좌하) → 복지기금(좌상) → 전국철도(우상) → 출발

        // 0: START (우측 하단 모서리)
        tiles.add(new Tile(0, "출발", Tile.Type.START));

        // 1-10: 하단 (출발 → 무인도)
        // City 4개 + 궁 1개 + City 2개 + 찬스 1개 + City 2개
        tiles.add(new City(1, "서울", "RED", 100000, 50000));
        tiles.add(new City(2, "부산", "RED", 100000, 50000));
        tiles.add(new City(3, "대구", "RED", 120000, 60000));
        tiles.add(new City(4, "인천", "RED", 120000, 60000));
        tiles.add(new Palace(5, "경복궁", 500000, 100000));
        tiles.add(new City(6, "광주", "RED", 150000, 75000));
        tiles.add(new City(7, "대전", "RED", 150000, 75000));
        tiles.add(new Tile(8, "찬스", Tile.Type.CHANCE));
        tiles.add(new City(9, "울산", "RED", 150000, 75000));
        tiles.add(new City(10, "수원", "RED", 150000, 75000));

        // 11: ISLAND (좌측 하단 모서리)
        tiles.add(new Tile(11, "무인도", Tile.Type.ISLAND));

        // 12-21: 좌측 (무인도 → 복지기금)
        // City 4개 + 궁 1개 + City 2개 + 찬스 1개 + City 2개
        tiles.add(new City(12, "창원", "BLUE", 180000, 90000));
        tiles.add(new City(13, "성남", "BLUE", 180000, 90000));
        tiles.add(new City(14, "용인", "BLUE", 200000, 100000));
        tiles.add(new City(15, "고양", "BLUE", 200000, 100000));
        tiles.add(new Palace(16, "창경궁", 500000, 100000));
        tiles.add(new City(17, "청주", "BLUE", 220000, 110000));
        tiles.add(new City(18, "천안", "BLUE", 220000, 110000));
        tiles.add(new Tile(19, "찬스", Tile.Type.CHANCE));
        tiles.add(new City(20, "전주", "BLUE", 220000, 110000));
        tiles.add(new City(21, "포항", "BLUE", 220000, 110000));

        // 22: WELFARE (좌측 상단 모서리)
        tiles.add(new Tile(22, "사회복지기금", Tile.Type.WELFARE));

        // 23-32: 상단 (복지기금 → 전국철도)
        // City 4개 + 궁 1개 + City 2개 + 찬스 1개 + City 2개
        tiles.add(new City(23, "안산", "GREEN", 250000, 125000));
        tiles.add(new City(24, "김해", "GREEN", 250000, 125000));
        tiles.add(new City(25, "평택", "GREEN", 280000, 140000));
        tiles.add(new City(26, "제주", "GREEN", 280000, 140000));
        tiles.add(new Palace(27, "덕수궁", 500000, 100000));
        tiles.add(new City(28, "강릉", "GREEN", 300000, 150000));
        tiles.add(new City(29, "속초", "GREEN", 300000, 150000));
        tiles.add(new Tile(30, "찬스", Tile.Type.CHANCE));
        tiles.add(new City(31, "춘천", "GREEN", 300000, 150000));
        tiles.add(new City(32, "원주", "GREEN", 300000, 150000));

        // 33: RAILROAD (우측 상단 모서리)
        tiles.add(new Tile(33, "전국철도", Tile.Type.RAILROAD));

        // 34-43: 우측 (전국철도 → 출발)
        // City 4개 + 궁 1개 + City 2개 + 찬스 1개 + City 2개
        tiles.add(new City(34, "여수", "YELLOW", 350000, 175000));
        tiles.add(new City(35, "순천", "YELLOW", 350000, 175000));
        tiles.add(new City(36, "목포", "YELLOW", 380000, 190000));
        tiles.add(new City(37, "광양", "YELLOW", 380000, 190000));
        tiles.add(new Palace(38, "경희궁", 500000, 100000));
        tiles.add(new City(39, "통영", "YELLOW", 400000, 200000));
        tiles.add(new City(40, "거제", "YELLOW", 400000, 200000));
        tiles.add(new Tile(41, "찬스", Tile.Type.CHANCE));
        tiles.add(new City(42, "양산", "YELLOW", 400000, 200000));
        tiles.add(new City(43, "김천", "YELLOW", 400000, 200000));
    }

    public Tile getTile(int index) {
        return tiles.get(index);
    }

    public int getSize() {
        return tiles.size();
    }

    public List<Tile> getAllTiles() {
        return new ArrayList<>(tiles);
    }

    public List<City> getCitiesByColor(String colorGroup) {
        List<City> result = new ArrayList<>();
        for (Tile tile : tiles) {
            if (tile instanceof City && colorGroup.equals(tile.colorGroup)) {
                result.add((City) tile);
            }
        }
        return result;
    }
}
