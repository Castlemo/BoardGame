package com.marblegame.core.input;

/**
 * 플레이어가 요청할 수 있는 입력 종류.
 * 향후 네트워크/멀티플레이에서 공용 프로토콜로 사용한다.
 */
public enum PlayerInputType {
    GAUGE_PRESS,
    GAUGE_RELEASE,
    PURCHASE_CITY,
    UPGRADE_CITY,
    TAKEOVER,
    SKIP_TURN,
    PAY_BAIL,
    TOGGLE_ODD_MODE,
    TOGGLE_EVEN_MODE,
    TILE_SELECTED
}
