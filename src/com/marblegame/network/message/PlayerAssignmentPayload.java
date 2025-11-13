package com.marblegame.network.message;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 호스트가 특정 클라이언트에게 플레이어 슬롯을 할당할 때 사용하는 단순 키-값 페이로드.
 * index=-1이면 관전자 모드로 취급한다.
 */
public final class PlayerAssignmentPayload {
    private final int playerIndex;
    private final String playerName;
    private final String note;

    public PlayerAssignmentPayload(int playerIndex, String playerName, String note) {
        this.playerIndex = playerIndex;
        this.playerName = playerName == null ? "" : playerName;
        this.note = note;
    }

    public int getPlayerIndex() {
        return playerIndex;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getNote() {
        return note;
    }

    public boolean isSpectator() {
        return playerIndex < 0;
    }

    public static String encode(PlayerAssignmentPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("index=").append(payload.playerIndex).append('\n');
        sb.append("name=").append(escape(payload.playerName));
        if (payload.note != null && !payload.note.isEmpty()) {
            sb.append('\n').append("note=").append(escape(payload.note));
        }
        return sb.toString();
    }

    public static PlayerAssignmentPayload decode(String raw) {
        if (raw == null || raw.isEmpty()) {
            throw new IllegalArgumentException("empty assignment payload");
        }
        Map<String, String> attributes = new LinkedHashMap<>();
        String[] lines = raw.split("\n");
        for (String line : lines) {
            if (line.isEmpty()) {
                continue;
            }
            int idx = line.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = line.substring(0, idx);
            String value = unescape(line.substring(idx + 1));
            attributes.put(key, value);
        }
        if (!attributes.containsKey("index")) {
            throw new IllegalArgumentException("assignment payload missing index");
        }
        int index = Integer.parseInt(attributes.get("index"));
        String name = attributes.getOrDefault("name", "");
        String note = attributes.get("note");
        return new PlayerAssignmentPayload(index, name, note);
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '=':
                    sb.append("\\e");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String unescape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean escaping = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaping) {
                switch (c) {
                    case '\\':
                        sb.append('\\');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'e':
                        sb.append('=');
                        break;
                    default:
                        sb.append(c);
                        break;
                }
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else {
                sb.append(c);
            }
        }
        if (escaping) {
            sb.append('\\');
        }
        return sb.toString();
    }
}
