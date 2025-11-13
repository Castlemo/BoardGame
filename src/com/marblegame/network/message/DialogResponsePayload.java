package com.marblegame.network.message;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DialogResponsePayload {
    private final String requestId;
    private final DialogType dialogType;
    private final int playerIndex;
    private final String result;
    private final Map<String, String> attributes;

    public DialogResponsePayload(String requestId, DialogType dialogType, int playerIndex, String result,
                                 Map<String, String> attributes) {
        if (requestId == null || requestId.isEmpty()) {
            throw new IllegalArgumentException("requestId must not be empty");
        }
        if (dialogType == null) {
            throw new IllegalArgumentException("dialogType must not be null");
        }
        this.requestId = requestId;
        this.dialogType = dialogType;
        this.playerIndex = playerIndex;
        this.result = result == null ? "" : result;
        this.attributes = attributes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(attributes);
    }

    public String getRequestId() {
        return requestId;
    }

    public DialogType getDialogType() {
        return dialogType;
    }

    public int getPlayerIndex() {
        return playerIndex;
    }

    public String getResult() {
        return result;
    }

    public Map<String, String> getAttributes() {
        return new LinkedHashMap<>(attributes);
    }

    public static String encode(DialogResponsePayload payload) {
        StringBuilder sb = new StringBuilder();
        sb.append(payload.dialogType.name());
        sb.append('\n').append("requestId=").append(escape(payload.requestId));
        sb.append('\n').append("playerIndex=").append(payload.playerIndex);
        sb.append('\n').append("result=").append(escape(payload.result));
        for (Map.Entry<String, String> entry : payload.attributes.entrySet()) {
            sb.append('\n')
                .append(escape(entry.getKey()))
                .append('=')
                .append(escape(entry.getValue()));
        }
        return sb.toString();
    }

    public static DialogResponsePayload decode(String raw) {
        if (raw == null || raw.isEmpty()) {
            throw new IllegalArgumentException("empty dialog response payload");
        }
        String[] lines = raw.split("\n", -1);
        if (lines.length == 0) {
            throw new IllegalArgumentException("malformed dialog response payload");
        }
        DialogType type = DialogType.valueOf(lines[0]);
        String requestId = null;
        int playerIndex = -1;
        String result = "";
        Map<String, String> attrs = new LinkedHashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) {
                continue;
            }
            int idx = line.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = unescape(line.substring(0, idx));
            String value = unescape(line.substring(idx + 1));
            switch (key) {
                case "requestId":
                    requestId = value;
                    break;
                case "playerIndex":
                    playerIndex = Integer.parseInt(value);
                    break;
                case "result":
                    result = value;
                    break;
                default:
                    attrs.put(key, value);
                    break;
            }
        }
        if (requestId == null) {
            throw new IllegalArgumentException("dialog response missing requestId");
        }
        return new DialogResponsePayload(requestId, type, playerIndex, result, attrs);
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
