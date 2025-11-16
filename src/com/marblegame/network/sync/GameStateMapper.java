package com.marblegame.network.sync;

import com.marblegame.model.Board;
import com.marblegame.model.City;
import com.marblegame.model.Player;
import com.marblegame.model.Tile;
import com.marblegame.model.TouristSpot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GameStateSnapshot ↔ 모델/메시지 변환 유틸리티
 */
public final class GameStateMapper {
    private GameStateMapper() {
    }

    /**
     * 현재 모델 상태를 스냅샷으로 변환
     */
    public static GameStateSnapshot capture(Board board, Player[] players, int currentPlayerIndex,
                                            int turnCount, String phase, int dice1, int dice2,
                                            boolean isDouble, List<String> availableActions,
                                            GameStateSnapshot.EventState eventState) {
        GameStateSnapshot snapshot = new GameStateSnapshot();
        snapshot.setTurnCount(turnCount);
        snapshot.setCurrentPlayerIndex(currentPlayerIndex);
        snapshot.setPhase(phase);
        snapshot.setAvailableActions(availableActions);
        snapshot.setEventState(eventState);

        GameStateSnapshot.DiceState diceState = new GameStateSnapshot.DiceState();
        diceState.setDice1(dice1);
        diceState.setDice2(dice2);
        diceState.setSum(dice1 + dice2);
        diceState.setDoubleRoll(isDouble);
        snapshot.setDiceState(diceState);

        for (int i = 0; i < players.length; i++) {
            Player player = players[i];
            GameStateSnapshot.PlayerState state = new GameStateSnapshot.PlayerState();
            state.setIndex(i);
            state.setPlayerId(player.playerId);
            state.setName(player.name);
            state.setCash(player.cash);
            state.setPosition(player.pos);
            state.setJailTurns(player.jailTurns);
            state.setBankrupt(player.bankrupt);
            state.setHasRailroadTicket(player.hasRailroadTicket);
            state.setHasExtraChance(player.hasExtraChance);
            snapshot.getPlayers().add(state);
        }

        for (int tileId = 0; tileId < board.getSize(); tileId++) {
            Tile tile = board.getTile(tileId);
            if (tile instanceof City) {
                City city = (City) tile;
                GameStateSnapshot.CityState cityState = new GameStateSnapshot.CityState();
                cityState.setTileId(tile.id);
                cityState.setOwner(city.owner);
                cityState.setLevel(city.level);
                cityState.setOlympicBoost(city.hasOlympicBoost);
                cityState.setDeleted(city.isDeleted);
                snapshot.getCities().add(cityState);
            } else if (tile instanceof TouristSpot) {
                TouristSpot spot = (TouristSpot) tile;
                GameStateSnapshot.TouristSpotState spotState = new GameStateSnapshot.TouristSpotState();
                spotState.setTileId(tile.id);
                spotState.setOwner(spot.owner);
                spotState.setLocked(spot.isLocked());
                spotState.setLockedBy(spot.lockedBy);
                snapshot.getTouristSpots().add(spotState);
            }
        }

        return snapshot;
    }

    /**
     * 스냅샷을 플레이어/보드 모델에 반영
     */
    public static void apply(GameStateSnapshot snapshot, Board board, Player[] players) {
        if (snapshot == null) {
            return;
        }

        for (GameStateSnapshot.PlayerState playerState : snapshot.getPlayers()) {
            int index = playerState.getIndex();
            if (index < 0 || index >= players.length) {
                continue;
            }
            Player player = players[index];
            player.name = playerState.getName();
            player.cash = playerState.getCash();
            player.pos = playerState.getPosition();
            player.jailTurns = playerState.getJailTurns();
            player.bankrupt = playerState.isBankrupt();
            player.hasRailroadTicket = playerState.isHasRailroadTicket();
            player.hasExtraChance = playerState.isHasExtraChance();
        }

        for (GameStateSnapshot.CityState cityState : snapshot.getCities()) {
            if (cityState.getTileId() < 0 || cityState.getTileId() >= board.getSize()) {
                continue;
            }
            Tile tile = board.getTile(cityState.getTileId());
            if (tile instanceof City) {
                City city = (City) tile;
                city.owner = cityState.getOwner();
                city.level = cityState.getLevel();
                city.hasOlympicBoost = cityState.isOlympicBoost();
                city.isDeleted = cityState.isDeleted();
            }
        }

        for (GameStateSnapshot.TouristSpotState spotState : snapshot.getTouristSpots()) {
            if (spotState.getTileId() < 0 || spotState.getTileId() >= board.getSize()) {
                continue;
            }
            Tile tile = board.getTile(spotState.getTileId());
            if (tile instanceof TouristSpot) {
                TouristSpot spot = (TouristSpot) tile;
                spot.owner = spotState.getOwner();
                spot.setLocked(spotState.isLocked());
                spot.lockedBy = spotState.getLockedBy();
            }
        }
    }

