package com.marblegame.network.snapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GameSnapshot 직렬화/역직렬화 도우미.
 * JSON 기반의 명시적 포맷을 사용하여 Java 기본 직렬화에 대한 의존성을 제거한다.
 */
public final class GameSnapshotSerializer {
    private GameSnapshotSerializer() {}

    public static String serialize(GameSnapshot snapshot) {
        if (snapshot == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(2048);
        sb.append('{');
        boolean first = true;
        first = appendNumberField(sb, first, "turnNumber", snapshot.turnNumber);
        first = appendNumberField(sb, first, "currentPlayerIndex", snapshot.currentPlayerIndex);
        first = appendNumberField(sb, first, "diceRollSequence", snapshot.diceRollSequence);
        first = appendNumberField(sb, first, "dice1", snapshot.dice1);
        first = appendNumberField(sb, first, "dice2", snapshot.dice2);
        first = appendBooleanField(sb, first, "oddModeSelected", snapshot.oddModeSelected);
        first = appendBooleanField(sb, first, "evenModeSelected", snapshot.evenModeSelected);
        first = appendBooleanField(sb, first, "tileSelectionEnabled", snapshot.tileSelectionEnabled);
        first = appendObjectField(sb, first, "buttons", buildButtonsJson(snapshot.buttons));
        first = appendArrayField(sb, first, "players", buildPlayersJson(snapshot.players));
        first = appendArrayField(sb, first, "cities", buildCitiesJson(snapshot.cities));
        appendArrayField(sb, first, "touristSpots", buildTouristSpotsJson(snapshot.touristSpots));
        sb.append('}');
        return sb.toString();
    }

    public static GameSnapshot deserialize(String payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        JsonParser parser = new JsonParser(payload);
        Object root = parser.parseValue();
        parser.ensureFullyParsed();
        if (!(root instanceof Map)) {
            throw new IllegalArgumentException("루트 JSON 객체가 아닙니다.");
        }
        Map<?, ?> map = (Map<?, ?>) root;
        GameSnapshot snapshot = new GameSnapshot();
        snapshot.turnNumber = asInt(map.get("turnNumber"));
        snapshot.currentPlayerIndex = asInt(map.get("currentPlayerIndex"));
        snapshot.diceRollSequence = asInt(map.get("diceRollSequence"));
        snapshot.dice1 = asInt(map.get("dice1"));
        snapshot.dice2 = asInt(map.get("dice2"));
        snapshot.oddModeSelected = asBoolean(map.get("oddModeSelected"));
        snapshot.evenModeSelected = asBoolean(map.get("evenModeSelected"));
        snapshot.tileSelectionEnabled = asBoolean(map.get("tileSelectionEnabled"));

        Object buttonsObj = map.get("buttons");
        if (buttonsObj instanceof Map) {
            snapshot.buttons = parseButtons((Map<?, ?>) buttonsObj);
        }

        Object playersObj = map.get("players");
        if (playersObj instanceof List) {
            parsePlayers(snapshot, (List<?>) playersObj);
        }

        Object citiesObj = map.get("cities");
        if (citiesObj instanceof List) {
            parseCities(snapshot, (List<?>) citiesObj);
        }

        Object touristObj = map.get("touristSpots");
        if (touristObj instanceof List) {
            parseTourist(snapshot, (List<?>) touristObj);
        }

        return snapshot;
    }

    private static boolean appendNumberField(StringBuilder sb, boolean first, String name, int value) {
        return appendRawField(sb, first, name, Integer.toString(value));
    }

    private static boolean appendBooleanField(StringBuilder sb, boolean first, String name, boolean value) {
        return appendRawField(sb, first, name, value ? "true" : "false");
    }

    private static boolean appendObjectField(StringBuilder sb, boolean first, String name, String objectJson) {
        return appendRawField(sb, first, name, objectJson);
    }

    private static boolean appendArrayField(StringBuilder sb, boolean first, String name, String arrayJson) {
        return appendRawField(sb, first, name, arrayJson);
    }

    private static boolean appendStringField(StringBuilder sb, boolean first, String name, String value) {
        return appendRawField(sb, first, name, value == null ? "null" : quote(value));
    }

    private static boolean appendRawField(StringBuilder sb, boolean first, String name, String rawValue) {
        if (!first) {
            sb.append(',');
        } else {
            first = false;
        }
        sb.append('"').append(name).append('"').append(':').append(rawValue);
        return first;
    }

    private static String buildButtonsJson(GameSnapshot.ButtonState buttons) {
        if (buttons == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        first = appendBooleanField(sb, first, "roll", buttons.roll);
        first = appendBooleanField(sb, first, "purchase", buttons.purchase);
        first = appendBooleanField(sb, first, "upgrade", buttons.upgrade);
        first = appendBooleanField(sb, first, "takeover", buttons.takeover);
        first = appendBooleanField(sb, first, "skip", buttons.skip);
        appendBooleanField(sb, first, "escape", buttons.escape);
        sb.append('}');
        return sb.toString();
    }

    private static String buildPlayersJson(List<GameSnapshot.PlayerState> players) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean firstPlayer = true;
        if (players != null) {
            for (GameSnapshot.PlayerState ps : players) {
                if (!firstPlayer) {
                    sb.append(',');
                } else {
                    firstPlayer = false;
                }
                sb.append('{');
                boolean first = true;
                first = appendStringField(sb, first, "name", ps.name);
                first = appendNumberField(sb, first, "cash", ps.cash);
                first = appendNumberField(sb, first, "position", ps.position);
                first = appendNumberField(sb, first, "jailTurns", ps.jailTurns);
                first = appendBooleanField(sb, first, "bankrupt", ps.bankrupt);
                first = appendBooleanField(sb, first, "hasRailroadTicket", ps.hasRailroadTicket);
                appendBooleanField(sb, first, "hasExtraChance", ps.hasExtraChance);
                sb.append('}');
            }
        }
        sb.append(']');
        return sb.toString();
    }

    private static String buildCitiesJson(List<GameSnapshot.CityState> cities) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean firstCity = true;
        if (cities != null) {
            for (GameSnapshot.CityState cs : cities) {
                if (!firstCity) {
                    sb.append(',');
                } else {
                    firstCity = false;
                }
                sb.append('{');
                boolean first = true;
                first = appendNumberField(sb, first, "tileId", cs.tileId);
                first = appendNullableNumberField(sb, first, "owner", cs.owner);
                first = appendNumberField(sb, first, "level", cs.level);
                first = appendBooleanField(sb, first, "hasOlympicBoost", cs.hasOlympicBoost);
                appendBooleanField(sb, first, "deleted", cs.deleted);
                sb.append('}');
            }
        }
        sb.append(']');
        return sb.toString();
    }

