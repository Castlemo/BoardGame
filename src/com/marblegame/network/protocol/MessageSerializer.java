package com.marblegame.network.protocol;

import java.util.HashMap;
import java.util.Map;

/**
 * 메시지 직렬화/역직렬화 클래스
 * 간단한 JSON 형태로 변환
 */
public class MessageSerializer {

    /**
     * Message를 JSON 문자열로 직렬화
     * @param message 메시지
     * @return JSON 문자열
     */
    public static String serialize(Message message) {
        StringBuilder json = new StringBuilder();
        json.append("{");

        // type
        json.append("\"type\":\"").append(message.getType().name()).append("\",");

        // playerId
        if (message.getPlayerId() != null) {
            json.append("\"playerId\":\"").append(escapeJson(message.getPlayerId())).append("\",");
        } else {
            json.append("\"playerId\":null,");
        }

        // timestamp
        json.append("\"timestamp\":").append(message.getTimestamp()).append(",");

        // data
        json.append("\"data\":{");
        Map<String, Object> data = message.getData();
        if (data != null && !data.isEmpty()) {
            boolean first = true;
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (!first) {
                    json.append(",");
                }
                first = false;
                json.append("\"").append(escapeJson(entry.getKey())).append("\":");
                json.append(serializeValue(entry.getValue()));
            }
        }
        json.append("}");

        json.append("}");
        return json.toString();
    }

    /**
     * JSON 문자열을 Message로 역직렬화
     * @param jsonString JSON 문자열
     * @return Message 객체
     * @throws IllegalArgumentException JSON 파싱 실패 시
     */
    public static Message deserialize(String jsonString) {
        try {
            Message message = new Message();

            // 간단한 JSON 파싱
            String content = jsonString.trim();
            if (!content.startsWith("{") || !content.endsWith("}")) {
                throw new IllegalArgumentException("Invalid JSON format");
            }

            content = content.substring(1, content.length() - 1);

            // 필드 파싱
            Map<String, String> fields = parseFields(content);

            // type 파싱
            String typeStr = fields.get("type");
            if (typeStr != null) {
                typeStr = unquote(typeStr);
                message.setType(MessageType.valueOf(typeStr));
            }

            // playerId 파싱
            String playerIdStr = fields.get("playerId");
            if (playerIdStr != null && !playerIdStr.equals("null")) {
                message.setPlayerId(unquote(playerIdStr));
            }

            // timestamp 파싱
            String timestampStr = fields.get("timestamp");
            if (timestampStr != null) {
                message.setTimestamp(Long.parseLong(timestampStr));
            }

            // data 파싱
            String dataStr = fields.get("data");
            if (dataStr != null && !dataStr.equals("null")) {
                Map<String, Object> data = parseData(dataStr);
                message.setData(data);
            }

            return message;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to deserialize message: " + e.getMessage(), e);
        }
    }

    /**
     * 값을 JSON 형식으로 직렬화
     */
    private static String serializeValue(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "\"" + escapeJson((String) value) + "\"";
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
                sb.append(serializeValue(entry.getValue()));
            }
            sb.append("}");
            return sb.toString();
        } else {
            // 기본적으로 toString() 사용
            return "\"" + escapeJson(value.toString()) + "\"";
        }
    }

    /**
     * JSON 문자열에서 필드 파싱
     */
    private static Map<String, String> parseFields(String content) {
        Map<String, String> fields = new HashMap<>();
        int depth = 0;
        int start = 0;
        String currentKey = null;
        boolean inString = false;
        boolean escape = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (escape) {
                escape = false;
                continue;
            }

            if (c == '\\') {
                escape = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (inString) {
                continue;
            }

            if (c == '{' || c == '[') {
                depth++;
            } else if (c == '}' || c == ']') {
                depth--;
            } else if (c == ':' && depth == 0) {
                currentKey = content.substring(start, i).trim();
                currentKey = unquote(currentKey);
                start = i + 1;
            } else if (c == ',' && depth == 0) {
                if (currentKey != null) {
                    String value = content.substring(start, i).trim();
                    fields.put(currentKey, value);
                }
                start = i + 1;
                currentKey = null;
            }
        }

        // 마지막 필드 처리
        if (currentKey != null) {
            String value = content.substring(start).trim();
            fields.put(currentKey, value);
        }

        return fields;
    }

    /**
     * data 객체 파싱
     */
    private static Map<String, Object> parseData(String dataStr) {
        Map<String, Object> data = new HashMap<>();

        dataStr = dataStr.trim();
        if (!dataStr.startsWith("{") || !dataStr.endsWith("}")) {
            return data;
        }

        dataStr = dataStr.substring(1, dataStr.length() - 1).trim();
        if (dataStr.isEmpty()) {
            return data;
        }

        Map<String, String> fields = parseFields(dataStr);

        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String key = entry.getKey();
            String valueStr = entry.getValue();
            Object value = parseValue(valueStr);
            data.put(key, value);
        }

        return data;
    }

    /**
     * 값 파싱
     */
    private static Object parseValue(String valueStr) {
        valueStr = valueStr.trim();

        if (valueStr.equals("null")) {
            return null;
        } else if (valueStr.equals("true")) {
            return true;
        } else if (valueStr.equals("false")) {
            return false;
        } else if (valueStr.startsWith("\"") && valueStr.endsWith("\"")) {
            return unquote(valueStr);
        } else if (valueStr.matches("-?\\d+")) {
            return Integer.parseInt(valueStr);
        } else if (valueStr.matches("-?\\d+\\.\\d+")) {
            return Double.parseDouble(valueStr);
        } else if (valueStr.startsWith("{") && valueStr.endsWith("}")) {
            return parseData(valueStr);
        } else {
            return valueStr;
        }
    }

    /**
     * JSON 문자열 이스케이프
     */
    private static String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * 따옴표 제거
     */
    private static String unquote(String str) {
        if (str == null) {
            return null;
        }
        str = str.trim();
        if (str.startsWith("\"") && str.endsWith("\"") && str.length() >= 2) {
            str = str.substring(1, str.length() - 1);
            // 이스케이프 문자 복원
            str = str.replace("\\\"", "\"")
                     .replace("\\\\", "\\")
                     .replace("\\n", "\n")
                     .replace("\\r", "\r")
                     .replace("\\t", "\t");
        }
        return str;
    }
}
