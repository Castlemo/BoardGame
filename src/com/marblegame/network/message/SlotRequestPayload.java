package com.marblegame.network.message;

/**
 * 클라이언트가 특정 슬롯을 요청하거나 해제할 때 사용하는 페이로드.
 * slotIndex가 -1이면 현재 슬롯을 해제한다.
 */
public final class SlotRequestPayload {
    private final int slotIndex;
    private final String playerName;

    public SlotRequestPayload(int slotIndex, String playerName) {
        this.slotIndex = slotIndex;
        this.playerName = playerName == null ? "" : playerName.trim();
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public String getPlayerName() {
        return playerName;
    }

    public static String encode(SlotRequestPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("slot=").append(payload.slotIndex).append('\n');
        sb.append("name=").append(escape(payload.playerName));
        return sb.toString();
    }

    public static SlotRequestPayload decode(String raw) {
        if (raw == null || raw.isEmpty()) {
            throw new IllegalArgumentException("empty slot request payload");
        }
        String[] lines = raw.split("\n");
        Integer slot = null;
        String name = "";
        for (String line : lines) {
            if (line == null || line.isEmpty()) {
                continue;
            }
            int idx = line.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = line.substring(0, idx);
            String value = unescape(line.substring(idx + 1));
            if ("slot".equals(key)) {
                slot = Integer.parseInt(value);
            } else if ("name".equals(key)) {
                name = value;
            }
        }
        if (slot == null) {
            throw new IllegalArgumentException("slot index missing");
        }
        return new SlotRequestPayload(slot, name);
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
