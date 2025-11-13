package com.marblegame.network.message;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class NetworkMessage {
    private static final Base64.Encoder PAYLOAD_ENCODER = Base64.getEncoder();
    private static final Base64.Decoder PAYLOAD_DECODER = Base64.getDecoder();

    private final MessageType type;
    private final String payload;

    public NetworkMessage(MessageType type, String payload) {
        this.type = type;
        this.payload = payload;
    }

    public MessageType getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    public String serialize() {
        String encodedPayload = payload == null
            ? ""
            : PAYLOAD_ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return type.name() + "|" + encodedPayload;
    }

    public static NetworkMessage deserialize(String raw) {
        int idx = raw.indexOf('|');
        if (idx < 0) {
            throw new IllegalArgumentException("잘못된 메시지 형식: " + raw);
        }
        String typePart = raw.substring(0, idx);
        String payloadPart = raw.substring(idx + 1);
        MessageType type = MessageType.valueOf(typePart);
        if (payloadPart.isEmpty()) {
            return new NetworkMessage(type, null);
        }
        try {
            byte[] decoded = PAYLOAD_DECODER.decode(payloadPart);
            return new NetworkMessage(type, new String(decoded, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException ex) {
            // 하위 호환: Base64 이전 포맷을 수용한다.
            return new NetworkMessage(type, payloadPart);
        }
    }
}
