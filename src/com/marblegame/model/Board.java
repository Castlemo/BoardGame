package com.marblegame.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 게임 보드 클래스
 * 32칸 순환형 보드 관리 (9x9 그리드)
 */
public class Board {
    private final List<Tile> tiles;

    public Board() {
        tiles = new ArrayList<>();
        initializeBoard();
    }

    private void initializeBoard() {
        // 32칸 보드: 9x9 그리드 (각 면 8칸)
        // 반시계 방향: Start(우하) → 무인도(좌하) → 올림픽(좌상) → 세계여행(우상) → Start

        // 0: START (우측 하단 모서리)
        tiles.add(new Tile(0, "Start", Tile.Type.START));

        // 1-7: 하단 (Start → 무인도)
        tiles.add(new City(1, "방콕", "LIME", 150000, 75000));
        tiles.add(new Tile(2, "찬스", Tile.Type.CHANCE));
        tiles.add(new City(3, "베이징", "LIME", 150000, 75000));
        tiles.add(new TouristSpot(4, "독도", 200000, 100000));
        tiles.add(new City(5, "타이페이", "GREEN", 180000, 90000));
        tiles.add(new City(6, "두바이", "GREEN", 200000, 100000));
        tiles.add(new City(7, "카이로", "GREEN", 200000, 100000));

        // 8: ISLAND (좌측 하단 모서리)
        tiles.add(new Tile(8, "무인도", Tile.Type.ISLAND));

        // 9-16: 좌측 (무인도 → 올림픽)
        tiles.add(new TouristSpot(9, "발리", 200000, 100000));
        tiles.add(new City(10, "도쿄", "CYAN", 220000, 110000));
        tiles.add(new City(11, "시드니", "CYAN", 220000, 110000));
        tiles.add(new Tile(12, "찬스", Tile.Type.CHANCE));
        tiles.add(new City(13, "퀘벡", "BLUE", 240000, 120000));
        tiles.add(new TouristSpot(14, "하와이", 200000, 100000));
        tiles.add(new City(15, "상파울로", "BLUE", 260000, 130000));

        // 16: OLYMPIC (좌측 상단 모서리)
        tiles.add(new Tile(16, "올림픽", Tile.Type.OLYMPIC));

        // 17-24: 상단 (올림픽 → 세계여행)
        tiles.add(new City(17, "프라하", "LIGHT_PURPLE", 280000, 140000));
        tiles.add(new TouristSpot(18, "푸켓", 200000, 100000));
        tiles.add(new City(19, "베를린", "LIGHT_PURPLE", 300000, 150000));
        tiles.add(new Tile(20, "찬스", Tile.Type.CHANCE));
        tiles.add(new City(21, "모스크바", "PURPLE", 320000, 160000));
        tiles.add(new City(22, "제네바", "PURPLE", 340000, 170000));
        tiles.add(new City(23, "로마", "PURPLE", 340000, 170000));

        // 24: WORLD_TOUR (우측 상단 모서리)
        tiles.add(new Tile(24, "세계여행", Tile.Type.WORLD_TOUR));

        // 25-31: 우측 (세계여행 → Start)
        tiles.add(new TouristSpot(25, "타히티", 200000, 100000));
        tiles.add(new City(26, "런던", "BROWN", 380000, 190000));
        tiles.add(new City(27, "파리", "BROWN", 380000, 190000));
        tiles.add(new Tile(28, "찬스", Tile.Type.CHANCE));
        tiles.add(new City(29, "뉴욕", "RED", 400000, 200000));
        tiles.add(new Tile(30, "국세청", Tile.Type.TAX));
        tiles.add(new City(31, "서울", "RED", 400000, 200000));
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
