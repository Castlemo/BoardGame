package com.marblegame.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 게임 보드 클래스
 * 28칸 순환형 보드 관리
 */
public class Board {
    private final List<Tile> tiles;

    public Board() {
        tiles = new ArrayList<>();
        initializeBoard();
    }

    private void initializeBoard() {
        // 0: START
        tiles.add(new Tile(0, "출발", Tile.Type.START));

        // 1-5: 빨강 그룹 (저가 구역)
        tiles.add(new City(1, "서울", "RED", 100000, 50000));
        tiles.add(new City(2, "부산", "RED", 100000, 50000));
        tiles.add(new City(3, "대구", "RED", 120000, 60000));
        tiles.add(new City(4, "인천", "RED", 120000, 60000));
        tiles.add(new City(5, "광주", "RED", 150000, 75000));

        // 6: CHANCE
        tiles.add(new Tile(6, "찬스", Tile.Type.CHANCE));

        // 7-11: 파랑 그룹 (중저가 구역)
        tiles.add(new City(7, "대전", "BLUE", 180000, 90000));
        tiles.add(new City(8, "울산", "BLUE", 180000, 90000));
        tiles.add(new City(9, "수원", "BLUE", 200000, 100000));
        tiles.add(new City(10, "창원", "BLUE", 200000, 100000));
        tiles.add(new City(11, "성남", "BLUE", 220000, 110000));

        // 12: CHANCE
        tiles.add(new Tile(12, "찬스", Tile.Type.CHANCE));

        // 13-17: 초록 그룹 (중고가 구역)
        tiles.add(new City(13, "용인", "GREEN", 250000, 125000));
        tiles.add(new City(14, "고양", "GREEN", 250000, 125000));
        tiles.add(new City(15, "청주", "GREEN", 280000, 140000));
        tiles.add(new City(16, "천안", "GREEN", 280000, 140000));
        tiles.add(new City(17, "전주", "GREEN", 300000, 150000));

        // 18: ISLAND
        tiles.add(new Tile(18, "무인도", Tile.Type.ISLAND));

        // 19-25: 노랑 그룹 (고가 구역)
        tiles.add(new City(19, "안산", "YELLOW", 350000, 175000));
        tiles.add(new City(20, "김해", "YELLOW", 350000, 175000));
        tiles.add(new City(21, "평택", "YELLOW", 380000, 190000));
        tiles.add(new City(22, "제주", "YELLOW", 380000, 190000));
        tiles.add(new City(23, "강릉", "YELLOW", 400000, 200000));

        // 24, 26, 27: CHANCE
        tiles.add(new Tile(24, "찬스", Tile.Type.CHANCE));
        tiles.add(new City(25, "속초", "YELLOW", 400000, 200000));
        tiles.add(new Tile(26, "찬스", Tile.Type.CHANCE));
        tiles.add(new Tile(27, "찬스", Tile.Type.CHANCE));
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
