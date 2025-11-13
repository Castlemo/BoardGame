package com.marblegame.network.snapshot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 호스트의 현재 게임 상태를 클라이언트로 전송하기 위한 스냅샷 DTO.
 */
public class GameSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    public int turnNumber;
    public int currentPlayerIndex;
    public int diceRollSequence;
    public int dice1;
    public int dice2;
    public boolean oddModeSelected;
    public boolean evenModeSelected;
    public boolean tileSelectionEnabled;

    public ButtonState buttons = new ButtonState();
    public final List<PlayerState> players = new ArrayList<>();
    public final List<CityState> cities = new ArrayList<>();
    public final List<TouristSpotState> touristSpots = new ArrayList<>();

    public static class PlayerState implements Serializable {
        private static final long serialVersionUID = 1L;

        public String name;
        public int cash;
        public int position;
        public int jailTurns;
        public boolean bankrupt;
        public boolean hasRailroadTicket;
        public boolean hasExtraChance;
    }

    public static class CityState implements Serializable {
        private static final long serialVersionUID = 1L;

        public int tileId;
        public Integer owner;
        public int level;
        public boolean hasOlympicBoost;
        public boolean deleted;
    }

    public static class TouristSpotState implements Serializable {
        private static final long serialVersionUID = 1L;

        public int tileId;
        public Integer owner;
        public boolean locked;
        public Integer lockedBy;
    }

    public static class ButtonState implements Serializable {
        private static final long serialVersionUID = 1L;

        public boolean roll;
        public boolean purchase;
        public boolean upgrade;
        public boolean takeover;
        public boolean skip;
        public boolean escape;
    }
}
