package com.marblegame.model;

/**
 * 기본 필드 클래스
 */
public class Tile {
    public enum Type {
        START, CITY, ISLAND, CHANCE
    }

    public final int id;
    public final String name;
    public final Type type;
    public final String colorGroup;

    public Tile(int id, String name, Type type, String colorGroup) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.colorGroup = colorGroup;
    }

    public Tile(int id, String name, Type type) {
        this(id, name, type, null);
    }

    @Override
    public String toString() {
        return String.format("[%d] %s", id, name);
    }
}
