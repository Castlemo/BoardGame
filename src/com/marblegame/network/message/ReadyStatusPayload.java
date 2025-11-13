package com.marblegame.network.message;

/**
 * 클라이언트의 준비 상태 토글을 표현한다.
 */
public final class ReadyStatusPayload {
    private final boolean ready;

    public ReadyStatusPayload(boolean ready) {
        this.ready = ready;
    }

    public boolean isReady() {
        return ready;
    }

    public static String encode(ReadyStatusPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
        return payload.ready ? "ready=true" : "ready=false";
    }

    public static ReadyStatusPayload decode(String raw) {
        if (raw == null || raw.isEmpty()) {
            throw new IllegalArgumentException("empty ready payload");
        }
        String[] parts = raw.split("=");
        if (parts.length != 2 || !"ready".equals(parts[0])) {
            throw new IllegalArgumentException("invalid ready payload");
        }
        return new ReadyStatusPayload(Boolean.parseBoolean(parts[1]));
    }
}