    private static String buildTouristSpotsJson(List<GameSnapshot.TouristSpotState> spots) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean firstSpot = true;
        if (spots != null) {
            for (GameSnapshot.TouristSpotState ts : spots) {
                if (!firstSpot) {
                    sb.append(',');
                } else {
                    firstSpot = false;
                }
                sb.append('{');
                boolean first = true;
                first = appendNumberField(sb, first, "tileId", ts.tileId);
                first = appendNullableNumberField(sb, first, "owner", ts.owner);
                first = appendBooleanField(sb, first, "locked", ts.locked);
                appendNullableNumberField(sb, first, "lockedBy", ts.lockedBy);
                sb.append('}');
            }
        }
        sb.append(']');
        return sb.toString();
    }

    private static boolean appendNullableNumberField(StringBuilder sb, boolean first, String name, Integer value) {
        return appendRawField(sb, first, name, value == null ? "null" : Integer.toString(value));
    }

    private static String quote(String value) {
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\':
                case '"':
                    sb.append('\\').append(c);
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private static GameSnapshot.ButtonState parseButtons(Map<?, ?> map) {
        GameSnapshot.ButtonState buttons = new GameSnapshot.ButtonState();
        buttons.roll = asBoolean(map.get("roll"));
        buttons.purchase = asBoolean(map.get("purchase"));
        buttons.upgrade = asBoolean(map.get("upgrade"));
        buttons.takeover = asBoolean(map.get("takeover"));
        buttons.skip = asBoolean(map.get("skip"));
        buttons.escape = asBoolean(map.get("escape"));
        return buttons;
    }

    private static void parsePlayers(GameSnapshot snapshot, List<?> players) {
        for (Object obj : players) {
            if (!(obj instanceof Map)) {
                continue;
            }
            Map<?, ?> map = (Map<?, ?>) obj;
            GameSnapshot.PlayerState ps = new GameSnapshot.PlayerState();
            ps.name = asString(map.get("name"));
            ps.cash = asInt(map.get("cash"));
            ps.position = asInt(map.get("position"));
            ps.jailTurns = asInt(map.get("jailTurns"));
            ps.bankrupt = asBoolean(map.get("bankrupt"));
            ps.hasRailroadTicket = asBoolean(map.get("hasRailroadTicket"));
            ps.hasExtraChance = asBoolean(map.get("hasExtraChance"));
            snapshot.players.add(ps);
        }
    }

    private static void parseCities(GameSnapshot snapshot, List<?> cities) {
        for (Object obj : cities) {
            if (!(obj instanceof Map)) {
                continue;
            }
            Map<?, ?> map = (Map<?, ?>) obj;
            GameSnapshot.CityState cs = new GameSnapshot.CityState();
            cs.tileId = asInt(map.get("tileId"));
            cs.owner = asNullableInt(map.get("owner"));
            cs.level = asInt(map.get("level"));
            cs.hasOlympicBoost = asBoolean(map.get("hasOlympicBoost"));
            cs.deleted = asBoolean(map.get("deleted"));
            snapshot.cities.add(cs);
        }
    }

    private static void parseTourist(GameSnapshot snapshot, List<?> spots) {
        for (Object obj : spots) {
            if (!(obj instanceof Map)) {
                continue;
            }
            Map<?, ?> map = (Map<?, ?>) obj;
            GameSnapshot.TouristSpotState ts = new GameSnapshot.TouristSpotState();
            ts.tileId = asInt(map.get("tileId"));
            ts.owner = asNullableInt(map.get("owner"));
            ts.locked = asBoolean(map.get("locked"));
            ts.lockedBy = asNullableInt(map.get("lockedBy"));
            snapshot.touristSpots.add(ts);
        }
    }

    private static int asInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String && !((String) value).isEmpty()) {
            return Integer.parseInt((String) value);
        }
        return 0;
    }

    private static Integer asNullableInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String && !((String) value).isEmpty()) {
            return Integer.parseInt((String) value);
        }
        return null;
    }

    private static boolean asBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    /**
     * 최소 JSON 파서. 객체/배열/문자열/숫자/불리언/null만 지원한다.
     */
    private static final class JsonParser {
        private final String json;
        private int pos = 0;

        JsonParser(String json) {
            this.json = json;
        }

        Object parseValue() {
            skipWhitespace();
            if (pos >= json.length()) {
                throw new IllegalArgumentException("예기치 않은 JSON 끝");
            }
            char c = json.charAt(pos);
            switch (c) {
                case '{':
                    return parseObject();
                case '[':
                    return parseArray();
                case '"':
                    return parseString();
                case 't':
                case 'f':
                    return parseBoolean();
                case 'n':
                    return parseNull();
                default:
                    if (c == '-' || (c >= '0' && c <= '9')) {
                        return parseNumber();
                    }
                    throw new IllegalArgumentException("지원하지 않는 토큰: " + c);
            }
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (peek(',')) {
                    pos++;
                    continue;
                }
                if (peek('}')) {
                    pos++;
                    break;
                }
                throw new IllegalArgumentException("객체 구문 오류");
            }
            return map;
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                pos++;
                return list;
            }
            while (true) {
                Object value = parseValue();
                list.add(value);
                skipWhitespace();
                if (peek(',')) {
                    pos++;
                    continue;
                }
                if (peek(']')) {
                    pos++;
                    break;
                }
                throw new IllegalArgumentException("배열 구문 오류");
            }
            return list;
        }

        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (pos < json.length()) {
                char c = json.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (pos >= json.length()) {
                        throw new IllegalArgumentException("잘못된 이스케이프");
                    }
                    char esc = json.charAt(pos++);
                    switch (esc) {
                        case '"':
                        case '\\':
                        case '/':
                            sb.append(esc);
                            break;
                        case 'b':
                            sb.append('\b');
                            break;
                        case 'f':
                            sb.append('\f');
                            break;
                        case 'n':
                            sb.append('\n');
                            break;
                        case 'r':
                            sb.append('\r');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case 'u':
                            if (pos + 4 > json.length()) {
                                throw new IllegalArgumentException("잘못된 유니코드 이스케이프");
                            }
                            String hex = json.substring(pos, pos + 4);
                            pos += 4;
                            sb.append((char) Integer.parseInt(hex, 16));
                            break;
                        default:
                            throw new IllegalArgumentException("알 수 없는 이스케이프: \\" + esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new IllegalArgumentException("문자열이 닫히지 않았습니다.");
        }

        private Object parseNumber() {
            int start = pos;
            if (json.charAt(pos) == '-') {
                pos++;
            }
            while (pos < json.length() && Character.isDigit(json.charAt(pos))) {
                pos++;
            }
            boolean isFraction = false;
            if (pos < json.length() && json.charAt(pos) == '.') {
                isFraction = true;
                pos++;
                while (pos < json.length() && Character.isDigit(json.charAt(pos))) {
                    pos++;
                }
            }
            if (pos < json.length() && (json.charAt(pos) == 'e' || json.charAt(pos) == 'E')) {
                isFraction = true;
                pos++;
                if (pos < json.length() && (json.charAt(pos) == '+' || json.charAt(pos) == '-')) {
                    pos++;
                }
                while (pos < json.length() && Character.isDigit(json.charAt(pos))) {
                    pos++;
                }
            }
            String number = json.substring(start, pos);
            try {
                if (isFraction) {
                    return Double.parseDouble(number);
                }
                long value = Long.parseLong(number);
                if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                    return (int) value;
                }
                return value;
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("잘못된 숫자: " + number, ex);
            }
        }

        private Boolean parseBoolean() {
            if (json.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            } else if (json.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("불리언 파싱 실패");
        }

        private Object parseNull() {
            if (json.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new IllegalArgumentException("null 파싱 실패");
        }

        private void skipWhitespace() {
            while (pos < json.length()) {
                char c = json.charAt(pos);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        void ensureFullyParsed() {
            skipWhitespace();
            if (pos != json.length()) {
                throw new IllegalArgumentException("JSON 데이터가 남았습니다.");
            }
        }

        private void expect(char c) {
            if (pos >= json.length() || json.charAt(pos) != c) {
                throw new IllegalArgumentException("문자 '" + c + "'가 필요합니다.");
            }
            pos++;
        }

        private boolean peek(char c) {
            return pos < json.length() && json.charAt(pos) == c;
        }
    }
}
