package com.marblegame.network.lobby;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 로비 상태를 단순 키-값 포맷으로 직렬화/역직렬화한다.
 */
public final class LobbyStateCodec {
    private LobbyStateCodec() {}

    public static String encode(LobbyStateView view) {
        if (view == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("maxPlayers=").append(view.maxPlayers).append('\n');
        sb.append("spectators=").append(view.spectatorCount).append('\n');
        sb.append("gameInProgress=").append(view.gameInProgress).append('\n');
        for (LobbySlotView slot : view.slots) {
            String prefix = "slot" + slot.index + ".";
            sb.append(prefix).append("label=").append(escape(slot.label)).append('\n');
            sb.append(prefix).append("occupied=").append(slot.occupied).append('\n');
            sb.append(prefix).append("ready=").append(slot.ready).append('\n');
            sb.append(prefix).append("name=").append(escape(slot.occupantName == null ? "" : slot.occupantName)).append('\n');
        }
        return sb.toString();
    }

    public static LobbyStateView decode(String raw) {
        if (raw == null || raw.isEmpty()) {
            throw new IllegalArgumentException("empty lobby state payload");
        }
        LobbyStateView view = new LobbyStateView();
        Map<Integer, LobbySlotView> slots = new LinkedHashMap<>();
        String[] lines = raw.split("\n");
        for (String line : lines) {
            if (line == null || line.isEmpty()) {
                continue;
            }
            int idx = line.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = line.substring(0, idx);
            String value = line.substring(idx + 1);
            if ("maxPlayers".equals(key)) {
                view.maxPlayers = parseInt(value, 0);
                continue;
            }
            if ("spectators".equals(key)) {
                view.spectatorCount = parseInt(value, 0);
                continue;
            }
            if ("gameInProgress".equals(key)) {
                view.gameInProgress = Boolean.parseBoolean(value);
                continue;
            }
            if (!key.startsWith("slot")) {
                continue;
            }
            int dotIdx = key.indexOf('.');
            if (dotIdx <= 4) {
                continue;
            }
            int slotIndex = parseInt(key.substring(4, dotIdx), -1);
            if (slotIndex < 0) {
                continue;
            }
            String property = key.substring(dotIdx + 1);
            LobbySlotView slot = slots.computeIfAbsent(slotIndex, idxKey -> {
                LobbySlotView s = new LobbySlotView();
                s.index = idxKey;
                return s;
            });
            switch (property) {
                case "label":
                    slot.label = unescape(value);
                    break;
                case "occupied":
                    slot.occupied = Boolean.parseBoolean(value);
                    break;
                case "ready":
                    slot.ready = Boolean.parseBoolean(value);
                    break;
                case "name":
                    slot.occupantName = unescape(value);
                    break;
                default:
                    break;
            }
        }
        view.slots.addAll(slots.values());
        view.slots.sort((a, b) -> Integer.compare(a.index, b.index));
        return view;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
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