    /**
     * 메시지 전송용 Map으로 변환
     */
    public static Map<String, Object> toMap(GameStateSnapshot snapshot) {
        Map<String, Object> map = new HashMap<>();
        map.put("turnCount", snapshot.getTurnCount());
        map.put("currentPlayerIndex", snapshot.getCurrentPlayerIndex());
        map.put("phase", snapshot.getPhase());

        GameStateSnapshot.DiceState dice = snapshot.getDiceState();
        Map<String, Object> diceMap = new HashMap<>();
        diceMap.put("dice1", dice.getDice1());
        diceMap.put("dice2", dice.getDice2());
        diceMap.put("sum", dice.getSum());
        diceMap.put("double", dice.isDoubleRoll());
        map.put("dice", diceMap);

        List<Map<String, Object>> playerList = new ArrayList<>();
        for (GameStateSnapshot.PlayerState playerState : snapshot.getPlayers()) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("index", playerState.getIndex());
            entry.put("playerId", playerState.getPlayerId());
            entry.put("name", playerState.getName());
            entry.put("cash", playerState.getCash());
            entry.put("position", playerState.getPosition());
            entry.put("jailTurns", playerState.getJailTurns());
            entry.put("bankrupt", playerState.isBankrupt());
            entry.put("hasRailroadTicket", playerState.isHasRailroadTicket());
            entry.put("hasExtraChance", playerState.isHasExtraChance());
            playerList.add(entry);
        }
        map.put("players", playerList);

