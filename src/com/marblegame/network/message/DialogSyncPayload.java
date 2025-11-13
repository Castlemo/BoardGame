package com.marblegame.network.message;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 다이얼로그 정보를 네트워크로 전달할 때 사용하는 단순 키-값 페이로드.
 */
public class DialogSyncPayload {
    private final DialogType type;
    private final Map<String, String> attributes;

    DialogSyncPayload(DialogType type, Map<String, String> attributes) {
        this.type = type;
        this.attributes = attributes;
    }

    public DialogType getType() {
        return type;
    }

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public String get(String key) {
        return attributes.get(key);
    }

    public int getInt(String key, int defaultValue) {
        String value = attributes.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = attributes.get(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    public static Builder builder(DialogType type) {
        return new Builder(type);
    }

    public static class Builder {
        private final DialogType type;
        private final Map<String, String> attributes = new LinkedHashMap<>();

        public Builder(DialogType type) {
            this.type = type;
        }

        public Builder put(String key, String value) {
            if (key != null && value != null) {
                attributes.put(key, value);
            }
            return this;
        }

        public Builder putInt(String key, int value) {
            return put(key, Integer.toString(value));
        }

        public Builder putBoolean(String key, boolean value) {
            return put(key, Boolean.toString(value));
        }

        public DialogSyncPayload build() {
            return new DialogSyncPayload(type, new LinkedHashMap<>(attributes));
        }
    }
}
