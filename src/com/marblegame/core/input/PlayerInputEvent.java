package com.marblegame.core.input;

/**
 * 플레이어 입력 이벤트. 타입과 선택적으로 정수 페이로드를 포함한다.
 */
public class PlayerInputEvent {
    private final PlayerInputType type;
    private final Integer intValue;

    private PlayerInputEvent(PlayerInputType type, Integer intValue) {
        this.type = type;
        this.intValue = intValue;
    }

    public static PlayerInputEvent of(PlayerInputType type) {
        return new PlayerInputEvent(type, null);
    }

    public static PlayerInputEvent withInt(PlayerInputType type, int value) {
        return new PlayerInputEvent(type, value);
    }

    public PlayerInputType getType() {
        return type;
    }

    public Integer getIntValue() {
        return intValue;
    }

    public int requireIntValue() {
        if (intValue == null) {
            throw new IllegalStateException("정수 값이 필요한 이벤트입니다: " + type);
        }
        return intValue;
    }
}