        List<Map<String, Object>> cityList = new ArrayList<>();
        for (GameStateSnapshot.CityState cityState : snapshot.getCities()) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("tileId", cityState.getTileId());
            entry.put("owner", cityState.getOwner());
            entry.put("level", cityState.getLevel());
            entry.put("olympicBoost", cityState.isOlympicBoost());
            entry.put("deleted", cityState.isDeleted());
            cityList.add(entry);
        }
        map.put("cities", cityList);

        List<Map<String, Object>> spotList = new ArrayList<>();
        for (GameStateSnapshot.TouristSpotState spotState : snapshot.getTouristSpots()) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("tileId", spotState.getTileId());
            entry.put("owner", spotState.getOwner());
            entry.put("locked", spotState.isLocked());
            entry.put("lockedBy", spotState.getLockedBy());
            spotList.add(entry);
        }
        map.put("touristSpots", spotList);

        map.put("availableActions", new ArrayList<>(snapshot.getAvailableActions()));

        GameStateSnapshot.EventState eventState = snapshot.getEventState();
        if (eventState != null) {
            Map<String, Object> eventMap = new HashMap<>();
            eventMap.put("id", eventState.getId());
            eventMap.put("type", eventState.getType());
            eventMap.put("data", eventState.getData());
            map.put("event", eventMap);
        }

        return map;
    }

    /**
     * 메시지 데이터 → 스냅샷
     */
    @SuppressWarnings("unchecked")
    public static GameStateSnapshot fromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }

        GameStateSnapshot snapshot = new GameStateSnapshot();
        snapshot.setTurnCount(toInt(map.get("turnCount"), 0));
        snapshot.setCurrentPlayerIndex(toInt(map.get("currentPlayerIndex"), 0));
        snapshot.setPhase(stringValue(map.get("phase")));

        Object diceObj = map.get("dice");
        if (diceObj instanceof Map) {
            Map<String, Object> diceMap = (Map<String, Object>) diceObj;
            GameStateSnapshot.DiceState diceState = new GameStateSnapshot.DiceState();
            diceState.setDice1(toInt(diceMap.get("dice1"), 0));
            diceState.setDice2(toInt(diceMap.get("dice2"), 0));
            diceState.setSum(toInt(diceMap.get("sum"), 0));
            diceState.setDoubleRoll(booleanValue(diceMap.get("double")));
            snapshot.setDiceState(diceState);
        }

        Object playersObj = map.get("players");
        if (playersObj instanceof List) {
            for (Object obj : (List<?>) playersObj) {
                if (obj instanceof Map) {
                    Map<String, Object> entry = (Map<String, Object>) obj;
                    GameStateSnapshot.PlayerState state = new GameStateSnapshot.PlayerState();
                    state.setIndex(toInt(entry.get("index"), snapshot.getPlayers().size()));
                    state.setPlayerId(stringValue(entry.get("playerId")));
                    state.setName(stringValue(entry.get("name")));
                    state.setCash(toInt(entry.get("cash"), 0));
                    state.setPosition(toInt(entry.get("position"), 0));
                    state.setJailTurns(toInt(entry.get("jailTurns"), 0));
                    state.setBankrupt(booleanValue(entry.get("bankrupt")));
                    state.setHasRailroadTicket(booleanValue(entry.get("hasRailroadTicket")));
                    state.setHasExtraChance(booleanValue(entry.get("hasExtraChance")));
                    snapshot.getPlayers().add(state);
                }
            }
        }

        Object citiesObj = map.get("cities");
        if (citiesObj instanceof List) {
            for (Object obj : (List<?>) citiesObj) {
                if (obj instanceof Map) {
                    Map<String, Object> entry = (Map<String, Object>) obj;
                    GameStateSnapshot.CityState state = new GameStateSnapshot.CityState();
                    state.setTileId(toInt(entry.get("tileId"), 0));
                    state.setOwner(toInteger(entry.get("owner")));
                    state.setLevel(toInt(entry.get("level"), 0));
                    state.setOlympicBoost(booleanValue(entry.get("olympicBoost")));
                    state.setDeleted(booleanValue(entry.get("deleted")));
                    snapshot.getCities().add(state);
                }
            }
        }

        Object spotsObj = map.get("touristSpots");
        if (spotsObj instanceof List) {
            for (Object obj : (List<?>) spotsObj) {
                if (obj instanceof Map) {
                    Map<String, Object> entry = (Map<String, Object>) obj;
                    GameStateSnapshot.TouristSpotState state = new GameStateSnapshot.TouristSpotState();
                    state.setTileId(toInt(entry.get("tileId"), 0));
                    state.setOwner(toInteger(entry.get("owner")));
                    state.setLocked(booleanValue(entry.get("locked")));
                    state.setLockedBy(toInteger(entry.get("lockedBy")));
                    snapshot.getTouristSpots().add(state);
                }
            }
        }

        Object actionsObj = map.get("availableActions");
        if (actionsObj instanceof List) {
            List<String> actions = new ArrayList<>();
            for (Object entry : (List<?>) actionsObj) {
                if (entry != null) {
                    actions.add(entry.toString());
                }
            }
            snapshot.setAvailableActions(actions);
        }

        Object eventObj = map.get("event");
        if (eventObj instanceof Map) {
            Map<String, Object> eventMap = (Map<String, Object>) eventObj;
            GameStateSnapshot.EventState eventState = new GameStateSnapshot.EventState();
            eventState.setId(toInt(eventMap.get("id"), 0));
            eventState.setType(stringValue(eventMap.get("type")));
            Object dataObj = eventMap.get("data");
            if (dataObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) dataObj;
                eventState.setData(data);
            }
            snapshot.setEventState(eventState);
        }

        return snapshot;
    }

    private static int toInt(Object value, int defaultValue) {
        Integer result = toInteger(value);
        return result != null ? result : defaultValue;
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean booleanValue(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }

    private static String stringValue(Object value) {
        return value != null ? value.toString() : null;
    }
}
