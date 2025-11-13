package com.marblegame.core.input;

/**
 * 플레이어 입력을 처리하는 대상.
 * 호스트/클라이언트 모드 모두에서 동일한 인터페이스를 사용한다.
 */
public interface PlayerInputSink {
    void handlePlayerInput(PlayerInputEvent event);
}
