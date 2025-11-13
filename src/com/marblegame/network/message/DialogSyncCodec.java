package com.marblegame.network.message;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 간단한 줄 기반 직렬화/역직렬화를 제공한다.
 * 첫 줄은 DialogType, 이후 각 줄은 key=value 형식이며 기본적인 escape 처리만 수행한다.
 */
public final class DialogSyncCodec {
    private DialogSyncCodec() {}

    public static String encode(DialogSyncPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
        StringBuilder sb = new StringBuilder();
        sb.append(payload.getType().name());
        for (Map.Entry<String, String> entry : payload.getAttributes().entrySet()) {
            sb.append('\n')
                .append(escape(entry.getKey()))
                .append('=')
                .append(escape(entry.getValue()));
        }
        return sb.toString();
    }

    public static DialogSyncPayload decode(String raw) {
        if (raw == null || raw.isEmpty()) {
            throw new IllegalArgumentException("payload is empty");
        }
        String[] lines = raw.split("\n", -1);
        if (lines.length == 0) {
            throw new IllegalArgumentException("payload is malformed");
        }
        DialogType type = DialogType.valueOf(lines[0]);
        Map<String, String> attributes = new LinkedHashMap<>();
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
            attributes.put(key, value);
        }
        return new DialogSyncPayload(type, attributes);
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
