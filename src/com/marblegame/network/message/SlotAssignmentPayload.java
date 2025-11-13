package com.marblegame.network.message;

/**
 * 호스트가 특정 클라이언트에게 슬롯 배정 결과를 알릴 때 사용하는 페이로드.
 */
public final class SlotAssignmentPayload {
    public enum Status {
        ASSIGNED,
        RELEASED,
        DENIED
    }

    private final int slotIndex;
    private final String playerName;
    private final Status status;
    private final String note;

    public SlotAssignmentPayload(int slotIndex, String playerName, Status status, String note) {
        this.slotIndex = slotIndex;
        this.playerName = playerName == null ? "" : playerName;
        this.status = status == null ? Status.DENIED : status;
        this.note = note == null ? "" : note;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Status getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public static String encode(SlotAssignmentPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("slot=").append(payload.slotIndex).append('\n');
        sb.append("name=").append(escape(payload.playerName)).append('\n');
        sb.append("status=").append(payload.status.name());
        if (!payload.note.isEmpty()) {
            sb.append('\n').append("note=").append(escape(payload.note));
        }
        return sb.toString();
    }

    public static SlotAssignmentPayload decode(String raw) {
        if (raw == null || raw.isEmpty()) {
            throw new IllegalArgumentException("empty slot assignment payload");
        }
        String[] lines = raw.split("\n");
        Integer slot = null;
        String name = "";
        Status status = Status.DENIED;
        String note = "";
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
            switch (key) {
                case "slot":
                    slot = Integer.parseInt(value);
                    break;
                case "name":
                    name = value;
                    break;
                case "status":
                    status = Status.valueOf(value);
                    break;
                case "note":
                    note = value;
                    break;
                default:
                    break;
            }
        }
        if (slot == null) {
            throw new IllegalArgumentException("slot index missing");
        }
        return new SlotAssignmentPayload(slot, name, status, note);
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
