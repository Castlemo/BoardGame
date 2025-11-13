package com.marblegame.network.message;

import com.marblegame.core.input.PlayerInputEvent;
import com.marblegame.core.input.PlayerInputType;

public final class RemoteActionCodec {
    private RemoteActionCodec() {}

    public static NetworkMessage encode(PlayerInputEvent event) {
        String payload = event.getType().name();
        if (event.getIntValue() != null) {
            payload += ":" + event.getIntValue();
        }
        return new NetworkMessage(MessageType.PLAYER_ACTION, payload);
    }

    public static PlayerInputEvent decode(NetworkMessage message) {
        if (message.getType() != MessageType.PLAYER_ACTION) {
            throw new IllegalArgumentException("PLAYER_ACTION 메시지 필요");
        }
        String payload = message.getPayload();
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("빈 액션 페이로드");
        }
        String[] parts = payload.split(":", 2);
        PlayerInputType type = PlayerInputType.valueOf(parts[0]);
        if (parts.length == 2) {
            int value = Integer.parseInt(parts[1]);
            return PlayerInputEvent.withInt(type, value);
        }
        return PlayerInputEvent.of(type);
    }
}
