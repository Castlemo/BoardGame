package com.marblegame.model;

/**
 * 2D6 주사위 클래스
 */
public class Dice {
    private int d1;
    private int d2;

    public void roll() {
        d1 = 1 + (int)(Math.random() * 6);
        d2 = 1 + (int)(Math.random() * 6);
    }

    public int sum() {
        return d1 + d2;
    }

    public int getD1() {
        return d1;
    }

    public int getD2() {
        return d2;
    }

    @Override
    public String toString() {
        return String.format("[%d, %d]", d1, d2);
    }
}
