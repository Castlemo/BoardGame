package com.marblegame.network;

/**
 * 네트워크 관련 상수 정의
 */
public class NetConstants {
    // 기본 포트
    public static final int DEFAULT_PORT = 9999;

    // 타임아웃 설정 (밀리초)
    public static final int SOCKET_TIMEOUT = HEARTBEAT_TIMEOUT; // allow heartbeat detection
    public static final int CONNECTION_TIMEOUT = 10000;  // 10초

    // 플레이어 제한
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 4;

    // 기본 게임 설정
    public static final int DEFAULT_INITIAL_CASH = 1_500_000;

    // 하트비트 설정
    public static final int HEARTBEAT_INTERVAL = 5000;  // 5초마다 핑
    public static final int HEARTBEAT_TIMEOUT = 15000;  // 15초 응답 없으면 연결 끊김

    // 메시지 크기 제한
    public static final int MAX_MESSAGE_SIZE = 1048576;  // 1MB

    // 재연결 설정
    public static final int MAX_RECONNECT_ATTEMPTS = 3;
    public static final int RECONNECT_DELAY = 2000;  // 2초

    // 프로토콜 버전
    public static final String PROTOCOL_VERSION = "1.0";

    // 인코딩
    public static final String CHARSET = "UTF-8";

    private NetConstants() {
        // 유틸리티 클래스, 인스턴스화 방지
    }
}
