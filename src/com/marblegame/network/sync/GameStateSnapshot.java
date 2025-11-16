package com.marblegame.network.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 전체 게임 상태 스냅샷 DTO
 * 네트워크를 통해 플레이어/보드 상태를 동기화할 때 사용한다.
 */
public class GameStateSnapshot {
    private int turnCount;
    private int currentPlayerIndex;
    private String phase;
    private DiceState diceState = new DiceState();
    private final List<PlayerState> players = new ArrayList<>();
    private final List<CityState> cities = new ArrayList<>();
    private final List<TouristSpotState> touristSpots = new ArrayList<>();
    private final List<String> availableActions = new ArrayList<>();
    private int eventSequence;
    private EventState eventState;

    public int getTurnCount() {
        return turnCount;
    }

    public void setTurnCount(int turnCount) {
        this.turnCount = turnCount;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public void setCurrentPlayerIndex(int currentPlayerIndex) {
        this.currentPlayerIndex = currentPlayerIndex;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public DiceState getDiceState() {
        return diceState;
    }

    public void setDiceState(DiceState diceState) {
        this.diceState = diceState;
    }

    public List<PlayerState> getPlayers() {
        return players;
    }

    public List<CityState> getCities() {
        return cities;
    }

    public List<TouristSpotState> getTouristSpots() {
        return touristSpots;
    }

    public List<String> getAvailableActions() {
        return availableActions;
    }

    public void setAvailableActions(List<String> actions) {
        availableActions.clear();
        if (actions != null) {
            availableActions.addAll(actions);
        }
    }

    public EventState getEventState() {
        return eventState;
    }

    public void setEventState(EventState eventState) {
        this.eventState = eventState;
    }

    public int getEventSequence() {
        return eventSequence;
    }

    public void setEventSequence(int eventSequence) {
        this.eventSequence = eventSequence;
    }

    /**
     * 주사위 상태
     */
    public static class DiceState {
        private int dice1;
        private int dice2;
        private int sum;
        private boolean doubleRoll;

        public int getDice1() {
            return dice1;
        }

        public void setDice1(int dice1) {
            this.dice1 = dice1;
        }

        public int getDice2() {
            return dice2;
        }

        public void setDice2(int dice2) {
            this.dice2 = dice2;
        }

        public int getSum() {
            return sum;
        }

        public void setSum(int sum) {
            this.sum = sum;
        }

        public boolean isDoubleRoll() {
            return doubleRoll;
        }

        public void setDoubleRoll(boolean doubleRoll) {
            this.doubleRoll = doubleRoll;
        }
    }

    /**
     * 플레이어 상태
     */
    public static class PlayerState {
        private int index;
        private String playerId;
        private String name;
        private int cash;
        private int position;
        private int jailTurns;
        private boolean bankrupt;
        private boolean hasRailroadTicket;
        private boolean hasExtraChance;

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getPlayerId() {
            return playerId;
        }

        public void setPlayerId(String playerId) {
            this.playerId = playerId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getCash() {
            return cash;
        }

        public void setCash(int cash) {
            this.cash = cash;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public int getJailTurns() {
            return jailTurns;
        }

        public void setJailTurns(int jailTurns) {
            this.jailTurns = jailTurns;
        }

        public boolean isBankrupt() {
            return bankrupt;
        }

        public void setBankrupt(boolean bankrupt) {
            this.bankrupt = bankrupt;
        }

        public boolean isHasRailroadTicket() {
            return hasRailroadTicket;
        }

        public void setHasRailroadTicket(boolean hasRailroadTicket) {
            this.hasRailroadTicket = hasRailroadTicket;
        }

        public boolean isHasExtraChance() {
            return hasExtraChance;
        }

        public void setHasExtraChance(boolean hasExtraChance) {
            this.hasExtraChance = hasExtraChance;
        }
    }

    /**
     * 도시 상태
     */
    public static class CityState {
        private int tileId;
        private Integer owner;
        private int level;
        private boolean olympicBoost;
        private boolean deleted;

        public int getTileId() {
            return tileId;
        }

        public void setTileId(int tileId) {
            this.tileId = tileId;
        }

        public Integer getOwner() {
            return owner;
        }

        public void setOwner(Integer owner) {
            this.owner = owner;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public boolean isOlympicBoost() {
            return olympicBoost;
        }

        public void setOlympicBoost(boolean olympicBoost) {
            this.olympicBoost = olympicBoost;
        }

        public boolean isDeleted() {
            return deleted;
        }

        public void setDeleted(boolean deleted) {
            this.deleted = deleted;
        }
    }

    /**
     * 관광지 상태
     */
    public static class TouristSpotState {
        private int tileId;
        private Integer owner;
        private boolean locked;
        private Integer lockedBy;

        public int getTileId() {
            return tileId;
        }

        public void setTileId(int tileId) {
            this.tileId = tileId;
        }

        public Integer getOwner() {
            return owner;
        }

        public void setOwner(Integer owner) {
            this.owner = owner;
        }

        public boolean isLocked() {
            return locked;
        }

        public void setLocked(boolean locked) {
            this.locked = locked;
        }

        public Integer getLockedBy() {
            return lockedBy;
        }

        public void setLockedBy(Integer lockedBy) {
            this.lockedBy = lockedBy;
        }
    }

    /**
     * 최근 발생한 이벤트 정보
     */
    public static class EventState {
        private int id;
        private String type;
        private Map<String, Object> data = new HashMap<>();

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, Object> getData() {
            return data;
        }

        public void setData(Map<String, Object> data) {
            this.data = data != null ? new HashMap<>(data) : new HashMap<>();
        }
    }
}
